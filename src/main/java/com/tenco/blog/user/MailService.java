package com.tenco.blog.user;

import com.tenco.blog._core.util.MailUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 메일 발송 서비스
 *
 * 비즈니스 로직
 * 1) 인증번호 생성
 * 2) 이메일 발송
 * 3) 세션에 인증번호 저장 (검증용)
 *
 * 핵심 개념
 *  - JavaMailSender   : Spring Boot 가 제공하는 메일 발송 인터페이스
 *  - MimeMessage      : 이메일 메시지를 나타내는 객체 (HTML, 첨부파일 지원)
 *  - MimeMessageHelper: 이메일 작성 편의 클래스
 *  - HttpSession      : 인증번호와 "인증 완료 도장" 임시 저장용
 *
 * 주의: Service 레이어에서 HttpSession 을 사용하는 것은 학습 단계 단순화 목적이다.
 *       실무에서는 Redis 같은 인메모리 DB 를 쓴다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MailService {

    // Spring Boot 가 제공하는 메일 발송 객체
    // application-dev.yml 의 spring.mail.* 설정을 사용해 자동 구성된다.
    private final JavaMailSender javaMailSender;

    // 세션 객체
    // - 인증번호 저장(key: "code_" + email)
    // - 인증 완료 도장 저장(key: "verified_email")
    private final HttpSession session;

    /**
     * [기능 1] 인증번호 발송하기
     *
     * @param email 사용자가 입력한 이메일 주소
     */
    public void 인증번호발송(String email) {
        // 1) 인증번호 생성
        String code = MailUtils.generateRandomCode();
        log.info("생성된 인증번호: {}", code); // 개발용 로그

        // 2) 이메일 전송 내용 설정
        //    MimeMessage 는 HTML, 첨부파일 등을 포함할 수 있는 표준 포맷.
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            // 3) 도우미 객체(Helper) 생성
            //    true: 멀티파트(HTML, 파일 등) 사용 허용
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("[MyBlog] 회원가입 이메일 인증번호");
            helper.setText("<h3>인증번호는 [" + code + "] 입니다.</h3>", true);

            // 4) 메일 발송 (SMTP 서버와 통신)
            javaMailSender.send(message);

            // 5) 세션에 인증번호 저장
            //    Key 패턴: "code_" + 이메일
            //    → 동시 접속자가 많아도 이메일 주소로 누구의 인증번호인지 구별 가능
            session.setAttribute("code_" + email, code);
            log.info("인증번호 발송 완료: {}", code);

        } catch (MessagingException e) {
            // 메일 발송 실패 시 예외 처리
            //  - SMTP 서버 연결 실패
            //  - 인증 실패 (앱 비밀번호 오류 등)
            //  - 네트워크 오류
            log.error("메일 발송 실패", e);
            throw new IllegalArgumentException("메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * [기능 2] 인증번호 검증 메서드
     *
     * 핵심 변경 사항
     *   인증 성공 시 "verified_email" 키로 인증된 이메일 주소를 세션에 별도 저장한다.
     *   이 도장이 있어야만 나중에 회원가입 시 서버가 "정말 이 이메일이 인증되었구나"
     *   라고 신뢰할 수 있다. (브라우저 hidden 필드는 우회 가능하므로 신뢰 못 함)
     *
     * @param email 검증할 이메일 (Key 를 찾기 위해 필요)
     * @param code  사용자가 입력한 인증번호
     * @return true(일치), false(불일치)
     */
    public boolean 인증번호확인(String email, String code) {
        // 세션에서 저장된 코드 가져오기
        String savedCode = (String) session.getAttribute("code_" + email);

        if (savedCode != null && savedCode.equals(code)) {
            // 1) 인증 성공 시 코드는 세션에서 삭제 (일회용 — 재사용 방지)
            session.removeAttribute("code_" + email);

            // 2) "이 이메일은 인증 완료됨" 도장을 세션에 찍는다.
            //    - 회원가입 시점에 UserService 가 이 도장을 확인한다.
            //    - 브라우저 JS / hidden 필드가 아닌 서버 세션에 기록되므로 우회 불가.
            //    - 회원가입이 완료되면 UserService 에서 다시 제거한다(재사용 방지).
            session.setAttribute("verified_email", email);

            return true;
        }
        return false;
    }
}
