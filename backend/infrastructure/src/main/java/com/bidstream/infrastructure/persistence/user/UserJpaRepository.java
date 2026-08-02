package com.bidstream.infrastructure.persistence.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

  @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE LOWER(u.email) = LOWER(:email)")
  Optional<UserEntity> findByEmailIgnoreCase(@Param("email") String email);

  boolean existsByEmailIgnoreCase(String email);

  @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.id = :id")
  Optional<UserEntity> findByIdWithRoles(@Param("id") long id);
}
