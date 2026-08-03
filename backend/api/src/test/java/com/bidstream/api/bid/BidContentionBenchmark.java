package com.bidstream.api.bid;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.IntegrationTestInitializer;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Benchmark de contención (CA-05.17). Ejecutar con {@code RUN_BID_BENCHMARK=true} para imprimir
 * resultados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
@EnabledIfEnvironmentVariable(named = "RUN_BID_BENCHMARK", matches = "true")
class BidContentionBenchmark {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void measureBidThroughput() throws Exception {
    int[] threadCounts = {1, 10, 50, 200};
    System.out.println("| Hilos | Pujas/s | Conflictos (409) | Duración (ms) |");
    System.out.println("|------:|--------:|-----------------:|--------------:|");

    for (int threads : threadCounts) {
      long lotId = createLiveLot("seller-bench-" + threads + "@test.com");
      List<String> tokens = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        tokens.add(registerBuyer("buyer-bench-" + threads + "-" + i + "@test.com"));
      }

      AtomicInteger conflicts = new AtomicInteger();
      ExecutorService pool = Executors.newFixedThreadPool(Math.min(threads, 50));
      CountDownLatch ready = new CountDownLatch(threads);
      CountDownLatch start = new CountDownLatch(1);
      List<Future<?>> futures = new ArrayList<>();

      long begin = System.nanoTime();
      for (int i = 0; i < threads; i++) {
        final int index = i;
        final String token = tokens.get(i);
        futures.add(
            pool.submit(
                () -> {
                  try {
                    ready.countDown();
                    start.await();
                    int status =
                        mockMvc
                            .perform(
                                post("/api/v1/lots/" + lotId + "/bids")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(APPLICATION_JSON)
                                    .content(
                                        """
                                        {"amount":"%s","clientRequestId":"bench-%d-%d"}
                                        """
                                            .formatted((100 + index) + ".00", threads, index)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    if (status == 409) {
                      conflicts.incrementAndGet();
                    }
                  } catch (Exception ex) {
                    throw new RuntimeException(ex);
                  }
                }));
      }
      ready.await();
      start.countDown();
      for (Future<?> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
      long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
      pool.shutdown();

      long accepted =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM bids WHERE lot_id = ?", Long.class, lotId);
      double throughput = accepted * 1000.0 / Math.max(elapsedMs, 1);
      System.out.printf(
          "| %5d | %7.1f | %16d | %13d |%n", threads, throughput, conflicts.get(), elapsedMs);
    }
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
        .andReturn();
    return login(email);
  }

  private String registerSeller(String email) throws Exception {
    registerBuyer(email);
    String token = login(email);
    mockMvc
        .perform(post("/api/v1/me/seller-application").header("Authorization", "Bearer " + token))
        .andReturn();
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
                        {"title":"Bench lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }
}
