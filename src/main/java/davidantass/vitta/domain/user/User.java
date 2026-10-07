package davidantass.vitta.domain.user;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiresAt;
    @Enumerated(EnumType.STRING)
    private Profile profile;

    public User() {
    }

    public User(String name, String email, String password) {
        updateDetails(name, email);
        this.password = password;
    }


    public User(String name, String email, String password, Profile profile) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.profile = profile;
    }

    /**
     * @param name
     * @param email
     */
    public void updateDetails(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }
    public String getEmail() {
        return email;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return profile == null
                ? Collections.emptySet()
                : Collections.singleton(new SimpleGrantedAuthority("ROLE_" + profile.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void createPasswordResetToken(String token, LocalDateTime expiresAt) {
        this.passwordResetToken = token;
        this.passwordResetTokenExpiresAt = expiresAt;
    }

    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetTokenExpiresAt = null;
    }

    public boolean hasValidPasswordResetToken(String token, LocalDateTime now) {
        return passwordResetToken != null && passwordResetToken.equals(token)
                && passwordResetTokenExpiresAt != null && passwordResetTokenExpiresAt.isAfter(now);
    }

    public Profile getProfile() {
        return profile;
    }
}
