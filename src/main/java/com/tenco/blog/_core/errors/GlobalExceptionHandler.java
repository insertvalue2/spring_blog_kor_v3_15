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

    @ExceptionHandler(Exception400.class)
    public String ex400(Exception400 e, HttpServletRequest request) {
        log.warn("=== 400 Bad Request 에러 발생 ===");
        log.warn("요청 URL: {}", request.getRequestURL());
        log.warn("에러메시지: {}", e.getMessage());

        request.setAttribute("msg", e.getMessage());
        return "err/400";
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

    // 데이터베이스 관련 및 제약조건 위반 오류 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolationException(DataIntegrityViolationException e,
                                                        HttpServletRequest request) {
        log.warn("=== 데이터 베이스 제약 조건 위반 오류 발생 ===");
        log.warn("요청 URL: {}", request.getRequestURL());
        log.warn("에러메시지: {}", e.getMessage());

        String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.contains("FOREIGN KEY")) {
            request.setAttribute("msg", "관련된 데이터가 있어 삭제할 수 없습니다");
        } else {
            // 실제로는 다른 내용으로 에러페이지에 내려 줘야 함.
            request.setAttribute("msg", "데이터베이스 제약조건 위한:" + e.getMessage());
        }
        return "err/500";

    }

}
