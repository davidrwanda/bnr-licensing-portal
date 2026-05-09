package rw.bnr.licensing.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rw.bnr.licensing.domain.model.AuditLog;
import rw.bnr.licensing.domain.model.User;
import rw.bnr.licensing.domain.repository.AuditLogRepository;

import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(UUID applicationId,
                       User actor,
                       String action,
                       String previousStatus,
                       String newStatus,
                       String metadata) {
        AuditLog entry = new AuditLog(
                applicationId,
                actor.getId(),
                actor.getEmail(),
                action,
                previousStatus,
                newStatus,
                metadata
        );
        auditLogRepository.save(entry);
    }
}
