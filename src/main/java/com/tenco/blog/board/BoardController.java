package com.tenco.blog.board;

import com.tenco.blog.purchase.PurchaseService;
import com.tenco.blog.reply.ReplyResponse;
import com.tenco.blog.reply.ReplyService;
import com.tenco.blog.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller // IoC
@RequiredArgsConstructor // DI
public class BoardController {

    private final BoardService boardService;
    // 댓글 목록 조회시 필요
    private final ReplyService replyService;
    // 유료 게시글 구매 처리
    private final PurchaseService purchaseService;

    /**
     * 게시글 작성 화면 요청
     * @return 페이지 반환
     * 주소설계 : http://localhost:8080/board/save-form
     */
    @GetMapping("/board/save-form")
    public String saveForm(HttpSession httpSession) {
        return "board/save-form";
    }

    /**
     * 게시글 작성 기능 요청
     * @return 페이지 반환
     * 주소설계 : http://localhost:8080/board/save-form
     */
    @PostMapping("/board/save")
    public String saveProc(BoardRequest.SaveDTO saveDTO, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        saveDTO.validate();
        boardService.게시글작성(saveDTO, sessionUser);
        return "redirect:/";
    }


    /**
     * 게시글 목록 화면 요청
     * 주소설계 : http://localhost:8080/
     */
    // 페이징 처리 주소설계 : http://localhost:8080/?page=1&size=2
    // 페이징 처리 주소설계 : http://localhost:8080/ <--- defaultValue 로 동작
    // @RequestParam(name= "page") 필수 값 처리
    @GetMapping({"/board/list", "/"})
    public String list(Model model,
                       @RequestParam(name = "page", defaultValue = "1") Integer page,
                       @RequestParam(name = "size", defaultValue = "5") Integer size,
                       @RequestParam(name = "keyword", required = false) String keyword) {

        BoardResponse.PageDTO boardPage = boardService.게시글목록(page, size, keyword);
        model.addAttribute("boardPage", boardPage);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        return "board/list";
    }

//    @GetMapping({"/", "index"})
//    public String list(Model model) {
//        List<BoardResponse.ListDTO> boardList = boardService.게시글목록();
//        // OSIV 개념을 false 설정했기 때문에 여기서 LAZY 요청을 하면 터져 버린다.
//        ///boardList.get(0).getUser().getUsername();
//
//        model.addAttribute("boardList", boardList);
//        return "board/list";
//    }

    // 게시글 상세보기 화면 요청
    // http://localhost:8080/board/1
    @GetMapping("/board/{id}")
    public String detailPage(@PathVariable(name = "id") Integer id, Model model, HttpSession session) {

        // 게시글 상세보기는 로그인 하지 않은 사용자도 들어올 수 있음
        User sessionUser = (User) session.getAttribute("sessionUser");
        Integer sessionUserId = sessionUser != null ? sessionUser.getId() : null;

        // 구매 여부까지 포함해서 상세 조회
        BoardResponse.DetailDTO detailDTO = boardService.게시글상세조회(id, sessionUserId);

        // 소유자 여부
        boolean isOwner = detailDTO.checkIsOwner(sessionUserId);

        // 본문 열람 가능 여부 = 무료글이거나 / 이미 구매했거나 / 본인 글
        //  Mustache 는 AND/OR 같은 논리 연산을 못 하므로, 자바에서 미리 계산해 내려준다.
        boolean canRead = !detailDTO.getPremium() || detailDTO.getPurchased() || isOwner;

        List<ReplyResponse.ListDTO> replyList = replyService.댓글목록조회(id, sessionUserId);

        // view 에 데이터 전달
        model.addAttribute("board", detailDTO);
        model.addAttribute("checkIsOwner", isOwner);
        model.addAttribute("canRead", canRead);
        model.addAttribute("replyList", replyList);

        return "board/detail";
    }

    /**
     * 유료 게시글 구매 요청
     *  - 로그인 필요 (인터셉터 또는 세션 확인)
     *  - PurchaseService 가 포인트 차감 + 구매 내역 저장을 한 트랜잭션으로 처리
     *
     * 주소설계 : POST http://localhost:8080/board/{id}/purchase
     */
    @PostMapping("/board/{id}/purchase")
    public String purchaseProc(@PathVariable(name = "id") Integer id, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");

        // 구매 처리 → 포인트가 차감된 최신 User 를 받는다
        User updatedUser = purchaseService.구매하기(id, sessionUser.getId());

        // 세션 동기화: 세션의 sessionUser 를 최신 포인트로 갱신해야
        // 마이페이지 등에서 차감된 포인트가 정상적으로 보인다.
        session.setAttribute("sessionUser", updatedUser);

        // 구매 후 다시 상세 페이지로 (이제 본문이 보임)
        return "redirect:/board/" + id;
    }


    // 삭제 기능 요청
    @PostMapping("/board/{id}/delete")
    public String deleteProc(@PathVariable(name = "id") Integer id, HttpSession session) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        boardService.게시글삭제(id, sessionUser);
        return "redirect:/";
    }


    // http://localhost:8080/board/1/update-form
    // 게시글 수정 화면 요청
    @GetMapping("/board/{id}/update-form")
    public String updateFormPage(@PathVariable(name = "id") Integer id, Model model, HttpSession session) {
       User sessionUser = (User) session.getAttribute("sessionUser");
       BoardResponse.DetailDTO detailDTO = boardService.게시글상세화면및인가처리(id, sessionUser);
       model.addAttribute("board", detailDTO);
       return "board/update-form";
    }


    @PostMapping("/board/{id}/update")
    public String updateProc(@PathVariable(name = "id") Integer id,
                             BoardRequest.UpdateDTO updateDTO, HttpSession session) {
        User sessionUser =  (User) session.getAttribute("sessionUser");
        updateDTO.validate();
        boardService.게시글수정(id, updateDTO, sessionUser);
        return "redirect:/board/" + id;
    }

}
