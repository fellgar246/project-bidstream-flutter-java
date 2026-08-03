package com.bidstream.infrastructure.persistence.device;

import com.bidstream.application.device.DeviceToken;
import com.bidstream.application.device.DeviceTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepository {

  private final DeviceTokenJpaRepository jpaRepository;
  private final Clock clock;

  public DeviceTokenRepositoryAdapter(DeviceTokenJpaRepository jpaRepository, Clock clock) {
    this.jpaRepository = jpaRepository;
    this.clock = clock;
  }

  @Override
  public DeviceToken upsert(long userId, String token, String platform) {
    Instant now = clock.instant();
    DeviceTokenEntity entity =
        jpaRepository
            .findByToken(token)
            .orElseGet(
                () -> {
                  DeviceTokenEntity created = new DeviceTokenEntity();
                  created.setCreatedAt(now);
                  return created;
                });
    entity.setUserId(userId);
    entity.setToken(token);
    entity.setPlatform(platform);
    entity.setLastSeenAt(now);
    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public Optional<DeviceToken> findByToken(String token) {
    return jpaRepository.findByToken(token).map(this::toDomain);
  }

  @Override
  public List<DeviceToken> findByUserId(long userId) {
    return jpaRepository.findByUserId(userId).stream().map(this::toDomain).toList();
  }

  @Override
  public void deleteByToken(String token) {
    jpaRepository.deleteByToken(token);
  }

  @Override
  public void deleteByUserIdAndToken(long userId, String token) {
    jpaRepository.deleteByUserIdAndToken(userId, token);
  }

  private DeviceToken toDomain(DeviceTokenEntity entity) {
    return new DeviceToken(
        entity.getId(),
        entity.getUserId(),
        entity.getToken(),
        entity.getPlatform(),
        entity.getLastSeenAt(),
        entity.getCreatedAt());
  }
}
