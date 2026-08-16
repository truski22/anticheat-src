package com.chessfraud.userservice.user.service;

import com.chessfraud.userservice.user.dto.LoginOutcome;
import com.chessfraud.userservice.user.dto.LoginResult;
import com.chessfraud.userservice.user.dto.RegisterOutcome;
import com.chessfraud.userservice.user.dto.RegisterResult;
import com.chessfraud.userservice.user.dto.UserInfoResult;
import com.chessfraud.userservice.user.model.User;
import com.chessfraud.userservice.user.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;


/**
 * Provides account-management operations
 */
@Service
public class UserAccountService{

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserRepository userRepository;

    public UserAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResult login(String name, String password) {
        Optional<User> userOpt = userRepository.findByName(name);
        if (userOpt.isEmpty()) {
            return new LoginResult(LoginOutcome.INVALID_CREDENTIALS);
        }
        User user = userOpt.get();
        LoginOutcome outcome = BCrypt.checkpw(password, user.getPassword())
                ? LoginOutcome.OK
                : LoginOutcome.INVALID_CREDENTIALS;
        return new LoginResult(outcome);
    }

    public RegisterResult register(String name, String email, String password) {
        if (userRepository.existsByName(name)) {
            return new RegisterResult(RegisterOutcome.USERNAME_TAKEN);
        }
        if (userRepository.existsByEmail(email)) {
            return new RegisterResult(RegisterOutcome.EMAIL_TAKEN);
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException raceLoser) {
            log.warn("[USER-SERVICE] Concurrent registration detected for name='{}', email='{}'", name, email);
            if (userRepository.existsByName(name)) {
                return new RegisterResult(RegisterOutcome.USERNAME_TAKEN);
            }
            if (userRepository.existsByEmail(email)) {
                return new RegisterResult(RegisterOutcome.EMAIL_TAKEN);
            }
            throw raceLoser;
        }

        return new RegisterResult(RegisterOutcome.OK);
    }

    public UserInfoResult getUserInfo(String name) {
        Optional<User> userOpt = userRepository.findByName(name);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        return new UserInfoResult(
                user.getEmail(),
                user.getTotalGames(),
                user.getCheatedGames(),
                user.getFairGames());
    }

    public boolean changePassword(String name, String newPassword) {
        Optional<User> userOpt = userRepository.findByName(name);
        if (userOpt.isEmpty()) {
            return false;
        }
        return updatePassword(userOpt.get(), newPassword);
    }

    public boolean changePasswordByEmail(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        return updatePassword(userOpt.get(), newPassword);
    }

    private boolean updatePassword(User user, String newPassword) {
        if (BCrypt.checkpw(newPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
        userRepository.save(user);
        return true;
    }

    public boolean usernameExists(String name) {
        return userRepository.existsByName(name);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
