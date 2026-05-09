package rw.bnr.licensing.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.bnr.licensing.domain.model.AuditLog;

import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
