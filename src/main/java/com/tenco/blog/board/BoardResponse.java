package com.tenco.blog.board;

import com.tenco.blog._core.util.MyDateUtil;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 응답 DTO
 * <p>
 * Open Session In View 가 false 일 때
 * 트랜잭션이 종료 되는 시점에 LAZY 로딩이 불가능하다.
 * Service 단에서 필요한 데이터를 모두 조회 또는 일부로 호출(트리거)해서 응답 DTO 변환해서 반환
 * 엔티티를 직접 반환하지 않고(Controller,View) 서비스단에서 DTO 내려줄 예정(결합도 감소)
 */
public class BoardResponse {

    // 게시글 목록 응답 DTO
    @Data
    public static class ListDTO {
        private Integer id;
        private String title;
        // username 평탄화 작업 : SSR 설계시 권장 방법, CSR 일 경우는 계층구조로 내려주는게 좋다
        private String username;
        private String createdAt;

        public ListDTO(Board board) {
            this.id = board.getId();
            this.title = board.getTitle();
            // 방어적 코드 활용
            if (board.getUser() != null) {
                this.username = board.getUser().getUsername();
            }
            if (board.getCreatedAt() != null) {
                this.createdAt = MyDateUtil.timestampFormat(board.getCreatedAt());
            }
        }
    }  // end of ListDTO inner class

    // 게시글 상세 보기 응답 DTO
    @Data
    public static class DetailDTO {
        private Integer id; // board PK
        private String title;
        private String content;
        private String username;
        private Integer userId; //  user PK
        private boolean isOwner;

        public DetailDTO(Board board) {
            this.id = board.getId();
            this.title = board.getTitle();
            this.content = board.getContent();
            if(board.getUser() != null) {
                this.username = board.getUser().getUsername();
                this.userId = board.getUser().getId();
            }
        }

        // 소유자 확인
        public boolean checkIsOwner(Integer sessionUserId) {
            if(sessionUserId == null) {
                return false;
            }
            if (sessionUserId.equals(this.userId)) {
                return true;
            } else {
                return false;
            }
        }

    } // end of DetailDTO inner class


    // 페이징 DTO 설계
    // Page<Board> page --> 우리 사용할 DTO 클래스 단순히 변환 하는 작업 및 편의 기능 추가
    @Data
    public static class PageDTO {
        private List<ListDTO> list;
        private int currentPage;
        private int size;
        private int totalPages;
        private long totalElements;
        private boolean first;
        private boolean last;
        private int prevPage;
        private int nextPage;
        private List<PageItem> pageItemNumbers;

        public PageDTO(Page<Board> page) {
            // List<Board> --> List<ListDTO> 형태로 변환 작업
            this.list = page.getContent().stream()
                    .map(board -> new ListDTO(board))
                    .toList();
            // 사전 지식 :  page.getNumber() <-- 0 번 부터 시작 함.
            // 화면에서는 변수 this.currentPage <-- 1번 부터 보여 줘야 함.
            this.currentPage = page.getNumber()  + 1;
            this.size = page.getSize(); // 현재 default 값은 2 임 (수정 가능)
            this.totalPages = page.getTotalPages();
            this.totalElements = page.getTotalElements(); // long로 설계 되어 있음
            this.first = page.isFirst();
            this.last = page.isLast();

            // 이전/다음 페이지 번호 ( 템플릿이 산술 계산을 못하기 때문에 여기서 계산해서 내려 줌)
            this.prevPage = this.first ? this.currentPage : this.currentPage - 1;
            this.nextPage = this.last ? this.currentPage : this.currentPage + 1;

            // 페이지 번호 윈도우 : 현재 페이지 기준 앞뒤 2페이지 (최대 5개)
            // 예1 : 현재 페이지 5 ---> [ 3, 4, 5, 6, 7 ]
            // 예2 : 현재 페이지 1 ---> [1, 2, 3]
            int start = Math.max(1, this.currentPage - 2);
            // 예3 : 현재 페이지 5, 총 페이지 5 일 경우 [3, 4, 5]
            int end = Math.min(this.totalPages, this.currentPage + 2);

            // 빈 List 먼저 생성 후 값 할당
            this.pageItemNumbers = new ArrayList<>();
            for(int i = start; i <= end; i++) {
                boolean isActive = (i == this.currentPage);
                this.pageItemNumbers.add(new PageItem(i, isActive));
            }
        }

    } // end of PageDTO

    @Data
    public static class PageItem {
        private int number;
        private boolean active;

        public PageItem(int number, boolean active) {
            this.number = number;
            this.active = active;
        }
    }


} // end of outer class
