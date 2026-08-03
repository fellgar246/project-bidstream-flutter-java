package com.bidstream.infrastructure.messaging;

import com.bidstream.application.tracing.TraceContext;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

@Component
public class TraceIdMessagePostProcessor implements MessagePostProcessor {

  @Override
  public Message postProcessMessage(Message message) {
    String traceId = MDC.get(TraceContext.TRACE_ID_MDC);
    if (traceId != null && !traceId.isBlank()) {
      message.getMessageProperties().setHeader(TraceContext.TRACE_HEADER, traceId);
    }
    String spanId = MDC.get(TraceContext.SPAN_ID_MDC);
    if (spanId != null && !spanId.isBlank()) {
      message.getMessageProperties().setHeader(TraceContext.SPAN_HEADER, spanId);
    }
    return message;
  }
}
