package com.bidstream.infrastructure.persistence.lot;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotImageJpaRepository extends JpaRepository<LotImageEntity, Long> {

  List<LotImageEntity> findByLot_IdOrderByPositionAsc(long lotId);

  int countByLot_Id(long lotId);

  boolean existsByLot_IdAndStatus(long lotId, String status);

  List<LotImageEntity> findByStatusAndCreatedAtBefore(String status, Instant cutoff);

  @Query("SELECT COALESCE(MAX(e.position), -1) FROM LotImageEntity e WHERE e.lot.id = :lotId")
  int findMaxPositionByLotId(@Param("lotId") long lotId);
}
