package com.example.accountbook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // 前后端分离场景下，将根路径转发到静态主页
        return "forward:/index.html";
    }
}

