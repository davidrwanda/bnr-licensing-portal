package rw.bnr.licensing.domain.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.bnr.licensing.domain.model.Application;
import rw.bnr.licensing.domain.model.ApplicationStatus;
import rw.bnr.licensing.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByApplicant(User applicant);

    List<Application> findByReviewer(User reviewer);

    List<Application> findByStatus(ApplicationStatus status);

    @Query("SELECT a FROM Application a WHERE a.status IN :statuses ORDER BY a.createdAt DESC")
    List<Application> findByStatusIn(@Param("statuses") List<ApplicationStatus> statuses);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT a FROM Application a WHERE a.id = :id")
    Optional<Application> findByIdWithLock(@Param("id") UUID id);
}
