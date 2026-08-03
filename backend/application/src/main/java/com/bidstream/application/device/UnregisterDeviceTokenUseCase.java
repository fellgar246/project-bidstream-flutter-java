package com.bidstream.application.device;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnregisterDeviceTokenUseCase {

  private final DeviceTokenRepository deviceTokenRepository;

  public UnregisterDeviceTokenUseCase(DeviceTokenRepository deviceTokenRepository) {
    this.deviceTokenRepository = deviceTokenRepository;
  }

  @Transactional
  public void execute(long userId, String token) {
    deviceTokenRepository.deleteByUserIdAndToken(userId, token);
  }
}
