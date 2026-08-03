package com.bidstream.application.realtime;

/** Ephemeral viewer presence for live lots (backed by Redis with TTL). */
public interface LotPresencePort {

  void markPresent(long lotId, long userId);

  void markAbsent(long lotId, long userId);

  void refresh(long lotId, long userId);

  int countWatching(long lotId);
}
