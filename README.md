# Banking Application API

A comprehensive Spring Boot banking application with user management, authentication, money transfers, and automated balance increases.

## 🚀 Technology Stack

- **Java 22** - Latest Java features and performance improvements
- **Spring Boot 3.2.6** - Modern Spring framework with auto-configuration
- **PostgreSQL** - Robust relational database for data persistence
- **Redis** - High-performance caching layer
- **Liquibase** - Database version control and migration management
- **JWT** - Secure token-based authentication
- **Spring Security** - Comprehensive security framework
- **Swagger/OpenAPI** - Interactive API documentation
- **Testcontainers** - Container-based integration testing
- **Maven** - Dependency management and build tool

## 📋 Features

### Core Banking Features
- **User Management**: Create, retrieve, update, and search users
- **Account Management**: Automatic account creation with initial balance
- **Money Transfers**: Secure peer-to-peer money transfers with validation
- **Authentication**: JWT-based authentication with email/phone login
- **Balance Growth**: Automated 10% balance increase every 30 seconds (max 207% of initial)

### Technical Features
- **Optimistic Locking**: Prevents concurrent modification issues
- **Caching**: Redis-based caching with 5-minute TTL
- **Retry Mechanism**: Automatic retry for optimistic locking failures
- **Input Validation**: Comprehensive validation with custom error handling
- **Security**: JWT authentication with role-based access control
- **Documentation**: Auto-generated Swagger API documentation
- **Testing**: Comprehensive unit and integration tests

## 🗄️ Database Schema

### Users Table
- `id` - Primary key
- `name` - User's full name
- `date_of_birth` - Birth date
- `password` - Encrypted password

### Account Table
- `id` - Primary key
- `user_id` - Foreign key to users
- `balance` - Current account balance
- `initial_balance` - Initial deposit amount
- `version` - Optimistic locking version

### Email_Data Table
- `id` - Primary key
- `user_id` - Foreign key to users
- `email` - Email address (unique)

### Phone_Data Table
- `id` - Primary key
- `user_id` - Foreign key to users
- `phone` - Phone number (unique, format: 79207865432)

## 🔧 Setup Instructions

### Prerequisites
- Java 22 or higher
- Maven 3.8+
- Docker and Docker Compose (for database services)

### 1. Clone the Repository
```bash
git clone <repository-url>
cd TestTask2
```

### 2. Start Database Services
```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379

### 3. Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access Swagger Documentation
Visit `http://localhost:8080/swagger-ui/index.html` for interactive API documentation.

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Test Coverage
- **Unit Tests**: Service layer business logic
- **Integration Tests**: Complete API workflows
- **Concurrent Tests**: Race condition scenarios
- **Cache Tests**: Redis caching behavior
- **Security Tests**: Authentication and authorization

## 🔐 API Endpoints

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

## ⚙️ Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret

# JWT
app.jwt.secret=mySecretKey...
app.jwt.expiration=86400000

# Cache
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Custom
app.cache.ttl-seconds=300
app.async.corePoolSize=5
```

### Environment Profiles
- **Default**: Development configuration
- **Production**: Production-ready settings (use `application-production.properties`)

## 🏗️ Architecture

### Package Structure
```
src/main/java/com/example/testtask/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── exception/       # Exception handling
├── repository/      # Data access layer
├── service/         # Business logic
└── TestTaskApplication.java
```

### Key Components
- **SecurityConfig**: JWT authentication and authorization
- **CacheConfig**: Redis caching configuration
- **ScheduledTasksConfig**: Automated balance increases
- **RetryConfig**: Optimistic locking retry mechanism
- **OpenApiConfig**: Swagger documentation setup

## 🔒 Security

### Authentication
- JWT tokens with configurable expiration
- Email or phone number login
- Secure password hashing

### Authorization
- Protected endpoints require valid JWT tokens
- Users can only update their own profiles
- Transfer operations require authentication

### Data Security
- Optimistic locking prevents concurrent modifications
- Input validation and sanitization
- Secure password storage

## 🎯 Business Rules

### User Creation
- Emails and phones must be unique across the system
- Initial balance must be positive
- Date of birth is required

### Money Transfers
- Users cannot transfer money to themselves
- Transfer amount must be positive
- Sender must have sufficient balance
- Optimistic locking prevents race conditions

### Balance Increases
- Automated 10% increase every 30 seconds
- Maximum balance is 207% of initial balance
- Runs in background scheduled task

## 📊 Performance Features

### Caching Strategy
- User data cached for 5 minutes
- Cache invalidation on updates
- Redis-based distributed caching

### Optimistic Locking
- Prevents lost updates in concurrent scenarios
- Automatic retry mechanism
- Version-based conflict resolution

### Database Optimization
- Proper indexing on search fields
- Efficient query patterns
- Connection pooling with HikariCP

## 🚀 Deployment

### Docker Support
The application includes Docker Compose configuration for easy deployment:
- PostgreSQL database
- Redis cache
- Application container (when Dockerfile is added)

### Production Considerations
- Use environment-specific configuration
- Configure proper logging levels
- Set up monitoring and health checks
- Use production-grade database and cache instances

## 📚 Testing Strategy

### Test Types
- **Unit Tests**: Individual component testing
- **Integration Tests**: End-to-end API testing
- **Concurrent Tests**: Race condition scenarios
- **Cache Tests**: Caching behavior verification

### Test Environment
- Testcontainers for realistic database testing
- In-memory Redis for cache testing
- Separate test configuration
- MockMvc for API testing

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Run the test suite
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For questions or issues, please create an issue in the repository or contact the development team.

---

**Note**: This is a demonstration banking application. For production use, additional security measures, compliance checks, and regulatory requirements should be implemented. 