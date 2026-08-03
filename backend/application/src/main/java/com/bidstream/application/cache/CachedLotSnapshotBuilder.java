package com.bidstream.application.cache;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.category.CategoryRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotImageStatus;
import com.bidstream.domain.user.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;

@Component
public class CachedLotSnapshotBuilder {

  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;
  private final LotImageRepository lotImageRepository;
  private final ObjectStoragePort objectStorage;

  public CachedLotSnapshotBuilder(
      CategoryRepository categoryRepository,
      UserRepository userRepository,
      LotImageRepository lotImageRepository,
      ObjectStoragePort objectStorage) {
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
    this.lotImageRepository = lotImageRepository;
    this.objectStorage = objectStorage;
  }

  public CachedLotSnapshot build(Lot lot) {
    return build(lot, null);
  }

  public CachedLotSnapshot build(Lot lot, Double rank) {
    var category =
        categoryRepository
            .findById(lot.categoryId())
            .orElseThrow(() -> new NoSuchElementException("Category not found"));
    var seller =
        userRepository
            .findById(lot.sellerId())
            .orElseThrow(() -> new NoSuchElementException("Seller not found"));

    return new CachedLotSnapshot(
        lot.id(),
        lot.title(),
        lot.description(),
        new CachedLotSnapshot.CachedCategory(category.id(), category.name()),
        new CachedLotSnapshot.CachedSeller(seller.id(), seller.displayName()),
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
        rank);
  }

  private List<CachedLotSnapshot.CachedImage> mapImages(long lotId) {
    return lotImageRepository.findByLotIdOrderByPosition(lotId).stream()
        .filter(image -> image.status() == LotImageStatus.READY)
        .map(this::toImage)
        .toList();
  }

  private CachedLotSnapshot.CachedImage toImage(LotImage image) {
    String url = objectStorage.publicUrl(image.storageKey());
    String thumbnailUrl =
        image.thumbnailKey() != null ? objectStorage.publicUrl(image.thumbnailKey()) : null;
    return new CachedLotSnapshot.CachedImage(
        image.id(),
        url,
        thumbnailUrl,
        image.position(),
        image.contentType(),
        image.status().name());
  }
}
