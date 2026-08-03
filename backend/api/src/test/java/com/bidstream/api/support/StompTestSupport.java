package com.bidstream.api.support;

import com.bidstream.application.realtime.LotEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public final class StompTestSupport {

  private StompTestSupport() {}

  public static StompSession connect(int port, String accessToken) throws Exception {
    return connectUrl(port, "/ws?token=" + accessToken);
  }

  public static StompSession connectWithoutToken(int port) throws Exception {
    return connectUrl(port, "/ws");
  }

  private static StompSession connectUrl(int port, String path) throws Exception {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    converter.setObjectMapper(mapper);

    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(converter);
    String url = "ws://localhost:" + port + path;
    return client.connectAsync(url, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
  }

  public static BlockingQueue<LotEventEnvelope> subscribeLotEvents(StompSession session, long lotId)
      throws Exception {
    BlockingQueue<LotEventEnvelope> queue = new LinkedBlockingQueue<>();
    session.subscribe(
        "/topic/lots/" + lotId,
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return LotEventEnvelope.class;
          }

          @Override
          @SuppressWarnings("unchecked")
          public void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((LotEventEnvelope) payload);
          }
        });
    session.send("/app/lots/" + lotId + "/subscribe", Map.of());
    return queue;
  }

  public static BlockingQueue<Map<String, Object>> subscribeOutbid(StompSession session)
      throws Exception {
    BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
    session.subscribe(
        "/user/queue/bids",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return Map.class;
          }

          @Override
          @SuppressWarnings("unchecked")
          public void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((Map<String, Object>) payload);
          }
        });
    return queue;
  }

  public static BlockingQueue<Map<String, Object>> subscribePresence(
      StompSession session, long lotId) throws Exception {
    BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
    session.subscribe(
        "/topic/lots/" + lotId + "/presence",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return Map.class;
          }

          @Override
          @SuppressWarnings("unchecked")
          public void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((Map<String, Object>) payload);
          }
        });
    return queue;
  }
}
