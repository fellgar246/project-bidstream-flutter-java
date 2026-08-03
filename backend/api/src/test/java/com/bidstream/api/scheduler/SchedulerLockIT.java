package com.bidstream.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.application.auction.CloseDueLotsService;
import com.bidstream.application.auction.StartDueLotsService;
import com.bidstream.application.bid.DistributedLockPort;
import com.bidstream.domain.lot.LotStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
@TestPropertySource(properties = "bidstream.scheduler.enabled=false")
class SchedulerLockIT {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private StartDueLotsService startDueLotsService;
  @Autowired private CloseDueLotsService closeDueLotsService;
  @Autowired private DistributedLockPort distributedLock;

  private long sellerId;

  @BeforeEach
  void seedSeller() {
    sellerId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO users (email, password_hash, display_name, created_at, updated_at)
            VALUES (?, 'hash', 'Seller', NOW(), NOW())
            ON CONFLICT (email) DO UPDATE SET display_name = EXCLUDED.display_name
            RETURNING id
            """,
            Long.class,
            "scheduler-seller-" + System.nanoTime() + "@test.com");
  }

  @Test
  void ca0710_schedulerLockIsExclusive() {
    String lockKey = "lock:scheduler:close-due-lots";
    Optional<DistributedLockPort.LockToken> first =
        distributedLock.tryAcquire(lockKey, Duration.ofSeconds(5), Duration.ofMillis(100));
    assertThat(first).isPresent();
    try {
      Optional<DistributedLockPort.LockToken> second =
          distributedLock.tryAcquire(lockKey, Duration.ofSeconds(5), Duration.ofMillis(50));
      assertThat(second).isEmpty();
    } finally {
      distributedLock.release(first.get());
    }
  }

  @Test
  void ca071_scheduledLotStarts() {
    long lotId = insertScheduledLot();
    int started = startDueLotsService.startDueLots();
    assertThat(started).isGreaterThanOrEqualTo(1);
    String status =
        jdbcTemplate.queryForObject("SELECT status FROM lots WHERE id = ?", String.class, lotId);
    assertThat(status).isEqualTo(LotStatus.LIVE.name());
  }

  @Test
  void ca072_liveLotClosesOnce() {
    long lotId = insertLiveLotPastEnd();
    closeDueLotsService.closeDueLots();
    closeDueLotsService.closeDueLots();
    String status =
        jdbcTemplate.queryForObject("SELECT status FROM lots WHERE id = ?", String.class, lotId);
    assertThat(status).isEqualTo(LotStatus.CLOSED_NO_SALE.name());
    Integer events =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ? AND event_type LIKE 'LotClosed%'",
            Integer.class,
            lotId);
    assertThat(events).isEqualTo(1);
  }

  private long insertScheduledLot() {
    jdbcTemplate.update(
        """
        INSERT INTO lots (seller_id, title, description, category_id, starting_price_cents,
          min_increment_cents, status, scheduled_start_at, scheduled_end_at, current_price_cents,
          bid_count, extension_count, version, created_at, updated_at)
        VALUES (?, 'Sched', 'Desc', 1, 10000, 500, 'SCHEDULED', ?, ?, 0, 0, 0, 0, NOW(), NOW())
        """,
        sellerId,
        java.sql.Timestamp.from(Instant.now().minus(5, ChronoUnit.SECONDS)),
        java.sql.Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM lots", Long.class);
  }

  private long insertLiveLotPastEnd() {
    jdbcTemplate.update(
        """
        INSERT INTO lots (seller_id, title, description, category_id, starting_price_cents,
          min_increment_cents, status, scheduled_start_at, scheduled_end_at, current_price_cents,
          bid_count, extension_count, version, created_at, updated_at)
        VALUES (?, 'Live', 'Desc', 1, 10000, 500, 'LIVE', ?, ?, 0, 0, 0, 0, NOW(), NOW())
        """,
        sellerId,
        java.sql.Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM lots", Long.class);
  }
}
