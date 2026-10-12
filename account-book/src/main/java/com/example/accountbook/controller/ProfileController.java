package com.example.accountbook.controller;

import com.example.accountbook.entity.User;
import com.example.accountbook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;

    @Autowired
    public ProfileController(UserRepository userRepository) { this.userRepository = userRepository; }

    private Long currentUserId(HttpSession session){
        if (session == null) {
            throw new UnauthorizedException("会话不存在，请重新登录");
        }
        Object u = session.getAttribute("USER");
        if (u == null) {
            throw new UnauthorizedException("未登录，请先登录");
        }
        if (u instanceof Map<?,?> m && m.get("id")!=null) {
            try {
                return Long.valueOf(String.valueOf(m.get("id")));
            } catch (Exception e) {
                throw new UnauthorizedException("用户ID无效，请重新登录");
            }
        }
        throw new UnauthorizedException("用户信息无效，请重新登录");
    }
    
    // 自定义未授权异常
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
    private static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String,Object>> me(HttpSession session){
        Long id = currentUserId(session);
        User u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", u.getId());
        result.put("username", u.getUsername() != null ? u.getUsername() : "");
        result.put("nickname", u.getNickname() != null ? u.getNickname() : "");
        result.put("avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<Map<String,Object>> update(@RequestBody Map<String,String> body, HttpSession session){
        Long id = currentUserId(session);
        User u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 昵称/头像
        if (body.containsKey("nickname")) {
            String nickname = body.get("nickname");
            u.setNickname(nickname != null ? nickname : "");
        }
        if (body.containsKey("avatarUrl")) {
            String avatarUrl = body.get("avatarUrl");
            u.setAvatarUrl(avatarUrl != null ? avatarUrl : "");
        }
        
        userRepository.save(u);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", u.getId());
        result.put("username", u.getUsername() != null ? u.getUsername() : "");
        result.put("nickname", u.getNickname() != null ? u.getNickname() : "");
        result.put("avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
        return ResponseEntity.ok(result);
    }
}

