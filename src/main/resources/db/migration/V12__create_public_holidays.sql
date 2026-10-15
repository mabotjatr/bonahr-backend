CREATE TABLE public_holidays (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    holiday_date DATE NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_public_holidays_country_date
        UNIQUE (country_code, holiday_date)
);

CREATE INDEX idx_public_holidays_country_date_active
    ON public_holidays (
        country_code,
        holiday_date,
        active
    );