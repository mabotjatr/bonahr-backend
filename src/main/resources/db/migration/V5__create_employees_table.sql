create TABLE employees (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,

    employee_number VARCHAR(50) NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(255),

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_employees_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT uq_employees_organisation_employee_number
        UNIQUE (organisation_id, employee_number),

    CONSTRAINT uq_employees_organisation_email
        UNIQUE (organisation_id, email)
);