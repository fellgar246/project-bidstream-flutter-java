package com.bidstream.api.ratelimit;

import com.bidstream.api.error.ErrorBody;
import com.bidstream.api.error.ErrorResponse;
import com.bidstream.application.tracing.TraceContext;
import com.bidstream.domain.ratelimit.RateLimitExceededException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitExceptionHandlingFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;

  public RateLimitExceptionHandlingFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (RateLimitExceededException ex) {
      writeRateLimited(response, ex);
    }
  }

  static void writeRateLimited(HttpServletResponse response, RateLimitExceededException ex)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }
    long retrySeconds = Math.max(1, ex.retryAfter().getSeconds());
    response.setStatus(429);
    response.setHeader("Retry-After", String.valueOf(retrySeconds));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    String traceId = MDC.get(TraceContext.TRACE_ID_MDC);
    if (traceId == null || traceId.isBlank()) {
      traceId = java.util.UUID.randomUUID().toString();
    }
    ErrorResponse body =
        new ErrorResponse(new ErrorBody("rate_limited", "Rate limit exceeded", Map.of()), traceId);
    response.getWriter().write(new ObjectMapper().writeValueAsString(body));
  }
}
