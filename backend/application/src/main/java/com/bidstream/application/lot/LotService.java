package com.bidstream.application.lot;

import com.bidstream.application.cache.LotCacheInvalidator;
import com.bidstream.domain.lot.ImageRequiredException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.lot.Watch;
import com.bidstream.domain.lot.WatchRepository;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LotService {

  private final LotRepository lotRepository;
  private final WatchRepository watchRepository;
  private final LotsProperties lotsProperties;
  private final Clock clock;
  private final LotCacheInvalidator lotCacheInvalidator;

  public LotService(
      LotRepository lotRepository,
      WatchRepository watchRepository,
      LotsProperties lotsProperties,
      Clock clock,
      LotCacheInvalidator lotCacheInvalidator) {
    this.lotRepository = lotRepository;
    this.watchRepository = watchRepository;
    this.lotsProperties = lotsProperties;
    this.clock = clock;
    this.lotCacheInvalidator = lotCacheInvalidator;
  }

  public Lot createLot(
      long sellerId,
      String title,
      String description,
      long categoryId,
      Money startingPrice,
      Money minIncrement,
      Money reservePrice) {
    Instant now = clock.instant();
    Lot draft =
        Lot.createDraft(
            sellerId,
            title,
            description,
            categoryId,
            startingPrice,
            minIncrement,
            reservePrice,
            now);
    Lot saved = lotRepository.save(draft);
    lotCacheInvalidator.invalidateCatalog();
    return saved;
  }

  public Lot updateLot(
      long userId,
      Set<Role> roles,
      long lotId,
      String title,
      String description,
      long categoryId,
      Money startingPrice,
      Money minIncrement,
      Money reservePrice) {
    Lot lot = findLotOrThrow(lotId);
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);
    Lot updated =
        lot.updateDraft(
            title,
            description,
            categoryId,
            startingPrice,
            minIncrement,
            reservePrice,
            clock.instant());
    Lot saved = lotRepository.save(updated);
    lotCacheInvalidator.invalidateLot(lotId);
    return saved;
  }

  public void deleteLot(long userId, Set<Role> roles, long lotId) {
    Lot lot = findLotOrThrow(lotId);
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);
    LotAccessGuard.requireDeletable(lot);
    lotRepository.deleteById(lotId);
    lotCacheInvalidator.invalidateLotAndCatalog(lotId);
  }

  public Lot scheduleLot(
      long userId, Set<Role> roles, long lotId, Instant scheduledStartAt, Instant scheduledEndAt) {
    Lot lot = findLotOrThrow(lotId);
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);
    if (lotsProperties.isRequireImagesForSchedule() && !lotRepository.hasReadyImages(lotId)) {
      throw new ImageRequiredException();
    }
    Lot scheduled = lot.schedule(scheduledStartAt, scheduledEndAt, clock.instant());
    Lot saved = lotRepository.save(scheduled);
    lotCacheInvalidator.invalidateLotAndCatalog(lotId);
    return saved;
  }

  public Lot cancelLot(long userId, Set<Role> roles, long lotId) {
    Lot lot = findLotOrThrow(lotId);
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);
    Lot cancelled = lot.cancel(clock.instant());
    Lot saved = lotRepository.save(cancelled);
    lotCacheInvalidator.invalidateLotAndCatalog(lotId);
    return saved;
  }

  public Lot getLot(long lotId) {
    return findLotOrThrow(lotId);
  }

  public LotPage listPublicLots(LotQuery query) {
    return lotRepository.findPublic(query);
  }

  public List<Lot> listSellerLots(long sellerId) {
    return lotRepository.findBySellerId(sellerId);
  }

  public Lot forceStatus(long lotId, LotStatus status) {
    Lot lot = findLotOrThrow(lotId);
    Lot saved = lotRepository.save(lot.forceStatus(status, clock.instant()));
    lotCacheInvalidator.invalidateLotAndCatalog(lotId);
    return saved;
  }

  public void watchLot(long userId, long lotId) {
    findLotOrThrow(lotId);
    if (watchRepository.findByUserIdAndLotId(userId, lotId).isPresent()) {
      return;
    }
    watchRepository.save(new Watch(0L, userId, lotId, clock.instant()));
  }

  public void unwatchLot(long userId, long lotId) {
    watchRepository.deleteByUserIdAndLotId(userId, lotId);
  }

  public LotPage listWatchlist(long userId, int page, int size) {
    List<Watch> watches = watchRepository.findByUserId(userId, page, size);
    long total = watchRepository.countByUserId(userId);
    List<Lot> lots =
        watches.stream()
            .map(w -> lotRepository.findById(w.lotId()))
            .flatMap(Optional::stream)
            .toList();
    return new LotPage(lots, page, size, total);
  }

  public boolean isWatched(long userId, long lotId) {
    return watchRepository.findByUserIdAndLotId(userId, lotId).isPresent();
  }

  public boolean canEdit(Lot lot, Long viewerUserId, Set<Role> viewerRoles) {
    if (viewerUserId == null) {
      return false;
    }
    if (!lot.isEditable()) {
      return false;
    }
    if (viewerRoles != null && viewerRoles.contains(Role.ADMIN)) {
      return true;
    }
    return lot.sellerId() == viewerUserId;
  }

  public boolean canBid(Lot lot, Long viewerUserId) {
    if (viewerUserId == null || lot.status() != LotStatus.LIVE) {
      return false;
    }
    if (lot.sellerId() == viewerUserId) {
      return false;
    }
    if (lot.scheduledEndAt() == null || !clock.instant().isBefore(lot.scheduledEndAt())) {
      return false;
    }
    return true;
  }

  private Lot findLotOrThrow(long lotId) {
    return lotRepository
        .findById(lotId)
        .orElseThrow(() -> new NoSuchElementException("Lot not found"));
  }
}
