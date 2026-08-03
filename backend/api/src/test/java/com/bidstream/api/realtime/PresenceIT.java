package com.bidstream.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.RedisTestContainer;
import com.bidstream.api.support.StompTestSupport;
import com.bidstream.api.support.TestEmails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = "bidstream.realtime.presence-ttl-seconds=2")
@ContextConfiguration(
    initializers = {PostgresTestContainer.Initializer.class, RedisTestContainer.Initializer.class})
class PresenceIT {

  @LocalServerPort private int port;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca068_presenceIncreasesOnSubscribe() throws Exception {
    long lotId = liveLot();
    String viewer = registerBuyer(TestEmails.unique("presence-v"));
    StompSession session = StompTestSupport.connect(port, viewer);
    BlockingQueue<Map<String, Object>> presence =
        StompTestSupport.subscribePresence(session, lotId);

    session.send("/app/lots/" + lotId + "/subscribe", Map.of());

    Map<String, Object> update = presence.poll(3, TimeUnit.SECONDS);
    assertThat(update).isNotNull();
    assertThat(((Number) update.get("watching")).intValue()).isGreaterThanOrEqualTo(1);

    session.disconnect();
  }

  @Test
  void ca068_clientDropsWithoutUnsubscribe_expiresWithinTtl() throws Exception {
    long lotId = liveLot();
    String viewer = registerBuyer(TestEmails.unique("presence-drop"));
    StompSession session = StompTestSupport.connect(port, viewer);
    BlockingQueue<Map<String, Object>> presence =
        StompTestSupport.subscribePresence(session, lotId);
    session.send("/app/lots/" + lotId + "/subscribe", Map.of());
    presence.poll(3, TimeUnit.SECONDS);

    session.disconnect();

    Thread.sleep(2500);
    String watcher = registerBuyer(TestEmails.unique("presence-watcher"));
    StompSession watcherSession = StompTestSupport.connect(port, watcher);
    BlockingQueue<Map<String, Object>> afterDrop =
        StompTestSupport.subscribePresence(watcherSession, lotId);
    watcherSession.send("/app/lots/" + lotId + "/subscribe", Map.of());
    Map<String, Object> update = afterDrop.poll(3, TimeUnit.SECONDS);
    assertThat(((Number) update.get("watching")).intValue()).isEqualTo(1);
    watcherSession.disconnect();
  }

  @Test
  void ca069_presenceRateLimited() throws Exception {
    long lotId = liveLot();
    BlockingQueue<Map<String, Object>> presence = new java.util.concurrent.LinkedBlockingQueue<>();
    for (int i = 0; i < 5; i++) {
      String token = registerBuyer(TestEmails.unique("presence-burst" + i));
      StompSession session = StompTestSupport.connect(port, token);
      if (i == 0) {
        presence = StompTestSupport.subscribePresence(session, lotId);
      }
      session.send("/app/lots/" + lotId + "/subscribe", Map.of());
    }

    int received = 0;
    long deadline = System.currentTimeMillis() + 1500;
    while (System.currentTimeMillis() < deadline) {
      Map<String, Object> msg = presence.poll(200, TimeUnit.MILLISECONDS);
      if (msg != null) {
        received++;
      }
    }
    assertThat(received).isLessThanOrEqualTo(2);
  }

  private long liveLot() throws Exception {
    String sellerToken = registerSeller(TestEmails.unique("presence-s"));
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
