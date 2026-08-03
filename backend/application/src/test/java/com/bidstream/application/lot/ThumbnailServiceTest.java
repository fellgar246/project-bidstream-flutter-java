package com.bidstream.application.lot;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ThumbnailServiceTest {

  @Test
  void generateAsync_swallowsFailures() throws IOException {
    ThumbnailService service = spy(new ThumbnailService(null, null, null));
    doThrow(new IOException("boom")).when(service).generate(anyLong());

    service.generateAsync(99L);
  }
}
