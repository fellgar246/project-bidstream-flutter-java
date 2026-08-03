package com.bidstream.infrastructure.persistence.lot;

import com.bidstream.infrastructure.persistence.category.CategoryEntity;
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
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lots")
@Getter
@Setter
public class LotEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private UserEntity seller;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CategoryEntity category;

  @Column(name = "starting_price_cents", nullable = false)
  private long startingPriceCents;

  @Column(name = "min_increment_cents", nullable = false)
  private long minIncrementCents;

  @Column(name = "reserve_price_cents")
  private Long reservePriceCents;

  @Column(nullable = false)
  private String status;

  @Column(name = "scheduled_start_at")
  private Instant scheduledStartAt;

  @Column(name = "scheduled_end_at")
  private Instant scheduledEndAt;

  @Column(name = "actual_end_at")
  private Instant actualEndAt;

  @Column(name = "current_price_cents", nullable = false)
  private long currentPriceCents;

  @Column(name = "bid_count", nullable = false)
  private int bidCount;

  @Column(name = "winning_bid_id")
  private Long winningBidId;

  @Column(name = "extension_count", nullable = false)
  private int extensionCount;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
