package com.bidstream.api.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
class MetricsIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca101_prometheusExposesBusinessMetrics() throws Exception {
    seedLiveLotWithBid();

    MvcResult result =
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andReturn();
    String body = result.getResponse().getContentAsString();

    assertThat(body).contains("bidstream_bids_placed_total");
    assertThat(body).contains("result=\"accepted\"");
    assertThat(body).contains("bidstream_bid_latency");
    assertThat(body).contains("bidstream_bid_retries");
    assertThat(body).contains("bidstream_lock_wait");
    assertThat(body).contains("bidstream_lots_live");
    assertThat(body).contains("bidstream_outbox_pending");
    assertThat(body).contains("bidstream_outbox_publish_latency");
    assertThat(body).contains("bidstream_ws_sessions");
    assertThat(body).contains("bidstream_auction_closed");
  }

  @Test
  void ca102_bidMetricsMatchRealOutcomes() throws Exception {
    var fixture = liveLot();
    String buyer = registerBuyer("metrics-buyer@test.com");

    placeBid(buyer, fixture.lotId(), "100.00", "metrics-req-1", status().isCreated());
    placeBid(buyer, fixture.lotId(), "102.00", "metrics-req-2", status().isConflict());

    MvcResult result =
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andReturn();
    String body = result.getResponse().getContentAsString();

    assertThat(countMetric(body, "bidstream_bids_placed_total", "accepted"))
        .isGreaterThanOrEqualTo(1.0);
    assertThat(countMetric(body, "bidstream_bids_placed_total", "too_low"))
        .isGreaterThanOrEqualTo(1.0);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bids WHERE lot_id = ?", Integer.class, fixture.lotId()))
        .isEqualTo(1);
  }

  private void seedLiveLotWithBid() throws Exception {
    var fixture = liveLot();
    String buyer = registerBuyer("metrics-seed@test.com");
    placeBid(buyer, fixture.lotId(), "100.00", "metrics-seed-req", status().isCreated());
  }

  private double countMetric(String prometheus, String metric, String resultLabel) {
    for (String line : prometheus.split("\n")) {
      if (line.startsWith(metric) && line.contains("result=\"" + resultLabel + "\"")) {
        return Double.parseDouble(line.substring(line.lastIndexOf(' ') + 1));
      }
    }
    return 0.0;
  }

  private record LiveLotFixture(long lotId, String sellerToken) {}

  private LiveLotFixture liveLot() throws Exception {
    String sellerToken = registerSeller("metrics-seller@test.com");
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":"%s","clientRequestId":"%s"}
                    """
                        .formatted(amount, clientRequestId)))
        .andExpect(expectedStatus);
  }

  private String registerBuyer(String email) throws Exception {
    String uniqueEmail = email.replace("@", "+" + UUID.randomUUID() + "@");
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Buyer"}
                    """
                        .formatted(uniqueEmail)))
        .andExpect(status().isCreated());
    return login(uniqueEmail);
  }

  private String registerSeller(String email) throws Exception {
    String uniqueEmail = email.replace("@", "+" + UUID.randomUUID() + "@");
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Seller"}
                    """
                        .formatted(uniqueEmail)))
        .andExpect(status().isCreated());
    String token = login(uniqueEmail);
    mockMvc
        .perform(post("/api/v1/me/seller-application").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    return login(uniqueEmail);
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Metrics lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }
}
