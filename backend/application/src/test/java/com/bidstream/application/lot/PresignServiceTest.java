package com.bidstream.application.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.FileTooLargeException;
import com.bidstream.domain.lot.ForbiddenLotAccessException;
import com.bidstream.domain.lot.ImageLimitReachedException;
import com.bidstream.domain.lot.InvalidTransitionException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotImageStatus;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.lot.UnsupportedMediaTypeException;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PresignServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private LotRepository lotRepository;
  @Mock private LotImageRepository lotImageRepository;
  @Mock private ObjectStoragePort objectStorage;

  private PresignService presignService;

  @BeforeEach
  void setUp() {
    StorageProperties properties = new StorageProperties();
    properties.setPresignExpirySeconds(300);
    presignService =
        new PresignService(lotRepository, lotImageRepository, objectStorage, properties, CLOCK);
  }

  @Test
  void rejectsUnsupportedContentType() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(
            () ->
                presignService.presign(
                    10L, Set.of(Role.SELLER), 1L, "photo.gif", "image/gif", 1024L))
        .isInstanceOf(UnsupportedMediaTypeException.class);

    verify(lotImageRepository, never()).save(any());
  }

  @Test
  void rejectsOversizedFile() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(
            () ->
                presignService.presign(
                    10L,
                    Set.of(Role.SELLER),
                    1L,
                    "photo.jpg",
                    "image/jpeg",
                    PresignService.MAX_SIZE_BYTES + 1))
        .isInstanceOf(FileTooLargeException.class);
  }

  @Test
  void rejectsWhenImageLimitReached() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.countByLotId(1L)).thenReturn(PresignService.MAX_IMAGES_PER_LOT);

    assertThatThrownBy(
            () ->
                presignService.presign(
                    10L, Set.of(Role.SELLER), 1L, "photo.jpg", "image/jpeg", 1024L))
        .isInstanceOf(ImageLimitReachedException.class);
  }

  @Test
  void rejectsWhenLotIsLive() {
    Lot lot = lotWithStatus(1L, 10L, LotStatus.LIVE);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(
            () ->
                presignService.presign(
                    10L, Set.of(Role.SELLER), 1L, "photo.jpg", "image/jpeg", 1024L))
        .isInstanceOf(InvalidTransitionException.class);
  }

  @Test
  void rejectsNonOwner() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(
            () ->
                presignService.presign(
                    99L, Set.of(Role.SELLER), 1L, "photo.jpg", "image/jpeg", 1024L))
        .isInstanceOf(ForbiddenLotAccessException.class);
  }

  @Test
  void happyPath_createsPendingRowAndReturnsPresignedUrl() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.countByLotId(1L)).thenReturn(0);
    when(lotImageRepository.findMaxPosition(1L)).thenReturn(-1);
    when(lotImageRepository.save(any(LotImage.class)))
        .thenAnswer(
            invocation -> {
              LotImage image = invocation.getArgument(0);
              return new LotImage(
                  42L,
                  image.lotId(),
                  image.storageKey(),
                  image.thumbnailKey(),
                  image.position(),
                  image.contentType(),
                  image.sizeBytes(),
                  image.status(),
                  image.createdAt(),
                  image.updatedAt());
            });
    when(objectStorage.presignPut(any(), eq("image/jpeg"), eq(300)))
        .thenReturn(
            new ObjectStoragePort.PresignedUpload("https://minio/upload", "lots/1/x.jpg", 300));

    PresignService.PresignResult result =
        presignService.presign(10L, Set.of(Role.SELLER), 1L, "photo.jpg", "image/jpeg", 1024L);

    assertThat(result.imageId()).isEqualTo(42L);
    assertThat(result.uploadUrl()).isEqualTo("https://minio/upload");
    assertThat(result.expiresIn()).isEqualTo(300);

    ArgumentCaptor<LotImage> captor = ArgumentCaptor.forClass(LotImage.class);
    verify(lotImageRepository).save(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(LotImageStatus.PENDING);
    assertThat(captor.getValue().storageKey()).startsWith("lots/1/");
  }

  private static Lot draftLot(long lotId, long sellerId) {
    return lotWithStatus(lotId, sellerId, LotStatus.DRAFT);
  }

  @Test
  void presign_allowsScheduledLot() {
    Lot lot = lotWithStatus(1L, 10L, LotStatus.SCHEDULED);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.countByLotId(1L)).thenReturn(0);
    when(lotImageRepository.findMaxPosition(1L)).thenReturn(-1);
    when(lotImageRepository.save(any(LotImage.class)))
        .thenAnswer(
            invocation -> {
              LotImage image = invocation.getArgument(0);
              return new LotImage(
                  7L,
                  image.lotId(),
                  image.storageKey(),
                  image.thumbnailKey(),
                  image.position(),
                  image.contentType(),
                  image.sizeBytes(),
                  image.status(),
                  image.createdAt(),
                  image.updatedAt());
            });
    when(objectStorage.presignPut(any(), eq("image/png"), eq(300)))
        .thenReturn(new ObjectStoragePort.PresignedUpload("url", "key", 300));

    PresignService.PresignResult result =
        presignService.presign(10L, Set.of(Role.SELLER), 1L, "photo.png", "image/png", 512L);

    assertThat(result.imageId()).isEqualTo(7L);
  }

  private static Lot lotWithStatus(long lotId, long sellerId, LotStatus status) {
    Instant now = NOW;
    return new Lot(
        lotId,
        sellerId,
        "Title",
        "Desc",
        1L,
        Money.fromCents(1000),
        Money.fromCents(100),
        null,
        status,
        null,
        null,
        null,
        Money.fromCents(1000),
        0,
        null,
        0,
        0L,
        now,
        now);
  }
}
