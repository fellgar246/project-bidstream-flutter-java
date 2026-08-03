package com.bidstream.api.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class DeviceTokenIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void registerUpsertsByToken() throws Exception {
    String accessToken = registerAndLogin("device-upsert@test.com");
    registerDevice(accessToken, "fcm-token-abc", "android");
    registerDevice(accessToken, "fcm-token-abc", "ios");

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE token = ?",
                Integer.class,
                "fcm-token-abc"))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT platform FROM device_tokens WHERE token = ?",
                String.class,
                "fcm-token-abc"))
        .isEqualTo("ios");
  }

  @Test
  void unregisterRemovesTokenOnLogout() throws Exception {
    String accessToken = registerAndLogin("device-logout@test.com");
    registerDevice(accessToken, "fcm-token-logout", "android");

    mockMvc
        .perform(
            delete("/api/v1/me/devices/fcm-token-logout")
                .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNoContent());

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE token = ?",
                Integer.class,
                "fcm-token-logout"))
        .isZero();
  }

  private String registerAndLogin(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Device User"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());

    MvcResult login =
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

    JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
    return body.get("accessToken").asText();
  }

  private void registerDevice(String accessToken, String token, String platform) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/me/devices")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"token":"%s","platform":"%s"}
                    """
                        .formatted(token, platform)))
        .andExpect(status().isOk());
  }
}
