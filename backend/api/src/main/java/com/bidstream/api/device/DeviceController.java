package com.bidstream.api.device;

import com.bidstream.application.device.DeviceToken;
import com.bidstream.application.device.RegisterDeviceTokenUseCase;
import com.bidstream.application.device.UnregisterDeviceTokenUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/devices")
public class DeviceController {

  private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;
  private final UnregisterDeviceTokenUseCase unregisterDeviceTokenUseCase;

  public DeviceController(
      RegisterDeviceTokenUseCase registerDeviceTokenUseCase,
      UnregisterDeviceTokenUseCase unregisterDeviceTokenUseCase) {
    this.registerDeviceTokenUseCase = registerDeviceTokenUseCase;
    this.unregisterDeviceTokenUseCase = unregisterDeviceTokenUseCase;
  }

  @PostMapping
  public DeviceTokenResponse register(
      Authentication authentication, @Valid @RequestBody RegisterDeviceRequest request) {
    long userId = (long) authentication.getPrincipal();
    DeviceToken token =
        registerDeviceTokenUseCase.execute(userId, request.token(), request.platform());
    return DeviceTokenResponse.from(token);
  }

  @DeleteMapping("/{token}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unregister(Authentication authentication, @PathVariable String token) {
    long userId = (long) authentication.getPrincipal();
    unregisterDeviceTokenUseCase.execute(userId, token);
  }

  public record RegisterDeviceRequest(@NotBlank String token, @NotBlank String platform) {}

  public record DeviceTokenResponse(
      long id, String token, String platform, String lastSeenAt, String createdAt) {

    static DeviceTokenResponse from(DeviceToken token) {
      return new DeviceTokenResponse(
          token.id(),
          token.token(),
          token.platform(),
          token.lastSeenAt().toString(),
          token.createdAt().toString());
    }
  }
}
