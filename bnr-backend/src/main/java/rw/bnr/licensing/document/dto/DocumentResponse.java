package rw.bnr.licensing.document.dto;

import rw.bnr.licensing.domain.model.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public record DocumentResponse(
        UUID id,
        UUID applicationId,
        String fileName,
        Long fileSize,
        String mimeType,
        Integer documentVersion,
        boolean superseded,
        String uploaderEmail,
        Instant uploadedAt
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getApplication().getId(),
                d.getFileName(),
                d.getFileSize(),
                d.getMimeType(),
                d.getDocumentVersion(),
                d.isSuperseded(),
                d.getUploader().getEmail(),
                d.getUploadedAt()
        );
    }
}
