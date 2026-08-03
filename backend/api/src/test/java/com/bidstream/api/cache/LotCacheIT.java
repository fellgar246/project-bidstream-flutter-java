package com.bidstream.api.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.api.support.TestQueryCounter;
import com.bidstream.infrastructure.cache.RedisLotCacheAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
@TestPropertySource(
    properties = {
      "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.bidstream.api.support.TestQueryCounter"
    })
class LotCacheIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private StringRedisTemplate redisTemplate;

  @BeforeEach
  void cleanRedis() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    TestQueryCounter.reset();
    jdbcTemplate.update("DELETE FROM bids");
    jdbcTemplate.update("DELETE FROM lots");
  }

  @Test
  void ca081_secondGetLotById_skipsPostgresQueries() throws Exception {
    long lotId = createLiveLot();

    mockMvc.perform(get("/api/v1/lots/" + lotId)).andExpect(status().isOk());
    TestQueryCounter.reset();

    mockMvc.perform(get("/api/v1/lots/" + lotId)).andExpect(status().isOk());
    assertThat(TestQueryCounter.selectCount()).isZero();
  }

  @Test
  void ca082_afterBid_getLotReturnsFreshPrice() throws Exception {
    long lotId = createLiveLot();
    String buyer = registerBuyer("buyer-cache@test.com");

    mockMvc.perform(get("/api/v1/lots/" + lotId)).andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + buyer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":"100.00","clientRequestId":"cache-bid-1"}
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/lots/" + lotId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentPrice").value("100.00"));
  }

  @Test
  void ca083_cacheDoesNotLeakPermissions() throws Exception {
    String sellerToken = registerSeller("seller-perm@test.com");
    long lotId = createDraftLot(sellerToken);
    String otherToken = registerSeller("other-perm@test.com");

    mockMvc
        .perform(get("/api/v1/lots/" + lotId).header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canEdit").value(true));

    mockMvc
        .perform(get("/api/v1/lots/" + lotId).header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canEdit").value(false));
  }

  @Test
  void ca084_cachedPayloadDoesNotContainReservePrice() throws Exception {
    String sellerToken = registerSeller("seller-reserve-cache@test.com");
    long lotId = createLotWithReserve(sellerToken, "500.00");
    setLotLive(lotId);

    mockMvc.perform(get("/api/v1/lots/" + lotId)).andExpect(status().isOk());

    String cached = redisTemplate.opsForValue().get(RedisLotCacheAdapter.LOT_KEY_PREFIX + lotId);
    assertThat(cached).isNotNull();
    assertThat(cached).doesNotContain("reservePrice");
  }

  @Test
  void ca085_catalogVersionInvalidatesListings() throws Exception {
    String sellerToken = registerSeller("seller-list-cache@test.com");

    mockMvc
        .perform(get("/api/v1/lots"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());

    createLiveLot(sellerToken);

    mockMvc
        .perform(get("/api/v1/lots"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  private long createLiveLot() throws Exception {
    return createLiveLot(registerSeller("seller-live-cache@test.com"));
  }

  private long createLiveLot(String sellerToken) throws Exception {
    long lotId = createDraftLot(sellerToken);
    setLotLive(lotId);
    return lotId;
  }

  private long createDraftLot(String sellerToken) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/lots")
                    .header("Authorization", "Bearer " + sellerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Cache lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private long createLotWithReserve(String sellerToken, String reserve) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/lots")
                    .header("Authorization", "Bearer " + sellerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Reserve cache lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00","reservePrice":"%s"}
                        """
                            .formatted(reserve)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private void setLotLive(long lotId) {
    Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
    jdbcTemplate.update(
        """
        UPDATE lots SET status = 'LIVE', scheduled_start_at = ?, scheduled_end_at = ?,
          current_price_cents = 0, bid_count = 0 WHERE id = ?
        """,
        java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
        java.sql.Timestamp.from(end),
        lotId);
  }

  private String registerSeller(String email) throws Exception {
    registerAndLogin(email, "password1234", "Seller");
    String token = login(email, "password1234");
    mockMvc.perform(
        post("/api/v1/me/seller-application").header("Authorization", "Bearer " + token));
    return login(email, "password1234");
  }

  private String registerBuyer(String email) throws Exception {
    return registerAndLogin(email, "password1234", "Buyer");
  }

  private String registerAndLogin(String email, String password, String displayName)
      throws Exception {
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"email":"%s","password":"%s","displayName":"%s"}
                """
                    .formatted(email, password, displayName)));
    return login(email, password);
  }

  private String login(String email, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
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
}
