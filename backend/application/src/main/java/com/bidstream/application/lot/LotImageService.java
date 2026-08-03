package com.bidstream.application.lot;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.InvalidImageOrderException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LotImageService {

  private final LotRepository lotRepository;
  private final LotImageRepository lotImageRepository;
  private final ObjectStoragePort objectStorage;
  private final Clock clock;

  public LotImageService(
      LotRepository lotRepository,
      LotImageRepository lotImageRepository,
      ObjectStoragePort objectStorage,
      Clock clock) {
    this.lotRepository = lotRepository;
    this.lotImageRepository = lotImageRepository;
    this.objectStorage = objectStorage;
    this.clock = clock;
  }

  public List<LotImage> listByLotId(long lotId) {
    return lotImageRepository.findByLotIdOrderByPosition(lotId);
  }

  public void deleteImage(long userId, Set<Role> roles, long lotId, long imageId) {
    Lot lot = lotRepository.findById(lotId).orElseThrow();
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);

    LotImage image =
        lotImageRepository
            .findByIdAndLotId(imageId, lotId)
            .orElseThrow(() -> new NoSuchElementException("Image not found"));

    objectStorage.deleteObject(image.storageKey());
    if (image.thumbnailKey() != null) {
      objectStorage.deleteObject(image.thumbnailKey());
    }
    lotImageRepository.deleteById(imageId);
  }

  public List<LotImage> reorderImages(long userId, Set<Role> roles, long lotId, List<Long> order) {
    Lot lot = lotRepository.findById(lotId).orElseThrow();
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);

    List<LotImage> existing = lotImageRepository.findByLotIdOrderByPosition(lotId);
    Set<Long> existingIds = new HashSet<>();
    for (LotImage image : existing) {
      existingIds.add(image.id());
    }

    if (order.size() != existingIds.size() || !existingIds.containsAll(order)) {
      throw new InvalidImageOrderException("Order must contain exactly the lot's image ids");
    }

    var now = clock.instant();
    for (int i = 0; i < order.size(); i++) {
      long imageId = order.get(i);
      LotImage image =
          lotImageRepository
              .findByIdAndLotId(imageId, lotId)
              .orElseThrow(() -> new InvalidImageOrderException("Image id does not belong to lot"));
      lotImageRepository.save(image.withPosition(i, now));
    }

    return lotImageRepository.findByLotIdOrderByPosition(lotId);
  }
}
