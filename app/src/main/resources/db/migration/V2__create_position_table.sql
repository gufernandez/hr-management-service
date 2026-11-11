CREATE TABLE position (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    level TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
