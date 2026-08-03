package com.bidstream.application.device;

import java.time.Instant;

public record DeviceToken(
    long id, long userId, String token, String platform, Instant lastSeenAt, Instant createdAt) {}
