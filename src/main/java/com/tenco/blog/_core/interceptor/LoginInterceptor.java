package com.tenco.blog._core.interceptor;

import com.tenco.blog._core.errors.Exception401;
import com.tenco.blog.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component // IoC 대상 - 싱글톤 패턴
public  class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
      HttpSession session = request.getSession();
      User sessionUser = (User) session.getAttribute("sessionUser");
      if(sessionUser == null) {
          throw new Exception401("로그인 먼저 해주세요");
      }
        return true;
    }
}
