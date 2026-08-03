package com.bidstream.api.ratelimit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitWebConfig implements WebMvcConfigurer {

  private final BidRateLimitInterceptor bidRateLimitInterceptor;

  public RateLimitWebConfig(BidRateLimitInterceptor bidRateLimitInterceptor) {
    this.bidRateLimitInterceptor = bidRateLimitInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(bidRateLimitInterceptor);
  }
}
