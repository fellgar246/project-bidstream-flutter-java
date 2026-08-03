package com.bidstream.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

  public static final String EVENTS_EXCHANGE = "bidstream.events";
  public static final String DLX_EXCHANGE = "bidstream.dlx";
  public static final String DEAD_LETTER_QUEUE = "bidstream.dead-letter";
  public static final String NOTIFICATIONS_QUEUE = "bidstream.notifications";
  public static final String INVOICING_QUEUE = "bidstream.invoicing";
  public static final String MAIL_QUEUE = "bidstream.mail";
  public static final String PUSH_QUEUE = "bidstream.push";

  @Bean
  TopicExchange eventsExchange() {
    return new TopicExchange(EVENTS_EXCHANGE, true, false);
  }

  @Bean
  DirectExchange deadLetterExchange() {
    return new DirectExchange(DLX_EXCHANGE, true, false);
  }

  @Bean
  Queue deadLetterQueue() {
    return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
  }

  @Bean
  Binding deadLetterBinding() {
    return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_QUEUE);
  }

  @Bean
  Queue notificationsQueue() {
    return durableQueueWithDlq(NOTIFICATIONS_QUEUE);
  }

  @Bean
  Queue invoicingQueue() {
    return durableQueueWithDlq(INVOICING_QUEUE);
  }

  @Bean
  Queue mailQueue() {
    return durableQueueWithDlq(MAIL_QUEUE);
  }

  @Bean
  Queue pushQueue() {
    return durableQueueWithDlq(PUSH_QUEUE);
  }

  private Queue durableQueueWithDlq(String name) {
    return QueueBuilder.durable(name)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
        .build();
  }

  @Bean
  Binding notificationsBidPlacedBinding() {
    return BindingBuilder.bind(notificationsQueue()).to(eventsExchange()).with("bid.placed");
  }

  @Bean
  Binding notificationsLotStartedBinding() {
    return BindingBuilder.bind(notificationsQueue()).to(eventsExchange()).with("lot.started");
  }

  @Bean
  Binding notificationsLotClosedBinding() {
    return BindingBuilder.bind(notificationsQueue()).to(eventsExchange()).with("lot.closed.*");
  }

  @Bean
  Binding invoicingBinding() {
    return BindingBuilder.bind(invoicingQueue()).to(eventsExchange()).with("lot.closed.sold");
  }

  @Bean
  Binding mailBinding() {
    return BindingBuilder.bind(mailQueue()).to(eventsExchange()).with("lot.closed.*");
  }

  @Bean
  Binding pushBidPlacedBinding() {
    return BindingBuilder.bind(pushQueue()).to(eventsExchange()).with("bid.placed");
  }

  @Bean
  Binding pushLotStartedBinding() {
    return BindingBuilder.bind(pushQueue()).to(eventsExchange()).with("lot.started");
  }

  @Bean
  Binding pushLotClosedBinding() {
    return BindingBuilder.bind(pushQueue()).to(eventsExchange()).with("lot.closed.*");
  }

  @Bean
  Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory,
      Jackson2JsonMessageConverter converter,
      TraceIdMessagePostProcessor traceIdMessagePostProcessor) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(converter);
    template.setMandatory(true);
    template.setBeforePublishPostProcessors(traceIdMessagePostProcessor);
    return template;
  }

  @Bean
  SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      Jackson2JsonMessageConverter converter,
      TraceIdReceivePostProcessor traceIdReceivePostProcessor) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(converter);
    factory.setDefaultRequeueRejected(false);
    factory.setAfterReceivePostProcessors(traceIdReceivePostProcessor);
    ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 5.0);
    backOff.setMaxElapsedTime(31_000L);
    factory.setRecoveryBackOff(backOff);
    return factory;
  }
}
