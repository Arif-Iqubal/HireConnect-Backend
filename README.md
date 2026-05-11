# HireConnect Backend

HireConnect Backend is a Spring Boot microservices platform for a recruitment application. It supports authentication, candidate and recruiter profiles, jobs, applications, interviews, notifications, subscriptions, analytics, API gateway routing, and Eureka-based service discovery.

## Services

- `Api-Gateway` - routes frontend requests to backend services and propagates JWT user context.
- `Service-Registry` - Eureka service discovery server.
- `Auth-Service` - registration, login, JWT, OAuth, and user identity APIs.
- `Profile-Service` - candidate and recruiter profile management.
- `Job-Service` - job posting, browsing, search, and recruiter job management.
- `Application-Service` - job applications, application status updates, withdrawals, and recruiter messages.
- `Interview-Service` - interview scheduling, confirmation, rescheduling, cancellation, and completion.
- `Notification-Service` - in-app and email notifications from platform events.
- `Subscription-Service` - recruiter plans, orders, verification, subscriptions, and invoices.
- `Analytics-Service` - platform metrics and analytics event processing.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Security
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Data JPA and Hibernate
- MySQL and H2 for tests
- RabbitMQ for asynchronous events
- Maven
- Lombok and MapStruct
- Swagger/OpenAPI
- Docker and Docker Compose
- JUnit 5, Mockito, JaCoCo, and SonarQube

## Communication

- The Angular frontend calls backend APIs through `Api-Gateway`.
- Services register with `Service-Registry`.
- Synchronous service calls use REST clients such as OpenFeign or `RestTemplate`.
- Asynchronous events flow through RabbitMQ, for example:
  - `APPLICATION_SUBMITTED`
  - `APPLICATION_STATUS_CHANGED`
  - `INTERVIEW_SCHEDULED`
  - `INTERVIEW_RESCHEDULED`
  - `SUBSCRIPTION_PURCHASED`

## Local Setup

Prerequisites:

- Java 17
- Maven
- Docker Desktop
- MySQL if running services outside Docker

Run the backend stack:

```bash
docker compose up -d --build
```

Run tests for one service:

```bash
cd Application-Service
mvn test
```

Generate a JaCoCo report:

```bash
mvn test
```

The report is created under:

```text
target/site/jacoco/index.html
```

## API Documentation

Each service exposes Swagger/OpenAPI endpoints when running. API documentation is also aggregated through the API Gateway where configured.

## Repository Notes

Generated files, local secrets, build output, coverage reports, IDE metadata, logs, and uploads are ignored by `.gitignore`.
