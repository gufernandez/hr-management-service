package com.banco.hr.application.ports.inbound;

public record CreateDepartmentCommand(String name, String costCenterCode) {
}
