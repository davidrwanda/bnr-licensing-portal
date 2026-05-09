package rw.bnr.licensing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author David NTAMAKEMWA
 */
public record DecisionRequest(
        @NotBlank @Size(max = 2000) String reason
) {}
