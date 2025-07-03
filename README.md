# TestTask - User Management API

REST API для управления пользователями и переводами денег, реализованное на Spring Boot.

## Технологический стек

- **Java 22**
- **Spring Boot 3.5.3**
- **PostgreSQL** (через Docker)
- **Liquibase** для миграций БД
- **JWT** для аутентификации
- **EhCache** для кэширования
- **Swagger/OpenAPI** для документации API
- **Maven** для сборки
- **Testcontainers** для интеграционных тестов

## Структура БД

### Таблица `users`
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `name` (VARCHAR(500), NOT NULL)
- `date_of_birth` (DATE, NOT NULL)
- `password` (VARCHAR(500), NOT NULL)

### Таблица `account`
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `user_id` (BIGINT, FK -> users.id, UNIQUE)
- `balance` (DECIMAL(19,2), NOT NULL)
- `initial_balance` (DECIMAL(19,2), NOT NULL)

### Таблица `email_data`
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `user_id` (BIGINT, FK -> users.id)
- `email` (VARCHAR(200), NOT NULL, UNIQUE)

### Таблица `phone_data`
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `user_id` (BIGINT, FK -> users.id)
- `phone` (VARCHAR(13), NOT NULL, UNIQUE)

## Запуск проекта

### Предварительные требования
- Java 22+ (для локальной разработки)
- Docker и Docker Compose
- Maven (для локальной разработки)

### Способ 1: Запуск с Docker Compose (рекомендуется)
```bash
# Запуск всего стека (БД + приложение)
docker-compose up --build

# Запуск в фоне
docker-compose up --build -d

# Остановка
docker-compose down
```

### Способ 2: Локальная разработка
```bash
# 1. Запуск БД
docker-compose up -d postgres

# 2. Запуск приложения с тестовым профилем (H2 БД)
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 3. Запуск с PostgreSQL
./mvnw spring-boot:run
```

### Способ 3: Сборка и запуск JAR
```bash
# Сборка
./mvnw clean package -DskipTests

# Запуск с тестовым профилем
java -jar target/TestTask-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

# Запуск с PostgreSQL (требует запущенной БД)
java -jar target/TestTask-0.0.1-SNAPSHOT.jar
```

### 4. Доступ к API
- **API Base URL**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Documentation**: http://localhost:8080/v3/api-docs

## API Endpoints

### Аутентификация
- `POST /api/auth/login` - Аутентификация пользователя

### Управление пользователями
- `POST /api/users` - Создание пользователя (не требует аутентификации)
- `GET /api/users/{id}` - Получение пользователя по ID
- `PUT /api/users/{id}` - Обновление пользователя
- `GET /api/users/search` - Поиск пользователей с фильтрацией

### Операции с аккаунтом
- `POST /api/accounts/transfer` - Перевод денег между пользователями

## Основные функции

### 1. Управление пользователями
- Создание пользователя с начальным балансом
- CRUD операции для пользователей
- Поиск с фильтрацией по:
  - Дате рождения (больше указанной даты)
  - Телефону (частичное совпадение)
  - Имени (частичное совпадение, без учета регистра)
  - Email (частичное совпадение)

### 2. JWT Аутентификация
- Аутентификация по email или телефону + пароль
- JWT токен содержит только USER_ID в claim
- Токен действителен 24 часа

### 3. Операции с балансом
- Автоматическое увеличение баланса на 10% каждые 30 секунд
- Максимальный баланс - 207% от начального депозита
- Переводы между пользователями с валидацией

### 4. Кэширование
- Кэширование на уровне API и DAO
- Использование EhCache
- Автоматическая инвалидация кэша при обновлениях

### 5. Валидация
- Валидация входных данных на уровне API
- Проверка уникальности email и телефонов
- Проверка формата телефона (11 цифр)
- Валидация email адресов

## Примеры использования

### Создание пользователя
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "dateOfBirth": "1990-01-01",
    "password": "password123",
    "initialBalance": 1000.00,
    "emails": [{"email": "john@example.com"}],
    "phones": [{"phone": "79201234567"}]
  }'
```

### Аутентификация
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "john@example.com",
    "password": "password123"
  }'
```

### Поиск пользователей
```bash
curl -X GET "http://localhost:8080/api/users/search?name=john&page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Перевод денег
```bash
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "transferTo": 2,
    "amount": 100.00
  }'
```

## Тестирование

### Запуск unit тестов
```bash
./mvnw test
```

### Запуск интеграционных тестов
```bash
./mvnw verify
```

## Логирование

Приложение использует SLF4J с Logback для логирования:
- **DEBUG** - детальная информация о выполнении операций
- **INFO** - основные события приложения
- **ERROR** - ошибки и исключения

## Monitoring

Доступен через Spring Boot Actuator (если добавлен):
- Health checks
- Metrics
- Application info

## Безопасность

- Пароли хэшируются с использованием BCrypt
- JWT токены подписываются секретным ключом
- Статeless аутентификация
- CORS настройка при необходимости

## Производительность

- Кэширование часто используемых данных
- Пагинация для списков
- Оптимизированные запросы к БД
- Асинхронная обработка увеличения баланса 