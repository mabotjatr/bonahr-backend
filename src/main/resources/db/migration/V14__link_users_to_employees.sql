ALTER TABLE users
    ADD COLUMN employee_id UUID;

ALTER TABLE users
    ADD CONSTRAINT fk_users_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id);

ALTER TABLE users
    ADD CONSTRAINT uq_users_employee
        UNIQUE (employee_id);