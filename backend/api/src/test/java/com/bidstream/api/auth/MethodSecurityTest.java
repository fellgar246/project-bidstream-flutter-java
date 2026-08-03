package com.bidstream.api.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.application.auth.AuthService;
import com.bidstream.application.lot.CreateLotUseCase;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class MethodSecurityTest {

  @Autowired private CreateLotUseCase createLotUseCase;
  @Autowired private AuthService authService;

  private long sellerId;

  @BeforeEach
  void setUp() {
    User user =
        authService.register(
            "seller-ms-" + System.nanoTime() + "@test.com", "password1234", "Seller");
    authService.applySellerRole(user.id(), "test");
    sellerId = user.id();
  }

  @Test
  @WithMockUser(roles = "BUYER")
  void buyerCannotCreateLot() {
    assertThatThrownBy(() -> createLot(1L)).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @WithMockUser(roles = "SELLER")
  void sellerCanCreateLot() {
    assertThatCode(() -> createLot(sellerId)).doesNotThrowAnyException();
  }

  private void createLot(long ownerId) {
    createLotUseCase.execute(
        ownerId,
        "Vintage watch",
        "A nice watch",
        1L,
        Money.fromString("100.00"),
        Money.fromString("5.00"),
        null);
  }
}
