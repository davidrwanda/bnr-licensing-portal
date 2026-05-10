package rw.bnr.licensing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.bnr.licensing.application.dto.*;
import rw.bnr.licensing.audit.service.AuditService;
import rw.bnr.licensing.common.exception.ConflictException;
import rw.bnr.licensing.common.exception.ForbiddenException;
import rw.bnr.licensing.common.exception.ResourceNotFoundException;
import rw.bnr.licensing.domain.model.*;
import rw.bnr.licensing.domain.repository.ApplicationRepository;
import rw.bnr.licensing.domain.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationStateMachine stateMachine;
    private final AuditService auditService;

    @Transactional
    public ApplicationResponse create(UUID actorId, CreateApplicationRequest request) {
        User applicant = loadUser(actorId);

        Application app = new Application();
        app.setApplicant(applicant);
        app.setInstitutionName(request.institutionName());
        app.setInstitutionType(request.institutionType());
        app.setContactAddress(request.contactAddress());
        app.setBusinessDescription(request.businessDescription());
        app.setStatus(ApplicationStatus.DRAFT);

        Application saved = applicationRepository.save(app);

        auditService.record(saved.getId(), applicant, "APPLICATION_CREATED",
                null, ApplicationStatus.DRAFT.name(), null);

        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listForActor(UUID actorId, Role role) {
        User actor = loadUser(actorId);
        List<Application> results = switch (role) {
            case APPLICANT -> applicationRepository.findByApplicant(actor);
            case REVIEWER -> applicationRepository.findByReviewer(actor);
            case APPROVER -> applicationRepository.findByStatus(ApplicationStatus.REVIEW_COMPLETE);
            // Admins see everything except drafts — drafts are the applicant's private workspace
            case ADMIN -> applicationRepository.findByStatusNot(ApplicationStatus.DRAFT);
        };
        return results.stream().map(ApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID applicationId, UUID actorId, Role role) {
        Application app = loadApplication(applicationId);
        assertCanView(app, actorId, role);
        return ApplicationResponse.from(app);
    }

    @Transactional
    public ApplicationResponse update(UUID applicationId, UUID actorId, UpdateApplicationRequest request) {
        Application app = loadApplication(applicationId);
        User actor = loadUser(actorId);

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new rw.bnr.licensing.common.exception.InvalidTransitionException(
                    "Only DRAFT applications can be edited");
        }
        if (!app.getApplicant().getId().equals(actorId)) {
            throw new ForbiddenException("You can only edit your own applications");
        }

        app.setInstitutionName(request.institutionName());
        app.setInstitutionType(request.institutionType());
        app.setContactAddress(request.contactAddress());
        app.setBusinessDescription(request.businessDescription());

        Application saved = applicationRepository.save(app);
        auditService.record(saved.getId(), actor, "APPLICATION_UPDATED",
                ApplicationStatus.DRAFT.name(), ApplicationStatus.DRAFT.name(), null);
        return ApplicationResponse.from(saved);
    }

    @Transactional
    public ApplicationResponse submit(UUID applicationId, UUID actorId) {
        return transitionTo(applicationId, actorId, ApplicationStatus.SUBMITTED, app -> {
            app.setSubmittedAt(Instant.now());
        });
    }

    @Transactional
    public ApplicationResponse assignReviewer(UUID applicationId, UUID actorId, AssignReviewerRequest request) {
        Application app = loadApplicationWithLock(applicationId);
        User actor = loadUser(actorId);

        stateMachine.assertTransitionAllowed(app.getStatus(), ApplicationStatus.UNDER_REVIEW);
        stateMachine.assertActorCanTransition(app, actor, ApplicationStatus.UNDER_REVIEW);

        User reviewer = userRepository.findById(request.reviewerId())
                .filter(u -> u.getRole() == Role.REVIEWER)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with id: " + request.reviewerId()));

        ApplicationStatus previous = app.getStatus();
        app.setReviewer(reviewer);
        app.setStatus(ApplicationStatus.UNDER_REVIEW);

        try {
            Application saved = applicationRepository.save(app);
            auditService.record(saved.getId(), actor, "REVIEWER_ASSIGNED",
                    previous.name(), ApplicationStatus.UNDER_REVIEW.name(),
                    "{\"assignedReviewer\":\"" + reviewer.getEmail() + "\"}");
            return ApplicationResponse.from(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("The application was modified concurrently. Please refresh and try again.");
        }
    }

    @Transactional
    public ApplicationResponse requestInfo(UUID applicationId, UUID actorId, ReviewNoteRequest request) {
        return transitionWithNotes(applicationId, actorId, ApplicationStatus.INFO_REQUESTED,
                request.notes(), "INFO_REQUESTED");
    }

    @Transactional
    public ApplicationResponse completeReview(UUID applicationId, UUID actorId, ReviewNoteRequest request) {
        return transitionWithNotes(applicationId, actorId, ApplicationStatus.REVIEW_COMPLETE,
                request.notes(), "REVIEW_COMPLETED");
    }

    @Transactional
    public ApplicationResponse resubmit(UUID applicationId, UUID actorId) {
        return transitionTo(applicationId, actorId, ApplicationStatus.RESUBMITTED, app -> {});
    }

    @Transactional
    public ApplicationResponse approve(UUID applicationId, UUID actorId, DecisionRequest request) {
        return makeDecision(applicationId, actorId, ApplicationStatus.APPROVED, request.reason(), "APPLICATION_APPROVED");
    }

    @Transactional
    public ApplicationResponse reject(UUID applicationId, UUID actorId, DecisionRequest request) {
        return makeDecision(applicationId, actorId, ApplicationStatus.REJECTED, request.reason(), "APPLICATION_REJECTED");
    }

    @Transactional
    public ApplicationResponse withdraw(UUID applicationId, UUID actorId) {
        return transitionTo(applicationId, actorId, ApplicationStatus.WITHDRAWN, app -> {});
    }

    private ApplicationResponse transitionTo(UUID applicationId, UUID actorId,
                                              ApplicationStatus target,
                                              java.util.function.Consumer<Application> mutator) {
        Application app = loadApplicationWithLock(applicationId);
        User actor = loadUser(actorId);

        stateMachine.assertTransitionAllowed(app.getStatus(), target);
        stateMachine.assertActorCanTransition(app, actor, target);

        ApplicationStatus previous = app.getStatus();
        app.setStatus(target);
        mutator.accept(app);

        try {
            Application saved = applicationRepository.save(app);
            auditService.record(saved.getId(), actor,
                    target.name(), previous.name(), target.name(), null);
            return ApplicationResponse.from(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("The application was modified concurrently. Please refresh and try again.");
        }
    }

    private ApplicationResponse transitionWithNotes(UUID applicationId, UUID actorId,
                                                     ApplicationStatus target, String notes,
                                                     String auditAction) {
        Application app = loadApplicationWithLock(applicationId);
        User actor = loadUser(actorId);

        stateMachine.assertTransitionAllowed(app.getStatus(), target);
        stateMachine.assertActorCanTransition(app, actor, target);

        ApplicationStatus previous = app.getStatus();
        app.setStatus(target);
        app.setReviewerNotes(notes);

        try {
            Application saved = applicationRepository.save(app);
            auditService.record(saved.getId(), actor, auditAction,
                    previous.name(), target.name(), "{\"notes\":\"" + notes + "\"}");
            return ApplicationResponse.from(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("The application was modified concurrently. Please refresh and try again.");
        }
    }

    private ApplicationResponse makeDecision(UUID applicationId, UUID actorId,
                                              ApplicationStatus target, String reason,
                                              String auditAction) {
        Application app = loadApplicationWithLock(applicationId);
        User actor = loadUser(actorId);

        stateMachine.assertTransitionAllowed(app.getStatus(), target);
        stateMachine.assertActorCanTransition(app, actor, target);

        ApplicationStatus previous = app.getStatus();
        app.setStatus(target);
        app.setDecisionReason(reason);
        app.setApprover(actor);
        app.setDecidedAt(Instant.now());

        try {
            Application saved = applicationRepository.save(app);
            auditService.record(saved.getId(), actor, auditAction,
                    previous.name(), target.name(), "{\"reason\":\"" + reason + "\"}");
            return ApplicationResponse.from(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("The application was modified concurrently. Please refresh and try again.");
        }
    }

    private void assertCanView(Application app, UUID actorId, Role role) {
        // DRAFT is only visible to the applicant who owns it — no one else
        if (app.getStatus() == ApplicationStatus.DRAFT) {
            boolean isOwner = role == Role.APPLICANT && app.getApplicant().getId().equals(actorId);
            if (!isOwner) {
                throw new ForbiddenException("You do not have access to this application");
            }
            return;
        }

        boolean allowed = switch (role) {
            case ADMIN -> true;
            case APPROVER -> true;
            // Reviewer can only view applications assigned to them
            case REVIEWER -> app.getReviewer() != null && app.getReviewer().getId().equals(actorId);
            case APPLICANT -> app.getApplicant().getId().equals(actorId);
        };
        if (!allowed) {
            throw new ForbiddenException("You do not have access to this application");
        }
    }

    private Application loadApplication(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private Application loadApplicationWithLock(UUID id) {
        return applicationRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private User loadUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
