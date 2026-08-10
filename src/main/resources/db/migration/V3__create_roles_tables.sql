CREATE TABLE roles (
    id UUID PRIMARY KEY,

    name VARCHAR(50) NOT NULL,

    description VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_roles_name
        UNIQUE (name)
);

INSERT INTO roles (id, name, description)
VALUES
    (
        gen_random_uuid(),
        'OWNER',
        'Organisation owner with full organisation administration access'
    ),
    (
        gen_random_uuid(),
        'HR_ADMIN',
        'Full human resources administration access'
    ),
    (
        gen_random_uuid(),
        'HR_MANAGER',
        'Human resources operational management access'
    ),
    (
        gen_random_uuid(),
        'MANAGER',
        'Manager access to assigned employees and workflows'
    ),
    (
        gen_random_uuid(),
        'RECRUITER',
        'Recruitment and applicant tracking access'
    ),
    (
        gen_random_uuid(),
        'PAYROLL_ADMIN',
        'Payroll administration access'
    ),
    (
        gen_random_uuid(),
        'EMPLOYEE',
        'Employee self-service access'
    );