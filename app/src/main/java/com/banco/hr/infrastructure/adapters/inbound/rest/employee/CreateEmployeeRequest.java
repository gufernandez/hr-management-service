package com.banco.hr.infrastructure.adapters.inbound.rest.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEmployeeRequest(
        @NotBlank(message = "O nome do colaborador é obrigatório.") String name,
        @NotBlank(message = "O e-mail do colaborador é obrigatório.")
        @Email(message = "O e-mail informado é inválido.") String email,
        @NotNull(message = "O departamento é obrigatório.") UUID departmentId,
        @NotNull(message = "O cargo é obrigatório.") UUID positionId) {
}
