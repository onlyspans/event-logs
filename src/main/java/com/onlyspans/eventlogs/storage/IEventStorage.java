package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.dto.PagedResult;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.EventEntity;

import java.util.List;

public interface IEventStorage {
    void add(List<EventEntity> events);
    PagedResult<EventEntity> search(QueryDto query);
    long count(QueryDto query);
}

