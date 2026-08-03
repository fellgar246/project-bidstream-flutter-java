package com.bidstream.api.lot;

import com.bidstream.domain.lot.LotStatus;
import jakarta.validation.constraints.NotNull;

public record ForceStatusRequest(@NotNull LotStatus status) {}
