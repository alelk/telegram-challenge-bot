# Quick Start Guide - Telegram Challenge Bot

## Быстрый старт за 5 минут

### Шаг 1: Создание бота в Telegram

1. Найдите [@BotFather](https://t.me/botfather) в Telegram
2. Отправьте команду `/newbot`
3. Придумайте имя для бота (например: "My Challenge Bot")
4. Придумайте username (должен заканчиваться на "bot", например: "my_challenge_bot")
5. Сохраните токен, который даст BotFather (формат: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)

### Шаг 2: Добавление бота в группу

1. Создайте группу в Telegram или откройте существующую
2. Добавьте вашего бота в группу (через меню → Add members)
3. Сделайте бота администратором (опционально, но рекомендуется)

### Шаг 3: Получение Chat ID группы

**Способ 1: Через raw_data_bot**
1. Добавьте [@raw_data_bot](https://t.me/raw_data_bot) в вашу группу
2. Отправьте любое сообщение
3. Бот покажет JSON, найдите `"chat":{"id":-1001234567890,...}`
4. Скопируйте значение `id` (с минусом!)

**Способ 2: Через API**
1. Отправьте сообщение в группу
2. Откройте в браузере: `https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates`
3. Найдите в JSON: `"chat":{"id":-1001234567890,...}`

### Шаг 4: Настройка конфигурации

Создайте файл `config.yaml.local`:

```yaml
botToken: "ВСТАВЬТЕ_ВАШ_ТОКЕН_СЮДА"
databasePath: "./challenge-bot.db"

groups:
  - chatId: -1001234567890  # ЗАМЕНИТЕ НА ВАШ CHAT ID
    threadId: null  # Оставьте null если не используете темы
    name: "Мой челлендж"
    
    challenge:
      questionTemplate: "{date} - Отжимания, приседания, пресс?"
      isAnonymous: false
      allowsMultipleAnswers: false
      options:
        - text: "Сделал по 25"
          points: 25
          countsAsCompleted: true
        - text: "Сделал по 50"
          points: 50
          countsAsCompleted: true
        - text: "Сделал по 75"
          points: 75
          countsAsCompleted: true
        - text: "Сделал по 100"
          points: 100
          countsAsCompleted: true
        - text: "Сделаю потом"
          points: 0
          countsAsCompleted: false
    
    schedule:
      frequency: DAILY
      time: "09:00:00"  # 9 утра по московскому времени
      timezone: "Europe/Moscow"
    
    report:
      frequency: WEEKLY
      dayOfWeek: MONDAY
      time: "20:00:00"  # 8 вечера по московскому времени
      timezone: "Europe/Moscow"
      includeCompletionStats: true
      includePointsStats: true
      sortBy: POINTS_DESC
```

### Шаг 5: Запуск бота

**Вариант A: Через Gradle (для разработки)**
```bash
./gradlew run --args="config.yaml.local"
```

**Вариант B: JAR файл**
```bash
./gradlew build
java -jar build/libs/telegram-challenge-bot-1.0.jar config.yaml.local
```

**Вариант C: Native Image (самый быстрый)**
```bash
./gradlew nativeCompile
./build/native/nativeCompile/challenge-bot config.yaml.local
```

### Шаг 6: Проверка работы

1. Дождитесь времени публикации челленджа (или измените время на ближайшее в конфиге)
2. Бот опубликует опрос в группу
3. Ответьте на опрос
4. Дождитесь времени отчёта - бот пришлёт статистику

## Примеры конфигураций

### Пример 1: Ежедневный фитнес-челлендж

```yaml
botToken: "YOUR_TOKEN"
databasePath: "./fitness-bot.db"

groups:
  - chatId: -1001234567890
    name: "Fitness Team"
    challenge:
      questionTemplate: "{date} ({dayOfWeek}) - Тренировка выполнена?"
      options:
        - text: "✅ Да, полностью"
          points: 100
          countsAsCompleted: true
        - text: "⚡ Частично"
          points: 50
          countsAsCompleted: true
        - text: "❌ Пропустил"
          points: 0
          countsAsCompleted: false
    schedule:
      frequency: DAILY
      time: "20:00:00"
      timezone: "Europe/Moscow"
    report:
      frequency: WEEKLY
      dayOfWeek: SUNDAY
      time: "21:00:00"
      timezone: "Europe/Moscow"
      sortBy: COMPLETION_DESC
```

### Пример 2: Учёба - челлендж по будням

```yaml
botToken: "YOUR_TOKEN"
databasePath: "./study-bot.db"

groups:
  - chatId: -1001234567890
    name: "Study Group"
    challenge:
      questionTemplate: "{date} - Позанимался сегодня?"
      options:
        - text: "📚 3+ часа"
          points: 100
          countsAsCompleted: true
        - text: "📖 2 часа"
          points: 75
          countsAsCompleted: true
        - text: "📝 1 час"
          points: 50
          countsAsCompleted: true
        - text: "😴 Не занимался"
          points: 0
          countsAsCompleted: false
    schedule:
      frequency: CUSTOM
      time: "22:00:00"
      timezone: "Europe/Moscow"
      daysOfWeek: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
    report:
      frequency: WEEKLY
      dayOfWeek: FRIDAY
      time: "23:00:00"
      timezone: "Europe/Moscow"
```

### Пример 3: Несколько групп

```yaml
botToken: "YOUR_TOKEN"
databasePath: "./multi-bot.db"

groups:
  # Группа 1: Фитнес
  - chatId: -1001111111111
    name: "Fitness"
    challenge:
      questionTemplate: "{date} - Тренировка?"
      options:
        - text: "Да"
          points: 100
          countsAsCompleted: true
        - text: "Нет"
          points: 0
          countsAsCompleted: false
    schedule:
      frequency: DAILY
      time: "20:00:00"
      timezone: "Europe/Moscow"
    report:
      frequency: WEEKLY
      dayOfWeek: MONDAY
      time: "09:00:00"
      timezone: "Europe/Moscow"
  
  # Группа 2: Чтение
  - chatId: -1002222222222
    name: "Reading"
    challenge:
      questionTemplate: "{date} - Читал книгу?"
      options:
        - text: "30+ страниц"
          points: 100
          countsAsCompleted: true
        - text: "15+ страниц"
          points: 50
          countsAsCompleted: true
        - text: "Не читал"
          points: 0
          countsAsCompleted: false
    schedule:
      frequency: DAILY
      time: "21:00:00"
      timezone: "Europe/Moscow"
    report:
      frequency: WEEKLY
      dayOfWeek: SUNDAY
      time: "20:00:00"
      timezone: "Europe/Moscow"
```

## Часовые пояса

Используйте стандартные идентификаторы часовых поясов:

- `Europe/Moscow` - Москва (UTC+3)
- `Europe/London` - Лондон (UTC+0)
- `America/New_York` - Нью-Йорк (UTC-5)
- `Asia/Tokyo` - Токио (UTC+9)
- `UTC` - Всемирное координированное время

Полный список: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

## Часто задаваемые вопросы

### Q: Бот не публикует опросы
**A:** Проверьте:
- Правильность токена бота
- Chat ID (должен быть с минусом для групп)
- Бот добавлен в группу
- Время в конфиге (возможно, уже прошло)
- Логи приложения

### Q: Как изменить время публикации?
**A:** Измените поле `time` в секции `schedule` конфига и перезапустите бота

### Q: Можно ли использовать в супергруппах с темами?
**A:** Да, укажите `threadId` в конфиге группы

### Q: Как остановить бота?
**A:** Нажмите Ctrl+C или отправьте SIGTERM сигнал процессу

### Q: Где хранятся данные?
**A:** В файле базы данных, путь указан в `databasePath` (по умолчанию `./challenge-bot.db`)

### Q: Можно ли запустить на сервере?
**A:** Да, используйте systemd service или Docker для автозапуска

## Деплой на сервер (Linux)

### Создание systemd сервиса

1. Создайте файл `/etc/systemd/system/challenge-bot.service`:

```ini
[Unit]
Description=Telegram Challenge Bot
After=network.target

[Service]
Type=simple
User=YOUR_USER
WorkingDirectory=/opt/challenge-bot
ExecStart=/opt/challenge-bot/challenge-bot /opt/challenge-bot/config.yaml
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

2. Скопируйте файлы:
```bash
sudo mkdir -p /opt/challenge-bot
sudo cp build/native/nativeCompile/challenge-bot /opt/challenge-bot/
sudo cp config.yaml.local /opt/challenge-bot/config.yaml
sudo chown -R YOUR_USER:YOUR_USER /opt/challenge-bot
```

3. Запустите сервис:
```bash
sudo systemctl daemon-reload
sudo systemctl enable challenge-bot
sudo systemctl start challenge-bot
sudo systemctl status challenge-bot
```

4. Просмотр логов:
```bash
sudo journalctl -u challenge-bot -f
```

## Полезные команды

```bash
# Проверка синтаксиса конфига (через попытку запуска)
./gradlew run --args="config.yaml.local" & sleep 5 && kill %1

# Просмотр содержимого базы данных
sqlite3 challenge-bot.db "SELECT * FROM challenges;"

# Бэкап базы данных
cp challenge-bot.db challenge-bot.db.backup

# Очистка базы данных (начать с нуля)
rm challenge-bot.db
```

## Поддержка

Если возникли проблемы:
1. Проверьте логи приложения
2. Убедитесь, что конфигурация корректна
3. Проверьте права бота в группе
4. Создайте Issue в GitHub с описанием проблемы

Удачи с вашим челленджем! 🚀

