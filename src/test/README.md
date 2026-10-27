# Functional tests

Run from the project root with Java 17 configured:

```powershell
.\gradlew.bat test
```

Open `build/reports/tests/test/index.html` for the results.

`SmartParkFunctionalTest` exercises the API through MockMvc with real JWT login,
services, and an isolated H2 database. It covers registration and normalization,
invalid requests, duplicate registrations, parking conflicts, occupancy and vehicle
listing, checkout and re-entry, minute rounding, and expiration.

Request bodies are in `src/test/resources/payloads/`. Pass a fixture filename to
`postApi(route, fixture)` when adding a case; keep JSON out of Java string literals.
`malformed.json` is deliberately invalid JSON for the bad-request test.

`application-test.yaml` supplies test-only credentials, disables demo seeding and
background scheduling, and selects the test database. Each test cleans the database
and logs in afresh. The test clock advances immediately; expiration tests invoke
the real scheduler method directly, so no sleeps are needed.

This is a focused functional suite, not a load/concurrency test or an exhaustive
JWT/security audit. Production source and build dependencies are unchanged.
