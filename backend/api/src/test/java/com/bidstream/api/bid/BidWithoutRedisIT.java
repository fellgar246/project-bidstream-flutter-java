package com.bidstream.api.bid;

import com.bidstream.api.support.IntegrationTestInitializer;
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
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class BidWithoutRedisIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca0514_redisStopped_stillMaintainsInvariants() throws Exception {
    RedisTestContainer.container().stop();

    long lotId = createLiveLot();
    List<String> buyerTokens = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      buyerTokens.add(registerBuyer("buyer-noredis-" + i + "@test.com"));
    }

    int threads = 50;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      final int index = i;
      final String token = buyerTokens.get(i);
      futures.add(
          pool.submit(
              () -> {
                try {
                  ready.countDown();
                  start.await();
                  return mockMvc
                      .perform(
                          post("/api/v1/lots/" + lotId + "/bids")
                              .header("Authorization", "Bearer " + token)
                              .contentType(APPLICATION_JSON)
                              .content(
                                  """
                                  {"amount":"%s","clientRequestId":"nr-%d"}
                                  """
                                      .formatted((100 + index) + ".00", index)))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }));
    }
    ready.await();
    start.countDown();

    int accepted = 0;
    for (Future<Integer> future : futures) {
      int status = future.get();
      if (status == 201 || status == 200) {
        accepted++;
      }
    }
    pool.shutdown();

    long bidRows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bids WHERE lot_id = ?", Long.class, lotId);
    int bidCount =
        jdbcTemplate.queryForObject(
            "SELECT bid_count FROM lots WHERE id = ?", Integer.class, lotId);
    long currentPrice =
        jdbcTemplate.queryForObject(
            "SELECT current_price_cents FROM lots WHERE id = ?", Long.class, lotId);

    assertThat(bidRows).isEqualTo(accepted);
    assertThat(bidCount).isEqualTo((int) bidRows);
    assertThat(currentPrice).isGreaterThan(0);

    if (!RedisTestContainer.container().isRunning()) {
      RedisTestContainer.container().start();
    }
  }

  private long createLiveLot() throws Exception {
    String sellerToken = registerSeller("seller-noredis@test.com");
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
    return lotId;
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
}
