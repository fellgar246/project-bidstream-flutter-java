package com.bidstream.api.auth;

import com.bidstream.application.auth.GetCurrentUserUseCase;
import com.bidstream.application.auth.LoginUseCase;
import com.bidstream.application.auth.LogoutUseCase;
import com.bidstream.application.auth.RefreshSessionUseCase;
import com.bidstream.application.auth.RegisterUserUseCase;
import com.bidstream.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final RegisterUserUseCase registerUserUseCase;
  private final LoginUseCase loginUseCase;
  private final RefreshSessionUseCase refreshSessionUseCase;
  private final LogoutUseCase logoutUseCase;
  private final GetCurrentUserUseCase getCurrentUserUseCase;

  public AuthController(
      RegisterUserUseCase registerUserUseCase,
      LoginUseCase loginUseCase,
      RefreshSessionUseCase refreshSessionUseCase,
      LogoutUseCase logoutUseCase,
      GetCurrentUserUseCase getCurrentUserUseCase) {
    this.registerUserUseCase = registerUserUseCase;
    this.loginUseCase = loginUseCase;
    this.refreshSessionUseCase = refreshSessionUseCase;
    this.logoutUseCase = logoutUseCase;
    this.getCurrentUserUseCase = getCurrentUserUseCase;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    User user =
        registerUserUseCase.execute(request.email(), request.password(), request.displayName());
    return UserResponse.from(user);
  }

  @PostMapping("/login")
  public AuthResponse login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    return AuthResponse.from(
        loginUseCase.execute(
            request.email(), request.password(), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(
      @Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
    return AuthResponse.from(
        refreshSessionUseCase.execute(request.refreshToken(), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody LogoutRequest request) {
    logoutUseCase.execute(request.refreshToken());
  }

  @GetMapping("/me")
  public UserResponse me(Authentication authentication) {
    long userId = (long) authentication.getPrincipal();
    return UserResponse.from(getCurrentUserUseCase.execute(userId));
  }
}
