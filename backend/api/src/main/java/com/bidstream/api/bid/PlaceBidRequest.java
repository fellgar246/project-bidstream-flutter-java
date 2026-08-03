package com.bidstream.api.bid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlaceBidRequest(
    @NotBlank @Pattern(regexp = "\\d+(\\.\\d{1,2})?") String amount,
    @NotBlank String clientRequestId) {}
