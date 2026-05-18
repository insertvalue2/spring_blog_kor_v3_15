package com.tenco.blog._core.interceptor;

import com.tenco.blog._core.errors.Exception401;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.util.Define;
import com.tenco.blog.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 세션에서 로그인 사용자 정보 조회
        // 단, AdminInterceptor 동작하기 전에 LoginInterceptor 가 먼저 선언이 되어 동작하기 된다.
        // 즉 로그인은 보장 되어 있음 ! (순서 중요)
        HttpSession session = request.getSession();
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);

        if(sessionUser == null) {
            throw new Exception401("로그인이 필요합니다");
        }

        if(!sessionUser.isAdmin()) {
            throw new Exception403("관리자 권한이 필요합니다");
        }


        return true;
    }
}
