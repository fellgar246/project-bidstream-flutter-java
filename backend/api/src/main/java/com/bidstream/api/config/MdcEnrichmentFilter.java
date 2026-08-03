package com.bidstream.api.config;

import com.bidstream.application.tracing.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class MdcEnrichmentFilter extends OncePerRequestFilter {

  private static final Pattern LOT_BID_PATH = Pattern.compile("/api/v1/lots/(\\d+)/bids/?$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
      MDC.put(TraceContext.USER_ID_MDC, String.valueOf(userId));
    }
    Matcher lotMatcher = LOT_BID_PATH.matcher(request.getRequestURI());
    if (lotMatcher.find()) {
      MDC.put(TraceContext.LOT_ID_MDC, lotMatcher.group(1));
    }
    filterChain.doFilter(request, response);
  }
}
