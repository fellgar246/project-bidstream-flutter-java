package com.bidstream.api.lot;

import com.bidstream.api.support.IntegrationTestInitializer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidstream.api.support.MinioTestContainer;
import com.bidstream.api.support.PostgresTestContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
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
class LotImageControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static MinioClient minioClient;

  @BeforeAll
  static void setUpMinio() {
    minioClient =
        MinioClient.builder()
            .endpoint(MinioTestContainer.endpoint())
            .credentials(MinioTestContainer.ACCESS_KEY, MinioTestContainer.SECRET_KEY)
            .build();
  }

  @Test
  void ca041_fullPresignUploadConfirmFlow() throws Exception {
    String token = registerSeller("seller-img-flow@test.com");
    long lotId = createLot(token);

    JsonNode presign = presign(token, lotId, "photo.jpg", "image/jpeg", tinyJpeg().length);
    long imageId = presign.get("imageId").asLong();
    String uploadUrl = presign.get("uploadUrl").asText();
    String storageKey = presign.get("storageKey").asText();

    uploadToPresignedUrl(uploadUrl, tinyJpeg(), "image/jpeg");

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/images/" + imageId + "/confirm")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.image.status").value("READY"));

    mockMvc
        .perform(get("/api/v1/lots/" + lotId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images[0].id").value(imageId));

    assertThat(objectExists(storageKey)).isTrue();
  }

  @Test
  void ca043_uploadMismatch_returns400AndDeletesObject() throws Exception {
    String token = registerSeller("seller-mismatch@test.com");
    long lotId = createLot(token);

    JsonNode presign = presign(token, lotId, "photo.jpg", "image/jpeg", 1024);
    long imageId = presign.get("imageId").asLong();
    String uploadUrl = presign.get("uploadUrl").asText();
    String storageKey = presign.get("storageKey").asText();

    uploadToPresignedUrl(uploadUrl, largePayload(5000), "image/jpeg");

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/images/" + imageId + "/confirm")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("upload_mismatch"));

    assertThat(objectExists(storageKey)).isFalse();
    assertThat(imageStatus(imageId)).isEqualTo("FAILED");
  }

  @Test
  void ca044_nonOwnerPresign_returns403() throws Exception {
    String ownerToken = registerSeller("owner-img@test.com");
    long lotId = createLot(ownerToken);
    String otherToken = registerSeller("other-img@test.com");

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/images/presign")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"fileName":"photo.jpg","contentType":"image/jpeg","sizeBytes":1024}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("forbidden"));
  }

  @Test
  void ca048_deleteRemovesOriginalAndThumbnail() throws Exception {
    String token = registerSeller("seller-delete-img@test.com");
    long lotId = createLot(token);

    JsonNode presign = presign(token, lotId, "photo.jpg", "image/jpeg", tinyJpeg().length);
    long imageId = presign.get("imageId").asLong();
    String storageKey = presign.get("storageKey").asText();
    uploadToPresignedUrl(presign.get("uploadUrl").asText(), tinyJpeg(), "image/jpeg");

    mockMvc
        .perform(
            post("/api/v1/lots/" + lotId + "/images/" + imageId + "/confirm")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    String thumbnailKey =
        jdbcTemplate.queryForObject(
            "SELECT thumbnail_key FROM lot_images WHERE id = ?", String.class, imageId);

    mockMvc
        .perform(
            delete("/api/v1/lots/" + lotId + "/images/" + imageId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    assertThat(objectExists(storageKey)).isFalse();
    if (thumbnailKey != null) {
      assertThat(objectExists(thumbnailKey)).isFalse();
    }
  }

  private JsonNode presign(
      String token, long lotId, String fileName, String contentType, long sizeBytes)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/lots/" + lotId + "/images/presign")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {"fileName":"%s","contentType":"%s","sizeBytes":%d}
                        """
                            .formatted(fileName, contentType, sizeBytes)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private static void uploadToPresignedUrl(String uploadUrl, byte[] body, String contentType)
      throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(uploadUrl))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
            .header("Content-Type", contentType)
            .build();
    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isBetween(200, 299);
  }

  private boolean objectExists(String storageKey) {
    try {
      minioClient.statObject(
          StatObjectArgs.builder().bucket(MinioTestContainer.BUCKET).object(storageKey).build());
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  private String imageStatus(long imageId) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM lot_images WHERE id = ?", String.class, imageId);
  }

  private static byte[] tinyJpeg() {
    return new byte[] {
      (byte) 0xFF,
      (byte) 0xD8,
      (byte) 0xFF,
      (byte) 0xE0,
      0x00,
      0x10,
      0x4A,
      0x46,
      0x49,
      0x46,
      0x00,
      0x01,
      0x01,
      0x00,
      0x00,
      0x01,
      0x00,
      0x01,
      0x00,
      0x00,
      (byte) 0xFF,
      (byte) 0xD9
    };
  }

  private static byte[] largePayload(int size) {
    byte[] bytes = new byte[size];
    java.util.Arrays.fill(bytes, (byte) 'x');
    return bytes;
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
                        {"title":"Photo lot","description":"Desc","categoryId":1,
                         "startingPrice":"100.00","minIncrement":"5.00"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }
}
