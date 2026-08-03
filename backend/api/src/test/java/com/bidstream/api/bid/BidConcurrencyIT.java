package com.bidstream.api.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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
class BidConcurrencyIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca0512_fiftyConcurrentBids_maintainsInvariants() throws Exception {
    long lotId = createLiveLot("seller-conc@test.com");
    List<String> buyerTokens = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      buyerTokens.add(registerBuyer("buyer-conc-" + i + "@test.com"));
    }

    int threads = 50;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<BidAttempt>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      final int index = i;
      final String token = buyerTokens.get(i);
      futures.add(
          pool.submit(
              () -> {
                try {
                  ready.countDown();
                  start.await();
                  String amount = formatAmount(100 + index);
                  int httpStatus =
                      placeBidRaw(token, lotId, amount, "bid-" + index).getResponse().getStatus();
                  return new BidAttempt(amount, httpStatus);
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }));
    }
    ready.await();
    start.countDown();

    List<BidAttempt> attempts = new ArrayList<>();
    for (Future<BidAttempt> future : futures) {
      attempts.add(future.get());
    }
    pool.shutdown();

    long acceptedCount =
        attempts.stream().filter(a -> a.status() == 201 || a.status() == 200).count();
    long bidRows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bids WHERE lot_id = ?", Long.class, lotId);
    long currentPrice =
        jdbcTemplate.queryForObject(
            "SELECT current_price_cents FROM lots WHERE id = ?", Long.class, lotId);
    int bidCount =
        jdbcTemplate.queryForObject(
            "SELECT bid_count FROM lots WHERE id = ?", Integer.class, lotId);

    assertThat(bidRows).isEqualTo(acceptedCount);
    assertThat(bidCount).isEqualTo((int) bidRows);
    assertThat(currentPrice).isEqualTo(maxAcceptedAmountCents(attempts));
  }

  @Test
  void ca0513_sameAmountConcurrently_exactlyOneAccepted() throws Exception {
    long lotId = createLiveLot("seller-same@test.com");
    String tokenA = registerBuyer("buyer-same-a@test.com");
    String tokenB = registerBuyer("buyer-same-b@test.com");

    placeBid(tokenA, lotId, "100.00", "setup-bid", status().isCreated());

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    Future<Integer> futureA =
        pool.submit(
            () -> {
              try {
                ready.countDown();
                start.await();
                return placeBidRaw(tokenA, lotId, "105.00", "same-a").getResponse().getStatus();
              } catch (Exception ex) {
                throw new RuntimeException(ex);
              }
            });
    Future<Integer> futureB =
        pool.submit(
            () -> {
              try {
                ready.countDown();
                start.await();
                return placeBidRaw(tokenB, lotId, "105.00", "same-b").getResponse().getStatus();
              } catch (Exception ex) {
                throw new RuntimeException(ex);
              }
            });
    ready.await();
    start.countDown();

    int statusA = futureA.get();
    int statusB = futureB.get();
    pool.shutdown();

    assertThat(List.of(statusA, statusB)).containsExactlyInAnyOrder(201, 409);
    long accepted =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bids WHERE lot_id = ? AND amount_cents = 10500",
            Long.class,
            lotId);
    assertThat(accepted).isEqualTo(1);
  }

  @Test
  void ca0516_concurrentBidsOnDifferentLots_doNotCrossLock() throws Exception {
    long lot1 = createLiveLot("seller-l1@test.com");
    long lot2 = createLiveLot("seller-l2@test.com");
    List<String> tokens = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      tokens.add(registerBuyer("buyer-iso-" + i + "@test.com"));
    }

    AtomicInteger lockTimeouts = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(20);
    CountDownLatch ready = new CountDownLatch(20);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      final String token = tokens.get(i);
      final int idx = i;
      futures.add(
          pool.submit(
              () -> {
                try {
                  ready.countDown();
                  start.await();
                  int status =
                      placeBidRaw(token, lot1, formatAmount(100 + idx), "l1-" + idx)
                          .getResponse()
                          .getStatus();
                  if (status == 503) {
                    lockTimeouts.incrementAndGet();
                  }
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }));
    }
    for (int i = 10; i < 20; i++) {
      final String token = tokens.get(i);
      final int idx = i;
      futures.add(
          pool.submit(
              () -> {
                try {
                  ready.countDown();
                  start.await();
                  int status =
                      placeBidRaw(token, lot2, formatAmount(100 + (idx - 10)), "l2-" + idx)
                          .getResponse()
                          .getStatus();
                  if (status == 503) {
                    lockTimeouts.incrementAndGet();
                  }
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }));
    }
    ready.await();
    start.countDown();
    for (Future<?> future : futures) {
      future.get();
    }
    pool.shutdown();

    assertThat(lockTimeouts.get()).isZero();
  }

  private long maxAcceptedAmountCents(List<BidAttempt> attempts) {
    return attempts.stream()
        .filter(a -> a.status() == 201 || a.status() == 200)
        .mapToLong(a -> (long) (Double.parseDouble(a.amount()) * 100))
        .max()
        .orElse(0L);
  }

  private String formatAmount(int dollars) {
    return dollars + ".00";
  }

  private MvcResult placeBidRaw(String token, long lotId, String amount, String clientRequestId)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"%s","clientRequestId":"%s"}
                    """
                        .formatted(amount, clientRequestId)))
        .andReturn();
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

  private long createLiveLot(String sellerEmail) throws Exception {
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

  private record BidAttempt(String amount, int status) {}
}
