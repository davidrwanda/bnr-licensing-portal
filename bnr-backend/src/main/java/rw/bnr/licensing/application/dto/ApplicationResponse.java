package rw.bnr.licensing.application.dto;

import rw.bnr.licensing.domain.model.Application;
import rw.bnr.licensing.domain.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public record ApplicationResponse(
        UUID id,
        String institutionName,
        String institutionType,
        String contactAddress,
        String businessDescription,
        ApplicationStatus status,
        Integer version,
        String reviewerNotes,
        String decisionReason,
        UserSummary applicant,
        UserSummary reviewer,
        UserSummary approver,
        Instant createdAt,
        Instant submittedAt,
        Instant decidedAt
) {
    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(),
                a.getInstitutionName(),
                a.getInstitutionType(),
                a.getContactAddress(),
                a.getBusinessDescription(),
                a.getStatus(),
                a.getVersion(),
                a.getReviewerNotes(),
                a.getDecisionReason(),
                a.getApplicant() != null ? UserSummary.from(a.getApplicant()) : null,
                a.getReviewer() != null ? UserSummary.from(a.getReviewer()) : null,
                a.getApprover() != null ? UserSummary.from(a.getApprover()) : null,
                a.getCreatedAt(),
                a.getSubmittedAt(),
                a.getDecidedAt()
        );
    }

    public record UserSummary(UUID id, String fullName, String email) {
        public static UserSummary from(rw.bnr.licensing.domain.model.User u) {
            return new UserSummary(u.getId(), u.getFullName(), u.getEmail());
        }
    }
}
