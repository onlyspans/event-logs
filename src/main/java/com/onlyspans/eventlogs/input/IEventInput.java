package com.onlyspans.eventlogs.input;

import com.onlyspans.eventlogs.entity.EventEntity;

import java.util.List;

public interface IEventInput {
    List<EventEntity> read();
}

