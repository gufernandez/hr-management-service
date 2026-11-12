package com.banco.hr.infrastructure.adapters.inbound.rest.position.dto;

import com.banco.hr.domain.model.position.Position;
import com.banco.hr.infrastructure.adapters.inbound.rest.ApiDateTimeFormatter;

import java.util.UUID;

public record PositionResponse(
        UUID id,
        String title,
        String level,
        String createdAt) {

    public static PositionResponse fromPosition(Position position) {
        return new PositionResponse(
                position.id().value(),
                position.title().value(),
                position.level().value(),
                ApiDateTimeFormatter.format(position.createdAt()));
    }
}
