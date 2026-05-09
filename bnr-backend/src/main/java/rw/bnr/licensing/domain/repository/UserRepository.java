package rw.bnr.licensing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.bnr.licensing.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
