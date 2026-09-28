package com.guoxu.ticketservice.toolkit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * DateUtil
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:34
 **/
@Slf4j
public final class DateUtil {

    /**
     * 计算小时差
     *
     * @param startTime 开始时间 2022-10-01 00:00:00
     * @param endTime   结束时间 2022-10-01 12:23:00
     * @return 12:23
     */
    public static String calculateHourDifference(Date startTime, Date endTime) {
        LocalDateTime startDateTime = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime endDateTime = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        Duration duration = Duration.between(startDateTime, endDateTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * 日期转换为列车行驶开始时间和结束时间
     *
     * @param date    时间
     * @param pattern 日期格式
     * @return 日期格式对应的时间
     */
    public static String convertDateToLocalTime(Date date, String pattern) {
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(pattern);
        return localDateTime.format(outputFormatter);
    }

    @SneakyThrows
    public static void main(String[] args) {
        String startTimeStr = "2022-10-01 01:00:00";
        String endTimeStr = "2022-10-01 12:23:00";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = formatter.parse(startTimeStr);
        Date endTime = formatter.parse(endTimeStr);
        String calculateHourDifference = calculateHourDifference(startTime, endTime);
        log.info("开始时间：{}，结束时间：{}，两个时间相差时分：{}", startTimeStr, endTimeStr, calculateHourDifference);
    }
}
