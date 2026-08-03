package com.bidstream.application.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.InvalidImageOrderException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotImageStatus;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotImageServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private LotRepository lotRepository;
  @Mock private LotImageRepository lotImageRepository;
  @Mock private ObjectStoragePort objectStorage;

  private LotImageService lotImageService;

  @BeforeEach
  void setUp() {
    lotImageService = new LotImageService(lotRepository, lotImageRepository, objectStorage, CLOCK);
  }

  @Test
  void listByLotId_returnsOrderedImages() {
    LotImage image = readyImage(1L, 1L, "lots/1/a.jpg", null);
    when(lotImageRepository.findByLotIdOrderByPosition(1L)).thenReturn(List.of(image));

    assertThat(lotImageService.listByLotId(1L)).containsExactly(image);
  }

  @Test
  void deleteImage_removesOriginalAndThumbnail() {
    Lot lot = draftLot(1L, 10L);
    LotImage image = readyImage(5L, 1L, "lots/1/a.jpg", "lots/1/a_thumb.webp");
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.findByIdAndLotId(5L, 1L)).thenReturn(Optional.of(image));

    lotImageService.deleteImage(10L, Set.of(Role.SELLER), 1L, 5L);

    verify(objectStorage).deleteObject("lots/1/a.jpg");
    verify(objectStorage).deleteObject("lots/1/a_thumb.webp");
    verify(lotImageRepository).deleteById(5L);
  }

  @Test
  void reorderImages_withForeignId_throwsValidationError() {
    Lot lot = draftLot(1L, 10L);
    LotImage first = readyImage(1L, 1L, "lots/1/a.jpg", null);
    LotImage second = readyImage(2L, 1L, "lots/1/b.jpg", null);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.findByLotIdOrderByPosition(1L)).thenReturn(List.of(first, second));

    assertThatThrownBy(
            () -> lotImageService.reorderImages(10L, Set.of(Role.SELLER), 1L, List.of(99L, 2L)))
        .isInstanceOf(InvalidImageOrderException.class);
  }

  @Test
  void reorderImages_updatesPositions() {
    Lot lot = draftLot(1L, 10L);
    LotImage first = readyImage(1L, 1L, "lots/1/a.jpg", null);
    LotImage second = readyImage(2L, 1L, "lots/1/b.jpg", null);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotImageRepository.findByLotIdOrderByPosition(1L))
        .thenReturn(List.of(first, second))
        .thenReturn(List.of(second.withPosition(0, NOW), first.withPosition(1, NOW)));
    when(lotImageRepository.findByIdAndLotId(2L, 1L)).thenReturn(Optional.of(second));
    when(lotImageRepository.findByIdAndLotId(1L, 1L)).thenReturn(Optional.of(first));
    when(lotImageRepository.save(any(LotImage.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<LotImage> reordered =
        lotImageService.reorderImages(10L, Set.of(Role.SELLER), 1L, List.of(2L, 1L));

    assertThat(reordered).hasSize(2);
    verify(lotImageRepository).save(second.withPosition(0, NOW));
    verify(lotImageRepository).save(first.withPosition(1, NOW));
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

  private static LotImage readyImage(long id, long lotId, String key, String thumb) {
    return new LotImage(
        id, lotId, key, thumb, 0, "image/jpeg", 1024L, LotImageStatus.READY, NOW, NOW);
  }
}
