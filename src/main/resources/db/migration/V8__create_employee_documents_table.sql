CREATE TABLE employee_documents (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,
    employee_id UUID NOT NULL,

    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(150),
    file_size BIGINT,

    uploaded_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_employee_documents_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT fk_employee_documents_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
);

CREATE INDEX idx_employee_documents_organisation_employee
    ON employee_documents (
        organisation_id,
        employee_id,
        created_at
    );

CREATE UNIQUE INDEX uq_employee_documents_storage_key
    ON employee_documents (storage_key);