# Полная документация Audius API

## 📚 Общая информация

**Base URL**: `https://discoveryprovider.audius.co` (или другие хосты)
**Версия API**: v1
**Формат**: REST API, JSON
**Аутентификация**: Большинство эндпоинтов публичные, не требуют авторизации
**Rate Limits**: Нет ограничений (бесплатно)
**Обязательный параметр**: `app_name` - имя вашего приложения

---

## 🎵 TRACKS (Треки)

### 1. Поиск треков
**Endpoint**: `GET /v1/tracks/search`

**Параметры запроса:**
- `query` (string, обязательный) - поисковый запрос
- `limit` (integer, опционально) - количество результатов (по умолчанию 10)
- `offset` (integer, опционально) - смещение для пагинации
- `app_name` (string, обязательный) - имя приложения

**Поля трека (Track Object):**
```json
{
  "id": "string",                    // Уникальный ID трека
  "title": "string",                 // Название трека
  "description": "string",           // Описание трека
  "artwork": {                       // Обложка трека (разные размеры)
    "150x150": "https://...",
    "480x480": "https://...",
    "1000x1000": "https://..."
  },
  "duration": 180,                   // Длительность в секундах
  "genre": "string",                 // Жанр (Electronic, Hip-Hop, Rock и т.д.)
  "mood": "string",                  // Настроение (Energetic, Chill, Dark и т.д.)
  "tags": "string",                  // Теги через запятую
  "release_date": "string",          // Дата релиза (ISO 8601)
  "permalink": "string",             // Ссылка на трек (/track/...)
  "play_count": 12345,               // Количество прослушиваний
  "favorite_count": 567,             // Количество лайков
  "repost_count": 89,                // Количество репостов
  "comment_count": 12,               // Количество комментариев
  "is_streamable": true,             // Можно ли стримить
  "is_downloadable": false,          // Можно ли скачать
  "is_playlist_upload": false,       // Загружен ли как часть плейлиста
  "is_available": true,              // Доступен ли трек
  "is_unlisted": false,              // Скрыт ли из поиска
  "is_premium": false,               // Премиум контент
  "isrc": "string",                  // ISRC код (международный стандарт)
  "iswc": "string",                  // ISWC код (для композиторов)
  "license": "string",               // Лицензия (all-rights-reserved, cc0 и т.д.)
  "field_visibility": {               // Видимость полей
    "genre": true,
    "mood": true,
    "tags": true,
    "share": true,
    "play_count": true,
    "remixes": true
  },
  "remix_of": null,                  // Информация об оригинальном треке (если это ремикс)
  "user": {                          // Информация об артисте (см. раздел Users)
    "id": "string",
    "name": "string",
    "handle": "@artist_handle",
    "profile_picture": {...},
    "cover_photo": {...},
    "bio": "string",
    "location": "string",
    "followee_count": 123,
    "follower_count": 456,
    "track_count": 78,
    "playlist_count": 5,
    "album_count": 3,
    "is_verified": true
  },
  "created_at": "string",           // Дата создания (ISO 8601)
  "updated_at": "string"             // Дата обновления (ISO 8601)
}
```

### 2. Получить трек по ID
**Endpoint**: `GET /v1/tracks/{track_id}`

**Параметры:**
- `track_id` (string, path) - ID трека
- `app_name` (string, query) - имя приложения

**Возвращает**: Полный объект трека

### 3. Получить трендовые треки
**Endpoint**: `GET /v1/tracks/trending`

**Параметры запроса:**
- `genre` (string, опционально) - фильтр по жанру
- `time` (string, опционально) - период времени: `week`, `month`, `year`, `allTime`
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список трендовых треков (топ 100)

### 4. Получить стрим URL трека
**Endpoint**: `GET /v1/tracks/{track_id}/stream`

**Параметры:**
- `track_id` (string, path) - ID трека
- `app_name` (string, query) - имя приложения

**Возвращает**: URL для стриминга аудио файла

### 5. Получить треки пользователя
**Endpoint**: `GET /v1/users/{user_id}/tracks`

**Параметры:**
- `user_id` (string, path) - ID пользователя
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список треков пользователя

---

## 👤 USERS (Пользователи/Артисты)

### 1. Поиск пользователей
**Endpoint**: `GET /v1/users/search`

**Параметры запроса:**
- `query` (string, обязательный) - поисковый запрос
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Поля пользователя (User Object):**
```json
{
  "id": "string",                    // Уникальный ID пользователя
  "name": "string",                  // Имя артиста
  "handle": "@artist_handle",        // Уникальный handle (username)
  "profile_picture": {               // Фотография профиля (разные размеры)
    "150x150": "https://...",
    "480x480": "https://...",
    "1000x1000": "https://..."
  },
  "cover_photo": {                   // Обложка профиля
    "640x": "https://...",
    "2000x": "https://..."
  },
  "bio": "string",                   // Биография артиста
  "location": "string",              // Местоположение (Город, Страна)
  "followee_count": 123,             // Количество подписок
  "follower_count": 456,             // Количество подписчиков
  "track_count": 78,                 // Количество треков
  "playlist_count": 5,               // Количество плейлистов
  "album_count": 3,                  // Количество альбомов
  "is_verified": true,               // Верифицирован ли аккаунт
  "is_deactivated": false,          // Деактивирован ли аккаунт
  "is_available": true,              // Доступен ли аккаунт
  "erc_wallet": "0x...",            // Ethereum кошелек
  "spl_wallet": "...",               // Solana кошелек
  "associated_wallets": [],          // Связанные кошельки
  "associated_sol_wallets": [],     // Связанные Solana кошельки
  "created_at": "string",            // Дата создания (ISO 8601)
  "updated_at": "string"             // Дата обновления (ISO 8601)
}
```

### 2. Получить пользователя по ID
**Endpoint**: `GET /v1/users/{user_id}`

**Параметры:**
- `user_id` (string, path) - ID пользователя
- `app_name` (string, query) - имя приложения

**Возвращает**: Полный объект пользователя

### 3. Получить пользователя по handle
**Endpoint**: `GET /v1/users/handle/{handle}`

**Параметры:**
- `handle` (string, path) - handle пользователя (без @)
- `app_name` (string, query) - имя приложения

**Возвращает**: Полный объект пользователя

### 4. Получить подписчиков пользователя
**Endpoint**: `GET /v1/users/{user_id}/followers`

**Параметры:**
- `user_id` (string, path) - ID пользователя
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список подписчиков

### 5. Получить подписки пользователя
**Endpoint**: `GET /v1/users/{user_id}/following`

**Параметры:**
- `user_id` (string, path) - ID пользователя
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список пользователей, на которых подписан

### 6. Получить избранные треки пользователя
**Endpoint**: `GET /v1/users/{user_id}/favorites`

**Параметры:**
- `user_id` (string, path) - ID пользователя
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список избранных треков

### 7. Получить репосты пользователя
**Endpoint**: `GET /v1/users/{user_id}/reposts`

**Параметры:**
- `user_id` (string, path) - ID пользователя
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список репостов (треки и плейлисты)

---

## 📋 PLAYLISTS (Плейлисты)

### 1. Поиск плейлистов
**Endpoint**: `GET /v1/playlists/search`

**Параметры запроса:**
- `query` (string, обязательный) - поисковый запрос
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Поля плейлиста (Playlist Object):**
```json
{
  "id": "string",                    // Уникальный ID плейлиста
  "playlist_name": "string",         // Название плейлиста
  "description": "string",           // Описание плейлиста
  "artwork": {                       // Обложка плейлиста
    "150x150": "https://...",
    "480x480": "https://...",
    "1000x1000": "https://..."
  },
  "is_album": false,                 // Является ли альбомом
  "is_private": false,               // Приватный ли плейлист
  "is_available": true,              // Доступен ли плейлист
  "playlist_contents": {             // Содержимое плейлиста
    "track_ids": [
      {
        "track": "track_id_1",
        "time": 1234567890
      },
      {
        "track": "track_id_2",
        "time": 1234567891
      }
    ]
  },
  "tracks": [...],                    // Полные объекты треков
  "user": {...},                     // Информация о создателе
  "favorite_count": 123,             // Количество лайков
  "repost_count": 45,                // Количество репостов
  "total_play_count": 5678,          // Общее количество прослушиваний
  "created_at": "string",            // Дата создания
  "updated_at": "string"             // Дата обновления
}
```

### 2. Получить плейлист по ID
**Endpoint**: `GET /v1/playlists/{playlist_id}`

**Параметры:**
- `playlist_id` (string, path) - ID плейлиста
- `app_name` (string, query) - имя приложения

**Возвращает**: Полный объект плейлиста с треками

### 3. Получить трендовые плейлисты
**Endpoint**: `GET /v1/playlists/trending`

**Параметры запроса:**
- `time` (string, опционально) - период: `week`, `month`, `year`, `allTime`
- `type` (string, опционально) - тип: `playlist` или `album`
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `omit_tracks` (boolean, опционально) - не включать треки в ответ
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список трендовых плейлистов/альбомов

### 4. Получить плейлисты пользователя
**Endpoint**: `GET /v1/users/{user_id}/playlists`

**Параметры:**
- `user_id` (string, path) - ID пользователя
- `limit` (integer, опционально) - количество результатов
- `offset` (integer, опционально) - смещение
- `app_name` (string, обязательный) - имя приложения

**Возвращает**: Список плейлистов пользователя

---

## 🔥 TRENDING (Тренды)

### 1. Трендовые треки
**Endpoint**: `GET /v1/tracks/trending`

**Параметры:**
- `genre` (string, опционально) - фильтр по жанру
- `time` (string, опционально) - `week`, `month`, `year`, `allTime`
- `limit` (integer, опционально)
- `offset` (integer, опционально)
- `app_name` (string, обязательный)

### 2. Трендовые плейлисты
**Endpoint**: `GET /v1/playlists/trending`

**Параметры:**
- `time` (string, опционально) - `week`, `month`, `year`, `allTime`
- `type` (string, опционально) - `playlist` или `album`
- `limit` (integer, опционально)
- `offset` (integer, опционально)
- `omit_tracks` (boolean, опционально)
- `app_name` (string, обязательный)

---

## 🎨 ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ

### Изображения (Artwork/Profile Picture)

Все изображения в Audius API возвращаются в формате объекта с разными размерами:

```json
{
  "150x150": "https://...",    // Маленькое изображение
  "480x480": "https://...",    // Среднее изображение
  "1000x1000": "https://..."   // Большое изображение
}
```

**Для обложек треков (artwork):**
- `150x150` - миниатюра
- `480x480` - стандартное качество
- `1000x1000` - высокое качество

**Для фотографий профиля (profile_picture):**
- `150x150` - миниатюра
- `480x480` - стандартное качество
- `1000x1000` - высокое качество
- `200x200` - альтернативный размер

**Для обложек профиля (cover_photo):**
- `640x` - стандартная ширина
- `2000x` - высокая ширина

### Жанры (Genres)

Популярные жанры в Audius:
- Electronic
- Hip-Hop
- Rock
- Pop
- Jazz
- Classical
- Country
- R&B
- Reggae
- Metal
- Folk
- Blues
- и другие...

### Настроения (Moods)

Примеры настроений:
- Energetic
- Chill
- Dark
- Happy
- Sad
- Romantic
- Aggressive
- и другие...

---

## 📊 СТАТИСТИКА И МЕТРИКИ

### Для треков:
- `play_count` - общее количество прослушиваний
- `favorite_count` - количество лайков
- `repost_count` - количество репостов
- `comment_count` - количество комментариев

### Для пользователей:
- `followee_count` - количество подписок
- `follower_count` - количество подписчиков
- `track_count` - количество треков
- `playlist_count` - количество плейлистов
- `album_count` - количество альбомов

### Для плейлистов:
- `favorite_count` - количество лайков
- `repost_count` - количество репостов
- `total_play_count` - общее количество прослушиваний всех треков

---

## 🔗 ПОЛЕЗНЫЕ ССЫЛКИ

- **Официальная документация**: https://docs.audius.org/api
- **Swagger UI (Basic API)**: https://api.audius.co/docs
- **Swagger UI (Full API)**: https://api.audius.co/full/docs
- **Postman Collection**: https://www.postman.com/samgutentag/audius-devs/documentation/7j0b9qz/audius-api

---

## 💡 ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ

### Поиск треков
```
GET https://discoveryprovider.audius.co/v1/tracks/search?query=electronic&limit=20&app_name=LumiSound
```

### Получить трендовые треки за неделю
```
GET https://discoveryprovider.audius.co/v1/tracks/trending?time=week&limit=50&app_name=LumiSound
```

### Получить информацию об артисте
```
GET https://discoveryprovider.audius.co/v1/users/{user_id}?app_name=LumiSound
```

### Получить треки артиста
```
GET https://discoveryprovider.audius.co/v1/users/{user_id}/tracks?limit=50&app_name=LumiSound
```

### Получить стрим URL трека
```
GET https://discoveryprovider.audius.co/v1/tracks/{track_id}/stream?app_name=LumiSound
```

---

## ⚠️ ВАЖНЫЕ ЗАМЕЧАНИЯ

1. **Всегда включайте `app_name`** в запросы - это обязательный параметр
2. **Нет rate limits** - API бесплатный, но используйте разумно
3. **Множественные хосты** - если один хост не работает, попробуйте другой
4. **Изображения могут быть null** - всегда проверяйте наличие URL перед использованием
5. **Некоторые поля могут быть null** - используйте безопасные значения по умолчанию

---

## 🎯 ЧТО УЖЕ РЕАЛИЗОВАНО В ПРОЕКТЕ

✅ Поиск треков (`/v1/tracks/search`)
✅ Извлечение обложек треков (artwork)
✅ Извлечение фотографий артистов (profile_picture)
✅ Получение стрим URL (`/v1/tracks/{track_id}/stream`)

## 🚀 ЧТО МОЖНО ДОБАВИТЬ

- Получение трендовых треков
- Поиск артистов
- Получение информации об артисте по ID
- Получение треков артиста
- Поиск плейлистов
- Получение плейлиста с треками
- Получение избранных треков пользователя
- Получение подписчиков/подписок артиста
