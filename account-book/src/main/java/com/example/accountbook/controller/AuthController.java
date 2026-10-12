package com.example.accountbook.controller;

import com.example.accountbook.entity.User;
import com.example.accountbook.service.AuthService;
import com.example.accountbook.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Autowired
    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ResponseEntity<Map<String, String>> captcha() {
        return ResponseEntity.ok(captchaService.generate());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaId = body.get("captchaId");
        String captchaCode = body.get("captchaCode");
        if (!captchaService.verify(captchaId, captchaCode)) {
            throw new RuntimeException("验证码不正确或已过期");
        }
        User u = authService.register(username, password);
        return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaId = body.get("captchaId");
        String captchaCode = body.get("captchaCode");
        if (!captchaService.verify(captchaId, captchaCode)) {
            throw new RuntimeException("验证码不正确或已过期");
        }
        User u = authService.login(username, password);
        session.setAttribute("USER", Map.of("id", u.getId(), "username", u.getUsername()));
        return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername()));
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String,String>> uploadAvatar(@RequestParam("file") MultipartFile file){
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mime = file.getContentType()!=null? file.getContentType(): "image/png";
            return ResponseEntity.ok(Map.of("avatarUrl", "data:"+mime+";base64,"+base64));
        } catch (Exception e) {
            throw new RuntimeException("头像上传失败");
        }
    }

    /** 检查登录状态 */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> check(HttpSession session) {
        if (session == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "会话不存在"));
        }
        Object userObj = session.getAttribute("USER");
        if (userObj == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        if (userObj instanceof Map<?, ?> userMap) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", userMap.get("id"));
            result.put("username", userMap.get("username"));
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "会话无效"));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }
}
