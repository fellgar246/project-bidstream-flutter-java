package com.bidstream.api.support;

import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.resource.jdbc.spi.StatementInspector;

public class TestQueryCounter implements StatementInspector {

  private static final AtomicInteger SELECT_COUNT = new AtomicInteger(0);

  public TestQueryCounter() {}

  public static void reset() {
    SELECT_COUNT.set(0);
  }

  public static int selectCount() {
    return SELECT_COUNT.get();
  }

  @Override
  public String inspect(String sql) {
    if (sql != null && sql.trim().toLowerCase().startsWith("select")) {
      SELECT_COUNT.incrementAndGet();
    }
    return sql;
  }
}
