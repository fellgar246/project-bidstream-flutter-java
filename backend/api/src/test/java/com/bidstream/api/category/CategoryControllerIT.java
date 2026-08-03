package com.bidstream.api.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.infrastructure.seed.CategorySeeder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class CategoryControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private CategorySeeder categorySeeder;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void seedCategories() {
    categorySeeder.seed();
  }

  @Test
  void listCategories_returnsTreeOrderedByName() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk()).andReturn();

    JsonNode roots = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(roots.size()).isGreaterThanOrEqualTo(4);

    for (int i = 1; i < roots.size(); i++) {
      String previousName = roots.get(i - 1).get("name").asText();
      long previousId = roots.get(i - 1).get("id").asLong();
      String currentName = roots.get(i).get("name").asText();
      long currentId = roots.get(i).get("id").asLong();

      int nameCompare = previousName.compareTo(currentName);
      assertThat(nameCompare <= 0).isTrue();
      if (nameCompare == 0) {
        assertThat(previousId).isLessThan(currentId);
      }
    }

    for (JsonNode root : roots) {
      JsonNode children = root.get("children");
      assertThat(children.isArray()).isTrue();
      assertThat(children.size()).isGreaterThanOrEqualTo(1);
    }
  }

  @Test
  void unknownRoute_returnsNotFoundEnvelope() throws Exception {
    registerUser("notfound@example.com");
    String token = login("notfound@example.com");

    mockMvc
        .perform(get("/api/v1/does-not-exist").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("not_found"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  private void registerUser(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"password1234","displayName":"Test"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
}
