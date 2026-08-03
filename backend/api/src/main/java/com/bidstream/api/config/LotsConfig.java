package com.bidstream.api.config;

import com.bidstream.application.lot.LotsProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LotsConfig {

  @Bean
  LotsProperties lotsProperties(
      @Value("${bidstream.lots.require-images-for-schedule:false}") boolean requireImages) {
    LotsProperties properties = new LotsProperties();
    properties.setRequireImagesForSchedule(requireImages);
    return properties;
  }
}
