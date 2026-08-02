package com.bidstream.application.auth;

import com.bidstream.domain.user.User;

public record AuthTokens(String accessToken, String refreshToken, long expiresIn, User user) {}
