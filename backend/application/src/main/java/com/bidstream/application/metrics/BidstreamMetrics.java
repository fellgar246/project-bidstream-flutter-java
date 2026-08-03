package com.bidstream.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class BidstreamMetrics {

  private final MeterRegistry registry;
  private final DistributionSummary bidRetries;
  private final Timer outboxPublishLatency;

  public BidstreamMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.bidRetries =
        DistributionSummary.builder("bidstream.bid.retries")
            .description("Optimistic lock retries per bid attempt")
            .register(registry);
    this.outboxPublishLatency =
        Timer.builder("bidstream.outbox.publish.latency")
            .description("Time to publish an outbox event to RabbitMQ")
            .register(registry);
    registerCounter("bidstream.bids.placed", "result", "accepted");
    registerCounter("bidstream.bids.placed", "result", "too_low");
    registerCounter("bidstream.bids.placed", "result", "conflict");
    registerCounter("bidstream.bids.placed", "result", "rate_limited");
    registerCounter("bidstream.auction.closed", "outcome", "sold");
    registerCounter("bidstream.auction.closed", "outcome", "no_sale");
    Timer.builder("bidstream.bid.latency").tag("outcome", "accepted").register(registry);
    Timer.builder("bidstream.lock.wait").tag("acquired", "true").register(registry);
    Timer.builder("bidstream.lock.wait").tag("acquired", "false").register(registry);
  }

  private void registerCounter(String name, String tagKey, String tagValue) {
    Counter.builder(name).tag(tagKey, tagValue).register(registry);
  }

  public void recordBidPlaced(String result) {
    registry.counter("bidstream.bids.placed", "result", result).increment();
  }

  public Timer.Sample startBidLatency() {
    return Timer.start(registry);
  }

  public void recordBidLatency(Timer.Sample sample, String outcome) {
    sample.stop(
        Timer.builder("bidstream.bid.latency")
            .tag("outcome", outcome)
            .description("End-to-end bid placement latency")
            .register(registry));
  }

  public void recordBidRetries(int retries) {
    if (retries > 0) {
      bidRetries.record(retries);
    }
  }

  public void recordLockWait(Duration duration, boolean acquired) {
    Timer.builder("bidstream.lock.wait")
        .tag("acquired", Boolean.toString(acquired))
        .description("Distributed lock acquisition wait time")
        .register(registry)
        .record(duration);
  }

  public void recordAuctionClosed(String outcome) {
    registry.counter("bidstream.auction.closed", "outcome", outcome).increment();
  }

  public void recordOutboxPublishLatency(Duration duration) {
    outboxPublishLatency.record(duration);
  }
}
