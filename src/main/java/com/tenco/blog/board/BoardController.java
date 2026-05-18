package com.tenco.blog.board;

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

        BoardResponse.DetailDTO detailDTO = boardService.게시글상세조회(id);

        // 게시글 상세보기는 로그인 하지 않은 사용자도 들어올 수 있음
        User sessionUser = (User) session.getAttribute("sessionUser");
        Integer sessionUserId = sessionUser != null ? sessionUser.getId() : null;
        List<ReplyResponse.ListDTO> replyList = replyService.댓글목록조회(id, sessionUserId);

        // view 에 데이터 전달
        model.addAttribute("board", detailDTO);
        model.addAttribute("checkIsOwner", detailDTO.checkIsOwner(sessionUserId));
        model.addAttribute("replyList", replyList);

        return "board/detail";
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
