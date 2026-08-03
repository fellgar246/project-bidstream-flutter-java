package com.bidstream.application.cache;

import java.time.Duration;
import java.util.Optional;

public interface CategoryCachePort {

  Optional<String> getTreeJson();

  void putTreeJson(String json, Duration ttl);

  void invalidate();
}
