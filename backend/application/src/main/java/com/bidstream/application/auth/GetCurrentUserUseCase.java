package com.bidstream.application.auth;

import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserUseCase {

  private final UserRepository userRepository;

  public GetCurrentUserUseCase(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public User execute(long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found"));
  }
}
