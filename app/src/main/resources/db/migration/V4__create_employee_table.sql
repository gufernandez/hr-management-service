CREATE TABLE employee (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    department_id UUID NOT NULL,
    position_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_employee_email UNIQUE (email),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_employee_position FOREIGN KEY (position_id) REFERENCES position (id),
    CONSTRAINT ck_employee_status CHECK (status IN ('CREATED', 'ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX ix_employee_department_status_created_at
    ON employee (department_id, status, created_at DESC);

CREATE INDEX ix_employee_status_created_at
    ON employee (status, created_at DESC);
