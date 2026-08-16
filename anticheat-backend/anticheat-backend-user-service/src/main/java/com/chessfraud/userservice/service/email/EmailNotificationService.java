package com.chessfraud.userservice.service.email;

import com.chessfraud.userservice.config.UserServiceConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional e-mails (e.g. verification codes) on behalf of the user-service.
 * Uses Gmail's SMTP relay with STARTTLS via Spring's {@link JavaMailSender}.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final UserServiceConfig config;

    public EmailNotificationService(JavaMailSender mailSender, UserServiceConfig config) {
        this.mailSender = mailSender;
        this.config = config;
    }

    /**
     * Sends an e-mail containing a numeric verification code to the specified address.
     *
     * @param toEmail recipient e-mail address
     * @param code    six-digit verification code to embed in the message body
     * @return {@code true} if the message was accepted by the SMTP server, {@code false} otherwise
     */
    public boolean sendVerificationCode(String toEmail, String code) {
        if (isBlank(config.getSmtpUser()) || isBlank(config.getSmtpPassword())) {
            log.error("[USER-SERVICE] FATAL: SMTP_USER or SMTP_PASSWORD not set.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(config.getSmtpUser());
            helper.setTo(toEmail);
            helper.setSubject("Password Change Request");

            String htmlContent =
                    "<html lang='en'><body style='font-family: Arial, sans-serif; text-align: center;'>"
                    + "<h3>Your verification code is:</h3>"
                    + "<span style='display: block; font-size: 40px; font-weight: bold; color: #333;'>"
                    + code + "</span>"
                    + "<p style='margin: 20px 0; font-size: 16px; color: #555;'>"
                    + "Please enter this code to proceed.</p>"
                    + "</body></html>";
            helper.setText(htmlContent, true);

            mailSender.send(message);
            return true;
        } catch (MessagingException | MailException e) {
            log.error("[USER-SERVICE] Failed to send verification e-mail to '{}': {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
