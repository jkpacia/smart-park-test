# SmartPark

A Java 17 / Spring Boot REST API for registering parking lots and vehicles, managing check-in and checkout, tracking available spaces, and calculating parking charges. Uses Spring Security JWT authentication, Spring Data JPA, and an in-memory H2 database.

## Build, run, and test

Install JDK 17 and set `JAVA_HOME` to its installation folder. The Gradle wrapper is included.

On Windows, open the project folder in **Git Bash** (included with Git for Windows). From the project root:

```bash
bash ./gradlew build     # Compile, test, and package
bash ./gradlew bootRun   # Start the API at http://localhost:8080
bash ./gradlew test      # Run functional tests
```

These commands also work in Bash on macOS/Linux.

The executable JAR is created in `build/libs/` (use the JAR without `-plain`). Run it with `java -jar build/libs/smart-park-test-0.0.1-SNAPSHOT.jar`.

Tests cover authentication, registration, validation, parking conflicts, occupancy, billing, and expiration. JSON request fixtures are in `src/test/resources/payloads/`; the HTML test report is at `build/reports/tests/test/index.html`.

## Authentication and sample data

Default local credentials: **admin / smartpark-demo**. Login returns a bearer token valid for one hour. All other endpoints require `Authorization: Bearer <token>`.

Override credentials and the signing secret with `SMARTPARK_USERNAME`, `SMARTPARK_PASSWORD`, and `SMARTPARK_JWT_SECRET` (at least 32 UTF-8 bytes). Configuration is in `src/main/resources/application.yaml`.

Startup preloads empty lots `CENTRAL` (10 spaces, 0.50/minute) and `RIVERSIDE` (5 spaces, 1.00/minute), plus vehicles `CAR-001`, `BIKE-001`, and `TRUCK-001`. H2 data resets when the application restarts.

## API

Requests and responses use JSON. Vehicle types are `CAR`, `MOTORCYCLE`, and `TRUCK`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/auth/login` | Obtain a JWT |
| POST | `/api/lots` | Register a parking lot |
| POST | `/api/vehicles` | Register a vehicle |
| POST | `/api/lots/{lotId}/check-ins` | Park a registered vehicle |
| POST | `/api/lots/{lotId}/check-outs` | Check out and return the parking charge |
| GET | `/api/lots/{lotId}/occupancy` | View occupied and available spaces |
| GET | `/api/lots/{lotId}/vehicles` | List currently parked vehicles |

### Test with Postman

**A Postman collection is included for testing all API endpoints** in `postman/collections/SmartPark API/`, with a local environment at `postman/environments/SmartPark Local.environment.yaml`.

Start the application, open the collection in Postman, and select **SmartPark Local**. Run **Auth > Login** first; its response script saves `accessToken` automatically for subsequent requests. The default `baseUrl` is `http://localhost:8080`.

### Test with curl scripts (Git Bash)

The `curls/` folder contains one numbered `.sh` file per request, with JSON payloads embedded directly in each script. Keep the application running and open a second **Git Bash** terminal in the project folder. Run the files in order:

```bash
bash curls/01-login.sh

# Copy accessToken from the login response (without the Bearer prefix).
export TOKEN='paste-accessToken-here'

bash curls/02-register-lot.sh
bash curls/03-register-vehicle.sh
bash curls/04-check-in.sh
bash curls/05-get-occupancy.sh
bash curls/06-get-parked-vehicles.sh
bash curls/07-check-out.sh
```

The scripts use `http://localhost:8080`; optionally set `export BASE_URL='http://localhost:8081'` to change it. If you changed the login credentials, edit the JSON in `01-login.sh`. Complete checkout within 15 minutes to see normal billing. Repeating registration returns a duplicate conflict; restart the application to reset the demo and run the full sequence again.

## Rules and assumptions

- A vehicle occupies one space and can be parked in only one lot at a time. Full lots, duplicate registrations, and invalid checkouts are rejected.
- License plates are trimmed and uppercased; owner names allow letters and spaces only.
- Checkout charges round each started minute upward using the rate recorded at check-in (61 seconds at 0.50/minute costs 1.00).
- A scheduler checks every second and removes vehicles parked for **more than 15 minutes**. Expired sessions have zero charge; exactly 15 minutes remains billable. Session history and vehicle registrations are retained.
- Errors return JSON with `code`, `message`, `fieldErrors`, `timestamp`, and `path`. Common statuses are 400 for invalid input, 401 for authentication failures, 404 for missing resources, and 409 for conflicts.
