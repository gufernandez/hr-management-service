package com.banco.hr.application.ports.inbound;

import com.banco.hr.application.ports.inbound.dto.EmployeeEventCommand;

public interface ConsumeEmployeeEventUseCase {

    boolean consume(EmployeeEventCommand command);
}
