package com.bidstream.infrastructure.persistence.lot;

import com.bidstream.domain.lot.Watch;
import com.bidstream.domain.lot.WatchRepository;
import com.bidstream.infrastructure.persistence.user.UserEntity;
import com.bidstream.infrastructure.persistence.user.UserJpaRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class WatchRepositoryAdapter implements WatchRepository {

  private final WatchJpaRepository watchJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final LotJpaRepository lotJpaRepository;
  private final Clock clock;

  public WatchRepositoryAdapter(
      WatchJpaRepository watchJpaRepository,
      UserJpaRepository userJpaRepository,
      LotJpaRepository lotJpaRepository,
      Clock clock) {
    this.watchJpaRepository = watchJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.lotJpaRepository = lotJpaRepository;
    this.clock = clock;
  }

  @Override
  public Watch save(Watch watch) {
    WatchEntity entity = new WatchEntity();
    UserEntity user = userJpaRepository.findById(watch.userId()).orElseThrow();
    LotEntity lot = lotJpaRepository.findById(watch.lotId()).orElseThrow();
    entity.setUser(user);
    entity.setLot(lot);
    entity.setCreatedAt(watch.createdAt() != null ? watch.createdAt() : clock.instant());
    WatchEntity saved = watchJpaRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  public Optional<Watch> findByUserIdAndLotId(long userId, long lotId) {
    return watchJpaRepository.findByUser_IdAndLot_Id(userId, lotId).map(this::toDomain);
  }

  @Override
  public void deleteByUserIdAndLotId(long userId, long lotId) {
    watchJpaRepository.deleteByUser_IdAndLot_Id(userId, lotId);
  }

  @Override
  public List<Watch> findByUserId(long userId, int page, int size) {
    return watchJpaRepository
        .findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public long countByUserId(long userId) {
    return watchJpaRepository.countByUser_Id(userId);
  }

  private Watch toDomain(WatchEntity entity) {
    return new Watch(
        entity.getId(), entity.getUser().getId(), entity.getLot().getId(), entity.getCreatedAt());
  }
}
