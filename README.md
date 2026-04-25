Система управления банковскими картами с JWT аутентификацией, переводами между картами и ролевой моделью (USER/ADMIN).

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