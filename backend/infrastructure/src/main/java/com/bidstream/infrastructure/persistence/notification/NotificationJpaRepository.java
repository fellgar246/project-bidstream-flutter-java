package com.bidstream.infrastructure.persistence.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

  @Query(
      """
      SELECT n FROM NotificationEntity n
      WHERE n.userId = :userId
        AND (:unreadOnly = false OR n.readAt IS NULL)
      ORDER BY n.createdAt DESC
      """)
  java.util.List<NotificationEntity> findByUser(
      @Param("userId") long userId, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

  long countByUserIdAndReadAtIsNull(long userId);

  java.util.Optional<NotificationEntity> findByIdAndUserId(long id, long userId);

  @Modifying
  @Query("UPDATE NotificationEntity n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
  int markAllRead(@Param("userId") long userId, @Param("readAt") java.time.Instant readAt);
}
