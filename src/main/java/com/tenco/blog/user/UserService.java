package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog._core.errors.Exception500;
import com.tenco.blog._core.util.FileUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * User 관련 비즈니스 로직을 처리하는 Service 계층
 * Controller 와 Repository 사이에서 실제 업무 로직을 담당
 */
@Slf4j
@Service // IoC
@RequiredArgsConstructor // DI
@Transactional(readOnly = true) // 기본적인 읽기 전용 트랜잭션 처리 , 조회시 더티 체킹 안 일어남
public class UserService {

    private final UserRepository userRepository;
    // DI
    private final PasswordEncoder passwordEncoder;

    // 세션 — 이메일 인증 "verified_email" 도장 확인용 (MailService 가 찍어둔 도장)
    private final HttpSession session;

    // 초기 파미미터 값을 가져 오는 방법
    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${tenco.key}")
    private String tencoKey;

    // http://192.168.4.101:8080/join-form (강사 서버 컴퓨터 주소)
    /**
     * 회원 가입 처리
     * @param joinDTO (사용자 회원가입 요청 정보)
     * @return User (저장된 사용자 정보)
     */
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        log.info("회원가입 서비스 시작");

        // [핵심] 이메일 인증 도장 확인
        //
        // 클라이언트 측 검증(브라우저 JS, hidden 필드) 은 우회 가능하다.
        //   - 개발자 도구로 hidden 값을 변조
        //   - JavaScript 비활성화 → onsubmit 자체가 실행 안 됨
        //   - Postman / curl 로 /join 에 직접 POST
        // 따라서 "정말 이메일 인증을 했는가?" 는 반드시 서버 세션의 도장으로 확인한다.
        //
        // 도장의 출처: MailService.인증번호확인() 이 성공 시 세션에 찍어 둔
        //              "verified_email" = 인증된 이메일 주소.
        //
        // 검증 규칙: 도장이 없거나, 도장의 이메일과 가입 폼의 이메일이 다르면 거부.
        String verifiedEmail = (String) session.getAttribute("verified_email");
        if (verifiedEmail == null || !verifiedEmail.equals(joinDTO.getEmail())) {
            throw new Exception400("이메일 인증을 먼저 완료해주세요");
        }

        // 회원가입시 사용자 이름 중복 체크
        userRepository.findByUsername(joinDTO.getUsername()).ifPresent(user -> {
            log.warn("회원가입 실패 - 중복된 사용자명 : {}", user.getUsername());
            throw new Exception400("이미 존재하는 사용자 이름입니다");
        });

        // 이메일 중복 체크 (애플리케이션 레벨 친절한 에러 메시지용)
        // DB 의 unique 제약과 함께 이중 방어 — Defense in Depth.
        userRepository.findByEmail(joinDTO.getEmail()).ifPresent(user -> {
            log.warn("회원가입 실패 - 중복된 이메일 : {}", user.getEmail());
            throw new Exception400("이미 등록된 이메일입니다");
        });

        // 프로필 이미지 저장 기능 구현 (선택 사항 임)
        String profileImageFilename = null;
        if(joinDTO.getProfileImage() != null && joinDTO.getProfileImage().isEmpty() == false) {
            try {
                // 이미지 파일이 맞는지 검증
                if(FileUtil.isImageFile(joinDTO.getProfileImage()) == false) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다");
                }
                profileImageFilename = FileUtil.saveFile(joinDTO.getProfileImage(), FileUtil.IMAGES_DIR);
            } catch (Exception e) {
                // 디스크 공간 없거나, 권한 없음
                throw new Exception500("프로필 이미지 저장 실패");
            }
        }
        // 코드 수정
        User user = joinDTO.toEntity(profileImageFilename);
        String hashPwd = passwordEncoder.encode(joinDTO.getPassword());

        System.out.println("rawPwd "  + joinDTO.getPassword());
        System.out.println("hashPwd "  + hashPwd);

        user.setPassword(hashPwd);
        User savedUser = userRepository.save(user);

        // 이메일 인증 도장 제거 (일회용 — 재사용 방지)
        //  - 같은 도장으로 또 다른 계정을 만드는 것을 막는다.
        //  - 다음 회원가입은 다시 이메일 인증부터 시작해야 한다.
        session.removeAttribute("verified_email");

        return savedUser;
    }


    /**
     * 로그인 처리
     * @param loginDTO (사용자가 요청한 로그인 정보)
     * @return User(조회된 정보 세션 저장용)
     */
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        log.info("로그인 서비스 시작");

        // 1. 사용지 계정 여부 확인
        User userEntity = userRepository.findByUsernameWithRoles(loginDTO.getUsername())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 사용자 이름 또는 사용자 비번 잘못 입력");
                    return new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
                });

        // 2. 암호화 된 비밀번호 검증
        if(!passwordEncoder.matches(loginDTO.getPassword(), userEntity.getPassword())) {
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
        }

        return userEntity;
    }

    /**
     * 사용자 정보 조회 (프로필 정보 보기 활용)
     * @param id (User PK)
     * @return UserEntity
     */
    public User 회원정보수정화면(Integer id) {
        log.info("사용자 정보 서비스 시작");
        User userEntity = userRepository.findById(id).orElseThrow(() -> {
            log.warn("사용자 정보 조회 실패");
            return new Exception404("사용자 정보를 찾을 수 없습니다");
        });
        return userEntity;
    }


    /**
     * 사용자 정보 수정 처리 (프로필 업데이트)
     * @param id  (User PK)
     * @param updateDTO (사용자가 요청한 데이터)
     * @return User
     */
    @Transactional
    public User 회원정보수정(Integer id, UserRequest.UpdateDTO updateDTO) {

        String newPassword = null;
        String newProfileImageFilename = null;

        // 1. 항상 조회 부터
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다"));

        // 2. 수정 권한 확인
        if(!userEntity.getId().equals(id)) {
            throw new Exception403("회원정보 수정 권한이 없습니다");
        }

        // 3. 로직 처리 1 - 사용자가 비밀번호를 입력 했을 경우 갱신
        if(updateDTO.getPassword() != null && !updateDTO.getPassword().isBlank()) {
            // 여기서 유효성 검사 해야 됨.
            updateDTO.validate();
            String rawPassword = updateDTO.getPassword();
            updateDTO.setPassword(passwordEncoder.encode(rawPassword));
        } else {
            updateDTO.setPassword(null);
        }

        // 4. 로직 처리 2 - 사용자가 새로운 이미지를 등록했을 경우
        if(updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()) {
            try {
                if(!FileUtil.isImageFile(updateDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다");
                }
                // 새 이미지 로컬 폴더에 저장 ( 중복되지 않을 이미지 파일 이름을 리턴)
                newProfileImageFilename = FileUtil.saveFile(updateDTO.getProfileImage(), FileUtil.IMAGES_DIR);
                updateDTO.setProfileImageFileName(newProfileImageFilename);

                // 기존 이미지 파일 삭제 해야 함(로컬에 계속 파일 쌓임)
                String oldProfileImageFileName = userEntity.getProfileImage();
                if(oldProfileImageFileName != null) {
                    FileUtil.deleteFile(oldProfileImageFileName, FileUtil.IMAGES_DIR);
                }
            } catch (IOException e) {
                throw new Exception400("파일 저장에 실패");
            }
        } else {
            updateDTO.setProfileImageFileName(userEntity.getProfileImage());
        }

        // 더티 체킹
        userEntity.update(updateDTO);
        return userEntity;
    }

    @Transactional
    public User 프로필이미지삭제(Integer id) {
        // 1. 정보 조회
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다")
        );
        // 2. 인가 처리
        if(userEntity.getId().equals(id) == false) {
            throw new Exception403("프로필 이미지 삭제 권한 없음");
        }

        // 3. 이미지가 등록되어 있으면 삭제 처리
        String profileImage = userEntity.getProfileImage();
        if(profileImage != null && !profileImage.isEmpty()) {
            // 내 서버 컴퓨터에 저장된(C://upload) 파일 삭제
            try {
                FileUtil.deleteFile(profileImage, FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                System.err.println("프로필 이미지 삭제시 오류 발생 " + e.getMessage());
            }
        }
        // 1차 캐쉬에 저장된 User 정보 수정 - 트랜잭션이 종료 되면 반영(더티 체킹)
        userEntity.setProfileImage(null);
        return userEntity;
    }

    public User 사용자이름조회(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * 카카오 인가 코드로 액세스 토큰 발급
     *
     * @param code 카카오 인가 코드
     * @return OAuth 액세스 토큰 정보
     */
    private UserResponse.OAuthToken 카카오액세스토큰발급(String code) {
        System.out.println("1. Kakao 인가 코드 수신 완료: " + code);

        RestTemplate restTemplate = new RestTemplate();

        // 헤더 생성 (MIME 타입 설정)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        // application/x-www-form-urlencoded 것은 데이터를 key1=value1&key2=value2 형태(HTML Form 태그 방식)로 보내겠다 의미 입니다.

        // 바디 생성 (MultiValueMap 사용)
        // RestTemplate은 바디(Body)에 MultiValueMap 타입의 객체가 들어오면, "아! 이걸 폼 데이터(key=value) 형식으로 변환해서 보내야겠구나"라고 인식하고 자동으로 변환
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", "http://localhost:8080/kakao-redirect");
        params.add("code", code);
        params.add("client_secret", kakaoClientSecret);

        // 바디 + 헤더 결합
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 요청 및 응답 받기
        ResponseEntity<UserResponse.OAuthToken> response = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                request,
                UserResponse.OAuthToken.class
        );

        UserResponse.OAuthToken oauthToken = response.getBody();
        log.info("Access Token 발급 완료: " + oauthToken.getAccessToken());

        return oauthToken;
    }


    /**
     * 카카오 액세스 토큰으로 프로필 정보 조회
     *
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 프로필 정보
     */
    private UserResponse.KakaoProfile 카카오프로필조회(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        // 헤더 생성 (Bearer + Access Token)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 요청 엔티티 생성 (바디 없음)
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // 요청 및 응답 받기
        ResponseEntity<UserResponse.KakaoProfile> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                request,
                UserResponse.KakaoProfile.class
        );

        UserResponse.KakaoProfile kakaoProfile = response.getBody();
        log.info("카카오 프로필 정보 수신 완료: " + kakaoProfile);

        return kakaoProfile;
    }




    /**
     * 카카오조회 및 자동회원 가입처리
     *
     * 비즈니스 로직:
     * 1. 고유한 username 생성 (닉네임_카카오ID)
     * 2. 기존 회원 여부 확인
     * 3. 신규 회원이면 자동 회원가입 처리
     * 4. 사용자 엔티티 반환
     *
     * @param kakaoProfile 카카오 프로필 정보
     * @return 사용자 엔티티
     */
    private User 카카오조회및자동회원가입처리(UserResponse.KakaoProfile kakaoProfile) {
        // 고유한 username 생성 (중복 방지용: 닉네임_카카오ID)
        String username = kakaoProfile.getKakaoAccount().getProfile().getNickname() + "_" + kakaoProfile.getId();
        // 회원 가입 여부 확인
        User user = 사용자이름조회(username);

        if (user == null) {
            log.info("기존 회원이 아님 자동 회원가입을 진행합니다.");
            // 회원가입용 엔티티 생성
            User newUser = User.builder()
                    .username(username)
                    // 임시 비밀번호도 일반 회원가입과 동일하게 BCrypt 해싱 처리
                    .password(passwordEncoder.encode(tencoKey)) // 임시 비밀번호 (DB Not Null 제약 대응, 해싱 저장)
                    .email(username + "@kakao.com") // 임의의 이메일 (선택사항)
                    .oAuthProvider(OAuthProvider.KAKAO) // ★ 로그인 경로 설정
                    .build();

            // 프로필 이미지가 있다면 설정
            String profileImage = kakaoProfile.getKakaoAccount().getProfile().getProfileImageUrl();
            if (profileImage != null && !profileImage.isEmpty()) {
                newUser.setProfileImage(profileImage); // URL 그대로 저장 (외부 링크)
            }

            // DB 에 INSERT 처리 (이 한 줄이 없으면 자바 객체만 만들어지고 DB 에는 안 들어감)
            // - save() 가 반환하는 객체는 영속 상태의 엔티티 (id 가 부여된 상태)
            // - 빌더에서 USER 권한이 이미 자동 부여되었기 때문에 별도 addRole 호출 불필요
            user = userRepository.save(newUser);
        } else {
            System.out.println("4. 이미 가입된 회원입니다. 로그인을 진행합니다.");
        }

        return user;
    }


    /**
     * 카카오 소셜 로그인 처리
     *
     * 비즈니스 로직:
     * 1. 인가 코드로 액세스 토큰 발급 요청
     * 2. 액세스 토큰으로 카카오 프로필 정보 조회
     * 3. 프로필 정보로 사용자 생성 또는 조회
     * 4. 로그인 처리 (사용자 엔티티 반환)
     *
     * 트랜잭션:
     * - 기본 트랜잭션 (읽기/쓰기)
     * - 회원가입 시 INSERT 쿼리 실행 가능
     *
     * @param code 카카오 인가 코드
     * @return 로그인한 사용자 엔티티
     */
    @Transactional
    public User 카카오소셜로그인(String code) {
        // 1. 인가 코드로 액세스 토큰 발급
        UserResponse.OAuthToken oauthToken = 카카오액세스토큰발급(code);

        // 2. 액세스 토큰으로 카카오 프로필 정보 조회
        UserResponse.KakaoProfile kakaoProfile = 카카오프로필조회(oauthToken.getAccessToken());

        // 3. 프로필 정보로 사용자 생성 또는 조회
        User user = 카카오조회및자동회원가입처리(kakaoProfile);

        // 4. 컨트롤러로 User 엔티티 반환 (세션 처리)
        return user;
    }

}




