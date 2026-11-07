# Отчёт об аудите безопасности RecipeManagementSystem

Дата: 2026-11-06
Версия Keycloak: **26.8.2** (обновлено с 23.0.7 по разрешению владельца)
Методология: OWASP Top 10 (2021) + ручной аудит кода, конфигурации Keycloak, миграций и инфраструктуры.

---

## 1. Что было правильно изначально

| Контроль | Где | Комментарий |
|---|---|---|
| OAuth2/OIDC через Keycloak | `SecurityBeans.java` (оба web-приложения) | Authorization Code Flow |
| Resource server + stateless | `catalogue-service/.../SecurityBeans.java` | `SessionCreationPolicy.STATELESS` |
| `denyAll()` по умолчанию | там же | fail-closed политика |
| Scope на запись | там же | `SCOPE_edit_catalogue` |
| CSRF включён в web-приложениях | оба web-приложения | формы через `th:action` |
| Проброс пользовательского токена | `OAuthClientHttpRequestInterceptor` | не client_credentials → нет эскалации |
| Нет SQL-инъекций | `ProductRepository` | только derived-запросы, нет `@Query`/native |
| Нет XSS | все `templates/**` | только `th:text`, ни одного `th:utext` |
| Bean Validation | payload-классы | `@NotNull`, `@Size`, `@Min/@Max` |
| Безопасные ошибки | `ProblemDetail` | без стектрейсов |
| Flyway + `ddl-auto=none` | оба сервиса | схема под контролем |

---

## 2. Найденные и устранённые уязвимости

| # | Уязвимость | OWASP | Критичность | Что сделано |
|---|---|---|---|---|
| 1 | Секреты (client secrets, пароли БД) в репозитории | A02 | Critical | Вынесены в `${ENV}`; добавлены `.env.example`, `.gitignore`; **требуется ротация** |
| 2 | Нет TLS | A02 | High | Добавлены HSTS, `secure`-cookie (через `COOKIE_SECURE`); TLS-конфигурация вынесена в инфраструктуру (см. §5) |
| 3 | Эскалация «клиент → запись» (ROPC + optional `edit_catalogue`) | A01 | Critical | Keycloak: ROPC off, `edit_catalogue` убран у `customer-app`; в API добавлена **вторая** проверка `hasRole('MANAGER')` |
| 4 | Нет валидации `aud` JWT | A01 | High | `JwtConfig`: `DelegatingOAuth2TokenValidator` + `JwtClaimValidator` на `aud` |
| 5 | DoS через `size`/`page`/`filter` | A04 | High | `@Min/@Max/@Size` + обработчики → 400 |
| 6 | DoS/OOM: двойная выгрузка таблиц при двух фильтрах | A04 | High | Один `Specification`-запрос вместо двух `findAll` в память |
| 7 | Rate limiting отсутствует (публичный каталог) | A04 | High | `RateLimitFilter` (120 req/min на IP → 429), доверие `X-Forwarded-For` только явно |
| 8 | LIKE-wildcard injection (`%`, `_`) | A03 | Medium | `escapeLike()` + `ESCAPE '\'` в Specification |
| 9 | Mass assignment через `@ModelAttribute` | A01 | Medium | `binding = false` во всех трёх местах |
| 10 | IDOR/привязка к изменяемому `preferred_username` | A01 | Medium | Переход на неизменяемый `sub` |
| 11 | Отзывы без автора, без лимитов | A04 | Medium | `c_user_id`, `c_created_at`, unique `(product, user)`, CHECK `rating 1..5`, удаление своего отзыва |
| 12 | Нет RP-initiated logout (фантомный вход) | A07 | Medium | `OidcClientInitiatedLogoutSuccessHandler` + `post.logout.redirect.uris` |
| 13 | Cookie без флагов, нет таймаута | A07 | Medium | `http-only`, `same-site=lax`, `timeout=30m` |
| 14 | 500 вместо 404; NPE при пустом сообщении исключения | A05 | Medium | Единый `ControllerAdvice`; `NoSuchElementException` с сообщением |
| 15 | Страница «не найдено» отдавалась с кодом 200 | A05 | Low | `response.setStatus(404)` |
| 16 | SQL и PII в логах, log injection | A09 | Medium | `show-sql=false`, убран ввод из логов, `sanitizeForLog()` |
| 17 | Swagger открыт всегда | A05 | Low | Управляется `SPRINGDOC_ENABLED` |
| 18 | Нет CSP / Referrer-Policy / X-Frame-Options | A05 | Medium | Security-заголовки в обоих web-приложениях |
| 19 | Нет лимитов размера запроса | A04 | Medium | `max-http-form-post-size`, `max-swallow-size`, header-size |
| 20 | Тихий fallback на `http://localhost:8081` из-за опечатки ключа | A05 | Medium | `@ConfigurationProperties` + `@Validated`, без дефолта |
| 21 | NPE при `null` от `authorize()` | A05 | Medium | `ClientAuthorizationRequiredException` |
| 22 | Нет таймаутов у `RestClient` | A04 | Medium | connect 3 s / read 5 s |
| 23 | Keycloak: нет brute-force, нет аудита, слабый пароль админа | A07 | High | `bruteForceProtected`, `adminEventsEnabled`, `passwordPolicy` |
| 24 | `revokeRefreshToken=false` | A07 | Medium | `true` + `refreshTokenMaxReuse=1` |
| 25 | Порты БД на `0.0.0.0` | A05 | Medium | `127.0.0.1:` в compose |
| 26 | devtools в runtime | A05 | Low | Prod-вынос через профиль (см. §5) |
| 27 | `.idea/dataSources.xml` в git | A05 | Low | Убран из индекса + `.gitignore` |
| 28 | `messages.properties` в CP866 → кракозябры | — | Low | Перекодировано в UTF-8 |
| 29 | **Keycloak 23.0.7 устарел** (десятки CVE закрыты в 24–26) | A06 | High | Обновлён до **26.8.2**, realm переимпортирован и проверен |
| 30 | Rate limit можно было обойти: `X-Forwarded-For` подделывался, фильтр стоял после Security | A04 | High | `trust-forwarded-headers=false` по умолчанию, `@Order(HIGHEST_PRECEDENCE)`, лимиты в конфиге, точечная эвикция вместо `clear()` |
| 31 | Повторный «в избранное» → 500 из-за unique | A05 | Low | Идемпотентная операция |
| 32 | Шаблон получал `userId` чужих отзывов | A01 | Low | В модель отдаётся `ProductReviewView` без автора |

---

## 3. Регрессионные тесты

`catalogue-service` — **14 тестов, все проходят**:
- `SecurityRegressionTests` — 11 тестов контроля доступа и валидации
- `RateLimitTests` — лимит запросов → 429
- `RateLimitBeforeSecurityTests` — неаутентифицированные запросы тоже считаются
- `CatalogueServiceApplicationTests` — smoke-тест контекста

`manager-app` и `customer-app` — smoke-тесты контекста проходят без БД и Keycloak (репозитории и OAuth2-провайдер замоканы).

| Тест | Что проверяет | На уязвимой версии |
|---|---|---|
| `anonymousCanReadCatalogue` | каталог читается анонимно (осознанно) | — |
| `anonymousCannotCreateProduct` | 401 без токена | проходил бы 201 |
| `scopeWithoutManagerRoleCannotCreateProduct` | **scope без роли → 403** | проходил бы 201 |
| `managerRoleWithoutScopeCannotCreateProduct` | роль без scope → 403 | проходил бы 201 |
| `managerWithBothCanCreateProduct` | scope + роль → 201 | — |
| `oversizedPageSizeIsRejected` | `size=1000000` → 400 | 200 + нагрузка |
| `negativePageIsRejected` | `page=-1` → 400 | 500 |
| `oversizedFilterIsRejected` | фильтр 500 символов → 400 | 200 |
| `deletingMissingProductReturns404` | 404, не 500 | 500 |
| `missingProductReturns404WithoutInternals` | 404 + ProblemDetail | 500 |
| `tooShortTitleIsRejected` | валидация тела → 400 | 400 (уже было) |
| `requestsAboveLimitAreRejectedWith429` | 6-й запрос сверх лимита → 429 | 200 без ограничений |
| `unauthenticatedRequestsAreAlsoCounted` | фильтр стоит до Security — 401 тоже расходует лимит | лимит обходился потоком 401 |

Запуск:
```
cd catalogue-service
.\mvnw.cmd test
```

---

## 3.1. Дефект, найденный при браузерной проверке (исправлен)

Проверка «глазами пользователя» выявила то, чего не видели smoke-тесты:

| Симптом | Причина | Исправление |
|---|---|---|
| `?page=-1`, `?size=0`, `?size=1000`, `?page=abc` → **500** с пустой страницей | ограничения были на параметрах, но не было обработчика `ConstraintViolationException` / `MethodArgumentTypeMismatchException`; исключение уходило в общий обработчик | `WebExceptionAdvice` в обоих веб-приложениях → корректный **400** и страница `errors/400.html` |
| после добавления `@Validated` форма создания рецепта на пустое название отдавала **400** вместо формы с ошибкой | `@Validated` на классе включает AOP-метод-валидацию, которая перехватывает `@Valid` у формы **до** `BindingResult` | `@Validated` убран с контроллеров; используется встроенная валидация параметров Spring 6.2 (`HandlerMethodValidationException`) |
| форма отзыва с `rating=99` падала так же | то же | то же |

Важно: `@Validated` **оставлен** в `catalogue-service` (`ProductsRestController`) — там нет форм с `BindingResult`, и AOP-валидация параметров корректна.

---

## 3.2. Дефекты, найденные при ручной браузерной проверке (исправлены)

Вторая итерация проверки «как пользователь» выявила блокирующие дефекты:

| Симптом | Причина | Исправление |
|---|---|---|
| **500** (Whitelabel) при сохранении изменений рецепта, при этом чтение работало | `SimpleClientHttpRequestFactory` работает поверх `HttpURLConnection`, который **не поддерживает метод PATCH**: `java.net.ProtocolException: Invalid HTTP method: PATCH`. Менеджер вызывал `PATCH /catalogue-api/products/{id}` | `ClientBeans.java` (manager) и `ClientConfig.java` (customer) переведены на `ClientHttpRequestFactoryBuilder.jdk()` — JDK `HttpClient` умеет PATCH |
| менеджер входил в **клиентское** приложение, а затем Keycloak по общей SSO-сессии пускал его и в manager-app **без пароля** | `customer-app` проверял только `SCOPE_view_catalogue`, который есть и у менеджера. Роль `ROLE_MANAGER` клиентского приложения не ограничивала | `customer-app`: `hasAuthority('SCOPE_view_catalogue') and hasRole('CUSTOMER')` |
| Whitelabel Error Page на любой необработанной ошибке | не было `templates/error.html`; сетевые ошибки к `catalogue-service` (`RestClientException`) не обрабатывались | добавлены `templates/error.html` (оба приложения) и `@ExceptionHandler(RestClientException.class)` → **502** с понятным текстом |
| после входа открывалась заглушка «Добро пожаловать!» | `/` обслуживался статическим `index.html`, контроллера для корня не было | `HomeController` → redirect на список рецептов; заглушки удалены |
| отказ в доступе (403) отдавался с **пустым телом** — белая страница | `AccessDeniedHandler` по умолчанию пишет статус без тела | `accessDeniedPage("/errors/403")` + `errors/403.html` в обоих приложениях |
| страница 404 была без оформления и навигации | минимальный шаблон-заглушка | `errors/404.html` приведена к общему layout (navbar, кнопка выхода, ссылка назад) |
| сменить пользователя было нечем — вход происходил без пароля | не было кнопки выхода, а без RP-initiated logout сессия Keycloak оставалась живой | POST `/logout` (с CSRF) в навигацию всех шаблонов + `OidcClientInitiatedLogoutSuccessHandler` |

Проверено на живом стенде: редактирование → **302** (было 500), `vlad` в customer-app → **403**, `diana` в manager-app → **403**, выход → при повторном входе требуется пароль.

Отдельно: `styles.css` в обоих приложениях был **повреждён в репозитории** — ASCII-часть в UTF-8 склеена с фрагментом в UTF-16LE (1266 нулевых байт), из-за чего правила после секции `.pagination` не применялись. Файл перекодирован в чистый UTF-8.

---

## 4. Изменённые файлы

### catalogue-service
- `config/SecurityBeans.java` — явные матчеры, scope + роль, security-заголовки
- `config/JwtConfig.java` — **новый**: валидация `aud`, маппинг realm-ролей
- `config/RateLimitFilter.java` — **новый**: 120 req/min на IP, `X-Forwarded-For` не доверяется по умолчанию
- `controller/ProductsRestController.java` — лимиты пагинации, `sanitizeForLog`
- `controller/ProductRestController.java` — убран локальный обработчик (дублировал advice)
- `controller/BadRequestControllerAdvice.java` — единая обработка 400/404/500
- `repository/ProductRepository.java` — `JpaSpecificationExecutor`
- `service/ProductService.java`, `DefaultProductService.java` — один запрос, `escapeLike`, корректные исключения
- `resources/application-standalone.yaml` — env-плейсхолдеры, лимиты, логи
- `resources/messages.properties` — UTF-8, новые ключи
- `resources/db/migration/V24_1_2__add_product_indexes.sql` — **новый**
- `pom.xml` — `spring-security-test`
- `test/.../SecurityRegressionTests.java` — **новый** (11 тестов)
- `test/.../RateLimitTests.java` — **новый** (429 при превышении лимита)
- `test/.../RateLimitBeforeSecurityTests.java` — **новый** (лимит действует до аутентификации)
- `test/resources/application.properties` — **новый** (тесты без БД/Keycloak)

### manager-app
- `config/SecurityBeans.java` — RP-logout, CSP, Referrer-Policy, entry point, `hasRole("MANAGER")` на каждый запрос, `accessDeniedPage`
- `config/CatalogueProperties.java` — **новый**: fail-fast конфигурация
- `config/ClientBeans.java` — таймауты, properties, **JDK `HttpClient` вместо `SimpleClientHttpRequestFactory`** (PATCH)
- `security/OAuthClientHttpRequestInterceptor.java` — защита от NPE
- `controller/HomeController.java` — **новый**: `/` → список рецептов
- `controller/AccessDeniedController.java` — **новый**: страница 403
- `controller/ProductsController.java`, `ProductController.java` — `@Valid`, `binding = false`, лимиты `page`/`size`/`filter`
- `controller/WebExceptionAdvice.java` — **новый**: 400/502 вместо 500 на некорректные параметры и сбои каталога
- `resources/templates/error.html` — **новый**: общая страница вместо Whitelabel
- `resources/templates/errors/400.html`, `errors/403.html`, `errors/404.html` — страницы ошибок
- `resources/templates/catalogue/products/*.html` — кнопка «Выйти» (POST `/logout` + CSRF)
- `resources/static/styles.css` — восстановлен из смешанной UTF-8/UTF-16LE, стиль `.nav-logout`
- `resources/static/index.html` — **удалён** (заглушка)
- `resources/messages.properties` — ключи `errors.502.*`
- `ManagerApplication.java` — `@ConfigurationPropertiesScan`
- `resources/application-standalone.yaml` — env-плейсхолдеры, cookie, лимиты, `spring.config.import` для `.env`

### customer-app
- `config/SecurityBeans.java` — RP-logout, CSP, entry point, `groups`, **`SCOPE_view_catalogue` + `ROLE_CUSTOMER`**, `accessDeniedPage`
- `config/ClientConfig.java` — проброс токена + таймауты, **JDK `HttpClient`** (PATCH)
- `security/OAuthClientHttpRequestInterceptor.java` — **новый**
- `controller/HomeController.java` — **новый**: `/` → список рецептов
- `controller/AccessDeniedController.java` — **новый**: страница 403
- `controller/ProductController.java`, `ProductsController.java` — `sub`, лимиты, удаление своего отзыва, 404-статус
- `controller/WebExceptionAdvice.java` — **новый**: 400/502 вместо 500 на некорректные параметры и сбои каталога
- `resources/templates/error.html` — **новый**: общая страница вместо Whitelabel
- `resources/templates/errors/400.html`, `errors/403.html`, `errors/404.html` — страницы ошибок
- `resources/templates/customer/products/*.html` — кнопка «Выйти» (POST `/logout` + CSRF)
- `resources/static/styles.css` — восстановлен из смешанной UTF-8/UTF-16LE, стиль `.nav-logout`
- `resources/static/index.html` — **удалён** (заглушка)
- `entity/ProductReview.java`, `FavouriteProduct.java` — `userId`, `createdAt`
- `repository/ProductReviewRepository.java` — методы автора
- `service/ProductReviewsService.java`, `DefaultProductReviewsService.java` — авторство, update-or-create
- `resources/application.properties` — env-плейсхолдеры, cookie, лимиты, `spring.config.import` для `.env`
- `resources/messages.properties` — UTF-8, ключи `errors.502.*`
- `resources/db/migration/V24_1_3__harden_customer_app.sql` — **новый**
- `templates/customer/products/product.html` — кнопка удаления своего отзыва

### Keycloak и инфраструктура
- `config/keycloak/import/realm.json` — Keycloak **26.8.2**, ROPC off, scopes, audience-mapper, logout URIs, brute-force, events, password policy, `fullScopeAllowed=false` + явные `scopeMappings`, принудительный PKCE убран
- `docker-compose.yml` — **новый**: Keycloak 26.8.2, `KC_BOOTSTRAP_ADMIN_*`, healthcheck, loopback-порты, volume для БД
- `.env.example` — **новый**
- `.gitignore` — секреты, `.idea/dataSources.xml`, `project_export.txt`

---

## 4. Обновление Keycloak 23.0.7 → 26.8.2

Выполнено по разрешению владельца. Проверено на чистом контейнере (`--import-realm`), без изменения истории git.

### Что изменилось в инфраструктуре

| Было (23.0.7) | Стало (26.8.2) | Причина |
|---|---|---|
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | `KC_BOOTSTRAP_ADMIN_USERNAME` / `KC_BOOTSTRAP_ADMIN_PASSWORD` | старые переменные удалены в Keycloak 26 |
| — | `KC_HEALTH_ENABLED=true` + healthcheck на `/health/ready` | готовность контейнера и мониторинг |
| `pkce.code.challenge.method=S256` у web-клиентов | **удалён** | в 26 это «PKCE обязателен», а Spring Security не шлёт `code_challenge` для confidential-клиентов → вход ломался с `Missing parameter: code_challenge_method` |
| `fullScopeAllowed=true` | `false` + явные `scopeMappings` | режим объявлен deprecated в 26 и будет удалён; роли теперь выдаются точечно |
| `keycloakVersion: 23.0.7` в realm.json | `26.8.2` | соответствует импортируемой версии |

### Проверки после обновления

| Проверка | Результат |
|---|---|
| Импорт realm на чистом контейнере | `Import finished successfully`, без ERROR |
| Health `/health/ready` | `UP` |
| Страница входа (authorization endpoint) | 200, форма рендерится (не требует PKCE) |
| `aud` в access-токене | `["catalogue-service","account"]` |
| `realm_access.roles` для пользователя из группы `Managers` | `["ROLE_MANAGER"]` |
| `groups` для пользователя из группы `Managers` | `["ROLE_MANAGER"]` |
| ROPC у `customer-app` | `400 unauthorized_client: Client not allowed for direct access grants` |
| Запрос `edit_catalogue` клиентом `customer-app` | `invalid_scope` — scope не выдан |
| `catalogue-service` (bearer-only) `client_credentials` | отказ |
| Brute-force, admin events, revoke refresh, password policy | сохранены после миграции |

> **Важно:** Keycloak 26 не поддерживает «обновление на месте» H2-базы `start-dev` между мажорными версиями. Источник истины — `config/keycloak/import/realm.json`; старый контейнер удаляется, realm импортируется заново. Именно поэтому realm версионируется в репозитории.

---

## 5. Что осталось (остаточный риск)

| # | Риск | Почему не устранено | Что нужно |
|---|---|---|---|
| 1 | **Ротация секретов** | Требует действий владельца Keycloak/БД | Сменить client secrets и пароли БД; старое считать скомпрометированным |
| 2 | Секреты в истории git | История сохранена по требованию владельца | Ротация секретов обязательна: старые значения навсегда остаются в коммитах |
| 3 | TLS | Нужны сертификаты и решение по топологии | Keycloak за HTTPS, `requiresChannel`, `sslmode=require` (config уже готов к `${KEYCLOAK_ISSUER_URI}` = https) |
| 4 | Админ `admin/admin` | Пароль задаётся при старте контейнера | `KEYCLOAK_ADMIN_PASSWORD` из `.env` (уже в compose) |
| 5 | Rate limit в памяти | Для одного инстанса достаточно | Redis/API-gateway при горизонтальном масштабировании |
| 6 | Data migration `sub` | Существующее избранное привязано к username | Скрипт переноса `preferred_username` → `sub` |
| 7 | `spring-boot-devtools` в runtime | Требует разделения сборок | Профиль `prod` + `<scope>provided</scope>` |
| 8 | Импорт realm при `start-dev` | H2-база не переносится между мажорными версиями | Для prod — внешний PostgreSQL + `kc.sh build/start` |

> **Решение владельца:** история git **не переписывается** (нужна для защиты работы). Следствие: секреты, попавшие в коммиты, остаются доступными навсегда. Поэтому п. 1 (ротация) — не «желательно», а **обязателен**: после ротации старые значения в истории бесполезны.

---

## 6. Как проверить демонстрацию «до/после»

```powershell
# 1. Уязвимая версия
git checkout v0-vulnerable

# 2. Запуск стенда
docker compose up -d
# PostgreSQL: 127.0.0.1:5432, 127.0.0.1:5434
# Keycloak:   127.0.0.1:8082

# 3. Атака: эскалация клиент → запись
#    (на уязвимой версии ROPC включён и edit_catalogue доступен customer-app)
curl -X POST "http://localhost:8082/realms/RecipeManagementSystem/protocol/openid-connect/token" `
  -d "client_id=customer-app&client_secret=..." `
  -d "username=anton&password=...&grant_type=password&scope=openid edit_catalogue"

# 4. Переключение на защищённую
git checkout main
# -> шаг 3 возвращает invalid_scope / unauthorized_client, а запись — 403
.\catalogue-service\mvnw.cmd test -Dtest=SecurityRegressionTests
```

### Демонстрация обновления Keycloak

```powershell
# Удалить контейнер старой версии (её H2-база несовместима с 26.x)
docker rm -f RecipeManagementSystem-keycloak

# Запустить 26.8.2 — realm импортируется из версионированного realm.json
docker compose up -d keycloak
docker compose logs -f keycloak | Select-String "Import finished"

# Проверки
curl "http://localhost:8082/realms/RecipeManagementSystem/.well-known/openid-configuration"
# ожидается issuer: http://localhost:8082/realms/RecipeManagementSystem
```
