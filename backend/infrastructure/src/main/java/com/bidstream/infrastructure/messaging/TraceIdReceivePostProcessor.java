package com.bidstream.infrastructure.messaging;

import com.bidstream.application.tracing.TraceContext;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

@Component
public class TraceIdReceivePostProcessor implements MessagePostProcessor {

  @Override
  public Message postProcessMessage(Message message) throws AmqpException {
    restoreFromMessage(message);
    return message;
  }

  public static void restoreFromMessage(Message message) {
    Object traceHeader = message.getMessageProperties().getHeader(TraceContext.TRACE_HEADER);
    if (traceHeader != null) {
      MDC.put(TraceContext.TRACE_ID_MDC, traceHeader.toString());
    }
    Object spanHeader = message.getMessageProperties().getHeader(TraceContext.SPAN_HEADER);
    if (spanHeader != null) {
      MDC.put(TraceContext.SPAN_ID_MDC, spanHeader.toString());
    }
  }

  public static void clear() {
    MDC.remove(TraceContext.TRACE_ID_MDC);
    MDC.remove(TraceContext.SPAN_ID_MDC);
  }
}
