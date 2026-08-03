package com.bidstream.infrastructure.persistence.lot;

import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotImageStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class LotImageRepositoryAdapter implements LotImageRepository {

  private final LotImageJpaRepository lotImageJpaRepository;
  private final LotJpaRepository lotJpaRepository;
  private final Clock clock;

  public LotImageRepositoryAdapter(
      LotImageJpaRepository lotImageJpaRepository,
      LotJpaRepository lotJpaRepository,
      Clock clock) {
    this.lotImageJpaRepository = lotImageJpaRepository;
    this.lotJpaRepository = lotJpaRepository;
    this.clock = clock;
  }

  @Override
  public LotImage save(LotImage image) {
    LotImageEntity entity =
        image.id() == 0L
            ? new LotImageEntity()
            : lotImageJpaRepository.findById(image.id()).orElseThrow();

    if (image.id() == 0L) {
      entity.setLot(lotJpaRepository.findById(image.lotId()).orElseThrow());
    }
    entity.setStorageKey(image.storageKey());
    entity.setThumbnailKey(image.thumbnailKey());
    entity.setPosition(image.position());
    entity.setContentType(image.contentType());
    entity.setSizeBytes(image.sizeBytes());
    entity.setStatus(image.status().name());

    Instant now = clock.instant();
    if (image.id() == 0L) {
      entity.setCreatedAt(image.createdAt() != null ? image.createdAt() : now);
    } else {
      entity.setCreatedAt(image.createdAt());
    }
    entity.setUpdatedAt(image.updatedAt() != null ? image.updatedAt() : now);

    return toDomain(lotImageJpaRepository.save(entity));
  }

  @Override
  public Optional<LotImage> findById(long id) {
    return lotImageJpaRepository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<LotImage> findByIdAndLotId(long id, long lotId) {
    return lotImageJpaRepository
        .findById(id)
        .filter(entity -> entity.getLot().getId() == lotId)
        .map(this::toDomain);
  }

  @Override
  public List<LotImage> findByLotIdOrderByPosition(long lotId) {
    return lotImageJpaRepository.findByLot_IdOrderByPositionAsc(lotId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public int countByLotId(long lotId) {
    return lotImageJpaRepository.countByLot_Id(lotId);
  }

  @Override
  public boolean hasReadyImages(long lotId) {
    return lotImageJpaRepository.existsByLot_IdAndStatus(lotId, LotImageStatus.READY.name());
  }

  @Override
  public void deleteById(long id) {
    lotImageJpaRepository.deleteById(id);
  }

  @Override
  public List<LotImage> findPendingOlderThan(Instant cutoff) {
    return lotImageJpaRepository
        .findByStatusAndCreatedAtBefore(LotImageStatus.PENDING.name(), cutoff)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  public int findMaxPosition(long lotId) {
    return lotImageJpaRepository.findMaxPositionByLotId(lotId);
  }

  private LotImage toDomain(LotImageEntity entity) {
    return new LotImage(
        entity.getId(),
        entity.getLot().getId(),
        entity.getStorageKey(),
        entity.getThumbnailKey(),
        entity.getPosition(),
        entity.getContentType(),
        entity.getSizeBytes(),
        LotImageStatus.valueOf(entity.getStatus()),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
