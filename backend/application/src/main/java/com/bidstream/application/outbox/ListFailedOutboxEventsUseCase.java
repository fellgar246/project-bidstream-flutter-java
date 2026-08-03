package com.bidstream.application.outbox;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListFailedOutboxEventsUseCase {

  private final OutboxPort outboxPort;

  public ListFailedOutboxEventsUseCase(OutboxPort outboxPort) {
    this.outboxPort = outboxPort;
  }

  @Transactional(readOnly = true)
  public List<OutboxEvent> execute() {
    return outboxPort.findFailed();
  }
}
