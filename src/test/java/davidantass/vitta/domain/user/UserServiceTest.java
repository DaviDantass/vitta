package davidantass.vitta.domain.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository repository;
    @Mock EmailService emailService;
    private UserService service;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        service = new UserService(repository, encoder, emailService);
    }

    private User authenticatedUser() {
        return mock(User.class);
    }

    @Test
    void changesPasswordOnlyAfterVerifyingCurrentPassword() {
        var user = authenticatedUser();
        var persisted = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(persisted.getPassword()).thenReturn(encoder.encode("old-password"));
        when(repository.findById(7L)).thenReturn(Optional.of(persisted));

        assertThat(service.changePassword(user, "old-password", "new-password", "new-password")).isTrue();
        verify(persisted).changePassword(argThat(hash -> encoder.matches("new-password", hash)));
    }

    @Test
    void rejectsWrongCurrentPasswordAndDoesNotPersist() {
        var user = authenticatedUser();
        var persisted = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(persisted.getPassword()).thenReturn(encoder.encode("old-password"));
        when(repository.findById(7L)).thenReturn(Optional.of(persisted));

        assertThat(service.changePassword(user, "wrong-password", "new-password", "new-password")).isFalse();
        verify(persisted, never()).changePassword(anyString());
    }

    @Test
    void rejectsDifferentConfirmationAndDoesNotPersist() {
        var user = authenticatedUser();
        var persisted = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(persisted.getPassword()).thenReturn(encoder.encode("old-password"));
        when(repository.findById(7L)).thenReturn(Optional.of(persisted));

        assertThat(service.changePassword(user, "old-password", "new-password", "different-password")).isFalse();
        verify(persisted, never()).changePassword(anyString());
    }

    @Test
    void rejectsReusingTheCurrentPassword() {
        var user = authenticatedUser();
        var persisted = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(persisted.getPassword()).thenReturn(encoder.encode("old-password"));
        when(repository.findById(7L)).thenReturn(Optional.of(persisted));

        assertThat(service.changePassword(user, "old-password", "old-password", "old-password")).isFalse();
        verify(persisted, never()).changePassword(anyString());
    }
}
