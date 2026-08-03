package com.bidstream.application.lot;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.FileTooLargeException;
import com.bidstream.domain.lot.ImageLimitReachedException;
import com.bidstream.domain.lot.InvalidTransitionException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotEvent;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.lot.UnsupportedMediaTypeException;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PresignService {

  static final int MAX_IMAGES_PER_LOT = 10;
  static final long MAX_SIZE_BYTES = 8L * 1024 * 1024;

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final LotRepository lotRepository;
  private final LotImageRepository lotImageRepository;
  private final ObjectStoragePort objectStorage;
  private final StorageProperties storageProperties;
  private final Clock clock;

  public PresignService(
      LotRepository lotRepository,
      LotImageRepository lotImageRepository,
      ObjectStoragePort objectStorage,
      StorageProperties storageProperties,
      Clock clock) {
    this.lotRepository = lotRepository;
    this.lotImageRepository = lotImageRepository;
    this.objectStorage = objectStorage;
    this.storageProperties = storageProperties;
    this.clock = clock;
  }

  public PresignResult presign(
      long userId,
      Set<Role> roles,
      long lotId,
      String fileName,
      String contentType,
      long sizeBytes) {
    Lot lot = lotRepository.findById(lotId).orElseThrow();
    LotAccessGuard.requireOwnerOrAdmin(lot, userId, roles);
    requireEditableLotStatus(lot);

    validateContentType(contentType);
    validateSize(sizeBytes);

    if (lotImageRepository.countByLotId(lotId) >= MAX_IMAGES_PER_LOT) {
      throw new ImageLimitReachedException(MAX_IMAGES_PER_LOT);
    }

    String storageKey = buildStorageKey(lotId, fileName, contentType);
    int position = lotImageRepository.findMaxPosition(lotId) + 1;
    Instant now = clock.instant();
    LotImage pending =
        lotImageRepository.save(
            LotImage.createPending(lotId, storageKey, position, contentType, sizeBytes, now));

    ObjectStoragePort.PresignedUpload upload =
        objectStorage.presignPut(
            storageKey, contentType, storageProperties.getPresignExpirySeconds());

    return new PresignResult(
        pending.id(), upload.uploadUrl(), upload.storageKey(), upload.expiresInSeconds());
  }

  static void validateContentType(String contentType) {
    if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new UnsupportedMediaTypeException(contentType);
    }
  }

  static void validateSize(long sizeBytes) {
    if (sizeBytes > MAX_SIZE_BYTES) {
      throw new FileTooLargeException(sizeBytes, MAX_SIZE_BYTES);
    }
  }

  private static void requireEditableLotStatus(Lot lot) {
    if (lot.status() != LotStatus.DRAFT && lot.status() != LotStatus.SCHEDULED) {
      throw new InvalidTransitionException(lot.status(), LotEvent.UPDATE);
    }
  }

  private static String buildStorageKey(long lotId, String fileName, String contentType) {
    String extension = extensionForContentType(contentType, fileName);
    return "lots/" + lotId + "/" + UUID.randomUUID() + "." + extension;
  }

  private static String extensionForContentType(String contentType, String fileName) {
    return switch (contentType) {
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default -> {
        int dot = fileName.lastIndexOf('.');
        yield dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "bin";
      }
    };
  }

  public record PresignResult(long imageId, String uploadUrl, String storageKey, int expiresIn) {}
}
