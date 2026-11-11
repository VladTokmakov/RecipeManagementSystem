		Recipe Management System (Система управления рецептами)

Система управления рецептами, построенная на базе Spring Boot и микросервисной архитектуры.

	Архитектура:
- catalogue-service - Сервис REST API для управления рецептами (порт 8081)
- manager-app - Веб-приложение для менеджеров по управлению рецептами (порт 8080)
- customer-app - Веб-приложение для клиентов (порт 8083)
- config - Файлы конфигурации (Keycloak)
- pom.xml - Родительский POM Maven

	    Технологии:
	Основные технологии:
- Java 21 - Язык программирования
- Spring Boot 3.5.3 - Фреймворк приложений
- Maven - Инструмент сборки и управления зависимостями
	Бэкенд технологии:
- Spring Data JPA - Слой доступа к базе данных
- PostgreSQL - Реляционная база данных
- Flyway - Инструмент миграции базы данных
- Spring Security OAuth2 - Аутентификация и авторизация
- Keycloak - Управление идентификацией и доступом
	Фронтенд технологии:
- Thymeleaf - Шаблонизатор на стороне сервера
- Spring MVC - Веб-фреймворк
- REST Client - Для межсервисного взаимодействия
	Инструменты разработки:
- Lombok 1.18.38 - Сокращение шаблонного кода
- SpringDoc OpenAPI 2.7.0 - Документация API (Swagger UI)
- Spring Boot DevTools - Инструменты повышения производительности разработки

	         Модули:
        1. Catalogue Service
Сервис REST API, предоставляющий возможности управления рецептами.
    Возможности:
- CRUD операции для рецептов
- Безопасность OAuth2 resource server
- Документация API с Swagger UI
- База данных PostgreSQL с миграциями Flyway
- Валидация и обработка ошибок
Порт: 8081
	Основные зависимости:
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter OAuth2 Resource Server
- PostgreSQL Driver
- Flyway Core
- SpringDoc OpenAPI
База данных: PostgreSQL (схема catalogue)

	    2. Manager App
Веб-приложение для менеджеров по управлению каталогом рецептов.
	Возможности:
- UI управления рецептами (создание, обновление, удаление)
- Аутентификация OAuth2 client с Keycloak
- Шаблоны Thymeleaf для рендеринга на стороне сервера
- Интеграция REST client с Catalogue Service
- Валидация форм
Порт: Настраиваемый (по умолчанию: 8080)
	Основные зависимости:
- Spring Boot Starter Web
- Spring Boot Starter Thymeleaf
- Spring Boot Starter OAuth2 Client
- Spring Boot Starter Validation
- Lombok

	    3. Customer App
Веб-приложение для клиентов по просмотру и взаимодействию с рецептами.
	Возможности:
- Просмотр рецептов с пагинацией
- Добавление рецептов в избранное
- Написание отзывов о рецептах
- Аутентификация OAuth2 client с Keycloak
- База данных PostgreSQL для данных клиентов
- Интеграция REST client с Catalogue Service
Порт: 8083
	Основные зависимости:
- Spring Boot Starter Web
- Spring Boot Starter Thymeleaf
- Spring Boot Starter OAuth2 Client
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- PostgreSQL Driver
- Flyway Core
- Lombok
База данных: PostgreSQL (схема customer_app)

	Архитектура безопасности:
Приложение использует OAuth2 с Keycloak для аутентификации и авторизации:
- Сервер Keycloak: Порт 8082
- Realm: RecipeManagementSystem
- OAuth2 Flows: Authorization Code Flow
	Роли и области действия:
- Роль Manager: Полный доступ к управлению рецептами
- Роль Customer: Просмотр рецептов, добавление в избранное, написание отзывов
- Области действия: openid, view_catalogue

	Конфигурация базы данных:
	База данных Catalogue Service:
- Хост: localhost:5432
- База данных: catalogue
- Имя пользователя: catalogue
- Пароль: catalogue
- Схемы: public, catalogue
	База данных Customer App:
- Хост: localhost:5434
- База данных: customer_app
- Имя пользователя: customer_app
- Пароль: customer_app
- Схемы: public, customer_app

	Запуск и остановка (Windows):
В корне проекта есть два скрипта PowerShell. Они поднимают и гасят весь стенд
(Keycloak, базы данных и три приложения) без IntelliJ IDEA.
	Запуск:
- .\start.ps1               обычный запуск
- .\start.ps1 -Rebuild      пересобрать jar перед запуском
- .\start.ps1 -SkipDocker   если контейнеры Keycloak и БД уже подняты
	Остановка:
- .\stop.ps1                остановить приложения (порты 8080/8081/8083)
- .\stop.ps1 -Docker        дополнительно остановить контейнеры (данные сохраняются)
	Что делают скрипты:
- start.ps1 проверяет .env, готовит сертификаты TLS, поднимает docker compose,
  ждёт готовности realm Keycloak, при необходимости собирает jar и запускает
  приложения скрытыми процессами java -jar.
  Процессы не привязаны к окну терминала и работают после его закрытия.
- jar пересобирается автоматически, если исходники или ресурсы новее него,
  поэтому не бывает запуска устаревшей сборки.
- используется настоящий java.exe из JDK (а не лаунчер javapath), поэтому
  на каждое приложение приходится ровно один процесс.
- PID слушающих порты процессов сохраняются в logs\pids.txt, логи — в logs\*.log.
- stop.ps1 останавливает приложения по сохранённым PID и освобождает порты.
  Процессы, запущенные из IntelliJ IDEA или не относящиеся к проекту, не затрагиваются.

	Учётные записи для проверки:
- vlad / password1234   — менеджер (manager-app)
- diana / password1234  — покупатель (customer-app)

	Адреса стенда (HTTPS):
- https://localhost             — customer-app
- https://localhost/manager     — manager-app
- https://localhost/auth        — Keycloak

HTTP-запросы на порт 80 перенаправляются на HTTPS (301).
Сертификат самоподписанный: при первом открытии браузер покажет
предупреждение. Чтобы его убрать, импортируйте config\certs\server.crt
в хранилище сертификатов ОС. Сертификаты создаются скриптом
scripts\New-Certs.ps1 (вызывается из start.ps1 автоматически) и в git
не попадают.

	Документация API (Swagger UI):
Swagger защищён тем же Keycloak, что и остальные приложения.
- URL: https://localhost/swagger-ui.html
- OpenAPI Spec: https://localhost/v3/api-docs
- при открытии браузер перенаправляет на страницу входа Keycloak;
  пускаются только менеджеры (vlad / password1234),
  покупатель получит 403
- кнопка Authorize внутри UI выдаёт токен по authorization code + PKCE
  с аудиенсом catalogue-service - им подписываются вызовы API
- чтобы отключить документацию полностью, задайте
  SPRINGDOC_ENABLED=false в .env
- отдельного пароля у Swagger больше нет: клиент Keycloak swagger-login,
  его секрет хранится в .env (SWAGGER_CLIENT_SECRET)

        Особенности реализации:
    Фильтрация:
- по названию рецепта
- по описанию
   