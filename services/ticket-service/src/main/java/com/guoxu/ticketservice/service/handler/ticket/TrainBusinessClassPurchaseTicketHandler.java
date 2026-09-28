package com.guoxu.ticketservice.service.handler.ticket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Pair;
import com.google.common.collect.Lists;
import com.guoxu.exception.ServiceException;
import com.guoxu.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.guoxu.ticketservice.common.enums.VehicleTypeEnum;
import com.guoxu.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.guoxu.ticketservice.dto.domain.TrainSeatBaseDTO;
import com.guoxu.ticketservice.service.SeatService;
import com.guoxu.ticketservice.service.handler.ticket.base.AbstractTrainPurchaseTicketTemplate;
import com.guoxu.ticketservice.service.handler.ticket.base.BitMapCheckSeat;
import com.guoxu.ticketservice.service.handler.ticket.base.BitMapCheckSeatStatusFactory;
import com.guoxu.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.guoxu.ticketservice.service.handler.ticket.select.SeatSelection;
import com.guoxu.ticketservice.toolkit.CarriageVacantSeatCalculateUtil;
import com.guoxu.ticketservice.toolkit.SeatNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.guoxu.ticketservice.service.handler.ticket.base.BitMapCheckSeatStatusFactory.TRAIN_BUSINESS;

/**
 * TrainBusinessClassPurchaseTicketHandler
 * 高铁商务座购票处理器组件
 * @author 执笔画棠
 * @date 2025/11/13 20:15
 **/
@Component
@RequiredArgsConstructor
public class TrainBusinessClassPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {// 继承抽象购票模板类
    private final SeatService seatService; // 座位服务，用于获取座位相关信息

    // 座位字母到数字索引的映射表，用于内部数组存储和计算
    private static final Map<Character, Integer> SEAT_Y_INT = Map.of('A', 0, 'C', 1, 'F', 2);

    /**
     * 获取当前处理器的标识
     * @return 处理器标识字符串，格式为"车辆类型+座位类型"
     */
    @Override
    public String mark() {
        // 组合车辆类型和座位类型作为标识
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.BUSINESS_CLASS.getName();
    }

    /**
     * 重写选座方法，根据乘客数量和选座需求选择合适的座位
     * @param requestParam 选座请求参数
     * @return 购票响应列表，包含每位乘客的座位信息
     */
    @Override
    protected List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam) {
        // 从请求参数中提取关键信息
        String trainId = requestParam.getRequestParam().getTrainId(); // 获取列车ID
        String departure = requestParam.getRequestParam().getDeparture(); // 获取出发站
        String arrival = requestParam.getRequestParam().getArrival(); // 获取到达站
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails(); // 获取乘客座位详情列表

        // 获取可用车厢号列表
        List<String> trainCarriageList = seatService.listUsableCarriageNumber(trainId, requestParam.getSeatType(),
                departure, arrival);

        // 获取各车厢剩余票数
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(trainId, departure,
                arrival, trainCarriageList);

        // 计算总剩余票数
        int remainingTicketSum = trainStationCarriageRemainingTicket.stream().mapToInt(Integer::intValue).sum();

        // 检查总剩余票数是否满足乘客需求
        if (remainingTicketSum < passengerSeatDetails.size()) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
        }

        // 根据乘客数量选择不同的选座策略
        if (passengerSeatDetails.size() < 3) { // 乘客数量少于3人
            if (CollUtil.isNotEmpty(requestParam.getRequestParam().getChooseSeats())) { // 如果用户指定了座位
                // 查找匹配的指定座位
                Pair<List<TrainPurchaseTicketRespDTO>, Boolean> actualSeatPair = findMatchSeats(requestParam,
                        trainCarriageList, trainStationCarriageRemainingTicket);
                return actualSeatPair.getKey();
            }
            // 执行普通选座逻辑
            return selectSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
        } else { // 乘客数量大于等于3人
            if (CollUtil.isNotEmpty(requestParam.getRequestParam().getChooseSeats())) { // 如果用户指定了座位
                // 查找匹配的指定座位
                Pair<List<TrainPurchaseTicketRespDTO>, Boolean> actualSeatPair = findMatchSeats(requestParam,
                        trainCarriageList, trainStationCarriageRemainingTicket);
                return actualSeatPair.getKey();
            }
            // 执行复杂选座逻辑（多人选座）
            return selectComplexSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
        }
    }

    /**
     * 查找匹配用户指定座位的方法
     * @param requestParam 选座请求参数
     * @param trainCarriageList 可用车厢列表
     * @param trainStationCarriageRemainingTicket 各车厢剩余票数
     * @return 包含座位分配结果和是否成功标志的键值对
     */
    private Pair<List<TrainPurchaseTicketRespDTO>, Boolean> findMatchSeats(SelectSeatDTO requestParam,
                                                                           List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
        // 构建列车座位基础DTO对象，包含必要的选座信息
        TrainSeatBaseDTO trainSeatBaseDTO = buildTrainSeatBaseDTO(requestParam);
        // 获取用户指定的座位数量
        int chooseSeatSize = trainSeatBaseDTO.getChooseSeatList().size();
        // 创建结果列表，预分配容量以提高性能
        List<TrainPurchaseTicketRespDTO> actualResult = Lists
                .newArrayListWithCapacity(trainSeatBaseDTO.getPassengerSeatDetails().size());
        // 获取商务座的位图检查座位实例
        BitMapCheckSeat instance = BitMapCheckSeatStatusFactory.getInstance(TRAIN_BUSINESS);
        // 创建车厢座位映射表，用于存储各车厢可用座位
        HashMap<String, List<Pair<Integer, Integer>>> carriagesSeatMap = new HashMap<>(4);
        // 获取乘客数量
        int passengersNumber = trainSeatBaseDTO.getPassengerSeatDetails().size();

        // 遍历所有车厢，查找匹配的座位
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            // 获取当前车厢号
            String carriagesNumber = trainCarriageList.get(i);
            // 获取当前车厢的可用座位列表
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainSeatBaseDTO.getTrainId(),
                    carriagesNumber, requestParam.getSeatType(), trainSeatBaseDTO.getDeparture(),
                    trainSeatBaseDTO.getArrival());

            // 创建座位状态二维数组（2行3列，高铁商务座布局）
            int[][] actualSeats = new int[2][3];

            // 将可用座位信息转换为二维数组表示（0表示可用，1表示已售）
            for (int j = 1; j < 3; j++) { // 遍历行（商务座只有2排）
                for (int k = 1; k < 4; k++) { // 遍历列（商务座每排3个座位：A、C、F）
                    // 检查座位是否在可用座位列表中，并设置相应状态
                    actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(0, k)) ? 0
                            : 1;
                }
            }

            // 计算当前车厢的空位列表
            List<Pair<Integer, Integer>> vacantSeatList = CarriageVacantSeatCalculateUtil
                    .buildCarriageVacantSeatList2(actualSeats, 2, 3);

            // 检查用户指定的座位是否存在且可用
            boolean isExists = instance.checkChooseSeat(trainSeatBaseDTO.getChooseSeatList(), actualSeats, SEAT_Y_INT);

            // 获取空位数量
            long vacantSeatCount = vacantSeatList.size();

            // 创建确定的座位列表和选择的座位列表
            List<Pair<Integer, Integer>> sureSeatList = new ArrayList<>();
            List<String> selectSeats = Lists.newArrayListWithCapacity(passengersNumber);

            // 标志位，用于标记是否需要继续查找下一个车厢
            boolean flag = false;
            // 如果用户指定的座位存在且当前车厢空位足够
            if (isExists && vacantSeatCount >= passengersNumber) {
                // 获取空位列表的迭代器
                Iterator<Pair<Integer, Integer>> pairIterator = vacantSeatList.iterator();

                // 遍历用户指定的每个座位
                for (int i1 = 0; i1 < chooseSeatSize; i1++) {
                    // 处理单个座位选择的情况
                    if (chooseSeatSize == 1) {
                        // 获取用户选择的座位信息
                        String chooseSeat = trainSeatBaseDTO.getChooseSeatList().get(i1);
                        // 解析座位行号
                        int seatX = Integer.parseInt(chooseSeat.substring(1));
                        // 解析座位列号（通过映射表转换字母为数字）
                        int seatY = SEAT_Y_INT.get(chooseSeat.charAt(0));

                        // 检查用户选择的座位是否可用
                        if (actualSeats[seatX][seatY] == 0) {
                            // 将座位添加到确定座位列表
                            sureSeatList.add(new Pair<>(seatX, seatY));
                            // 从空位列表中移除已选中的座位
                            while (pairIterator.hasNext()) {
                                Pair<Integer, Integer> pair = pairIterator.next();
                                if (pair.getKey() == seatX && pair.getValue() == seatY) {
                                    pairIterator.remove();
                                    break;
                                }
                            }
                        } else {
                            // 如果用户选择的座位不可用，尝试同一列的其他排
                            if (actualSeats[1][seatY] == 0) {
                                sureSeatList.add(new Pair<>(1, seatY));
                                // 从空位列表中移除已选中的座位
                                while (pairIterator.hasNext()) {
                                    Pair<Integer, Integer> pair = pairIterator.next();
                                    if (pair.getKey() == 1 && pair.getValue() == seatY) {
                                        pairIterator.remove();
                                        break;
                                    }
                                }
                            } else {
                                // 如果该列都不可用，设置标志位继续查找下一车厢
                                flag = true;
                            }
                        }
                    } else {
                        // 处理多个座位选择的情况
                        String chooseSeat = trainSeatBaseDTO.getChooseSeatList().get(i1);
                        int seatX = Integer.parseInt(chooseSeat.substring(1));
                        int seatY = SEAT_Y_INT.get(chooseSeat.charAt(0));

                        // 检查座位是否可用
                        if (actualSeats[seatX][seatY] == 0) {
                            sureSeatList.add(new Pair<>(seatX, seatY));
                            // 从空位列表中移除已选中的座位
                            while (pairIterator.hasNext()) {
                                Pair<Integer, Integer> pair = pairIterator.next();
                                if (pair.getKey() == seatX && pair.getValue() == seatY) {
                                    pairIterator.remove();
                                    break;
                                }
                            }
                        }
                    }
                }

                // 如果需要继续查找且不是最后一个车厢，则跳过当前车厢
                if (flag && i < trainStationCarriageRemainingTicket.size() - 1) {
                    continue;
                }

                // 如果确定的座位数量不足，从空位列表中补充
                if (sureSeatList.size() != passengersNumber) {
                    int needSeatSize = passengersNumber - sureSeatList.size();
                    sureSeatList.addAll(vacantSeatList.subList(0, needSeatSize));
                }

                // 将座位坐标转换为标准座位号格式
                for (Pair<Integer, Integer> each : sureSeatList) {
                    selectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(0, (each.getValue() + 1)));
                }

                // 使用原子整数作为计数器，为每位乘客分配座位
                AtomicInteger countNum = new AtomicInteger(0);
                for (String selectSeat : selectSeats) {
                    // 创建购票响应对象
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    // 获取当前乘客信息
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = trainSeatBaseDTO.getPassengerSeatDetails()
                            .get(countNum.getAndIncrement());
                    // 设置座位信息
                    result.setSeatNumber(selectSeat);
                    result.setSeatType(currentTicketPassenger.getSeatType());
                    result.setCarriageNumber(carriagesNumber);
                    result.setPassengerId(currentTicketPassenger.getPassengerId());
                    // 添加到结果列表
                    actualResult.add(result);
                }

                // 返回成功的结果和标志
                return new Pair<>(actualResult, Boolean.TRUE);
            } else {
                if (i < trainStationCarriageRemainingTicket.size()) {
                    if (vacantSeatCount > 0) {
                        carriagesSeatMap.put(carriagesNumber, vacantSeatList);
                    }
                    if (i == trainStationCarriageRemainingTicket.size() - 1) {
                        Pair<String, List<Pair<Integer, Integer>>> findSureCarriage = null;
                        for (Map.Entry<String, List<Pair<Integer, Integer>>> entry : carriagesSeatMap.entrySet()) {
                            if (entry.getValue().size() >= passengersNumber) {
                                findSureCarriage = new Pair<>(entry.getKey(),
                                        entry.getValue().subList(0, passengersNumber));
                                break;
                            }
                        }
                        if (null != findSureCarriage) {
                            sureSeatList = findSureCarriage.getValue().subList(0, passengersNumber);
                            for (Pair<Integer, Integer> each : sureSeatList) {
                                selectSeats.add(
                                        "0" + (each.getKey() + 1) + SeatNumberUtil.convert(0, each.getValue() + 1));
                            }
                            AtomicInteger countNum = new AtomicInteger(0);
                            for (String selectSeat : selectSeats) {
                                TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                                PurchaseTicketPassengerDetailDTO currentTicketPassenger = trainSeatBaseDTO
                                        .getPassengerSeatDetails().get(countNum.getAndIncrement());
                                result.setSeatNumber(selectSeat);
                                result.setSeatType(currentTicketPassenger.getSeatType());
                                result.setCarriageNumber(findSureCarriage.getKey());
                                result.setPassengerId(currentTicketPassenger.getPassengerId());
                                actualResult.add(result);
                            }
                        } else {
                            int sureSeatListSize = 0;
                            AtomicInteger countNum = new AtomicInteger(0);
                            for (Map.Entry<String, List<Pair<Integer, Integer>>> entry : carriagesSeatMap.entrySet()) {
                                if (sureSeatListSize < passengersNumber) {
                                    if (sureSeatListSize + entry.getValue().size() < passengersNumber) {
                                        sureSeatListSize = sureSeatListSize + entry.getValue().size();
                                        List<String> actualSelectSeats = new ArrayList<>();
                                        for (Pair<Integer, Integer> each : entry.getValue()) {
                                            actualSelectSeats.add("0" + (each.getKey() + 1)
                                                    + SeatNumberUtil.convert(0, each.getValue() + 1));
                                        }
                                        for (String selectSeat : actualSelectSeats) {
                                            TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                                            PurchaseTicketPassengerDetailDTO currentTicketPassenger = trainSeatBaseDTO
                                                    .getPassengerSeatDetails().get(countNum.getAndIncrement());
                                            result.setSeatNumber(selectSeat);
                                            result.setSeatType(currentTicketPassenger.getSeatType());
                                            result.setCarriageNumber(entry.getKey());
                                            result.setPassengerId(currentTicketPassenger.getPassengerId());
                                            actualResult.add(result);
                                        }
                                    } else {
                                        int needSeatSize = entry.getValue().size()
                                                - (sureSeatListSize + entry.getValue().size() - passengersNumber);
                                        sureSeatListSize = sureSeatListSize + needSeatSize;
                                        if (sureSeatListSize >= passengersNumber) {
                                            List<String> actualSelectSeats = new ArrayList<>();
                                            for (Pair<Integer, Integer> each : entry.getValue().subList(0,
                                                    needSeatSize)) {
                                                actualSelectSeats.add("0" + (each.getKey() + 1)
                                                        + SeatNumberUtil.convert(0, each.getValue() + 1));
                                            }
                                            for (String selectSeat : actualSelectSeats) {
                                                TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                                                PurchaseTicketPassengerDetailDTO currentTicketPassenger = trainSeatBaseDTO
                                                        .getPassengerSeatDetails().get(countNum.getAndIncrement());
                                                result.setSeatNumber(selectSeat);
                                                result.setSeatType(currentTicketPassenger.getSeatType());
                                                result.setCarriageNumber(entry.getKey());
                                                result.setPassengerId(currentTicketPassenger.getPassengerId());
                                                actualResult.add(result);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        return new Pair<>(actualResult, Boolean.TRUE);
                    }
                }
            }
        }
        return new Pair<>(null, Boolean.FALSE);
    }

    /**
     * 选择座位方法 - 根据请求参数和可用座位信息为乘客分配座位
     * 实现多级降级策略：优先邻座分配，其次同车厢不邻座，最后不同车厢分配
     *
     * @param requestParam 请求参数，包含列车信息、乘客信息和选座要求
     * @param trainCarriageList 列车车厢列表
     * @param trainStationCarriageRemainingTicket 各车厢剩余票数量
     * @return 座位分配结果列表
     */
    private List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam, List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
        // 获取列车信息
        String trainId = requestParam.getRequestParam().getTrainId();
        String departure = requestParam.getRequestParam().getDeparture();
        String arrival = requestParam.getRequestParam().getArrival();
        // 获取乘客信息列表
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();
        // 初始化结果列表
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        // 存储各车厢降级分配的可用座位数
        Map<String, Integer> demotionStockNumMap = new LinkedHashMap<>();
        // 存储各车厢的实际座位状态
        Map<String, int[][]> actualSeatsMap = new HashMap<>();
        // 存储已选择的座位信息
        Map<String, int[][]> carriagesNumberSeatsMap = new HashMap<>();

        String carriagesNumber;
        // 遍历所有车厢
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            carriagesNumber = trainCarriageList.get(i);
            // 获取当前车厢的可用座位列表
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber,
                    requestParam.getSeatType(), departure, arrival);

            // 初始化座位状态二维数组（商务座 2排3列）
            int[][] actualSeats = new int[2][3];
            // 构建座位状态矩阵，0表示可用，1表示已售
            for (int j = 1; j < 3; j++) {
                for (int k = 1; k < 4; k++) {
                    // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                    actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(0, k)) ? 0
                            : 1;
                }
            }

            // 尝试使用邻座算法选择座位
            int[][] select = SeatSelection.adjacent(passengerSeatDetails.size(), actualSeats);
            if (select != null) {
                // 邻座算法成功，记录选择的座位
                carriagesNumberSeatsMap.put(carriagesNumber, select);
                break;
            }

            // 计算当前车厢的可用座位数量（用于降级分配）
            int demotionStockNum = 0;
            for (int[] actualSeat : actualSeats) {
                for (int i1 : actualSeat) {
                    if (i1 == 0) {
                        demotionStockNum++;
                    }
                }
            }
            // 记录车厢的可用座位数和座位状态
            demotionStockNumMap.putIfAbsent(carriagesNumber, demotionStockNum);
            actualSeatsMap.putIfAbsent(carriagesNumber, actualSeats);

            // 如果不是最后一个车厢，继续检查下一车厢
            if (i < trainStationCarriageRemainingTicket.size() - 1) {
                continue;
            }

            // 如果邻座算法无法匹配，尝试对用户进行降级分配：同车厢不邻座
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                String carriagesNumberBack = entry.getKey();
                int demotionStockNumBack = entry.getValue();
                // 如果当前车厢可用座位数大于乘客数
                if (demotionStockNumBack > passengerSeatDetails.size()) {
                    int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                    // 尝试分配不相邻的座位
                    int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(passengerSeatDetails.size(), seats);
                    if (Objects.equals(nonAdjacentSeats.length, passengerSeatDetails.size())) {
                        select = nonAdjacentSeats;
                        carriagesNumberSeatsMap.put(carriagesNumberBack, select);
                        break;
                    }
                }
            }

            // 如果同车厢也已无法匹配，则对用户座位再次降级：不同车厢不邻座
            if (Objects.isNull(select)) {
                for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                    String carriagesNumberBack = entry.getKey();
                    int demotionStockNumBack = entry.getValue();
                    int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                    // 获取当前车厢所有可用的不相邻座位
                    int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(demotionStockNumBack, seats);
                    carriagesNumberSeatsMap.put(entry.getKey(), nonAdjacentSeats);
                }
            }
        }

        // 乘车人员在单一车厢座位不满足，触发乘车人分布在不同车厢
        // 计算所有已分配座位的总数
        int count = (int) carriagesNumberSeatsMap.values().stream()
                .flatMap(Arrays::stream)
                .count();

        // 如果有座位分配结果且数量匹配乘客数，则生成最终的座位分配
        if (CollUtil.isNotEmpty(carriagesNumberSeatsMap) && passengerSeatDetails.size() == count) {
            int countNum = 0;
            // 遍历每个车厢的座位分配
            for (Map.Entry<String, int[][]> entry : carriagesNumberSeatsMap.entrySet()) {
                List<String> selectSeats = new ArrayList<>();
                // 将座位坐标转换为标准座位号
                for (int[] ints : entry.getValue()) {
                    selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(0, ints[1]));
                }
                // 为每位乘客分配座位
                for (String selectSeat : selectSeats) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum++);
                    result.setSeatNumber(selectSeat);
                    result.setSeatType(currentTicketPassenger.getSeatType());
                    result.setCarriageNumber(entry.getKey());
                    result.setPassengerId(currentTicketPassenger.getPassengerId());
                    actualResult.add(result);
                }
            }
        }

        // 返回最终的座位分配结果
        return actualResult;
    }

    /**
     * 复杂座位选择方法 - 处理多人购买商务座的复杂场景
     * 实现分批邻座分配和多级降级策略，优先确保乘客尽量坐在一起
     *
     * @param requestParam 请求参数，包含列车信息、乘客信息和选座要求
     * @param trainCarriageList 列车车厢列表
     * @param trainStationCarriageRemainingTicket 各车厢剩余票数量
     * @return 座位分配结果列表
     */
    private List<TrainPurchaseTicketRespDTO> selectComplexSeats(SelectSeatDTO requestParam, List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
        // 获取列车信息
        String trainId = requestParam.getRequestParam().getTrainId();
        String departure = requestParam.getRequestParam().getDeparture();
        String arrival = requestParam.getRequestParam().getArrival();
        // 获取乘客信息列表
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();
        // 初始化结果列表
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        // 存储各车厢降级分配的可用座位数
        Map<String, Integer> demotionStockNumMap = new LinkedHashMap<>();
        // 存储各车厢的实际座位状态
        Map<String, int[][]> actualSeatsMap = new HashMap<>();
        // 存储已选择的座位信息
        Map<String, int[][]> carriagesNumberSeatsMap = new HashMap<>();
        String carriagesNumber;
        // 多人分配同一车厢邻座 - 使用分批处理策略
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            carriagesNumber = trainCarriageList.get(i);
            // 获取当前车厢的可用座位列表
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber,
                    requestParam.getSeatType(), departure, arrival);

            // 初始化座位状态二维数组（商务座 2排3列）
            int[][] actualSeats = new int[2][3];
            // 构建座位状态矩阵，0表示可用，1表示已售
            for (int j = 1; j < 3; j++) {
                for (int k = 1; k < 4; k++) {
                    // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                    actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(0, k)) ? 0
                            : 1;
                }
            }
            // 深拷贝座位状态矩阵，用于后续分批选择座位（避免修改原始矩阵）
            int[][] actualSeatsTranscript = deepCopy(actualSeats);
            // 存储每批次选择的座位
            List<int[][]> actualSelects = new ArrayList<>();

            // 将乘客列表分批，每批最多2人（因为商务座每排最多3个座位，便于邻座分配）
            List<List<PurchaseTicketPassengerDetailDTO>> splitPassengerSeatDetails = ListUtil
                    .split(passengerSeatDetails, 2);

            // 逐批次尝试为乘客分配邻座
            for (List<PurchaseTicketPassengerDetailDTO> each : splitPassengerSeatDetails) {
                // 尝试为当前批次乘客分配邻座
                int[][] select = SeatSelection.adjacent(each.size(), actualSeatsTranscript);
                if (select != null) {
                    // 标记已分配座位为已售（更新座位状态矩阵）
                    for (int[] ints : select) {
                        actualSeatsTranscript[ints[0] - 1][ints[1] - 1] = 1;
                    }
                    // 记录当前批次选择的座位
                    actualSelects.add(select);
                }
            }
            // 如果所有批次都成功分配到座位
            if (actualSelects.size() == splitPassengerSeatDetails.size()) {
                int[][] actualSelect = null;
                // 合并所有批次选择的座位到一个数组中
                for (int j = 0; j < actualSelects.size(); j++) {
                    if (j == 0) {
                        actualSelect = mergeArrays(actualSelects.get(j), actualSelects.get(j + 1));
                    }
                    if (j != 0 && actualSelects.size() > 2) {
                        actualSelect = mergeArrays(actualSelect, actualSelects.get(j + 1));
                    }
                }
                // 记录该车厢的座位分配结果
                carriagesNumberSeatsMap.put(carriagesNumber, actualSelect);
                break;
            }
            // 计算当前车厢的可用座位数量（用于降级分配策略）
            int demotionStockNum = 0;
            for (int[] actualSeat : actualSeats) {
                for (int i1 : actualSeat) {
                    if (i1 == 0) { // 0表示座位可用
                        demotionStockNum++;
                    }
                }
            }
            // 记录车厢的可用座位数和座位状态（用于后续降级分配）
            demotionStockNumMap.putIfAbsent(carriagesNumber, demotionStockNum);
            actualSeatsMap.putIfAbsent(carriagesNumber, actualSeats);
        }
        // 第一级降级策略：如果邻座算法无法匹配，尝试对用户进行降级分配：同车厢不邻座
        if (CollUtil.isEmpty(carriagesNumberSeatsMap)) {
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                String carriagesNumberBack = entry.getKey();
                int demotionStockNumBack = entry.getValue();
                // 如果当前车厢可用座位数大于乘客数
                if (demotionStockNumBack > passengerSeatDetails.size()) {
                    int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                    // 尝试分配不相邻的座位
                    int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(passengerSeatDetails.size(), seats);
                    // 验证是否成功分配了所有乘客的座位
                    if (Objects.equals(nonAdjacentSeats.length, passengerSeatDetails.size())) {
                        carriagesNumberSeatsMap.put(carriagesNumberBack, nonAdjacentSeats);
                        break;
                    }
                }
            }
        }
        // 第二级降级策略：如果同车厢也已无法匹配，则对用户座位再次降级：不同车厢不邻座
        if (CollUtil.isEmpty(carriagesNumberSeatsMap)) {
            // 记录未分配的乘客数量
            int undistributedPassengerSize = passengerSeatDetails.size();
            // 遍历各车厢，为未分配的乘客分配座位
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                String carriagesNumberBack = entry.getKey();
                int demotionStockNumBack = entry.getValue();
                int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                // 分配当前车厢可容纳的未分配乘客数
                int[][] nonAdjacentSeats = SeatSelection
                        .nonAdjacent(Math.min(undistributedPassengerSize, demotionStockNumBack), seats);
                // 更新未分配乘客数量
                undistributedPassengerSize = undistributedPassengerSize - demotionStockNumBack;
                // 记录当前车厢的座位分配
                carriagesNumberSeatsMap.put(entry.getKey(), nonAdjacentSeats);
            }
        }
        // 乘车人员在单一车厢座位不满足，触发乘车人分布在不同车厢
        // 计算所有已分配座位的总数
        int count = (int) carriagesNumberSeatsMap.values().stream()
                .flatMap(Arrays::stream)
                .count();
        // 如果有座位分配结果且数量匹配乘客数，则生成最终的座位分配
        if (CollUtil.isNotEmpty(carriagesNumberSeatsMap) && passengerSeatDetails.size() == count) {
            int countNum = 0;
            // 遍历每个车厢的座位分配
            for (Map.Entry<String, int[][]> entry : carriagesNumberSeatsMap.entrySet()) {
                List<String> selectSeats = new ArrayList<>();
                // 将座位坐标转换为标准座位号
                for (int[] ints : entry.getValue()) {
                    selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(0, ints[1]));
                }
                // 为每位乘客分配座位
                for (String selectSeat : selectSeats) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum++);
                    result.setSeatNumber(selectSeat);
                    result.setSeatType(currentTicketPassenger.getSeatType());
                    result.setCarriageNumber(entry.getKey());
                    result.setPassengerId(currentTicketPassenger.getPassengerId());
                    actualResult.add(result);
                }
            }
        }
        return actualResult;
    }

    /**
     * 合并两个二维数组
     * 将两个座位数组合并为一个，用于整合多批次选择的座位
     *
     * @param array1 第一个二维数组
     * @param array2 第二个二维数组
     * @return 合并后的二维数组
     */
    public static int[][] mergeArrays(int[][] array1, int[][] array2) {
        List<int[]> list = new ArrayList<>(Arrays.asList(array1));
        list.addAll(Arrays.asList(array2));
        return list.toArray(new int[0][]);
    }

    /**
     * 深拷贝二维数组
     * 创建原始座位状态数组的完全副本，避免修改原始数据
     *
     * @param originalArray 原始二维数组
     * @return 深拷贝后的二维数组
     */
    public static int[][] deepCopy(int[][] originalArray) {
        int[][] copy = new int[originalArray.length][originalArray[0].length];
        for (int i = 0; i < originalArray.length; i++) {
            System.arraycopy(originalArray[i], 0, copy[i], 0, originalArray[i].length);
        }
        return copy;
    }
}
