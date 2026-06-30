package com.xushu.webtable.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailUtils {
    @Value("${spring.mail.username}")
    private String from;
    private final JavaMailSender javaMailSender;

    public MailUtils(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }
    public void sendMail(String email, String emailMsg) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from); // 发件人
        msg.setTo(email);                // 收件人
        msg.setSubject("【webtable网盘】邮箱验证码");
        msg.setText("您的验证码：" + emailMsg + "，5分钟有效");//这里写死了，还要判端这个邮箱在数据库里面是否存在，如果不存在就跳转登录界面，5分钟有效的逻辑也没有写
        javaMailSender.send(msg);
    }
}
