package com.bidstream.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.domain.auth.AlreadySellerException;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

  @Mock private com.bidstream.domain.user.UserRepository userRepository;
  @InjectMocks private ListUsersUseCase useCase;

  @Test
  void execute_returnsPagedUsers() {
    User user = new User(1L, "a@b.com", "A", EnumSet.of(Role.BUYER), true);
    when(userRepository.findAll(0, 20)).thenReturn(java.util.List.of(user));
    when(userRepository.count()).thenReturn(1L);

    ListUsersUseCase.PagedUsers result = useCase.execute(0, 20);

    assertThat(result.users()).containsExactly(user);
    assertThat(result.total()).isEqualTo(1L);
  }
}

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

  @Mock private AuthService authService;
  @InjectMocks private LoginUseCase useCase;

  @Test
  void execute_delegatesToAuthService() {
    User user = new User(1L, "a@b.com", "A", EnumSet.of(Role.BUYER), true);
    AuthTokens tokens = new AuthTokens("a", "r", 900, user);
    when(authService.login("a@b.com", "pass", "agent")).thenReturn(tokens);

    AuthTokens result = useCase.execute("a@b.com", "pass", "agent");

    assertThat(result).isEqualTo(tokens);
  }
}

@ExtendWith(MockitoExtension.class)
class RefreshSessionUseCaseTest {

  @Mock private AuthService authService;
  @InjectMocks private RefreshSessionUseCase useCase;

  @Test
  void execute_delegatesToAuthService() {
    User user = new User(1L, "a@b.com", "A", EnumSet.of(Role.BUYER), true);
    AuthTokens tokens = new AuthTokens("a", "r", 900, user);
    when(authService.refresh("refresh", "agent")).thenReturn(tokens);

    AuthTokens result = useCase.execute("refresh", "agent");

    assertThat(result).isEqualTo(tokens);
  }
}

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

  @Mock private AuthService authService;
  @InjectMocks private LogoutUseCase useCase;

  @Test
  void execute_delegatesToAuthService() {
    useCase.execute("refresh");

    verify(authService).logout("refresh");
  }
}

@ExtendWith(MockitoExtension.class)
class ApplySellerRoleUseCaseTest {

  @Mock private AuthService authService;
  @InjectMocks private ApplySellerRoleUseCase useCase;

  @Test
  void execute_delegatesToAuthService() {
    User user = new User(1L, "a@b.com", "A", EnumSet.of(Role.BUYER, Role.SELLER), true);
    AuthTokens tokens = new AuthTokens("a", "r", 900, user);
    when(authService.applySellerRole(1L, "agent")).thenReturn(tokens);

    useCase.execute(1L, "agent");

    verify(authService).applySellerRole(1L, "agent");
  }
}

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

  @Mock private com.bidstream.domain.user.UserRepository userRepository;
  @InjectMocks private GetCurrentUserUseCase useCase;

  @Test
  void execute_throwsWhenUserMissing() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(99L))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }
}

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

  @Mock private AuthService authService;
  @InjectMocks private RegisterUserUseCase useCase;

  @Test
  void execute_delegatesToAuthService() {
    User user = new User(1L, "a@b.com", "A", EnumSet.of(Role.BUYER), true);
    when(authService.register("a@b.com", "pass", "A")).thenReturn(user);

    useCase.execute("a@b.com", "pass", "A");

    verify(authService).register("a@b.com", "pass", "A");
  }
}

@ExtendWith(MockitoExtension.class)
class AuthServiceApplySellerTest {

  @Mock private com.bidstream.domain.user.UserRepository userRepository;
  @Mock private com.bidstream.domain.auth.RefreshTokenRepository refreshTokenRepository;
  @Mock private com.bidstream.application.auth.port.PasswordEncoderPort passwordEncoder;
  @Mock private com.bidstream.application.auth.port.JwtPort jwtPort;

  @Test
  void applySellerRole_rejectsAlreadySeller() {
    AuthService service =
        new AuthService(
            userRepository,
            refreshTokenRepository,
            passwordEncoder,
            jwtPort,
            java.time.Clock.systemUTC(),
            900,
            7);
    User seller = new User(1L, "a@b.com", "A", EnumSet.of(Role.BUYER, Role.SELLER), true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(seller));

    assertThatThrownBy(() -> service.applySellerRole(1L, "agent"))
        .isInstanceOf(AlreadySellerException.class);
  }
}
