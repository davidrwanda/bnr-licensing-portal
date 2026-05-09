package rw.bnr.licensing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author David NTAMAKEMWA
 */
public record ReviewNoteRequest(
        @NotBlank @Size(max = 2000) String notes
) {}
