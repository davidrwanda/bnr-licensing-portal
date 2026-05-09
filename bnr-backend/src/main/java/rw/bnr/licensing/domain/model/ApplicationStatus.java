package rw.bnr.licensing.domain.model;

/**
 * @author David NTAMAKEMWA
 */
public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    INFO_REQUESTED,
    RESUBMITTED,
    REVIEW_COMPLETE,
    APPROVED,
    REJECTED,
    WITHDRAWN;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == WITHDRAWN;
    }
}
