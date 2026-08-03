package com.bidstream.infrastructure.push;

import com.bidstream.application.push.PushPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bidstream.fcm.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushAdapter implements PushPort {

  private final List<SentPush> sent = new ArrayList<>();
  private List<String> invalidTokens = List.of();

  public void setInvalidTokens(List<String> tokens) {
    this.invalidTokens = List.copyOf(tokens);
  }

  @Override
  public PushResult send(PushMessage message, List<String> deviceTokens) {
    List<String> invalid = new ArrayList<>();
    for (String token : deviceTokens) {
      sent.add(new SentPush(message, token));
      if (invalidTokens.contains(token)) {
        invalid.add(token);
      }
    }
    return new PushResult(deviceTokens.size() - invalid.size(), invalid);
  }

  public List<SentPush> sentPushes() {
    return List.copyOf(sent);
  }

  public void clear() {
    sent.clear();
    invalidTokens = List.of();
  }

  public record SentPush(PushMessage message, String token) {}
}
