package rw.bnr.licensing.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import rw.bnr.licensing.application.service.ApplicationStateMachine;
import rw.bnr.licensing.common.exception.ForbiddenException;
import rw.bnr.licensing.common.exception.InvalidTransitionException;
import rw.bnr.licensing.domain.model.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author David NTAMAKEMWA
 *
 * Unit tests for the state machine. No Spring context — pure logic.
 */
@Tag("unit")
class ApplicationStateMachineTest {

    private ApplicationStateMachine stateMachine;

    private User applicant;
    private User reviewer;
    private User otherReviewer;
    private User approver;
    private User admin;

    @BeforeEach
    void setUp() {
        stateMachine = new ApplicationStateMachine();

        applicant = makeUser(Role.APPLICANT);
        reviewer = makeUser(Role.REVIEWER);
        otherReviewer = makeUser(Role.REVIEWER);
        approver = makeUser(Role.APPROVER);
        admin = makeUser(Role.ADMIN);
    }

    // ---- Valid transitions ----

    @Test
    void draft_to_submitted_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED))
                .doesNotThrowAnyException();
    }

    @Test
    void submitted_to_under_review_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW))
                .doesNotThrowAnyException();
    }

    @Test
    void under_review_to_info_requested_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.UNDER_REVIEW, ApplicationStatus.INFO_REQUESTED))
                .doesNotThrowAnyException();
    }

    @Test
    void under_review_to_review_complete_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REVIEW_COMPLETE))
                .doesNotThrowAnyException();
    }

    @Test
    void review_complete_to_approved_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.REVIEW_COMPLETE, ApplicationStatus.APPROVED))
                .doesNotThrowAnyException();
    }

    @Test
    void review_complete_to_rejected_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.REVIEW_COMPLETE, ApplicationStatus.REJECTED))
                .doesNotThrowAnyException();
    }

    @Test
    void info_requested_to_resubmitted_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.INFO_REQUESTED, ApplicationStatus.RESUBMITTED))
                .doesNotThrowAnyException();
    }

    @Test
    void resubmitted_to_under_review_is_allowed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.RESUBMITTED, ApplicationStatus.UNDER_REVIEW))
                .doesNotThrowAnyException();
    }

    // ---- Invalid transitions ----

    @Test
    void draft_to_approved_is_rejected() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.DRAFT, ApplicationStatus.APPROVED))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void submitted_to_review_complete_skipping_under_review_is_rejected() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.SUBMITTED, ApplicationStatus.REVIEW_COMPLETE))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void under_review_to_approved_is_rejected() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(
                ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED))
                .isInstanceOf(InvalidTransitionException.class);
    }

    // ---- Terminal states cannot be left ----

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"APPROVED", "REJECTED", "WITHDRAWN"})
    void terminal_states_reject_all_transitions(ApplicationStatus terminalState) {
        for (ApplicationStatus target : ApplicationStatus.values()) {
            if (target != terminalState) {
                assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(terminalState, target))
                        .isInstanceOf(InvalidTransitionException.class)
                        .withFailMessage("Expected transition from %s to %s to be rejected", terminalState, target);
            }
        }
    }

    // ---- Actor authorisation ----

    @Test
    void applicant_can_submit_own_application() {
        Application app = makeApp(applicant, ApplicationStatus.DRAFT);
        assertThatCode(() -> stateMachine.assertActorCanTransition(app, applicant, ApplicationStatus.SUBMITTED))
                .doesNotThrowAnyException();
    }

    @Test
    void reviewer_cannot_submit_application() {
        Application app = makeApp(applicant, ApplicationStatus.DRAFT);
        assertThatThrownBy(() -> stateMachine.assertActorCanTransition(app, reviewer, ApplicationStatus.SUBMITTED))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approver_cannot_approve_application_they_reviewed() {
        // Edge case: same person has APPROVER role but also appears as reviewer
        User reviewerWhoIsApprover = makeUser(Role.APPROVER);
        Application app = makeApp(applicant, ApplicationStatus.REVIEW_COMPLETE);
        app.setReviewer(reviewerWhoIsApprover);

        assertThatThrownBy(() -> stateMachine.assertActorCanTransition(
                app, reviewerWhoIsApprover, ApplicationStatus.APPROVED))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("cannot make the final approval decision");
    }

    @Test
    void approver_can_approve_application_reviewed_by_someone_else() {
        Application app = makeApp(applicant, ApplicationStatus.REVIEW_COMPLETE);
        app.setReviewer(reviewer);

        assertThatCode(() -> stateMachine.assertActorCanTransition(app, approver, ApplicationStatus.APPROVED))
                .doesNotThrowAnyException();
    }

    @Test
    void non_assigned_reviewer_cannot_complete_review() {
        Application app = makeApp(applicant, ApplicationStatus.UNDER_REVIEW);
        app.setReviewer(reviewer);

        assertThatThrownBy(() -> stateMachine.assertActorCanTransition(
                app, otherReviewer, ApplicationStatus.REVIEW_COMPLETE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not the assigned reviewer");
    }

    @Test
    void admin_can_assign_reviewer() {
        Application app = makeApp(applicant, ApplicationStatus.SUBMITTED);
        assertThatCode(() -> stateMachine.assertActorCanTransition(app, admin, ApplicationStatus.UNDER_REVIEW))
                .doesNotThrowAnyException();
    }

    @Test
    void applicant_cannot_approve() {
        Application app = makeApp(applicant, ApplicationStatus.REVIEW_COMPLETE);
        assertThatThrownBy(() -> stateMachine.assertActorCanTransition(app, applicant, ApplicationStatus.APPROVED))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void applicant_cannot_act_on_another_applicants_application() {
        User otherApplicant = makeUser(Role.APPLICANT);
        Application app = makeApp(applicant, ApplicationStatus.DRAFT);

        assertThatThrownBy(() -> stateMachine.assertActorCanTransition(
                app, otherApplicant, ApplicationStatus.SUBMITTED))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- Helpers ----

    private User makeUser(Role role) {
        User u = new User("user-" + UUID.randomUUID() + "@test.rw", "hash", "Test User", role);
        // Set id via reflection so we can compare them without a database
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(u, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }

    private Application makeApp(User owner, ApplicationStatus status) {
        Application app = new Application();
        app.setApplicant(owner);
        app.setStatus(status);
        app.setInstitutionName("Test Bank");
        app.setInstitutionType("Commercial");
        return app;
    }
}
