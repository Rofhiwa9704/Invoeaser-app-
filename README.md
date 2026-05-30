# 🧾 Invoeaser — Intelligent Invoicing Automation Platform

Invoeaser is a modern, multi-tenant billing automation platform designed for freelancers, contractors, and growing businesses who want to simplify invoicing, automate billing cycles, and maintain full control over invoice customization.

Built with **Java (Spring Boot)** and **Next.js** (front-end coming soon), Invoeaser emphasizes ease of use, intelligent defaults, and deep flexibility through customizable templates and branding.

---

## 🚀 Key Features

### ✅ MVP-Ready Core
- **Invoice Generation**
  - Create invoices manually or from predefined billing schedules
  - Support for HOURLY, DAILY, FIXED rate types
  - Custom due dates or preset due date strategies (e.g. Net 7, Net 30)

- **PDF Invoice Rendering**
  - Beautiful, minimal invoice templates using Thymeleaf
  - Dynamic table rendering for TRADITIONAL, HOURS_LOG, FLAT_RATE, etc.
  - Custom invoice templates via JSON-defined columns
  - Embedded branding and logos

- **Email Dispatch Engine**
  - Immediate or scheduled invoice emailing
  - Uses RabbitMQ + Spring Scheduler for async resilience
  - Built-in retry mechanism with audit logging

- **Audit & Monitoring**
  - Email audit logs with CSV export
  - Retry failed invoice sends (manual + scheduled)
  - Role-based access control via JWT

- **Tenant-Aware Architecture**
  - Multi-tenant support by design
  - Entity separation by tenant ID
  - Secure role enforcement (admin, user)

---

## 📦 Tech Stack

| Layer          | Technology                     |
|----------------|-------------------------------|
| Backend        | Java 17, Spring Boot 3         |
| Messaging      | RabbitMQ                       |
| PDF Rendering  | Thymeleaf                      |
| Object Storage | DigitalOcean Spaces (S3 API)   |
| Database       | PostgreSQL + Liquibase         |
| Auth           | JWT-based (OAuth2 optional)    |
| Frontend       | **Next.js (planned)**          |

---

## 🧩 Architecture Overview



    +------------------+
          |  BillingSchedule |
          +------------------+
                   |
      Schedules Invoices on 10th/15th/20th
                   |
                   v
           +--------------+
           |   Invoice    |
           +--------------+
           | - items      |
           | - templateId |
           | - supplier   |
           +--------------+
                   |
                   v
          +-----------------+
          | Thymeleaf PDF   |
          +-----------------+
                   |
           +---------------------+
           | RabbitMQ (async)    |
           +---------------------+
                   |
           +---------------------+
           | MailSender Service  |
           +---------------------+


---

## 🧪 Getting Started

### 🚀 Quick Setup (TL;DR)

```bash
# Clone and navigate
git clone https://github.com/Kingstechco/invoeaser-app.git
cd invoeaser-app

# Start infrastructure services
./start-services.sh

# Run the application
./mvnw spring-boot:run

# Access at http://localhost:8080
```

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Maven 3.6+ (or use the included Maven wrapper)

### 1. Clone the Repository

```bash
git clone https://github.com/Kingstechco/invoeaser-app.git
cd invoeaser-app
```

### 2. Start Infrastructure Services with Docker

Start PostgreSQL, Redis, and RabbitMQ using Docker:

```bash
# Start PostgreSQL database
docker run -d \
  --name postgres-invoeaser \
  -e POSTGRES_DB=invoeaser_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:13

# Start Redis
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis

# Start RabbitMQ with Management UI
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

### 3. Verify Services are Running

```bash
# Check all containers are running
docker ps

# You should see three containers:
# - postgres-invoeaser (PostgreSQL on port 5432)
# - redis (Redis on port 6379)  
# - rabbitmq (RabbitMQ on ports 5672, 15672)
```

### 4. Run the Application

Using Maven wrapper (recommended):

```bash
./mvnw spring-boot:run
```

Or using installed Maven:

```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

### 5. Access Services

#### Application
- **Main Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator Health**: http://localhost:8080/actuator/health

#### Infrastructure Services
- **PostgreSQL Database**: localhost:5432
  - Database: `invoeaser_db`
  - Username: `postgres` 
  - Password: `postgres`

- **RabbitMQ Management**: http://localhost:15672
  - Username: `guest`
  - Password: `guest`

- **Redis**: localhost:6379 (no password by default)

## 🐳 Docker Compose Setup (Recommended)

For easier management, use the included `docker-compose.yml` file:

```bash
# Start all infrastructure services
docker-compose up -d

# Or use the convenient startup script
./start-services.sh
```

This will start:
- **PostgreSQL** with persistent data storage
- **Redis** with data persistence enabled
- **RabbitMQ** with management UI
- All services networked together

### Managing Services

```bash
# Start services
./start-services.sh

# Stop services
./stop-services.sh

# View logs
docker-compose logs -f

# Stop and remove all data (CAUTION!)
docker-compose down -v
```

## 🔧 Database Setup

The application uses **Hibernate DDL auto-generation** (`ddl-auto: update`), so database tables will be created automatically on first startup.

### Manual Database Connection

To connect to the PostgreSQL database directly:

```bash
# Using Docker exec
docker exec -it postgres-invoeaser psql -U postgres -d invoeaser_db

# List all tables
\dt

# Describe a specific table (e.g., invoices)
\d invoices
```

## ⚙️ Configuration

### Database Configuration (application.yaml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/invoeaser_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
```

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 6000
```

### RabbitMQ Configuration

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## 🧪 Testing

Run tests using Maven:

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=InvoiceServiceTest
```

## 🚨 Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 5432, 6379, 5672, 8080, and 15672 are not in use
2. **Java version**: Verify you're using Java 17+
3. **Docker issues**: Ensure Docker daemon is running

### Check Service Health

```bash
# Check if application is responding
curl http://localhost:8080/actuator/health

# Check RabbitMQ Management UI
curl http://localhost:15672

# Check database connection
docker exec postgres-invoeaser pg_isready -U postgres

# Check Redis connection  
docker exec redis redis-cli ping
```

### Logs

For more detailed logging, add to application.yaml:

```yaml
logging:
  level:
    co.za.kingstechco: DEBUG
    org.hibernate.SQL: DEBUG
```

## 💼 Business Use Cases

 - 🔁 Recurring invoice automation

 - 🧑‍💼 Multi-recipient/supplier support per user

 - 🪄 User-defined branded templates

 - 📨 Scheduled & tracked invoice emailing

 - 📈 Audit-ready logs for compliance

## 🧱 Folder Structure

src
├── controller
├── dto
├── entity
├── mapper
├── mail
├── pdf
├── repository
├── scheduler
└── service


## 🧰 Development Tips

Run migrations via Liquibase for consistent schema changes

 - Use @Loggable for tracking core service calls

 - Extend InvoiceTemplateRenderer for new template types

 - Templates are in src/main/resources/templates/invoice-pdf.html

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing
Contributions, bug reports, and feature suggestions are welcome! Just fork and PR or open an issue.

## ✨ Shaping the Future

Invoeaser is more than just invoicing. It's built to evolve into a full freelancer finance assistant:

 - Invoice AI assistant

 - Time tracking

 - Expense matching

 - Payment reconciliation

Be part of the journey.

