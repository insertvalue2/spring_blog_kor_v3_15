package com.tenco.blog._core.errors;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 모든 컨트롤러에서 발생하는 예외를 이 클래스에서 처리 하겠다.
// RuntimeException 이 발생되면 해당 이 파일로 예외 처리가 오게 됨.
@Slf4j
@ControllerAdvice // IoC -> 에러 페이지 찾아 가능 녀석
// @RestControllerAdvice // 에러를 데이터로 반환할 때 사용
public class GlobalExceptionHandler {

    /**
     * 400 Bad Request — alert 창 + 이전 페이지로 돌아가기
     *
     * 이전에는 err/400 페이지로 이동했지만, 회원가입 / 이메일 인증 같은 폼 흐름에서는
     * "alert 띄우고 이전 페이지로 돌아가서 다시 입력" 흐름이 사용자 친화적이다.
     *
     * 작은따옴표 이스케이프: 에러 메시지에 ' 가 포함되어도 JS 구문 오류 안 나도록 \\' 로 변환.
     */
    @ExceptionHandler(Exception400.class)
    @ResponseBody
    public String ex400(Exception400 e, HttpServletRequest request) {
        log.warn("=== 400 Bad Request 에러 발생 ===");
        log.warn("요청 URL: {}", request.getRequestURL());
        log.warn("에러메시지: {}", e.getMessage());

        String message = e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다";
        String escapedMessage = message.replace("'", "\\'");
        return """
            <script>
                alert('%s');
                history.back();
            </script>
            """.formatted(escapedMessage);
    }

//    @ExceptionHandler(Exception401.class)
//    public String ex401(Exception401 e, HttpServletRequest request) {
//        log.warn("=== 401 Unauthorized 에러 발생 ===");
//        log.warn("요청 URL: {}", request.getRequestURL());
//        log.warn("에러메시지: {}", e.getMessage());
//
//        request.setAttribute("msg", e.getMessage());
//        return "err/401";
//    }


    @ExceptionHandler(Exception401.class)
    @ResponseBody
    public String ex401(Exception401 e, HttpServletRequest request) {
        String script = """
            <script>
                alert('%s');
                location.href='/login-form';
            </script>
            """.formatted(e.getMessage());
        return script;
    }

//    @ExceptionHandler(Exception403.class)
//    public String ex403(Exception403 e, HttpServletRequest request) {
//        log.warn("=== 403 Forbidden 에러 발생 ===");
//        log.warn("요청 URL: {}", request.getRequestURL());
//        log.warn("에러메시지: {}", e.getMessage());
//
//        request.setAttribute("msg", e.getMessage());
//        return "err/403";
//    }

    @ExceptionHandler(Exception403.class)
    @ResponseBody // 파일 찾지 말고 데이터 반환
    public String ex403(Exception403 e, HttpServletRequest request) {

//        String script = "<script>alert(' " + e.getMessage() + " ');" +
//                "history.back();" +
//                "</script>";

        String script = """
        <script>
            alert('%s');
            history.back();
        </script>
        """.formatted(e.getMessage());

        return script;
    }

    @ExceptionHandler(Exception404.class)
    public String ex404(Exception404 e, HttpServletRequest request) {
        log.warn("=== 404 Not Found 에러 발생 ===");
        log.warn("요청 URL: {}", request.getRequestURL());
        log.warn("에러메시지: {}", e.getMessage());

        request.setAttribute("msg", e.getMessage());
        return "err/404";
    }

    @ExceptionHandler(Exception500.class)
    public String ex500(Exception500 e, HttpServletRequest request) {
        log.warn("=== 500 Internal Server Error 에러 발생 ===");
        log.warn("요청 URL: {}", request.getRequestURL());
        log.warn("에러메시지: {}", e.getMessage());

        request.setAttribute("msg", e.getMessage());
        return "err/500";
    }

    // 기타 모든 RuntimeException 처리 (최후의 보루)
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.warn("=== 예상치 못한 런타임 에러 발생 ===");
        log.warn("요청 URL: {}", request.getRequestURL());
        log.warn("에러메시지: {}", e.getMessage());

        request.setAttribute("msg", "시스템 오류가 발생했습니다. 관리자에게 문의해주세요");
        return "err/500";
    }

    /**
     * 데이터베이스 제약조건 위반 오류 처리
     *
     * 동시 가입 같은 경쟁 상황에서 발생할 수 있다 — 거의 동시에 같은 이메일로 가입 시도하면
     * 애플리케이션의 findByEmail 중복 체크는 통과되지만 DB 의 unique 제약에서 막힌다.
     *
     * 친절한 alert 로 변경 후 이전 페이지로 돌아가서 재입력하도록 유도.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public String handleDataIntegrityViolationException(DataIntegrityViolationException e,
                                                        HttpServletRequest request) {
        log.warn("=== 데이터 베이스 제약 조건 위반 오류 발생 ===");
        log.warn("요청 URL: {}", request.getRequestURL());
        log.warn("에러메시지: {}", e.getMessage());

        String errorMessage = e.getMessage();
        String userMessage;

        if (errorMessage != null && errorMessage.contains("email")) {
            userMessage = "이미 등록된 이메일입니다";
        } else if (errorMessage != null && errorMessage.contains("username")) {
            userMessage = "이미 존재하는 사용자 이름입니다";
        } else if (errorMessage != null && errorMessage.contains("FOREIGN KEY")) {
            userMessage = "관련된 데이터가 있어 삭제할 수 없습니다";
        } else {
            userMessage = "데이터베이스 제약조건 위반이 발생했습니다";
        }

        String escapedMessage = userMessage.replace("'", "\\'");
        return """
            <script>
                alert('%s');
                history.back();
            </script>
            """.formatted(escapedMessage);
    }

}
