package com.bidstream.domain.user;

import java.util.EnumSet;
import java.util.Set;

public record User(long id, String email, String displayName, Set<Role> roles, boolean enabled) {

  public User {
    roles = roles == null ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
  }

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }
}
