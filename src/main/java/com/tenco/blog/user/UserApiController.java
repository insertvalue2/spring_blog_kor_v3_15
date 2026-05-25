package com.tenco.blog.user;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 사용자 API 컨트롤러 (AJAX 전용)
 *
 * AJAX 요청을 처리하는 컨트롤러.
 *
 * @RestController = @Controller + @ResponseBody
 *  - 반환값을 JSON 으로 자동 변환
 *  - 화면(View) 을 반환하지 않고 데이터만 반환
 *
 * API 엔드포인트
 *  - POST /api/email/send   : 인증번호 발송
 *  - POST /api/email/verify : 인증번호 확인
 *  - POST /api/point/charge : 포인트 충전 (테스트용)
 */
@RequiredArgsConstructor
@RestController
public class UserApiController {

    private final MailService mailService;
    private final UserService userService;

    /**
     * 인증번호 발송 API
     *
     * @param reqDTO 이메일 주소가 담긴 DTO
     * @return 성공 메시지 (JSON)
     */
    @PostMapping("/api/email/send")
    public ResponseEntity<?> 인증번호발송(@RequestBody UserRequest.EmailCheckDTO reqDTO) {
        // 1) 유효성 검사 (이메일 비어있지 않은지, @ 포함되었는지)
        reqDTO.validate();

        // 2) 메일 서비스에 인증번호 발송 요청
        mailService.인증번호발송(reqDTO.getEmail());

        // 3) 성공 응답 반환 (JSON 형식)
        return ResponseEntity.ok().body(Map.of("message", "인증번호가 발송되었습니다."));
    }

    /**
     * 인증번호 확인 API
     *
     * 처리 과정
     *  1) DTO 유효성 검사
     *  2) 메일 서비스에 인증번호 확인 요청
     *  3) 성공 시 세션에 "verified_email" 도장이 자동으로 찍힘 (MailService 내부)
     *
     * @param reqDTO 이메일 주소와 인증번호가 담긴 DTO
     * @return 인증 결과 메시지 (JSON)
     */
    @PostMapping("/api/email/verify")
    public ResponseEntity<?> 인증번호확인(@RequestBody UserRequest.EmailCheckDTO reqDTO) {
        // 1) 유효성 검사
        reqDTO.validate();

        // 인증번호 입력 확인
        if (reqDTO.getCode() == null || reqDTO.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "인증번호를 입력해주세요."));
        }

        // 2) 메일 서비스에 인증번호 확인 요청
        boolean isVerified = mailService.인증번호확인(reqDTO.getEmail(), reqDTO.getCode());

        // 3) 결과에 따른 응답 반환
        if (isVerified) {
            return ResponseEntity.ok().body(Map.of("message", "인증되었습니다."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "인증번호가 일치하지 않습니다."));
        }
    }

    /**
     * 포인트 충전 API (테스트용)
     *
     * 처리 과정
     *  1) DTO 유효성 검사
     *  2) 세션에서 로그인 사용자 확인
     *  3) 포인트 충전 후 현재 포인트 응답
     *
     * 실무에서는 PG(결제) 연동으로 대체된다.
     *
     * @param reqDTO  충전 금액 DTO
     * @param session 세션 (로그인 사용자)
     * @return 충전 결과 (현재 포인트 포함)
     */
    @PostMapping("/api/point/charge")
    public ResponseEntity<?> 포인트충전(@RequestBody UserRequest.PointChargeDTO reqDTO,
                                   HttpSession session) {
        // 1) 유효성 검사
        reqDTO.validate();

        // 2) 세션에서 사용자 정보 추출
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        // 3) 포인트 충전 처리
        User updatedUser = userService.포인트충전(sessionUser.getId(), reqDTO.getAmount());

        // 4) 세션 동기화 (충전된 포인트를 세션 사용자에도 반영)
        session.setAttribute("sessionUser", updatedUser);

        return ResponseEntity.ok().body(Map.of(
                "message", "포인트가 충전되었습니다.",
                "point", updatedUser.getPoint()
        ));
    }
}
