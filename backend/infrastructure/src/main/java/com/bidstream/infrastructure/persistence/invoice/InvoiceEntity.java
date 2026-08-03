package com.bidstream.infrastructure.persistence.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class InvoiceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lot_id", nullable = false, unique = true)
  private long lotId;

  @Column(name = "buyer_id", nullable = false)
  private long buyerId;

  @Column(name = "seller_id", nullable = false)
  private long sellerId;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(nullable = false)
  private String status;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public long getLotId() {
    return lotId;
  }

  public void setLotId(long lotId) {
    this.lotId = lotId;
  }

  public long getBuyerId() {
    return buyerId;
  }

  public void setBuyerId(long buyerId) {
    this.buyerId = buyerId;
  }

  public long getSellerId() {
    return sellerId;
  }

  public void setSellerId(long sellerId) {
    this.sellerId = sellerId;
  }

  public long getAmountCents() {
    return amountCents;
  }

  public void setAmountCents(long amountCents) {
    this.amountCents = amountCents;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(Instant issuedAt) {
    this.issuedAt = issuedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
