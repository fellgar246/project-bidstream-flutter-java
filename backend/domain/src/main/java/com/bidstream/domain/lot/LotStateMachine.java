package com.bidstream.domain.lot;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Explicit transition table for lot lifecycle (SPEC-03 §4.5). */
public final class LotStateMachine {

  private static final Map<LotStatus, Map<LotEvent, LotStatus>> TRANSITIONS = buildTransitions();

  private LotStateMachine() {}

  public static boolean isAllowed(LotStatus from, LotEvent event) {
    Map<LotEvent, LotStatus> fromTransitions = TRANSITIONS.get(from);
    return fromTransitions != null && fromTransitions.containsKey(event);
  }

  public static LotStatus transition(LotStatus from, LotEvent event) {
    if (!isAllowed(from, event)) {
      throw new InvalidTransitionException(from, event);
    }
    return TRANSITIONS.get(from).get(event);
  }

  public static Set<LotEvent> allowedEvents(LotStatus from) {
    Map<LotEvent, LotStatus> fromTransitions = TRANSITIONS.get(from);
    if (fromTransitions == null) {
      return Set.of();
    }
    return EnumSet.copyOf(fromTransitions.keySet());
  }

  private static Map<LotStatus, Map<LotEvent, LotStatus>> buildTransitions() {
    Map<LotStatus, Map<LotEvent, LotStatus>> table = new EnumMap<>(LotStatus.class);

    put(table, LotStatus.DRAFT, LotEvent.SCHEDULE, LotStatus.SCHEDULED);
    put(table, LotStatus.DRAFT, LotEvent.CANCEL, LotStatus.CANCELLED);

    put(table, LotStatus.SCHEDULED, LotEvent.CANCEL, LotStatus.CANCELLED);
    put(table, LotStatus.SCHEDULED, LotEvent.START, LotStatus.LIVE);

    put(table, LotStatus.LIVE, LotEvent.CLOSE_SOLD, LotStatus.CLOSED_SOLD);
    put(table, LotStatus.LIVE, LotEvent.CLOSE_NO_SALE, LotStatus.CLOSED_NO_SALE);

    return Map.copyOf(table);
  }

  private static void put(
      Map<LotStatus, Map<LotEvent, LotStatus>> table,
      LotStatus from,
      LotEvent event,
      LotStatus to) {
    table.computeIfAbsent(from, ignored -> new EnumMap<>(LotEvent.class)).put(event, to);
  }
}
