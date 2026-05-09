package rw.bnr.licensing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author David NTAMAKEMWA
 */
public record CreateApplicationRequest(
        @NotBlank @Size(max = 255) String institutionName,
        @NotBlank @Size(max = 100) String institutionType,
        @Size(max = 500) String contactAddress,
        @Size(max = 2000) String businessDescription
) {}
