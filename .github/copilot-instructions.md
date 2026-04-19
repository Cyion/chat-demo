# Project Guidelines

Full-stack 1:1 chat application — Spring Boot 4 backend, React 19 frontend, PostgreSQL 17.

## Tech Stack

| Layer | Stack |
|-------|-------|
| Backend | Java 21, Spring Boot 4.0.5, Spring Security 7, Spring Data JPA, WebSocket/STOMP, JWT (JJWT 0.12.6) |
| Frontend | React 19, TypeScript 5.7, Vite 6, Tailwind CSS 4 (`@tailwindcss/vite` plugin), @stomp/stompjs 7, React Router 7 |
| Database | PostgreSQL 17, UUID primary keys |
| Infra | Docker Compose, multi-stage builds, Nginx reverse proxy |

## Build and Test

```bash
# Backend (from backend/)
./mvnw package          # compile + test + JAR
./mvnw test             # tests only

# Frontend (from frontend/)
npm run dev             # Vite dev server (proxy: /api, /ws → :8080)
npm run build           # tsc + Vite production build
npm run lint            # ESLint

# Full stack
docker compose up -d --build
```

## Architecture

- **Backend**: Layered — Controllers → Services → JPA Repositories. Package: `de.bredex.chat.*`
- **Frontend**: React SPA with Context API (AuthContext, WebSocketContext). Centralized `apiFetch()` client with auto JWT header.
- **Auth**: Stateless JWT (24h, no refresh tokens). Token in `sessionStorage`. WebSocket auth via STOMP interceptor.
- **Real-time**: STOMP over WebSocket at `/ws`. Topics: `/topic/chat/{chatId}`, `/topic/user/{userId}`. Subscriptions are validated server-side — users can only subscribe to chats they participate in and their own user topic.
- **Chat dedup**: Sorted participant UUID hash (`participant_hash` column) ensures unique 1:1 chats. Race conditions handled via `DataIntegrityViolationException`.

## Conventions

- **DTOs**: Java records for all request/response objects
- **IDs**: UUID everywhere (entities, DTOs, API paths)
- **DB schema**: Managed via `postgre/init.sql` (mounted in Docker). Hibernate `ddl-auto=validate` ensures entity/schema alignment. Schema changes: update `init.sql`, rebuild containers.
- **Entities**: Temporal fields use `Instant`. Lifecycle managed via `@PrePersist` / `@PreUpdate` callbacks.
- **Transactions**: `@Transactional(readOnly = true)` for queries, `@Transactional` for writes. Default Spring rollback on runtime exceptions.
- **Error handling**: `GlobalExceptionHandler` maps exceptions to `ErrorResponse` (status, error, message, timestamp)
- **Test profile** (`@ActiveProfiles("test")`): H2 in-memory with PostgreSQL mode, `ddl-auto=create-drop`, 1h JWT expiry.
- **Test style**: Unit tests with Mockito + `@ExtendWith(MockitoExtension.class)`. Integration tests with `@SpringBootTest` + MockMvc.
- **Spring Boot 4 / Jackson 3**: Package is `tools.jackson.databind` (not `com.fasterxml.jackson`). The property `spring.jackson.serialization.write-dates-as-timestamps` does not work — Jackson 3 defaults to ISO 8601.
- **Spring Security 7**: Lambda DSL with `authorizeHttpRequests` + `requestMatchers()`. `AutoConfigureMockMvc` is in `org.springframework.boot.webmvc.test.autoconfigure`.
- **Frontend messages**: Deduplicated by ID on receive (guards against HTTP POST + WebSocket double-delivery).

## API

- Swagger UI: `/swagger-ui.html` (via SpringDoc OpenAPI 2.8.6)
- Public routes: `/api/auth/**`, `/ws/**`, Swagger endpoints
- All other routes require `Authorization: Bearer <token>`
- Messages: paginated (`page`, `size` params, default 50)
