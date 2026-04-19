---
description: "Use when writing or modifying Spring Boot backend tests in this project, including unit tests, MockMvc integration tests, and test profile setup."
name: "Backend Test Hard Rules"
applyTo: "backend/src/test/**/*.java"
---
# Backend Test Hard Rules

- Treat these rules as mandatory for backend tests in this repository.
- Write clear, focused tests that verify one behavior at a time.
- Prefer explicit, behavior-focused test method names that describe the scenario and expected outcome.
- Follow Arrange-Act-Assert (AAA) pattern: set up test data, execute the code under test, verify results.
- Keep tests independent - each test should run in isolation without depending on other tests.
- Start with the simplest test case, then add edge cases and error conditions.
- Tests should fail for the right reason - verify they catch the bugs they're meant to catch.
- Mock external dependencies to keep tests fast and reliable.
- Unit tests must use Mockito with @ExtendWith(MockitoExtension.class).
- Integration tests must use @SpringBootTest and MockMvc.
- Integration tests must run with @ActiveProfiles("test").
- Keep test data and assertions aligned with UUID-based IDs used by the API and entities.
- For JSON/Jackson references in tests, use Jackson 3 compatible imports (tools.jackson.databind) where applicable.
- For MockMvc auto-configuration, use org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc.
- When adding auth-related tests, assert Bearer token behavior and secured vs public route boundaries.
