package com.tenco.blog.reply;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// @Repository - 부모의 클래스에 정의 되어 있음()
public interface ReplyRepository extends JpaRepository<Reply, Integer> {

    // 1. 등록 및 수정 save(Reply entity)
    // 2. 단건 조회 : findById(Integer id)
    // 3. 전체 조회 : findAll()
    // 4. 삭제 : deleteById(Integer id) - reply
    // 5. 데이터 개수: count()
    // 6. 존재 여부 확인: existsById(Integer id)


//    select r.*, b.*, u.*
//    from reply_tb r
//    inner join board_tb b on r.board_id = b.id
//    inner join user_tb u on r.user_id = u.id
//    where r.board_id = 1
//    order by r.created_at asc;
    // JPQL 문법으로 변환
    // 게시글 ID로 댓글 목록 조회(한번에 댓글 작성자 정보 포함 - JOIN FETCH 사용)
    @Query("""
    SELECT r FROM Reply r 
    JOIN FETCH r.user 
    JOIN FETCH r.board 
    WHERE r.board.id = :boardId 
    ORDER BY r.createdAt ASC
""")
    List<Reply> findByBoardIdWithUser(@Param("boardId") Integer boardId);

    /**
     * 이전 수정,삭제 기능에서는 수정은 더티 체킹으로 처리를 하였고 삭제는 기본적으로
     * 제공하는 em.remove() 메서드를 사용해서 처리 했었다. 지금은 직접 JQPL 쿼리를
     * 선언해서 DELETE 처리하는 구문이라 다른 상황이다.
     * @Query(...) <- JPA 기본적으로 SELECT 쿼리로만 인식을 하기 때문에
     * INSERT, UPDATE, DELETE 는 JPA 에게 SELECT 쿼리가 아니야 라고 알려줘야 제대로 동작
     * 그 어노테이션이 @Modifying 이다 !! 반드시 기억 !!!!
     */
    @Modifying
    @Query("DELETE FROM Reply r WHERE r.board.id = :boardId")
    void deleteByBoardId(@Param("boardId") Integer boardId);

}
