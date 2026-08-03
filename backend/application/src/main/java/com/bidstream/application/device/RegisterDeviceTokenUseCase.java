package com.bidstream.application.device;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterDeviceTokenUseCase {

  private final DeviceTokenRepository deviceTokenRepository;

  public RegisterDeviceTokenUseCase(DeviceTokenRepository deviceTokenRepository) {
    this.deviceTokenRepository = deviceTokenRepository;
  }

  @Transactional
  public DeviceToken execute(long userId, String token, String platform) {
    return deviceTokenRepository.upsert(userId, token, platform);
  }
}
