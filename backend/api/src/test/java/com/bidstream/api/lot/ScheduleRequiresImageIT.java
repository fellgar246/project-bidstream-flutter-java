package com.bidstream.api.lot;

import com.bidstream.api.support.IntegrationTestInitializer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.MinioTestContainer;
import com.bidstream.api.support.PostgresTestContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class ScheduleRequiresImageIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca046_scheduleWithoutReadyImages_returns409() throws Exception {
    String token = registerSeller("seller-no-img@test.com");
    long lotId = createLot(token);
    Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
    Instant end = start.plus(2, ChronoUnit.HOURS);

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/schedule")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"scheduledStartAt":"%s","scheduledEndAt":"%s"}
                    """
                        .formatted(start, end)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("image_required"));
  }

  @Test
  void ca046_scheduleWithReadyImage_succeeds() throws Exception {
    String token = registerSeller("seller-with-img@test.com");
    long lotId = createLot(token);
    insertReadyImage(lotId);
    Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
    Instant end = start.plus(2, ChronoUnit.HOURS);

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/schedule")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"scheduledStartAt":"%s","scheduledEndAt":"%s"}
                    """
                        .formatted(start, end)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SCHEDULED"));
  }

  private void insertReadyImage(long lotId) {
    Instant now = Instant.now();
    jdbcTemplate.update(
        """
        INSERT INTO lot_images (lot_id, storage_key, position, content_type, size_bytes, status,
          created_at, updated_at)
        VALUES (?, ?, 0, 'image/jpeg', 1024, 'READY', ?, ?)
        """,
        lotId,
        "lots/" + lotId + "/ready.jpg",
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(now));
  }

  private String registerSeller(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Seller"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
    String loginToken = login(email);
    mockMvc
        .perform(
            post("/api/v1/me/seller-application").header("Authorization", "Bearer " + loginToken))
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
                        {"title":"Schedule lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }
}
