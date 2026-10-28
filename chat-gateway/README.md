# chat-gateway — API Gateway / BFF (практическая работа № 5)

Единая точка входа для браузера: `http://localhost:8088`.

- Шлюз — конфиденциальный OIDC-клиент `course-bff` (Authorization Code).
- Токены хранятся только на сервере, в `WebSession`; сессия лежит в Redis (`course:gateway:sessions:*`). Браузер получает только cookie `SESSION` (HttpOnly, SameSite=Lax).
- `TokenRelay` подставляет `Authorization: Bearer <token>` при проксировании в `chat-rest` (порт 8089).
- Маршруты — белый список: `/api/profiles`, `/api/profiles/**`, `/api/reports`, `/api/reports/**`.
- Изменяющие запросы требуют CSRF-токен (`GET /bff/csrf`).
- Если backend недоступен, шлюз отвечает 502, а при таймауте — 504.

## Запуск

```
docker compose up -d postgres redis keycloak rabbitmq
mvn -DskipTests install
mvn -pl chat-rest spring-boot:run
mvn -pl chat-gateway spring-boot:run
```

Если Keycloak уже запускался со старым realm, пересоздайте контейнер: `docker compose up -d --force-recreate keycloak`.

Пользователи: `user / user` (роль `user`) и `admin / admin` (роль `admin`).

## Проверка

| Что проверяем | Как | Ожидается |
|---|---|---|
| Сессия в Redis | `docker compose exec redis redis-cli --scan --pattern 'course:gateway:sessions:*'` | ключи сессий |
| Токенов нет в браузере | DevTools → Application | только cookie `SESSION`, Local/Session Storage пусты |
| TokenRelay | кнопка «Профили» | 200 + JSON |
| Прямой запрос к backend | `http://localhost:8089/api/profiles` без токена | 401 |
| Запрос без CSRF | POST `/api/profiles` без заголовка `X-CSRF-TOKEN` | 403 |
| Авторизация на backend | `user` → «Жалобы (admin)» | 403 (у `admin` — 200) |
| Сбой backend | остановить chat-rest → «Профили»; `/bff/me` | 502; `/bff/me` → 200 |

Запрос с CSRF из консоли DevTools:

```js
const csrf = await fetch('/bff/csrf').then(r => r.json());
const response = await fetch('/api/profiles', {
  method: 'POST',
  credentials: 'same-origin',
  headers: {'Content-Type': 'application/json', [csrf.headerName]: csrf.token},
  body: JSON.stringify({nickname: 'EricEvans', age: 30, preferredLanguage: 'en'})
});
console.log(response.status, await response.json());
```
