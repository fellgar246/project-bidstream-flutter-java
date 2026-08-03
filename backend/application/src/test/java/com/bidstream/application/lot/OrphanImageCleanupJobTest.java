package com.bidstream.application.lot;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import com.bidstream.domain.lot.LotImageStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrphanImageCleanupJobTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private LotImageRepository lotImageRepository;
  @Mock private ObjectStoragePort objectStorage;

  private OrphanImageCleanupJob cleanupJob;

  @BeforeEach
  void setUp() {
    cleanupJob = new OrphanImageCleanupJob(lotImageRepository, objectStorage, CLOCK);
  }

  @Test
  void cleanupOrphans_deletesStalePendingRows() {
    LotImage stale =
        new LotImage(
            1L,
            10L,
            "lots/10/stale.jpg",
            null,
            0,
            "image/jpeg",
            100L,
            LotImageStatus.PENDING,
            NOW.minus(2, ChronoUnit.HOURS),
            NOW.minus(2, ChronoUnit.HOURS));
    when(lotImageRepository.findPendingOlderThan(NOW.minus(1, ChronoUnit.HOURS)))
        .thenReturn(List.of(stale));

    cleanupJob.cleanupOrphans();

    verify(objectStorage).deleteObject("lots/10/stale.jpg");
    verify(lotImageRepository).deleteById(1L);
  }
}
