CREATE TABLE organisations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200),
    registration_number VARCHAR(100),
    tax_number VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_organisations_registration_number
        UNIQUE (registration_number),

    CONSTRAINT uq_organisations_tax_number
        UNIQUE (tax_number),

    CONSTRAINT chk_organisations_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE INDEX idx_organisations_status
    ON organisations(status);