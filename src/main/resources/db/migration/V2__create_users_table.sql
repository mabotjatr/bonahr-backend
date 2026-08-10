CREATE TABLE users (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,

    email VARCHAR(255) NOT NULL,

    password_hash VARCHAR(255),

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'INVITED',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT uq_users_organisation_email
        UNIQUE (organisation_id, email),

    CONSTRAINT chk_users_status
        CHECK (
            status IN (
                'INVITED',
                'ACTIVE',
                'SUSPENDED',
                'LOCKED'
            )
        )
);

CREATE INDEX idx_users_organisation_id
    ON users(organisation_id);

CREATE INDEX idx_users_organisation_email
    ON users(organisation_id, email);

CREATE INDEX idx_users_status
    ON users(status);