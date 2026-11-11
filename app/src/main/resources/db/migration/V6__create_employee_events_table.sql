CREATE TABLE employee_events (
    event_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    employee_status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_employee_events_type
        CHECK (event_type IN ('CREATION', 'UPDATE')),
    CONSTRAINT ck_employee_events_status
        CHECK (employee_status IN ('CREATED', 'ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX ix_employee_events_employee_time
    ON employee_events (employee_id, event_time);
