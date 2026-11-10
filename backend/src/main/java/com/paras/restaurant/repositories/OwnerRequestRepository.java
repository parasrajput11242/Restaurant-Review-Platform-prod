package com.paras.restaurant.repositories;

import com.paras.restaurant.domain.entities.OwnerRequest;
import com.paras.restaurant.domain.entities.OwnerRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerRequestRepository extends ElasticsearchRepository<OwnerRequest, String> {

    Page<OwnerRequest> findByStatus(OwnerRequestStatus status, Pageable pageable);
}
