package com.bidstream.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidstream.api.support.PostgresTestContainer;
import com.bidstream.api.support.RedisTestContainer;
import com.bidstream.api.support.StompTestSupport;
import com.bidstream.api.support.TestEmails;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    initializers = {PostgresTestContainer.Initializer.class, RedisTestContainer.Initializer.class})
class WebSocketAuthIT {

  @LocalServerPort private int port;

  @Test
  void ca061_noToken_handshakeRejected() {
    assertThatThrownBy(() -> StompTestSupport.connectWithoutToken(port))
        .isInstanceOf(Exception.class);
  }

  @Test
  void ca061_invalidToken_handshakeRejected() {
    assertThatThrownBy(() -> StompTestSupport.connect(port, "not-a-jwt"))
        .isInstanceOf(Exception.class);
  }

  @Test
  void ca062_validToken_connects() throws Exception {
    String token = registerAndLogin(TestEmails.unique("ws-auth"));
    var session = StompTestSupport.connect(port, token);
    assertThat(session.isConnected()).isTrue();
    session.disconnect();
  }

  private String registerAndLogin(String email) throws Exception {
    var client = HttpClient.newHttpClient();
    client.send(
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/auth/register"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    """
                        {"email":"%s","password":"password1234","displayName":"User"}
                        """
                        .formatted(email)))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> login =
        client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """
                        {"email":"%s","password":"password1234"}
                        """
                            .formatted(email)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    return login.body().substring(login.body().indexOf("\"accessToken\":\"") + 15).split("\"")[0];
  }
}
