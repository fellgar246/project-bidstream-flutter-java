package com.bidstream.infrastructure.persistence.bid;

import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidPage;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.money.Money;
import com.bidstream.infrastructure.persistence.lot.LotEntity;
import com.bidstream.infrastructure.persistence.lot.LotJpaRepository;
import com.bidstream.infrastructure.persistence.user.UserEntity;
import com.bidstream.infrastructure.persistence.user.UserJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class BidRepositoryAdapter implements BidRepository {

  private final BidJpaRepository bidJpaRepository;
  private final LotJpaRepository lotJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final Clock clock;

  public BidRepositoryAdapter(
      BidJpaRepository bidJpaRepository,
      LotJpaRepository lotJpaRepository,
      UserJpaRepository userJpaRepository,
      Clock clock) {
    this.bidJpaRepository = bidJpaRepository;
    this.lotJpaRepository = lotJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.clock = clock;
  }

  @Override
  public Bid save(Bid bid) {
    BidEntity entity = new BidEntity();
    LotEntity lot = lotJpaRepository.findById(bid.lotId()).orElseThrow();
    UserEntity bidder = userJpaRepository.findById(bid.bidderId()).orElseThrow();
    entity.setLot(lot);
    entity.setBidder(bidder);
    entity.setAmountCents(bid.amount().cents());
    entity.setPlacedAt(bid.placedAt());
    entity.setClientRequestId(bid.clientRequestId());
    Instant now = clock.instant();
    entity.setCreatedAt(bid.createdAt() != null ? bid.createdAt() : now);
    BidEntity saved = bidJpaRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  public Optional<Bid> findByLotIdAndBidderIdAndClientRequestId(
      long lotId, long bidderId, String clientRequestId) {
    return bidJpaRepository
        .findByLot_IdAndBidder_IdAndClientRequestId(lotId, bidderId, clientRequestId)
        .map(this::toDomain);
  }

  @Override
  public Optional<Bid> findHighestBidByLotId(long lotId) {
    return bidJpaRepository
        .findFirstByLot_IdOrderByAmountCentsDescIdDesc(lotId)
        .map(this::toDomain);
  }

  @Override
  public List<Bid> findByLotIdAfterBidId(long lotId, long afterBidId, int limit) {
    return bidJpaRepository
        .findByLot_IdAndIdGreaterThanOrderByIdAsc(lotId, afterBidId, PageRequest.of(0, limit))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public BidPage findByLotId(long lotId, int page, int size) {
    Page<BidEntity> result =
        bidJpaRepository.findByLot_IdOrderByAmountCentsDescIdDesc(
            lotId, PageRequest.of(page, size));
    return new BidPage(
        result.getContent().stream().map(this::toDomain).toList(),
        page,
        size,
        result.getTotalElements());
  }

  @Override
  public BidPage findByBidderId(long bidderId, int page, int size) {
    Page<BidEntity> result =
        bidJpaRepository.findByBidder_IdOrderByPlacedAtDesc(bidderId, PageRequest.of(page, size));
    return new BidPage(
        result.getContent().stream().map(this::toDomain).toList(),
        page,
        size,
        result.getTotalElements());
  }

  @Override
  public long countByLotId(long lotId) {
    return bidJpaRepository.countByLot_Id(lotId);
  }

  private Bid toDomain(BidEntity entity) {
    return new Bid(
        entity.getId(),
        entity.getLot().getId(),
        entity.getBidder().getId(),
        Money.fromCents(entity.getAmountCents()),
        entity.getPlacedAt(),
        entity.getClientRequestId(),
        entity.getCreatedAt());
  }
}
