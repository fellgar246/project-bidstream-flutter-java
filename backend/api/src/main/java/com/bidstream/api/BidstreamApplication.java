package com.bidstream.api;

import com.bidstream.application.config.ApplicationConfig;
import com.bidstream.infrastructure.config.InfrastructureConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ApplicationConfig.class, InfrastructureConfig.class})
public class BidstreamApplication {

  public static void main(String[] args) {
    SpringApplication.run(BidstreamApplication.class, args);
  }
}
