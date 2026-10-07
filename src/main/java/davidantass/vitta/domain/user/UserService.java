package davidantass.vitta.domain.user;

import davidantass.vitta.domain.BusinessRuleException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
        if (!passwordEncoder.matches(currentPassword, user.getPassword())
                || !newPassword.equals(confirmation)) {
            return false;
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        return true;
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
