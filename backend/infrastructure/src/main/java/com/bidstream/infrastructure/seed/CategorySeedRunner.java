package com.bidstream.infrastructure.seed;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CategorySeedRunner {

  private final CategorySeeder categorySeeder;

  public CategorySeedRunner(CategorySeeder categorySeeder) {
    this.categorySeeder = categorySeeder;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    categorySeeder.seed();
  }
}
