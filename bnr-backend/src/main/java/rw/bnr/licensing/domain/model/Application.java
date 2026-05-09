package rw.bnr.licensing.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(nullable = false)
    private String institutionName;

    @Column(nullable = false)
    private String institutionType;

    @Column(columnDefinition = "TEXT")
    private String contactAddress;

    @Column(columnDefinition = "TEXT")
    private String businessDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    // Optimistic locking — every write must include the current version
    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @Column(columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(columnDefinition = "TEXT")
    private String decisionReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    private Instant submittedAt;
    private Instant decidedAt;

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
