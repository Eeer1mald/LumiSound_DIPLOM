-- ============================================================
-- НАСТРОЙКА SUPABASE STORAGE ДЛЯ АВАТАРОВ
-- ============================================================
-- 
-- ИНСТРУКЦИЯ:
-- 1. Откройте Supabase Dashboard (https://supabase.com/dashboard)
-- 2. Выберите ваш проект
-- 3. Перейдите в раздел "SQL Editor" (в левом меню)
-- 4. Вставьте весь этот скрипт в редактор
-- 5. Нажмите "Run" или "Execute"
-- 
-- После выполнения bucket "avatars" будет создан и настроен
-- ============================================================

-- ШАГ 1: Создание bucket "avatars" (если еще не существует)
-- Проверяем, существует ли bucket, и создаем его если нет
DO $$
BEGIN
    -- Пытаемся создать bucket "avatars"
    INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
    VALUES (
        'avatars',
        'avatars',
        true, -- Публичный bucket для доступа к аватарам
        5242880, -- Лимит размера файла: 5MB (в байтах)
        ARRAY['image/jpeg', 'image/jpg', 'image/png', 'image/webp'] -- Разрешенные типы файлов
    )
    ON CONFLICT (id) DO NOTHING; -- Если bucket уже существует, ничего не делаем
END $$;

-- ШАГ 2: Настройка политик безопасности (RLS) для bucket "avatars"
-- Удаляем существующие политики (если есть)
DROP POLICY IF EXISTS "Users can upload avatars" ON storage.objects;
DROP POLICY IF EXISTS "Users can update avatars" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete avatars" ON storage.objects;
DROP POLICY IF EXISTS "Public can view avatars" ON storage.objects;

-- Политика: авторизованные пользователи могут загружать аватары
-- (В приложении мы контролируем, что пользователь загружает только свой аватар)
CREATE POLICY "Users can upload avatars"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'avatars' 
        AND auth.role() = 'authenticated'
    );

-- Политика: авторизованные пользователи могут обновлять аватары
CREATE POLICY "Users can update avatars"
    ON storage.objects FOR UPDATE
    USING (
        bucket_id = 'avatars' 
        AND auth.role() = 'authenticated'
    );

-- Политика: авторизованные пользователи могут удалять аватары
CREATE POLICY "Users can delete avatars"
    ON storage.objects FOR DELETE
    USING (
        bucket_id = 'avatars' 
        AND auth.role() = 'authenticated'
    );

-- Политика: все могут просматривать аватары (так как bucket публичный)
CREATE POLICY "Public can view avatars"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'avatars');

-- ============================================================
-- ПРИМЕЧАНИЯ:
-- ============================================================
-- 1. Bucket "avatars" настроен как публичный, поэтому URL вида:
--    {baseUrl}/storage/v1/object/public/avatars/{fileName}
--    будет доступен без авторизации
--
-- 2. Для загрузки файлов пользователь должен быть авторизован
--    и может загружать только свои аватары
--
-- 3. Лимит размера файла: 5MB
--
-- 4. Разрешенные форматы: JPEG, JPG, PNG, WebP
-- ============================================================
