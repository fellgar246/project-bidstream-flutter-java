package com.bidstream.api.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.RedisTestContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
@ContextConfiguration(
    initializers = {PostgresTestContainer.Initializer.class, RedisTestContainer.Initializer.class})
class BidIdempotencyIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca056_repeatRequest_returnsSameBidWithoutMutation() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    LiveLotFixture fixture = createLiveLot("seller-idem-" + suffix + "@test.com");
    String buyerToken = registerBuyer("buyer-idem-" + suffix + "@test.com");

    MvcResult first =
        placeBid(buyerToken, fixture.lotId(), "105.00", "client-req-1", status().isCreated());
    long bidId =
        objectMapper.readTree(first.getResponse().getContentAsString()).at("/bid/id").asLong();

    MvcResult second =
        placeBid(buyerToken, fixture.lotId(), "105.00", "client-req-1", status().isOk());
    long replayId =
        objectMapper.readTree(second.getResponse().getContentAsString()).at("/bid/id").asLong();

    assertThat(replayId).isEqualTo(bidId);
    assertThat(countBids(fixture.lotId())).isEqualTo(1);
    assertThat(currentPriceCents(fixture.lotId())).isEqualTo(10500L);
    assertThat(bidCount(fixture.lotId())).isEqualTo(1);
  }

  @Test
  void ca057_parallelIdenticalRequests_produceSingleBidRow() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    LiveLotFixture fixture = createLiveLot("seller-par-" + suffix + "@test.com");
    String buyerToken = registerBuyer("buyer-par-" + suffix + "@test.com");
    int threads = 2;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      futures.add(
          pool.submit(
              () -> {
                try {
                  ready.countDown();
                  start.await();
                  return placeBidWithRetry(buyerToken, fixture.lotId(), "105.00", "parallel-req");
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }));
    }
    ready.await();
    start.countDown();

    List<Long> bidIds = new ArrayList<>();
    for (Future<Integer> future : futures) {
      int httpStatus = future.get();
      assertThat(httpStatus).isIn(200, 201);
      // Re-fetch bid id via GET is heavy; query DB for the single bid row instead.
    }
    pool.shutdown();

    long bidId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM bids WHERE lot_id = ? LIMIT 1", Long.class, fixture.lotId());
    assertThat(bidId).isPositive();
    assertThat(countBids(fixture.lotId())).isEqualTo(1);
  }

  private int placeBidWithRetry(String token, long lotId, String amount, String clientRequestId)
      throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      int status = placeBid(token, lotId, amount, clientRequestId, null).getResponse().getStatus();
      if (status != 503) {
        return status;
      }
      Thread.sleep(50);
    }
    return placeBid(token, lotId, amount, clientRequestId, null).getResponse().getStatus();
  }

  private MvcResult placeBid(
      String token,
      long lotId,
      String amount,
      String clientRequestId,
      org.springframework.test.web.servlet.ResultMatcher expectedStatus)
      throws Exception {
    var request =
        post("/api/v1/lots/" + lotId + "/bids")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content(
                """
                {"amount":"%s","clientRequestId":"%s"}
                """
                    .formatted(amount, clientRequestId));
    if (expectedStatus != null) {
      return mockMvc.perform(request).andExpect(expectedStatus).andReturn();
    }
    return mockMvc.perform(request).andReturn();
  }

  private LiveLotFixture createLiveLot(String sellerEmail) throws Exception {
    String sellerToken = registerSeller(sellerEmail);
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
    return new LiveLotFixture(lotId);
  }

  private long countBids(long lotId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM bids WHERE lot_id = ?", Long.class, lotId);
  }

  private long currentPriceCents(long lotId) {
    return jdbcTemplate.queryForObject(
        "SELECT current_price_cents FROM lots WHERE id = ?", Long.class, lotId);
  }

  private int bidCount(long lotId) {
    return jdbcTemplate.queryForObject(
        "SELECT bid_count FROM lots WHERE id = ?", Integer.class, lotId);
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

  private record LiveLotFixture(long lotId) {}
}
