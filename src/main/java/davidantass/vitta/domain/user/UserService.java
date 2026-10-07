package davidantass.vitta.domain.user;

import davidantass.vitta.domain.BusinessRuleException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock = Clock.systemUTC();

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username.replace("@", " @")));
    }

    @Transactional
    public Long save(String name, String email, String password, Profile profile) {
        validateEmail(email, null);
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(name, email, encodedPassword, profile);
        repository.saveAndFlush(user);
        return user.getId();
    }

    @Transactional
    public void updateDetails(Long id, String name, String email) {
        var user = findUser(id);
        validateEmail(email, id);
        user.updateDetails(name, email);
    }

    @Transactional
    public boolean changePassword(User user, String currentPassword, String newPassword, String confirmation) {
        var persistedUser = repository.findById(user.getId()).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, persistedUser.getPassword())
                || !newPassword.equals(confirmation)
                || passwordEncoder.matches(newPassword, persistedUser.getPassword())) {
            return false;
        }
        persistedUser.changePassword(passwordEncoder.encode(newPassword));
        return true;
    }

    @Transactional
    public void requestPasswordReset(String email, String siteUrl) {
        repository.findByEmailIgnoreCase(email).ifPresent(user -> {
            byte[] tokenBytes = new byte[32];
            secureRandom.nextBytes(tokenBytes);
            String token = HexFormat.of().formatHex(tokenBytes);
            user.createPasswordResetToken(hashToken(token), LocalDateTime.now(clock).plusHours(1));
            emailService.sendPasswordResetEmail(user, siteUrl + "/reset-password?token=" + token);
        });
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword, String confirmation) {
        var user = repository.findByPasswordResetToken(hashToken(token)).orElse(null);
        if (user == null || !user.hasValidPasswordResetToken(hashToken(token), LocalDateTime.now(clock))
                || !newPassword.equals(confirmation)
                || passwordEncoder.matches(newPassword, user.getPassword())) {
            return false;
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        user.clearPasswordResetToken();
        return true;
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash password reset token.", exception);
        }
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findUser(id));
    }

    private void validateEmail(String email, Long id) {
        if (repository.isAlreadyRegistered(email, id)) {
            throw new BusinessRuleException("Email already registered for another user!");
        }
    }

    private User findUser(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));
    }
}
