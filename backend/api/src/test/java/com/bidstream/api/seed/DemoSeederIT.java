package com.bidstream.api.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.infrastructure.seed.DemoSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class DemoSeederIT {

  @Autowired private DemoSeeder demoSeeder;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca1012_seedIsIdempotent() {
    DemoSeeder.DemoSeedResult first = demoSeeder.seed();
    int usersAfterFirst = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    int lotsAfterFirst = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lots", Integer.class);
    int bidsAfterFirst = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bids", Integer.class);

    DemoSeeder.DemoSeedResult second = demoSeeder.seed();

    assertThat(first.lots()).isGreaterThanOrEqualTo(40);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class))
        .isEqualTo(usersAfterFirst);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lots", Integer.class))
        .isEqualTo(lotsAfterFirst);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bids", Integer.class))
        .isEqualTo(bidsAfterFirst);
    assertThat(second.bidsCreated()).isZero();
  }
}
