package com.bidstream.api.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class LotControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca031_buyerCannotCreateLot() throws Exception {
    String token = registerAndLogin("buyer-lot@test.com", "password1234", "Buyer");

    mockMvc
        .perform(
            post("/api/v1/lots")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"title":"Watch","description":"Desc","categoryId":1,
                     "startingPrice":"100.00","minIncrement":"5.00"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("forbidden"));
  }

  @Test
  void ca032_editForeignLot_returns403() throws Exception {
    String sellerToken = registerSeller("seller-a@test.com");
    long lotId = createLot(sellerToken);

    String otherSellerToken = registerSeller("seller-b@test.com");
    mockMvc
        .perform(
            patch("/api/v1/lots/" + lotId)
                .header("Authorization", "Bearer " + otherSellerToken)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"title":"Hacked","description":"Desc","categoryId":1,
                     "startingPrice":"100.00","minIncrement":"5.00"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("forbidden"));
  }

  @Test
  void ca033_patchLiveLot_returnsInvalidTransition() throws Exception {
    String token = registerSeller("seller-live@test.com");
    long lotId = createLot(token);
    setLotStatus(lotId, "LIVE");

    mockMvc
        .perform(
            patch("/api/v1/lots/" + lotId)
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"title":"Updated","description":"Desc","categoryId":1,
                     "startingPrice":"100.00","minIncrement":"5.00"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("invalid_transition"));
  }

  @Test
  void ca034_scheduleWithInvalidWindow_returnsValidationError() throws Exception {
    String token = registerSeller("seller-sched@test.com");
    long lotId = createLot(token);
    insertReadyImage(lotId);
    Instant start = Instant.now().plus(10, ChronoUnit.MINUTES);
    Instant end = start.minus(1, ChronoUnit.MINUTES);

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/schedule")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"scheduledStartAt":"%s","scheduledEndAt":"%s"}
                    """
                        .formatted(start, end)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("validation_error"))
        .andExpect(jsonPath("$.error.details.scheduledEndAt").exists());
  }

  @Test
  void ca035_cancelAlreadyCancelled_returns409() throws Exception {
    String token = registerSeller("seller-cancel@test.com");
    long lotId = createLot(token);
    cancelLot(token, lotId);

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/cancel").header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("invalid_transition"));
  }

  @Test
  void ca036_reservePriceNeverSerialized() throws Exception {
    String token = registerSeller("seller-reserve@test.com");
    long lotId = createLotWithReserve(token, "150.00");

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/lots/" + lotId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasReserve").value(true))
            .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain("reservePrice");
  }

  @Test
  void ca037_paginationStableWithSameEndTime() throws Exception {
    registerSeller("seller-page@test.com");
    long sellerId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, "seller-page@test.com");
    Instant end = Instant.now().plus(2, ChronoUnit.DAYS);
    for (int i = 0; i < 25; i++) {
      insertLiveLot(sellerId, "Lot " + i, end, 1000L + i);
    }

    var page0 =
        fetchLotIds(
            "/api/v1/lots?status=LIVE&sellerId=" + sellerId + "&size=10&page=0&sort=endingSoon");
    var page1 =
        fetchLotIds(
            "/api/v1/lots?status=LIVE&sellerId=" + sellerId + "&size=10&page=1&sort=endingSoon");
    var page2 =
        fetchLotIds(
            "/api/v1/lots?status=LIVE&sellerId=" + sellerId + "&size=10&page=2&sort=endingSoon");

    var all = new java.util.ArrayList<Long>();
    all.addAll(page0);
    all.addAll(page1);
    all.addAll(page2);

    assertThat(all).doesNotHaveDuplicates();
    assertThat(all).hasSize(25);
  }

  @Test
  void ca038_publicListNeverReturnsDraftLots() throws Exception {
    String token = registerSeller("seller-draft@test.com");
    createLot(token);

    mockMvc
        .perform(get("/api/v1/lots?status=DRAFT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  private java.util.List<Long> fetchLotIds(String url) throws Exception {
    MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    JsonNode content =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
    java.util.List<Long> ids = new java.util.ArrayList<>();
    content.forEach(node -> ids.add(node.get("id").asLong()));
    return ids;
  }

  private String registerAndLogin(String email, String password, String displayName)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"%s","displayName":"%s"}
                    """
                        .formatted(email, password, displayName)))
        .andExpect(status().isCreated());
    return login(email, password);
  }

  private String registerSeller(String email) throws Exception {
    String token = registerAndLogin(email, "password1234", "Seller");
    mockMvc
        .perform(post("/api/v1/me/seller-application").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    return login(email, "password1234");
  }

  private String login(String email, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"%s"}
                        """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("accessToken")
        .asText();
  }

  private long createLot(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/lots")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Vintage watch","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private long createLotWithReserve(String token, String reserve) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/lots")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Reserve lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00","reservePrice":"%s"}
                        """
                            .formatted(reserve)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private void cancelLot(String token, long lotId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/cancel").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  private void setLotStatus(long lotId, String status) {
    jdbcTemplate.update("UPDATE lots SET status = ? WHERE id = ?", status, lotId);
  }

  private void insertReadyImage(long lotId) {
    Instant now = Instant.now();
    jdbcTemplate.update(
        """
        INSERT INTO lot_images (lot_id, storage_key, position, content_type, size_bytes, status,
          created_at, updated_at)
        VALUES (?, ?, 0, 'image/jpeg', 1024, 'READY', ?, ?)
        """,
        lotId,
        "lots/" + lotId + "/ready.jpg",
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(now));
  }

  private void insertLiveLot(long sellerId, String title, Instant end, long currentPriceCents) {
    Instant now = Instant.now();
    jdbcTemplate.update(
        """
        INSERT INTO lots (seller_id, title, description, category_id,
          starting_price_cents, min_increment_cents, status,
          scheduled_start_at, scheduled_end_at, current_price_cents,
          bid_count, extension_count, version, created_at, updated_at)
        VALUES (?, ?, 'Desc', 1, 10000, 500, 'LIVE', ?, ?, ?, 0, 0, 0, ?, ?)
        """,
        sellerId,
        title,
        java.sql.Timestamp.from(end.minus(1, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(end),
        currentPriceCents,
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(now));
  }
}
