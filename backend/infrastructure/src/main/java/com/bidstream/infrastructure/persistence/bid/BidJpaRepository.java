package com.bidstream.infrastructure.persistence.bid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidJpaRepository extends JpaRepository<BidEntity, Long> {

  java.util.Optional<BidEntity> findByLot_IdAndBidder_IdAndClientRequestId(
      long lotId, long bidderId, String clientRequestId);

  Page<BidEntity> findByLot_IdOrderByAmountCentsDescIdDesc(long lotId, Pageable pageable);

  Page<BidEntity> findByBidder_IdOrderByPlacedAtDesc(long bidderId, Pageable pageable);

  long countByLot_Id(long lotId);
}
