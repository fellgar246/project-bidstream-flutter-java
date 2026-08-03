package com.bidstream.api.lot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateLotRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull @Positive Long categoryId,
    @NotBlank String startingPrice,
    @NotBlank String minIncrement,
    String reservePrice) {}
