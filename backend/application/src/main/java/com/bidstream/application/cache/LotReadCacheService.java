package com.bidstream.application.cache;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LotReadCacheService {

  private static final Duration LIVE_LOT_TTL = Duration.ofSeconds(60);
  private static final Duration STATIC_LOT_TTL = Duration.ofMinutes(10);
  private static final Duration PAGE_TTL = Duration.ofSeconds(30);

  private final LotCachePort lotCachePort;
  private final CachedLotSnapshotBuilder snapshotBuilder;
  private final LotCacheLoader lotCacheLoader;

  public LotReadCacheService(
      LotCachePort lotCachePort,
      CachedLotSnapshotBuilder snapshotBuilder,
      LotCacheLoader lotCacheLoader) {
    this.lotCachePort = lotCachePort;
    this.snapshotBuilder = snapshotBuilder;
    this.lotCacheLoader = lotCacheLoader;
  }

  public CachedLotSnapshot getLotSnapshot(long lotId) {
    Optional<CachedLotSnapshot> cached = lotCachePort.getLot(lotId);
    if (cached.isPresent()) {
      return cached.get();
    }
    Lot lot = lotCacheLoader.loadLot(lotId);
    CachedLotSnapshot snapshot = snapshotBuilder.build(lot);
    lotCachePort.putLot(lotId, snapshot, ttlFor(lot.status()));
    return snapshot;
  }

  public CachedLotPageSnapshot getLotPage(LotQuery query, LotPageLoader pageLoader) {
    long catalogVersion = lotCachePort.getCatalogVersion();
    String pageKey = buildPageCacheKey(query, catalogVersion);
    Optional<CachedLotPageSnapshot> cached = lotCachePort.getLotPage(pageKey);
    if (cached.isPresent()) {
      return cached.get();
    }
    LotPage page = pageLoader.load(query);
    List<CachedLotSnapshot> content = page.content().stream().map(snapshotBuilder::build).toList();
    CachedLotPageSnapshot snapshot =
        new CachedLotPageSnapshot(content, page.page(), page.size(), page.totalElements());
    lotCachePort.putLotPage(pageKey, snapshot, PAGE_TTL);
    return snapshot;
  }

  static String buildPageCacheKey(LotQuery query, long catalogVersion) {
    String raw =
        String.join(
            "|",
            query.status().map(Enum::name).orElse(""),
            query.categoryId().map(String::valueOf).orElse(""),
            query.minPriceCents().map(String::valueOf).orElse(""),
            query.maxPriceCents().map(String::valueOf).orElse(""),
            query.sellerId().map(String::valueOf).orElse(""),
            query.searchQuery().orElse(""),
            String.valueOf(query.page()),
            String.valueOf(query.size()),
            query.sort().name(),
            String.valueOf(query.facets()),
            "v" + catalogVersion);
    return sha256(raw);
  }

  private static Duration ttlFor(LotStatus status) {
    return status == LotStatus.LIVE ? LIVE_LOT_TTL : STATIC_LOT_TTL;
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }

  @FunctionalInterface
  public interface LotPageLoader {
    LotPage load(LotQuery query);
  }
}
