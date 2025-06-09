# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yass is a multi-client card game application with:
- **Kotlin backend** (Javalin framework, PostgreSQL, jOOQ, Arrow functional programming)
- **React TypeScript client** (modern UI with shadcn/ui, Zustand state management, i18n)
- **Legacy React JS webclient** (minimal Material-UI interface)
- Real-time communication via Centrifugo and WebSockets
- Authentication via Ory

## Development Commands

### Backend (Kotlin/Maven)
- **Start server**: `./mvnw exec:java -Dexec.mainClass="ch.yass.Yass"`
- **Run tests**: `./mvnw test`
- **Build**: `./mvnw clean package -DskipTests`
- **Database migrations**: Set `db.migrate = true` in application.conf, then start server
- **Generate DB code**: `./mvnw jooq-codegen:generate` (after DB migrations)

### Frontend - Main Client
- **Development**: `cd client && npm run dev`
- **Build**: `cd client && npm run build`
- **Lint**: `cd client && npm run lint`
- **Generate API client**: `cd client && npm run generate-client`

### Frontend - Legacy Webclient
- **Development**: `cd webclient && npm run dev`
- **Build**: `cd webclient && npm run build`
- **Lint**: `cd webclient && npm run lint`

### Infrastructure
- **Start dependencies**: `docker compose up` (PostgreSQL, Ory, Centrifugo)

## Architecture

### Backend Structure
- **Domain-driven design** with clear module separation (Game, Identity, Admin, Core)
- **Functional error handling** using Arrow's `raise` DSL and context receivers
- **Database-first approach** with jOOQ for type-safe SQL queries
- **Real-time updates** via Centrifugo pub/sub channels
- **Authentication** integrated with Ory for user management
- **Dependency injection** using Kodein DI

### Frontend Structure  
- **Main client** (`/client`): Modern React with TypeScript, shadcn/ui components, Zustand stores
- **Legacy webclient** (`/webclient`): Simpler React with Material-UI for basic game analysis
- **State management**: Zustand stores for game state, player data, language, loading states
- **Real-time communication**: Centrifuge client for WebSocket connections
- **Internationalization**: i18next with German/English support

### Key Modules
- **Game Engine** (`server/main/kotlin/ch/yass/game/engine/`): Core game logic, card validation, scoring
- **Game Service** (`server/main/kotlin/ch/yass/game/`): Game state management, player actions
- **WebSocket Handler** (`client/src/components/game/websocket-handler.tsx`): Real-time game updates
- **API Layer**: Auto-generated TypeScript client from OpenAPI spec

## Database Workflow
1. Create migration in `server/main/resources/db/migration/`
2. Set `db.migrate = true` in application.conf
3. Start application to run migrations
4. Run `./mvnw jooq-codegen:generate` to update type-safe database code

## Error Handling Philosophy
- **Domain errors**: Use Arrow's `raise` DSL for expected business logic failures
- **Exceptions**: Only for unexpected system failures outside domain scope
- **Type-safe error propagation** throughout the application stack

## Environment Configuration
Required environment variables (use direnv or similar):
- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `CENTRIFUGO_API_KEY`
- `SERVER_CORS`, `COOKIE_DOMAIN`
- `ENVIRONMENT`

## Testing
- **Integration tests** in `server/test/kotlin/ch/yass/integration/`
- **Testcontainers** for PostgreSQL in tests
- **WireMock** for external service mocking
- **No specific frontend test framework** - check before adding tests

## Deployment
Manual deployment process:
1. `./mvnw clean package -DskipTests`
2. `npm run build` (in client directory)
3. `rsync` built artifacts to server
4. Restart service