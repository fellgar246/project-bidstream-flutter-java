# Topología RabbitMQ — BidStream

```mermaid
flowchart LR
  subgraph producers
    Relay[Outbox relay]
  end

  Relay -->|publish| EX[(bidstream.events<br/>topic, durable)]

  EX -->|bid.placed| NQ[bidstream.notifications]
  EX -->|lot.started| NQ
  EX -->|lot.closed.*| NQ

  EX -->|lot.closed.sold| IQ[bidstream.invoicing]

  EX -->|lot.closed.*| MQ[bidstream.mail]

  NQ -->|3 retries| DLX[(bidstream.dlx)]
  IQ --> DLX
  MQ --> DLX
  DLX --> DLQ[bidstream.dead-letter]

  NQ --> NC[NotificationConsumer]
  IQ --> IC[InvoiceConsumer]
  MQ --> MC[MailConsumer]
```

| Cola | Routing keys | Consumidor |
|------|----------------|------------|
| `bidstream.notifications` | `bid.placed`, `lot.started`, `lot.closed.*` | Inserta en `notifications` + STOMP `/user/queue/notifications` |
| `bidstream.invoicing` | `lot.closed.sold` | Crea `invoices` (unique por `lot_id`) |
| `bidstream.mail` | `lot.closed.*` | Correo vía MailHog al ganador y vendedor |

Mensajes persistentes; publisher confirms activados en el relay.
