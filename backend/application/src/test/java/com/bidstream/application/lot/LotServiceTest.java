package com.bidstream.application.lot;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.domain.lot.ForbiddenLotAccessException;
import com.bidstream.domain.lot.ImageRequiredException;
import com.bidstream.domain.lot.InvalidTransitionException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.WatchRepository;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private LotRepository lotRepository;
  @Mock private WatchRepository watchRepository;

  private LotService lotService;

  @BeforeEach
  void setUp() {
    LotsProperties properties = new LotsProperties();
    lotService = new LotService(lotRepository, watchRepository, properties, CLOCK);
  }

  @Test
  void rb02_updateByNonOwner_throwsForbidden() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(
            () ->
                lotService.updateLot(
                    99L,
                    Set.of(Role.SELLER),
                    1L,
                    "New",
                    "Desc",
                    1L,
                    Money.fromCents(1000),
                    Money.fromCents(100),
                    null))
        .isInstanceOf(ForbiddenLotAccessException.class);

    verify(lotRepository, never()).save(any());
  }

  @Test
  void rb02_cancelByNonOwner_throwsForbidden() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(() -> lotService.cancelLot(99L, Set.of(Role.SELLER), 1L))
        .isInstanceOf(ForbiddenLotAccessException.class);
  }

  @Test
  void rb02_deleteByNonOwner_throwsForbidden() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(() -> lotService.deleteLot(99L, Set.of(Role.SELLER), 1L))
        .isInstanceOf(ForbiddenLotAccessException.class);

    verify(lotRepository, never()).deleteById(1L);
  }

  @Test
  void rb02_adminCanCancelForeignLot() {
    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    lotService.cancelLot(99L, EnumSet.of(Role.ADMIN), 1L);

    verify(lotRepository).save(any());
  }

  @Test
  void rb12_scheduleWithoutReadyImages_throwsImageRequired() {
    LotsProperties properties = new LotsProperties();
    properties.setRequireImagesForSchedule(true);
    lotService = new LotService(lotRepository, watchRepository, properties, CLOCK);

    Lot lot = draftLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(lotRepository.hasReadyImages(1L)).thenReturn(false);
    Instant start = NOW.plusSeconds(600);
    Instant end = start.plusSeconds(3600);

    assertThatThrownBy(() -> lotService.scheduleLot(10L, Set.of(Role.SELLER), 1L, start, end))
        .isInstanceOf(ImageRequiredException.class);

    verify(lotRepository, never()).save(any());
  }

  @Test
  void deleteNonDraft_throwsInvalidTransition() {
    Lot lot = scheduledLot(1L, 10L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

    assertThatThrownBy(() -> lotService.deleteLot(10L, Set.of(Role.SELLER), 1L))
        .isInstanceOf(InvalidTransitionException.class);

    verify(lotRepository, never()).deleteById(1L);
  }

  private Lot draftLot(long lotId, long sellerId) {
    Lot draft =
        Lot.createDraft(
            sellerId,
            "Title",
            "Description",
            1L,
            Money.fromCents(1000),
            Money.fromCents(100),
            null,
            NOW);
    return draft.withId(lotId);
  }

  private Lot scheduledLot(long lotId, long sellerId) {
    Lot draft = draftLot(lotId, sellerId);
    Instant start = NOW.plusSeconds(600);
    Instant end = start.plusSeconds(3600);
    return draft.schedule(start, end, NOW);
  }
}
