## 🔧 Setup Instructions

### Prerequisites
- Java 22 or higher
- Maven 3.8+
- Docker and Docker Compose (for database services)

### Start Database Services
```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379

### Run the Application

`
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dliquibase.secureParsing=false"
`

The application will start on `http://localhost:8080`

### Access Swagger Documentation
Visit `http://localhost:8080/swagger-ui/index.html` for interactive API documentation.

## 🧪 Testing

### Run All Tests
```bash
mvn test
```