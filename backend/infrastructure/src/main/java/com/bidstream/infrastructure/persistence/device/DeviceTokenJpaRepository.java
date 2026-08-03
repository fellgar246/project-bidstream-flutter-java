package com.bidstream.infrastructure.persistence.device;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenEntity, Long> {

  Optional<DeviceTokenEntity> findByToken(String token);

  List<DeviceTokenEntity> findByUserId(long userId);

  void deleteByToken(String token);

  void deleteByUserIdAndToken(long userId, String token);
}
