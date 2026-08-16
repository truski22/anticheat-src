package com.chessfraud.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration holder for the user-service
 */
@ConfigurationProperties(prefix = "user-service")
public class UserServiceConfig {

    private String smtpUser;
    private String smtpPassword;

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }
}
