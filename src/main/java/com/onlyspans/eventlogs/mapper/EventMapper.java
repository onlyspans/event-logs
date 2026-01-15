package com.onlyspans.eventlogs.mapper;

import com.onlyspans.eventlogs.dto.ExportEventsRequest;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.dto.SearchEventsRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    QueryDto toQueryDto(SearchEventsRequest request);

    @Mapping(target = "page", constant = "0")
    @Mapping(target = "size", expression = "java(Integer.MAX_VALUE)")
    QueryDto toQueryDto(ExportEventsRequest request);
}
