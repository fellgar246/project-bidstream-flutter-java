package com.bidstream.application.lot;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes orphaned objects from MinIO when presigned uploads were never confirmed.
 *
 * <p>Orphans are inevitable with presigned URLs: the client may abandon the upload after the API
 * creates a PENDING row, or the upload may fail without calling confirm. This job reconciles storage
 * with the database for rows stuck in PENDING beyond the grace period.
 */
@Component
public class OrphanImageCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(OrphanImageCleanupJob.class);
  private static final Duration PENDING_GRACE = Duration.ofHours(1);

  private final LotImageRepository lotImageRepository;
  private final ObjectStoragePort objectStorage;
  private final Clock clock;

  public OrphanImageCleanupJob(
      LotImageRepository lotImageRepository, ObjectStoragePort objectStorage, Clock clock) {
    this.lotImageRepository = lotImageRepository;
    this.objectStorage = objectStorage;
    this.clock = clock;
  }

  @Scheduled(fixedRate = 3_600_000)
  public void cleanupOrphans() {
    var cutoff = clock.instant().minus(PENDING_GRACE);
    List<LotImage> orphans = lotImageRepository.findPendingOlderThan(cutoff);
    for (LotImage orphan : orphans) {
      try {
        objectStorage.deleteObject(orphan.storageKey());
      } catch (RuntimeException ex) {
        log.warn("Could not delete orphan object {}: {}", orphan.storageKey(), ex.getMessage());
      }
      lotImageRepository.deleteById(orphan.id());
      log.info("Removed orphan image id={} storageKey={}", orphan.id(), orphan.storageKey());
    }
  }
}
