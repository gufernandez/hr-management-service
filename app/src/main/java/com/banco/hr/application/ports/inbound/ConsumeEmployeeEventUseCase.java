package com.banco.hr.application.ports.inbound;

public interface ConsumeEmployeeEventUseCase {

    boolean consume(EmployeeEventCommand command);
}
