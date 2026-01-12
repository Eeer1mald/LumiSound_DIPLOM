-- ============================================================
-- ОБНОВЛЕНИЕ ТАБЛИЦ ДЛЯ ОТСЛЕЖИВАНИЯ ПРОСЛУШИВАНИЙ
-- ============================================================
-- 
-- ИНСТРУКЦИЯ:
-- 1. Откройте Supabase Dashboard (https://supabase.com/dashboard)
-- 2. Выберите ваш проект
-- 3. Перейдите в раздел "SQL Editor" (в левом меню)
-- 4. Вставьте весь этот скрипт в редактор
-- 5. Нажмите "Run" или "Execute"
-- 
-- Этот скрипт добавит поле play_count в favorite_tracks
-- и создаст функцию для автоматического обновления счетчиков
-- ============================================================

-- ШАГ 1: Добавление поля play_count в favorite_tracks (если его еще нет)
DO $$
BEGIN
    -- Проверяем, существует ли колонка play_count
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'favorite_tracks' 
        AND column_name = 'play_count'
    ) THEN
        ALTER TABLE public.favorite_tracks 
        ADD COLUMN play_count integer DEFAULT 0;
        
        -- Обновляем существующие записи
        UPDATE public.favorite_tracks 
        SET play_count = 0 
        WHERE play_count IS NULL;
        
        RAISE NOTICE 'Колонка play_count добавлена в favorite_tracks';
    ELSE
        RAISE NOTICE 'Колонка play_count уже существует в favorite_tracks';
    END IF;
END $$;

-- ШАГ 2: Создание функции для увеличения счетчика прослушиваний трека
CREATE OR REPLACE FUNCTION increment_track_play_count(
    p_user_id uuid,
    p_track_id text,
    p_track_title text,
    p_track_artist text,
    p_track_cover_url text DEFAULT NULL,
    p_track_preview_url text DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Обновляем или вставляем запись в favorite_tracks
    INSERT INTO public.favorite_tracks (
        user_id,
        track_id,
        track_title,
        track_artist,
        track_cover_url,
        track_preview_url,
        play_count
    )
    VALUES (
        p_user_id,
        p_track_id,
        p_track_title,
        p_track_artist,
        NULLIF(p_track_cover_url, ''), -- Преобразуем пустую строку в NULL
        NULLIF(p_track_preview_url, ''), -- Преобразуем пустую строку в NULL
        1
    )
    ON CONFLICT (user_id, track_id)
    DO UPDATE SET 
        play_count = favorite_tracks.play_count + 1,
        track_title = EXCLUDED.track_title,
        track_artist = EXCLUDED.track_artist,
        track_cover_url = COALESCE(NULLIF(EXCLUDED.track_cover_url, ''), favorite_tracks.track_cover_url),
        track_preview_url = COALESCE(NULLIF(EXCLUDED.track_preview_url, ''), favorite_tracks.track_preview_url);
END;
$$;

-- ШАГ 3: Создание функции для увеличения счетчика прослушиваний артиста
CREATE OR REPLACE FUNCTION increment_artist_play_count(
    p_user_id uuid,
    p_artist_id text,
    p_artist_name text,
    p_artist_image_url text DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Обновляем или вставляем запись в favorite_artists
    INSERT INTO public.favorite_artists (
        user_id,
        artist_id,
        artist_name,
        artist_image_url,
        play_count
    )
    VALUES (
        p_user_id,
        p_artist_id,
        p_artist_name,
        NULLIF(p_artist_image_url, ''), -- Преобразуем пустую строку в NULL
        1
    )
    ON CONFLICT (user_id, artist_id)
    DO UPDATE SET 
        play_count = favorite_artists.play_count + 1,
        artist_name = EXCLUDED.artist_name,
        artist_image_url = COALESCE(NULLIF(EXCLUDED.artist_image_url, ''), favorite_artists.artist_image_url);
END;
$$;

-- ШАГ 4: Добавление уникального ограничения для favorite_tracks (если его еще нет)
-- Это нужно для ON CONFLICT в функции
-- ПРИМЕЧАНИЕ: Если ограничение уже существует, этот блок просто пропустится
DO $$
BEGIN
    -- Проверяем, существует ли уникальное ограничение
    IF NOT EXISTS (
        SELECT 1 
        FROM pg_constraint 
        WHERE conname = 'favorite_tracks_user_track_unique'
    ) THEN
        BEGIN
            -- Пытаемся создать уникальное ограничение
            ALTER TABLE public.favorite_tracks 
            ADD CONSTRAINT favorite_tracks_user_track_unique UNIQUE (user_id, track_id);
            
            RAISE NOTICE 'Уникальное ограничение создано для favorite_tracks';
        EXCEPTION
            WHEN duplicate_object THEN
                RAISE NOTICE 'Уникальное ограничение favorite_tracks_user_track_unique уже существует (обработано исключение)';
        END;
    ELSE
        RAISE NOTICE 'Уникальное ограничение уже существует для favorite_tracks';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        -- Если возникла любая другая ошибка, просто игнорируем (ограничение уже существует)
        RAISE NOTICE 'Ограничение favorite_tracks_user_track_unique уже существует или не может быть создано: %', SQLERRM;
END $$;

-- ШАГ 5: Добавление уникального ограничения для favorite_artists (если его еще нет)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM pg_constraint 
        WHERE conname = 'favorite_artists_user_artist_unique'
    ) THEN
        BEGIN
            -- Пытаемся создать уникальное ограничение
            ALTER TABLE public.favorite_artists 
            ADD CONSTRAINT favorite_artists_user_artist_unique UNIQUE (user_id, artist_id);
            
            RAISE NOTICE 'Уникальное ограничение создано для favorite_artists';
        EXCEPTION
            WHEN duplicate_object THEN
                RAISE NOTICE 'Уникальное ограничение favorite_artists_user_artist_unique уже существует (обработано исключение)';
        END;
    ELSE
        RAISE NOTICE 'Уникальное ограничение уже существует для favorite_artists';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        -- Если возникла любая другая ошибка, просто игнорируем (ограничение уже существует)
        RAISE NOTICE 'Ограничение favorite_artists_user_artist_unique уже существует или не может быть создано: %', SQLERRM;
END $$;

-- ============================================================
-- ПРИМЕЧАНИЯ:
-- ============================================================
-- 1. Функции increment_track_play_count и increment_artist_play_count
--    автоматически увеличивают счетчик прослушиваний при каждом вызове
--
-- 2. Если трек/артист еще не в избранном, он будет добавлен со счетчиком = 1
--
-- 3. Если трек/артист уже в избранном, счетчик увеличится на 1
--
-- 4. Уникальные ограничения гарантируют, что для каждого пользователя
--    трек/артист будет только одна запись
-- ============================================================
