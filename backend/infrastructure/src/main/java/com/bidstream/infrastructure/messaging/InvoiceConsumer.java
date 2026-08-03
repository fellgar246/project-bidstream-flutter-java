package com.bidstream.infrastructure.messaging;

import com.bidstream.application.invoice.Invoice;
import com.bidstream.application.invoice.InvoiceRepository;
import com.bidstream.application.messaging.ProcessedEventPort;
import com.bidstream.application.outbox.OutboxEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class InvoiceConsumer {

  static final String CONSUMER_NAME = "invoicing";

  private final ProcessedEventPort processedEventPort;
  private final InvoiceRepository invoiceRepository;
  private final Clock clock;

  public InvoiceConsumer(
      ProcessedEventPort processedEventPort,
      InvoiceRepository invoiceRepository,
      Clock clock) {
    this.processedEventPort = processedEventPort;
    this.invoiceRepository = invoiceRepository;
    this.clock = clock;
  }

  @RabbitListener(queues = RabbitMqConfig.INVOICING_QUEUE)
  public void handle(IntegrationMessage message) {
    if (!OutboxEvent.LOT_CLOSED_SOLD.equals(message.eventType())) {
      return;
    }
    Instant now = clock.instant();
    if (processedEventPort.tryMarkProcessed(CONSUMER_NAME, message.eventId(), now)) {
      return;
    }
    Map<String, Object> payload = message.payload();
    Invoice invoice =
        new Invoice(
            0L,
            ((Number) payload.get("lotId")).longValue(),
            ((Number) payload.get("winnerId")).longValue(),
            ((Number) payload.get("sellerId")).longValue(),
            ((Number) payload.get("amountCents")).longValue(),
            Invoice.STATUS_ISSUED,
            now,
            now);
    invoiceRepository.save(invoice);
  }
}
