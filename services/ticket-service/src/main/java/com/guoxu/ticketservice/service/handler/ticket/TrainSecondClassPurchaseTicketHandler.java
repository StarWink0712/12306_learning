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
import com.guoxu.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.guoxu.ticketservice.service.handler.ticket.select.SeatSelection;
import com.guoxu.ticketservice.toolkit.SeatNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TrainSecondClassPurchaseTicketHandler
 *
 * @author 执笔画棠
 * @date 2025/11/13 20:24
 **/
@Component
@RequiredArgsConstructor
public class TrainSecondClassPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {

    // 座位服务，用于查询座位相关的业务数据，比如哪些座位可用、余票数量等
    private final SeatService seatService;

    // 定义座位编号字母与数组索引的映射关系：
    // 二等座座位号通常为 01A ~ 09E, 10A ~ 18E（但这里映射到 0~4，即 A->0, B->1, C->2, D->3, F->4）
    private static final Map<Character, Integer> SEAT_Y_INT = Map.of('A', 0, 'B', 1, 'C', 2, 'D', 3, 'F', 4);

    // 重写父类方法，返回当前处理器对应的车辆类型和座位类型标识，用于区分不同的购票模板
    @Override
    public String mark() {
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.SECOND_CLASS.getName();
    }

    // 重写父类选座核心方法，根据传入的请求参数为乘客选择合适的二等座
    @Override
    protected List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam) {
        // 获取车次ID
        String trainId = requestParam.getRequestParam().getTrainId();
        // 获取出发站
        String departure = requestParam.getRequestParam().getDeparture();
        // 获取到达站
        String arrival = requestParam.getRequestParam().getArrival();
        // 获取乘客及座位需求详情列表
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();
        // 查询该车次、二等座类型、出发到达站下可用的车厢号列表
        List<String> trainCarriageList = seatService.listUsableCarriageNumber(trainId, requestParam.getSeatType(),
                departure, arrival);
        // 查询这些车厢中的剩余二等座总数
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(trainId, departure,
                arrival, trainCarriageList);
        // 计算所有车厢剩余座位总数
        int remainingTicketSum = trainStationCarriageRemainingTicket.stream().mapToInt(Integer::intValue).sum();
        // 如果总的余票数比乘客人数还少，抛出异常，提示用户更换座位类型或车次
        if (remainingTicketSum < passengerSeatDetails.size()) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
        }
        // 如果乘客人数少于6人
        if (passengerSeatDetails.size() < 6) {
            // 如果用户指定了想坐的座位
            if (CollUtil.isNotEmpty(requestParam.getRequestParam().getChooseSeats())) {
                // 尝试查找与用户指定座位匹配的结果
                return findMatchSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket).getKey();
            }
            // 如果没有指定座位，则执行常规选座逻辑（优先分配相邻座位）
            return selectSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
        } else {
            // 如果乘客人数大于等于6人
            // 如果用户指定了想坐的座位
            if (CollUtil.isNotEmpty(requestParam.getRequestParam().getChooseSeats())) {
                // 尝试匹配用户指定座位
                return findMatchSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket).getKey();
            }
            // 如果没有指定，则执行更复杂的选座逻辑（可能涉及跨车厢、降级分配等）
            return selectComplexSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
        }
    }

    // 计算用户选择的座位相对于第一个座位的偏移量，用于判断是否可能连座
    private List<Pair<Integer, Integer>> calcChooseSeatLevelPairList(int[][] actualSeats, List<String> chooseSeatList) {
        // 解析第一个座位，获取其所在行(x)和列(y)
        String firstChooseSeat = chooseSeatList.get(0);
        int firstSeatX = Integer.parseInt(firstChooseSeat.substring(1));
        int firstSeatY = SEAT_Y_INT.get(firstChooseSeat.charAt(0));
        // 用于存储每个选择座位相对于第一个座位的偏移
        List<Pair<Integer, Integer>> chooseSeatLevelPairList = new ArrayList<>();
        // 先将第一个座位相对自身的偏移加入（即 (0,0)）
        chooseSeatLevelPairList.add(new Pair<>(firstSeatX, firstSeatY));
        // 记录最小行偏移
        int minLevelX = 0;
        // 遍历其他选择的座位
        for (int i = 1; i < chooseSeatList.size(); i++) {
            String chooseSeat = chooseSeatList.get(i);
            int chooseSeatX = Integer.parseInt(chooseSeat.substring(1));
            int chooseSeatY = SEAT_Y_INT.get(chooseSeat.charAt(0));
            // 计算当前座位与第一个座位在行方向的偏移，并更新最小偏移值
            minLevelX = Math.min(minLevelX, chooseSeatX - firstSeatX);
            // 将当前座位的相对偏移加入列表
            chooseSeatLevelPairList.add(new Pair<>(chooseSeatX - firstSeatX, chooseSeatY - firstSeatY));
        }
        // 遍历可能的补偿行，尝试找到一组连续可用的座位
        for (int i = Math.abs(minLevelX); i < 18; i++) {
            // 用于存放最终可以分配的座位坐标
            List<Pair<Integer, Integer>> sureSeatList = new ArrayList<>();
            // 检查补偿后第一个座位所在位置是否空闲
            if (actualSeats[i][firstSeatY] == 0) {
                // 如果空闲，将其作为第一个座位加入结果
                sureSeatList.add(new Pair<>(i, firstSeatY));
                // 遍历其余相对偏移的座位
                for (int j = 1; j < chooseSeatList.size(); j++) {
                    Pair<Integer, Integer> pair = chooseSeatLevelPairList.get(j);
                    int chooseSeatX = pair.getKey();
                    int chooseSeatY = pair.getValue();
                    int x = i + chooseSeatX;
                    // 如果超出最大行数，返回空列表
                    if (x >= 18) {
                        return Collections.emptyList();
                    }
                    // 判断该位置是否空闲
                    if (actualSeats[i + chooseSeatX][firstSeatY + chooseSeatY] == 0) {
                        // 空闲则加入结果
                        sureSeatList.add(new Pair<>(i + chooseSeatX, firstSeatY + chooseSeatY));
                    } else {
                        // 不空闲则终止，该组合不可行
                        break;
                    }
                }
            }
            // 如果最终找到的座位数量与乘客人数一致，则返回该组座位
            if (sureSeatList.size() == chooseSeatList.size()) {
                return sureSeatList;
            }
        }
        // 找不到合适的组合则返回空
        return Collections.emptyList();
    }

    // 查找用户指定座位或尽量分配相邻座位（适用于乘客数较少的情况）
    private Pair<List<TrainPurchaseTicketRespDTO>, Boolean> findMatchSeats(SelectSeatDTO requestParam,
                                                                           List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
        // 构建包含乘客与座位信息的基础数据对象
        TrainSeatBaseDTO trainSeatBaseDTO = buildTrainSeatBaseDTO(requestParam);
        // 用于存放最终分配的座位结果
        List<TrainPurchaseTicketRespDTO> actualResult = Lists
                .newArrayListWithCapacity(trainSeatBaseDTO.getPassengerSeatDetails().size());
        // 用于记录每个车厢中的可用座位坐标
        HashMap<String, List<Pair<Integer, Integer>>> carriagesSeatMap = new HashMap<>(16);
        // 总的乘客人数
        int passengersNumber = trainSeatBaseDTO.getPassengerSeatDetails().size();
        // 遍历每一个可用车厢
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            String carriagesNumber = trainCarriageList.get(i);
            // 查询该车厢中当前车次、座位类型、出发到达站下的可用座位
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainSeatBaseDTO.getTrainId(),
                    carriagesNumber, requestParam.getSeatType(), trainSeatBaseDTO.getDeparture(),
                    trainSeatBaseDTO.getArrival());
            // 构建一个18行（排）5列（座位）的二维数组，表示该车厢的座位占用情况，0表示空闲，1表示已占用
            int[][] actualSeats = new int[18][5];
            // 用于记录该车厢中当前可用的空座位坐标
            List<Pair<Integer, Integer>> carriagesVacantSeat = new ArrayList<>();
            // 遍历车厢中的每个座位（行1~18，列1~5）
            for (int j = 1; j < 19; j++) {
                for (int k = 1; k < 6; k++) {
                    // 根据行号判断座位编号格式，构造如 "01A" 或 "10A" 的字符串，并检查是否在可用列表中
                    if (j <= 9) {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(2, k))
                                ? 0
                                : 1;
                    } else {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("" + j + SeatNumberUtil.convert(2, k))
                                ? 0
                                : 1;
                    }
                    // 如果空闲，则记录该座位坐标
                    if (actualSeats[j - 1][k - 1] == 0) {
                        carriagesVacantSeat.add(new Pair<>(j - 1, k - 1));
                    }
                }
            }
            // 用于存放最终选中的座位编号
            List<String> selectSeats = new ArrayList<>(passengersNumber);
            // 尝试基于用户指定的座位计算相对偏移，看是否能连座
            List<Pair<Integer, Integer>> sureSeatList = calcChooseSeatLevelPairList(actualSeats,
                    trainSeatBaseDTO.getChooseSeatList());
            // 如果找到了连座组合且该车厢的空闲座位数足够
            if (CollUtil.isNotEmpty(sureSeatList) && carriagesVacantSeat.size() >= passengersNumber) {
                List<Pair<Integer, Integer>> vacantSeatList = new ArrayList<>();
                // 如果连座数少于乘客数，先将连座部分标记为已占用，再从剩余空座位中补充
                if (sureSeatList.size() != passengersNumber) {
                    for (int i1 = 0; i1 < sureSeatList.size(); i1++) {
                        Pair<Integer, Integer> pair = sureSeatList.get(i1);
                        actualSeats[pair.getKey()][pair.getValue()] = 1;
                    }
                    // 收集剩余的空闲座位
                    for (int i1 = 0; i1 < 18; i1++) {
                        for (int j = 0; j < 5; j++) {
                            if (actualSeats[i1][j] == 0) {
                                vacantSeatList.add(new Pair<>(i1, j));
                            }
                        }
                    }
                    // 计算还需要分配的座位数
                    int needSeatSize = passengersNumber - sureSeatList.size();
                    // 补充缺少的座位
                    sureSeatList.addAll(vacantSeatList.subList(0, needSeatSize));
                }
                // 构造最终的座位编号字符串，并生成返回的DTO对象
                for (Pair<Integer, Integer> each : sureSeatList) {
                    if (each.getKey() <= 8) {
                        selectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(2, each.getValue() + 1));
                    } else {
                        selectSeats.add("" + (each.getKey() + 1) + SeatNumberUtil.convert(2, each.getValue() + 1));
                    }
                }
                AtomicInteger countNum = new AtomicInteger(0);
                for (String selectSeat : selectSeats) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = trainSeatBaseDTO.getPassengerSeatDetails()
                            .get(countNum.getAndIncrement());
                    result.setSeatNumber(selectSeat);
                    result.setSeatType(currentTicketPassenger.getSeatType());
                    result.setCarriageNumber(carriagesNumber);
                    result.setPassengerId(currentTicketPassenger.getPassengerId());
                    actualResult.add(result);
                }
                // 返回分配结果以及成功标志
                return new Pair<>(actualResult, Boolean.TRUE);
            } else {
                // 如果没有找到连座组合或者空位不够，但仍有一些空闲座位
                if (CollUtil.isNotEmpty(carriagesVacantSeat)) {
                    // 记录该车厢的空闲座位
                    carriagesSeatMap.put(carriagesNumber, carriagesVacantSeat);
                    // 如果已经是最后一个车厢
                    if (i == trainStationCarriageRemainingTicket.size() - 1) {
                        // 尝试找到一个空闲座位数足够的车厢
                        Pair<String, List<Pair<Integer, Integer>>> findSureCarriageSeat = null;
                        for (Map.Entry<String, List<Pair<Integer, Integer>>> entry : carriagesSeatMap.entrySet()) {
                            if (entry.getValue().size() >= passengersNumber) {
                                findSureCarriageSeat = new Pair<>(entry.getKey(),
                                        entry.getValue().subList(0, passengersNumber));
                                break;
                            }
                        }
                        // 如果找到，则分配这些座位
                        if (findSureCarriageSeat != null) {
                            for (Pair<Integer, Integer> each : findSureCarriageSeat.getValue()) {
                                if (each.getKey() <= 8) {
                                    selectSeats.add(
                                            "0" + (each.getKey() + 1) + SeatNumberUtil.convert(2, each.getValue() + 1));
                                } else {
                                    selectSeats.add(
                                            "" + (each.getKey() + 1) + SeatNumberUtil.convert(2, each.getValue() + 1));
                                }
                            }
                            AtomicInteger countNum = new AtomicInteger(0);
                            for (String selectSeat : selectSeats) {
                                TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                                PurchaseTicketPassengerDetailDTO currentTicketPassenger = trainSeatBaseDTO
                                        .getPassengerSeatDetails().get(countNum.getAndIncrement());
                                result.setSeatNumber(selectSeat);
                                result.setSeatType(currentTicketPassenger.getSeatType());
                                result.setCarriageNumber(findSureCarriageSeat.getKey());
                                result.setPassengerId(currentTicketPassenger.getPassengerId());
                                actualResult.add(result);
                            }
                            return new Pair<>(actualResult, Boolean.TRUE);
                        } else {
                            // 如果没有一个车厢的座位完全够，但有多个车厢有部分座位，尝试分配
                            int sureSeatListSize = 0;
                            AtomicInteger countNum = new AtomicInteger(0);
                            for (Map.Entry<String, List<Pair<Integer, Integer>>> entry : carriagesSeatMap.entrySet()) {
                                if (sureSeatListSize < passengersNumber) {
                                    if (sureSeatListSize + entry.getValue().size() < passengersNumber) {
                                        sureSeatListSize = sureSeatListSize + entry.getValue().size();
                                        List<String> actualSelectSeats = new ArrayList<>();
                                        for (Pair<Integer, Integer> each : entry.getValue()) {
                                            if (each.getKey() <= 8) {
                                                actualSelectSeats.add("0" + (each.getKey() + 1)
                                                        + SeatNumberUtil.convert(2, each.getValue() + 1));
                                            } else {
                                                actualSelectSeats.add("" + (each.getKey() + 1)
                                                        + SeatNumberUtil.convert(2, each.getValue() + 1));
                                            }
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
                                                if (each.getKey() <= 8) {
                                                    actualSelectSeats.add("0" + (each.getKey() + 1)
                                                            + SeatNumberUtil.convert(2, each.getValue() + 1));
                                                } else {
                                                    actualSelectSeats.add("" + (each.getKey() + 1)
                                                            + SeatNumberUtil.convert(2, each.getValue() + 1));
                                                }
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
                            return new Pair<>(actualResult, Boolean.TRUE);
                        }
                    }
                }
            }
        }
        // 如果最终没有分配成功，返回null和失败标志
        return new Pair<>(null, Boolean.FALSE);
    }

    // 普通选座逻辑：优先分配相邻座位，其次降级为不邻座但同车厢，最后为不邻座不同车厢
    private List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam, List<String> trainCarriageList,
                                                         List<Integer> trainStationCarriageRemainingTicket) {
        // 基础信息获取
        String trainId = requestParam.getRequestParam().getTrainId();
        String departure = requestParam.getRequestParam().getDeparture();
        String arrival = requestParam.getRequestParam().getArrival();
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();
        // 用于存放最终的分配结果
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        // 记录每个车厢可降级分配的余票数
        Map<String, Integer> demotionStockNumMap = new LinkedHashMap<>();
        // 记录每个车厢的座位二维数组
        Map<String, int[][]> actualSeatsMap = new HashMap<>();
        // 记录每个车厢的座位二维数组（用于后续降级分配）
        Map<String, int[][]> carriagesNumberSeatsMap = new HashMap<>();
        String carriagesNumber;
        // 遍历每个可用车厢
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            carriagesNumber = trainCarriageList.get(i);
            // 查询该车厢可用座位
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber,
                    requestParam.getSeatType(), departure, arrival);
            // 构建座位占用二维数组
            int[][] actualSeats = new int[18][5];
            for (int j = 1; j < 19; j++) {
                for (int k = 1; k < 6; k++) {
                    // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                    if (j <= 9) {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(2, k))
                                ? 0
                                : 1;
                    } else {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("" + j + SeatNumberUtil.convert(2, k))
                                ? 0
                                : 1;
                    }
                }
            }
            // 尝试使用邻座算法为所有乘客分配相邻座位
            int[][] select = SeatSelection.adjacent(passengerSeatDetails.size(), actualSeats);
            if (select != null) {
                // 如果成功，记录该车厢与座位选择，并退出循环
                carriagesNumberSeatsMap.put(carriagesNumber, select);
                break;
            }
            // 统计该车厢的空闲座位数
            int demotionStockNum = 0;
            for (int[] actualSeat : actualSeats) {
                for (int i1 : actualSeat) {
                    if (i1 == 0) {
                        demotionStockNum++;
                    }
                }
            }
            // 记录该车厢的可降级余票数
            demotionStockNumMap.putIfAbsent(carriagesNumber, demotionStockNum);
            // 记录该车厢的座位状态
            actualSeatsMap.putIfAbsent(carriagesNumber, actualSeats);
            // 如果不是最后一个车厢，继续检查下一个
            if (i < trainStationCarriageRemainingTicket.size() - 1) {
                continue;
            }
            // 如果邻座算法失败，尝试为所有乘客分配同车厢不邻座座位
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                String carriagesNumberBack = entry.getKey();
                int demotionStockNumBack = entry.getValue();
                if (demotionStockNumBack > passengerSeatDetails.size()) {
                    int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                    int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(passengerSeatDetails.size(), seats);
                    if (Objects.equals(nonAdjacentSeats.length, passengerSeatDetails.size())) {
                        select = nonAdjacentSeats;
                        carriagesNumberSeatsMap.put(carriagesNumberBack, select);
                        break;
                    }
                }
            }
            // 如果同车厢不邻座也失败，尝试为乘客分配不同车厢的不邻座座位
            if (Objects.isNull(select)) {
                for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                    String carriagesNumberBack = entry.getKey();
                    int demotionStockNumBack = entry.getValue();
                    int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                    int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(demotionStockNumBack, seats);
                    carriagesNumberSeatsMap.put(entry.getKey(), nonAdjacentSeats);
                }
            }
        }
        // 如果多个车厢的座位合起来刚好满足所有乘客
        if (CollUtil.isNotEmpty(carriagesNumberSeatsMap)
                && passengerSeatDetails.size() == (int) carriagesNumberSeatsMap.values().stream()
                .flatMap(Arrays::stream)
                .count()) {
            int countNum = 0;
            // 遍历每个车厢的座位分配结果，构造返回的DTO
            for (Map.Entry<String, int[][]> entry : carriagesNumberSeatsMap.entrySet()) {
                List<String> selectSeats = new ArrayList<>();
                for (int[] ints : entry.getValue()) {
                    if (ints[0] <= 9) {
                        selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(2, ints[1]));
                    } else {
                        selectSeats.add("" + ints[0] + SeatNumberUtil.convert(2, ints[1]));
                    }
                }
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

    // 复杂选座逻辑：主要用于乘客较多时的分配，可能分多组邻座或降级分配
    private List<TrainPurchaseTicketRespDTO> selectComplexSeats(SelectSeatDTO requestParam,
                                                                List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
        // 基础信息获取
        String trainId = requestParam.getRequestParam().getTrainId();
        String departure = requestParam.getRequestParam().getDeparture();
        String arrival = requestParam.getRequestParam().getArrival();
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();
        // 结果列表
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        // 记录每个车厢的可降级余票数
        Map<String, Integer> demotionStockNumMap = new LinkedHashMap<>();
        // 记录每个车厢的座位状态
        Map<String, int[][]> actualSeatsMap = new HashMap<>();
        // 记录每个车厢分配的座位二维数组
        Map<String, int[][]> carriagesNumberSeatsMap = new HashMap<>();
        String carriagesNumber;
        // 尝试为乘客分组（每组最多3人）分配同一车厢的邻座
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            carriagesNumber = trainCarriageList.get(i);
            // 查询可用座位
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber,
                    requestParam.getSeatType(), departure, arrival);
            // 构建座位二维数组
            int[][] actualSeats = new int[18][5];
            for (int j = 1; j < 19; j++) {
                for (int k = 1; k < 6; k++) {
                    // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                    if (j <= 9) {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(2, k))
                                ? 0
                                : 1;
                    } else {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("" + j + SeatNumberUtil.convert(2, k))
                                ? 0
                                : 1;
                    }
                }
            }
            // 深拷贝一份座位状态，避免修改原数组
            int[][] actualSeatsTranscript = deepCopy(actualSeats);
            // 用于存放不同乘客组的座位分配结果
            List<int[][]> actualSelects = new ArrayList<>();
            // 将乘客分成多个小组，每组最多3人
            List<List<PurchaseTicketPassengerDetailDTO>> splitPassengerSeatDetails = ListUtil
                    .split(passengerSeatDetails, 3);
            // 遍历每个乘客组，尝试分配邻座
            for (List<PurchaseTicketPassengerDetailDTO> each : splitPassengerSeatDetails) {
                int[][] select = SeatSelection.adjacent(each.size(), actualSeatsTranscript);
                if (select != null) {
                    // 如果成功分配，将对应座位标记为已占用，并记录该组分配结果
                    for (int[] ints : select) {
                        actualSeatsTranscript[ints[0] - 1][ints[1] - 1] = 1;
                    }
                    actualSelects.add(select);
                }
            }
            // 如果所有组都成功分配到邻座
            if (actualSelects.size() == splitPassengerSeatDetails.size()) {
                int[][] actualSelect = null;
                // 合并所有组的座位分配结果为一个大的二维数组
                for (int j = 0; j < actualSelects.size(); j++) {
                    if (j == 0) {
                        actualSelect = mergeArrays(actualSelects.get(j), actualSelects.get(j + 1));
                    }
                    if (j != 0 && actualSelects.size() > 2) {
                        actualSelect = mergeArrays(actualSelect, actualSelects.get(j + 1));
                    }
                }
                // 记录该车厢的合并分配结果
                carriagesNumberSeatsMap.put(carriagesNumber, actualSelect);
                break;
            }
            // 统计该车厢剩余空闲座位数
            int demotionStockNum = 0;
            for (int[] actualSeat : actualSeats) {
                for (int i1 : actualSeat) {
                    if (i1 == 0) {
                        demotionStockNum++;
                    }
                }
            }
            // 记录该车厢的可降级余票数
            demotionStockNumMap.putIfAbsent(carriagesNumber, demotionStockNum);
            // 记录该车厢的座位状态
            actualSeatsMap.putIfAbsent(carriagesNumber, actualSeats);
        }
        // 如果邻座分配失败，尝试为所有乘客分配同车厢不邻座座位
        if (CollUtil.isEmpty(carriagesNumberSeatsMap)) {
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                String carriagesNumberBack = entry.getKey();
                int demotionStockNumBack = entry.getValue();
                if (demotionStockNumBack > passengerSeatDetails.size()) {
                    int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                    int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(passengerSeatDetails.size(), seats);
                    if (Objects.equals(nonAdjacentSeats.length, passengerSeatDetails.size())) {
                        carriagesNumberSeatsMap.put(carriagesNumberBack, nonAdjacentSeats);
                        break;
                    }
                }
            }
        }
        // 如果同车厢不邻座也失败，尝试分配不同车厢的不邻座座位
        if (CollUtil.isEmpty(carriagesNumberSeatsMap)) {
            int undistributedPassengerSize = passengerSeatDetails.size();
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                String carriagesNumberBack = entry.getKey();
                int demotionStockNumBack = entry.getValue();
                int[][] seats = actualSeatsMap.get(carriagesNumberBack);
                // 只分配需要的座位数，避免浪费
                int[][] nonAdjacentSeats = SeatSelection
                        .nonAdjacent(Math.min(undistributedPassengerSize, demotionStockNumBack), seats);
                undistributedPassengerSize = undistributedPassengerSize - demotionStockNumBack;
                carriagesNumberSeatsMap.put(entry.getKey(), nonAdjacentSeats);
            }
        }
        // 如果多个车厢的座位合起来满足所有乘客
        if (CollUtil.isNotEmpty(carriagesNumberSeatsMap)
                && passengerSeatDetails.size() == (int) carriagesNumberSeatsMap.values().stream()
                .flatMap(Arrays::stream)
                .count()) {
            int countNum = 0;
            // 构造最终的座位分配结果
            for (Map.Entry<String, int[][]> entry : carriagesNumberSeatsMap.entrySet()) {
                List<String> selectSeats = new ArrayList<>();
                for (int[] ints : entry.getValue()) {
                    if (ints[0] <= 9) {
                        selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(2, ints[1]));
                    } else {
                        selectSeats.add("" + ints[0] + SeatNumberUtil.convert(2, ints[1]));
                    }
                }
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

    // 合并两个二维座位数组为一个
    public static int[][] mergeArrays(int[][] array1, int[][] array2) {
        List<int[]> list = new ArrayList<>(Arrays.asList(array1));
        list.addAll(Arrays.asList(array2));
        return list.toArray(new int[0][]);
    }

    // 深拷贝一个二维整型数组
    public static int[][] deepCopy(int[][] originalArray) {
        int[][] copy = new int[originalArray.length][originalArray[0].length];
        for (int i = 0; i < originalArray.length; i++) {
            System.arraycopy(originalArray[i], 0, copy[i], 0, originalArray[i].length);
        }
        return copy;
    }
}