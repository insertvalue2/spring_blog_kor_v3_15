package com.tenco.blog.mytest;

import com.tenco.blog.board.Board;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController // IoC , @Controller + @ResponseBody
public class RestControllerTest {

    // 테스트 주소
    // 웹 브라우저 주소창에 시작
    // (우리 도메인에서 요청 주소는 http://localhost:8080/todos/3 보낼 예정)
    // https://jsonplaceholder.typicode.com/todos/1

    @GetMapping("/todos/{id}")
    public String test1(@PathVariable(name = "id") Integer id) {
        // 헷갈리지 마세요. ( 시작은 브라우저에서 우리 서버로 요청)

        // 우리 서버에서 ---> 다른 서버로 HTTP 요청을 보낼 예정 (코드로)
        //   localhost --->   jsonplaceholder 서버로 HTTP 요청 (Server to Server)

        // 1. 요청할 외부 API 주소를 문자열로 조립
        String url = "https://jsonplaceholder.typicode.com/todos/" + id;

        // 2. HttpClient 대신 RestTemplate 사용
        RestTemplate template1 = new RestTemplate();

        // 3. HTTP 요청 헤더 만들기
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);

        // 4. HTTP 엔티티 만들기 ( HTTP 메세지 )
        HttpEntity requestHttpMessage = new HttpEntity(headers);

        // 5 exchange 메서드를 통해서 GET 요청 보내기
        ResponseEntity<String> responseHttpMessage = template1.exchange(
                url, // 어디로 보낼지 (String, URI 객체 둘다 가능)
                HttpMethod.GET, // 어떤 방식으로 보낼지 결정 (GET, POST, PUT , DELETE ..)
                requestHttpMessage, // 위 에서만든 HTTPEntity (헤더 + 바디 묶음)
                String.class   // 응답 본문 (응답 body ) 의미 (String 설계 함)
        );

        System.out.println(responseHttpMessage.getStatusCode());
        System.out.println(responseHttpMessage.getHeaders());
        System.out.println(responseHttpMessage.getBody());

        // 사용자 요청한 우리 웹 브라우저 출력 됨.
        return responseHttpMessage.getBody();
    }

    // Server to Server  ( 시작은 우리 웹 브라우저에서 출발)
    // http://localhost:8080/exchnage-test
    @GetMapping("/exchnage-test")
    public ResponseEntity<?> test2() {

        // 1. 다른 서버로 요청할 주소
        String url = "https://jsonplaceholder.typicode.com/posts";

        // 2. 통신할 객체 선언
        RestTemplate restTemplate = new RestTemplate();

        // 3. HTTP 헤더 만들기
//        문서에서 확인해야 함
//        headers: {
//            'Content-type': 'application/json; charset=UTF-8',

//        },
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/json; charset=utf-8");

        // 4. HTTP Body 만들기 (데이터 샘플로 처리)
        PostDTO postDTO = PostDTO.builder()
                .userId(1)
                .title("exchange 메서드 연습")
                .body("헤더를 마음대로 설정 가능 , GET , POST, PUT, DELETE 다 사용 가능")
                .build();

        // 5. HttpEntity 생성 (헤더 + 바디 ) 결합
        HttpEntity<PostDTO> request = new HttpEntity(postDTO, headers);

        // 6. 통신 요청
        ResponseEntity<PostDTO> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                PostDTO.class
        );

        // 7 응답 확인
        PostDTO resultBody = response.getBody();
        System.out.println("응답 상태 : " + response.getStatusCode());
        System.out.println("생성된 타이틀 : " + response.getBody().getTitle());
        System.out.println("저장된 바디 : " + resultBody.getTitle());

        return ResponseEntity.status(HttpStatus.CREATED).body(resultBody);
    }


}




