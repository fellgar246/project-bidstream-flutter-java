package com.bidstream.application.cache;

import java.time.Duration;
import java.util.Optional;

public interface LotCachePort {

  Optional<CachedLotSnapshot> getLot(long lotId);

  void putLot(long lotId, CachedLotSnapshot snapshot, Duration ttl);

  void invalidateLot(long lotId);

  Optional<CachedLotPageSnapshot> getLotPage(String pageKey);

  void putLotPage(String pageKey, CachedLotPageSnapshot page, Duration ttl);

  long getCatalogVersion();

  long incrementCatalogVersion();
}
