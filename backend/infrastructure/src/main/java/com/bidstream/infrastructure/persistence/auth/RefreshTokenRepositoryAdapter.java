package com.bidstream.infrastructure.persistence.auth;

import com.bidstream.domain.auth.RefreshToken;
import com.bidstream.domain.auth.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpaRepository;
  private final Clock clock;

  public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository, Clock clock) {
    this.jpaRepository = jpaRepository;
    this.clock = clock;
  }

  @Override
  public RefreshToken save(long userId, String tokenHash, Instant expiresAt, String userAgent) {
    RefreshTokenEntity entity = new RefreshTokenEntity();
    entity.setUserId(userId);
    entity.setTokenHash(tokenHash);
    entity.setExpiresAt(expiresAt);
    entity.setUserAgent(userAgent);
    entity.setCreatedAt(clock.instant());
    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public Optional<RefreshToken> findByTokenHash(String tokenHash) {
    return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
  }

  @Override
  public void revoke(long id, Long replacedById) {
    jpaRepository
        .findById(id)
        .ifPresent(
            entity -> {
              entity.setRevokedAt(clock.instant());
              entity.setReplacedById(replacedById);
              jpaRepository.save(entity);
            });
  }

  @Override
  @Transactional
  public void revokeAllForUser(long userId) {
    jpaRepository.revokeAllActiveForUser(userId);
  }

  private RefreshToken toDomain(RefreshTokenEntity entity) {
    return new RefreshToken(
        entity.getId(),
        entity.getUserId(),
        entity.getTokenHash(),
        entity.getExpiresAt(),
        entity.getRevokedAt(),
        entity.getReplacedById(),
        entity.getUserAgent(),
        entity.getCreatedAt());
  }
}
