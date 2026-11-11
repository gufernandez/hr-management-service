CREATE TABLE department (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    cost_center_code TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_department_name UNIQUE (name)
);
