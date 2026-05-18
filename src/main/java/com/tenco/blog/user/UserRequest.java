package com.tenco.blog.user;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class UserRequest {

    // 로그인 DTO
    @Data
    public static class LoginDTO {
        private String username;
        private String password;

        // 유효성 검사
        public void validate() {
            if(username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("사용자명을 입력하세요");
            }

            if(password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("비밀번호를 입력하세요");
            }
        }
    }

    // 회원가입 DTO
    @Data
    public static class JoinDTO {
        private String username;
        private String password;
        private String email;

        /**
         * 프로필 이미지 (선택 사항, null 이 될 수 있음)
         * MultipartFile : Spring 에서 제공해주고 있는 파일 업로드를 처리하기 위한 인터페이스이다.
         */
        private MultipartFile profileImage;

        // 편의 기능 추가 - 내가 가지고 있는 멤버 변수에 값으로 User 엔티를 생성
        public User toEntity(String profileImage) {
            return User.builder()
                    .username(username)
                    .password(password)
                    .profileImage(profileImage)
                    .email(email)
                    .build();
        }

        // 유효성 검사 메서드 만들기
        public void validate() {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("사용자명은 필수 입니다");
            }

            if(password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("비밀번호는 필수 입니다");
            }

            if(password.length() < 4 || password.length() > 255) {
                throw new IllegalArgumentException("비밀번호는 최소 4글자 이상이어야 합니다");
            }

            if(email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("이메을은 필수 입니다");
            }
            // 입력값 : abc@naver.com --> contains() -->   true   --> ! --> false
            if(email.contains("@") == false) {
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다");
            }

        }
    }

    @Data
    public static class UpdateDTO {

        private String password;
        private MultipartFile profileImage;
        private String profileImageFileName;

        public void validate() {
            if(password == null || password.isBlank()) {
                throw new IllegalArgumentException("비밀번호는 필수 입니다");
            }
            if (password.length() < 4) {
                throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다");
            }
        }
    }

}
