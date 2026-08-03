package com.bidstream.application.scheduler;

import com.bidstream.application.auction.CloseDueLotsService;
import com.bidstream.application.auction.StartDueLotsService;
import com.bidstream.application.bid.DistributedLockPort;
import com.bidstream.application.outbox.OutboxRelayService;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "bidstream.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AuctionSchedulerJobs {

  private static final Logger log = LoggerFactory.getLogger(AuctionSchedulerJobs.class);
  private static final Duration LOCK_TTL = Duration.ofSeconds(5);
  private static final Duration LOCK_WAIT = Duration.ofMillis(50);

  private static final String LOCK_START = "lock:scheduler:start-due-lots";
  private static final String LOCK_CLOSE = "lock:scheduler:close-due-lots";
  private static final String LOCK_OUTBOX = "lock:scheduler:publish-outbox";

  private final StartDueLotsService startDueLotsService;
  private final CloseDueLotsService closeDueLotsService;
  private final OutboxRelayService outboxRelayService;
  private final DistributedLockPort distributedLock;

  public AuctionSchedulerJobs(
      StartDueLotsService startDueLotsService,
      CloseDueLotsService closeDueLotsService,
      OutboxRelayService outboxRelayService,
      DistributedLockPort distributedLock) {
    this.startDueLotsService = startDueLotsService;
    this.closeDueLotsService = closeDueLotsService;
    this.outboxRelayService = outboxRelayService;
    this.distributedLock = distributedLock;
  }

  @Scheduled(fixedRate = 1_000)
  public void startDueLots() {
    runWithLock(LOCK_START, () -> startDueLotsService.startDueLots());
  }

  @Scheduled(fixedRate = 1_000)
  public void closeDueLots() {
    runWithLock(LOCK_CLOSE, () -> closeDueLotsService.closeDueLots());
  }

  @Scheduled(fixedRate = 500)
  public void publishOutbox() {
    runWithLock(LOCK_OUTBOX, () -> outboxRelayService.relayBatch());
  }

  private void runWithLock(String lockKey, Runnable job) {
    Optional<DistributedLockPort.LockToken> lock =
        distributedLock.tryAcquire(lockKey, LOCK_TTL, LOCK_WAIT);
    if (lock.isEmpty()) {
      return;
    }
    try {
      job.run();
    } catch (RuntimeException ex) {
      log.warn("Scheduler job {} failed: {}", lockKey, ex.getMessage());
    } finally {
      distributedLock.release(lock.get());
    }
  }
}
