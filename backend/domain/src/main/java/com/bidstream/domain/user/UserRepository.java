package com.bidstream.domain.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository {

  Optional<User> findByEmail(String email);

  Optional<User> findById(long id);

  Optional<String> findPasswordHashByUserId(long userId);

  boolean existsByEmailIgnoreCase(String email);

  User create(String email, String passwordHash, String displayName, Set<Role> roles);

  User updateRoles(long userId, Set<Role> roles);

  List<User> findAll(int page, int size);

  long count();
}
