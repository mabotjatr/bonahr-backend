CREATE TABLE employee_audit_log (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,
    employee_id UUID NOT NULL,

    action VARCHAR(50) NOT NULL,

    actor_user_id UUID,

    changes JSONB,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_employee_audit_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT fk_employee_audit_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
);

CREATE INDEX idx_employee_audit_organisation_employee
    ON employee_audit_log (
        organisation_id,
        employee_id,
        created_at
    );