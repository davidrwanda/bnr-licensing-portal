package rw.bnr.licensing.document.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rw.bnr.licensing.auth.filter.JwtAuthFilter;
import rw.bnr.licensing.common.exception.ResourceNotFoundException;
import rw.bnr.licensing.common.response.ApiResponse;
import rw.bnr.licensing.document.dto.DocumentResponse;
import rw.bnr.licensing.document.service.DocumentService;
import rw.bnr.licensing.domain.model.Role;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/applications/{applicationId}/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable UUID applicationId,
            @RequestParam("file") MultipartFile file,
            org.springframework.security.core.Authentication auth) throws IOException {
        var principal = getPrincipal(auth);
        DocumentResponse response = documentService.upload(
                applicationId, principal.id(), Role.valueOf(principal.role()), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/applications/{applicationId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listCurrent(
            @PathVariable UUID applicationId,
            org.springframework.security.core.Authentication auth) {
        var principal = getPrincipal(auth);
        return ResponseEntity.ok(ApiResponse.ok(
                documentService.listCurrent(applicationId, principal.id(), Role.valueOf(principal.role()))));
    }

    @GetMapping("/applications/{applicationId}/documents/history")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listHistory(
            @PathVariable UUID applicationId,
            org.springframework.security.core.Authentication auth) {
        var principal = getPrincipal(auth);
        return ResponseEntity.ok(ApiResponse.ok(
                documentService.listHistory(applicationId, principal.id(), Role.valueOf(principal.role()))));
    }

    /**
     * Binary download — returns the file stream directly rather than an ApiResponse envelope,
     * which is the correct pattern for file downloads. Errors (404, 403) still go through
     * GlobalExceptionHandler and return JSON ApiResponse bodies.
     */
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable UUID documentId,
            org.springframework.security.core.Authentication auth) {
        var principal = getPrincipal(auth);
        Path filePath = documentService.getFilePath(
                documentId, principal.id(), Role.valueOf(principal.role()));

        Resource resource = new PathResource(filePath);
        if (!resource.exists()) {
            throw new ResourceNotFoundException("Document file not found on server");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private JwtAuthFilter.AuthenticatedUser getPrincipal(org.springframework.security.core.Authentication auth) {
        return (JwtAuthFilter.AuthenticatedUser) auth.getPrincipal();
    }
}
