package com.bidstream.infrastructure.push;

import com.bidstream.application.push.PushPort;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "bidstream.fcm.enabled", havingValue = "true")
public class FcmPushAdapter implements PushPort {

  private static final Logger log = LoggerFactory.getLogger(FcmPushAdapter.class);

  private final RestClient restClient;
  private final String serverKey;

  public FcmPushAdapter(
      RestClient.Builder restClientBuilder,
      @Value("${bidstream.fcm.server-key:}") String serverKey) {
    this.restClient = restClientBuilder.baseUrl("https://fcm.googleapis.com").build();
    this.serverKey = serverKey;
  }

  @Override
  public PushResult send(PushMessage message, List<String> deviceTokens) {
    if (deviceTokens.isEmpty()) {
      return new PushResult(0, List.of());
    }
    List<String> invalidTokens = new ArrayList<>();
    int successCount = 0;
    for (String token : deviceTokens) {
      try {
        var response =
            restClient
                .post()
                .uri("/fcm/send")
                .header("Authorization", "key=" + serverKey)
                .body(buildPayload(message, token))
                .retrieve()
                .toEntity(FcmResponse.class);
        FcmResponse body = response.getBody();
        if (body != null && body.failure() > 0) {
          if (body.results() != null) {
            for (int i = 0; i < body.results().size(); i++) {
              FcmResult result = body.results().get(i);
              if (result.error() != null && isInvalidToken(result.error())) {
                invalidTokens.add(token);
              }
            }
          }
        } else {
          successCount++;
        }
      } catch (Exception ex) {
        log.warn("FCM send failed for token {}: {}", token, ex.getMessage());
      }
    }
    return new PushResult(successCount, invalidTokens);
  }

  private boolean isInvalidToken(String error) {
    return "NotRegistered".equals(error)
        || "InvalidRegistration".equals(error)
        || "MismatchSenderId".equals(error);
  }

  private FcmRequest buildPayload(PushMessage message, String token) {
    return new FcmRequest(
        token,
        new FcmNotification(message.title(), message.body()),
        message.data(),
        "high");
  }

  record FcmRequest(
      String to, FcmNotification notification, java.util.Map<String, String> data, String priority) {}

  record FcmNotification(String title, String body) {}

  record FcmResponse(int success, int failure, List<FcmResult> results) {}

  record FcmResult(String error) {}
}
