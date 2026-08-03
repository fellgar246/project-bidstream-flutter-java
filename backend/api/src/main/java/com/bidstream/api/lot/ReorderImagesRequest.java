package com.bidstream.api.lot;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderImagesRequest(@NotEmpty List<Long> order) {}
