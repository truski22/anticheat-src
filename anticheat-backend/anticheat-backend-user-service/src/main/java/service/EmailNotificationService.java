package service;

import config.UserServiceConfig;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Sends transactional e-mails (e.g. verification codes) on behalf of the user-service.
 * Uses Gmail's SMTP relay with STARTTLS.
 */
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final UserServiceConfig config;

    /**
     * @param config service configuration that supplies SMTP credentials and the image path
     */
    public EmailNotificationService(UserServiceConfig config) {
        this.config = config;
    }

    /**
     * Sends an e-mail containing a numeric verification code to the specified address.
     * The message is an HTML e-mail with an inline branded image.
     *
     * @param toEmail recipient e-mail address
     * @param code    six-digit verification code to embed in the message body
     * @return {@code true} if the message was accepted by the SMTP server, {@code false} otherwise
     */
    public boolean sendVerificationCode(String toEmail, String code) {
        String smtpUser     = config.getSmtpUser();
        String smtpPassword = config.getSmtpPassword();

        if (smtpUser == null || smtpUser.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            log.error("[USER-SERVICE] FATAL: SMTP_USER or SMTP_PASSWORD not set.");
            return false;
        }

        String imagePath = config.getEmailImagePath();

        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpUser));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Password Change Request", "UTF-8");

            String htmlContent =
                    "<html lang='en'><body style='font-family: Arial, sans-serif; text-align: center;'>"
                    + "<h3>Your verification code is:</h3>"
                    + "<span style='display: block; font-size: 40px; font-weight: bold; color: #333;'>"
                    + code + "</span>"
                    + "<p style='margin: 20px 0; font-size: 16px; color: #555;'>"
                    + "Please enter this code to proceed.</p>"
                    + "<img src='cid:image1' alt='Verification Image' width='300' height='200'"
                    + " style='margin-top: 20px;'/>"
                    + "</body></html>";

            MimeMultipart multipart = new MimeMultipart("related");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            MimeBodyPart imagePart = new MimeBodyPart();
            DataSource fds = new FileDataSource(imagePath);
            imagePart.setDataHandler(new DataHandler(fds));
            imagePart.setHeader("Content-ID", "<image1>");
            imagePart.setDisposition(MimeBodyPart.INLINE);
            multipart.addBodyPart(imagePart);

            message.setContent(multipart);
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            log.error("[USER-SERVICE] Failed to send verification e-mail to '{}': {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}
