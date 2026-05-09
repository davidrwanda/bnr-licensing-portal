package rw.bnr.licensing.auth.dto;

import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String email,
        String fullName,
        String role
) {}
