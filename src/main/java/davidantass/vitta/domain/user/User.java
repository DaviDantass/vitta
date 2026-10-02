package davidantass.vitta.domain.user;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
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
        return java.util.List.of();
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

    public Profile getProfile() {
        return profile;
    }
}
