# Parking API
A REST API that finds parking places near a position. It returns the nearest parking places first and shows how many free spaces are available.
It works for several cities. Each city has its own data source, but web and mobile apps always use the same API. 

## 1. What was done, how and why
### The API
```
GET /api/v1/parkings?lat=46.5838&lon=0.3377&radius=1000
```
```json
{
  "parkings": [
    {
      "name": "THEATRE",
      "latitude": 46.58383455409422,
      "longitude": 0.33779491061805567,
      "distanceMeters": 8,
      "availableSpaces": 93,
      "totalSpaces": 320
    }
  ]
}
```

| Param    | Required | Notes                         |
|----------|----------|-------------------------------|
| `lat`    | yes      | -90..90                       |
| `lon`    | yes      | -180..180                     |
| `radius` | no       | meters, 1..5000, default 1000 |

`availableSpaces` / `totalSpaces` are `null` when the provider does not report them. The client never names a city: it is found from `lat` / `lon`.

Every error is an RFC 9457 body (`application/problem+json`), for example:

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400,
  "detail": "Invalid request parameters: lat must be between -90 and 90", "instance": "/api/v1/parkings" }
```

| Status | `title` | When | `detail` |
|---|---|---|---|
| 400 | Bad Request | `lat`, `lon` or `radius` out of range (`NaN` included) | `Invalid request parameters: ` + e.g. `lat must be between -90 and 90` |
| 400 | Bad Request | missing or non-numeric parameter | e.g. `Required parameter 'lat' is not present.` |
| 404 | City not covered | the position does not belong to any configured city | `No supported city at this position` |
| 404 | Not Found | unknown path | |
| 405 | Method Not Allowed | `POST`, `PUT`, `PATCH`, `DELETE` (`HEAD` and `OPTIONS` are accepted) | |
| 500 | Internal Server Error | unexpected error, logged with its stack trace | `An unexpected error occurred` |
| 502 | Provider failure | the city's provider answered an HTTP error, an unreadable or empty body, or is unreachable | `The parking data source is currently unavailable` |
| 504 | Provider timeout | the city's provider did not answer within the city's `timeout` | `The parking data source did not answer in time` |

Internal details (provider URL, provider error, stack trace) are logged, never returned.

### Main Technical Choices

- **Three data models:** Provider data is mapped to an internal model and then to a public API DTO using MapStruct. This keeps the API consistent across cities, even when provider formats change.
- **One adapter per provider:** A common interface selects the correct adapter for each city. Adding a new city does not require changes to the controller or service.
- **Automatic city detection:** The API finds the nearest city center within its coverage radius. It returns `404` if no city matches and validates the configuration at startup.
- **Distance calculation and sorting:** The API calculates distances, filters results by radius, and sorts them because providers return all parking locations without geographic filtering.
- **30-second cache per city:** Caffeine reduces response times and provider load. Cache expiry is configurable, failed requests are not cached, and concurrent requests share one provider call.
- **Automatic retries:** Temporary network errors and selected server errors are retried twice with increasing delays to improve reliability.
- **Standard error responses:** RFC 9457 Problem Details provides a consistent error format and prevents internal details from being exposed.
- **Input validation:** Spring Bean Validation checks request parameters and returns clear `400` responses for invalid inputs. The rules are also documented in OpenAPI.
- **Tolerant provider parsing:** Only required fields are mapped, and entries with invalid locations are skipped to prevent incomplete data from breaking the entire response.
- **Web and mobile support:** CORS, gzip compression, and the versioned `/api/v1` endpoint support browser and mobile clients while allowing future API changes.
- **Privacy:** The user's location is used for the search but is never logged, avoiding unnecessary storage of personal data.
- **Tests at different levels:** Unit tests cover business logic, mapping, caching, HTTP calls, and web validation. Integration tests verify the full application using mocked providers, including error scenarios and OpenAPI export.
- **OpenAPI documentation:** The API spec is generated from code and validated against the implementation. It is available in JSON and YAML formats, and Swagger UI provides an interactive interface for testing.
- **Docker multi-stage build:** The Dockerfile uses a multi-stage build to produce a small image with only the JRE and a non-root user.
- **CI/CD:** GitHub Actions runs the tests and builds the Docker image on pushes to `main` and branches matching `feature/**` and on pull requests to `main`. The OpenAPI spec is exported to `docs/` for documentation.

### Run, test, document

JDK 21+ and Maven 3.9+ (Spring Boot 4).

```bash
mvn spring-boot:run
curl "http://localhost:8080/api/v1/parkings?lat=46.5838&lon=0.3377&radius=1000"
```

- **Tests:** `mvn test` runs the small tests. `mvn verify` runs every test, including integration tests and the API spec check.
- **Docker:** `docker build -t parking-api . && docker run -p 8080:8080 -e POITIERS_PROVIDER_URL=... parking-api` (multi-stage build, JRE only, non-root user).
- **API docs:** Swagger UI is at http://localhost:8080/swagger-ui.html. The API spec is at `/v3/api-docs`. `mvn verify` also saves it in `docs/openapi.yaml` and `docs/openapi.json`.
- **Health:** `/actuator/health`.

---

## 2. Improvements for a Real-World Project

The following improvements were identified in this project and should be implemented in a real-world project.

### Performance and Shared Cache
- **Shared cache:** The current cache is stored in memory, so each application instance has its own data. In a real project, use a shared cache like Redis to keep data consistent across instances and reduce calls to external providers.
- **Pagination:** The Poitiers API returns all parking locations at once. Other providers may use pagination, so the application should support fetching all pages when needed.

### Resilience and Error Handling
- **Circuit breaker and fallback:** Add a circuit breaker to stop repeated calls to an unavailable provider. When possible, return the last successful result and clearly mark it as outdated instead of returning an error immediately.

### Security and Access Control
- **Rate limiting:** Limit the number of requests per client to prevent excessive traffic and protect the application and external providers. Return `429 Too Many Requests` with a `Retry-After` header when the limit is reached.
- **HTTPS and security headers:** Configure HTTPS and security headers at the API gateway. Disable Swagger UI in production or protect it with authentication.

### City Configuration and Geographic Accuracy
- **Dynamic city configuration:** Cities are currently defined in `application.yml`, so adding a city requires a redeployment. In a real project, store city configurations and geographic boundaries (polygons) in a database.
- **Spatial indexing:** Use an in-memory spatial index refreshed periodically to find the correct city efficiently without querying the database for every request.

## 3. Functional suggestions
- **Add the provider data update time:** Include the date and time when the parking data was last updated in the API response. This helps clients know whether the number of available spaces is recent or potentially outdated.
- **Return parkings without coordinates:** Currently, parkings without coordinates are ignored. An alternative is to return them without coordinates, allowing mobile applications to display them in a list even when they cannot be shown on a map.
- **Search across multiple cities:** Currently, when a location matches two cities, the API selects the city with the nearest center. Instead, it could query both cities and merge the results. This would help users living near city borders find nearby parkings in either city.