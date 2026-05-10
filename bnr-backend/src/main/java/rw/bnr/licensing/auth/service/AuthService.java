package rw.bnr.licensing.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.bnr.licensing.auth.dto.LoginRequest;
import rw.bnr.licensing.auth.dto.LoginResponse;
import rw.bnr.licensing.common.exception.UnauthorizedException;
import rw.bnr.licensing.domain.model.RefreshToken;
import rw.bnr.licensing.domain.model.User;
import rw.bnr.licensing.domain.repository.RefreshTokenRepository;
import rw.bnr.licensing.domain.repository.UserRepository;

import java.time.Instant;

/**
 * @author David NTAMAKEMWA
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        refreshTokenRepository.revokeAllByUser(user);

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken(user);

        Instant expiresAt = Instant.now().plusMillis(jwtService.getRefreshExpiryMs());
        refreshTokenRepository.save(new RefreshToken(user, rawRefreshToken, expiresAt));

        return new LoginResponse(
                accessToken,
                rawRefreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = stored.getUser();
        stored.setRevoked(true);

        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);

        Instant expiresAt = Instant.now().plusMillis(jwtService.getRefreshExpiryMs());
        refreshTokenRepository.save(new RefreshToken(user, newRefresh, expiresAt));

        return new LoginResponse(
                newAccess, newRefresh,
                user.getId(), user.getEmail(),
                user.getFullName(), user.getRole().name()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken).ifPresent(t -> t.setRevoked(true));
    }
}
