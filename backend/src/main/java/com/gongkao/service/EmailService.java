package com.gongkao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final long CODE_TTL_MINUTES = 5;

    public void sendVerificationCode(String email, String type) {
        // 频率限制：同一邮箱+类型 60 秒内只能发一次
        String rateKey = "verify:rate:" + email + ":" + type;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            throw new RuntimeException("发送过于频繁，请60秒后重试");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        String redisKey = "verify:" + email + ":" + type;

        redisTemplate.opsForValue().set(redisKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateKey, "1", 60, TimeUnit.SECONDS);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("BALA公考 - 验证码");
        message.setText("您的验证码是：" + code + "\n\n验证码5分钟内有效，请勿泄露给他人。");

        try {
            mailSender.send(message);
            log.info("验证码已发送至 {} (type={})", email, type);
        } catch (Exception e) {
            log.error("发送邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    public boolean verifyCode(String email, String type, String code) {
        String redisKey = "verify:" + email + ":" + type;
        String stored = redisTemplate.opsForValue().get(redisKey);
        if (stored != null && stored.equals(code)) {
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }
}
