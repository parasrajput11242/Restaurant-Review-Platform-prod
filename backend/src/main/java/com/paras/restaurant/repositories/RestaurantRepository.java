package com.paras.restaurant.repositories;

import com.paras.restaurant.domain.entities.Restaurant;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/** CRUD only. Searching (text, rating, distance, owner) is in RestaurantSearchRepository. */
@Repository
public interface RestaurantRepository extends ElasticsearchRepository<Restaurant, String> {
}
