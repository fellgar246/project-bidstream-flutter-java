package com.bidstream.application.auth;

import com.bidstream.application.auth.port.JwtPort;
import com.bidstream.application.auth.port.PasswordEncoderPort;
import com.bidstream.domain.auth.AlreadySellerException;
import com.bidstream.domain.auth.DuplicateEmailException;
import com.bidstream.domain.auth.InvalidCredentialsException;
import com.bidstream.domain.auth.RefreshToken;
import com.bidstream.domain.auth.RefreshTokenRepository;
import com.bidstream.domain.auth.TokenReuseException;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoderPort passwordEncoder;
  private final JwtPort jwtPort;
  private final Clock clock;
  private final long accessTokenTtlSeconds;
  private final long refreshTokenTtlDays;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoderPort passwordEncoder,
      JwtPort jwtPort,
      Clock clock,
      @Value("${bidstream.auth.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
      @Value("${bidstream.auth.refresh-token-ttl-days:7}") long refreshTokenTtlDays) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtPort = jwtPort;
    this.clock = clock;
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    this.refreshTokenTtlDays = refreshTokenTtlDays;
  }

  public static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  @Transactional
  public User register(String email, String password, String displayName) {
    String normalized = normalizeEmail(email);
    if (userRepository.existsByEmailIgnoreCase(normalized)) {
      throw new DuplicateEmailException(normalized);
    }
    String hash = passwordEncoder.hash(password);
    return userRepository.create(normalized, hash, displayName.trim(), EnumSet.of(Role.BUYER));
  }

  @Transactional
  public AuthTokens login(String email, String password, String userAgent) {
    User user = authenticate(email, password);
    return buildTokens(user, userAgent);
  }

  @Transactional(noRollbackFor = TokenReuseException.class)
  public AuthTokens refresh(String rawRefreshToken, String userAgent) {
    String hash = hashRefreshToken(rawRefreshToken);
    Instant now = clock.instant();

    RefreshToken stored =
        refreshTokenRepository.findByTokenHash(hash).orElseThrow(InvalidCredentialsException::new);

    if (stored.isRevoked()) {
      refreshTokenRepository.revokeAllForUser(stored.userId());
      throw new TokenReuseException();
    }

    if (stored.isExpired(now)) {
      throw new InvalidCredentialsException();
    }

    User user =
        userRepository
            .findById(stored.userId())
            .filter(User::enabled)
            .orElseThrow(InvalidCredentialsException::new);

    String newRawRefresh = generateRefreshToken();
    String newHash = hashRefreshToken(newRawRefresh);
    Instant expiresAt = clock.instant().plus(refreshTokenTtlDays, ChronoUnit.DAYS);
    RefreshToken replacement =
        refreshTokenRepository.save(user.id(), newHash, expiresAt, userAgent);
    refreshTokenRepository.revoke(stored.id(), replacement.id());

    return new AuthTokens(
        jwtPort.createAccessToken(user), newRawRefresh, accessTokenTtlSeconds, user);
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    String hash = hashRefreshToken(rawRefreshToken);
    refreshTokenRepository
        .findByTokenHash(hash)
        .ifPresent(token -> refreshTokenRepository.revoke(token.id(), null));
  }

  @Transactional
  public AuthTokens applySellerRole(long userId, String userAgent) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));

    if (user.hasRole(Role.SELLER)) {
      throw new AlreadySellerException();
    }

    EnumSet<Role> updated = EnumSet.copyOf(user.roles());
    updated.add(Role.SELLER);
    User updatedUser = userRepository.updateRoles(userId, updated);
    return buildTokens(updatedUser, userAgent);
  }

  private User authenticate(String email, String password) {
    String normalized = normalizeEmail(email);
    User user =
        userRepository
            .findByEmail(normalized)
            .filter(User::enabled)
            .orElseThrow(InvalidCredentialsException::new);

    String storedHash =
        userRepository
            .findPasswordHashByUserId(user.id())
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, storedHash)) {
      throw new InvalidCredentialsException();
    }
    return user;
  }

  private AuthTokens buildTokens(User user, String userAgent) {
    String rawRefresh = generateRefreshToken();
    String hash = hashRefreshToken(rawRefresh);
    Instant expiresAt = clock.instant().plus(refreshTokenTtlDays, ChronoUnit.DAYS);
    refreshTokenRepository.save(user.id(), hash, expiresAt, userAgent);

    return new AuthTokens(jwtPort.createAccessToken(user), rawRefresh, accessTokenTtlSeconds, user);
  }

  public static String generateRefreshToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hashRefreshToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
