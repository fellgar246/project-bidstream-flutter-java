package com.bidstream.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.RedisTestContainer;
import com.bidstream.api.support.StompTestSupport;
import com.bidstream.api.support.TestEmails;
import com.bidstream.application.realtime.LotEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(
    initializers = {PostgresTestContainer.Initializer.class, RedisTestContainer.Initializer.class})
class BidBroadcastIT {

  @LocalServerPort private int port;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca063_twoClientsReceiveBidPlacedWithin300ms() throws Exception {
    long lotId = liveLot();
    String buyer1 = registerBuyer(TestEmails.unique("broadcast-b1"));
    String buyer2 = registerBuyer(TestEmails.unique("broadcast-b2"));

    StompSession session1 = StompTestSupport.connect(port, buyer1);
    StompSession session2 = StompTestSupport.connect(port, buyer2);
    BlockingQueue<LotEventEnvelope> events1 = StompTestSupport.subscribeLotEvents(session1, lotId);
    BlockingQueue<LotEventEnvelope> events2 = StompTestSupport.subscribeLotEvents(session2, lotId);

    placeBid(buyer1, lotId, "100.00", "req-bc-1");

    long start = System.currentTimeMillis();
    LotEventEnvelope e1 = events1.poll(2, TimeUnit.SECONDS);
    LotEventEnvelope e2 = events2.poll(2, TimeUnit.SECONDS);
    long elapsed = System.currentTimeMillis() - start;

    assertThat(elapsed).isLessThan(300L);
    assertThat(e1).isNotNull();
    assertThat(e2).isNotNull();
    assertThat(e1.type()).isEqualTo(LotEventEnvelope.BID_PLACED);
    assertThat(e2.eventId()).isEqualTo(e1.eventId());

    session1.disconnect();
    session2.disconnect();
  }

  @Test
  void ca065_outbidGoesToPreviousBidderOnly() throws Exception {
    long lotId = liveLot();
    String buyer1 = registerBuyer(TestEmails.unique("outbid-b1"));
    String buyer2 = registerBuyer(TestEmails.unique("outbid-b2"));
    String buyer3 = registerBuyer(TestEmails.unique("outbid-b3"));

    placeBid(buyer1, lotId, "100.00", "req-o1");

    StompSession session1 = StompTestSupport.connect(port, buyer1);
    StompSession session2 = StompTestSupport.connect(port, buyer2);
    StompSession session3 = StompTestSupport.connect(port, buyer3);
    BlockingQueue<Map<String, Object>> outbid1 = StompTestSupport.subscribeOutbid(session1);
    BlockingQueue<Map<String, Object>> outbid2 = StompTestSupport.subscribeOutbid(session2);
    StompTestSupport.subscribeLotEvents(session2, lotId);
    StompTestSupport.subscribeLotEvents(session3, lotId);

    placeBid(buyer2, lotId, "105.00", "req-o2");

    assertThat(outbid1.poll(2, TimeUnit.SECONDS)).isNotNull();
    assertThat(outbid2.poll(500, TimeUnit.MILLISECONDS)).isNull();

    session1.disconnect();
    session2.disconnect();
    session3.disconnect();
  }

  @Test
  void ca066_extendedBidEmitsLotExtended() throws Exception {
    long lotId = liveLotEndingSoon();
    String buyer = registerBuyer(TestEmails.unique("extend-b"));
    StompSession session = StompTestSupport.connect(port, buyer);
    BlockingQueue<LotEventEnvelope> events = StompTestSupport.subscribeLotEvents(session, lotId);

    placeBid(buyer, lotId, "100.00", "req-ext");

    LotEventEnvelope bidEvent = events.poll(2, TimeUnit.SECONDS);
    LotEventEnvelope extendEvent = events.poll(2, TimeUnit.SECONDS);

    assertThat(bidEvent.type()).isEqualTo(LotEventEnvelope.BID_PLACED);
    assertThat(extendEvent).isNotNull();
    assertThat(extendEvent.type()).isEqualTo(LotEventEnvelope.LOT_EXTENDED);
    assertThat(extendEvent.payload()).containsKey("scheduledEndAt");

    session.disconnect();
  }

  @Test
  void ca067_concurrentBids_eventIdsStrictlyIncreasing() throws Exception {
    long lotId = liveLot();
    int threads = 100;
    java.util.concurrent.atomic.AtomicInteger nextCents =
        new java.util.concurrent.atomic.AtomicInteger(10000);
    List<String> tokens = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      tokens.add(registerBuyer(TestEmails.unique("order-b" + i)));
    }

    ExecutorService pool = Executors.newFixedThreadPool(20);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Long> eventIds = Collections.synchronizedList(new ArrayList<>());

    StompSession session =
        StompTestSupport.connect(port, registerBuyer(TestEmails.unique("order-watch")));
    BlockingQueue<LotEventEnvelope> events = StompTestSupport.subscribeLotEvents(session, lotId);

    Thread collector =
        new Thread(
            () -> {
              try {
                while (eventIds.size() < threads) {
                  LotEventEnvelope event = events.poll(60, TimeUnit.SECONDS);
                  if (event != null) {
                    eventIds.add(event.eventId());
                  }
                }
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
              }
            });
    collector.start();

    Object bidOrderLock = new Object();
    for (int i = 0; i < threads; i++) {
      int idx = i;
      pool.submit(
          () -> {
            try {
              ready.countDown();
              start.await(10, TimeUnit.SECONDS);
              synchronized (bidOrderLock) {
                int cents = nextCents.getAndAdd(500);
                String amount = "%d.%02d".formatted(cents / 100, cents % 100);
                placeBid(tokens.get(idx), lotId, amount, "req-order-" + idx);
              }
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
          });
    }

    ready.await(30, TimeUnit.SECONDS);
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(120, TimeUnit.SECONDS)).isTrue();
    collector.join(65_000);

    List<Long> bidPlacedIds = eventIds.stream().filter(id -> id % 2 == 0).sorted().toList();
    assertThat(bidPlacedIds).hasSize(threads);
    for (int i = 1; i < bidPlacedIds.size(); i++) {
      assertThat(bidPlacedIds.get(i)).isGreaterThan(bidPlacedIds.get(i - 1));
    }

    session.disconnect();
  }

  private static String moneyFor(int idx) {
    return "%d.00".formatted(100 + idx * 5L);
  }

  private long liveLot() throws Exception {
    String sellerToken = registerSeller(TestEmails.unique("broadcast-seller"));
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

  private long liveLotEndingSoon() throws Exception {
    String sellerToken = registerSeller(TestEmails.unique("extend-seller"));
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
    return lotId;
  }

  private void placeBid(String token, long lotId, String amount, String clientRequestId)
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
        .andExpect(status().isCreated());
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
