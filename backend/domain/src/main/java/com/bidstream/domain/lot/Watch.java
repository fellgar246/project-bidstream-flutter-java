package com.bidstream.domain.lot;

import java.time.Instant;

public record Watch(long id, long userId, long lotId, Instant createdAt) {}
