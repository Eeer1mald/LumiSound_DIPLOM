-- ============================================================
-- СОЗДАНИЕ ТАБЛИЦЫ PROFILES В SUPABASE
-- ============================================================
-- 
-- ИНСТРУКЦИЯ:
-- 1. Откройте Supabase Dashboard (https://supabase.com/dashboard)
-- 2. Выберите ваш проект
-- 3. Перейдите в раздел "SQL Editor" (в левом меню)
-- 4. Вставьте весь этот скрипт в редактор
-- 5. Нажмите "Run" или "Execute"
-- 
-- После выполнения таблица profiles будет создана и готова к использованию
-- ============================================================

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    bio TEXT,
    favorite_genre TEXT,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Создание индекса для быстрого поиска по email
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);

-- Включение Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Удаление существующих политик (если есть) перед созданием новых
DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;

-- Политика: пользователи могут читать свой собственный профиль
CREATE POLICY "Users can read own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

-- Политика: пользователи могут обновлять свой собственный профиль
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- Политика: пользователи могут вставлять свой собственный профиль
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

-- Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Удаление существующего триггера (если есть)
DROP TRIGGER IF EXISTS update_profiles_updated_at ON public.profiles;

-- Триггер для автоматического обновления updated_at
CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
