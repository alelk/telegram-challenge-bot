# Telegram Challenge Bot - Архитектура и Детали Реализации

## Обзор проекта

Telegram Challenge Bot - это легковесное Kotlin-приложение для автоматизации челленджей в Telegram группах. Бот
публикует опросы-задания по расписанию, отслеживает выполнение, ведёт статистику и отправляет отчёты.

## Ключевые особенности

- ✅ **Многогрупповая поддержка** - одновременная работа с несколькими Telegram группами
- ✅ **Гибкая конфигурация** - YAML-файл с настройками для каждой группы
- ✅ **Система баллов** - каждый вариант ответа имеет свой вес
- ✅ **Автоматические отчёты** - еженедельная/ежедневная статистика
- ✅ **GraalVM Native** - компиляция в нативный образ для минимального потребления ресурсов
- ✅ **Встроенная БД** - H2 database для хранения истории и статистики

## Архитектура

### Стек технологий

- **Язык**: Kotlin 1.9.23
- **Telegram API**: ktgbotapi (TelegramBotAPI) 18.2.1
- **Конфигурация**: Hoplite 2.8.0
- **База данных**: Exposed ORM + H2 2.2.224
- **Асинхронность**: kotlinx-coroutines 1.8.0
- **Дата/время**: kotlinx-datetime 0.5.0
- **Логирование**: kotlin-logging + slf4j-simple

### Структура проекта

```
src/main/kotlin/io/github/alelk/apps/challengetgbot/
├── App.kt                    # Точка входа приложения
├── config/
│   └── AppConfig.kt         # Модель конфигурации
├── db/
│   ├── Challenges.kt           # Таблица челленджей (Exposed)
│   ├── PoolAnswers.kt          # Таблица ответов на челленджи (Exposed)
│   ├── PollOptionConfigs.kt    # Конфигурации челленджей (опросов) для каждой групы ТГ (Exposed)
│   └── DatabaseService.kt   # Сервис управления БД
├── domain/
│   └── ChallengeEntity.kt         # Доменная модель - Информация о челлендже
│   └── PoolAnswerEntity.kt        # Доменная модель - Информация об ответе на челлендж
│   └── UserStatistics.kt          # Доменная модель - Статистика пользователя
├── repository/
│   └── ChallengeRepository.kt # Репозиторий для работы с данными
├── scheduler/
│   └── ChallengeScheduler.kt  # Планировщик заданий
└── telegram/
    └── TelegramBotService.kt  # Интеграция с Telegram Bot API
```

## Детали реализации

### 1. Конфигурация (config/)

#### AppConfig.kt

Определяет структуру YAML-конфигурации:

- `AppConfig` - корневая конфигурация (токен бота, путь к БД, список групп)
- `GroupConfig` - настройки группы (chatId, threadId, имя, челлендж, расписание, отчёты)
- `ChallengeConfig` - параметры опроса (шаблон вопроса, варианты ответов)
- `PollOption` - вариант ответа (текст, баллы, флаг завершения)
- `ScheduleConfig` - расписание публикаций (частота, время, часовой пояс, дни недели)
- `ReportConfig` - настройки отчётов (частота, день недели, время, сортировка)

**Шаблонизация вопросов:**

- `{date}` - "7 января"
- `{day}` - день месяца
- `{month}` - название месяца
- `{year}` - год
- `{dayOfWeek}` - день недели на русском

### 2. База данных (db/)

#### Схема (PollOptionConfigs.kt)

**Таблица Challenges:**

- Хранит информацию о опубликованных опросах
- Поля: groupName, pollId (уникальный), messageId, chatId, questionText, postedAt

**Таблица PollAnswers:**

- Хранит ответы пользователей на опросы
- Поля: challengeId, userId, userName, firstName, lastName, optionIds, points, isCompleted, answeredAt
- Уникальный индекс: (challengeId, userId) - один ответ на опрос от пользователя

**Таблица PollOptionConfigs:**

- Хранит конфигурацию вариантов ответа для каждого челленджа
- Поля: challengeId, optionIndex, optionText, points, countsAsCompleted
- Уникальный индекс: (challengeId, optionIndex)

#### DatabaseService.kt

- Инициализация H2 базы данных в режиме PostgreSQL совместимости
- Автоматическое создание схемы при запуске
- Предоставляет `query()` метод для выполнения транзакций

### 3. Репозиторий (repository/)

#### ChallengeRepository.kt

Предоставляет методы для работы с данными:

- `saveChallenge()` - сохранить новый челлендж
- `findChallengeByPollId()` - найти челлендж по ID опроса
- `savePollAnswer()` - сохранить/обновить ответ пользователя
- `getUserStatistics()` - получить статистику пользователей за период
- `getTotalChallengesCount()` - подсчёт общего количества челленджей
- `savePollOptionConfig()` - сохранить конфигурацию варианта ответа
- `getPollOptionConfig()` - получить конфигурацию по challengeId и индексу

**Особенности:**

- Upsert логика для ответов (обновление при повторном выборе)
- Агрегация статистики через SQL GROUP BY
- Фильтрация по временным интервалам

### 4. Telegram интеграция (telegram/)

#### TelegramBotService.kt

**Методы:**

- `start()` - запуск long polling, обработка poll answers
- `postChallenge()` - публикация опроса в группу
    - Создание опроса через `sendRegularPoll`
    - Сохранение челленджа в БД
    - Сохранение конфигурации вариантов с индексами
- `sendReport()` - отправка отчёта о статистике
- `handlePollAnswer()` - обработка ответа пользователя
    - Поиск челленджа по pollId
    - Расчёт баллов и флага выполнения
    - Сохранение ответа в БД
- `formatReport()` - форматирование текста отчёта
    - Раздел "Выполнено заданий"
    - Раздел "Очки"
    - Сортировка по настройкам

**Особенности работы с API:**

- Использование типобезопасных ID (ChatId, MessageThreadId, PollId)
- Правильная конвертация Long → RawChatId для создания ChatId
- Извлечение poll из message.content.poll
- Неанонимные опросы для отслеживания пользователей

### 5. Планировщик (scheduler/)

#### ChallengeScheduler.kt

**Функционал:**

- `startScheduling()` - запуск планировщика для группы
    - Создаёт две корутины: для челленджей и отчётов
    - Работает в переданном CoroutineScope
- `stopScheduling()` - остановка всех планировщиков
- `scheduleChallenge()` - планирование публикации челленджей
    - Расчёт следующего времени публикации
    - Форматирование вопроса с подстановкой переменных
    - Публикация через TelegramBotService
- `scheduleReport()` - планирование отправки отчётов
    - Расчёт следующего времени отчёта
    - Получение статистики из репозитория
    - Отправка через TelegramBotService

**Алгоритмы расчёта времени:**

- `calculateNextChallengeTime()` - для DAILY/WEEKLY/CUSTOM частот
- `calculateNextReportTime()` - для DAILY/WEEKLY/MONTHLY частот
- `findNextWeeklyTime()` - поиск следующего дня из списка дней недели
- Поддержка часовых поясов через TimeZone

**Форматирование:**

- Русские названия месяцев и дней недели
- Формат даты "7 января"

### 6. Главное приложение (App.kt)

**Последовательность запуска:**

1. Загрузка конфигурации из файла (путь из аргументов или env)
2. Инициализация базы данных
3. Создание репозитория
4. Создание Telegram бота
5. Создание TelegramBotService
6. Создание планировщика
7. Запуск бота в отдельной корутине
8. Запуск планировщиков для каждой группы
9. Регистрация shutdown hook для graceful shutdown

**Error handling:**

- Ошибка загрузки конфигурации → выход из приложения
- Ошибки в планировщиках → логирование + retry через 1 минуту
- Ошибки отправки → логирование, не ломает работу других групп

## Конфигурация для запуска

### Минимальный config.yaml

```yaml
botToken: "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
databasePath: "./challenge-bot.db"

groups:
  - chatId: -1001234567890
    name: "My Group"
    challenge:
      questionTemplate: "{date} - Daily challenge?"
      options:
        - text: "Done"
          points: 100
          countsAsCompleted: true
        - text: "Skip"
          points: 0
          countsAsCompleted: false
    schedule:
      frequency: DAILY
      time: "09:00:00"
      timezone: "Europe/Moscow"
    report:
      frequency: WEEKLY
      dayOfWeek: MONDAY
      time: "20:00:00"
      timezone: "Europe/Moscow"
```

## Сборка и запуск

### Разработка

```bash
./gradlew run --args="config.yaml"
```

### Продакшн

```bash
./gradlew build
java -jar build/libs/telegram-challenge-bot-1.0.jar config.yaml
```

### GraalVM Native Image

```bash
./gradlew nativeCompile
./build/native/nativeCompile/challenge-bot config.yaml
```

## Возможности для расширения

1. **Дополнительные типы отчётов** - графики, топ-10, прогресс
2. **Webhook вместо Long Polling** - для продакшн деплоя
3. **PostgreSQL** - вместо H2 для масштабирования
4. **Админ команды** - /stats, /reset, /pause
5. **Мультиязычность** - поддержка разных языков
6. **Экспорт статистики** - CSV, Excel
7. **Напоминания** - для пользователей, которые не ответили
8. **Стриксы** - подсчёт последовательных дней выполнения

## Безопасность

- ✅ Токен бота из конфига (не в коде)
- ✅ Валидация входных данных
- ✅ Graceful shutdown
- ⚠️ TODO: Шифрование токена в конфиге
- ⚠️ TODO: Rate limiting для API запросов

## Производительность

- Легковесный: ~50MB RAM (native image)
- Быстрый старт: ~1-2 секунды (native image)
- Minimal CPU usage при idle
- H2 database в файле - no external dependencies

## Лицензия

MIT License

