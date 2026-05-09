package rw.bnr.licensing.application.service;

import org.springframework.stereotype.Component;
import rw.bnr.licensing.common.exception.InvalidTransitionException;
import rw.bnr.licensing.domain.model.Application;
import rw.bnr.licensing.domain.model.ApplicationStatus;
import rw.bnr.licensing.domain.model.Role;
import rw.bnr.licensing.domain.model.User;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author David NTAMAKEMWA
 *
 * Encodes every legal state transition. All mutation goes through here before
 * the repository is touched. Illegal transitions throw at the service layer,
 * not in the UI.
 */
@Component
public class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ApplicationStatus.DRAFT,
                EnumSet.of(ApplicationStatus.SUBMITTED, ApplicationStatus.WITHDRAWN));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.SUBMITTED,
                EnumSet.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.WITHDRAWN));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.UNDER_REVIEW,
                EnumSet.of(ApplicationStatus.INFO_REQUESTED, ApplicationStatus.REVIEW_COMPLETE, ApplicationStatus.WITHDRAWN));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.INFO_REQUESTED,
                EnumSet.of(ApplicationStatus.RESUBMITTED, ApplicationStatus.WITHDRAWN));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.RESUBMITTED,
                EnumSet.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.WITHDRAWN));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.REVIEW_COMPLETE,
                EnumSet.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED));
        // Terminal states — no outbound edges
        ALLOWED_TRANSITIONS.put(ApplicationStatus.APPROVED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));
    }

    public void assertTransitionAllowed(ApplicationStatus from, ApplicationStatus to) {
        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ApplicationStatus.class));
        if (!allowed.contains(to)) {
            throw new InvalidTransitionException(
                    String.format("Cannot transition from %s to %s", from, to));
        }
    }

    public void assertActorCanTransition(Application application, User actor, ApplicationStatus targetStatus) {
        Role role = actor.getRole();

        switch (targetStatus) {
            case SUBMITTED -> {
                requireRole(role, Role.APPLICANT, targetStatus);
                requireOwnership(application, actor);
            }
            case WITHDRAWN -> {
                // Any owner-role can withdraw their own application before a terminal state
                requireRole(role, Role.APPLICANT, targetStatus);
                requireOwnership(application, actor);
            }
            case UNDER_REVIEW -> requireRole(role, Role.ADMIN, targetStatus);
            case INFO_REQUESTED, REVIEW_COMPLETE -> {
                requireRole(role, Role.REVIEWER, targetStatus);
                requireAssignedReviewer(application, actor);
            }
            case RESUBMITTED -> {
                requireRole(role, Role.APPLICANT, targetStatus);
                requireOwnership(application, actor);
            }
            case APPROVED, REJECTED -> {
                requireRole(role, Role.APPROVER, targetStatus);
                // Separation of duties: the reviewer cannot also be the approver
                assertNotSamePersonAsReviewer(application, actor);
            }
            default -> throw new InvalidTransitionException("Unrecognised target status: " + targetStatus);
        }
    }

    private void requireRole(Role actual, Role required, ApplicationStatus target) {
        if (actual != required) {
            throw new rw.bnr.licensing.common.exception.ForbiddenException(
                    String.format("Only a %s can move an application to %s", required, target));
        }
    }

    private void requireOwnership(Application application, User actor) {
        if (!application.getApplicant().getId().equals(actor.getId())) {
            throw new rw.bnr.licensing.common.exception.ForbiddenException(
                    "You can only act on your own applications");
        }
    }

    private void requireAssignedReviewer(Application application, User actor) {
        if (application.getReviewer() == null ||
                !application.getReviewer().getId().equals(actor.getId())) {
            throw new rw.bnr.licensing.common.exception.ForbiddenException(
                    "You are not the assigned reviewer for this application");
        }
    }

    private void assertNotSamePersonAsReviewer(Application application, User actor) {
        if (application.getReviewer() != null &&
                application.getReviewer().getId().equals(actor.getId())) {
            throw new rw.bnr.licensing.common.exception.ForbiddenException(
                    "The reviewer of an application cannot make the final approval decision on it");
        }
    }
}
