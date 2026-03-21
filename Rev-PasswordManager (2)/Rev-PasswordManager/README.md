# RevVault — AI-Powered Password Manager

A full-stack, enterprise-grade password management application with integrated AI security features. Built with Angular 18, Spring Boot 3, and MySQL, with optional deployment via Docker Compose.

---

## ✨ Features

### 🔐 Core Password Management
- **Encrypted Vault** — Store passwords, login credentials, secure notes, and file attachments with AES-256-GCM encryption
- **Categories & Folders** — Organize vault entries with custom categories and nested folders
- **Favorites & Filters** — Quickly access pinned entries, recently used items, or trashed records
- **Secure Sharing** — Share vault entries via time-limited, token-protected links
- **Vault Snapshots** — Import and export your vault with automatic backup snapshots

### 🤖 AI Security Features (Powered by Groq LLaMA 3.3 70B)
- **AI Password Analyzer** — Get instant AI-generated security analysis of any password including strength rating, vulnerabilities, and suggestions
- **AI Auto-Categorize** — Automatically suggest categories and tags for new vault entries based on the website and username
- **AI Security Assistant** — A globally accessible floating chat widget that is vault-aware and can answer questions like *"Show my weak passwords"* or *"What is my security score?"*

### 🛡️ Security & Authentication
- JWT-based authentication with access and refresh tokens
- Two-Factor Authentication (2FA) support
- Duress password support for coercion scenarios
- Rate limiting, session management, and audit logging
- Security alerts and login history tracking
- Idle session auto-logout

### 📊 Dashboard & Analytics
- Overall security score (0–100)
- Password health breakdown by strength category
- Reused password detection
- Password age tracking (flags passwords older than 90 days)
- Security trend history

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 18, TypeScript, Lucide Icons |
| Backend | Spring Boot 3, Java 21, Spring Security |
| Database | MySQL 8.0 |
| AI / LLM | Groq API — `llama-3.3-70b-versatile` |
| HTTP Client | OkHttp3 (backend AI calls) |
| Auth | JWT (HS384), BCrypt |
| Containerization | Docker, Docker Compose |
| Web Server | Nginx (production frontend) |
| Build Tools | Maven 3.9, Node.js 20, Angular CLI |

---

## 🚀 Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (for containerized deployment)
- **OR** for local development:
  - Java 21 (JDK)
  - Maven 3.9+
  - Node.js 20+ and npm
  - MySQL 8.0

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd Rev-PasswordManager
```

### 2. Configure Environment Variables

Create a `.env` file in the project root (next to `docker-compose.yml`):

```env
# Database
MYSQL_ROOT_PASSWORD=your-secure-root-password
MYSQL_DATABASE=rev_password_manager
MYSQL_USER=appuser
MYSQL_PASSWORD=your-secure-app-password

# JWT — use a long random secret in production
JWT_SECRET=your-very-long-random-jwt-secret

# Email (optional — for registration verification and password reset)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your@gmail.com
SPRING_MAIL_PASSWORD=your-app-password

# AI — Groq API Key (free at https://console.groq.com)
LLM_API_KEY=gsk_your_groq_api_key_here
```

---

## 🐳 Deployment with Docker Compose

The easiest way to run the full stack:

```bash
docker compose up -d --build
```

This spins up three containers:

| Container | Service | Port |
|---|---|---|
| `password-manager-mysql` | MySQL 8.0 database | `3307` (host) |
| `password-manager-backend` | Spring Boot API | `8082` (host → `8080` internal) |
| `password-manager-frontend` | Angular app via Nginx | `80` (host) |

Open **http://localhost** in your browser.

To stop:
```bash
docker compose down
```

To stop and wipe all data volumes:
```bash
docker compose down -v
```

---

## 💻 Local Development

### Backend (Spring Boot)

```bash
# From the project root
mvn spring-boot:run
```

The API server will start at **http://localhost:8080**.

> **Note:** Ensure the MySQL container is running first:
> ```bash
> docker start password-manager-mysql
> ```

The local profile reads from `src/main/resources/application-local.properties`. The MySQL port defaults to `3307` to match the Docker mapping.

### Frontend (Angular)

```bash
cd frontend
npm install --legacy-peer-deps
npm run start
```

The dev server starts at **http://localhost:4200** with hot-reload.

---

## 🤖 AI Configuration

The AI features use [Groq](https://console.groq.com) for fast, free LLM inference.

Get a free API key at **https://console.groq.com** and set it in `application.properties`:

```properties
llm.provider=OPENAI
llm.base-url=https://api.groq.com/openai/v1
llm.api-key=gsk_your_groq_api_key
llm.model=llama-3.3-70b-versatile
llm.temperature=0.7
llm.max-tokens=2048
```

### AI Endpoints

| Endpoint | Description |
|---|---|
| `POST /api/ai/analyze-password` | Password strength analysis |
| `POST /api/ai/categorize-entry` | Auto-categorize a vault entry |
| `POST /api/ai/chat` | AI security assistant (vault-aware) |
| `GET /api/ai/health` | Check LLM service connectivity |

---

## 📁 Project Structure

```
Rev-PasswordManager/
├── src/main/java/com/revature/passwordmanager/
│   ├── controller/          # REST API controllers
│   ├── service/             # Business logic
│   │   └── ai/              # AI services (chat, analysis, categorization)
│   ├── config/              # Spring configuration (LLM, CORS, Security)
│   ├── dto/                 # Request/Response DTOs
│   ├── entity/              # JPA entities
│   └── security/            # JWT filters, guards
├── src/main/resources/
│   ├── application.properties         # Shared config
│   └── application-local.properties   # Local dev overrides
├── frontend/
│   ├── src/app/
│   │   ├── core/            # Guards, interceptors, API services
│   │   ├── features/        # Page components (vault, dashboard, ai, auth)
│   │   ├── shared/          # Reusable components (chatbot-widget, top-header)
│   │   └── layout/          # Shell layout with sidebar navigation
│   ├── Dockerfile
│   └── nginx.conf
├── Dockerfile               # Backend Docker image
└── docker-compose.yml       # Full stack orchestration
```

---

## 🔑 Default Credentials

> ⚠️ Change all default passwords before deploying to any public environment.

| Setting | Default Value |
|---|---|
| MySQL root password | `changeit-root-password` |
| MySQL app password | `changeit-app-password` |
| JWT secret | `local-dev-jwt-secret-change-me` |

---

## 📋 API Documentation

Swagger UI is available in local/dev mode at:

**http://localhost:8080/swagger-ui.html**

---

## 🧪 Health Checks

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Spring Boot health (DB, disk, liveness) |
| `GET /actuator/metrics` | Application metrics |
| `GET /api/ai/health` | Groq LLM connectivity check |

---

## 📜 License

This project was developed as part of the **Revature P3 Project** program.
