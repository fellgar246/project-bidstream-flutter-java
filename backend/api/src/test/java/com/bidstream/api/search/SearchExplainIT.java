package com.bidstream.api.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.api.support.IntegrationTestInitializer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class SearchExplainIT {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca0811_searchUsesGinIndex() {
    jdbcTemplate.update(
        """
        INSERT INTO users (email, password_hash, display_name, created_at, updated_at)
        VALUES ('explain@test.com', 'hash', 'Explain', NOW(), NOW())
        ON CONFLICT (email) DO NOTHING
        """);
    long sellerId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'explain@test.com'", Long.class);
    jdbcTemplate.update(
        """
        INSERT INTO lots (
          seller_id, title, description, category_id, starting_price_cents, min_increment_cents,
          status, current_price_cents, bid_count, version, created_at, updated_at
        ) VALUES (?, 'Reloj suizo', 'Suizo antiguo', 1, 10000, 500, 'LIVE', 10000, 0, 0, NOW(), NOW())
        """,
        sellerId);
    jdbcTemplate.execute("ANALYZE lots");
    for (int i = 0; i < 500; i++) {
      jdbcTemplate.update(
          """
          INSERT INTO lots (
            seller_id, title, description, category_id, starting_price_cents, min_increment_cents,
            status, current_price_cents, bid_count, version, created_at, updated_at
          ) VALUES (?, ?, 'desc', 1, 10000, 500, 'LIVE', 10000, 0, 0, NOW(), NOW())
          """,
          sellerId,
          "Reloj suizo " + i);
    }
    jdbcTemplate.execute("ANALYZE lots");
    jdbcTemplate.execute("DROP INDEX IF EXISTS idx_lots_category");
    jdbcTemplate.execute("DROP INDEX IF EXISTS idx_lots_status_end");
    jdbcTemplate.execute("SET enable_seqscan TO off");

    List<String> plan =
        jdbcTemplate.query(
            """
            EXPLAIN
            SELECT l.id
            FROM lots l
            WHERE l.search_vector @@ websearch_to_tsquery('spanish', ?)
              AND l.status = 'LIVE'
            ORDER BY ts_rank(l.search_vector, websearch_to_tsquery('spanish', ?)) DESC, l.id ASC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getString(1),
            "reloj suizo",
            "reloj suizo");
    assertThat(String.join("\n", plan)).contains("Bitmap Index Scan on idx_lots_search");
  }
}
