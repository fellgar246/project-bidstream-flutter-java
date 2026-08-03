package com.bidstream.api.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class SearchIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  private long sellerId;
  private int runId;

  @BeforeEach
  void seedSpanishLots() throws Exception {
    runId = (int) (System.nanoTime() % 100000);
    jdbcTemplate.update("DELETE FROM bids");
    jdbcTemplate.update("DELETE FROM lots");
    sellerId = ensureSeller();
    insertLiveLot("Reloj suizo antiguo", "Pieza suiza en excelente estado", 1L, 12_000L);
    insertLiveLot("Cuadro moderno", "Arte abstracto contemporáneo", 1L, 80_000L);
    insertLiveLot("Reloj digital", "Cronómetro deportivo", 1L, 5_000L);
  }

  @Test
  void ca089_spanishStemmingFindsRelatedTitle() throws Exception {
    mockMvc
        .perform(get("/api/v1/lots").param("q", "relojes suizos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Reloj suizo antiguo"));
  }

  @Test
  void ca0810_specialCharactersDoNotBreakSearch() throws Exception {
    for (String query : new String[] {"reloj'", "reloj&suizo", "reloj!", "reloj:suizo"}) {
      mockMvc.perform(get("/api/v1/lots").param("q", query)).andExpect(status().isOk());
    }
  }

  @Test
  void ca0812_categoryFacetCountsSumToTotalResults() throws Exception {
    var result =
        mockMvc
            .perform(get("/api/v1/lots").param("q", "reloj").param("facets", "true"))
            .andExpect(status().isOk())
            .andReturn();

    long total =
        objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("page")
            .get("totalElements")
            .asLong();
    long facetSum =
        objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("facets")
            .get("categories")
            .findValues("count")
            .stream()
            .mapToLong(node -> node.asLong())
            .sum();
    assertThat(facetSum).isEqualTo(total);
  }

  private long ensureSeller() throws Exception {
    String email = "search-seller-" + runId + "@test.com";
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Search Seller"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/me/seller-application").header("Authorization", "Bearer " + login(email)))
        .andExpect(status().isOk());
    return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"password1234"}
                        """
                            .formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("accessToken")
        .asText();
  }

  private void insertLiveLot(String title, String description, long categoryId, long priceCents) {
    Instant end = Instant.now().plus(2, ChronoUnit.HOURS);
    jdbcTemplate.update(
        """
        INSERT INTO lots (
          seller_id, title, description, category_id, starting_price_cents, min_increment_cents,
          status, scheduled_start_at, scheduled_end_at, current_price_cents, bid_count,
          version, created_at, updated_at
        ) VALUES (?, ?, ?, ?, 10000, 500, 'LIVE', ?, ?, ?, 0, 0, NOW(), NOW())
        """,
        sellerId,
        title,
        description,
        categoryId,
        java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(end),
        priceCents);
  }
}
