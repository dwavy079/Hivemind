package com.filesharing.service;

import com.filesharing.dto.AuthDtos.AuthResponse;
import com.filesharing.dto.AuthDtos.LoginRequest;
import com.filesharing.dto.AuthDtos.RefreshRequest;
import com.filesharing.dto.AuthDtos.RegisterRequest;
import com.filesharing.exception.ApiExceptions.ConflictException;
import com.filesharing.exception.ApiExceptions.UnauthorizedException;
import com.filesharing.model.Role;
import com.filesharing.model.User;
import com.filesharing.repository.UserRepository;
import com.filesharing.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        String refreshToken = jwtService.generateRefreshToken(user);
        user.setCurrentRefreshToken(refreshToken);
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, refreshToken, user.getFullName(), user.getEmail());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Revoke any prior refresh token: only the most recent login stays valid.
        String refreshToken = jwtService.generateRefreshToken(user);
        user.setCurrentRefreshToken(refreshToken);
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, refreshToken, user.getFullName(), user.getEmail());
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        boolean isRefreshType = "refresh".equals(jwtService.extractTokenType(token));
        boolean matchesStored = token.equals(user.getCurrentRefreshToken());
        boolean valid = jwtService.isTokenValid(token, user);

        if (!isRefreshType || !matchesStored || !valid) {
            throw new UnauthorizedException("Refresh token is invalid, expired, or revoked");
        }

        // Rotate: issue a new refresh token and invalidate the old one.
        String newRefreshToken = jwtService.generateRefreshToken(user);
        user.setCurrentRefreshToken(newRefreshToken);
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, newRefreshToken, user.getFullName(), user.getEmail());
    }

    @Transactional
    public void logout(User user) {
        user.setCurrentRefreshToken(null);
        userRepository.save(user);
    }
}
