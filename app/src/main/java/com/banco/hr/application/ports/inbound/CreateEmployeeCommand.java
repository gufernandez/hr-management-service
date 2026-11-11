package com.banco.hr.application.ports.inbound;

import java.util.UUID;

public record CreateEmployeeCommand(
        String name,
        String email,
        UUID departmentId,
        UUID positionId) {
}
