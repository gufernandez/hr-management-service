package com.banco.hr.infrastructure.adapters.inbound.rest.department;

import jakarta.validation.constraints.NotBlank;

public record CreateDepartmentRequest(
        @NotBlank(message = "O nome do departamento é obrigatório.")
        String name,
        @NotBlank(message = "O código do centro de custo é obrigatório.")
        String costCenterCode) {
}
