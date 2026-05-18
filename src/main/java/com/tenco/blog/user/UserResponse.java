package com.tenco.blog.user;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

public class UserResponse {

    /**
     * 액세스 토큰 응답 정보
     */
    @Data
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OAuthToken {
        private String accessToken;
        private String tokenType;
        private String refreshToken;
        private Integer expiresIn;
        private String scope;
        private Integer refreshTokenExpiresIn;
    }

    /**
     * 카카오 사용자 정보 응답
     */
    // // KakaoProfile.kakaoAccount.profile.nickname
    // KakaoProfile.kakaoAccount.profile.nickname
    @Data
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class KakaoProfile {
        private Long id;
        private KakaoAccount kakaoAccount;

        @Data
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class KakaoAccount {
            private Profile profile;

            @Data
            @NoArgsConstructor
            @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
            public static class Profile {
                private String nickname;
                private String thumbnailImageUrl;
                private String profileImageUrl;
                private Boolean isDefaultImage;
                private Boolean isDefaultNickname;
            }
        }
    }
}