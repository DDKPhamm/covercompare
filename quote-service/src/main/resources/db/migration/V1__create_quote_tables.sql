CREATE TABLE quote (
    id                      UUID         PRIMARY KEY,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until             TIMESTAMP WITH TIME ZONE NOT NULL,
    cover_type              VARCHAR(40)  NOT NULL,
    driver_date_of_birth    DATE         NOT NULL,
    licence_held_years      INTEGER      NOT NULL CHECK (licence_held_years >= 0),
    no_claims_years         INTEGER      NOT NULL CHECK (no_claims_years >= 0),
    postcode                VARCHAR(8)   NOT NULL,
    vehicle_make            VARCHAR(50)  NOT NULL,
    vehicle_model           VARCHAR(50)  NOT NULL,
    vehicle_year            INTEGER      NOT NULL,
    vehicle_insurance_group INTEGER      NOT NULL CHECK (vehicle_insurance_group BETWEEN 1 AND 50),
    voluntary_excess        INTEGER      NOT NULL CHECK (voluntary_excess >= 0)
);

CREATE TABLE quote_rating_factor (
    quote_id      UUID          NOT NULL REFERENCES quote (id) ON DELETE CASCADE,
    display_order INTEGER       NOT NULL,
    factor        VARCHAR(50)   NOT NULL,
    multiplier    NUMERIC(6, 4) NOT NULL,
    reason        VARCHAR(255)  NOT NULL,
    PRIMARY KEY (quote_id, display_order)
);

CREATE TABLE insurer_quote (
    id                    UUID           PRIMARY KEY,
    quote_id              UUID           NOT NULL REFERENCES quote (id) ON DELETE CASCADE,
    display_order         INTEGER        NOT NULL,
    insurer_code          VARCHAR(30)    NOT NULL,
    insurer_name          VARCHAR(100)   NOT NULL,
    status                VARCHAR(20)    NOT NULL CHECK (status IN ('QUOTED', 'DECLINED')),
    net_premium           NUMERIC(10, 2),
    insurance_premium_tax NUMERIC(10, 2),
    total_annual_premium  NUMERIC(10, 2),
    decline_reason        VARCHAR(255),
    CONSTRAINT insurer_quote_price_matches_status CHECK (
        (status = 'QUOTED' AND total_annual_premium IS NOT NULL AND decline_reason IS NULL)
        OR (status = 'DECLINED' AND total_annual_premium IS NULL AND decline_reason IS NOT NULL)
    )
);

CREATE INDEX idx_insurer_quote_quote_id ON insurer_quote (quote_id);
