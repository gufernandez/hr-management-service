ALTER TABLE position
    ADD CONSTRAINT uq_position_title_level UNIQUE (title, level);
