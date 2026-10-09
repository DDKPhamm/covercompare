-- Insurers are now remote services that can time out or fail, so an insurer row can also be
-- UNAVAILABLE. The reason column now holds a decline reason or an unavailability reason.

ALTER TABLE insurer_quote RENAME COLUMN decline_reason TO reason;

ALTER TABLE insurer_quote DROP CONSTRAINT insurer_quote_status_check;
ALTER TABLE insurer_quote DROP CONSTRAINT insurer_quote_price_matches_status;

ALTER TABLE insurer_quote
    ADD CONSTRAINT insurer_quote_status_check CHECK (status IN ('QUOTED', 'DECLINED', 'UNAVAILABLE'));

ALTER TABLE insurer_quote
    ADD CONSTRAINT insurer_quote_price_matches_status CHECK (
        (status = 'QUOTED' AND total_annual_premium IS NOT NULL AND reason IS NULL)
        OR (status IN ('DECLINED', 'UNAVAILABLE') AND total_annual_premium IS NULL AND reason IS NOT NULL)
    );
