package rw.bnr.licensing.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.bnr.licensing.audit.dto.AuditLogResponse;
import rw.bnr.licensing.audit.service.AuditQueryService;
import rw.bnr.licensing.auth.filter.JwtAuthFilter;
import rw.bnr.licensing.common.response.ApiResponse;
import rw.bnr.licensing.common.response.PagedResponse;
import rw.bnr.licensing.domain.model.Role;

import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/applications/{applicationId}/audit")
    @PreAuthorize("hasAnyRole('REVIEWER', 'APPROVER', 'ADMIN', 'APPLICANT')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getApplicationAudit(
            @PathVariable UUID applicationId,
            org.springframework.security.core.Authentication auth) {
        var principal = getPrincipal(auth);
        List<AuditLogResponse> logs = auditQueryService.getForApplication(
                applicationId, principal.id(), Role.valueOf(principal.role()));
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(auditQueryService.getAll(page, size)));
    }

    private JwtAuthFilter.AuthenticatedUser getPrincipal(org.springframework.security.core.Authentication auth) {
        return (JwtAuthFilter.AuthenticatedUser) auth.getPrincipal();
    }
}
