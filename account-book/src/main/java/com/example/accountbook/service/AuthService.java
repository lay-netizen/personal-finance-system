package com.example.accountbook.service;

import com.example.accountbook.entity.User;
import com.example.accountbook.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;

@Service
public class AuthService {

    private final UserRepository userRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initRoot() {
        if (!userRepository.existsByUsername("root")) {
            User u = new User();
            u.setUsername("root");
            u.setPasswordHash(BCrypt.hashpw("123456", BCrypt.gensalt()));
            userRepository.save(u);
        }
    }

    public User register(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        return userRepository.save(u);
    }

    public User login(String username, String rawPassword) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!BCrypt.checkpw(rawPassword, u.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }
        return u;
    }
}
