package com.bidstream.api.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.application.lot.CreateLotUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class MethodSecurityTest {

  @Autowired private CreateLotUseCase createLotUseCase;

  @Test
  @WithMockUser(roles = "BUYER")
  void buyerCannotCreateLot() {
    assertThatThrownBy(() -> createLotUseCase.execute()).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @WithMockUser(roles = "SELLER")
  void sellerCanCreateLot() {
    createLotUseCase.execute();
  }
}
