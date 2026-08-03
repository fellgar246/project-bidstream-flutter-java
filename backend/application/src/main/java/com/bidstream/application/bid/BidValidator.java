package com.bidstream.application.bid;

import com.bidstream.domain.bid.BidNotLiveException;
import com.bidstream.domain.bid.BidSelfException;
import com.bidstream.domain.bid.BidTooLowException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Validates RB-04 against a fresh lot snapshot. */
@Component
public class BidValidator {

  public void validate(Lot lot, long bidderId, Money amount, Instant now) {
    if (lot.status() != LotStatus.LIVE
        || lot.scheduledEndAt() == null
        || !now.isBefore(lot.scheduledEndAt())) {
      throw new BidNotLiveException();
    }
    if (lot.sellerId() == bidderId) {
      throw new BidSelfException();
    }
    Money minimum = lot.minimumNextBid();
    if (amount.compareTo(minimum) < 0) {
      throw new BidTooLowException(minimum);
    }
  }
}
