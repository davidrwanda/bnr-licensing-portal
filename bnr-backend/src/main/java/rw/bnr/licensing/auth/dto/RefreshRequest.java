package rw.bnr.licensing.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author David NTAMAKEMWA
 */
public record RefreshRequest(@NotBlank String refreshToken) {}
