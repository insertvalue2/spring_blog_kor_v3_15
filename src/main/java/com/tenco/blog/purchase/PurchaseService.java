package com.tenco.blog.purchase;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog.board.Board;
import com.tenco.blog.board.BoardRepository;
import com.tenco.blog.user.User;
import com.tenco.blog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구매 서비스
 *
 * 유료 게시글 구매 로직을 처리한다.
 *  - 포인트 차감
 *  - 구매 내역 저장
 *  - 중복 구매 방지 / 작성자 구매 방지 / 포인트 부족 방지
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // 유료 게시글 기본 가격 (500포인트)
    private static final Integer PREMIUM_BOARD_PRICE = 500;

    /**
     * 유료 게시글 구매
     *
     * 트랜잭션 처리: 포인트 차감과 구매 내역 저장을 하나의 트랜잭션으로 묶어
     * 실패 시 롤백하여 데이터 정합성을 보장한다.
     *
     * @param boardId 게시글 ID
     * @param userId  구매자 ID
     * @return 포인트가 차감된 사용자 엔티티 (Controller 에서 세션 동기화에 사용)
     */
    @Transactional
    public User 구매하기(Integer boardId, Integer userId) {
        // 1. 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new Exception404("게시글을 찾을 수 없습니다"));

        // 2. 유료 게시글인지 확인
        if (board.getPremium() == null || !board.getPremium()) {
            throw new Exception400("유료 게시글이 아닙니다");
        }

        // 3. 작성자가 자신의 게시글을 구매하려는 경우 방지
        //    (Board.isOwner() 는 권한 없을 때 예외를 던지는 구조라 여기서는 직접 비교)
        if (board.getUser() != null && board.getUser().getId().equals(userId)) {
            throw new Exception403("자신이 작성한 게시글은 구매할 수 없습니다");
        }

        // 4. 이미 구매한 게시글인지 확인
        if (purchaseRepository.existsByUserIdAndBoardId(userId, boardId)) {
            throw new Exception400("이미 구매한 게시글입니다");
        }

        // 5. 사용자 조회 (포인트 차감을 하기 위해 User  엔티티가 필요)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다"));

        // 6. 포인트 확인 및 차감 (포인트 부족 시 User.deductPoint 내부에서 예외)
        user.deductPoint(PREMIUM_BOARD_PRICE);

        // 7. 구매 내역 저장
        Purchase purchase = Purchase.builder()
                .user(user)
                .board(board)
                .price(PREMIUM_BOARD_PRICE)
                .build();
        purchaseRepository.save(purchase);

        // 8. 사용자 정보 저장 (포인트 차감 반영) + 차감된 사용자 반환
        //    Controller 가 이 반환값으로 세션의 sessionUser 를 갱신해야
        //    화면(마이페이지)의 포인트가 최신으로 보인다.
        return userRepository.save(user);
    }

    /**
     * 구매 여부 확인
     *
     * @param userId  사용자 ID (로그인 안 했으면 null)
     * @param boardId 게시글 ID
     * @return 구매 여부
     */
    public boolean 구매여부확인(Integer userId, Integer boardId) {
        if (userId == null) {
            return false;
        }
        return purchaseRepository.existsByUserIdAndBoardId(userId, boardId);
    }
}
