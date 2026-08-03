package com.bidstream.infrastructure.persistence.lot;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchJpaRepository extends JpaRepository<WatchEntity, Long> {

  Optional<WatchEntity> findByUser_IdAndLot_Id(long userId, long lotId);

  void deleteByUser_IdAndLot_Id(long userId, long lotId);

  List<WatchEntity> findByUser_IdOrderByCreatedAtDesc(long userId, Pageable pageable);

  long countByUser_Id(long userId);
}
