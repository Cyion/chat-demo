# Chat Demo

Full-stack 1:1 chat application with real-time messaging via WebSocket/STOMP.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 4.0.5, Spring Security 7, Spring Data JPA, WebSocket/STOMP, JWT (JJWT 0.12.6) |
| Frontend | React 19, TypeScript 5.7, Vite 6, Tailwind CSS 4, @stomp/stompjs 7, React Router 7 |
| Database | PostgreSQL 17 |
| Infra | Docker Compose, multi-stage builds, Nginx reverse proxy |

## Quick Start

### Docker Compose (recommended)

```bash
docker compose up -d --build
```

The app is available at **http://localhost:3000**. Nginx proxies `/api` and `/ws` to the backend.

### Local Development

**Prerequisites:** Java 21, Node.js 22, PostgreSQL 17

```bash
# Backend (from backend/)
./mvnw package          # compile + test + JAR
./mvnw test             # tests only
./mvnw spring-boot:run  # run dev server on :8080

# Frontend (from frontend/)
npm install
npm run dev             # Vite dev server on :5173 (proxies /api, /ws → :8080)
npm run build           # production build
npm run lint            # ESLint
```

## Project Structure

```
├── docker-compose.yml
├── backend/                        # Spring Boot API
│   └── src/main/java/de/bredex/chat/
│       ├── controller/             # REST endpoints
│       ├── service/                # Business logic
│       ├── repository/             # JPA repositories
│       ├── entity/                 # User, Chat, Message
│       ├── dto/                    # Request/response records
│       ├── security/               # JWT, auth filters, WebSocket interceptor
│       ├── config/                 # Security, WebSocket, CORS
│       └── exception/              # Global error handling
├── frontend/                       # React SPA
│   └── src/
│       ├── api/                    # REST client (apiFetch + endpoint modules)
│       ├── components/             # ChatSidebar, UserSearchModal, ProtectedRoute
│       ├── context/                # AuthContext, WebSocketContext
│       ├── pages/                  # LoginPage, RegisterPage, ChatLayout, ChatPage
│       └── types/                  # TypeScript type definitions
└── postgre/
    └── init.sql                    # Database schema
```

## Architecture

### Backend

Layered architecture: **Controllers → Services → JPA Repositories**.

- **Auth:** Stateless JWT (24h expiry, no refresh tokens). Token stored in `sessionStorage`.
- **Real-time:** STOMP over WebSocket at `/ws`. Topics: `/topic/chat/{chatId}`, `/topic/user/{userId}`. Subscriptions are validated server-side.
- **Chat dedup:** Sorted participant UUID hash (`participant_hash` column) ensures unique 1:1 chats. Race conditions handled via `DataIntegrityViolationException`.
- **Schema management:** `postgre/init.sql` mounted in Docker. Hibernate `ddl-auto=validate` ensures entity/schema alignment.

### Frontend

React SPA using Context API for global state:
- **AuthContext** — JWT management and login state
- **WebSocketContext** — STOMP client lifecycle
- **apiFetch()** — centralized HTTP client with auto JWT header and 401 handling
- Messages deduplicated by ID to guard against HTTP POST + WebSocket double-delivery

## API

**Swagger UI:** [http://localhost:3000/swagger-ui.html](http://localhost:3000/swagger-ui.html) (via SpringDoc OpenAPI 2.8.6)

### Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register a new user |
| POST | `/api/auth/login` | No | Login and receive JWT |

### Users

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/users/search?username=` | Yes | Case-insensitive user search (max 20) |

### Chats

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/chats` | Yes | List current user's chats |
| POST | `/api/chats` | Yes | Create or retrieve existing 1:1 chat |

### Messages

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/chats/{chatId}/messages?page=0&size=50` | Yes | Paginated message history |
| POST | `/api/chats/{chatId}/messages` | Yes | Send a message |

### WebSocket (STOMP)

| Endpoint | Purpose |
|----------|---------|
| `/ws` | STOMP connection endpoint |
| `/topic/chat/{chatId}` | Real-time messages for a chat |
| `/topic/user/{userId}` | User-specific notifications |

Authentication via JWT token in STOMP headers.

## Database Schema

Schema defined in [`postgre/init.sql`](postgre/init.sql). Four tables with UUID primary keys:

| Table | Purpose |
|-------|---------|
| `users` | User accounts (username, password hash) |
| `chats` | Chat rooms with unique `participant_hash` |
| `chat_participants` | Join table linking users to chats |
| `messages` | Chat messages with sender reference |

## Configuration

Configuration via environment variables (with defaults for local dev):

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `chatdb` | Database name |
| `DB_USER` | `chat` | Database user |
| `DB_PASSWORD` | `chat` | Database password |
| `JWT_SECRET` | dev default (256-bit) | HMAC-SHA256 signing key |
| `CORS_ORIGINS` | `http://localhost:3000` | Allowed CORS origins |

## Testing

```bash
# Run all backend tests
cd backend && ./mvnw test
```

- **Unit tests:** Mockito with `@ExtendWith(MockitoExtension.class)`
- **Integration tests:** `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- **Test profile:** H2 in-memory database with PostgreSQL compatibility mode, `ddl-auto=create-drop`, 1h JWT expiry
