package com.bidstream.infrastructure.persistence.user;

import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepository {

  private final UserJpaRepository jpaRepository;
  private final Clock clock;

  public UserRepositoryAdapter(UserJpaRepository jpaRepository, Clock clock) {
    this.jpaRepository = jpaRepository;
    this.clock = clock;
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return jpaRepository.findByEmailIgnoreCase(email).map(this::toDomain);
  }

  @Override
  public Optional<User> findById(long id) {
    return jpaRepository.findByIdWithRoles(id).map(this::toDomain);
  }

  @Override
  public Optional<String> findPasswordHashByUserId(long userId) {
    return jpaRepository.findById(userId).map(UserEntity::getPasswordHash);
  }

  @Override
  public boolean existsByEmailIgnoreCase(String email) {
    return jpaRepository.existsByEmailIgnoreCase(email);
  }

  @Override
  public User create(String email, String passwordHash, String displayName, Set<Role> roles) {
    Instant now = clock.instant();
    UserEntity entity = new UserEntity();
    entity.setEmail(email);
    entity.setPasswordHash(passwordHash);
    entity.setDisplayName(displayName);
    entity.setEnabled(true);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    for (Role role : roles) {
      UserRoleEntity roleEntity = new UserRoleEntity();
      roleEntity.setUser(entity);
      roleEntity.setRole(role);
      entity.getRoles().add(roleEntity);
    }

    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public User updateRoles(long userId, Set<Role> roles) {
    UserEntity entity =
        jpaRepository
            .findByIdWithRoles(userId)
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));

    entity.getRoles().clear();
    jpaRepository.saveAndFlush(entity);

    for (Role role : roles) {
      UserRoleEntity roleEntity = new UserRoleEntity();
      roleEntity.setUser(entity);
      roleEntity.setRole(role);
      entity.getRoles().add(roleEntity);
    }
    entity.setUpdatedAt(clock.instant());

    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public List<User> findAll(int page, int size) {
    return jpaRepository.findAll(PageRequest.of(page, size)).stream()
        .map(this::toDomainWithoutRolesFetch)
        .toList();
  }

  @Override
  public long count() {
    return jpaRepository.count();
  }

  private User toDomain(UserEntity entity) {
    Set<Role> roles =
        entity.getRoles().stream()
            .map(UserRoleEntity::getRole)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
    return new User(
        entity.getId(), entity.getEmail(), entity.getDisplayName(), roles, entity.isEnabled());
  }

  private User toDomainWithoutRolesFetch(UserEntity entity) {
    return jpaRepository.findByIdWithRoles(entity.getId()).map(this::toDomain).orElseThrow();
  }
}
