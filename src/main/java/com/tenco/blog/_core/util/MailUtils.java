package com.tenco.blog._core.util;

import java.util.Random;

/**
 * 메일 발송 관련 유틸리티 클래스
 *
 * 인증번호 생성 기능을 제공합니다.
 *
 * 핵심 개념
 * 1) 유틸리티 클래스: 정적 메서드만 제공하는 클래스
 * 2) 랜덤 숫자 생성: Random 클래스 사용
 * 3) 범위 설정: 100000 ~ 999999 (6자리 숫자 보장)
 */
public class MailUtils {

    /**
     * 6자리 랜덤 숫자 문자열을 생성한다.
     * 예) "123456"
     *
     * 왜 100_000 부터 시작? 앞자리에 0이 붙으면 6자리가 안 되니까.
     */
    public static String generateRandomCode() {
        Random random = new Random();
        // 100000 ~ 999999 사이의 6자리 숫자 보장
        int code = 100_000 + random.nextInt(900_000);
        return String.valueOf(code);
    }
}
