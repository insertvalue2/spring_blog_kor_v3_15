package com.tenco.blog.reply;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog.board.Board;
import com.tenco.blog.user.User;
import lombok.Data;

public class ReplyRequest {

    @Data
    public static class SaveDTO {
        private Integer boardId; // 게시글 PK
        private String comment; // 댓글 내용

        // 유효성 검사
        public void validate() {
            if(comment == null || comment.isBlank()) {
                throw new Exception400("댓글 내용을 입력해주세요");
            }
            if(comment.length() > 500) {
                throw new Exception400("댓글은 500자 이하여야 합니다");
            }
            if(boardId == null) {
                throw new Exception400("잘못된 요청입니다");
            }
        }

        // DTO를 엔티티로 변환 편의 기능
        public Reply toEntity(User user, Board board) {
            return Reply
                    .builder()
                    .comment(this.comment)
                    .user(user)
                    .board(board)
                    .build();
        }

    }
}
