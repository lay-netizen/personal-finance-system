package com.example.accountbook.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

    private static class Entry {
        String code;
        long expireAt;
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private static final long TTL_MS = 2 * 60 * 1000; // 2分钟

    public Map<String, String> generate() {
        String code = randomCode(5);
        String id = UUID.randomUUID().toString();
        String img = renderBase64Png(code);
        Entry e = new Entry();
        e.code = code.toLowerCase();
        e.expireAt = Instant.now().toEpochMilli() + TTL_MS;
        store.put(id, e);
        return Map.of("captchaId", id, "imageBase64", img);
    }

    public boolean verify(String id, String input) {
        if (id == null || input == null) return false;
        Entry e = store.remove(id);
        if (e == null) return false;
        if (Instant.now().toEpochMilli() > e.expireAt) return false;
        return e.code.equals(input.toLowerCase());
    }

    private String randomCode(int len) {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt((int)(Math.random()*chars.length())));
        return sb.toString();
    }

    private String renderBase64Png(String code) {
        int w = 120, h = 40;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0xFFEC99)); g.fillRect(0,0,w,h);
        g.setColor(new Color(0x7D5200));
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString(code, 18, 26);
        g.dispose();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败");
        }
    }
}
