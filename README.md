# Система управления банковскими картами

Система управления банковскими картами с JWT аутентификацией, переводами между картами и ролевой моделью (USER/ADMIN).

---

## 📚 Документация API

После запуска приложения документация доступна по адресам:

| Тип | URL |
|-----|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | `/docs/openapi.yaml` (файл в проекте) |

### Как тестировать через Swagger UI:

1. Открой в браузере: http://localhost:8080/swagger-ui.html
2. Нажми кнопку **"Authorize"** (справа вверху)
3. Введи JWT токен в формате: `Bearer your-token-here`
4. Теперь можно тестировать любые защищенные эндпоинты

---

### Требования

- Docker & Docker Compose
- Java 17 (для локального запуска)
- Maven

### Запуск через Docker

```bash
# Клонировать репозиторий
git clone https://github.com/YOUR_USERNAME/bank-card-system.git
cd bank-card-system

# Запустить PostgreSQL и приложение
docker-compose up -d

# Проверить логи
docker-compose logs -f app

# Остановить все контейнеры
docker-compose down

👥 Тестовые пользователи
Роль	Username	Password
Пользователь (USER)	user	user123
Администратор (ADMIN)	admin	admin123

🔑 Эндпоинты API
Аутентификация (доступно без токена)
Метод	Эндпоинт	Описание
POST	/api/auth/register	Регистрация нового пользователя
POST	/api/auth/login	Вход (возвращает JWT токен)
Пользовательские операции (требуют роль USER)
Метод	Эндпоинт	Описание
GET	/api/cards	Список карт (пагинация + фильтрация)
POST	/api/cards	Создание новой карты
GET	/api/cards/{id}/balance	Просмотр баланса
POST	/api/cards/{id}/block	Блокировка карты
POST	/api/cards/transfer	Перевод между своими картами
Параметры GET /api/cards:

Параметр	Тип	Описание
page	int	Номер страницы (default: 0)
size	int	Размер страницы (default: 10)
search	string	Поиск по номеру карты
status	string	Фильтр по статусу (ACTIVE/BLOCKED/EXPIRED)
Административные операции (требуют роль ADMIN)
Метод	Эндпоинт	Описание
GET	/api/admin/cards	Просмотр всех карт
DELETE	/api/admin/cards/{id}	Удаление карты
POST	/api/admin/cards/{id}/activate	Активация карты
GET	/api/admin/users	Просмотр всех пользователей
DELETE	/api/admin/users/{id}	Удаление пользователя

🧪 Запуск тестов
bash
# Все тесты
./mvnw test

# Только unit тесты
./mvnw test -Dtest=*Test

# Только интеграционные тесты
./mvnw test -Dtest=*IntegrationTest

# С отчетом о покрытии
./mvnw test jacoco:report

📦 Технологический стек
Компонент	Технология	Назначение
Фреймворк	Spring Boot 3.2.5	Основа приложения
Безопасность	Spring Security + JWT	Аутентификация и авторизация
База данных	PostgreSQL 15	Хранение данных
ORM	Spring Data JPA / Hibernate	Работа с БД
Миграции	Liquibase	Управление схемой БД
Документация	Swagger / OpenAPI 2.5.0	Документирование API
Шифрование	Spring Security Crypto (AES)	Шифрование номеров карт
Тестирование	JUnit 5, Mockito, Testcontainers	Модульное и интеграционное тестирование
Контейнеризация	Docker / Docker Compose	Развертывание и окружение
Сборка	Maven	Управление зависимостями

🔒 Безопасность
Механизм	Описание
Пароли пользователей	Хранятся в зашифрованном виде (BCrypt)
Номера карт	Хранятся в зашифрованном виде (AES-256)
Маскирование карт	В API возвращаются только маскированные номера: **** **** **** 1234
JWT токены	Срок действия: 24 часа
Ролевая модель	USER и ADMIN с разными правами

📁 Архитектура проекта
Проект построен на многослойной (трехуровневой) архитектуре с четким разделением ответственности.

text
src/main/java/org/example/bank/
│
├── controller/          # Web / REST слой
│   ├── AuthController.java      # Аутентификация (login, register)
│   ├── CardController.java      # Операции с картами для USER
│   └── AdminController.java     # Администрирование для ADMIN
│
├── service/             # Бизнес-логика (сервисный слой)
│   ├── AuthService.java         # Регистрация и аутентификация
│   ├── CardService.java         # CRUD карт, блокировка, переводы
│   ├── UserService.java         # Управление пользователями
│   └── CardExpirationScheduler.java  # Автоматическое обновление статуса EXPIRED
│
├── repository/          # Data Access Layer (DAO)
│   ├── UserRepository.java      # JPA репозиторий для пользователей
│   └── CardRepository.java      # JPA репозиторий для карт
│
├── entity/              # JPA сущности (таблицы БД)
│   ├── User.java                # Пользователь (username, password, role)
│   ├── Card.java                # Карта (номер, баланс, статус, срок действия)
│   └── enums/                   # Перечисления
│       ├── Role.java            # ROLE_USER, ROLE_ADMIN
│       └── CardStatus.java      # ACTIVE, BLOCKED, EXPIRED
│
├── dto/                 # Data Transfer Objects (для API)
│   ├── request/                 # Входящие DTO
│   │   ├── AuthRequest.java     # Логин (username + password)
│   │   ├── RegisterRequest.java # Регистрация
│   │   ├── CardRequest.java     # Создание карты
│   │   └── TransferRequest.java # Перевод (fromCardId, toCardId, amount)
│   └── response/                # Исходящие DTO
│       ├── AuthResponse.java    # JWT токен
│       ├── CardResponse.java    # Данные карты (маскированный номер, баланс)
│       └── ErrorResponse.java   # Стандартизированная ошибка
│
├── security/            # Безопасность и JWT
│   ├── jwt/
│   │   ├── JwtTokenProvider.java      # Генерация и валидация JWT
│   │   └── JwtAuthenticationFilter.java # Перехват запросов, проверка токена
│   ├── CustomUserDetailsService.java   # Загрузка пользователя из БД
│   └── SecurityConfig.java             # Настройки Spring Security
│
├── config/              # Конфигурации приложения
│   ├── CryptoConfig.java        # Настройки шифрования (AES)
│   └── OpenApiConfig.java       # Swagger / OpenAPI документация
│
├── exception/           # Обработка ошибок
│   ├── GlobalExceptionHandler.java    # Глобальный перехват исключений
│   ├── CardNotFoundException.java     # Карта не найдена (404)
│   ├── InsufficientFundsException.java # Недостаточно средств (400)
│   └── UnauthorizedAccessException.java # Нет прав (403)
│
└── utils/               # Утилиты и вспомогательные классы
    ├── CardNumberGenerator.java   # Генерация номера карты
    ├── CardNumberMasker.java      # Маскирование номера (**** **** **** 1234)
    └── CardNumberEncryptor.java   # Шифрование/дешифрование номера (AES)
🔄 Поток данных
text
[Клиент] → [Controller] → [Service] → [Repository] → [БД]
   ↑           ↓              ↓             ↓
   │         [DTO]        [Entity]      [Entity]
   │           ↓                          
   └───── [HTTP Response] ←────────────────┘
   
Описание слоев:
Слой	Назначение	Взаимодействие
Controller	Принимает HTTP запросы, валидирует входные DTO, вызывает сервисы	Не содержит бизнес-логики
Service	Содержит бизнес-логику (проверка баланса, переводы, блокировка)	Транзакции (@Transactional)
Repository	Интерфейсы для работы с БД (Spring Data JPA)	Запросы к PostgreSQL
Entity	Маппинг Java объектов на таблицы БД	Содержит JPA аннотации
DTO	Объекты для обмена между API и клиентом	Сквозная валидация (@Valid)
Security	Проверка JWT токенов, ролевой доступ	Фильтры перед контроллерами

📁 Структура тестов
text
src/test/java/org/example/bank/
├── controller/           # Интеграционные тесты REST API
│   ├── AuthControllerRegisterTest.java
│   ├── CardControllerIntegrationTest.java
│   └── AdminControllerIntegrationTest.java
├── service/              # Юнит-тесты бизнес-логики
│   ├── CardServiceTest.java
│   ├── CardExpirationSchedulerTest.java
│   └── CardExpirationSchedulerIntegrationTest.java
├── repository/           # Тесты репозиториев (с Testcontainers)
│   ├── UserRepositoryTest.java
│   └── CardRepositoryTest.java
└── utils/                # Тесты утилит
    └── CardNumberEncryptionTest.java

🔄 Автоматические задачи
Задача	Расписание	Описание
Обновление истекших карт	Каждый день в 00:00	Карты с истекшим сроком действия получают статус EXPIRED

📊 Коды ответов API
Код	Описание
200	Успешный запрос
201	Ресурс создан
400	Неверный запрос / ошибка валидации
401	Не авторизован (нет или неверный JWT)
403	Доступ запрещен (недостаточно прав)
404	Ресурс не найден
500	Внутренняя ошибка сервера