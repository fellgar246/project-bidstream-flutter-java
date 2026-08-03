package com.bidstream.api.lot;

import com.bidstream.application.cache.CachedLotSnapshot;
import com.bidstream.application.lot.LotService;
import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.category.CategoryRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotImageStatus;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LotResponseMapper {

  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;
  private final LotService lotService;
  private final LotImageRepository lotImageRepository;
  private final ObjectStoragePort objectStorage;

  public LotResponseMapper(
      CategoryRepository categoryRepository,
      UserRepository userRepository,
      LotService lotService,
      LotImageRepository lotImageRepository,
      ObjectStoragePort objectStorage) {
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
    this.lotService = lotService;
    this.lotImageRepository = lotImageRepository;
    this.objectStorage = objectStorage;
  }

  public LotResponse toResponse(Lot lot, Long viewerUserId, Set<Role> viewerRoles) {
    var category =
        categoryRepository
            .findById(lot.categoryId())
            .orElseThrow(() -> new NoSuchElementException("Category not found"));
    var seller =
        userRepository
            .findById(lot.sellerId())
            .orElseThrow(() -> new NoSuchElementException("Seller not found"));

    boolean watched = viewerUserId != null && lotService.isWatched(viewerUserId, lot.id());

    return new LotResponse(
        lot.id(),
        lot.title(),
        lot.description(),
        new CategorySummary(category.id(), category.name()),
        new SellerSummary(seller.id(), seller.displayName()),
        lot.startingPrice().toString(),
        lot.minIncrement().toString(),
        lot.currentPrice().toString(),
        lot.bidCount(),
        lot.hasReserve(),
        lot.isReserveMet(),
        lot.status().name(),
        lot.scheduledStartAt() != null ? lot.scheduledStartAt().toString() : null,
        lot.scheduledEndAt() != null ? lot.scheduledEndAt().toString() : null,
        lot.actualEndAt() != null ? lot.actualEndAt().toString() : null,
        mapImages(lot.id()),
        watched,
        lotService.canEdit(lot, viewerUserId, viewerRoles),
        lotService.canBid(lot, viewerUserId),
        null);
  }

  public LotResponse fromSnapshot(
      CachedLotSnapshot snapshot, Long viewerUserId, Set<Role> viewerRoles) {
    Lot lot = snapshotToLot(snapshot);
    boolean watched = viewerUserId != null && lotService.isWatched(viewerUserId, lot.id());
    List<LotImageSummary> images =
        snapshot.images().stream()
            .map(
                image ->
                    new LotImageSummary(
                        image.id(),
                        image.url(),
                        image.thumbnailUrl(),
                        image.position(),
                        image.contentType(),
                        image.status()))
            .toList();

    return new LotResponse(
        snapshot.id(),
        snapshot.title(),
        snapshot.description(),
        new CategorySummary(snapshot.category().id(), snapshot.category().name()),
        new SellerSummary(snapshot.seller().id(), snapshot.seller().displayName()),
        snapshot.startingPrice(),
        snapshot.minIncrement(),
        snapshot.currentPrice(),
        snapshot.bidCount(),
        snapshot.hasReserve(),
        snapshot.reserveMet(),
        snapshot.status(),
        snapshot.scheduledStartAt(),
        snapshot.scheduledEndAt(),
        snapshot.actualEndAt(),
        images,
        watched,
        lotService.canEdit(lot, viewerUserId, viewerRoles),
        lotService.canBid(lot, viewerUserId),
        snapshot.rank());
  }

  private static Lot snapshotToLot(CachedLotSnapshot snapshot) {
    Instant placeholder = Instant.EPOCH;
    return new Lot(
        snapshot.id(),
        snapshot.seller().id(),
        snapshot.title(),
        snapshot.description(),
        snapshot.category().id(),
        com.bidstream.domain.money.Money.fromString(snapshot.startingPrice()),
        com.bidstream.domain.money.Money.fromString(snapshot.minIncrement()),
        null,
        com.bidstream.domain.lot.LotStatus.valueOf(snapshot.status()),
        snapshot.scheduledStartAt() != null
            ? java.time.Instant.parse(snapshot.scheduledStartAt())
            : null,
        snapshot.scheduledEndAt() != null
            ? java.time.Instant.parse(snapshot.scheduledEndAt())
            : null,
        snapshot.actualEndAt() != null ? java.time.Instant.parse(snapshot.actualEndAt()) : null,
        com.bidstream.domain.money.Money.fromString(snapshot.currentPrice()),
        snapshot.bidCount(),
        null,
        0,
        0L,
        placeholder,
        placeholder);
  }

  private List<LotImageSummary> mapImages(long lotId) {
    return lotImageRepository.findByLotIdOrderByPosition(lotId).stream()
        .filter(image -> image.status() == LotImageStatus.READY)
        .map(this::toSummary)
        .toList();
  }

  private LotImageSummary toSummary(LotImage image) {
    String url = objectStorage.publicUrl(image.storageKey());
    String thumbnailUrl =
        image.thumbnailKey() != null ? objectStorage.publicUrl(image.thumbnailKey()) : null;
    return new LotImageSummary(
        image.id(),
        url,
        thumbnailUrl,
        image.position(),
        image.contentType(),
        image.status().name());
  }
}
