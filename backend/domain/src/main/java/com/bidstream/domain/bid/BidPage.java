package com.bidstream.domain.bid;

import java.util.List;

public record BidPage(List<Bid> content, int page, int size, long totalElements) {}
