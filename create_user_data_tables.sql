-- ============================================================
-- СОЗДАНИЕ ТАБЛИЦ ДЛЯ Пользовательских данных LUMISOUND
-- ============================================================
-- 
-- ЧТО ДЕЛАЕТ ЭТОТ СКРИПТ:
-- 1. Создает таблицу favorite_tracks для избранных треков пользователя
-- 2. Создает таблицу favorite_artists для любимых артистов
-- 3. Создает таблицу track_history для истории прослушиваний (для статистики)
-- 4. Настраивает безопасность (RLS - Row Level Security)
-- 5. Создает индексы для быстрого поиска
-- 
-- ВАЖНО: Эти таблицы связаны с auth.users через user_id
-- ============================================================

-- ============================================================
-- ТАБЛИЦА 1: favorite_tracks (Избранные треки)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.favorite_tracks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Данные трека (минимальные, основная информация из внешних API)
    track_id TEXT NOT NULL, -- ID трека из внешнего API (Audius)
    track_title TEXT NOT NULL,
    track_artist TEXT NOT NULL,
    track_cover_url TEXT,
    track_preview_url TEXT,
    
    -- Метаданные
    added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Уникальность: один трек может быть избранным только один раз для пользователя
    UNIQUE(user_id, track_id)
);

-- Индекс для быстрого поиска избранных треков пользователя
CREATE INDEX IF NOT EXISTS idx_favorite_tracks_user_id ON public.favorite_tracks(user_id);
CREATE INDEX IF NOT EXISTS idx_favorite_tracks_added_at ON public.favorite_tracks(added_at DESC);

-- Включение RLS
ALTER TABLE public.favorite_tracks ENABLE ROW LEVEL SECURITY;

-- Политики безопасности для favorite_tracks
DROP POLICY IF EXISTS "Users can read own favorite tracks" ON public.favorite_tracks;
DROP POLICY IF EXISTS "Users can insert own favorite tracks" ON public.favorite_tracks;
DROP POLICY IF EXISTS "Users can delete own favorite tracks" ON public.favorite_tracks;

CREATE POLICY "Users can read own favorite tracks"
    ON public.favorite_tracks FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own favorite tracks"
    ON public.favorite_tracks FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own favorite tracks"
    ON public.favorite_tracks FOR DELETE
    USING (auth.uid() = user_id);

-- ============================================================
-- ТАБЛИЦА 2: favorite_artists (Любимые артисты)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.favorite_artists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Данные артиста (минимальные, основная информация из внешних API)
    artist_id TEXT NOT NULL, -- ID артиста из внешнего API (Audius)
    artist_name TEXT NOT NULL,
    artist_image_url TEXT,
    
    -- Метаданные
    added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    play_count INTEGER DEFAULT 0, -- Счетчик прослушиваний для сортировки
    
    -- Уникальность: один артист может быть избранным только один раз для пользователя
    UNIQUE(user_id, artist_id)
);

-- Индекс для быстрого поиска любимых артистов пользователя
CREATE INDEX IF NOT EXISTS idx_favorite_artists_user_id ON public.favorite_artists(user_id);
CREATE INDEX IF NOT EXISTS idx_favorite_artists_play_count ON public.favorite_artists(play_count DESC);

-- Включение RLS
ALTER TABLE public.favorite_artists ENABLE ROW LEVEL SECURITY;

-- Политики безопасности для favorite_artists
DROP POLICY IF EXISTS "Users can read own favorite artists" ON public.favorite_artists;
DROP POLICY IF EXISTS "Users can insert own favorite artists" ON public.favorite_artists;
DROP POLICY IF EXISTS "Users can update own favorite artists" ON public.favorite_artists;
DROP POLICY IF EXISTS "Users can delete own favorite artists" ON public.favorite_artists;

CREATE POLICY "Users can read own favorite artists"
    ON public.favorite_artists FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own favorite artists"
    ON public.favorite_artists FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own favorite artists"
    ON public.favorite_artists FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own favorite artists"
    ON public.favorite_artists FOR DELETE
    USING (auth.uid() = user_id);

-- ============================================================
-- ТАБЛИЦА 3: track_history (История прослушиваний)
-- ============================================================
-- Используется для статистики "Любимые треки за последнее время"
-- и "Часто прослушиваемые артисты"
CREATE TABLE IF NOT EXISTS public.track_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Данные трека
    track_id TEXT NOT NULL,
    track_title TEXT NOT NULL,
    track_artist TEXT NOT NULL,
    track_artist_id TEXT, -- ID артиста для связи с favorite_artists
    
    -- Метаданные
    played_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Индексы для быстрой статистики
CREATE INDEX IF NOT EXISTS idx_track_history_user_id ON public.track_history(user_id);
CREATE INDEX IF NOT EXISTS idx_track_history_played_at ON public.track_history(played_at DESC);
CREATE INDEX IF NOT EXISTS idx_track_history_user_track ON public.track_history(user_id, track_id);

-- Включение RLS
ALTER TABLE public.track_history ENABLE ROW LEVEL SECURITY;

-- Политики безопасности для track_history
DROP POLICY IF EXISTS "Users can read own track history" ON public.track_history;
DROP POLICY IF EXISTS "Users can insert own track history" ON public.track_history;
DROP POLICY IF EXISTS "Users can delete own track history" ON public.track_history;

CREATE POLICY "Users can read own track history"
    ON public.track_history FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own track history"
    ON public.track_history FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own track history"
    ON public.track_history FOR DELETE
    USING (auth.uid() = user_id);

-- ============================================================
-- ФУНКЦИЯ: Обновление счетчика прослушиваний артиста
-- ============================================================
-- Эта функция автоматически увеличивает play_count для артиста
-- когда пользователь прослушивает трек
CREATE OR REPLACE FUNCTION update_artist_play_count()
RETURNS TRIGGER AS $$
BEGIN
    -- Если артист уже в избранном, увеличиваем счетчик
    UPDATE public.favorite_artists
    SET play_count = play_count + 1
    WHERE user_id = NEW.user_id 
      AND artist_id = NEW.track_artist_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггер для автоматического обновления счетчика
DROP TRIGGER IF EXISTS trigger_update_artist_play_count ON public.track_history;
CREATE TRIGGER trigger_update_artist_play_count
    AFTER INSERT ON public.track_history
    FOR EACH ROW
    WHEN (NEW.track_artist_id IS NOT NULL)
    EXECUTE FUNCTION update_artist_play_count();

-- ============================================================
-- ФУНКЦИЯ: Очистка старых записей истории
-- ============================================================
-- Очищает записи истории старше 30 дней (опционально)
CREATE OR REPLACE FUNCTION cleanup_old_track_history()
RETURNS void AS $$
BEGIN
    DELETE FROM public.track_history
    WHERE played_at < NOW() - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- ГОТОВО! Таблицы созданы и настроены
-- ============================================================
-- После выполнения этого скрипта:
-- 1. Таблицы favorite_tracks, favorite_artists, track_history созданы
-- 2. Безопасность настроена (пользователи видят только свои данные)
-- 3. Индексы созданы для быстрого поиска
-- 4. Автоматическое обновление счетчиков работает
-- 5. Приложение сможет сохранять и получать пользовательские данные
-- ============================================================
