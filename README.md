# FileStorageService

A microservice for storage of media files. It uses azure blob storage and integrates with other microservices in the ecosystem.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running the Service](#running-the-service)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Database](#database)
- [Integration](#integration)
- [Monitoring and Health](#monitoring-and-health)

## Features

### File storage
- Upload, download and delete media files

### Security
- Firebase Authentication integration
- JWT token validation
- Multi-tenancy support
- Tenant isolation

### Integration
- REST API endpoints
- Azure Blob storage
- REST communication with TripService
- Kafka event streaming for audit logs
- Prometheus metrics for monitoring

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Firebase project with service account credentials
- Azure Storage account
- Kafka cluster (for event streaming)
- Docker (optional, for containerized deployment)

## Configuration

### Application Properties

The service is configured via `src/main/resources/application.properties`. Key configurations:

```properties
#Application
spring.application.name=FileStorageService
server.port=8088

#Azure Blob storage
spring.cloud.azure.storage.blob.account-name=<STORAGE_ACCOUNT_NAME>
spring.cloud.azure.storage.blob.account-key=<STORAGE_ACCOUNT_KEY>
spring.cloud.azure.storage.blob.endpoint=<YOUR-BLOB-STORAGE-ENDPOINT>

#Database
spring.datasource.url=jdbc:postgresql://localhost:5432/file_storage_db
spring.datasource.username=postgres
spring.datasource.password=postgres

#Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.audit-topic=audit-topic

#Trip service
trip-service.url=<TRIP-SERVICE-LOCATION>

#Config server (optional)
spring.config.import=optional:configserver:http://localhost:8888
```

### Kafka Configuration

Event streaming configuration for asynchronous communication:

- **`spring.kafka.bootstrap-servers`**: Kafka broker address for connecting to the Kafka cluster
- **`spring.kafka.audit-topic`**: Topic name for publishing audit events


## Running the Service

### Local Development

```bash
# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/FileStorageServiceService-1.0.0.jar
```

### Using Docker

```bash
# Build Docker image
docker build -t traversium-file-storage-service .

# Run container
docker run -p 8088:8088 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/file_storage_db \
  traversium-file-storage-service
```

### Verify Service is Running

```bash
# Health check
curl http://localhost:8088/actuator/health

# Liveness probe
curl http://localhost:8088/actuator/health/liveness

# Readiness probe
curl http://localhost:8088/actuator/health/readiness
```

## API Documentation

### REST API

Once the service is running, access the Swagger UI:

```
http://localhost:8088/swagger-ui.html
```

### Key Endpoints

- `POST /rest/v1/media` - Upload media file
- `DELETE /rest/v1/media/{filename}` - Delete media file
- `GET /rest/v1/media/{filename}` - Get media file


## Architecture

### Multi-Tenancy

The service implements schema-based multi-tenancy using the `common-multitenancy` library. Each tenant has an isolated database schema and blob storage

### Security

- **Firebase Authentication**: All requests must include a valid Firebase ID token in the Authorization header
- **Tenant Filter**: Extracts and validates tenant context from request headers
- **Principal**: User context is available via `TraversiumPrincipal` in secured endpoints

### Event-Driven Architecture

The service publishes events to Kafka:
- **Audit Events**: Track post and delete operations

## Database

### Schema Management

Database migrations are managed by Flyway. Migration scripts are located in:
```
src/main/resources/db/migration/tenant/
```

## Integration

### REST Clients

**TripService Client** (`src/main/kotlin/travesium/filestorageservice/restclient/TripServiceClient.kt`)
- Check view permissions


### Kafka Integration

**Producers:**
- Audit events

**Configuration:** See `src/main/kotlin/travesium/filestorageservice/kafka/KafkaConfig.kt`

## Monitoring and Health

### Health Checks

- **Liveness**: `/actuator/health/liveness` - Indicates if the application is running
- **Readiness**: `/actuator/health/readiness` - Indicates if the application is ready to serve traffic
- **Database**: `/actuator/health/db` - Database connectivity check

### Metrics

Prometheus metrics exposed at:
```
http://localhost:8088/actuator/prometheus
```

Key metrics:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Database connection pool metrics
- Custom business metrics

### Logging

Logs are structured in JSON format (Logstash encoder) for ELK Stack integration:
- Application logs: Log4j2
- Request/response logging
- Error tracking