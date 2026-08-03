import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import 'stomp_client.dart';

String wsBaseUrl() {
  const apiBase = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://localhost:8080/api/v1');
  final uri = Uri.parse(apiBase);
  final scheme = uri.scheme == 'https' ? 'wss' : 'ws';
  return '$scheme://${uri.host}:${uri.port}/ws';
}

class BidstreamStompClient {
  BidstreamStompClient({
    required this.readAccessToken,
    required this.onStateChanged,
    required this.onLotEvent,
    required this.onPresence,
    required this.onOutbid,
    required this.fetchMissedEvents,
    required this.refetchLot,
  });

  final Future<String?> Function() readAccessToken;
  final void Function(StompConnectionState state) onStateChanged;
  final void Function(LotEventMessage event) onLotEvent;
  final void Function(int watching) onPresence;
  final void Function(Map<String, dynamic> payload) onOutbid;
  final Future<List<LotEventMessage>> Function(int lotId, int afterEventId) fetchMissedEvents;
  final Future<void> Function(int lotId) refetchLot;

  StompClient? _client;
  int? _activeLotId;
  final LotEventCursor _cursor = LotEventCursor();
  final ReconnectBackoff _backoff = ReconnectBackoff();
  Timer? _reconnectTimer;
  bool _disposed = false;

  Future<void> connectToLot(int lotId) async {
    _activeLotId = lotId;
    _cursor.lastEventId = 0;
    await _openSocket();
  }

  Future<void> _openSocket() async {
    if (_disposed || _activeLotId == null) {
      return;
    }
    final lotId = _activeLotId!;
    final token = await readAccessToken();
    if (token == null || token.isEmpty) {
      onStateChanged(StompConnectionState.disconnected);
      return;
    }

    onStateChanged(StompConnectionState.connecting);
    _client?.deactivate();
    _client = StompClient(
      config: StompConfig(
        url: '${wsBaseUrl()}?token=$token',
        onConnect: (frame) async {
          _backoff.reset();
          onStateChanged(StompConnectionState.connected);
          await refetchLot(lotId);
          final missed = await fetchMissedEvents(lotId, _cursor.lastEventId);
          _cursor.replaceFromHistory(missed);
          for (final event in missed) {
            _dispatch(event);
          }
          _subscribe(lotId);
        },
        onWebSocketError: (_) => _scheduleReconnect(),
        onStompError: (_) => _scheduleReconnect(),
        onDisconnect: (_) {
          onStateChanged(StompConnectionState.disconnected);
          _scheduleReconnect();
        },
        reconnectDelay: Duration.zero,
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
      ),
    );
    _client!.activate();
  }

  void _subscribe(int lotId) {
    final client = _client;
    if (client == null || !client.connected) {
      return;
    }
    client.subscribe(
      destination: '/topic/lots/$lotId',
      callback: (frame) => _handleLotFrame(frame),
    );
    client.subscribe(
      destination: '/topic/lots/$lotId/presence',
      callback: (frame) {
        final body = frame.body;
        if (body == null) {
          return;
        }
        final json = jsonDecode(body) as Map<String, dynamic>;
        final watching = json['watching'];
        if (watching is num) {
          onPresence(watching.toInt());
        }
      },
    );
    client.subscribe(
      destination: '/user/queue/bids',
      callback: (frame) {
        final body = frame.body;
        if (body == null) {
          return;
        }
        onOutbid(Map<String, dynamic>.from(jsonDecode(body) as Map));
      },
    );
    client.send(destination: '/app/lots/$lotId/subscribe', body: '{}');
  }

  void _handleLotFrame(StompFrame frame) {
    final body = frame.body;
    if (body == null) {
      return;
    }
    final event = LotEventMessage.fromJson(Map<String, dynamic>.from(jsonDecode(body) as Map));
    for (final applied in _cursor.ingest(event)) {
      _dispatch(applied);
    }
  }

  void _dispatch(LotEventMessage event) {
    onLotEvent(event);
  }

  void _scheduleReconnect() {
    if (_disposed || _activeLotId == null) {
      return;
    }
    _reconnectTimer?.cancel();
    final delay = _backoff.nextDelay();
    onStateChanged(StompConnectionState.connecting);
    _reconnectTimer = Timer(delay, () {
      unawaited(_openSocket());
    });
  }

  Future<void> disconnect() async {
    _disposed = true;
    _reconnectTimer?.cancel();
    final lotId = _activeLotId;
    if (lotId != null && _client?.connected == true) {
      _client!.send(destination: '/app/lots/$lotId/unsubscribe', body: '{}');
    }
    _client?.deactivate();
    onStateChanged(StompConnectionState.disconnected);
  }
}
