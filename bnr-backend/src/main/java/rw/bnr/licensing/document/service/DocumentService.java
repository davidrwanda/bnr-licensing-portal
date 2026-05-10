package rw.bnr.licensing.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rw.bnr.licensing.audit.service.AuditService;
import rw.bnr.licensing.common.exception.ForbiddenException;
import rw.bnr.licensing.common.exception.InvalidTransitionException;
import rw.bnr.licensing.common.exception.ResourceNotFoundException;
import rw.bnr.licensing.document.dto.DocumentResponse;
import rw.bnr.licensing.domain.model.*;
import rw.bnr.licensing.domain.repository.ApplicationRepository;
import rw.bnr.licensing.domain.repository.DocumentRepository;
import rw.bnr.licensing.domain.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.max-bytes}")
    private long maxBytes;

    @Transactional
    public DocumentResponse upload(UUID applicationId, UUID actorId, Role actorRole, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidTransitionException("Uploaded file must not be empty");
        }
        if (file.getSize() > maxBytes) {
            throw new InvalidTransitionException("File exceeds the maximum allowed size of 5 MB");
        }

        Application app = loadApplication(applicationId);
        User actor = loadUser(actorId);

        assertCanUpload(app, actor, actorRole);

        boolean superseding = app.getStatus() == ApplicationStatus.RESUBMITTED
                || app.getStatus() == ApplicationStatus.INFO_REQUESTED;
        if (superseding) {
            int supersededCount = documentRepository.markAllSupersededForApplication(applicationId);
            if (supersededCount > 0) {
                auditService.record(applicationId, actor, "DOCUMENTS_SUPERSEDED",
                        null, null,
                        "{\"count\":" + supersededCount + "}");
            }
        }

        int nextVersion = documentRepository.findMaxVersionByApplicationId(applicationId) + 1;

        Path dir = Paths.get(uploadDir, applicationId.toString());
        Files.createDirectories(dir);
        String storedName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
        Path dest = dir.resolve(storedName);
        file.transferTo(dest);

        Document doc = new Document();
        doc.setApplication(app);
        doc.setUploader(actor);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileSize(file.getSize());
        doc.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        doc.setStoragePath(dest.toString());
        doc.setDocumentVersion(nextVersion);

        Document saved = documentRepository.save(doc);

        auditService.record(applicationId, actor, "DOCUMENT_UPLOADED",
                null, null,
                "{\"fileName\":\"" + file.getOriginalFilename()
                        + "\",\"version\":" + nextVersion
                        + ",\"sizeBytes\":" + file.getSize() + "}");

        return DocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listCurrent(UUID applicationId, UUID actorId, Role actorRole) {
        Application app = loadApplication(applicationId);
        assertCanView(app, actorId, actorRole);
        return documentRepository
                .findByApplicationIdAndSupersededFalseOrderByUploadedAtDesc(applicationId)
                .stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listHistory(UUID applicationId, UUID actorId, Role actorRole) {
        Application app = loadApplication(applicationId);
        assertCanView(app, actorId, actorRole);
        return documentRepository
                .findByApplicationIdOrderByDocumentVersionAscUploadedAtDesc(applicationId)
                .stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentForDownload(UUID documentId, UUID actorId, Role actorRole) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        assertCanView(doc.getApplication(), actorId, actorRole);
        return DocumentResponse.from(doc);
    }

    @Transactional(readOnly = true)
    public Path getFilePath(UUID documentId, UUID actorId, Role actorRole) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        assertCanView(doc.getApplication(), actorId, actorRole);
        return Paths.get(doc.getStoragePath());
    }

    private void assertCanUpload(Application app, User actor, Role role) {
        switch (role) {
            case APPLICANT -> {
                if (!app.getApplicant().getId().equals(actor.getId())) {
                    throw new ForbiddenException("You can only upload to your own applications");
                }
            }
            case ADMIN, REVIEWER -> {}
            case APPROVER -> throw new ForbiddenException("Approvers cannot upload documents");
        }
    }

    private void assertCanView(Application app, UUID actorId, Role role) {
        boolean allowed = switch (role) {
            case ADMIN, REVIEWER, APPROVER -> true;
            case APPLICANT -> app.getApplicant().getId().equals(actorId);
        };
        if (!allowed) {
            throw new ForbiddenException("You do not have access to this document");
        }
    }

    private Application loadApplication(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private User loadUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    // replaces anything that isn't a safe filename character to avoid path traversal
    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
