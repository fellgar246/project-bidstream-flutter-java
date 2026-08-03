package com.bidstream.api.auth;

import com.bidstream.application.auth.ListUsersUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final ListUsersUseCase listUsersUseCase;

  public AdminController(ListUsersUseCase listUsersUseCase) {
    this.listUsersUseCase = listUsersUseCase;
  }

  @GetMapping("/users")
  public PagedUsersResponse listUsers(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ListUsersUseCase.PagedUsers result = listUsersUseCase.execute(page, size);
    List<UserResponse> users = result.users().stream().map(UserResponse::from).toList();
    return new PagedUsersResponse(users, result.page(), result.size(), result.total());
  }

  public record PagedUsersResponse(List<UserResponse> users, int page, int size, long total) {}
}
