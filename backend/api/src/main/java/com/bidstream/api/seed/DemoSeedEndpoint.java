package com.bidstream.api.seed;

import com.bidstream.infrastructure.seed.DemoSeeder;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "demoSeed")
@ConditionalOnProperty(name = "bidstream.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoSeedEndpoint {

  private final DemoSeeder demoSeeder;

  public DemoSeedEndpoint(DemoSeeder demoSeeder) {
    this.demoSeeder = demoSeeder;
  }

  @WriteOperation
  public DemoSeeder.DemoSeedResult seed() {
    return demoSeeder.seed();
  }
}
