package rw.bnr.licensing.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.bnr.licensing.application.dto.*;
import rw.bnr.licensing.application.service.ApplicationService;
import rw.bnr.licensing.auth.filter.JwtAuthFilter;
import rw.bnr.licensing.common.response.ApiResponse;
import rw.bnr.licensing.domain.model.Role;

import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> create(
            @Valid @RequestBody CreateApplicationRequest request,
            org.springframework.security.core.Authentication auth) {
        UUID actorId = getPrincipal(auth).id();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(applicationService.create(actorId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> list(
            org.springframework.security.core.Authentication auth) {
        var principal = getPrincipal(auth);
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.listForActor(principal.id(), Role.valueOf(principal.role()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getById(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication auth) {
        var principal = getPrincipal(auth);
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getById(id, principal.id(), Role.valueOf(principal.role()))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationRequest request,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.update(id, getPrincipal(auth).id(), request)));
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submit(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.submit(id, getPrincipal(auth).id())));
    }

    @PatchMapping("/{id}/assign-reviewer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> assignReviewer(
            @PathVariable UUID id,
            @Valid @RequestBody AssignReviewerRequest request,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.assignReviewer(id, getPrincipal(auth).id(), request)));
    }

    @PatchMapping("/{id}/request-info")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> requestInfo(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewNoteRequest request,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.requestInfo(id, getPrincipal(auth).id(), request)));
    }

    @PatchMapping("/{id}/complete-review")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> completeReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewNoteRequest request,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.completeReview(id, getPrincipal(auth).id(), request)));
    }

    @PatchMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> resubmit(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.resubmit(id, getPrincipal(auth).id())));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('APPROVER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.approve(id, getPrincipal(auth).id(), request)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('APPROVER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.reject(id, getPrincipal(auth).id(), request)));
    }

    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdraw(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.withdraw(id, getPrincipal(auth).id())));
    }

    private JwtAuthFilter.AuthenticatedUser getPrincipal(org.springframework.security.core.Authentication auth) {
        return (JwtAuthFilter.AuthenticatedUser) auth.getPrincipal();
    }
}
