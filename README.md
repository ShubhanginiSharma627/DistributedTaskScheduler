# Distributed Task Scheduler

A robust, fault-tolerant distributed task scheduling system built with Spring Boot. This system provides reliable task execution with automatic retry mechanisms, worker failure detection, and comprehensive monitoring capabilities.

## Features

### Core Functionality
- **Task Scheduling**: Schedule tasks for immediate or future execution
- **Multiple Task Types**: Support for HTTP, Shell, and Dummy task types
- **Distributed Execution**: Multiple worker instances can process tasks concurrently
- **Fault Tolerance**: Automatic recovery from worker failures and system restarts

### Reliability & Resilience
- **Automatic Retry**: Exponential backoff retry mechanism with configurable limits
- **Worker Failure Detection**: Heartbeat monitoring with automatic task reassignment
- **System Recovery**: Automatic recovery of running tasks after system restart
- **Optimistic Locking**: Prevents concurrent modification conflicts
- **Atomic Operations**: Database-level atomic task assignment

### Monitoring & Observability
- **Health Endpoints**: Comprehensive health checks and system status
- **Metrics Collection**: Execution metrics, success rates, and performance data
- **Structured Logging**: Correlation IDs and structured event logging
- **Worker Status Tracking**: Real-time worker registration and activity monitoring

### API & Management
- **REST API**: Full REST API for task management
- **Task Lifecycle Management**: Create, monitor, and cancel tasks
- **Pagination & Filtering**: Efficient task listing with filters
- **Error Handling**: Comprehensive error responses with validation

## Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.2.1** - Application framework
- **Spring Data JPA** - Data persistence
- **PostgreSQL** - Primary database
- **H2** - Testing database
- **Flyway** - Database migrations
- **Maven** - Build tool
- **JUnit 5** - Testing framework
- **jqwik** - Property-based testing

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (for production)

### Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE task_scheduler;
CREATE USER task_scheduler WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE task_scheduler TO task_scheduler;
```

2. Set environment variables (optional):
```bash
export DB_USERNAME=task_scheduler
export DB_PASSWORD=your_password
```

### Running the Application

1. Clone the repository:
```bash
git clone <repository-url>
cd distributed-task-scheduler
```

2. Build the project:
```bash
mvn clean compile
```

3. Run database migrations:
```bash
mvn flyway:migrate
```

4. Start the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

### Development Mode

For development with H2 database:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## API Documentation

### Task Management

#### Create a Task
```http
POST /tasks
Content-Type: application/json

{
  "type": "HTTP",
  "payload": {
    "url": "https://api.example.com/webhook",
    "method": "POST",
    "headers": {"Content-Type": "application/json"},
    "body": "{\"message\": \"Hello World\"}"
  },
  "scheduleAt": "2024-01-01T12:00:00Z",
  "maxRetries": 3
}
```

#### Get Task Status
```http
GET /tasks/{taskId}
```

#### List Tasks
```http
GET /tasks?status=PENDING&type=HTTP&page=0&size=20
```

#### Cancel Task
```http
DELETE /tasks/{taskId}
```

### Health & Monitoring

#### System Health
```http
GET /health
```

#### Worker Status
```http
GET /health/workers
```

#### Execution Metrics
```http
GET /health/metrics?hours=24
```

#### System Recovery
```http
POST /health/recovery
```

## Configuration

### Application Configuration

Key configuration properties in `application.yml`:

```yaml
task-scheduler:
  scheduler:
    enabled: true
    polling-interval-ms: 5000
  
  worker:
    enabled: true
    heartbeat-interval-ms: 30000
    heartbeat-timeout-ms: 60000
  
  retry:
    default-max-retries: 3
    base-delay-ms: 1000
    max-delay-ms: 300000
  
  monitoring:
    failure-detection-interval-ms: 30000
    metrics-collection-enabled: true
```

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/task_scheduler
    username: ${DB_USERNAME:task_scheduler}
    password: ${DB_PASSWORD:password}
    
  jpa:
    hibernate:
      ddl-auto: validate
```

### Environment Variables

- `DB_USERNAME` - Database username (default: task_scheduler)
- `DB_PASSWORD` - Database password (default: password)
- `SPRING_PROFILES_ACTIVE` - Active profiles (dev, test, prod)

## Task Types

### HTTP Tasks
Execute HTTP requests with configurable methods, headers, and body:
```json
{
  "type": "HTTP",
  "payload": {
    "url": "https://api.example.com/endpoint",
    "method": "POST",
    "headers": {"Authorization": "Bearer token"},
    "body": "{\"data\": \"value\"}"
  }
}
```

### Shell Tasks
Execute shell commands:
```json
{
  "type": "SHELL",
  "payload": {
    "command": "echo 'Hello World'",
    "workingDirectory": "/tmp"
  }
}
```

### Dummy Tasks
For testing and development:
```json
{
  "type": "DUMMY",
  "payload": {
    "sleepDurationMs": 5000,
    "shouldFail": false
  }
}
```

## Architecture

### Core Components

- **TaskService**: Task lifecycle management
- **SchedulerService**: Task scheduling and assignment
- **WorkerService**: Worker registration and heartbeat management
- **TaskExecutionService**: Task execution coordination
- **RetryManager**: Retry logic and failure handling
- **MonitoringService**: Metrics collection and health monitoring
- **SystemRecoveryService**: System recovery and consistency checks

### Database Schema

- **tasks**: Task definitions and status
- **task_executions**: Execution history and results
- **worker_heartbeats**: Worker registration and activity

### Task Lifecycle

1. **Created** → Task is created with PENDING status
2. **Scheduled** → Scheduler assigns task to available worker
3. **Running** → Worker executes the task
4. **Completed** → Task finishes with SUCCESS or FAILED status
5. **Retry** → Failed tasks are rescheduled based on retry configuration

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TaskServiceTest

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

### Test Categories

- **Unit Tests**: Individual component testing
- **Integration Tests**: Database and service integration
- **Property-Based Tests**: Universal property validation using jqwik

### Property-Based Testing

The project uses property-based testing to validate system correctness:

```java
@RepeatedTest(100)
void taskCreationMaintainsIntegrity() {
    // Property: For any valid task definition, 
    // creating the task should result in a stored task 
    // with unique ID, correct data, and PENDING status
}
```

## Monitoring & Operations

### Health Checks

- **Liveness**: `/health/live` - Application is running
- **Readiness**: `/health/ready` - Application is ready to serve traffic
- **Health**: `/health` - Comprehensive system health

### Metrics

- Task execution counts and success rates
- Worker activity and failure detection
- System uptime and performance metrics
- Database connection pool status

### Logging

Structured logging with correlation IDs for request tracing:
- Task lifecycle events
- Worker registration and heartbeat events
- System recovery operations
- Performance metrics


### Building

```bash
# Clean build
mvn clean compile

# Package application
mvn package

# Skip tests
mvn package -DskipTests

# Build Docker image (if Dockerfile exists)
docker build -t distributed-task-scheduler .
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

