package com.bidstream.infrastructure.persistence.lot;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import com.bidstream.infrastructure.persistence.category.CategoryEntity;
import com.bidstream.infrastructure.persistence.category.CategoryJpaRepository;
import com.bidstream.infrastructure.persistence.user.UserEntity;
import com.bidstream.infrastructure.persistence.user.UserJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class LotRepositoryAdapter implements LotRepository {

  private final LotJpaRepository lotJpaRepository;
  private final LotImageJpaRepository lotImageJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final CategoryJpaRepository categoryJpaRepository;
  private final Clock clock;

  public LotRepositoryAdapter(
      LotJpaRepository lotJpaRepository,
      LotImageJpaRepository lotImageJpaRepository,
      UserJpaRepository userJpaRepository,
      CategoryJpaRepository categoryJpaRepository,
      Clock clock) {
    this.lotJpaRepository = lotJpaRepository;
    this.lotImageJpaRepository = lotImageJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.categoryJpaRepository = categoryJpaRepository;
    this.clock = clock;
  }

  @Override
  public Lot save(Lot lot) {
    LotEntity entity =
        lot.id() == 0L ? new LotEntity() : lotJpaRepository.findById(lot.id()).orElseThrow();

    UserEntity seller = userJpaRepository.findById(lot.sellerId()).orElseThrow();
    CategoryEntity category = categoryJpaRepository.findById(lot.categoryId()).orElseThrow();

    entity.setSeller(seller);
    entity.setTitle(lot.title());
    entity.setDescription(lot.description());
    entity.setCategory(category);
    entity.setStartingPriceCents(lot.startingPrice().cents());
    entity.setMinIncrementCents(lot.minIncrement().cents());
    entity.setReservePriceCents(lot.reservePrice() == null ? null : lot.reservePrice().cents());
    entity.setStatus(lot.status().name());
    entity.setScheduledStartAt(lot.scheduledStartAt());
    entity.setScheduledEndAt(lot.scheduledEndAt());
    entity.setActualEndAt(lot.actualEndAt());
    entity.setCurrentPriceCents(lot.currentPrice().cents());
    entity.setBidCount(lot.bidCount());
    entity.setWinningBidId(lot.winningBidId());
    entity.setExtensionCount(lot.extensionCount());

    Instant now = clock.instant();
    if (lot.id() == 0L) {
      entity.setCreatedAt(lot.createdAt() != null ? lot.createdAt() : now);
    } else {
      entity.setCreatedAt(lot.createdAt());
    }
    entity.setUpdatedAt(lot.updatedAt() != null ? lot.updatedAt() : now);

    LotEntity saved = lotJpaRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  public Optional<Lot> findById(long id) {
    return lotJpaRepository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Lot> findByIdForUpdate(long id) {
    return lotJpaRepository.findByIdForUpdate(id).map(this::toDomain);
  }

  @Override
  public void deleteById(long id) {
    lotJpaRepository.deleteById(id);
  }

  @Override
  public LotPage findPublic(LotQuery query) {
    PageRequest pageRequest =
        PageRequest.of(query.page(), query.size(), LotSpecifications.toSort(query.sort()));
    Page<LotEntity> page =
        lotJpaRepository.findAll(LotSpecifications.forPublicQuery(query), pageRequest);
    List<Lot> content = page.getContent().stream().map(this::toDomain).toList();
    return new LotPage(content, query.page(), query.size(), page.getTotalElements());
  }

  @Override
  public List<Lot> findBySellerId(long sellerId) {
    return lotJpaRepository.findBySeller_IdOrderByCreatedAtDesc(sellerId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public long countBySellerId(long sellerId) {
    return lotJpaRepository.countBySeller_Id(sellerId);
  }

  @Override
  public boolean hasReadyImages(long lotId) {
    return lotImageJpaRepository.existsByLot_IdAndStatus(lotId, "READY");
  }

  @Override
  public List<Lot> findScheduledReadyToStart(Instant now, int limit) {
    return lotJpaRepository
        .findScheduledReadyToStart(now, PageRequest.of(0, limit))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Long> findLiveIdsReadyToClose(Instant now, int limit) {
    return lotJpaRepository.findLiveIdsReadyToClose(now, PageRequest.of(0, limit));
  }

  private Lot toDomain(LotEntity entity) {
    Money reservePrice =
        entity.getReservePriceCents() == null
            ? null
            : Money.fromCents(entity.getReservePriceCents());
    return new Lot(
        entity.getId(),
        entity.getSeller().getId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getCategory().getId(),
        Money.fromCents(entity.getStartingPriceCents()),
        Money.fromCents(entity.getMinIncrementCents()),
        reservePrice,
        LotStatus.valueOf(entity.getStatus()),
        entity.getScheduledStartAt(),
        entity.getScheduledEndAt(),
        entity.getActualEndAt(),
        Money.fromCents(entity.getCurrentPriceCents()),
        entity.getBidCount(),
        entity.getWinningBidId(),
        entity.getExtensionCount(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
