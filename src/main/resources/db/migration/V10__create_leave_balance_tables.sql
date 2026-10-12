CREATE TABLE leave_policies (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,

    accrual_method VARCHAR(30) NOT NULL,
    accrual_rate NUMERIC(12,4) NOT NULL,

    allow_negative BOOLEAN NOT NULL DEFAULT FALSE,
    negative_limit_days NUMERIC(12,4) NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_leave_policies_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT fk_leave_policies_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(id),

    CONSTRAINT uq_leave_policies_organisation_leave_type
        UNIQUE (organisation_id, leave_type_id),

    CONSTRAINT chk_leave_policies_accrual_rate
        CHECK (accrual_rate > 0),

    CONSTRAINT chk_leave_policies_negative_limit
        CHECK (negative_limit_days >= 0),

    CONSTRAINT chk_leave_policies_negative_configuration
        CHECK (
            allow_negative = TRUE
            OR negative_limit_days = 0
        )
);

CREATE INDEX idx_leave_policies_organisation
    ON leave_policies (
        organisation_id,
        active
    );


CREATE TABLE employee_leave_accounts (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,
    leave_policy_id UUID NOT NULL,

    current_balance NUMERIC(12,4) NOT NULL DEFAULT 0,
    reserved_days NUMERIC(12,4) NOT NULL DEFAULT 0,

    last_accrual_date DATE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_employee_leave_accounts_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT fk_employee_leave_accounts_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT fk_employee_leave_accounts_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(id),

    CONSTRAINT fk_employee_leave_accounts_policy
        FOREIGN KEY (leave_policy_id)
        REFERENCES leave_policies(id),

    CONSTRAINT uq_employee_leave_accounts_employee_leave_type
        UNIQUE (
            organisation_id,
            employee_id,
            leave_type_id
        ),

    CONSTRAINT chk_employee_leave_accounts_reserved_days
        CHECK (reserved_days >= 0)
);

CREATE INDEX idx_employee_leave_accounts_organisation_employee
    ON employee_leave_accounts (
        organisation_id,
        employee_id
    );

CREATE INDEX idx_employee_leave_accounts_policy
    ON employee_leave_accounts (
        leave_policy_id
    );


CREATE TABLE leave_balance_transactions (
    id UUID PRIMARY KEY,

    organisation_id UUID NOT NULL,
    leave_account_id UUID NOT NULL,
    leave_request_id UUID,

    transaction_type VARCHAR(40) NOT NULL,

    amount NUMERIC(12,4) NOT NULL,

    balance_after NUMERIC(12,4) NOT NULL,
    reserved_after NUMERIC(12,4) NOT NULL,

    actor_user_id UUID,

    comment VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_leave_balance_transactions_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id),

    CONSTRAINT fk_leave_balance_transactions_account
        FOREIGN KEY (leave_account_id)
        REFERENCES employee_leave_accounts(id),

    CONSTRAINT fk_leave_balance_transactions_leave_request
        FOREIGN KEY (leave_request_id)
        REFERENCES leave_requests(id),

    CONSTRAINT chk_leave_balance_transactions_reserved_after
        CHECK (reserved_after >= 0)
);

CREATE INDEX idx_leave_balance_transactions_account
    ON leave_balance_transactions (
        leave_account_id,
        created_at
    );

CREATE INDEX idx_leave_balance_transactions_leave_request
    ON leave_balance_transactions (
        leave_request_id
    );

CREATE INDEX idx_leave_balance_transactions_organisation
    ON leave_balance_transactions (
        organisation_id,
        created_at
    );