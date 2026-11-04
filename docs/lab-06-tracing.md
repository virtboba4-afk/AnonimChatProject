# Практическая работа № 6. Корреляция запросов и распределённая трассировка

Сценарий: `GET /api/profiles/{id}/activity`.

```
Браузер → chat-gateway:8088 → chat-rest:8089 → chat-audit:8081
Каждый сервис → OTLP/HTTP :4318 → OpenTelemetry Collector → Tempo ← Grafana
```

`chat-rest` проверяет, что профиль существует (иначе сразу 404 без вызова `chat-audit`), и запрашивает историю событий у `chat-audit` от имени сервисной учётной записи `course-service-client`. Токен пользователя в `chat-audit` не передаётся.

## Что сделано

| Требование | Где |
|---|---|
| Проверка и нормализация `X-Request-ID` | `RequestIds` в каждом сервисе |
| Входящий фильтр в сервлетных сервисах | `RequestIdFilter` (`chat-rest`, `chat-audit`) |
| Входящий фильтр в шлюзе, Reactor Context и MDC | `RequestIdWebFilter` |
| Проброс заголовка через настроенный HTTP-клиент | `HttpClientConfig` (`chat-rest`) |
| MDC в логах | `logging.pattern.correlation` в консоли, поля `traceId`, `spanId`, `requestId` в JSON-файлах `runtime-logs/*.log` |
| Экспорт трасс в Tempo | `micrometer-tracing-bridge-otel` и `opentelemetry-exporter-otlp` в трёх сервисах |
| Безопасность сохранена | BFF и CSRF в шлюзе, JWT в `chat-rest`, новый JWT с аудиторией `chat-audit` и ролью `service` на `/internal/**` |

В методичке используется Spring Boot 4 и `spring-boot-starter-opentelemetry`. В проекте Spring Boot 3.2, поэтому подключены отдельные библиотеки Micrometer Tracing и OTLP-экспортёр. Цепочка и результат те же.

## Запуск

Нужны JDK 21 и Docker. Если Keycloak уже запускался со старым realm, его нужно пересоздать, иначе токены не получат аудиторию `chat-audit`.

```powershell
docker compose up -d
docker compose up -d --force-recreate keycloak
mvn -DskipTests install
mvn -pl chat-audit spring-boot:run
mvn -pl chat-rest spring-boot:run
mvn -pl chat-gateway spring-boot:run
```

Логи пишутся в `../runtime-logs` относительно рабочего каталога модуля. При запуске из IDEA задайте в конфигурации запуска переменную среды `LOG_DIR` с абсолютным путём к `runtime-logs` в корне проекта.

Пользователи: `user / user`, `admin / admin`.

## Автоматическая проверка

```powershell
.\scripts\verify-tracing.ps1 | Format-List
```

Ожидается `Status: OK` и `HttpSpans: 5`.

## Демонстрация преподавателю

1. **Сохранённый и заменённый `X-Request-ID`.** Войдите на `http://localhost:8088` и выполните в консоли браузера:

   ```js
   const csrf = await fetch('/bff/csrf').then(r => r.json());
   const created = await fetch('/api/profiles', {
     method: 'POST',
     headers: {'Content-Type': 'application/json', [csrf.headerName]: csrf.token},
     body: JSON.stringify({nickname: 'demo-' + Date.now(), age: 30, preferredLanguage: 'en'})
   }).then(r => r.json());
   window.demoId = created.id;

   const requestId = crypto.randomUUID();
   const ok = await fetch(`/api/profiles/${demoId}/activity`, {headers: {'X-Request-ID': requestId}});
   console.log(requestId, ok.status, ok.headers.get('X-Request-ID'));

   const bad = await fetch(`/api/profiles/${demoId}/activity`, {headers: {'X-Request-ID': '1-1-1-1-1'}});
   console.log(bad.status, bad.headers.get('X-Request-ID'));
   ```

2. **Логи трёх сервисов.** Подставьте `requestId`:

   ```powershell
   Get-ChildItem runtime-logs -Filter '*.log' | Select-String -SimpleMatch 'UUID'
   ```

   У всех записей один `requestId` и один `traceId`, а `spanId` у сервисов разные.

3. **Дерево спанов.** Grafana `http://localhost:3000` (`admin / admin-dev-only`) → Explore → Tempo → поиск по Trace ID:

   ```
   chat-gateway SERVER
   └─ chat-gateway CLIENT
      └─ chat-rest SERVER
         └─ chat-rest CLIENT
            └─ chat-audit SERVER
   ```

4. **Ранний отказ по CSRF.** Шлюз отвечает 403 с новым UUID, в `chat-rest` запрос не попадает:

   ```js
   const denied = await fetch('/api/profiles', {
     method: 'POST',
     headers: {'Content-Type': 'application/json', 'X-Request-ID': 'not-a-uuid'},
     body: JSON.stringify({nickname: 'NoCsrf', age: 30, preferredLanguage: 'en'})
   });
   console.log(denied.status, denied.headers.get('X-Request-ID'));
   ```

5. **Ошибка со сквозной корреляцией.** Остановите `chat-audit` и повторите запрос активности. `chat-rest` вернёт 503 и тот же `X-Request-ID`, а в его логе появится `Downstream call failed`.

## Что объяснить устно

- Прикладной интерцептор и фильтры переносят только `X-Request-ID`. Это идентификатор для поиска по логам, он не даёт прав доступа.
- OpenTelemetry создаёт спаны, для каждого прыжка формирует новый `traceparent` и сохраняет `parent span ID`. `Trace ID` на всём пути один, `Span ID` у каждого этапа свой.
- Невалидный `traceparent` не ломает запрос: библиотека начинает новую трассу.
- `X-Request-ID` не подходит для метки метрики из-за неограниченной кардинальности.
