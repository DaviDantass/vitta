package davidantass.vitta.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END
            FROM User u
            WHERE LOWER(u.email) = LOWER(:email) AND (:id IS NULL OR u.id <> :id)
            """)
    boolean isAlreadyRegistered(String email, Long id);
}
