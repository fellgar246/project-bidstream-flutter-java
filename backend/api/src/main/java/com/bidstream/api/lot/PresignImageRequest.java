package com.bidstream.api.lot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignImageRequest(
    @NotBlank String fileName, @NotBlank String contentType, @NotNull @Positive Long sizeBytes) {}
