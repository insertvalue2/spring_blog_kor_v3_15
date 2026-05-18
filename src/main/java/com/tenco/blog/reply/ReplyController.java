package com.tenco.blog.reply;

import com.tenco.blog.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller // IoC
@RequiredArgsConstructor // DI 처리
public class ReplyController {

    private final ReplyService replyService;

    // 댓글 등록 기능 요청
    @PostMapping("/reply/save")
    public String saveProc(ReplyRequest.SaveDTO saveDTO, HttpSession session) {
        // 1. 인증검사 --> LoginInterceptor 처리
        User sessionUser = (User) session.getAttribute("sessionUser");
        // 2. 유효성 검사
        saveDTO.validate();

        replyService.댓글작성(saveDTO, sessionUser.getId());

        // 해당 게시글에 댓글 작성후 리다이렉션 처리 (해당 게시글로)
        return "redirect:/board/" + saveDTO.getBoardId();
    }

    // 댓글 삭제 기능 요청
    // /reply/30/delete
    @PostMapping("/reply/{id}/delete")
    public String delete(@PathVariable(name = "id") Integer replyId,
                         HttpSession session,
                         @RequestParam(name = "boardId") Integer boardId) {
        // 1. 인증검사 (인터셉터)
        User sessionUser = (User) session.getAttribute("sessionUser");
        replyService.댓글삭제(replyId,sessionUser.getId());

        return "redirect:/board/" + boardId;
    }
}




