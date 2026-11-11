CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    employee_status VARCHAR(20) NOT NULL,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_outbox_employee_status
        CHECK (employee_status IN ('CREATED', 'ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT ck_outbox_event_type
        CHECK (event_type IN ('EmployeeCreatedEvent', 'EmployeeStatusChangedEvent')),
    CONSTRAINT ck_outbox_processing_status
        CHECK (processing_status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX ix_outbox_events_pending_created_at
    ON outbox_events (processing_status, created_at);
