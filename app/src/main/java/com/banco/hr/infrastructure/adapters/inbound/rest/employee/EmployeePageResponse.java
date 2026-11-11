package com.banco.hr.infrastructure.adapters.inbound.rest.employee;

import com.banco.hr.application.ports.inbound.EmployeePage;

import java.util.List;

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
