package com.bidstream.api.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class WatchIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ca039_watchIsIdempotent() throws Exception {
    String token = registerSeller("watch@test.com");
    long lotId = createLot(token);

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/watch").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/watch").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM watches WHERE lot_id = ?", Integer.class, lotId);
    assertThat(count).isEqualTo(1);
  }

  private String registerSeller(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Seller"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/me/seller-application").header("Authorization", "Bearer " + login(email)))
        .andExpect(status().isOk());

    return login(email);
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Watch","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }
}
