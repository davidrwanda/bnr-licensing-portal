package rw.bnr.licensing.user.dto;

import rw.bnr.licensing.domain.model.User;

import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public record UserResponse(UUID id, String fullName, String email, String role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
    }
}
