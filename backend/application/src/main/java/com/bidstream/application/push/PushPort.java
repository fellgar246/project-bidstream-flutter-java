package com.bidstream.application.push;

import java.util.List;
import java.util.Map;

public interface PushPort {

  PushResult send(PushMessage message, List<String> deviceTokens);

  record PushMessage(String title, String body, Map<String, String> data) {}

  record PushResult(int successCount, List<String> invalidTokens) {}
}
