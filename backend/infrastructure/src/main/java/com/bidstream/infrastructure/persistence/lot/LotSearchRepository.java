package com.bidstream.infrastructure.persistence.lot;

import com.bidstream.domain.lot.CategoryFacet;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotSearchFacets;
import com.bidstream.domain.lot.LotSearchHit;
import com.bidstream.domain.lot.LotSearchResult;
import com.bidstream.domain.lot.LotSort;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.lot.PriceRangeFacet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class LotSearchRepository {

  private static final List<long[]> PRICE_BUCKETS =
      List.of(
          new long[] {0L, 10_000L},
          new long[] {10_000L, 50_000L},
          new long[] {50_000L, 100_000L},
          new long[] {100_000L, Long.MAX_VALUE});

  private final EntityManager entityManager;
  private final LotJpaRepository lotJpaRepository;

  LotSearchRepository(EntityManager entityManager, LotJpaRepository lotJpaRepository) {
    this.entityManager = entityManager;
    this.lotJpaRepository = lotJpaRepository;
  }

  LotSearchResult search(LotQuery query) {
    String tsQuery = "websearch_to_tsquery('spanish', :q)";
    LotStatus status = query.status().orElse(LotStatus.LIVE);
    SearchFilters filters = SearchFilters.from(query, status);

    long total = countMatches(tsQuery, filters);
    List<LotSearchHit> hits = findHits(tsQuery, filters, query);
    Map<Long, Double> ranks = new LinkedHashMap<>();
    List<Lot> lots = new ArrayList<>();
    for (LotSearchHit hit : hits) {
      lots.add(hit.lot());
      ranks.put(hit.lot().id(), hit.rank());
    }
    LotPage page = new LotPage(lots, query.page(), query.size(), total);

    Optional<LotSearchFacets> facets =
        query.facets() ? Optional.of(computeFacets(tsQuery, filters)) : Optional.empty();
    return new LotSearchResult(page, ranks, facets);
  }

  String explainPlan(String searchTerm) {
    Query explain =
        entityManager.createNativeQuery(
            """
            EXPLAIN
            SELECT l.id
            FROM lots l
            WHERE l.search_vector @@ websearch_to_tsquery('spanish', :q)
              AND l.status = 'LIVE'
            ORDER BY ts_rank(l.search_vector, websearch_to_tsquery('spanish', :q)) DESC, l.id ASC
            LIMIT 1
            """);
    explain.setParameter("q", searchTerm);
    @SuppressWarnings("unchecked")
    List<String> rows = explain.getResultList();
    return String.join("\n", rows);
  }

  private long countMatches(String tsQuery, SearchFilters filters) {
    Query countQuery =
        entityManager.createNativeQuery(
            """
            SELECT COUNT(*)
            FROM lots l
            WHERE l.search_vector @@ """
                + tsQuery
                + filters.whereClause());
    bindFilters(countQuery, filters);
    return ((Number) countQuery.getSingleResult()).longValue();
  }

  private List<LotSearchHit> findHits(String tsQuery, SearchFilters filters, LotQuery query) {
    String orderBy = orderByClause(query.sort(), tsQuery);
    Query dataQuery =
        entityManager.createNativeQuery(
            """
            SELECT l.id, ts_rank(l.search_vector, """
                + tsQuery
                + ") AS rank "
                + """
            FROM lots l
            WHERE l.search_vector @@ """
                + tsQuery
                + filters.whereClause()
                + " "
                + orderBy
                + " LIMIT :limit OFFSET :offset");
    bindFilters(dataQuery, filters);
    dataQuery.setParameter("limit", query.size());
    dataQuery.setParameter("offset", (long) query.page() * query.size());

    @SuppressWarnings("unchecked")
    List<Object[]> rows = dataQuery.getResultList();
    List<LotSearchHit> hits = new ArrayList<>();
    for (Object[] row : rows) {
      long lotId = ((Number) row[0]).longValue();
      double rank = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0d;
      Lot lot =
          lotJpaRepository
              .findById(lotId)
              .map(LotEntityMapper::toDomain)
              .orElseThrow(() -> new IllegalStateException("Lot not found: " + lotId));
      hits.add(new LotSearchHit(lot, rank));
    }
    return hits;
  }

  private LotSearchFacets computeFacets(String tsQuery, SearchFilters filtersWithoutCategory) {
    SearchFilters facetFilters = filtersWithoutCategory.withoutCategory();
    Map<Long, CategoryFacet> categories = new LinkedHashMap<>();

    Query categoryQuery =
        entityManager.createNativeQuery(
            """
            SELECT c.id, c.name, COUNT(*) AS cnt
            FROM lots l
            JOIN categories c ON c.id = l.category_id
            WHERE l.search_vector @@ """
                + tsQuery
                + facetFilters.whereClause()
                + """
            GROUP BY c.id, c.name
            ORDER BY cnt DESC, c.name ASC
            """);
    bindFilters(categoryQuery, facetFilters);
    @SuppressWarnings("unchecked")
    List<Object[]> categoryRows = categoryQuery.getResultList();
    for (Object[] row : categoryRows) {
      long id = ((Number) row[0]).longValue();
      String name = (String) row[1];
      long count = ((Number) row[2]).longValue();
      categories.put(id, new CategoryFacet(id, name, count));
    }

    List<PriceRangeFacet> priceRanges = new ArrayList<>();
    for (long[] bucket : PRICE_BUCKETS) {
      long from = bucket[0];
      long to = bucket[1];
      Query priceQuery =
          entityManager.createNativeQuery(
              """
              SELECT COUNT(*)
              FROM lots l
              WHERE l.search_vector @@ """
                  + tsQuery
                  + facetFilters.whereClause()
                  + """
               AND l.current_price_cents >= :fromCents
               AND l.current_price_cents < :toCents
              """);
      bindFilters(priceQuery, facetFilters);
      priceQuery.setParameter("fromCents", from);
      long toParam = to == Long.MAX_VALUE ? Long.MAX_VALUE : to;
      priceQuery.setParameter("toCents", toParam);
      long count = ((Number) priceQuery.getSingleResult()).longValue();
      long displayTo = to == Long.MAX_VALUE ? Long.MAX_VALUE : to;
      priceRanges.add(new PriceRangeFacet(from, displayTo, count));
    }

    return new LotSearchFacets(new ArrayList<>(categories.values()), priceRanges);
  }

  private static String orderByClause(LotSort sort, String tsQuery) {
    return switch (sort) {
      case ENDING_SOON ->
          "ORDER BY ts_rank(l.search_vector, "
              + tsQuery
              + ") DESC, l.scheduled_end_at ASC, l.id ASC";
      case NEWEST ->
          "ORDER BY ts_rank(l.search_vector, " + tsQuery + ") DESC, l.created_at DESC, l.id ASC";
      case PRICE_ASC ->
          "ORDER BY ts_rank(l.search_vector, "
              + tsQuery
              + ") DESC, l.current_price_cents ASC, l.id ASC";
      case PRICE_DESC ->
          "ORDER BY ts_rank(l.search_vector, "
              + tsQuery
              + ") DESC, l.current_price_cents DESC, l.id ASC";
    };
  }

  private static void bindFilters(Query query, SearchFilters filters) {
    query.setParameter("q", filters.searchTerm());
    query.setParameter("status", filters.status().name());
    filters.categoryId().ifPresent(id -> query.setParameter("categoryId", id));
    filters.minPriceCents().ifPresent(min -> query.setParameter("minPriceCents", min));
    filters.maxPriceCents().ifPresent(max -> query.setParameter("maxPriceCents", max));
    filters.sellerId().ifPresent(id -> query.setParameter("sellerId", id));
  }

  private record SearchFilters(
      String searchTerm,
      LotStatus status,
      Optional<Long> categoryId,
      Optional<Long> minPriceCents,
      Optional<Long> maxPriceCents,
      Optional<Long> sellerId) {

    static SearchFilters from(LotQuery query, LotStatus status) {
      return new SearchFilters(
          query.searchQuery().orElse(""),
          status,
          query.categoryId(),
          query.minPriceCents(),
          query.maxPriceCents(),
          query.sellerId());
    }

    SearchFilters withoutCategory() {
      return new SearchFilters(
          searchTerm, status, Optional.empty(), minPriceCents, maxPriceCents, sellerId);
    }

    String whereClause() {
      StringBuilder clause = new StringBuilder();
      clause.append(" AND l.status = :status");
      clause.append(" AND l.status NOT IN ('DRAFT', 'CANCELLED')");
      categoryId.ifPresent(id -> clause.append(" AND l.category_id = :categoryId"));
      minPriceCents.ifPresent(min -> clause.append(" AND l.current_price_cents >= :minPriceCents"));
      maxPriceCents.ifPresent(max -> clause.append(" AND l.current_price_cents <= :maxPriceCents"));
      sellerId.ifPresent(id -> clause.append(" AND l.seller_id = :sellerId"));
      return clause.toString();
    }
  }
}
