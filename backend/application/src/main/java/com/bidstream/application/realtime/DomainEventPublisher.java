package com.bidstream.application.realtime;

/** Port for publishing lot realtime events without coupling to WebSocket/STOMP. */
public interface DomainEventPublisher {

  void publish(LotRealtimeEvent event);
}
