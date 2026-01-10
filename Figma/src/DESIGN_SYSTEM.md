# Дизайн-система LumiSound

## 🎨 Цветовая палитра

### Основные цвета

```css
/* Фон */
--background-primary: #0F1020;      /* Глубокий индиго */
--background-secondary: #1A1B2E;    /* Темно-индиго */
--background-tertiary: #16182A;     /* Темный индиго */
--background-overlay: #0B0C10;      /* Почти черный */

/* Границы и разделители */
--border-primary: #2A2D3E;          /* Серо-индиго */

/* Текст */
--text-primary: #E6E6EB;            /* Светло-серый */
--text-secondary: #9A9AB0;          /* Серый */

/* Акценты */
--accent-primary: #FF5C6C;          /* Неоновый коралл */
--accent-secondary: #7B6DFF;        /* Фиолетовый */
--accent-blue: #00C6FF;             /* Голубой */
```

### Градиенты

```css
/* Основной градиент */
background: linear-gradient(to right, #7B6DFF, #FF5C6C);

/* Фоновый градиент */
background: linear-gradient(to bottom right, #1A1B2E, #16182A);

/* Легкий градиент */
background: linear-gradient(to right, rgba(123, 109, 255, 0.2), rgba(255, 92, 108, 0.2));
```

## 📐 Размеры и отступы

### Скругления (border-radius)
- Маленькие элементы: `8px - 12px`
- Средние элементы: `16px - 20px`
- Большие карточки: `24px - 32px`

### Отступы (padding/margin)
- Экстра маленький: `4px - 8px`
- Маленький: `12px - 16px`
- Средний: `20px - 24px`
- Большой: `32px - 48px`

### Размеры иконок
- Маленькие: `16px - 20px`
- Средние: `24px - 28px`
- Большие: `32px - 48px`

## 🔤 Типографика

### Шрифт
- **Семейство**: Inter
- **Веса**: 400 (Regular), 600 (Semibold), 700 (Bold)

### Размеры текста

```css
/* Заголовки */
--text-3xl: 2rem (32px)    /* Большие заголовки */
--text-2xl: 1.5rem (24px)  /* Заголовки экранов */
--text-xl: 1.25rem (20px)  /* Подзаголовки */
--text-lg: 1.125rem (18px) /* Крупный текст */

/* Основной текст */
--text-base: 1rem (16px)   /* Обычный текст */
--text-sm: 0.875rem (14px) /* Мелкий текст */
--text-xs: 0.75rem (12px)  /* Очень мелкий текст */
```

## 🎭 Эффекты

### Тени

```css
/* Маленькая тень */
box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);

/* Средняя тень */
box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);

/* Glow эффект (фиолетовый) */
box-shadow: 0 0 16px rgba(123, 109, 255, 0.3);
box-shadow: 0 8px 24px rgba(123, 109, 255, 0.3);

/* Glow эффект (коралловый) */
box-shadow: 0 4px 16px rgba(255, 92, 108, 0.3);
```

### Backdrop Blur (Glassmorphism)

```css
backdrop-filter: blur(8px);
background: rgba(26, 27, 46, 0.8);
```

### Transitions

```css
/* Стандартная анимация */
transition: all 200ms ease;
transition: all 300ms ease;

/* Трансформация */
transition: transform 200ms ease;
```

## 🧩 Компоненты

### Кнопки

#### Основная кнопка (Gradient Button)
```tsx
className="bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] text-white 
           py-3 px-6 rounded-full 
           shadow-[0_8px_24px_rgba(123,109,255,0.3)] 
           active:scale-95 transition-transform"
```

#### Вторичная кнопка
```tsx
className="bg-[#1A1B2E] text-[#9A9AB0] 
           py-3 px-6 rounded-full 
           border border-[#2A2D3E]/40
           active:scale-95 transition-transform"
```

### Карточки

#### Стандартная карточка
```tsx
className="bg-gradient-to-br from-[#1A1B2E]/80 to-[#16182A]/80 
           backdrop-blur-sm rounded-2xl p-6 
           border border-[#2A2D3E]/30"
```

#### Интерактивная карточка
```tsx
className="... active:scale-98 transition-transform cursor-pointer 
           hover:border-[#7B6DFF]/40"
```

### Поля ввода

```tsx
className="w-full px-4 py-3.5 rounded-2xl 
           bg-[#1A1B2E]/80 border border-[#2A2D3E]/40 
           text-[#E6E6EB] placeholder:text-[#9A9AB0]
           focus:outline-none focus:border-[#7B6DFF] 
           focus:shadow-[0_0_16px_rgba(123,109,255,0.25)]"
```

### Навигация

#### Нижняя навигация
- Фиксированная внизу экрана
- Высота: `~60px`
- 4 иконки с подписями
- Активная иконка: градиент + подсветка
- Неактивная иконка: серый цвет

## 📏 Макет

### Размер экрана
- Ширина: `360px`
- Высота: `800px`
- Скругление: `24px`

### Структура экрана
1. **Статус-бар**: 24px (Android)
2. **Заголовок**: ~60-80px
3. **Контент**: flex-1 (прокручиваемый)
4. **Нижняя навигация**: ~60px

## 🎨 Паттерны использования

### Glassmorphism эффект
```tsx
<div className="bg-[#1A1B2E]/80 backdrop-blur-sm 
                border border-[#2A2D3E]/30">
```

### Градиентная иконка
```tsx
<div className="bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C]
                flex items-center justify-center
                shadow-[0_8px_24px_rgba(123,109,255,0.3)]">
```

### Hover эффект на карточке
```tsx
<div className="group">
  <div className="opacity-0 group-hover:opacity-100 transition-opacity">
    {/* Контент при наведении */}
  </div>
</div>
```

## ⚡ Интерактивность

### Active состояния
```tsx
className="active:scale-95 transition-transform"
className="active:scale-98 transition-transform"
```

### Hover состояния
```tsx
className="hover:border-[#7B6DFF]/40 transition-colors"
className="group-hover:opacity-100 transition-opacity"
```

## 🎯 Best Practices

1. **Используйте градиенты умеренно** - только для акцентов и активных элементов
2. **Соблюдайте контраст** - текст должен быть читаемым
3. **Применяйте backdrop-blur** - для создания глубины
4. **Добавляйте transitions** - для плавности интерфейса
5. **Используйте тени** - для создания иерархии
6. **Скругления должны быть единообразными** - 16-20px для большинства элементов
