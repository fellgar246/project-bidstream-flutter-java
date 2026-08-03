package com.bidstream.infrastructure.seed;

import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import com.bidstream.infrastructure.persistence.bid.BidEntity;
import com.bidstream.infrastructure.persistence.bid.BidJpaRepository;
import com.bidstream.infrastructure.persistence.category.CategoryEntity;
import com.bidstream.infrastructure.persistence.category.CategoryJpaRepository;
import com.bidstream.infrastructure.persistence.lot.LotEntity;
import com.bidstream.infrastructure.persistence.lot.LotJpaRepository;
import com.bidstream.infrastructure.persistence.user.UserEntity;
import com.bidstream.infrastructure.persistence.user.UserJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoSeeder {

  public static final String DEMO_BUYER_EMAIL = "demo-buyer@bidstream.demo";
  public static final String DEMO_SELLER_EMAIL = "demo-seller@bidstream.demo";
  public static final String DEMO_PASSWORD = "demo1234";
  public static final String LOT_TITLE_PREFIX = "[Demo] ";

  private static final int TARGET_LOTS = 40;
  private static final int TARGET_BIDS = 300;

  private final UserRepository userRepository;
  private final UserJpaRepository userJpaRepository;
  private final CategoryJpaRepository categoryRepository;
  private final LotJpaRepository lotRepository;
  private final BidJpaRepository bidRepository;
  private final PasswordEncoder passwordEncoder;
  private final CategorySeeder categorySeeder;

  public DemoSeeder(
      UserRepository userRepository,
      UserJpaRepository userJpaRepository,
      CategoryJpaRepository categoryRepository,
      LotJpaRepository lotRepository,
      BidJpaRepository bidRepository,
      PasswordEncoder passwordEncoder,
      CategorySeeder categorySeeder) {
    this.userRepository = userRepository;
    this.userJpaRepository = userJpaRepository;
    this.categoryRepository = categoryRepository;
    this.lotRepository = lotRepository;
    this.bidRepository = bidRepository;
    this.passwordEncoder = passwordEncoder;
    this.categorySeeder = categorySeeder;
  }

  @Transactional
  public DemoSeedResult seed() {
    categorySeeder.seed();
    User buyer = ensureDemoBuyer();
    User seller = ensureDemoSeller();
    List<UserEntity> extraBuyers = ensureExtraBuyers(8);
    List<LotEntity> lots = ensureDemoLots(seller.id());
    int bidsCreated = ensureDemoBids(lots, buyer, extraBuyers);
    return new DemoSeedResult(buyer.id(), seller.id(), lots.size(), bidsCreated);
  }

  private User ensureDemoBuyer() {
    return userRepository
        .findByEmail(DEMO_BUYER_EMAIL)
        .orElseGet(
            () ->
                userRepository.create(
                    DEMO_BUYER_EMAIL,
                    passwordEncoder.encode(DEMO_PASSWORD),
                    "Demo Buyer",
                    EnumSet.of(Role.BUYER)));
  }

  private User ensureDemoSeller() {
    return userRepository
        .findByEmail(DEMO_SELLER_EMAIL)
        .orElseGet(
            () -> {
              User created =
                  userRepository.create(
                      DEMO_SELLER_EMAIL,
                      passwordEncoder.encode(DEMO_PASSWORD),
                      "Demo Seller",
                      EnumSet.of(Role.BUYER));
              return userRepository.updateRoles(created.id(), EnumSet.of(Role.BUYER, Role.SELLER));
            });
  }

  private List<UserEntity> ensureExtraBuyers(int count) {
    List<UserEntity> buyers = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      final int bidderNum = i;
      String email = "demo-bidder-" + bidderNum + "@bidstream.demo";
      UserEntity entity =
          userJpaRepository
              .findByEmailIgnoreCase(email)
              .orElseGet(
                  () -> {
                    User created =
                        userRepository.create(
                            email,
                            passwordEncoder.encode(DEMO_PASSWORD),
                            "Demo Bidder " + bidderNum,
                            EnumSet.of(Role.BUYER));
                    return userJpaRepository.findById(created.id()).orElseThrow();
                  });
      buyers.add(entity);
    }
    return buyers;
  }

  private List<LotEntity> ensureDemoLots(long sellerId) {
    UserEntity seller = userJpaRepository.findById(sellerId).orElseThrow();
    List<CategoryEntity> categories = categoryRepository.findAll();
    if (categories.isEmpty()) {
      throw new IllegalStateException("Categories must be seeded before demo lots");
    }

    long existing =
        lotRepository.findBySeller_IdOrderByCreatedAtDesc(sellerId).stream()
            .filter(lot -> lot.getTitle().startsWith(LOT_TITLE_PREFIX))
            .count();
    if (existing >= TARGET_LOTS) {
      return lotRepository.findBySeller_IdOrderByCreatedAtDesc(sellerId).stream()
          .filter(lot -> lot.getTitle().startsWith(LOT_TITLE_PREFIX))
          .limit(TARGET_LOTS)
          .toList();
    }

    Instant now = Instant.now();
    List<LotEntity> created = new ArrayList<>();
    for (int i = (int) existing; i < TARGET_LOTS; i++) {
      CategoryEntity category = categories.get(i % categories.size());
      LotEntity lot = new LotEntity();
      lot.setSeller(seller);
      lot.setTitle(LOT_TITLE_PREFIX + "Lot " + (i + 1));
      lot.setDescription("Demo lot for portfolio showcase #" + (i + 1));
      lot.setCategory(category);
      lot.setStartingPriceCents(10_000);
      lot.setMinIncrementCents(500);
      lot.setCurrentPriceCents(0);
      lot.setBidCount(0);
      lot.setExtensionCount(0);
      lot.setVersion(0);
      lot.setCreatedAt(now);
      lot.setUpdatedAt(now);

      if (i < 5) {
        lot.setStatus("LIVE");
        lot.setScheduledStartAt(now.minus(1, ChronoUnit.HOURS));
        lot.setScheduledEndAt(now.plus(15 + i, ChronoUnit.MINUTES));
      } else if (i < 15) {
        lot.setStatus("SCHEDULED");
        lot.setScheduledStartAt(now.plus(i, ChronoUnit.HOURS));
        lot.setScheduledEndAt(now.plus(i + 2, ChronoUnit.HOURS));
      } else if (i < 35) {
        lot.setStatus("CLOSED");
        lot.setScheduledStartAt(now.minus(48, ChronoUnit.HOURS));
        lot.setScheduledEndAt(now.minus(24, ChronoUnit.HOURS));
        lot.setActualEndAt(now.minus(24, ChronoUnit.HOURS));
        lot.setCurrentPriceCents(15_000 + i * 100L);
        lot.setBidCount(3);
      } else {
        lot.setStatus("DRAFT");
      }
      created.add(lotRepository.save(lot));
    }
    return lotRepository.findBySeller_IdOrderByCreatedAtDesc(sellerId).stream()
        .filter(lot -> lot.getTitle().startsWith(LOT_TITLE_PREFIX))
        .limit(TARGET_LOTS)
        .toList();
  }

  private int ensureDemoBids(List<LotEntity> lots, User buyer, List<UserEntity> extraBuyers) {
    long existing = lots.stream().mapToLong(lot -> bidRepository.countByLot_Id(lot.getId())).sum();
    if (existing >= TARGET_BIDS) {
      return 0;
    }

    Instant now = Instant.now();
    int created = 0;
    List<UserEntity> bidders = new ArrayList<>();
    bidders.add(userJpaRepository.findById(buyer.id()).orElseThrow());
    bidders.addAll(extraBuyers);

    int lotIndex = 0;
    while (existing + created < TARGET_BIDS && !lots.isEmpty()) {
      LotEntity lot = lots.get(lotIndex % lots.size());
      if (!"LIVE".equals(lot.getStatus()) && !"CLOSED".equals(lot.getStatus())) {
        lotIndex++;
        continue;
      }
      UserEntity bidder = bidders.get((lotIndex + created) % bidders.size());
      String clientRequestId = "demo-seed-" + lot.getId() + "-" + (existing + created);
      if (bidRepository
          .findByLot_IdAndBidder_IdAndClientRequestId(lot.getId(), bidder.getId(), clientRequestId)
          .isPresent()) {
        lotIndex++;
        continue;
      }

      long amountCents = lot.getCurrentPriceCents() + lot.getMinIncrementCents();
      BidEntity bid = new BidEntity();
      bid.setLot(lot);
      bid.setBidder(bidder);
      bid.setAmountCents(amountCents);
      bid.setPlacedAt(now.minusSeconds(created));
      bid.setClientRequestId(clientRequestId);
      bid.setCreatedAt(now);
      bidRepository.save(bid);

      lot.setCurrentPriceCents(amountCents);
      lot.setBidCount(lot.getBidCount() + 1);
      lot.setUpdatedAt(now);
      lotRepository.save(lot);
      created++;
      lotIndex++;
    }
    return created;
  }

  public record DemoSeedResult(long buyerId, long sellerId, int lots, int bidsCreated) {}
}
