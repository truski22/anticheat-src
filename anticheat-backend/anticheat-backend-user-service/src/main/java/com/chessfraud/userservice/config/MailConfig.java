package com.chessfraud.userservice.config;

import com.chessfraud.userservice.service.email.EmailNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Builds the {@link JavaMailSender} used by {@link EmailNotificationService}.
 * Gmail's SMTP relay with STARTTLS, same as the original hand-rolled Session/Transport setup.
 */
@Configuration
public class MailConfig {

    private final UserServiceConfig config;

    public MailConfig(UserServiceConfig config) {
        this.config = config;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(config.getSmtpUser());
        mailSender.setPassword(config.getSmtpPassword());

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }
}
