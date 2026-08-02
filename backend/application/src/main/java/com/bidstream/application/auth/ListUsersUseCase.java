package com.bidstream.application.auth;

import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersUseCase {

  private final UserRepository userRepository;

  public ListUsersUseCase(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional(readOnly = true)
  public PagedUsers execute(int page, int size) {
    List<User> users = userRepository.findAll(page, size);
    long total = userRepository.count();
    return new PagedUsers(users, page, size, total);
  }

  public record PagedUsers(List<User> users, int page, int size, long total) {}
}
