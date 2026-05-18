package com.tenco.blog._core.config;

import com.tenco.blog._core.interceptor.AdminInterceptor;
import com.tenco.blog._core.interceptor.LoginInterceptor;
import com.tenco.blog._core.interceptor.SessionInterceptor;
import com.tenco.blog._core.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

// 자바 코드로 스프링 부트 설정 파일을 다둘 수 있다.

// @Component
@Configuration // IoC 대상 - 하나 이상의 IoC 처리를 하고 싶을 때 사용 한다.
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired // DI 처리
    private LoginInterceptor loginInterceptor;
    @Autowired // DI 처리
    private SessionInterceptor sessionInterceptor;
    @Autowired
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 화면에 SessionUser 정보를 내려줄 사용 됨. 
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/**"); // 모든 URL 요청서 동작 함


        // 인증 처리 인터셉터 동작 함
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/board/**", "/user/**", "/reply/**", "/admin/**")
                .excludePathPatterns(
                    // 로그인 관련(인증이 필요 없는 페이지)
                    "/login-form",  // 로그인 화면 요청 시
                    "/join-form",   // 회원 가입 화면 요청 시
                    "/logout", // 로그아웃

                    // 게시글 조회 관련 (인증 없이도 볼 수 있는 페이지)
                    "/board/list",  // 게시글 목록 화면 요청 시
                    "/"          ,  // 메인 페이지
                    "/index"          ,  // 메인 페이지
                    "/board/{id:\\d+}", // 게시글 상세보기( 숫자 ID만 허용)

                    // 정적 리소스 (CSS, JS, 이미지 등)
                    "/css/**",          // CSS 파일 제외
                    "/js/**",           // JS 파일 제외
                    "/images/**",      //  이미지 파일 제외
                    "/favicon.ico",    // 파비콘 제외

                    // H2 데이터베시스 콘솔( 개발 환경용)
                    "/h2-console/**"    // H2 콘솔 접근
                );

        // 관리자 페이지 요청이 들어 왔을 때 1단계 로그인 여부 확인, 2단계 Role 확인해서
        // ADMIN 일 경우만 관리자 페이지로 이동 가능하게 처리
        registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/**");
    }

    // 정적 리소스 핸들러 설정
    // 외부 사용자가 내 서버 컴퓨터에 특정 경로를 바로 확인을 할 수 있게 한다면
    // 보안상 취약 할 수 있습니다. ( 사용자에게는 가짜 경로를 보여주고 내부에서는
    // 실정 경로를 찾을 수 있도록 처리 하는 기법(보안상)

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
       String externalPath = Paths.get(FileUtil.IMAGES_DIR).toString();
       registry.addResourceHandler("/images/**")
                // file: 추가 하기
               .addResourceLocations("file:" + externalPath);
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}


