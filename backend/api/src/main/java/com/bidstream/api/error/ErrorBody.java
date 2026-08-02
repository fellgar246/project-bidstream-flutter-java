package com.bidstream.api.error;

import java.util.Map;

public record ErrorBody(String code, String message, Map<String, String> details) {}
