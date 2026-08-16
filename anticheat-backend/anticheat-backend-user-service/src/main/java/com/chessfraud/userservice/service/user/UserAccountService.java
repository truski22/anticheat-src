package com.chessfraud.userservice.service.user;

import com.chessfraud.userservice.dto.user.LoginResult;
import com.chessfraud.userservice.dto.user.RegisterResult;
import com.chessfraud.userservice.dto.user.UserInfoResult;
import com.chessfraud.userservice.model.user.User;
import com.chessfraud.userservice.repository.user.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            return new LoginResult("KO");
        }
        User user = userOpt.get();
        String response = BCrypt.checkpw(password, user.getPassword()) ? "OK" : "KO";
        return new LoginResult(response);
    }

    public RegisterResult register(String name, String email, String password) {
        boolean nameExists = userRepository.existsByName(name);
        if (nameExists) {
            return new RegisterResult("UNV");
        }
        boolean emailExists = userRepository.existsByEmail(email);
        if (emailExists) {
            return new RegisterResult("ENV");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        user.setPassword(hashedPassword);
        userRepository.save(user);
        return new RegisterResult("OK");
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
