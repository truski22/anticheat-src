package com.chessfraud.userservice.service.user;

import com.chessfraud.userservice.dto.user.RegisterOutcome;
import com.chessfraud.userservice.dto.user.RegisterResult;
import com.chessfraud.userservice.model.user.User;
import com.chessfraud.userservice.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void registerLosingTheNameRaceReturnsUsernameTaken() {
        UserAccountService service = new UserAccountService(userRepository);


        when(userRepository.existsByName("alice")).thenReturn(false, true);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_pkey\""));

        RegisterResult result = service.register("alice", "alice@example.com", "hunter2hunter2");

        assertThat(result.getOutcome()).isEqualTo(RegisterOutcome.USERNAME_TAKEN);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void registerLosingTheEmailRaceReturnsEmailTaken() {
        UserAccountService service = new UserAccountService(userRepository);

        when(userRepository.existsByName("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false, true);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_email_key\""));

        RegisterResult result = service.register("bob", "bob@example.com", "hunter2hunter2");

        assertThat(result.getOutcome()).isEqualTo(RegisterOutcome.EMAIL_TAKEN);
    }

    @Test
    void registerWithNoPriorCheckHitAndCleanSaveSucceeds() {
        UserAccountService service = new UserAccountService(userRepository);

        when(userRepository.existsByName("carol")).thenReturn(false);
        when(userRepository.existsByEmail("carol@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResult result = service.register("carol", "carol@example.com", "hunter2hunter2");

        assertThat(result.getOutcome()).isEqualTo(RegisterOutcome.OK);
        assertThat(result.isSuccess()).isTrue();
    }
}
