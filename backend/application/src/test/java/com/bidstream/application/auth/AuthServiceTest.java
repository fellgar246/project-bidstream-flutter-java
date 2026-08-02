package com.bidstream.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.application.auth.port.JwtPort;
import com.bidstream.application.auth.port.PasswordEncoderPort;
import com.bidstream.domain.auth.DuplicateEmailException;
import com.bidstream.domain.auth.InvalidCredentialsException;
import com.bidstream.domain.auth.RefreshToken;
import com.bidstream.domain.auth.RefreshTokenRepository;
import com.bidstream.domain.auth.TokenReuseException;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoderPort passwordEncoder;
  @Mock private JwtPort jwtPort;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository, refreshTokenRepository, passwordEncoder, jwtPort, CLOCK, 900, 7);
  }

  @Test
  void normalizeEmail_trimsAndLowercases() {
    assertThat(AuthService.normalizeEmail("  User@Example.COM ")).isEqualTo("user@example.com");
  }

  @Test
  void register_rejectsDuplicateEmailCaseInsensitive() {
    when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register("User@Example.com", "password1234", "Test User"))
        .isInstanceOf(DuplicateEmailException.class);

    verify(userRepository, never()).create(anyString(), anyString(), anyString(), any());
  }

  @Test
  void register_hashesPasswordAndCreatesBuyer() {
    when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
    when(passwordEncoder.hash("password1234")).thenReturn("$2a$12$hash");
    User created = new User(1L, "user@example.com", "Test User", EnumSet.of(Role.BUYER), true);
    when(userRepository.create(
            eq("user@example.com"), eq("$2a$12$hash"), eq("Test User"), eq(EnumSet.of(Role.BUYER))))
        .thenReturn(created);

    User result = authService.register("user@example.com", "password1234", "Test User");

    assertThat(result).isEqualTo(created);
    verify(passwordEncoder).hash("password1234");
  }

  @Test
  void login_issuesTokensWhenCredentialsValid() {
    User user = new User(1L, "user@example.com", "Test", EnumSet.of(Role.BUYER), true);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPasswordHashByUserId(1L)).thenReturn(Optional.of("$2a$12$hash"));
    when(passwordEncoder.matches("password1234", "$2a$12$hash")).thenReturn(true);
    when(jwtPort.createAccessToken(user)).thenReturn("access-token");
    when(refreshTokenRepository.save(anyLong(), anyString(), any(), anyString()))
        .thenReturn(sampleRefreshToken(10L, false));

    AuthTokens tokens = authService.login("user@example.com", "password1234", "agent");

    assertThat(tokens.accessToken()).isEqualTo("access-token");
    assertThat(tokens.refreshToken()).isNotBlank();
    ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
    verify(refreshTokenRepository).save(eq(1L), hashCaptor.capture(), any(), eq("agent"));
    assertThat(hashCaptor.getValue())
        .isEqualTo(AuthService.hashRefreshToken(tokens.refreshToken()));
  }

  @Test
  void refresh_rotatesTokenAndRevokesPrevious() {
    String rawOld = AuthService.generateRefreshToken();
    String hashOld = AuthService.hashRefreshToken(rawOld);
    RefreshToken stored =
        new RefreshToken(5L, 1L, hashOld, NOW.plusSeconds(3600), null, null, "agent", NOW);
    User user = new User(1L, "user@example.com", "Test", EnumSet.of(Role.BUYER), true);

    when(refreshTokenRepository.findByTokenHash(hashOld)).thenReturn(Optional.of(stored));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(jwtPort.createAccessToken(user)).thenReturn("new-access");
    when(refreshTokenRepository.save(anyLong(), anyString(), any(), anyString()))
        .thenReturn(sampleRefreshToken(6L, false));

    AuthTokens tokens = authService.refresh(rawOld, "agent");

    assertThat(tokens.accessToken()).isEqualTo("new-access");
    verify(refreshTokenRepository).revoke(5L, 6L);
  }

  @Test
  void refresh_detectsReuseAndRevokesAllUserTokens() {
    String rawOld = AuthService.generateRefreshToken();
    String hashOld = AuthService.hashRefreshToken(rawOld);
    RefreshToken revoked =
        new RefreshToken(5L, 1L, hashOld, NOW.plusSeconds(3600), NOW, 6L, "agent", NOW);

    when(refreshTokenRepository.findByTokenHash(hashOld)).thenReturn(Optional.of(revoked));

    assertThatThrownBy(() -> authService.refresh(rawOld, "agent"))
        .isInstanceOf(TokenReuseException.class);

    verify(refreshTokenRepository).revokeAllForUser(1L);
  }

  @Test
  void login_rejectsInvalidPassword() {
    User user = new User(1L, "user@example.com", "Test", EnumSet.of(Role.BUYER), true);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findPasswordHashByUserId(1L)).thenReturn(Optional.of("$2a$12$hash"));
    when(passwordEncoder.matches("wrong", "$2a$12$hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.login("user@example.com", "wrong", "agent"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void logout_revokesRefreshTokenWhenPresent() {
    String raw = AuthService.generateRefreshToken();
    String hash = AuthService.hashRefreshToken(raw);
    RefreshToken stored =
        new RefreshToken(3L, 1L, hash, NOW.plusSeconds(3600), null, null, "agent", NOW);
    when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

    authService.logout(raw);

    verify(refreshTokenRepository).revoke(3L, null);
  }

  @Test
  void applySellerRole_addsSellerAndIssuesTokens() {
    User buyer = new User(1L, "user@example.com", "Test", EnumSet.of(Role.BUYER), true);
    User seller =
        new User(1L, "user@example.com", "Test", EnumSet.of(Role.BUYER, Role.SELLER), true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
    when(userRepository.updateRoles(1L, EnumSet.of(Role.BUYER, Role.SELLER))).thenReturn(seller);
    when(jwtPort.createAccessToken(seller)).thenReturn("access");
    when(refreshTokenRepository.save(anyLong(), anyString(), any(), anyString()))
        .thenReturn(sampleRefreshToken(7L, false));

    AuthTokens tokens = authService.applySellerRole(1L, "agent");

    assertThat(tokens.accessToken()).isEqualTo("access");
    assertThat(tokens.user().hasRole(Role.SELLER)).isTrue();
  }

  private RefreshToken sampleRefreshToken(long id, boolean revoked) {
    return new RefreshToken(
        id, 1L, "abc", NOW.plusSeconds(3600), revoked ? NOW : null, null, "agent", NOW);
  }
}
