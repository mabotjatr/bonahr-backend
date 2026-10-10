CREATE TABLE leave_types (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,

    paid BOOLEAN NOT NULL,
    requires_approval BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_leave_types_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT uq_leave_types_organisation_code
        UNIQUE (organisation_id, code)
);

CREATE INDEX idx_leave_types_organisation
    ON leave_types (
        organisation_id,
        active
    );


CREATE TABLE leave_requests (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    status VARCHAR(30) NOT NULL,

    reason VARCHAR(1000),

    decision_comment VARCHAR(1000),

    requested_by_user_id UUID,
    decided_by_user_id UUID,

    requested_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_leave_requests_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT fk_leave_requests_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT fk_leave_requests_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(id),

    CONSTRAINT chk_leave_requests_dates
        CHECK (end_date >= start_date)
);

CREATE INDEX idx_leave_requests_organisation_employee
    ON leave_requests (
        organisation_id,
        employee_id,
        start_date
    );

CREATE INDEX idx_leave_requests_organisation_status
    ON leave_requests (
        organisation_id,
        status
    );

CREATE INDEX idx_leave_requests_leave_type
    ON leave_requests (
        leave_type_id
    );