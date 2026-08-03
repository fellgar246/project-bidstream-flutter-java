package com.bidstream.application.tracing;

public final class TraceContext {

  public static final String TRACE_HEADER = "X-Trace-Id";
  public static final String SPAN_HEADER = "X-Span-Id";
  public static final String TRACE_ID_MDC = "traceId";
  public static final String SPAN_ID_MDC = "spanId";
  public static final String USER_ID_MDC = "userId";
  public static final String LOT_ID_MDC = "lotId";

  private TraceContext() {}
}
