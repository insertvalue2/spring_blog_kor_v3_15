package com.tenco.blog._core.errors;

public class Exception401 extends RuntimeException {

    // 예외 메시지를 외부에서 받아서 내 부모클래스 RuntimeException 에게 생성자로 전달
    public Exception401(String msg) {
        super(msg); // 즉, 부모 클래스 메세지도 내가 직접 작성 부분으로 설정 됨.
    }
}
