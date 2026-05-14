# Описание основных функций приложения LumiSound

Программа имеет современный и интуитивно понятный графический интерфейс, выполненный в стиле Material Design с тёмной цветовой схемой. Приложение построено на архитектуре MVVM и состоит из набора экранов, реализованных с помощью Jetpack Compose, каждый из которых отвечает за определённую функциональную область.

Для реализации поставленных задач было использовано большое количество функций. Основные из них:

- регистрация и авторизация пользователя через Supabase Auth;
- поиск треков и исполнителей через Audius API;
- воспроизведение музыки с управлением через уведомления;
- выставление оценок трекам по пяти критериям;
- создание и управление плейлистами;
- навигация по экранам через Jetpack Navigation Compose.

Рассмотрим основные функции проекта.

---

## Функция «Регистрация пользователя»

Функция «Регистрация пользователя» выполняет процесс создания нового аккаунта, который включает заполнение полей: имя пользователя, email, пароль и его подтверждение. В процессе ввода пароля отображается индикатор его сложности (слабый, хороший, надёжный). После ввода данных и нажатия кнопки «Создать аккаунт» вызывается метод `signUpCreateProfileAndLogin` в `RegisterViewModel`, который отправляет запрос в Supabase Auth. В случае некорректного заполнения полей система отображает соответствующие сообщения об ошибках. После успешной регистрации пользователь переходит на экран подтверждения email, а затем — на экран настройки профиля.

Пример метода регистрации из `AuthRepositoryImpl`:

```kotlin
override suspend fun signUpLoginCreateProfile(
    username: String,
    email: String,
    password: String
): Result<SupabaseTokenResponse> {
    return runCatching {
        // 1) Регистрация с подтверждением email
        supabase.signUp(
            email, password,
            redirectUrl = "https://eeer1mald.github.io/LumiSound_DIPLOM/email-confirmed.html"
        )
        savePendingUsername(email, username)
        // 2) Попытка немедленного входа для получения токена
        val tokenResponse = supabase.signIn(email, password)
        sessionManager.saveAccessToken(tokenResponse.accessToken)
        sessionManager.saveEmail(email)
        tokenResponse.user?.id?.let { userId ->
            sessionManager.saveUserId(userId)
        }
        tokenResponse.refreshToken?.let { sessionManager.saveRefreshToken(it) }
        tokenResponse.expiresIn?.let { expiresIn ->
            sessionManager.saveTokenExpiry(
                System.currentTimeMillis() + expiresIn * 1000L
            )
        }
        tokenResponse
    }
}
```

---

## Функция «Авторизация пользователя»

Функция «Авторизация пользователя» реализует процесс входа в аккаунт, требующий заполнения полей email и пароль. После ввода данных и нажатия кнопки «Войти» система отправляет запрос в Supabase Auth для проверки учётных данных. В случае указания неверного email или пароля, а также при отсутствии интернет-соединения, система отображает соответствующее уведомление об ошибке. После успешного входа токен сохраняется в `SessionManager`, и пользователь переходит на главный экран.

Пример метода авторизации из `LoginViewModel`:

```kotlin
is LoginUiAction.Submit -> {
    // ...валидация полей...
    _uiState.value = LoginUiState.Submitting(
        email = email,
        password = password,
        isPasswordVisible = isPasswordVisible
    )
    viewModelScope.launch {
        authRepository.login(email, password)
            .onSuccess { token ->
                val pendingUsername = pendingUsernameStore.get(email)
                if (pendingUsername != null) {
                    // Новый пользователь — переходим на настройку профиля
                    _uiState.value = LoginUiState.Success(
                        email = email, password = password
                    )
                    _sideEffect.emit(LoginSideEffect.NavigateToProfileSetup)
                } else {
                    // Обычный вход
                    runCatching {
                        authRepository.syncProfileIfNeeded(token.accessToken, email)
                    }
                    _uiState.value = LoginUiState.Success(
                        email = email, password = password
                    )
                    _sideEffect.emit(LoginSideEffect.NavigateToHome)
                }
            }
            .onFailure { exception ->
                _uiState.value = LoginUiState.Error(
                    email = email,
                    password = password,
                    isPasswordVisible = isPasswordVisible,
                    emailError = null,
                    passwordError = null,
                    errorMessage = exception.message
                        ?: "Не удалось войти. Повторите попытку."
                )
            }
    }
}
```

---

## Функция «Поиск треков и исполнителей»

Функция «Поиск» осуществляет параллельный поиск треков, исполнителей и публичных плейлистов через Audius API и Supabase. При вводе запроса в строку поиска одновременно запускаются три независимых запроса. Результаты отображаются в соответствующих секциях экрана. Если поисковая строка пуста, отображается TikTok-подобный вертикальный фид с двумя вкладками: «Рекомендации» и «Для вас».

Пример метода поиска из `SearchViewModel`:

```kotlin
fun searchTracks(query: String) {
    if (query.isBlank()) {
        _searchResults.value = emptyList()
        _artistResults.value = emptyList()
        _playlistResults.value = emptyList()
        _error.value = null
        return
    }
    viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        // Параллельный поиск треков, артистов и плейлистов
        launch {
            musicRepository.searchArtists(query, limit = 5)
                .onSuccess { artists ->
                    val q = query.trim().lowercase()
                    val match = artists.firstOrNull { artist ->
                        val name = artist.name.lowercase()
                        name == q || name.startsWith(q)
                            || q.length >= 3 && name.contains(q)
                    }
                    _artistResults.value =
                        if (match != null) listOf(match) else emptyList()
                }
                .onFailure { _artistResults.value = emptyList() }
        }
        launch {
            authRepository.searchPublicPlaylists(query, limit = 8)
                .let { _playlistResults.value = it }
        }
        musicRepository.searchTracks(query, limit = 20)
            .onSuccess { tracks ->
                _searchResults.value = tracks
                _isLoading.value = false
            }
            .onFailure { exception ->
                _error.value = exception.message ?: "Ошибка при поиске треков"
                _searchResults.value = emptyList()
                _isLoading.value = false
            }
    }
}
```

---

## Функция «Оценка трека»

Функция «Оценка трека» осуществляет процесс выставления оценки по пяти критериям: Рифмы/Образы, Структура/Ритмика, Реализация стиля, Индивидуальность/Харизма, Атмосфера/Вайб — каждый от 1 до 10 баллов. Итоговая оценка вычисляется автоматически как среднее арифметическое. Пользователь также может добавить текстовую рецензию. После нажатия кнопки сохранения данные отправляются в таблицу `track_ratings` в Supabase. Если оценка для данного трека уже существует, она обновляется (upsert).

Пример метода сохранения оценки из `ReviewViewModel`:

```kotlin
fun saveRating(
    audiusTrackId: String,
    trackTitle: String,
    trackArtist: String,
    trackCoverUrl: String?
) {
    val token = sessionManager.getAccessToken() ?: return
    val s = _state.value
    viewModelScope.launch {
        _state.value = s.copy(isSaving = true, error = null)
        authRepository.upsertTrackRating(
            token,
            SupabaseService.TrackRatingInsert(
                audiusTrackId = audiusTrackId,
                trackTitle = trackTitle,
                trackArtist = trackArtist,
                trackCoverUrl = trackCoverUrl,
                rhymeScore = s.rhymeScore,
                imageryScore = s.imageryScore,
                structureScore = s.structureScore,
                charismaScore = s.charismaScore,
                atmosphereScore = s.atmosphereScore,
                review = s.review.takeIf { it.isNotBlank() },
                username = authRepository.getProfile(token).getOrNull()?.username,
                userAvatarUrl = s.userAvatarUrl
            )
        ).onSuccess { saved ->
            val newAverage = authRepository.getTrackAverageRating(token, audiusTrackId)
            _state.value = _state.value.copy(
                isSaving = false,
                savedSuccess = true,
                existingRating = saved,
                averageRating = newAverage,
                review = "" // очищаем поле после сохранения
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(isSaving = false, error = e.message)
        }
    }
}
```
