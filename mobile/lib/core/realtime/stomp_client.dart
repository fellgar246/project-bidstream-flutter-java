import 'dart:math';

/// Exponential backoff with jitter for STOMP reconnect (1s → 2s → 4s → … → 30s cap).
class ReconnectBackoff {
  ReconnectBackoff({
    this.initialDelay = const Duration(seconds: 1),
    this.maxDelay = const Duration(seconds: 30),
    this.multiplier = 2,
    Random? random,
  }) : _random = random ?? Random();

  final Duration initialDelay;
  final Duration maxDelay;
  final int multiplier;
  final Random _random;

  int _attempt = 0;

  Duration nextDelay() {
    final exponent = min(_attempt, 10);
    final baseMs =
        initialDelay.inMilliseconds * pow(multiplier, exponent).toInt();
    final cappedMs = min(baseMs, maxDelay.inMilliseconds);
    final jitterMs = _random.nextInt(max(1, cappedMs ~/ 4));
    _attempt++;
    return Duration(milliseconds: cappedMs + jitterMs);
  }

  void reset() {
    _attempt = 0;
  }
}

/// Applies lot events in strict [eventId] order, skipping duplicates.
class LotEventCursor {
  LotEventCursor({this.lastEventId = 0});

  int lastEventId;
  final Map<int, LotEventMessage> _buffer = {};

  List<LotEventMessage> ingest(LotEventMessage event) {
    if (event.eventId <= lastEventId) {
      return const [];
    }
    _buffer[event.eventId] = event;
    final applied = <LotEventMessage>[];
    while (true) {
      final int nextId;
      if (lastEventId == 0) {
        if (_buffer.isEmpty) {
          break;
        }
        nextId = _buffer.keys.reduce((a, b) => a < b ? a : b);
      } else {
        nextId = lastEventId + 1;
      }
      if (!_buffer.containsKey(nextId)) {
        break;
      }
      lastEventId = nextId;
      applied.add(_buffer.remove(nextId)!);
    }
    return applied;
  }

  void replaceFromHistory(Iterable<LotEventMessage> history) {
    for (final event in history) {
      if (event.eventId > lastEventId) {
        lastEventId = event.eventId;
      }
    }
  }
}

class LotEventMessage {
  const LotEventMessage({
    required this.eventId,
    required this.type,
    required this.lotId,
    required this.occurredAt,
    required this.payload,
  });

  final int eventId;
  final String type;
  final int lotId;
  final String occurredAt;
  final Map<String, dynamic> payload;

  factory LotEventMessage.fromJson(Map<String, dynamic> json) {
    return LotEventMessage(
      eventId: json['eventId'] as int,
      type: json['type'] as String,
      lotId: json['lotId'] as int,
      occurredAt: json['occurredAt'] as String,
      payload: Map<String, dynamic>.from(json['payload'] as Map),
    );
  }
}

enum StompConnectionState { connected, connecting, disconnected }
