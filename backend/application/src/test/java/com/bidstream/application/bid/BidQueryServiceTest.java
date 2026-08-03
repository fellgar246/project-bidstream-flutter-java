package com.bidstream.application.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidPage;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BidQueryServiceTest {

  @Mock private BidRepository bidRepository;
  @Mock private LotRepository lotRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private BidQueryService service;

  @Test
  void listLotBids_delegatesToRepository() {
    BidPage page = new BidPage(List.of(), 0, 20, 0);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(sampleLot()));
    when(bidRepository.findByLotId(1L, 0, 20)).thenReturn(page);

    assertThat(service.listLotBids(1L, 0, 20)).isSameAs(page);
  }

  @Test
  void listMyBidsWithLotStatus_joinsLot() {
    Bid bid = Bid.create(1L, 2L, Money.fromString("10.00"), Instant.now(), "req").withId(1L);
    when(bidRepository.findByBidderId(2L, 0, 20)).thenReturn(new BidPage(List.of(bid), 0, 20, 1));
    when(lotRepository.findById(1L)).thenReturn(Optional.of(sampleLot()));

    var views = service.listMyBidsWithLotStatus(2L, 0, 20);

    assertThat(views).hasSize(1);
    assertThat(views.getFirst().lot().id()).isEqualTo(1L);
  }

  @Test
  void bidderDisplayName_fallsBackWhenMissing() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());
    assertThat(service.bidderDisplayName(99L)).isEqualTo("Bidder");
  }

  private static Lot sampleLot() {
    Instant now = Instant.now();
    return new Lot(
        1L,
        1L,
        "T",
        "D",
        1L,
        Money.fromString("10.00"),
        Money.fromString("1.00"),
        null,
        LotStatus.LIVE,
        now,
        now.plusSeconds(60),
        null,
        Money.fromString("10.00"),
        0,
        null,
        0,
        0L,
        now,
        now);
  }
}
