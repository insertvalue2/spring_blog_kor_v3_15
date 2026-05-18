package com.tenco.blog.reply;

import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog.board.Board;
import com.tenco.blog.board.BoardRepository;
import com.tenco.blog.user.User;
import com.tenco.blog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// Service 계층에서는 여러 Repository를 조합해서 비즈니스 규칙을 완성한다
// 즉, 서비스 계층이 필요한 이유 중 하나이다.
@Service // IoC
@RequiredArgsConstructor // DI 처리
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // 댓글 목록 조회
    public List<ReplyResponse.ListDTO> 댓글목록조회(Integer boardId, Integer sessionUserId) {

        List<Reply> replyList = replyRepository.findByBoardIdWithUser(boardId);

//        List<ReplyResponse.ListDTO> listDtoList = new ArrayList<>();
//        for(int i =0; i < replyList.size(); i++) {
//            Reply tempReply = replyList.get(i);
//            // 객체 생성을 위해 필요한 데이터
//            ReplyResponse.ListDTO listDTO = new ReplyResponse.ListDTO(tempReply, sessionUserId);
//            listDtoList.add(listDTO);
//        }
        // Stream API 활용
        return replyList.stream()
                .map(reply -> new ReplyResponse.ListDTO(reply, sessionUserId))
                .toList();
    }


    @Transactional
    public Reply 댓글작성(ReplyRequest.SaveDTO saveDTO, Integer id) {
        // 게시글 조회
        Board boardEntity = boardRepository.findById(saveDTO.getBoardId()).orElseThrow(
                () -> new Exception404("해당 게시글을 찾을 수 없습니다"));

        // id 로 사용자 조회
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다")
        );
        Reply reply = saveDTO.toEntity(userEntity, boardEntity);
        replyRepository.save(reply);
        return reply;
    }

    @Transactional
    public void 댓글삭제(Integer replyId, Integer sessionUserId) {
        Reply replyEntity = replyRepository.findById(replyId).orElseThrow(
                () -> new Exception404("해당 댓글을 찾을 수 없습니다"));

        // 인가 처리
        if(replyEntity.getUser().getId() != sessionUserId) {
            throw new Exception403("댓글 삭제 권한이 없습니다");
        }
        // 댓글 삭제
        replyRepository.delete(replyEntity);
    }
}
