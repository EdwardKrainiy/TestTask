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
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access Swagger Documentation
Visit `http://localhost:8080/swagger-ui/index.html` for interactive API documentation.

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - User authentication

### User Management
- `POST /api/users` - Create new user
- `GET /api/users/{userId}` - Get user by ID (requires auth)
- `PUT /api/users/{userId}` - Update user (requires auth, own profile only)
- `GET /api/users/search` - Search users with filters (requires auth)

### Account Management
- `POST /api/accounts/transfer` - Transfer money between users (requires auth)

## 📝 API Usage Examples

### 1. Create User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "dateOfBirth": "1990-01-01",
    "password": "password123",
    "emails": ["john.doe@example.com"],
    "phones": ["79201234567"],
    "initialBalance": 1000.00
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "john.doe@example.com",
    "password": "password123"
  }'
```

### 3. Transfer Money
```bash
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "transferTo": 2,
    "amount": 100.00
  }'
```

### 4. Search Users
```bash
curl -X GET "http://localhost:8080/api/users/search?name=John&page=0&size=10" \
  -H "Authorization: Bearer <jwt-token>"
```