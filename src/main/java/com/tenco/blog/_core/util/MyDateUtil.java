package com.tenco.blog._core.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.sql.Date;
import java.sql.Timestamp;

// SRP - 단일 책임 원칙
// 날짜/시간 관련된 유틸리티 클래스
public class MyDateUtil {

    // 1. TimeStamp 포메터
    public static String timestampFormat(Timestamp timestamp) {
        // TimeStamp --> Date 형태로 변환
        Date currentDate = new Date(timestamp.getTime());
        return DateFormatUtils.format(currentDate, "yyyy-MM-dd HH:mm");
    }
}
