ФИО: Еремеев Александр Николаевич  
Группа: Б9123-09.03.01 цд  
Домашнее задание 6

---

## Описание

Продолжение проекта Pokedex (ДЗ №4/5). В этом задании поверх существующей архитектуры добавлена реактивная логика на основе Flow / StateFlow / SharedFlow.

Приложение отображает список первых 151 покемонов через PokeAPI, поддерживает поиск, переход на экран деталей и сохранение избранных в локальную базу данных (Room).

---

## Что было добавлено в ДЗ №6

### Flow-логика в PokemonListViewModel

Состояние экрана собирается через combine из двух независимых источников:

**_listState** — MutableStateFlow с результатом загрузки/поиска через API

**favouriteRepository.favouriteIds** — Flow из Room, автоматически обновляет UI при изменении избранного

Поиск реализован с задержкой 300 мс через отмену предыдущей Job, что имитирует debounce-поведение.

### Flow-логика в PokemonDetailViewModel

Состояние экрана собирается через combine из трёх источников:

**retryTrigger** — MutableSharedFlow, поток действий пользователя (retry)

**detailFlow** — загрузка деталей покемона через flatMapLatest по триггеру

**isFavouriteFlow** — Flow из Room с distinctUntilChanged, реактивно отражает статус избранного

Retry работает как поток событий: нажатие кнопки эмитит в SharedFlow, что запускает новую загрузку через flatMapLatest.

### Источники данных (3+)

1. Сеть (Retrofit) — список и детали покемонов
2. Room (через FavouriteRepository) — Flow избранных ID
3. SharedFlow retryTrigger — поток действий пользователя

---

## Стек технологий
Kotlin, Jetpack Compose

ViewModel, StateFlow, SharedFlow, Flow operators (combine, flatMapLatest, map, distinctUntilChanged)

Hilt (DI)

Room (локальная БД)

Retrofit + coroutines (сеть)

Navigation Compose

---

## Тесты

**Юнит-тесты (32 теста)**


**PokemonListViewModelTest** — 11 тестов: загрузка, ошибка, retry, пустой результат, поиск, добавление/удаление избранных, дубликаты, отражение избранных в состоянии

**PokemonDetailViewModelTest** — 7 тестов: начальное состояние, успешная загрузка, ошибка, retry, SavedStateHandle, реактивное обновление isFavourite, toggle

**PokemonRepositoryTest** — 6 тестов: получение списка, ошибка сети, кэширование, поиск, возврат кэша при недоступности сети

**FavouriteRepositoryUnitTest** — 5 тестов: Flow из DAO, добавление, удаление, isFavourite

**Интеграционные тесты (15 тестов)**


**FavouriteDaoTest** — 7 тестов: добавление/чтение через Flow, удаление, дубликаты, последовательность эмиссий

**FavouriteRepositoryIntegrationTest** — 4 теста: изменения Flow при add/remove, дубликаты, isFavourite

**PokemonListViewModelIntegrationTest** — 4 теста: сохранение/удаление избранного в Room, дубликаты, retry из Error в Success