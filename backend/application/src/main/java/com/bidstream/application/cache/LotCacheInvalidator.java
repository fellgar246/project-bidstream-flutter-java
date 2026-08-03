package com.bidstream.application.cache;

import org.springframework.stereotype.Component;

@Component
public class LotCacheInvalidator {

  private final LotCachePort lotCachePort;

  public LotCacheInvalidator(LotCachePort lotCachePort) {
    this.lotCachePort = lotCachePort;
  }

  public void invalidateLot(long lotId) {
    lotCachePort.invalidateLot(lotId);
  }

  public void invalidateCatalog() {
    lotCachePort.incrementCatalogVersion();
  }

  public void invalidateLotAndCatalog(long lotId) {
    invalidateLot(lotId);
    invalidateCatalog();
  }
}
