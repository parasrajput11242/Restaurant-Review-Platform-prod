package com.paras.restaurant.mappers;

import com.paras.restaurant.domain.dtos.OwnerRequestDto;
import com.paras.restaurant.domain.entities.OwnerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OwnerRequestMapper {

    OwnerRequestDto toDto(OwnerRequest request);
}
