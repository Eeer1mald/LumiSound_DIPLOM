-- ============================================================
-- СОЗДАНИЕ ТАБЛИЦЫ PROFILES ДЛЯ ПРИЛОЖЕНИЯ LUMISOUND
-- ============================================================
-- 
-- ЧТО ДЕЛАЕТ ЭТОТ СКРИПТ:
-- 1. Создает таблицу profiles для хранения дополнительной информации о пользователях
-- 2. Эта таблица связана с таблицей auth.users (которая создается автоматически Supabase)
-- 3. Настраивает безопасность (RLS - Row Level Security)
-- 4. Создает автоматическое обновление времени изменения записи
-- 
-- ВАЖНО: Эта таблица работает вместе с Supabase Auth
-- Когда пользователь регистрируется через Supabase, он попадает в auth.users
-- А его дополнительные данные (username, bio, avatar) сохраняются в profiles
-- ============================================================

-- ШАГ 1: Создание таблицы profiles
-- Эта таблица хранит дополнительную информацию о пользователе
CREATE TABLE IF NOT EXISTS public.profiles (
    -- id - это UUID из таблицы auth.users (Supabase создает его автоматически)
    -- ON DELETE CASCADE означает: если пользователь удален, его профиль тоже удалится
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- username - имя пользователя (как в вашей таблице users)
    username TEXT NOT NULL,
    
    -- email - почта пользователя (для быстрого поиска)
    email TEXT NOT NULL UNIQUE,
    
    -- bio - биография пользователя (необязательное поле, может быть пустым)
    bio TEXT,
    
    -- favorite_genre - любимый жанр музыки (необязательное поле)
    favorite_genre TEXT,
    
    -- avatar_url - ссылка на аватар пользователя (необязательное поле)
    avatar_url TEXT,
    
    -- created_at - дата создания профиля (заполняется автоматически)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- updated_at - дата последнего обновления (обновляется автоматически)
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ШАГ 2: Создание индекса для быстрого поиска по email
-- Индекс ускоряет поиск пользователей по email
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);

-- ШАГ 3: Включение Row Level Security (RLS)
-- RLS - это механизм безопасности Supabase, который контролирует доступ к данным
-- Это означает, что пользователи могут видеть/изменять только свои данные
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- ШАГ 4: Удаление старых политик безопасности (если они уже существуют)
-- Это нужно, чтобы не было конфликтов при повторном выполнении скрипта
DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;

-- ШАГ 5: Создание политик безопасности
-- Политика 1: Пользователи могут ЧИТАТЬ только свой профиль
CREATE POLICY "Users can read own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);
    -- auth.uid() - это ID текущего авторизованного пользователя
    -- Это означает: "показывай профиль только если его id совпадает с id текущего пользователя"

-- Политика 2: Пользователи могут ОБНОВЛЯТЬ только свой профиль
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);
    -- "разреши обновление только если это профиль текущего пользователя"

-- Политика 3: Пользователи могут СОЗДАВАТЬ только свой профиль
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);
    -- "разреши создание профиля только если id совпадает с id текущего пользователя"

-- ШАГ 6: Создание функции для автоматического обновления updated_at
-- Эта функция будет вызываться каждый раз, когда запись обновляется
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    -- Когда запись обновляется, устанавливаем updated_at в текущее время
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ШАГ 7: Удаление старого триггера (если он уже существует)
DROP TRIGGER IF EXISTS update_profiles_updated_at ON public.profiles;

-- ШАГ 8: Создание триггера
-- Триггер - это механизм, который автоматически выполняет функцию
-- В данном случае: перед каждым обновлением записи вызывается функция update_updated_at_column
CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- ГОТОВО! Таблица profiles создана и настроена
-- ============================================================
-- После выполнения этого скрипта:
-- 1. Таблица profiles будет создана
-- 2. Безопасность настроена (пользователи видят только свои данные)
-- 3. Автоматическое обновление времени работает
-- 4. Приложение сможет сохранять профили пользователей
-- ============================================================
