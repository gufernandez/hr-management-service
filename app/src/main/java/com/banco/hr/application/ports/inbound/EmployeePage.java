package com.banco.hr.application.ports.inbound;

import com.banco.hr.domain.model.employee.Employee;

import java.util.List;
import java.util.Objects;

public record EmployeePage(
        List<Employee> content,
        long offset,
        int limit,
        long total) {

    public EmployeePage {
        content = List.copyOf(Objects.requireNonNull(content));
        if (offset < 0 || limit < 1 || total < 0) {
            throw new IllegalArgumentException("Os metadados da página de colaboradores são inválidos.");
        }
        if (content.size() > limit) {
            throw new IllegalArgumentException("O conteúdo excede o limite da página.");
        }
    }
}
