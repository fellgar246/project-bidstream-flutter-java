package com.bidstream.api.bid;

import com.bidstream.application.bid.ListMyBidsUseCase;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/bids")
public class MyBidsController {

  private final ListMyBidsUseCase listMyBidsUseCase;
  private final BidResponseMapper bidResponseMapper;

  public MyBidsController(
      ListMyBidsUseCase listMyBidsUseCase, BidResponseMapper bidResponseMapper) {
    this.listMyBidsUseCase = listMyBidsUseCase;
    this.bidResponseMapper = bidResponseMapper;
  }

  @GetMapping
  public List<MyBidResponse> listMyBids(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    long userId = (long) authentication.getPrincipal();
    return listMyBidsUseCase.execute(userId, page, size).stream()
        .map(bidResponseMapper::toMyBidResponse)
        .toList();
  }
}
