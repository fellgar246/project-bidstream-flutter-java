package com.bidstream.api.error;

public record ErrorResponse(ErrorBody error, String traceId) {}
