package com.paras.restaurant.repositories;

import com.paras.restaurant.domain.entities.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import java.util.List;

/**
 * One combinable Elasticsearch query instead of one repository method per filter:
 * text, minimum rating and distance can all be used together (the old code silently
 * ignored the location whenever a text query was present).
 */
@Repository
@RequiredArgsConstructor
public class RestaurantSearchRepository {

    private final ElasticsearchOperations operations;

    public Page<Restaurant> search(
            String text,
            Float minRating,
            Float latitude,
            Float longitude,
            Float radiusKm,
            Pageable pageable
    ) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        if (StringUtils.hasText(text)) {
            String trimmed = text.trim();
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(trimmed)
                    .fields("name", "cuisineType")
                    .fuzziness("AUTO")));
        }

        if (minRating != null && minRating > 0) {
            double min = minRating;
            bool.filter(f -> f.range(r -> r.number(n -> n.field("averageRating").gte(min))));
        }

        if (latitude != null && longitude != null && radiusKm != null) {
            double lat = latitude;
            double lon = longitude;
            String distance = radiusKm + "km";
            bool.filter(f -> f.geoDistance(g -> g
                    .field("geoLocation")
                    .distance(distance)
                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))));
        }

        return execute(bool, pageable, Sort.by(Sort.Order.desc("averageRating")));
    }

    public Page<Restaurant> findByOwner(String ownerId, Pageable pageable) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.filter(f -> f.term(t -> t.field("createdBy.id").value(ownerId)));
        return execute(bool, pageable, Sort.by(Sort.Order.desc("averageRating")));
    }

    private Page<Restaurant> execute(BoolQuery.Builder bool, Pageable pageable, Sort defaultSort) {
        // An empty bool query matches every document.
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), defaultSort);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool.build()))
                .withPageable(effective)
                .build();

        SearchHits<Restaurant> hits = operations.search(query, Restaurant.class);
        List<Restaurant> content = hits.getSearchHits().stream().map(SearchHit::getContent).toList();
        return new PageImpl<>(content, effective, hits.getTotalHits());
    }
}
