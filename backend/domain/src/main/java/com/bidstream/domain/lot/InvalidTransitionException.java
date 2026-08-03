package com.bidstream.domain.lot;

public class InvalidTransitionException extends RuntimeException {

  private final LotStatus from;
  private final LotEvent event;

  public InvalidTransitionException(LotStatus from, LotEvent event) {
    super("Invalid transition from " + from + " on event " + event);
    this.from = from;
    this.event = event;
  }

  public LotStatus from() {
    return from;
  }

  public LotEvent event() {
    return event;
  }
}
