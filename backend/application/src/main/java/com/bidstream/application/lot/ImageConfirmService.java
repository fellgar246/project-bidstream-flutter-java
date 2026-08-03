package com.bidstream.application.lot;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.UploadMismatchException;
import com.bidstream.domain.lot.UploadNotFoundException;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ImageConfirmService {

  private final LotRepository lotRepository;
  private final LotImageRepository lotImageRepository;
  private final ObjectStoragePort objectStorage;
  private final ThumbnailService thumbnailService;
  private final Clock clock;

  public ImageConfirmService(
      LotRepository lotRepository,
      LotImageRepository lotImageRepository,
      ObjectStoragePort objectStorage,
      ThumbnailService thumbnailService,
      Clock clock) {
    this.lotRepository = lotRepository;
    this.lotImageRepository = lotImageRepository;
    this.objectStorage = objectStorage;
    this.thumbnailService = thumbnailService;
    this.clock = clock;
  }

  public LotImage confirm(long userId, Set<Role> roles, long lotId, long imageId) {
    Lot lot = lotRepository.findById(lotId).orElseThrow();
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);

    LotImage image =
        lotImageRepository
            .findByIdAndLotId(imageId, lotId)
            .orElseThrow(() -> new NoSuchElementException("Image not found"));

    ObjectStoragePort.ObjectMetadata metadata =
        objectStorage.headObject(image.storageKey()).orElseThrow(UploadNotFoundException::new);

    if (metadata.sizeBytes() != image.sizeBytes()
        || !metadata.contentType().equals(image.contentType())) {
      objectStorage.deleteObject(image.storageKey());
      LotImage failed = lotImageRepository.save(image.markFailed(clock.instant()));
      throw new UploadMismatchException();
    }

    LotImage ready = lotImageRepository.save(image.markReady(clock.instant()));
    thumbnailService.generateAsync(ready.id());
    return ready;
  }
}
