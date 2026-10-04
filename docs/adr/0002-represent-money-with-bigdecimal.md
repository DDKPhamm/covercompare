# 2. Represent money with BigDecimal and round once per component

Date: 2026-10-04
Status: Accepted

## Context

Binary floating point (`double`) cannot represent most decimal fractions exactly, so `0.1 + 0.2`
is `0.30000000000000004`. In insurance, penny errors compound across millions of quotes and are
visible to customers and regulators.

## Decision

- All premiums, multipliers and tax rates are `BigDecimal`, created from strings.
- Intermediate results are kept at full precision; rounding happens only when a customer-facing
  amount is produced: net premium and insurance premium tax are each rounded to pence with
  `HALF_UP`, and the total is their sum.
- The database stores money as `NUMERIC(10, 2)`.

## Consequences

- Totals always equal net plus tax exactly, with no "off by a penny" display bugs.
- `BigDecimal` is more verbose than `double`, and equality must use `compareTo` (2.0 and 2.00 are
  not `equals`). Tests use AssertJ's `isEqualByComparingTo` where scale may differ.
