package com.bidstream.application.device;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository {

  DeviceToken upsert(long userId, String token, String platform);

  Optional<DeviceToken> findByToken(String token);

  List<DeviceToken> findByUserId(long userId);

  void deleteByToken(String token);

  void deleteByUserIdAndToken(long userId, String token);
}
