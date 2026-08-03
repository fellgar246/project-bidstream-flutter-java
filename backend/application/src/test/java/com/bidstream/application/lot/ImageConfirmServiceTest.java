package com.bidstream.application.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotImageStatus;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.lot.UploadMismatchException;
import com.bidstream.domain.lot.UploadNotFoundException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageConfirmServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private LotRepository lotRepository;
  @Mock private LotImageRepository lotImageRepository;
  @Mock private ObjectStoragePort objectStorage;
  @Mock private ThumbnailService thumbnailService;

  private ImageConfirmService imageConfirmService;

  @BeforeEach
  void setUp() {
    imageConfirmService =
        new ImageConfirmService(
            lotRepository, lotImageRepository, objectStorage, thumbnailService, CLOCK);
  }

  @Test
  void missingObject_throwsUploadNotFound() {
    Lot lot = draftLot(1L, 10L);
    LotImage pending = pendingImage(5L, 1L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.findByIdAndLotId(5L, 1L)).thenReturn(Optional.of(pending));
    when(objectStorage.headObject(pending.storageKey())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> imageConfirmService.confirm(10L, Set.of(Role.SELLER), 1L, 5L))
        .isInstanceOf(UploadNotFoundException.class);

    verify(lotImageRepository, never()).save(any());
  }

  @Test
  void mismatch_deletesObjectMarksFailedAndThrows() {
    Lot lot = draftLot(1L, 10L);
    LotImage pending = pendingImage(5L, 1L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.findByIdAndLotId(5L, 1L)).thenReturn(Optional.of(pending));
    when(objectStorage.headObject(pending.storageKey()))
        .thenReturn(Optional.of(new ObjectStoragePort.ObjectMetadata(9999L, "image/jpeg")));

    assertThatThrownBy(() -> imageConfirmService.confirm(10L, Set.of(Role.SELLER), 1L, 5L))
        .isInstanceOf(UploadMismatchException.class);

    verify(objectStorage).deleteObject(pending.storageKey());
    verify(lotImageRepository).save(any(LotImage.class));
    verify(thumbnailService, never()).generateAsync(anyLong());
  }

  @Test
  void happyPath_marksReadyAndTriggersThumbnail() {
    Lot lot = draftLot(1L, 10L);
    LotImage pending = pendingImage(5L, 1L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.findByIdAndLotId(5L, 1L)).thenReturn(Optional.of(pending));
    when(objectStorage.headObject(pending.storageKey()))
        .thenReturn(Optional.of(new ObjectStoragePort.ObjectMetadata(1024L, "image/jpeg")));
    when(lotImageRepository.save(any(LotImage.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LotImage result = imageConfirmService.confirm(10L, Set.of(Role.SELLER), 1L, 5L);

    assertThat(result.status()).isEqualTo(LotImageStatus.READY);
    verify(thumbnailService).generateAsync(5L);
  }

  private static Lot draftLot(long lotId, long sellerId) {
    return new Lot(
        lotId,
        sellerId,
        "Title",
        "Desc",
        1L,
        Money.fromCents(1000),
        Money.fromCents(100),
        null,
        LotStatus.DRAFT,
        null,
        null,
        null,
        Money.fromCents(1000),
        0,
        null,
        0,
        0L,
        NOW,
        NOW);
  }

  private static LotImage pendingImage(long imageId, long lotId) {
    return new LotImage(
        imageId,
        lotId,
        "lots/" + lotId + "/abc.jpg",
        null,
        0,
        "image/jpeg",
        1024L,
        LotImageStatus.PENDING,
        NOW,
        NOW);
  }
}
