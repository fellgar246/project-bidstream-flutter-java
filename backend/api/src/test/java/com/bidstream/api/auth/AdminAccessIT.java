package com.bidstream.api.auth;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
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
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class AdminAccessIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void buyerGetsForbiddenEnvelopeOnAdminUsers() throws Exception {
    registerAndLogin("buyer-admin@example.com");
    String accessToken = login("buyer-admin@example.com");

    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("forbidden"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  void protectedEndpointWithoutTokenReturnsUnauthorizedEnvelope() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("unauthorized"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  void adminCanListUsers() throws Exception {
    registerAndLogin("admin-test-buyer@example.com");
    grantAdminRole("admin-test-buyer@example.com");
    String adminToken = login("admin-test-buyer@example.com");

    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users").isArray());
  }

  private void registerAndLogin(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Test User"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
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
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    return body.get("accessToken").asText();
  }

  private void grantAdminRole(String email) {
    Long userId =
        jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'ADMIN')", userId);
  }
}
