package com.bidstream.api.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.api.support.TestEmails;
import com.bidstream.application.outbox.OutboxRelayService;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class OutboxAtomicityIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private OutboxRelayService outboxRelayService;
  @SpyBean private LotRepository lotRepository;

  @Test
  void ca075_rollbackDoesNotLeaveOutboxRow() throws Exception {
    long lotId = liveLot();
    String buyer = registerBuyer(TestEmails.unique("outbox-rb"));

    AtomicInteger saves = new AtomicInteger();
    org.mockito.Mockito.doAnswer(
            inv -> {
              if (saves.incrementAndGet() == 1) {
                throw new RuntimeException("forced rollback");
              }
              return inv.callRealMethod();
            })
        .when(lotRepository)
        .save(org.mockito.ArgumentMatchers.any(Lot.class));

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + buyer)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"100.00","clientRequestId":"req-outbox-rb"}
                    """))
        .andExpect(status().is5xxServerError());

    Integer bids =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bids WHERE lot_id = ?", Integer.class, lotId);
    Integer outbox =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?", Integer.class, lotId);
    assertThat(bids).isZero();
    assertThat(outbox).isZero();
  }

  @Test
  void ca076_eventsPublishAfterRelay() throws Exception {
    long lotId = liveLot();
    String buyer = registerBuyer(TestEmails.unique("outbox-relay"));
    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/bids")
                .header("Authorization", "Bearer " + buyer)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"amount":"100.00","clientRequestId":"req-relay-1"}
                    """))
        .andExpect(status().isCreated());

    outboxRelayService.relayBatch();
    for (int i = 0; i < 5; i++) {
      if (outboxRelayService.relayBatch() == 0) {
        break;
      }
    }

    Integer unpublished =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ? AND published_at IS NULL",
            Integer.class,
            lotId);
    assertThat(unpublished).isZero();
  }

  private long liveLot() throws Exception {
    String seller = registerSeller(TestEmails.unique("outbox-s"));
    long lotId = createLot(seller);
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
                        {"title":"Outbox lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }
}
