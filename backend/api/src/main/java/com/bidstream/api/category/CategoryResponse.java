package com.bidstream.api.category;

import java.util.List;

public record CategoryResponse(
    long id, String slug, String name, List<CategoryResponse> children) {}
