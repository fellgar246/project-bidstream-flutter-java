package com.bidstream.api.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.application.outbox.OutboxRelayService;
import com.bidstream.application.tracing.TraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
@TestPropertySource(properties = "bidstream.scheduler.enabled=true")
class TraceIdPropagationIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private OutboxRelayService outboxRelayService;

  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachLogAppender() {
    Logger bidLogger = (Logger) LoggerFactory.getLogger("com.bidstream.api.bid.BidController");
    Logger notificationLogger =
        (Logger)
            LoggerFactory.getLogger("com.bidstream.infrastructure.messaging.NotificationConsumer");
    appender = new ListAppender<>();
    appender.start();
    bidLogger.addAppender(appender);
    notificationLogger.addAppender(appender);
  }

  @AfterEach
  void detachLogAppender() {
    Logger bidLogger = (Logger) LoggerFactory.getLogger("com.bidstream.api.bid.BidController");
    Logger notificationLogger =
        (Logger)
            LoggerFactory.getLogger("com.bidstream.infrastructure.messaging.NotificationConsumer");
    bidLogger.detachAppender(appender);
    notificationLogger.detachAppender(appender);
  }

  @Test
  void ca104_traceIdPropagatesThroughOutboxAndRabbit() throws Exception {
    String traceId = UUID.randomUUID().toString();
    var fixture = liveLot();
    String buyer1 = registerBuyer("trace-buyer1@test.com");
    String buyer2 = registerBuyer("trace-buyer2@test.com");

    placeBid(buyer1, fixture.lotId(), "100.00", "trace-req-1");

    mockMvc
        .perform(
            post("/api/v1/lots/" + fixture.lotId() + "/bids")
                .header("Authorization", "Bearer " + buyer2)
                .header(TraceContext.TRACE_HEADER, traceId)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"105.00","clientRequestId":"trace-req-2"}
                    """))
        .andExpect(status().isCreated());

    for (int i = 0; i < 5; i++) {
      outboxRelayService.relayBatch();
    }

    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () -> {
              boolean bidLog =
                  appender.list.stream()
                      .anyMatch(
                          event ->
                              hasTraceId(event, traceId)
                                  && event.getLoggerName().contains("BidController"));
              boolean consumerLog =
                  appender.list.stream()
                      .anyMatch(
                          event ->
                              hasTraceId(event, traceId)
                                  && event.getLoggerName().contains("NotificationConsumer"));
              assertThat(bidLog).isTrue();
              assertThat(consumerLog).isTrue();
            });
  }

  private static boolean hasTraceId(ILoggingEvent event, String traceId) {
    return traceId.equals(event.getMDCPropertyMap().get("traceId"))
        || event.getFormattedMessage().contains(traceId);
  }

  private record LiveLotFixture(long lotId) {}

  private LiveLotFixture liveLot() throws Exception {
    String sellerToken = registerSeller("trace-seller-setup@test.com");
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
    String uniqueEmail = email.replace("@", "+" + UUID.randomUUID() + "@");
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
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
                .contentType(APPLICATION_JSON)
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
                        {"title":"Trace lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }
}
