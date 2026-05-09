package rw.bnr.licensing.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.bnr.licensing.common.response.ApiResponse;
import rw.bnr.licensing.domain.model.Role;
import rw.bnr.licensing.domain.repository.UserRepository;
import rw.bnr.licensing.user.dto.UserResponse;

import java.util.List;

/**
 * @author David NTAMAKEMWA
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listByRole(
            @RequestParam(required = false) String role) {

        List<UserResponse> users = role != null
                ? userRepository.findByRoleAndActiveTrue(Role.valueOf(role.toUpperCase()))
                        .stream().map(UserResponse::from).toList()
                : userRepository.findByActiveTrue()
                        .stream().map(UserResponse::from).toList();

        return ResponseEntity.ok(ApiResponse.ok(users));
    }
}
