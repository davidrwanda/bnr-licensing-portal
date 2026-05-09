package rw.bnr.licensing.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author David NTAMAKEMWA
 *
 * Append-only. No setter methods exist for this class by design.
 * The database user is granted INSERT-only on this table (see V2 migration).
 * BIGSERIAL PK means deletions are detectable through ID gaps.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "audit_logs_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "application_id")
    private java.util.UUID applicationId;

    @Column(name = "actor_id", nullable = false)
    private java.util.UUID actorId;

    // Denormalised so that history is readable even after account changes
    @Column(nullable = false)
    private String actorEmail;

    @Column(nullable = false)
    private String action;

    private String previousStatus;
    private String newStatus;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Set by the database clock via DEFAULT NOW() — not the application clock
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog(java.util.UUID applicationId,
                    java.util.UUID actorId,
                    String actorEmail,
                    String action,
                    String previousStatus,
                    String newStatus,
                    String metadata) {
        this.applicationId = applicationId;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }
}
