package com.bidstream.api.ratelimit;

import com.bidstream.domain.ratelimit.RateLimitExceededException;
import com.bidstream.infrastructure.ratelimit.DistributedRateLimiter;
import com.bidstream.infrastructure.ratelimit.DistributedRateLimiter.RateLimitDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 21)
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private final DistributedRateLimiter rateLimiter;
  private final ObjectMapper objectMapper;

  public LoginRateLimitFilter(DistributedRateLimiter rateLimiter, ObjectMapper objectMapper) {
    this.rateLimiter = rateLimiter;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !(HttpMethod.POST.matches(request.getMethod())
        && "/api/v1/auth/login".equals(request.getRequestURI()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
    String body = new String(wrapped.getCachedBody(), StandardCharsets.UTF_8);
    String email = LoginEmailExtractor.extractEmail(body, objectMapper);
    RateLimitDecision decision =
        rateLimiter.tryConsumeLogin(PublicReadRateLimitFilter.clientIp(request), email);
    if (!decision.allowed()) {
      throw new RateLimitExceededException(decision.retryAfter());
    }
    response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
    filterChain.doFilter(wrapped, response);
  }
}
