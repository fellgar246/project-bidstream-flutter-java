package com.bidstream.api.ratelimit;

import com.bidstream.domain.ratelimit.RateLimitExceededException;
import com.bidstream.infrastructure.ratelimit.DistributedRateLimiter;
import com.bidstream.infrastructure.ratelimit.DistributedRateLimiter.RateLimitDecision;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BidRateLimitInterceptor implements HandlerInterceptor {

  private final DistributedRateLimiter rateLimiter;

  public BidRateLimitInterceptor(DistributedRateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!HttpMethod.POST.matches(request.getMethod())) {
      return true;
    }
    if (!request.getRequestURI().matches("/api/v1/lots/\\d+/bids")) {
      return true;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
      return true;
    }
    long userId = (long) authentication.getPrincipal();
    long lotId = Long.parseLong(request.getRequestURI().split("/")[4]);
    RateLimitDecision decision = rateLimiter.tryConsumeBid(userId, lotId);
    if (!decision.allowed()) {
      throw new RateLimitExceededException(decision.retryAfter());
    }
    response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
    return true;
  }
}
