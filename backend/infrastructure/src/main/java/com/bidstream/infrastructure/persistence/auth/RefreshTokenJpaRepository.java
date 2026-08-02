package com.bidstream.infrastructure.persistence.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "UPDATE RefreshTokenEntity t SET t.revokedAt = CURRENT_TIMESTAMP WHERE t.userId = :userId AND t.revokedAt IS NULL")
  void revokeAllActiveForUser(@Param("userId") long userId);
}
