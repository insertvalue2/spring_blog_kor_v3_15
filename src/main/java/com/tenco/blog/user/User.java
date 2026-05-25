package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.NotEnoughPointException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Table(name = "user_tb")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    // 사용자명 중복 방지를 위한 유니크 제약 조건 설정
    @Column(unique = true)
    private String username;

    private String password;

    // 이메일 유니크 제약 — 동일 이메일 중복 가입 방지 (DB 레벨 안전망)
    // 애플리케이션 체크(UserService) 와 함께 이중 방어 구조.
    @Column(unique = true)
    private String email;
    // 엔티티가 영속화 될 때 자동으로 현재 시간을 주입해라 pc -> db
    @CreationTimestamp
    private Timestamp createdAt;

    // User 테이블에는 이미지 파일명만 저장할 예정 (실제 데이터는 내 서버 컴퓨터 로컬에 저장할 예정)
    @Column(nullable =  true) // null 허용, 기본값
    private String profileImage;  // 프로필 이미지는 선택 사항(회원 가입 시)

    // User : UserRole 연관 관계를 단방향 1 : N 구조 설계
    // JPA가 1 : N 구조일 경우 (User , UserRole) , @JoinColumn(name ="user_id") 의미는
    // 여기 테이블에 컬럼 user_id 생성해 라는 의미이다. 그런데 1 : N 구조에서 FK 컬럼이
    // 1 쪽 테이블에 생성되는 경우는 없다. 무조건 N 쪽에 FK 컬럼이 만들어져야 하기 때문에
    // 자동으로 User 테이블에 @JoinColumn("user_id") 하더라도 알아서 UserRole 컬럼을 지가 생성 한다

    /**
     * 사용자 권한 목록
     * User (1) : UserRole (N) 연관 관계를 정의 함
     *
     * 1. @OneToMany + @JoinColumn(name ="user_id")
     * - User 가 UserRole 리스트를 관리 합니다. (단방향)
     * - 실제 DB user_role_tb 테이블에 FK 컬럼은 user_id 명이 user_role_tb 생성 된다.
     *
     * 2. CascadeType.ALL (운명 공동체)
     * Java 기준에서 User 저장하면 Role 도 자동 저장되고, User 삭제하면 가지고 있던
     * Role들도 다 삭제가 됩니다. DB 에서 실제 delete 쿼리가 발생 됩니다.
     *
     * 3. orphanRemoval (리스와 DBㄹ르 동기화)
     *  DB 에서 실제 delete 쿼리가 발생 됩니다. = true 처리
     *
     * 4. fetch = FetchType.EAGER (특별취급)
     * 데이터 양이 얼마 되지 않습니다. 그래서 한번에 데이터를 채워서 가지고 오는것이
     * 편리하다
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserRole> roles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // null 허용 안함
    @ColumnDefault("'LOCAL'") // 어노테이션으로 디폴트값 선언 방법 ( 문자열 일 경우 ' ' 반드시 사용)
    private OAuthProvider oAuthProvider;

    // 보유 포인트 (유료 게시글 구매에 사용) — 기본값 0
    @ColumnDefault("0")
    private Integer point = 0;

    @Builder
    public User(Integer id, String username, String password,
                String email,
                String profileImage,
                OAuthProvider oAuthProvider,
                List<UserRole> roles,
                Integer point) {       // ← roles, point 매개변수 추가
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.profileImage = profileImage;

        // 1) roles 가 null 이면 빈 리스트로 초기화
        //    (외부에서 빌더에 roles 안 넘기면 null 이 들어오므로 NPE 방어)
        this.roles = (roles != null) ? roles : new ArrayList<>();

        // 2) roles 가 비어있으면 — 권한 없는 사용자 생성 방지를 위해
        //    USER 권한을 자동으로 주입
        if (this.roles.isEmpty()) {
            this.roles.add(UserRole.builder().role(Role.USER).build());
        }

        // 3) oAuthProvider 가 null 이면 LOCAL 로 기본값 설정
        //    (회원가입 흐름에서 명시적으로 안 주는 경우 대비)
        this.oAuthProvider = (oAuthProvider != null) ? oAuthProvider : OAuthProvider.LOCAL;

        // 4) point 가 null 이면 0 으로 기본값 설정
        this.point = (point != null) ? point : 0;
    }

    // 편의 기능 추가 - 회원 정보 수정
    public void update(UserRequest.UpdateDTO updateDTO) {
        if(updateDTO.getPassword() != null) {
            // 암호화 변경되어 들어 옴
            this.password = updateDTO.getPassword();
        }

        if(updateDTO.getProfileImageFileName() != null ) {
            // 신규 파일 생성해서 들어 옴
            this.profileImage = updateDTO.getProfileImageFileName();
        }
    }

    // ===================== 포인트 관련 편의 메서드 =====================

    /**
     * 포인트를 차감한다. (유료 게시글 구매 시)
     * 포인트가 부족하면 Exception400 을 던져 트랜잭션을 롤백시킨다.
     *
     * @param amount 차감할 포인트
     */
    public void deductPoint(Integer amount) {
        // amount <= 0 은 개발자 실수/잘못된 호출 → 일반 400 (이전 페이지로)
        if (amount == null || amount <= 0) {
            throw new Exception400("차감할 포인트는 0보다 커야 합니다");
        }
        if (this.point == null) {
            this.point = 0;
        }
        // 포인트 부족은 사용자가 충전하면 해결됨 → 전용 예외 (마이페이지로 유도)
        if (this.point < amount) {
            throw new NotEnoughPointException("포인트가 부족합니다. 현재 포인트: " + this.point + " (마이페이지에서 충전 후 다시 시도해주세요)");
        }
        this.point -= amount;
    }

    /**
     * 포인트를 충전한다. (테스트용 충전 API 에서 사용)
     *
     * @param amount 충전할 포인트
     */
    public void chargePoint(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new Exception400("충전할 포인트는 0보다 커야 합니다");
        }
        if (this.point == null) {
            this.point = 0;
        }
        this.point += amount;
    }


    // ==  User 엔티티에 권한 관련 편의 기능 만들기 보기 ==
    // Role 추가 편의 메서드
    // Role.ADMIN, Role.USER
    public void addRole(Role role) {
        //this.roles.get(0) = new UserRole(1, Role.USER);
        this.roles.add(UserRole.builder().role(role).build());
    }

    // 해당 Role 을 가지고 있는 여부 확인
    // boolean isAdmin = user.haRole(Role.USER);
    public boolean hasRole(Role role) {
        // 1. 방어적 코드 작성
        if (this.roles == null || this.roles.isEmpty()) {
            // Role(해당 유저에 권한이) 자체가 설정 되지 않은 상태
            return false;
        }
        for(UserRole userRole: this.roles) {
            if(userRole.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    // 관리지 여부 확인 편의 메서드 - 머스태치에서 is 생략하고 admin으로 접근 가능합니다.
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    // 머스태치 화면에서 사용할 편의 메서드 1
    public String getRoleDisplay() {
        return isAdmin() ? "ADMIN" : "USER";
    }

    // 머스태치 화면에서 사용할 편의 메서드 2
    // OAuthProvider 값에 따라서 경로 변수를 다르게 리턴
    public String getProfilePath() {
        if(this.profileImage == null) {
            return null;
        }
        // 이미지 경로가 http 로 시작 (소셜 가입)
        if(this.profileImage.startsWith("http")) {
            return this.profileImage;
        }
        // 로컬 이미지(서버 기준 경로)
        return "/images/" + this.profileImage;
    }

    // 머스태치 화면에서 사용할 편의 메서드 3
    public boolean isLocal() {
        // true -> 이메일 가입자를 의미 함
        return this.oAuthProvider == OAuthProvider.LOCAL;
    }

}
