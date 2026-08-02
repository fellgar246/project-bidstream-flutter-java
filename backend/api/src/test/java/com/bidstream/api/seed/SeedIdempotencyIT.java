package com.bidstream.api.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.infrastructure.seed.CategorySeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class SeedIdempotencyIT {

  @Autowired private CategorySeeder categorySeeder;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void seed_isIdempotent() {
    categorySeeder.seed();
    long firstCount = countCategories();

    categorySeeder.seed();
    long secondCount = countCategories();

    assertThat(firstCount).isGreaterThan(0);
    assertThat(secondCount).isEqualTo(firstCount);
  }

  private long countCategories() {
    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Long.class);
    return count == null ? 0L : count;
  }
}
