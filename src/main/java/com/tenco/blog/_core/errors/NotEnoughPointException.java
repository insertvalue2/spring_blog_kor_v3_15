package com.tenco.blog._core.errors;

// 포인트 부족 예외 (유료 게시글 구매 시 보유 포인트가 모자랄 때)
// 이름 그대로 "포인트가 충분하지 않다(not enough)" 는 뜻.
// 일반 400(이전 페이지로) 과 달리, 이 예외는 "마이페이지로 보내 충전을 유도" 하는 용도.
public class NotEnoughPointException extends RuntimeException {

    // 예외 메시지를 외부에서 받아서 부모(RuntimeException) 생성자로 전달
    public NotEnoughPointException(String msg) {
        super(msg);
    }
    // throw new NotEnoughPointException("포인트가 부족합니다"); 사용 예시
}
