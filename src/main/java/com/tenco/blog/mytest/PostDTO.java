package com.tenco.blog.mytest;


//fetch('https://jsonplaceholder.typicode.com/posts', {
//    method: 'POST',
//            body: JSON.stringify({
//            title: 'foo',
//            body: 'bar',
//            userId: 1,
//  }),
//    headers: {
//        'Content-type': 'application/json; charset=UTF-8',
//    },
//})

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    private Integer userId;
    private String title;
    private String body;
}
