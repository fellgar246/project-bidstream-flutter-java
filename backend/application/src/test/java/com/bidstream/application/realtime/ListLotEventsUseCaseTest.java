package com.bidstream.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListLotEventsUseCaseTest {

  private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

  @Mock private LotRepository lotRepository;
  @Mock private BidRepository bidRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private ListLotEventsUseCase useCase;

  @Test
  void execute_returnsBidEventsAfterCursor() {
    Lot lot = sampleLot();
    Bid bid = Bid.create(1L, 2L, Money.fromString("105.00"), NOW, "req-1").withId(3L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(bidRepository.findByLotIdAfterBidId(1L, 1L, 100)).thenReturn(List.of(bid));
    when(userRepository.findById(2L))
        .thenReturn(Optional.of(new User(2L, "a@test.com", "Ana", Set.of(Role.BUYER), true)));

    List<LotEventEnvelope> events = useCase.execute(1L, 2L, 100);

    assertThat(events).hasSize(1);
    assertThat(events.getFirst().type()).isEqualTo(LotEventEnvelope.BID_PLACED);
    assertThat(events.getFirst().eventId()).isEqualTo(6L);
    assertThat(events.getFirst().payload().get("bidderDisplayName")).isEqualTo("Ana");
  }

  @Test
  void toBidPlacedEnvelope_mapsPayload() {
    Lot lot = sampleLot();
    Bid bid = Bid.create(1L, 2L, Money.fromString("105.00"), NOW, "req-1").withId(3L);
    LotEventEnvelope envelope = ListLotEventsUseCase.toBidPlacedEnvelope(bid, lot, "Ana");
    assertThat(envelope.payload().get("currentPrice")).isEqualTo("105.00");
    assertThat(envelope.payload().get("bidderDisplayName")).isEqualTo("Ana");
  }

  @Test
  void toExtendedEnvelope_mapsScheduledEndAt() {
    Lot lot = sampleLot();
    Bid bid = Bid.create(1L, 2L, Money.fromString("105.00"), NOW, "req-1").withId(3L);
    LotEventEnvelope envelope = ListLotEventsUseCase.toExtendedEnvelope(bid, lot);
    assertThat(envelope.type()).isEqualTo(LotEventEnvelope.LOT_EXTENDED);
    assertThat(envelope.eventId()).isEqualTo(7L);
  }

  private static Lot sampleLot() {
    return new Lot(
        1L,
        10L,
        "Title",
        "Desc",
        1L,
        Money.fromString("100.00"),
        Money.fromString("5.00"),
        null,
        LotStatus.LIVE,
        NOW.minusSeconds(3600),
        NOW.plusSeconds(3600),
        null,
        Money.fromString("105.00"),
        1,
        null,
        0,
        0L,
        NOW,
        NOW);
  }
}
