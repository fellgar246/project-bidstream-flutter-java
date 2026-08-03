package com.bidstream.infrastructure.messaging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TracingRabbitListenerAdvice {

  @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      return joinPoint.proceed();
    } finally {
      TraceIdReceivePostProcessor.clear();
    }
  }
}
