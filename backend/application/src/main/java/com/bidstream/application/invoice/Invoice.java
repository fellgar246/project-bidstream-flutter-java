package com.bidstream.application.invoice;

import java.time.Instant;

public record Invoice(
    long id,
    long lotId,
    long buyerId,
    long sellerId,
    long amountCents,
    String status,
    Instant issuedAt,
    Instant createdAt) {

  public static final String STATUS_ISSUED = "ISSUED";
}
