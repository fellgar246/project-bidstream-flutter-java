package com.bidstream.application.bid;

import com.bidstream.application.metrics.BidstreamMetrics;
import com.bidstream.domain.bid.BidConflictException;
import com.bidstream.domain.bid.BidTooLowException;
import com.bidstream.domain.bid.LockTimeoutException;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.ratelimit.RateLimitExceededException;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class PlaceBidUseCase {

  private final PlaceBidService placeBidService;
  private final BidstreamMetrics metrics;

  public PlaceBidUseCase(PlaceBidService placeBidService, BidstreamMetrics metrics) {
    this.placeBidService = placeBidService;
    this.metrics = metrics;
  }

  public PlaceBidService.PlaceBidOutcome execute(
      long lotId, long bidderId, Money amount, String clientRequestId) {
    Timer.Sample sample = metrics.startBidLatency();
    try {
      PlaceBidService.PlaceBidOutcome outcome =
          placeBidService.placeBid(lotId, bidderId, amount, clientRequestId);
      metrics.recordBidPlaced("accepted");
      metrics.recordBidLatency(sample, "accepted");
      return outcome;
    } catch (BidTooLowException ex) {
      metrics.recordBidPlaced("too_low");
      metrics.recordBidLatency(sample, "too_low");
      throw ex;
    } catch (BidConflictException ex) {
      metrics.recordBidPlaced("conflict");
      metrics.recordBidLatency(sample, "conflict");
      throw ex;
    } catch (LockTimeoutException ex) {
      metrics.recordBidPlaced("conflict");
      metrics.recordBidLatency(sample, "conflict");
      throw ex;
    } catch (RateLimitExceededException ex) {
      metrics.recordBidPlaced("rate_limited");
      metrics.recordBidLatency(sample, "rate_limited");
      throw ex;
    }
  }
}
