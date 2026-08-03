package com.bidstream.api.lot;

import com.bidstream.application.realtime.ListLotEventsUseCase;
import com.bidstream.application.realtime.LotEventEnvelope;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lots/{lotId}/events")
public class LotEventsController {

  private final ListLotEventsUseCase listLotEventsUseCase;

  public LotEventsController(ListLotEventsUseCase listLotEventsUseCase) {
    this.listLotEventsUseCase = listLotEventsUseCase;
  }

  @GetMapping
  public List<LotEventEnvelope> listEvents(
      @PathVariable long lotId,
      @RequestParam(defaultValue = "0") long afterEventId,
      @RequestParam(defaultValue = "100") int limit) {
    return listLotEventsUseCase.execute(lotId, afterEventId, limit);
  }
}
