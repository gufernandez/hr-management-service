package com.banco.hr.infrastructure.adapters.inbound.rest.employee.dto;

import java.util.List;

import com.banco.hr.domain.model.employee.EmployeePage;

public record EmployeePageResponse(
        List<EmployeeResponse> content,
        long offset,
        int limit,
        long total) {

    public static EmployeePageResponse fromPage(EmployeePage page) {
        return new EmployeePageResponse(
                page.content().stream().map(EmployeeResponse::fromEmployee).toList(),
                page.offset(),
                page.limit(),
                page.total());
    }
}
