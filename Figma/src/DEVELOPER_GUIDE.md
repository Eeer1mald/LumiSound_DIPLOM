# Руководство для разработчиков LumiSound

## 📁 Структура проекта

```
/
├── App.tsx                          # Главный компонент приложения
├── components/
│   ├── HomeScreen.tsx              # Главный экран
│   ├── NowPlayingScreen.tsx        # Экран воспроизведения
│   ├── RatingsScreen.tsx           # Экран оценок
│   ├── SearchScreen.tsx            # Поиск
│   ├── PlaylistsScreen.tsx         # Плейлисты
│   ├── ProfileScreen.tsx           # Профиль
│   ├── BottomNavigation.tsx        # Нижняя навигация
│   ├── TrackCard.tsx               # Карточка трека
│   ├── SearchBar.tsx               # Поле поиска
│   ├── MobileFrame.tsx             # Рамка мобильного устройства
│   ├── AndroidStatusBar.tsx        # Статус-бар Android
│   ├── LumiSoundLogo.tsx           # Логотип
│   ├── AllScreensDemo.tsx          # Демо всех экранов
│   └── auth/
│       ├── AuthScreens.tsx         # Экраны авторизации
│       ├── WelcomeScreen.tsx       # Приветствие
│       ├── LoginScreen.tsx         # Вход
│       └── SignUpScreen.tsx        # Регистрация
├── utils/
│   └── mockData.ts                 # Моковые данные и утилиты
├── styles/
│   └── globals.css                 # Глобальные стили
├── README.md                        # Основная документация
├── DESIGN_SYSTEM.md                # Дизайн-система
├── USAGE.md                        # Инструкции по использованию
└── DEVELOPER_GUIDE.md              # Это руководство
```

## 🔧 Основные концепции

### 1. Навигация

Навигация реализована через состояние в `App.tsx`:

```tsx
type Screen = 'home' | 'search' | 'playlists' | 'profile' | 'now-playing' | 'ratings';
const [currentScreen, setCurrentScreen] = useState<Screen>('home');
```

**Переключение экранов:**
```tsx
const handleNavigate = (screen: 'home' | 'search' | 'playlists' | 'profile') => {
  setCurrentScreen(screen);
};
```

### 2. Работа с треками

**Открытие трека для воспроизведения:**
```tsx
const handleTrackClick = (trackId: number) => {
  const track = getTrackById(trackId);
  if (track) {
    setCurrentTrack(track);
    setCurrentScreen('now-playing');
  }
};
```

### 3. Моковые данные

Все данные находятся в `/utils/mockData.ts`:

```tsx
import { mockTracks, mockRatedTracks, mockPlaylists, mockUserStats } from './utils/mockData';
```

**Утилиты:**
- `getTrackById(id)` - получить трек по ID
- `getPlaylistById(id)` - получить плейлист по ID
- `formatTime(seconds)` - форматировать время

## 🎨 Работа с компонентами

### Создание нового экрана

```tsx
import React from 'react';
import { MobileFrame } from './MobileFrame';
import { BottomNavigation } from './BottomNavigation';
import { LumiSoundLogo } from './LumiSoundLogo';

interface MyScreenProps {
  onNavigate: (screen: 'home' | 'search' | 'playlists' | 'profile') => void;
}

export function MyScreen({ onNavigate }: MyScreenProps) {
  return (
    <MobileFrame>
      <div className="h-full flex flex-col bg-[#0F1020]">
        {/* Header */}
        <div className="px-4 py-3 bg-[#0F1020]/80 backdrop-blur-sm border-b border-[#2A2D3E]/20">
          <LumiSoundLogo width={112} />
          <h1 className="text-[#E6E6EB] text-2xl">Мой экран</h1>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto px-4 pb-4">
          {/* Ваш контент */}
        </div>

        {/* Bottom Navigation */}
        <BottomNavigation 
          activeItem="home" 
          onItemClick={onNavigate}
        />
      </div>
    </MobileFrame>
  );
}
```

### Использование карточек

```tsx
import { TrackCard } from './TrackCard';

<TrackCard
  title="Название трека"
  artist="Исполнитель"
  imageUrl="https://..."
  onClick={() => handleTrackClick(1)}
/>
```

### Градиентные кнопки

```tsx
<button className="bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] 
                   text-white py-3 px-6 rounded-full 
                   shadow-[0_8px_24px_rgba(123,109,255,0.3)] 
                   active:scale-95 transition-transform">
  Кнопка
</button>
```

## 🔌 Интеграция с бэкендом

### Шаг 1: Создание API слоя

Создайте `/api/client.ts`:

```tsx
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:3000/api';

export async function fetchTracks() {
  const response = await fetch(`${API_BASE_URL}/tracks`);
  return response.json();
}

export async function rateTrack(trackId: number, rating: number) {
  const response = await fetch(`${API_BASE_URL}/tracks/${trackId}/rate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rating }),
  });
  return response.json();
}
```

### Шаг 2: Замена моковых данных

```tsx
import { useEffect, useState } from 'react';
import { fetchTracks } from './api/client';

export default function App() {
  const [tracks, setTracks] = useState([]);

  useEffect(() => {
    fetchTracks().then(setTracks);
  }, []);

  // ...
}
```

### Шаг 3: Добавление состояния загрузки

```tsx
const [isLoading, setIsLoading] = useState(true);
const [error, setError] = useState(null);

useEffect(() => {
  setIsLoading(true);
  fetchTracks()
    .then(setTracks)
    .catch(setError)
    .finally(() => setIsLoading(false));
}, []);
```

## 🎵 Добавление аудио плеера

### Установка библиотеки

```bash
npm install howler
```

### Создание аудио контекста

Создайте `/contexts/AudioContext.tsx`:

```tsx
import React, { createContext, useContext, useState } from 'react';
import { Howl } from 'howler';

interface AudioContextType {
  play: (url: string) => void;
  pause: () => void;
  isPlaying: boolean;
}

const AudioContext = createContext<AudioContextType | null>(null);

export function AudioProvider({ children }) {
  const [sound, setSound] = useState<Howl | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  const play = (url: string) => {
    if (sound) sound.unload();
    
    const newSound = new Howl({
      src: [url],
      onplay: () => setIsPlaying(true),
      onpause: () => setIsPlaying(false),
    });
    
    newSound.play();
    setSound(newSound);
  };

  const pause = () => {
    if (sound) sound.pause();
  };

  return (
    <AudioContext.Provider value={{ play, pause, isPlaying }}>
      {children}
    </AudioContext.Provider>
  );
}

export const useAudio = () => useContext(AudioContext);
```

### Использование в компоненте

```tsx
import { useAudio } from '../contexts/AudioContext';

export function NowPlayingScreen({ track }) {
  const { play, pause, isPlaying } = useAudio();

  const handlePlayPause = () => {
    if (isPlaying) {
      pause();
    } else {
      play(track.audioUrl);
    }
  };

  // ...
}
```

## 🗄️ Работа с базой данных (Supabase)

### Установка Supabase

```bash
npm install @supabase/supabase-js
```

### Инициализация клиента

Создайте `/lib/supabase.ts`:

```tsx
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.REACT_APP_SUPABASE_URL!;
const supabaseAnonKey = process.env.REACT_APP_SUPABASE_ANON_KEY!;

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
```

### Структура таблиц

```sql
-- Таблица треков
CREATE TABLE tracks (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  title TEXT NOT NULL,
  artist TEXT NOT NULL,
  cover_url TEXT,
  audio_url TEXT,
  duration INTEGER,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Таблица оценок
CREATE TABLE ratings (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  user_id UUID REFERENCES auth.users(id),
  track_id BIGINT REFERENCES tracks(id),
  rating INTEGER CHECK (rating >= 1 AND rating <= 10),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  UNIQUE(user_id, track_id)
);

-- Таблица плейлистов
CREATE TABLE playlists (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  user_id UUID REFERENCES auth.users(id),
  name TEXT NOT NULL,
  cover_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Связь плейлистов и треков
CREATE TABLE playlist_tracks (
  playlist_id BIGINT REFERENCES playlists(id) ON DELETE CASCADE,
  track_id BIGINT REFERENCES tracks(id) ON DELETE CASCADE,
  added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  PRIMARY KEY (playlist_id, track_id)
);
```

### Примеры запросов

```tsx
// Получить все треки
const { data: tracks } = await supabase
  .from('tracks')
  .select('*');

// Оценить трек
const { data, error } = await supabase
  .from('ratings')
  .upsert({ 
    user_id: user.id, 
    track_id: trackId, 
    rating: rating 
  });

// Получить оценки пользователя
const { data: ratings } = await supabase
  .from('ratings')
  .select('*, tracks(*)')
  .eq('user_id', user.id)
  .order('created_at', { ascending: false });
```

## 🔐 Аутентификация

### Подключение экранов авторизации

```tsx
import { useState } from 'react';
import { supabase } from './lib/supabase';
import { WelcomeScreen } from './components/WelcomeScreen';
import { LoginScreen } from './components/LoginScreen';

export default function App() {
  const [session, setSession] = useState(null);

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
    });
  }, []);

  if (!session) {
    return <LoginScreen onLogin={handleLogin} />;
  }

  return <MainApp />;
}
```

## 📊 Аналитика и статистика

### Вычисление статистики

```tsx
// Средняя оценка
const averageRating = ratings.reduce((sum, r) => sum + r.rating, 0) / ratings.length;

// Треки с высокой оценкой
const topRatedTracks = ratings.filter(r => r.rating >= 8);

// Самый популярный жанр
const genreStats = tracks.reduce((acc, track) => {
  acc[track.genre] = (acc[track.genre] || 0) + 1;
  return acc;
}, {});
```

## 🎯 Best Practices

1. **Используйте TypeScript** для типобезопасности
2. **Создавайте переиспользуемые компоненты**
3. **Выносите константы** в отдельные файлы
4. **Обрабатывайте ошибки** в API запросах
5. **Добавляйте loading состояния** для UX
6. **Оптимизируйте изображения** (используйте CDN)
7. **Кэшируйте данные** где возможно
8. **Используйте React Query** для работы с API

## 🐛 Отладка

### Полезные инструменты

```tsx
// Логирование состояния
console.log('Current screen:', currentScreen);
console.log('Current track:', currentTrack);

// React DevTools
// Установите расширение React Developer Tools для браузера
```

## 📱 Адаптивность

Приложение оптимизировано для мобильных устройств (360×800px), но может быть адаптировано для планшетов:

```tsx
<div className="w-[360px] md:w-[768px] lg:w-[1024px]">
  {/* Контент */}
</div>
```

## 🚀 Развертывание

### Vercel

```bash
npm install -g vercel
vercel
```

### Netlify

```bash
npm run build
# Загрузите папку build/ в Netlify
```

## 📝 Дальнейшее развитие

- [ ] Offline режим с PWA
- [ ] Push уведомления
- [ ] Социальные функции (шаринг плейлистов)
- [ ] Интеграция с внешними музыкальными API (Spotify, Last.fm)
- [ ] Эквалайзер
- [ ] Тексты песен
- [ ] Подкасты
- [ ] Радио
