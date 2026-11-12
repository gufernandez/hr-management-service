package com.banco.hr.infrastructure.adapters.inbound.rest.position.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePositionRequest(
        @NotBlank(message = "O título do cargo é obrigatório.")
        String title,
        @NotBlank(message = "O nível do cargo é obrigatório.")
        String level) {
}
