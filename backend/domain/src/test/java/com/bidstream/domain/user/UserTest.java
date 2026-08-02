package com.bidstream.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void hasRole_returnsTrueWhenRolePresent() {
    User user =
        new User(1L, "a@b.com", "Alice", java.util.EnumSet.of(Role.BUYER, Role.SELLER), true);

    assertThat(user.hasRole(Role.BUYER)).isTrue();
    assertThat(user.hasRole(Role.SELLER)).isTrue();
    assertThat(user.hasRole(Role.ADMIN)).isFalse();
  }

  @Test
  void constructor_copiesRolesDefensively() {
    var roles = java.util.EnumSet.of(Role.BUYER);
    User user = new User(1L, "a@b.com", "Alice", roles, true);
    roles.add(Role.ADMIN);

    assertThat(user.hasRole(Role.ADMIN)).isFalse();
  }
}
