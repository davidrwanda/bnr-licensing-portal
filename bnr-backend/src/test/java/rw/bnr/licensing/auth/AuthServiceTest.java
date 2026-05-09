package rw.bnr.licensing.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import rw.bnr.licensing.auth.dto.LoginRequest;
import rw.bnr.licensing.auth.dto.LoginResponse;
import rw.bnr.licensing.auth.service.AuthService;
import rw.bnr.licensing.auth.service.JwtService;
import rw.bnr.licensing.common.exception.UnauthorizedException;
import rw.bnr.licensing.domain.model.RefreshToken;
import rw.bnr.licensing.domain.model.Role;
import rw.bnr.licensing.domain.model.User;
import rw.bnr.licensing.domain.repository.RefreshTokenRepository;
import rw.bnr.licensing.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author David NTAMAKEMWA
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthService authService;

    @Test
    void login_returns_tokens_for_valid_credentials() {
        User user = buildUser(Role.APPLICANT);
        when(userRepository.findByEmail("applicant@test.rw")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getRefreshExpiryMs()).thenReturn(604800000L);

        LoginResponse response = authService.login(new LoginRequest("applicant@test.rw", "pass"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.role()).isEqualTo("APPLICANT");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_throws_when_authentication_manager_rejects_credentials() {
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(new LoginRequest("bad@test.rw", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_throws_when_token_not_found() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_throws_when_token_is_revoked() {
        User user = buildUser(Role.REVIEWER);
        RefreshToken token = new RefreshToken(user, "revoked-token",
                Instant.now().plusSeconds(3600));
        token.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired or revoked");
    }

    @Test
    void refresh_issues_new_tokens_and_revokes_old_one() {
        User user = buildUser(Role.REVIEWER);
        RefreshToken stored = new RefreshToken(user, "valid-token",
                Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");
        when(jwtService.getRefreshExpiryMs()).thenReturn(604800000L);

        LoginResponse response = authService.refresh("valid-token");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.accessToken()).isEqualTo("new-access");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void logout_marks_token_as_revoked() {
        User user = buildUser(Role.ADMIN);
        RefreshToken token = new RefreshToken(user, "logout-token",
                Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("logout-token")).thenReturn(Optional.of(token));

        authService.logout("logout-token");

        assertThat(token.isRevoked()).isTrue();
    }

    private User buildUser(Role role) {
        User u = new User("user@test.rw", "$2a$12$hash", "Test User", role);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(u, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }
}
