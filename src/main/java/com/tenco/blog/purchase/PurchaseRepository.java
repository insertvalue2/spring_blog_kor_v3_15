package com.tenco.blog.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 구매 내역 Repository
 *
 * User 와 Board 의 구매 관계를 관리한다.
 */
@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

    /**
     * 사용자와 게시글의 구매 내역 조회
     *  - 구매 객체 자체(가격, 시각 등)가 필요할 때 사용 (상세/환불 등)
     */
    @Query("SELECT p FROM Purchase p WHERE p.user.id = :userId AND p.board.id = :boardId")
    Optional<Purchase> findByUserIdAndBoardId(@Param("userId") Integer userId,
                                              @Param("boardId") Integer boardId);

    /**
     * 사용자와 게시글의 구매 여부 확인
     *  - "COUNT(p) > 0" JPQL 로 존재 여부만 boolean 으로 받아온다.
     *  - 엔티티 전체를 로드하지 않고 DB 에서 boolean 만 받으므로 가볍다.
     */
    @Query("SELECT COUNT(p) > 0 FROM Purchase p WHERE p.user.id = :userId AND p.board.id = :boardId")
    boolean existsByUserIdAndBoardId(@Param("userId") Integer userId,
                                     @Param("boardId") Integer boardId);
}
