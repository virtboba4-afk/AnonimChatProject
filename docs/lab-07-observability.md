# Практическая работа № 7. Промышленная наблюдаемость

## Критический сценарий

Просмотр активности профиля: `GET /api/profiles/{id}/activity` проходит через `chat-gateway`, `chat-rest` и `chat-audit`. Запуск описан в [lab-06-tracing.md](lab-06-tracing.md).

## Что сделано

| Требование | Реализация |
|---|---|
| Контекст трассировки сохранён | Работа № 6: W3C Trace Context, выборка 100 % |
| HTTP-метрики минимум двух сервисов | `http_server_requests_seconds_*` у трёх сервисов, `uri` нормализован до шаблона |
| Доменный счётчик с ограниченными метками | `audit_activity_requests_total{result="success\|failure"}`, а также таймер `audit_activity_processing` и датчик `audit_activity_active` |
| JSON ECS в логах | `logback-spring.xml` и `logback-ecs-encoder`, файлы `runtime-logs/*.log`, доставка Alloy → Loki |
| Actuator изолирован | `health` и `prometheus` на `127.0.0.1:9088`, `9089`, `9081`. На бизнес-портах только `/livez` и `/readyz` |
| Дашборд | Grafana → **Chat Golden Signals** |
| Алерт и runbook | `infra/prometheus/alerts.yml`, панель Runbook на дашборде |
| Тесты правил | `infra/prometheus/rules-test.yml` |

Spring Boot 3.2 ещё не умеет `logging.structured.format`, поэтому формат ECS задаёт энкодер Elastic. Он не выводит пары из `addKeyValue`, поэтому поля `event`, `outcome` и `failureType` передаются через MDC на время записи.

| Назначение | Адрес |
|---|---|
| BFF | `http://localhost:8088` |
| Метрики gateway / rest / audit | `http://localhost:9088/actuator/prometheus`, `:9089`, `:9081` |
| Prometheus / Alertmanager | `http://localhost:9090`, `http://localhost:9093` |
| Grafana | `http://localhost:3000` (`admin / admin-dev-only`) |

## SLI и SLO

| Параметр | Значение |
|---|---|
| Точка замера | Сервлет `chat-rest`, метрика `http_server_requests_seconds` |
| Подходящие запросы | Аутентифицированные `GET /api/profiles/{id}/activity` со статусом `2xx` или `5xx` |
| Исключены | `401`, `403`, `404`, `400` и служебные эндпоинты |
| Хороший запрос | Статус `200` быстрее 500 мс (бакет `le="0.5"`) |
| SLI | хорошие запросы / подходящие запросы |
| SLO | не менее 99,5 % на скользящем окне 7 дней |
| Бюджет ошибок | 0,5 %, то есть не более 100 плохих ответов на 20 000 подходящих запросов |
| Burn rate | доля плохих ответов / 0,005 |

Правило `chat:activity_good_ratio:rate5m` считает SLI на окне 5 минут. Это наглядно для занятия, но само по себе не подтверждает семидневный SLO. Для него нужна непрерывная история и отношение суммарных приростов за 7 дней.

## Алерт

`ChatActivityHighErrorRatio`: доля 5xx выше 20 % за минуту, не менее 10 запросов, выдержка 20 секунд. Порог учебный, в боевой среде алерт строят по burn rate на нескольких окнах. Получатель `local-only` никуда не отправляет сообщения, доставка видна в интерфейсе Alertmanager.

Runbook находится на дашборде и открывается по ссылке `runbook_url` из алерта.

## Проверка

```powershell
.\scripts\smoke-health.ps1
docker compose run --rm --no-deps --entrypoint promtool prometheus check config /etc/prometheus/prometheus.yml
docker compose run --rm --no-deps -w /etc/prometheus --entrypoint promtool prometheus test rules rules-test.yml
mvn -pl chat-rest test
.\scripts\verify-observability.ps1 | Format-List
```

Ожидается `Status: OK`, `HttpSpans: 5` и `FaultExperiment: 503 -> firing -> Alertmanager -> NORMAL -> resolved`. Ключ `-SkipFaultExperiment` пропускает фазу сбоя.

Скрипты работают и в Windows PowerShell 5.1, и в `pwsh`.

## Демонстрация полного цикла

Откройте дашборд **Chat Golden Signals**, `http://localhost:9090/alerts` и `http://localhost:9093`.

1. **Норма.**

   ```powershell
   .\scripts\set-diagnostic-mode.ps1 -Mode NORMAL
   .\scripts\generate-traffic.ps1 -Count 30 -IntervalMs 200
   ```

2. **Медленный успех.** Все ответы 200, панель 5xx спокойная, но p95 выше 0,75 с и SLI падает. Поэтому скорость нельзя оценивать по кодам ответа.

   ```powershell
   try {
       .\scripts\set-diagnostic-mode.ps1 -Mode SLOW -Seconds 45
       .\scripts\generate-traffic.ps1 -Count 20 -IntervalMs 200
   } finally {
       .\scripts\set-diagnostic-mode.ps1 -Mode NORMAL
   }
   ```

3. **Быстрый отказ.** Алерт проходит `pending` → `firing` и появляется в Alertmanager.

   ```powershell
   try {
       .\scripts\set-diagnostic-mode.ps1 -Mode FAIL -Seconds 120
       .\scripts\generate-traffic.ps1 -Count 180 -IntervalMs 300
   } finally {
       .\scripts\set-diagnostic-mode.ps1 -Mode NORMAL
   }
   ```

4. **Поиск причины.** Grafana → Explore → Loki:

   ```
   {job="course-apps",service="chat-audit"} | json | outcome="failure"
   ```

   Возьмите `requestId` и найдите всю цепочку. Ссылка `TraceID` в записи лога открывает трассу в Tempo:

   ```
   {job="course-apps"} | json | requestId="ВСТАВЬТЕ_REQUEST_ID"
   ```

5. **Восстановление.** Алерт снимается примерно через минуту, когда ошибки выходят из окна.

   ```powershell
   .\scripts\generate-traffic.ps1 -Count 100 -IntervalMs 300
   ```

## Режимы сбоя

`PUT /internal/diagnostics/mode` в `chat-audit` требует роль `operator`, то есть токен `course-ops-client`. Режим хранится в памяти и сбрасывается сам, максимум через 120 секунд.

| Режим | Поведение |
|---|---|
| `NORMAL` | Штатная работа |
| `SLOW` | Задержка 750 мс: укладывается в таймаут чтения 2 с, но нарушает порог 500 мс |
| `FAIL` | Немедленный 503 от `chat-audit`, `chat-rest` тоже отвечает 503 |

Режимы не влияют на `/livez` и `/readyz`: процесс жив и принимает команды, хотя бизнес-метод падает. Поэтому зелёный health-check не гарантирует отсутствие ошибок.
