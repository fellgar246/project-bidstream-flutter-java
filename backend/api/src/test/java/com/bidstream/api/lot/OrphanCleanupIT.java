package com.bidstream.api.lot;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.api.support.MinioTestContainer;
import com.bidstream.application.lot.OrphanImageCleanupJob;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class OrphanCleanupIT {

  @Autowired private OrphanImageCleanupJob orphanImageCleanupJob;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static MinioClient minioClient;

  @BeforeAll
  static void setUpMinio() {
    minioClient =
        MinioClient.builder()
            .endpoint(MinioTestContainer.endpoint())
            .credentials(MinioTestContainer.ACCESS_KEY, MinioTestContainer.SECRET_KEY)
            .build();
  }

  @Test
  void ca049_removesStalePendingImagesAndLeavesRecentOnes() throws Exception {
    long lotId = insertDraftLot();
    String staleKey = "lots/" + lotId + "/stale.jpg";
    String recentKey = "lots/" + lotId + "/recent.jpg";
    putObject(staleKey);
    putObject(recentKey);

    Instant staleCreated = Instant.now().minus(2, ChronoUnit.HOURS);
    Instant recentCreated = Instant.now().minus(10, ChronoUnit.MINUTES);
    insertPendingImage(lotId, staleKey, 0, staleCreated);
    insertPendingImage(lotId, recentKey, 1, recentCreated);

    orphanImageCleanupJob.cleanupOrphans();

    assertThat(countImages(staleKey)).isZero();
    assertThat(countImages(recentKey)).isEqualTo(1);
    assertThat(objectExists(staleKey)).isFalse();
    assertThat(objectExists(recentKey)).isTrue();
  }

  private long insertDraftLot() {
    Long sellerId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", Long.class);
    if (sellerId == null) {
      jdbcTemplate.update(
          """
          INSERT INTO users (email, password_hash, display_name, created_at, updated_at)
          VALUES ('orphan@test.com', 'hash', 'Orphan', NOW(), NOW())
          """);
      sellerId =
          jdbcTemplate.queryForObject(
              "SELECT id FROM users WHERE email = ?", Long.class, "orphan@test.com");
    }
    Instant now = Instant.now();
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO lots (seller_id, title, description, category_id, starting_price_cents,
          min_increment_cents, status, current_price_cents, bid_count, extension_count, version,
          created_at, updated_at)
        VALUES (?, 'Orphan lot', 'Desc', 1, 10000, 500, 'DRAFT', 10000, 0, 0, 0, ?, ?)
        RETURNING id
        """,
        Long.class,
        sellerId,
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(now));
  }

  private void insertPendingImage(long lotId, String storageKey, int position, Instant createdAt) {
    jdbcTemplate.update(
        """
        INSERT INTO lot_images (lot_id, storage_key, position, content_type, size_bytes, status,
          created_at, updated_at)
        VALUES (?, ?, ?, 'image/jpeg', 10, 'PENDING', ?, ?)
        """,
        lotId,
        storageKey,
        position,
        java.sql.Timestamp.from(createdAt),
        java.sql.Timestamp.from(createdAt));
  }

  private int countImages(String storageKey) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lot_images WHERE storage_key = ?", Integer.class, storageKey);
    return count == null ? 0 : count;
  }

  private void putObject(String storageKey) throws Exception {
    byte[] data = "orphan-data".getBytes();
    minioClient.putObject(
        PutObjectArgs.builder().bucket(MinioTestContainer.BUCKET).object(storageKey).stream(
                new ByteArrayInputStream(data), data.length, -1)
            .contentType("image/jpeg")
            .build());
  }

  private boolean objectExists(String storageKey) {
    try {
      minioClient.statObject(
          StatObjectArgs.builder().bucket(MinioTestContainer.BUCKET).object(storageKey).build());
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}
