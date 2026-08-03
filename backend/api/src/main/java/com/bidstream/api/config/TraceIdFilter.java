package com.bidstream.api.config;

import com.bidstream.application.tracing.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = request.getHeader(TraceContext.TRACE_HEADER);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    String spanId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put(TraceContext.TRACE_ID_MDC, traceId);
    MDC.put(TraceContext.SPAN_ID_MDC, spanId);
    response.setHeader(TraceContext.TRACE_HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(TraceContext.TRACE_ID_MDC);
      MDC.remove(TraceContext.SPAN_ID_MDC);
      MDC.remove(TraceContext.USER_ID_MDC);
      MDC.remove(TraceContext.LOT_ID_MDC);
    }
  }
}
