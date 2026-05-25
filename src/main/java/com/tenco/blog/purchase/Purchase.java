package com.tenco.blog.purchase;

import com.tenco.blog.board.Board;
import com.tenco.blog.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * 구매 내역 엔티티
 *
 * User 와 Board 의 Many-to-Many 관계를 Purchase 라는 중간 테이블로 구현한다.
 *  - 한 사용자는 여러 게시글을 구매할 수 있다.
 *  - 한 게시글은 여러 사용자에게 구매될 수 있다.
 *
 * 단방향 관계 설계
 *  - Purchase -> User  (ManyToOne): 구매한 사용자
 *  - Purchase -> Board (ManyToOne): 구매한 게시글
 *
 * 유니크 제약 (user_id, board_id) — 같은 사람이 같은 글을 두 번 구매하지 못하게.
 */
@Data
@NoArgsConstructor
@Table(
        name = "purchase_tb",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_board", columnNames = {"user_id", "board_id"})
        }
)
@Entity
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 단방향 관계: Purchase -> User (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 단방향 관계: Purchase -> Board (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    // 구매 시 지불한 포인트
    private Integer price;

    @CreationTimestamp
    private Timestamp createdAt;

    @Builder
    public Purchase(User user, Board board, Integer price) {
        this.user = user;
        this.board = board;
        this.price = price;
    }
}
