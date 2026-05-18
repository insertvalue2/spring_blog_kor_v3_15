package com.tenco.blog.admin;

import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.util.Define;
import com.tenco.blog.user.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin/dashboard")
    public String dashboardPage(HttpSession session, Model model) {

        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        model.addAttribute("admin", sessionUser);
        return "admin/dashboard";
    }

}
