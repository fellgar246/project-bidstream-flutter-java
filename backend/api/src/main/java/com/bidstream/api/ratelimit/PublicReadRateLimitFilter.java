package com.bidstream.api.ratelimit;

import com.bidstream.domain.ratelimit.RateLimitExceededException;
import com.bidstream.infrastructure.ratelimit.DistributedRateLimiter;
import com.bidstream.infrastructure.ratelimit.DistributedRateLimiter.RateLimitDecision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PublicReadRateLimitFilter extends OncePerRequestFilter {

  private final DistributedRateLimiter rateLimiter;

  public PublicReadRateLimitFilter(DistributedRateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!HttpMethod.GET.matches(request.getMethod())) {
      return true;
    }
    String path = request.getRequestURI();
    return !(path.equals("/api/v1/lots")
        || path.matches("/api/v1/lots/\\d+")
        || path.equals("/api/v1/categories"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    RateLimitDecision decision = rateLimiter.tryConsumePublicRead(clientIp(request));
    if (!decision.allowed()) {
      throw new RateLimitExceededException(decision.retryAfter());
    }
    response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
    filterChain.doFilter(request, response);
  }

  static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
