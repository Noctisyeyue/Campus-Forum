package com.campus.forum.listener;

import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮件发送消息队列监听器，消费 "mail" 队列中的邮件任务
 */
@Slf4j
@Component
@RabbitListener(queues = "mail")
public class MailQueueListener {

    /** JavaMail 邮件发送器 */
    @Resource
    JavaMailSender sender;

    /** 发件人邮箱地址，从配置文件读取 */
    @Value("${spring.mail.username}")
    String username;

    /**
     * 处理邮件发送，根据 type 区分注册/重置/修改邮箱
     * @param data 邮件信息（email, code, type）
     */
    @RabbitHandler
    public void sendMailMessage(Map<String, Object> data) {
        String email = data.get("email").toString();
        Integer code = (Integer) data.get("code");
        switch (data.get("type").toString()) {
            case "register" ->
                    createMessage("欢迎注册我们的网站",
                            "您的邮件注册验证码为: " + code + "，有效时间3分钟，为了保障您的账户安全，请勿向他人泄露验证码信息。",
                            email);
            case "reset" ->
                    createMessage("您的密码重置邮件",
                            "你好，您正在执行重置密码操作，验证码: " + code + "，有效时间3分钟，如非本人操作，请无视。",
                            email);
            case "modify" ->
                    createMessage("您的邮件修改验证邮件",
                            "您好，您正在绑定新的电子邮件地址，验证码: " + code + "，有效时间3分钟，如非本人操作，请无视。",
                            email);
            default -> {}
        }
    }

    /**
     * 封装 MIME 邮件并发送，发件人显示为"校园论坛"
     * @param title 标题
     * @param content 内容
     * @param email 收件人
     */
    private void createMessage(String title, String content, String email) {
        try {
            MimeMessage message = sender.createMimeMessage();
            // 启用 multipart	允许邮件包含 HTML、附件等复杂内容
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // new InternetAddress(邮箱地址, 显示名, 编码)
            helper.setFrom(new InternetAddress(username, "校园论坛", "UTF-8"));
            helper.setTo(email);
            helper.setSubject(title);
            helper.setText(content);
            sender.send(message);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", email, e);
        }
    }
}
