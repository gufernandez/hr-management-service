package com.banco.hr.application.ports.outbound;

import com.banco.hr.domain.model.employee.EmployeeEvent;

import java.util.UUID;

public interface EmployeeEventRepositoryPort {

    boolean existsByEventId(UUID eventId);

    void save(EmployeeEvent employeeEvent);
}
