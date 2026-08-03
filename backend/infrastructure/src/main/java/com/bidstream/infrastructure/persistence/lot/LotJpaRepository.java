package com.bidstream.infrastructure.persistence.lot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LotJpaRepository
    extends JpaRepository<LotEntity, Long>, JpaSpecificationExecutor<LotEntity> {

  long countBySeller_Id(long sellerId);

  java.util.List<LotEntity> findBySeller_IdOrderByCreatedAtDesc(long sellerId);
}
