package com.bidstream.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.infrastructure.ratelimit.DistributedRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class RateLimitIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private StringRedisTemplate redisTemplate;
  @Autowired private RedisClient rateLimitRedisClient;

  @BeforeEach
  void cleanRedis() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Test
  void ca086_eleventhBidInTenSeconds_returns429WithoutBidRow() throws Exception {
    LiveFixture fixture = liveFixture("seller-rl@test.com", "buyer-rl@test.com");
    long bidsBefore = countBids(fixture.lotId());

    for (int i = 0; i < 10; i++) {
      long amountCents = 10_000L + i * 500L;
      placeBid(
          fixture.buyerToken(), fixture.lotId(), "req-" + i, amountCents, status().isCreated());
    }

    mockMvc
        .perform(
            post("/api/v1/lots/" + fixture.lotId() + "/bids")
                .header("Authorization", "Bearer " + fixture.buyerToken())
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"200.00","clientRequestId":"req-11"}
                    """))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error.code").value("rate_limited"))
        .andExpect(header().exists("Retry-After"));

    assertThat(countBids(fixture.lotId())).isEqualTo(bidsBefore + 10);
  }

  @Test
  void ca087_rateLimitIsScopedPerLot() throws Exception {
    LiveFixture lot1 = liveFixture("seller-rl1@test.com", "buyer-rl1@test.com");
    LiveFixture lot2 = liveFixture("seller-rl2@test.com", "buyer-rl1@test.com");

    for (int i = 0; i < 10; i++) {
      placeBid(
          lot1.buyerToken(), lot1.lotId(), "lot1-" + i, 10_000L + i * 500L, status().isCreated());
    }

    placeBid(lot2.buyerToken(), lot2.lotId(), "lot2-1", 10_000L, status().isCreated());
  }

  @Test
  void ca088_rateLimitSharedAcrossInstances() {
    RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
    try (StatefulRedisConnection<String, byte[]> connection_a =
            rateLimitRedisClient.connect(codec);
        StatefulRedisConnection<String, byte[]> connection_b =
            rateLimitRedisClient.connect(codec)) {
      ProxyManager<String> proxyA = LettuceBasedProxyManager.builderFor(connection_a).build();
      ProxyManager<String> proxyB = LettuceBasedProxyManager.builderFor(connection_b).build();
      DistributedRateLimiter limiterA = new DistributedRateLimiter(proxyA);
      DistributedRateLimiter limiterB = new DistributedRateLimiter(proxyB);

      for (int i = 0; i < 10; i++) {
        assertThat(limiterA.tryConsumeBid(99L, 1L).allowed()).isTrue();
      }
      assertThat(limiterB.tryConsumeBid(99L, 1L).allowed()).isFalse();
    }
  }

  private long countBids(long lotId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bids WHERE lot_id = ?", Long.class, lotId);
    return count != null ? count : 0L;
  }

  private LiveFixture liveFixture(String sellerEmail, String buyerEmail) throws Exception {
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
    String buyerToken = registerBuyer(buyerEmail);
    return new LiveFixture(lotId, buyerToken);
  }

  private void placeBid(
      String token,
      long lotId,
      String clientRequestId,
      long amountCents,
      org.springframework.test.web.servlet.ResultMatcher expectedStatus)
      throws Exception {
    String amount = "%d.%02d".formatted(amountCents / 100, amountCents % 100);
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

  private String registerSeller(String email) throws Exception {
    registerAndLogin(email, "password1234", "Seller");
    String token = login(email, "password1234");
    mockMvc
        .perform(post("/api/v1/me/seller-application").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    return login(email, "password1234");
  }

  private String registerBuyer(String email) throws Exception {
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(APPLICATION_JSON)
            .content(
                """
                    {"email":"%s","password":"password1234","displayName":"Buyer"}
                    """
                    .formatted(email)));
    return login(email, "password1234");
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
                        {"title":"RL lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private record LiveFixture(long lotId, String buyerToken) {}
}
