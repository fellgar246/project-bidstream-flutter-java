package com.bidstream.api.bid;

import com.bidstream.api.support.IntegrationTestInitializer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.RedisTestContainer;
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
class BidControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca051_bidTooLow_returnsMinimumCents() throws Exception {
    var fixture = liveLot();
    String buyer = registerBuyer("buyer-low@test.com");
    placeBid(buyer, fixture.lotId(), "100.00", "req-1", status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/lots/" + fixture.lotId() + "/bids")
                .header("Authorization", "Bearer " + buyer)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"102.00","clientRequestId":"req-2"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("bid_too_low"))
        .andExpect(jsonPath("$.error.details.minimumCents").value("10500"));
  }

  @Test
  void ca053_sellerBid_returnsBidSelf() throws Exception {
    String sellerToken = registerSeller("seller-self@test.com");
    long lotId = createLot(sellerToken);
    Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
    jdbcTemplate.update(
        """
        UPDATE lots SET status = 'LIVE', scheduled_start_at = ?, scheduled_end_at = ?,
          current_price_cents = 0, bid_count = 0 WHERE id = ?
        """,
        java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(end),
        lotId);

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"100.00","clientRequestId":"req-seller"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("bid_self"));
  }

  @Test
  void ca054_scheduledLot_returnsBidNotLive() throws Exception {
    String sellerToken = registerSeller("seller-sched-bid@test.com");
    long lotId = createLot(sellerToken);
    String buyer = registerBuyer("buyer-sched@test.com");

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + buyer)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"100.00","clientRequestId":"req-sched"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("bid_not_live"));
  }

  @Test
  void ca055_bidAfterEnd_returnsBidNotLive() throws Exception {
    String sellerToken = registerSeller("seller-end@test.com");
    long lotId = createLot(sellerToken);
    Instant past = Instant.now().minus(1, ChronoUnit.SECONDS);
    jdbcTemplate.update(
        """
        UPDATE lots SET status = 'LIVE', scheduled_start_at = ?, scheduled_end_at = ? WHERE id = ?
        """,
        java.sql.Timestamp.from(past.minus(1, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(past),
        lotId);
    String buyer = registerBuyer("buyer-end@test.com");

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + buyer)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"100.00","clientRequestId":"req-late"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("bid_not_live"));
  }

  @Test
  void ca058_antiSniping_extendsClose() throws Exception {
    String sellerToken = registerSeller("seller-snipe@test.com");
    long lotId = createLot(sellerToken);
    Instant end = Instant.now().plus(10, ChronoUnit.SECONDS);
    jdbcTemplate.update(
        """
        UPDATE lots SET status = 'LIVE', scheduled_start_at = ?, scheduled_end_at = ?,
          current_price_cents = 0, bid_count = 0 WHERE id = ?
        """,
        java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(end),
        lotId);
    String buyer = registerBuyer("buyer-snipe@test.com");

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + buyer)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"100.00","clientRequestId":"req-snipe"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.lot.extended").value(true));
  }

  private LiveLotFixture liveLot() throws Exception {
    String sellerToken = registerSeller("seller-func@test.com");
    long lotId = createLot(sellerToken);
    Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
    jdbcTemplate.update(
        """
        UPDATE lots SET status = 'LIVE', scheduled_start_at = ?, scheduled_end_at = ?,
          current_price_cents = 0, bid_count = 0 WHERE id = ?
        """,
        java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(end),
        lotId);
    return new LiveLotFixture(lotId, sellerToken);
  }

  private void placeBid(
      String token,
      long lotId,
      String amount,
      String clientRequestId,
      org.springframework.test.web.servlet.ResultMatcher expectedStatus)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"%s","clientRequestId":"%s"}
                    """
                        .formatted(amount, clientRequestId)))
        .andExpect(expectedStatus);
  }

  private String registerBuyer(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Buyer"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
    return login(email);
  }

  private String registerSeller(String email) throws Exception {
    registerBuyer(email);
    String token = login(email);
    mockMvc
        .perform(post("/api/v1/me/seller-application").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    return login(email);
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(APPLICATION_JSON)
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

  private record LiveLotFixture(long lotId, String sellerToken) {}
}
