package com.bidstream.api.bid;

import com.bidstream.application.bid.ListLotBidsUseCase;
import com.bidstream.application.bid.PlaceBidUseCase;
import com.bidstream.domain.bid.BidPage;
import com.bidstream.domain.money.Money;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lots/{lotId}/bids")
public class BidController {

  private final PlaceBidUseCase placeBidUseCase;
  private final ListLotBidsUseCase listLotBidsUseCase;
  private final BidResponseMapper bidResponseMapper;

  public BidController(
      PlaceBidUseCase placeBidUseCase,
      ListLotBidsUseCase listLotBidsUseCase,
      BidResponseMapper bidResponseMapper) {
    this.placeBidUseCase = placeBidUseCase;
    this.listLotBidsUseCase = listLotBidsUseCase;
    this.bidResponseMapper = bidResponseMapper;
  }

  @PostMapping
  public ResponseEntity<PlaceBidResponse> placeBid(
      Authentication authentication,
      @PathVariable long lotId,
      @Valid @RequestBody PlaceBidRequest request) {
    long bidderId = (long) authentication.getPrincipal();
    var outcome =
        placeBidUseCase.execute(
            lotId, bidderId, Money.fromString(request.amount()), request.clientRequestId());
    PlaceBidResponse body = bidResponseMapper.toPlaceBidResponse(outcome.result());
    HttpStatus status = outcome.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status).body(body);
  }

  @GetMapping
  public BidPageResponse listBids(
      @PathVariable long lotId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    BidPage bidPage = listLotBidsUseCase.execute(lotId, page, size);
    return new BidPageResponse(
        bidPage.content().stream().map(bidResponseMapper::toSummary).toList(),
        new BidPageResponse.PageMetadata(
            bidPage.page(), bidPage.size(), bidPage.totalElements(), bidPage.totalPages()));
  }
}
