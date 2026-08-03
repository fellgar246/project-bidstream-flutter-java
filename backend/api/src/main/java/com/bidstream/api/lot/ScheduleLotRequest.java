package com.bidstream.api.lot;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ScheduleLotRequest(
    @NotNull Instant scheduledStartAt, @NotNull Instant scheduledEndAt) {}
