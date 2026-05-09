package rw.bnr.licensing.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.bnr.licensing.audit.dto.AuditLogResponse;
import rw.bnr.licensing.common.exception.ForbiddenException;
import rw.bnr.licensing.common.exception.ResourceNotFoundException;
import rw.bnr.licensing.common.response.PagedResponse;
import rw.bnr.licensing.domain.model.Application;
import rw.bnr.licensing.domain.model.Role;
import rw.bnr.licensing.domain.repository.ApplicationRepository;
import rw.bnr.licensing.domain.repository.AuditLogRepository;

import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 *
 * Read-side audit queries, separated from AuditService (which handles writes)
 * to keep the write path free of pagination concerns.
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getForApplication(UUID applicationId, UUID actorId, Role role) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        assertCanViewAudit(app, actorId, role);

        return auditLogRepository
                .findByApplicationIdOrderByCreatedAtAsc(applicationId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAll(int page, int size) {
        int cappedSize = Math.min(size, 100);
        return PagedResponse.from(
                auditLogRepository
                        .findAllByOrderByCreatedAtDesc(PageRequest.of(page, cappedSize))
                        .map(AuditLogResponse::from)
        );
    }

    private void assertCanViewAudit(Application app, UUID actorId, Role role) {
        boolean allowed = switch (role) {
            case ADMIN -> true;
            case REVIEWER, APPROVER -> true;  // can see audit for any application in their queue
            case APPLICANT -> app.getApplicant().getId().equals(actorId);  // owner only
        };
        if (!allowed) {
            throw new ForbiddenException("You do not have access to this application's audit trail");
        }
    }
}
