package com.bidstream.api.auth;

import com.bidstream.domain.user.Role;
import java.util.List;
import java.util.stream.Collectors;

public record UserResponse(long id, String email, String displayName, List<String> roles) {

  public static UserResponse from(com.bidstream.domain.user.User user) {
    List<String> roles =
        user.roles().stream().map(Role::name).sorted().collect(Collectors.toList());
    return new UserResponse(user.id(), user.email(), user.displayName(), roles);
  }
}
