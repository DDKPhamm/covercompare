# Service contracts

Example messages that services exchange. Both sides of each integration test against the same
files, so a change that breaks the other side fails the build of the service that made it.

| File | Provider test | Consumer test |
|------|---------------|---------------|
| `pricing/rating-request.json` | `pricing-service`: `RatingContractTest` sends it | `quote-service`: `RatingClientTest` checks it sends exactly this |
| `pricing/rating-response.json` | `pricing-service`: `RatingContractTest` must return exactly this | `quote-service`: `RatingClientTest` must be able to read it |

To change a contract: update the file, then make both services pass. Removing or renaming a field
is a breaking change; adding an optional field is not.
