package rw.bnr.licensing.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @author David NTAMAKEMWA
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
