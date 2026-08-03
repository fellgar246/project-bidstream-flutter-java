package com.bidstream.api.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.api.support.RabbitMqTestContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
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
class HealthIndicatorIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void ensureRabbitRunning() {
    RabbitMqTestContainer.start();
  }

  @Test
  void ca106_livenessUpWhenRabbitDown() throws Exception {
    RabbitMqTestContainer.stop();

    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());

    MvcResult readiness =
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().is(503)).andReturn();
    JsonNode readinessBody = objectMapper.readTree(readiness.getResponse().getContentAsString());
    assertThat(readinessBody.get("status").asText()).isEqualTo("DOWN");

    RabbitMqTestContainer.start();
  }

  @Test
  void ca107_outboxHealthDownWhenBacklogTooLarge() throws Exception {
    jdbcTemplate.update("DELETE FROM processed_events");
    jdbcTemplate.update("DELETE FROM outbox_events");
    Instant now = Instant.now();
    for (int i = 0; i < 1500; i++) {
      jdbcTemplate.update(
          """
          INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload,
            occurred_at, attempts)
          VALUES ('Lot', 1, 'BidPlaced', '{}', ?, 0)
          """,
          java.sql.Timestamp.from(now));
    }

    MvcResult result =
        mockMvc.perform(get("/actuator/health/outbox")).andExpect(status().is(503)).andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.get("status").asText()).isEqualTo("DOWN");
  }
}
