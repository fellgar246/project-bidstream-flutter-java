package com.bidstream.infrastructure.persistence.bid;

import com.bidstream.infrastructure.persistence.lot.LotEntity;
import com.bidstream.infrastructure.persistence.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bids")
@Getter
@Setter
public class BidEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lot_id", nullable = false)
  private LotEntity lot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "bidder_id", nullable = false)
  private UserEntity bidder;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(name = "placed_at", nullable = false)
  private Instant placedAt;

  @Column(name = "client_request_id", nullable = false, length = 64)
  private String clientRequestId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
