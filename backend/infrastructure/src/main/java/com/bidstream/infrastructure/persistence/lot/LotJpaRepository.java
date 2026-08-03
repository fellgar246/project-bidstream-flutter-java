package com.bidstream.infrastructure.persistence.lot;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotJpaRepository
    extends JpaRepository<LotEntity, Long>, JpaSpecificationExecutor<LotEntity> {

  long countBySeller_Id(long sellerId);

  java.util.List<LotEntity> findBySeller_IdOrderByCreatedAtDesc(long sellerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM LotEntity l WHERE l.id = :id")
  java.util.Optional<LotEntity> findByIdForUpdate(@Param("id") long id);

  @Query(
      """
      SELECT l FROM LotEntity l
      WHERE l.status = 'SCHEDULED' AND l.scheduledStartAt <= :now
      ORDER BY l.scheduledStartAt
      """)
  java.util.List<LotEntity> findScheduledReadyToStart(
      @Param("now") java.time.Instant now, org.springframework.data.domain.Pageable pageable);

  @Query(
      """
      SELECT l.id FROM LotEntity l
      WHERE l.status = 'LIVE' AND l.scheduledEndAt <= :now
      ORDER BY l.scheduledEndAt
      """)
  java.util.List<Long> findLiveIdsReadyToClose(
      @Param("now") java.time.Instant now, org.springframework.data.domain.Pageable pageable);
}
