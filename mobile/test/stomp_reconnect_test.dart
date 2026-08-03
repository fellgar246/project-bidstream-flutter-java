import 'dart:math';

import 'package:bidstream/core/realtime/stomp_client.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('ReconnectBackoff', () {
    test('ca0610_backoff_doublesWithCapAndJitter', () {
      final backoff = ReconnectBackoff(random: Random(1));
      expect(backoff.nextDelay().inMilliseconds, greaterThanOrEqualTo(1000));
      expect(backoff.nextDelay().inMilliseconds, lessThanOrEqualTo(3000));
      for (var i = 0; i < 8; i++) {
        backoff.nextDelay();
      }
      expect(backoff.nextDelay().inMilliseconds, lessThanOrEqualTo(37500));
    });
  });

  group('LotEventCursor', () {
    test('ca0610_cursor_appliesInOrderAndSkipsDuplicates', () {
      final cursor = LotEventCursor();
      expect(cursor.ingest(_event(2)).map((e) => e.eventId), [2]);
      expect(cursor.ingest(_event(4)), isEmpty);
      final applied = cursor.ingest(_event(3));
      expect(applied.map((e) => e.eventId), [3, 4]);
      expect(cursor.ingest(_event(3)), isEmpty);
      expect(cursor.ingest(_event(5)).map((e) => e.eventId), [5]);
    });

    test('ca0610_history_restoresCursor', () {
      final cursor = LotEventCursor();
      cursor.replaceFromHistory([_event(10), _event(12)]);
      expect(cursor.lastEventId, 12);
      expect(cursor.ingest(_event(11)), isEmpty);
      expect(cursor.ingest(_event(13)).single.eventId, 13);
    });
  });
}

LotEventMessage _event(int id) {
  return LotEventMessage(
    eventId: id,
    type: 'BID_PLACED',
    lotId: 1,
    occurredAt: '2026-08-01T18:30:00.123Z',
    payload: const {'amount': '100.00'},
  );
}
