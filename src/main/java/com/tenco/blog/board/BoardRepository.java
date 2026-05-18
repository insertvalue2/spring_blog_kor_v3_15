package com.tenco.blog.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Board 엔티티에 대한 JPA Repository 인터페이스 (클래스 아님!)
 * 게시글과 관련된 데이터 베이스 접근을 담당하게 됨
 * 기본적인 CRUD 이미 제공 됨.
 */
// @Repository // IoC - 굳이 명시할 필요 없음 ( JpaRepository 인터페이스를 상속했기 때문에 두번 선언할 필요 없음)
public interface BoardRepository extends JpaRepository<Board, Integer> {

    // 1. 등록 및 수정 save(Board entity)
    // 2. 단건 조회 : findById(Integer id)
    // 3. 전체 조회 : findAll()
    // 4. 삭제 : deleteById(Integer id)
    // 5. 데이터 개수: count()
    // 6. 존재 여부 확인: existsById(Integer id)

    // 단건 조회
    // 1. 게시글 ID로 조회시 사용자 정보도 함께 가져오기
    @Query("""
            SELECT b FROM Board b JOIN FETCH b.user WHERE b.id = :id
           """)
    Optional<Board> findByIdJoinUser(@Param("id") Integer id);

    // 2. 전체 게시글 조회 (단 한번에 작성자 정보도 조회)
    @Query("""
    SELECT b FROM Board b JOIN FETCH b.user ORDER BY b.id DESC
    """)
    List<Board> findAllJoinUser();

    // 3. 데이터 수정은 더티 체킹으로 처리


    // 4. 전체 게시글 조회 + 페이징 처리
    @Query(value = """
        SELECT DISTINCT b FROM Board b JOIN FETCH b.user ORDER BY b.createdAt DESC
    """,
        countQuery = """
            SELECT count(DISTINCT b) FROM Board b
            """)
    Page<Board> findAllWithUserOrderByCreatedAtDesc(Pageable pageable);


    // 5. 전체 게시글 조회 + 페이징 처리 + LIKE 검색
    @Query(value = """
      SELECT DISTINCT b 
      FROM Board b 
      JOIN FETCH b.user
      WHERE LOWER(b.title) LIKE LOWER( CONCAT('%', :keyword , '%'))
          OR LOWER(b.content) LIKE LOWER( CONCAT('%', :keyword , '%'))    
      ORDER BY b.createdAt DESC    
    """,
    countQuery = """
        SELECT count(DISTINCT b)
        FROM Board b
        WHERE LOWER(b.title) LIKE LOWER( CONCAT('%', :keyword , '%'))
           OR LOWER(b.content) LIKE LOWER( CONCAT('%', :keyword , '%'))         
    """)
    Page<Board> findByTitleContainingOrContentContaining(@Param("keyword") String keyword,
                                                         Pageable pageable);

}

