package rw.bnr.licensing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.bnr.licensing.domain.model.Document;

import java.util.List;
import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByApplicationIdAndSupersededFalseOrderByUploadedAtDesc(UUID applicationId);

    List<Document> findByApplicationIdOrderByDocumentVersionAscUploadedAtDesc(UUID applicationId);

    @Query("SELECT COALESCE(MAX(d.documentVersion), 0) FROM Document d WHERE d.application.id = :applicationId")
    int findMaxVersionByApplicationId(@Param("applicationId") UUID applicationId);

    @Modifying
    @Query("UPDATE Document d SET d.superseded = true WHERE d.application.id = :applicationId AND d.superseded = false")
    int markAllSupersededForApplication(@Param("applicationId") UUID applicationId);
}
