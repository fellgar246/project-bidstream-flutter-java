package com.bidstream.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.IntegrationTestInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void fullAuthFlow_registerLoginRefreshReuse() throws Exception {
    register("flow@example.com", "password1234", "Flow User");

    assertThatThrownOnDuplicateRegister("Flow@Example.com");

    JsonNode login = login("flow@example.com", "password1234");
    String accessToken = login.get("accessToken").asText();
    String refreshToken = login.get("refreshToken").asText();
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    assertRefreshTokenStoredAsHash(refreshToken);

    JsonNode refreshed = refresh(refreshToken);
    String newRefresh = refreshed.get("refreshToken").asText();
    assertThat(newRefresh).isNotEqualTo(refreshToken);

    assertPreviousTokenRevokedWithReplacement(refreshToken, newRefresh);

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("token_reuse_detected"));

    assertAllRefreshTokensRevoked("flow@example.com");
  }

  @Test
  void sellerApplication_issuesUpdatedTokens() throws Exception {
    register("seller@example.com", "password1234", "Seller User");
    JsonNode login = login("seller@example.com", "password1234");
    String accessToken = login.get("accessToken").asText();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/me/seller-application")
                    .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.roles").isArray())
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.get("user").get("roles").toString()).contains("BUYER").contains("SELLER");
    assertThat(body.get("accessToken").asText()).isNotBlank();
  }

  private void register(String email, String password, String displayName) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"%s","displayName":"%s"}
                    """
                        .formatted(email, password, displayName)))
        .andExpect(status().isCreated());
  }

  private void assertThatThrownOnDuplicateRegister(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Dup"}
                    """
                        .formatted(email)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("conflict"));
  }

  private JsonNode login(String email, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"%s"}
                        """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode refresh(String refreshToken) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private void assertRefreshTokenStoredAsHash(String rawToken) {
    String hash =
        jdbcTemplate.queryForObject("SELECT token_hash FROM refresh_tokens LIMIT 1", String.class);
    assertThat(hash).isNotEqualTo(rawToken);
    assertThat(hash).hasSize(64);
  }

  private void assertPreviousTokenRevokedWithReplacement(String oldRaw, String newRaw)
      throws Exception {
    String oldHash =
        jdbcTemplate.queryForObject(
            "SELECT token_hash FROM refresh_tokens WHERE revoked_at IS NOT NULL LIMIT 1",
            String.class);
    assertThat(oldHash)
        .isEqualTo(com.bidstream.application.auth.AuthService.hashRefreshToken(oldRaw));

    Long replacedById =
        jdbcTemplate.queryForObject(
            "SELECT replaced_by_id FROM refresh_tokens WHERE revoked_at IS NOT NULL LIMIT 1",
            Long.class);
    String newHash =
        jdbcTemplate.queryForObject(
            "SELECT token_hash FROM refresh_tokens WHERE id = ?", String.class, replacedById);
    assertThat(newHash)
        .isEqualTo(com.bidstream.application.auth.AuthService.hashRefreshToken(newRaw));
  }

  private void assertAllRefreshTokensRevoked(String email) {
    Integer active =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM refresh_tokens rt
            JOIN users u ON u.id = rt.user_id
            WHERE u.email = ? AND rt.revoked_at IS NULL
            """,
            Integer.class,
            email);
    assertThat(active).isZero();
  }
}
