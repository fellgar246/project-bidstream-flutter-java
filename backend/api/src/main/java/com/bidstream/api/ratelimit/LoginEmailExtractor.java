package com.bidstream.api.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class LoginEmailExtractor {

  private LoginEmailExtractor() {}

  static String extractEmail(String body, ObjectMapper objectMapper) {
    if (body == null || body.isBlank()) {
      return "";
    }
    try {
      JsonNode node = objectMapper.readTree(body);
      JsonNode email = node.get("email");
      return email != null && !email.isNull() ? email.asText("") : "";
    } catch (Exception ex) {
      return "";
    }
  }
}
