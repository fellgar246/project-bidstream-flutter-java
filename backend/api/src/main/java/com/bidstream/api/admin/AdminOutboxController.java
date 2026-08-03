package com.bidstream.api.admin;

import com.bidstream.application.outbox.ListFailedOutboxEventsUseCase;
import com.bidstream.application.outbox.OutboxEvent;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/outbox")
public class AdminOutboxController {

  private final ListFailedOutboxEventsUseCase listFailedOutboxEventsUseCase;

  public AdminOutboxController(ListFailedOutboxEventsUseCase listFailedOutboxEventsUseCase) {
    this.listFailedOutboxEventsUseCase = listFailedOutboxEventsUseCase;
  }

  @GetMapping("/failed")
  public List<FailedOutboxResponse> listFailed() {
    return listFailedOutboxEventsUseCase.execute().stream()
        .map(FailedOutboxResponse::from)
        .toList();
  }

  public record FailedOutboxResponse(
      long id,
      String aggregateType,
      long aggregateId,
      String eventType,
      Map<String, Object> payload,
      String occurredAt,
      int attempts,
      String lastError) {

    static FailedOutboxResponse from(OutboxEvent event) {
      return new FailedOutboxResponse(
          event.id(),
          event.aggregateType(),
          event.aggregateId(),
          event.eventType(),
          event.payload(),
          event.occurredAt().toString(),
          event.attempts(),
          event.lastError());
    }
  }
}
