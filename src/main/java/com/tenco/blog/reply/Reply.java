package com.tenco.blog.reply;

import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog.board.Board;
import com.tenco.blog.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Entity
@Table(name = "reply_tb")
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 500 , nullable = false)
    private String comment;

    @CreationTimestamp // pc --> db 자동 주입
    private Timestamp createdAt;

    // Reply -> User 연관관계 설정 (FK -> 자바에서 표현하는 개념)
    // 1 : N, N : 1 , M:N <- 중 선택
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 하나의 게시글에는 여러개의 댓글이 작성될 수 있다.  1 : N
    // N : 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @Builder
    public Reply(String comment, User user, Board board) {
        this.comment = comment;
        this.user = user;
        this.board = board;
    }

    /**
     * 댓글 소유자 확인 로직 (세션정보, DB 작성된 user_id)
     * @return
     */
    public boolean isOwner(Integer userId) {
        if(this.user == null || userId == null) {
            return false;
        }
        // 본인이 작성한 댓글이 아님
        if(this.user.getId() != userId) {
            return  false;
        }
        return true;
    }
}
