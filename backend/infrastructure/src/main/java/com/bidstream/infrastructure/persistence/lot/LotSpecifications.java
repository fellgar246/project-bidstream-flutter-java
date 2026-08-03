package com.bidstream.infrastructure.persistence.lot;

import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotSort;
import com.bidstream.domain.lot.LotStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class LotSpecifications {

  private LotSpecifications() {}

  static Specification<LotEntity> forPublicQuery(LotQuery query) {
    return (root, cq, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      LotStatus status = query.status().orElse(LotStatus.LIVE);
      predicates.add(cb.equal(root.get("status"), status.name()));
      predicates.add(cb.notEqual(root.get("status"), LotStatus.DRAFT.name()));
      predicates.add(cb.notEqual(root.get("status"), LotStatus.CANCELLED.name()));

      query
          .categoryId()
          .ifPresent(
              categoryId -> predicates.add(cb.equal(root.get("category").get("id"), categoryId)));
      query
          .sellerId()
          .ifPresent(sellerId -> predicates.add(cb.equal(root.get("seller").get("id"), sellerId)));
      query
          .minPriceCents()
          .ifPresent(
              min -> predicates.add(cb.greaterThanOrEqualTo(root.get("currentPriceCents"), min)));
      query
          .maxPriceCents()
          .ifPresent(
              max -> predicates.add(cb.lessThanOrEqualTo(root.get("currentPriceCents"), max)));
      query
          .searchQuery()
          .filter(q -> !q.isBlank())
          .ifPresent(
              q ->
                  predicates.add(
                      cb.like(cb.lower(root.get("title")), "%" + q.toLowerCase() + "%")));

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  static org.springframework.data.domain.Sort toSort(LotSort sort) {
    return switch (sort) {
      case ENDING_SOON ->
          org.springframework.data.domain.Sort.by(
              org.springframework.data.domain.Sort.Order.asc("scheduledEndAt"),
              org.springframework.data.domain.Sort.Order.asc("id"));
      case NEWEST ->
          org.springframework.data.domain.Sort.by(
              org.springframework.data.domain.Sort.Order.desc("createdAt"),
              org.springframework.data.domain.Sort.Order.asc("id"));
      case PRICE_ASC ->
          org.springframework.data.domain.Sort.by(
              org.springframework.data.domain.Sort.Order.asc("currentPriceCents"),
              org.springframework.data.domain.Sort.Order.asc("id"));
      case PRICE_DESC ->
          org.springframework.data.domain.Sort.by(
              org.springframework.data.domain.Sort.Order.desc("currentPriceCents"),
              org.springframework.data.domain.Sort.Order.asc("id"));
    };
  }
}
