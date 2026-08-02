package com.bidstream.api.health;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

  private final String version;

  public HealthController(@Value("${bidstream.version}") String version) {
    this.version = version;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "version", version);
  }
}
