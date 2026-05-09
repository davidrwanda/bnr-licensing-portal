package rw.bnr.licensing.audit.dto;

import rw.bnr.licensing.domain.model.AuditLog;

import java.time.Instant;

/**
 * @author David NTAMAKEMWA
 */
public record AuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String previousStatus,
        String newStatus,
        String metadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorEmail(),
                log.getAction(),
                log.getPreviousStatus(),
                log.getNewStatus(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }
}
