package com.tenco.blog.reply;

import com.tenco.blog._core.util.MyDateUtil;
import lombok.Data;

public class ReplyResponse {

    /**
     * 댓글 목록 응답 DTO
     */
    @Data
    public static class ListDTO {
        private Integer id; // 댓글 pk
        private String comment; // 댓글 내용
        private Integer userId; // 댓글 작성사 PK
        private String username; // 댓글 작성자 이름
        private boolean isOwner; // 댓글 소유자 여부 true, false (로그인 사용자 기준)
        private String createdAt; // 댓글 작성 시간

        public ListDTO(Reply reply, Integer sessionUserId) {
            this.id = reply.getId();
            this.comment = reply.getComment();
            // JOIN FETCH로 한방에 들고올 예정
            if(reply.getUser() != null) {
                this.userId = reply.getUser().getId();
                this.username = reply.getUser().getUsername();
            }
            if(reply.getCreatedAt() != null) {
                this.createdAt = MyDateUtil.timestampFormat(reply.getCreatedAt());
            }
            this.isOwner = reply.isOwner(sessionUserId);
        }
    }
}
