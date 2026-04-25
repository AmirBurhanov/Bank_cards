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

## 🚀 Быстрый старт

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
🛠 Технологии
Технология	Версия	Назначение
Java	17	Язык программирования
Spring Boot	3.2.5	Фреймворк
Spring Security	-	Аутентификация и авторизация
JWT	0.11.5	Токены доступа
Spring Data JPA	-	Работа с БД
PostgreSQL	15	База данных
Liquibase	-	Миграции БД
Docker	-	Контейнеризация
Swagger/OpenAPI	2.5.0	Документация API
JUnit 5	-	Тестирование
Testcontainers	1.19.7	Интеграционные тесты

🔒 Безопасность
Механизм	Описание
Пароли пользователей	Хранятся в зашифрованном виде (BCrypt)
Номера карт	Хранятся в зашифрованном виде (AES-256)
Маскирование карт	В API возвращаются только маскированные номера: **** **** **** 1234
JWT токены	Срок действия: 24 часа
Ролевая модель	USER и ADMIN с разными правами


📁 Структура проекта
text
src/
├── main/java/org/example/bank/
│   ├── controller/      # REST контроллеры (Swagger аннотации)
│   ├── service/         # Бизнес-логика
│   ├── repository/      # JPA репозитории
│   ├── entity/          # Сущности БД
│   ├── dto/             # DTO для запросов/ответов
│   ├── security/        # Spring Security + JWT
│   ├── exception/       # Глобальный обработчик ошибок
│   ├── config/          # Конфигурации (OpenAPI, Crypto)
│   └── utils/           # Утилиты (шифрование, маскирование)
├── main/resources/
│   └── db/changelog/    # Liquibase миграции
└── test/                # Unit и интеграционные тесты


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