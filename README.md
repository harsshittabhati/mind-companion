# 🧠 Mind Companion

> An AI-powered mental wellness companion built with Spring Boot 3, featuring real-time chat, mood tracking, journaling, crisis detection, and gamification.

---

## Table of Contents

- [Overview](#overview)
- [Why This Project?](#why-this-project)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Security](#security)
- [PWA Support](#pwa-support)
- [Screenshots](#screenshots)

---

## Overview

Mind Companion is a full-stack mental health web application that provides users with an AI companion named **Serenity** — powered by Groq's `llama-3.3-70b-versatile` model. Users can chat with Serenity in real time, track their daily mood, write journal entries, and monitor their wellness through an analytics dashboard.

The application includes a crisis detection system that automatically detects distress signals in chat messages, saves emergency alerts to the database, and sends HTML crisis emails to a designated emergency contact via Gmail SMTP.

---

## Why This Project?

Mental health is one of the most underserved areas in technology. Millions of people struggle silently every day — not because they don't want help, but because help isn't always accessible. Therapy is expensive, stigma is real, and reaching out to someone at 2 AM when you're at your lowest isn't always possible.

Mind Companion was built around a simple idea: **what if support was always available?**

Not as a replacement for professional therapy — Serenity is clear about that — but as a safe space where someone can express how they're feeling without judgment, track their emotional patterns over time, and know that if they're ever in crisis, someone will be notified.

A few specific problems this project addresses:

**Accessibility.** Most mental health apps are paywalled or require professional involvement to get started. Mind Companion requires nothing except signing up.

**Privacy.** Conversations about mental health are deeply personal. Every message is encrypted with AES-256 before being stored. Users can enable confidential mode so nothing is saved at all. Data retention is configurable and fully deletable under GDPR.

**Crisis response.** Existing chat apps don't act on what you say. Mind Companion does — if crisis language is detected, an emergency contact is notified immediately, without the user having to ask for help or even be aware it happened.

**Continuity.** A journal, a mood log, a streak counter, badges — these aren't just features. They're ways of showing someone that their progress matters and that showing up consistently, even when it's hard, is worth something.

This project was also a deliberate technical challenge: building a production-grade Spring Boot application with real security (JWT, AES-256, WebSocket auth), real-time communication, AI integration, scheduled jobs, PDF generation, and a PWA — all from scratch, without shortcuts.

The result is an application that takes both the human problem and the engineering problem seriously.

---

## Features

### 🤖 AI Chat with Serenity
- Real-time WebSocket chat (STOMP + SockJS)
- REST fallback endpoint for HTTP-based clients
- Groq API integration (`llama-3.3-70b-versatile`)
- AES-256 CBC encryption for all stored messages
- Conversation history context (last 20 messages sent to AI)
- Confidential mode — messages processed but never written to disk

### 🚨 Crisis Detection & Emergency Alerts
- Keyword-based crisis detection on every message
- Crisis keywords: `suicide`, `kill myself`, `want to die`, `self harm`, etc.
- Automatic `EmergencyAlert` saved to database on detection
- HTML crisis email sent to emergency contact via Gmail SMTP
- Crisis alert marked as email-sent after successful delivery
- WebSocket push to `/queue/crisis` for immediate frontend notification

### 😊 Mood Tracking
- Daily mood check-in (1–10 scale with mood level enum)
- Weekly and full history endpoints
- Average mood calculation over 7 and 30 days
- Mood timeline data for Chart.js rendering

### 📓 Journal
- Daily journal entries with mood tags
- AI-generated daily writing prompt
- Full journal history

### 📊 Analytics Dashboard
- Sentiment breakdown (POSITIVE, NEGATIVE, NEUTRAL, CRISIS)
- Mood timeline chart (last 30 days)
- Session stats: total messages, crisis count, positive rate, average intensity
- PDF wellness report download (Apache PDFBox 3.0.2)

### 🏆 Gamification
- XP points awarded for chat, mood check-ins, and journal entries
- Level system with titles (Newcomer → Serenity Master)
- Activity streak tracking (current and longest)
- Badge system with automatic awarding

### 🔒 GDPR & Privacy
- Configurable data retention policy per user (default 365 days)
- Scheduled auto-delete job runs daily at 2:00 AM
- `DELETE /api/user/data` — erase all user data (keep account)
- `DELETE /api/user/account` — full account deletion
- Confidential mode: messages never persisted to database

### 📱 PWA
- `manifest.json` with name, icons, theme color, and shortcuts
- Service worker with network-first caching strategy
- Offline fallback page
- "Add to Home Screen" support on mobile

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.14 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Real-time | Spring WebSocket (STOMP + SockJS) |
| Database | MySQL 8 + Spring Data JPA + Hibernate 6 |
| AI | Groq API via OkHttp 4.12.0 |
| Email | JavaMail (Gmail SMTP) |
| PDF | Apache PDFBox 3.0.2 |
| Frontend | Thymeleaf + Bootstrap 5.3.3 + Chart.js 4.4.0 |
| Encryption | AES-256 CBC (`EncryptionUtil`) |
| Build | Maven + Lombok 1.18.32 |
| CI/CD | GitHub Actions |
| Container | Docker (eclipse-temurin:21-jre-alpine) |

---

## Project Structure

```
src/main/java/com/mindcompanion/
├── config/
│   ├── AppConfig.java
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   └── WebSocketAuthInterceptor.java
├── controller/
│   ├── AuthController.java
│   ├── ChatController.java
│   ├── MoodController.java
│   ├── JournalController.java
│   ├── EmergencyAlertController.java
│   ├── AnalyticsController.java
│   ├── UserDataController.java
│   └── FrontendController.java
├── service/
│   ├── ChatService.java
│   ├── MoodService.java
│   ├── JournalService.java
│   ├── EmergencyAlertService.java
│   ├── EmailService.java
│   ├── AnalyticsService.java
│   ├── GamificationService.java
│   └── PdfReportService.java
├── model/
│   ├── User.java
│   ├── ChatMessage.java
│   ├── MoodEntry.java
│   ├── JournalEntry.java
│   ├── EmergencyAlert.java
│   ├── Badge.java
│   ├── UserBadge.java
│   └── enums/
│       ├── Role.java          (PATIENT, THERAPIST, ADMIN)
│       ├── SentimentType.java (POSITIVE, NEGATIVE, NEUTRAL, CRISIS)
│       └── MoodLevel.java
├── repository/          (7 JPA repositories)
├── security/
│   ├── UserDetailsImpl.java
│   ├── UserDetailsServiceImpl.java
│   └── jwt/
│       ├── JwtUtils.java
│       └── JwtAuthFilter.java
├── scheduler/
│   └── DataRetentionScheduler.java
└── util/
    └── EncryptionUtil.java

src/main/resources/
├── application.properties
├── static/
│   ├── manifest.json
│   ├── service-worker.js
│   ├── offline.html
│   └── icons/
│       ├── icon-192.png
│       └── icon-512.png
└── templates/
    ├── layout/base.html
    ├── auth/login.html
    ├── auth/register.html
    ├── dashboard.html
    ├── chat.html
    ├── mood.html
    ├── journal.html
    └── profile.html
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- MySQL 8
- A [Groq API key](https://console.groq.com)
- A Gmail account with an [App Password](https://myaccount.google.com/apppasswords)

### 1. Clone the repository

```bash
git clone https://github.com/harsshittabhati/mind-companion.git
cd mind-companion
```

### 2. Create the MySQL database

```sql
CREATE DATABASE mind_companion_db;
CREATE USER 'mindapp'@'localhost' IDENTIFIED BY 'MindApp@2024';
GRANT ALL PRIVILEGES ON mind_companion_db.* TO 'mindapp'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Set environment variables

The app reads secrets from environment variables. Set these before running:

```bash
# Windows PowerShell
$env:GROQ_API_KEY="gsk_your_key_here"
$env:GMAIL_USERNAME="your@gmail.com"
$env:GMAIL_APP_PASSWORD="your_app_password"
```

Or configure them in your IDE's run configuration.

### 4. Run the application

```bash
mvn spring-boot:run
```

The app starts at `http://localhost:8080`.

### 5. Docker

```bash
# Build
mvn clean package -DskipTests
docker build -t mind-companion .

# Run
docker run -p 8080:8080 \
  -e GROQ_API_KEY=your_key \
  -e GMAIL_USERNAME=your@gmail.com \
  -e GMAIL_APP_PASSWORD=your_password \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/mind_companion_db \
  mind-companion
```

---

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `GROQ_API_KEY` | Groq API key for AI responses | Yes |
| `GMAIL_USERNAME` | Gmail address for sending crisis emails | Yes |
| `GMAIL_APP_PASSWORD` | Gmail App Password (not your account password) | Yes |

These are injected via `${GROQ_API_KEY:default}` placeholders in `application.properties`. Never commit real keys.

---

## API Reference

All endpoints except `/api/auth/**` require a Bearer JWT token in the `Authorization` header.

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT token |

**Register body:**
```json
{
  "username": "harshita",
  "email": "harshita@example.com",
  "password": "secret123",
  "fullName": "Harshita Bhati"
}
```

**Login response:**
```json
{
  "token": "eyJhbGci...",
  "id": 1,
  "username": "harshita",
  "email": "harshita@example.com",
  "role": "ROLE_PATIENT"
}
```

### Chat

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/chat/send` | Send a message to Serenity (REST) |
| `GET` | `/api/chat/history` | Get full decrypted chat history |
| `DELETE` | `/api/chat/history` | Delete all chat messages |
| `WS` | `/ws` → `/app/chat.send` | WebSocket chat endpoint |

**Send body:**
```json
{
  "message": "I'm feeling anxious today",
  "sessionId": "session-abc123"
}
```

**Response:**
```json
{
  "message": "I hear you — anxiety can feel really overwhelming...",
  "senderType": "BOT",
  "sentiment": "NEGATIVE",
  "intensityScore": 0.3,
  "isCrisis": false,
  "sessionId": "session-abc123",
  "createdAt": "2026-06-06T10:30:00"
}
```

### Mood

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/mood/checkin` | Submit today's mood |
| `GET` | `/api/mood/today` | Get today's mood entry |
| `GET` | `/api/mood/history` | Get full mood history |
| `GET` | `/api/mood/weekly` | Get last 7 days |

### Journal

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/journal/entry` | Save a journal entry |
| `GET` | `/api/journal/today` | Get today's entry |
| `GET` | `/api/journal/history` | Get full journal history |
| `GET` | `/api/journal/prompt` | Get today's AI writing prompt |

### Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/analytics/dashboard` | Full dashboard data |
| `GET` | `/api/analytics/sentiment` | Sentiment breakdown counts |
| `GET` | `/api/analytics/mood?days=7` | Average mood over N days |
| `GET` | `/api/analytics/mood/timeline?days=30` | Daily mood scores for chart |
| `GET` | `/api/analytics/stats` | Session statistics |
| `GET` | `/api/analytics/gamification` | XP, level, streak, badges |
| `GET` | `/api/analytics/badges` | Earned badges list |
| `GET` | `/api/analytics/streak` | Current activity streak |
| `GET` | `/api/analytics/report/pdf` | Download PDF wellness report |

### Emergency Alerts

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/alerts/my` | Get current user's alerts |
| `GET` | `/api/alerts/unresolved` | All unresolved alerts (admin) |
| `PUT` | `/api/alerts/{id}/resolve` | Resolve an alert |
| `GET` | `/api/alerts/count/unresolved` | Count of unresolved alerts |

### Privacy & GDPR

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/user/privacy` | Get privacy settings |
| `PUT` | `/api/user/privacy` | Update retention policy / confidential mode |
| `DELETE` | `/api/user/data` | Erase all data (keep account) |
| `DELETE` | `/api/user/account` | Delete account and all data |

---

## Database Schema

| Table | Description |
|-------|-------------|
| `users` | User accounts with gamification fields and privacy settings |
| `chat_messages` | AES-256 encrypted chat messages with sentiment and crisis flag |
| `mood_entries` | Daily mood check-ins with score and notes |
| `journal_entries` | Journal entries with mood tags |
| `emergency_alerts` | Crisis alerts with trigger keyword and resolution status |
| `badges` | Badge definitions (seeded via SQL) |
| `user_badges` | Many-to-many between users and badges with earned timestamp |

Tables are auto-created by Hibernate (`ddl-auto=update`). No migration scripts needed for initial setup.

---

## Security

- **JWT authentication** — stateless, token expires in 24 hours
- **BCrypt** password hashing
- **AES-256 CBC** encryption for all chat message content at rest
- **Spring Security** filter chain — all `/api/**` routes require authentication except `/api/auth/**`
- **WebSocket authentication** via `WebSocketAuthInterceptor` — JWT validated on STOMP CONNECT
- Secrets injected via environment variables — never hardcoded
- `.env` excluded from git via `.gitignore`

---

## PWA Support

Mind Companion is installable as a Progressive Web App:

- `manifest.json` — name, icons (192×192, 512×512), theme color `#6c63ff`, shortcuts to Chat and Mood
- `service-worker.js` — network-first strategy, offline fallback to `offline.html`
- API calls (`/api/`, `/ws`) are excluded from service worker caching
- Apple touch icon and `apple-mobile-web-app-capable` meta tags for iOS

To install: open the app in Chrome → address bar install button or browser menu → "Add to Home Screen".

---

## CI/CD

GitHub Actions workflow (`.github/workflows/deploy.yml`) runs on every push to `main`:

1. Checkout code
2. Set up Java 21 (Temurin)
3. Build with Maven (`-DskipTests`)
4. Upload JAR as build artifact (60.7 MB)

The built JAR and `Dockerfile` are ready for deployment to Railway, Render, or any Docker-compatible platform.

---

## Crisis Resources

Serenity always shares these resources when crisis language is detected:

- **iCall (India):** 9152987821
- **Vandrevala Foundation:** 1860-2662-345
- **AASRA:** 9820466627

---

## License

This project is for educational and portfolio purposes.
