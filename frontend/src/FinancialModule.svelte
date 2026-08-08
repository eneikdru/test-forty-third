<script>
  import { onMount } from 'svelte';
  import SettingsAndAnalytics from './components/SettingsAndAnalytics.svelte';
  import KnowledgeBase from './components/KnowledgeBase.svelte';

  // Svelte 5 state runes
  let selectedRole = $state('Economist'); // 'Economist', 'Teacher', 'Postgraduate', 'Admin'
  let activeCategory = $state('База знаний'); // 'База знаний' по умолчанию
  let activeSubTab = $state('Бюджет'); // 'Бюджет' или 'Нагрузка' (для экономиста)
  let budgetDocs = $state([]);
  let loadDocs = $state([]);
  let stipendDocs = $state([]);
  let loading = $state(false);
  let errorMessage = $state('');

  // Secure Authentication states
  let isAuthenticated = $state(false);
  let token = $state('');
  let usernameInput = $state('');
  let passwordInput = $state('');
  let loginErrorMessage = $state('');
  let isLoggedOut = $state(false);

  // Translation helpers for metadata to ensure 100% Russian language
  function getDocumentTypeName(type) {
    const map = {
      Position: 'Положение',
      Procedure: 'Порядок',
      Project: 'Проект',
      Other: 'Иной документ'
    };
    return map[type] || 'Документ';
  }

  function getProgramName(program) {
    const map = {
      postgraduate: 'Аспирантура',
      residency: 'Ординатура',
      both: 'Аспирантура и ординатура'
    };
    return map[program] || 'Все программы';
  }

  function getProcessName(process) {
    const map = {
      admission: 'Приём',
      certification: 'Аттестация',
      stipends: 'Стипендии',
      practice: 'Практика',
      result_tracking: 'Учёт результатов',
      other: 'Учебный процесс'
    };
    return map[process] || 'Иной процесс';
  }

  function getQuarterName(quarter) {
    const map = {
      Q1: '1-й квартал',
      Q2: '2-й квартал',
      Q3: '3-й квартал',
      Q4: '4-й квартал',
      ANNUAL: 'Годовой'
    };
    return map[quarter] || quarter || 'Период не указан';
  }

  function getStatusName(status) {
    const map = {
      DRAFT: 'ЧЕРНОВИК',
      REVIEW: 'НА РАССМОТРЕНИИ',
      APPROVED: 'УТВЕРЖДЕН',
      ARCHIVED: 'В АРХИВЕ'
    };
    return map[status] || status || 'СТАТУС НЕИЗВЕСТЕН';
  }

  function translateTag(tag) {
    const map = {
      Budget: 'Бюджет',
      Load: 'Нагрузка',
      Stipends: 'Стипендии',
      Book: 'Книга',
      Glossary: 'Глоссарий'
    };
    return map[tag] || '';
  }

  function translateError(err) {
    if (!err) return 'Неизвестная ошибка реестра';
    if (err.code === 'ACCESS_DENIED') {
      return 'Доступ ограничен. У вашей роли отсутствуют необходимые права для просмотра этого раздела.';
    }
    if (err.code === 'UNAUTHORIZED') {
      return 'Вы не авторизованы в системе. Пожалуйста, выполните вход.';
    }
    let msg = err.message || '';
    if (msg.includes('does not have access')) {
      return 'Доступ ограничен. У вашей роли отсутствуют необходимые права для просмотра этого раздела.';
    }
    if (/[a-zA-Z]/.test(msg)) {
      return 'Внутренняя ошибка сервера при обработке запроса.';
    }
    return msg || 'Ошибка обработки запроса';
  }

  // Fetch data function with Token support
  async function fetchData() {
    loading = true;
    errorMessage = '';
    budgetDocs = [];
    loadDocs = [];
    stipendDocs = [];

    const headers = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    } else {
      headers['X-User-Role'] = selectedRole;
    }

    try {
      if (selectedRole === 'Economist') {
        // Fetch budget
        const bRes = await fetch('/api/financial/budget', { headers });
        if (bRes.ok) {
          budgetDocs = await bRes.json();
        } else {
          const err = await bRes.json().catch(() => ({}));
          errorMessage = translateError(err);
        }

        // Fetch load
        const lRes = await fetch('/api/financial/load', { headers });
        if (lRes.ok) {
          loadDocs = await lRes.json();
        }

        // Fetch stipends
        const sRes = await fetch('/api/financial/stipends', { headers });
        if (sRes.ok) {
          stipendDocs = await sRes.json();
        }
      } else if (selectedRole === 'Teacher') {
        // Fetch load
        const lRes = await fetch('/api/financial/load', { headers });
        if (lRes.ok) {
          loadDocs = await lRes.json();
        } else {
          const err = await lRes.json().catch(() => ({}));
          errorMessage = translateError(err);
        }

        // Fetch stipends
        const sRes = await fetch('/api/financial/stipends', { headers });
        if (sRes.ok) {
          stipendDocs = await sRes.json();
        }
      } else {
        // Fetch only stipends for student
        const sRes = await fetch('/api/financial/stipends', { headers });
        if (sRes.ok) {
          stipendDocs = await sRes.json();
        } else {
          const err = await sRes.json().catch(() => ({}));
          errorMessage = translateError(err);
        }
      }
    } catch (err) {
      errorMessage = 'Сбой сети при подключении к базе данных.';
    } finally {
      loading = false;
    }
  }

  // Manual authentications handler
  async function handleManualLogin() {
    loginErrorMessage = '';
    if (!usernameInput || !passwordInput) {
      loginErrorMessage = 'Пожалуйста, заполните все поля ввода.';
      return;
    }
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: usernameInput, password: passwordInput })
      });
      if (res.ok) {
        const data = await res.json();
        token = data.token;
        let returnedRole = data.role;
        if (returnedRole === 'Administrator') {
          selectedRole = 'Admin';
        } else {
          selectedRole = returnedRole;
        }
        isLoggedOut = false;
        isAuthenticated = true;
        loginErrorMessage = '';
        usernameInput = '';
        passwordInput = '';
        fetchData();
      } else {
        const err = await res.json().catch(() => ({}));
        loginErrorMessage = err.message || 'Неверное имя пользователя или пароль.';
      }
    } catch (e) {
      loginErrorMessage = 'Ошибка сети при попытке входа в систему.';
    }
  }

  // Automatic login when dropdown selector is switched
  async function loginUnderTheHood(username, password) {
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (res.ok) {
        const data = await res.json();
        token = data.token;
        isAuthenticated = true;
        fetchData();
      } else {
        isAuthenticated = false;
        token = '';
      }
    } catch (e) {
      isAuthenticated = false;
      token = '';
    }
  }

  async function handleLogout() {
    if (token) {
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      }).catch(() => {});
    }
    token = '';
    isAuthenticated = false;
    isLoggedOut = true;
  }

  // Fetch when role changes
  $effect(() => {
    if (activeCategory !== 'База знаний') {
      if (selectedRole === 'Admin') {
        activeCategory = 'Интеграция';
      } else if (selectedRole === 'Economist') {
        activeCategory = 'Финансы';
        activeSubTab = 'Бюджет';
      } else if (selectedRole === 'Teacher') {
        activeCategory = 'Нагрузка';
      } else {
        activeCategory = 'Стипендии';
      }
    }

    // Perform under-the-hood corporate credentials login based on selected role if not logged out
    if (!isLoggedOut) {
      let username = selectedRole.toLowerCase();
      if (username === 'admin') username = 'admin';
      loginUnderTheHood(username, 'password123');
    }
  });

  onMount(() => {
    loginUnderTheHood('economist', 'password123');
  });
</script>

<style>
  :global(body) {
    background-color: #f8f9ff;
    color: #0b1c30;
    font-family: 'Inter', sans-serif;
    margin: 0;
    padding: 0;
    min-height: 100vh;
  }
</style>

{#if !isAuthenticated}
  <!-- Окно авторизации в случае неавторизованного сеанса (Полностью локализовано на русский язык) -->
  <div class="min-h-screen flex items-center justify-center bg-[#f8f9ff] px-4 font-sans text-[#0b1c30]">
    <div class="bg-white p-8 rounded-xl border border-[#c6c6cd] shadow-md w-full max-w-md flex flex-col gap-6">
      <div class="text-center">
        <h2 class="text-2xl font-bold text-[#0b1c30]">Вход в личный кабинет</h2>
        <p class="text-xs text-[#45464d] mt-1.5">Введите ваши корпоративные учётные данные для авторизации</p>
      </div>

      {#if loginErrorMessage}
        <div class="bg-[#ffdad6] text-[#93000a] p-3 rounded-lg border border-[#ba1a1a] text-xs font-bold" role="alert">
          {loginErrorMessage}
        </div>
      {/if}

      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1.5">
          <label for="username" class="text-xs font-bold text-[#45464d] uppercase tracking-wide">Имя пользователя</label>
          <input
            id="username"
            type="text"
            bind:value={usernameInput}
            placeholder="Логин (например, economist)"
            class="bg-white border border-[#76777d] rounded px-3 py-2 text-sm text-[#0b1c30] placeholder:text-[#94a3b8] focus:border-[#000000] focus:ring-0 outline-none"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label for="password" class="text-xs font-bold text-[#45464d] uppercase tracking-wide">Пароль</label>
          <input
            id="password"
            type="password"
            bind:value={passwordInput}
            placeholder="Введите пароль"
            class="bg-white border border-[#76777d] rounded px-3 py-2 text-sm text-[#0b1c30] placeholder:text-[#94a3b8] focus:border-[#000000] focus:ring-0 outline-none"
          />
        </div>

        <button
          type="button"
          onclick={handleManualLogin}
          class="bg-[#3182CE] hover:bg-[#2b6cb0] text-white text-sm font-bold py-2.5 px-4 rounded transition-colors mt-2 uppercase tracking-wide shadow-sm"
        >
          Войти в систему
        </button>
      </div>
    </div>
  </div>
{:else}
  <div class="min-h-screen flex flex-col md:flex-row bg-[#f8f9ff] text-[#0b1c30] antialiased">

    <!-- Боковая панель навигации (для десктопа) -->
    <aside class="hidden md:flex flex-col w-64 bg-[#e5eeff] border-r border-[#c6c6cd] h-screen sticky top-0 z-40">
      <div class="px-6 py-6 border-b border-[#c6c6cd]">
        <h2 class="text-xl font-bold tracking-tight text-[#0b1c30]">ЦНИИ Эпидемиологии</h2>
        <p class="text-xs text-[#45464d] mt-1">Информационная система</p>
      </div>

      <nav class="flex-1 py-4 flex flex-col gap-1">
        <!-- База знаний доступна для всех -->
        <button
          type="button"
          onclick={() => activeCategory = 'База знаний'}
          class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'База знаний' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
        >
          <span class="material-symbols-outlined">library_books</span>
          <span>База знаний</span>
        </button>

        {#if selectedRole === 'Admin'}
          <!-- Навигация для Администратора -->
          <button
            type="button"
            onclick={() => activeCategory = 'Интеграция'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Интеграция' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">settings_suggest</span>
            <span>Настройки и аналитика</span>
          </button>
        {:else if selectedRole === 'Economist'}
          <!-- Навигация для Экономиста -->
          <button
            type="button"
            onclick={() => activeCategory = 'Финансы'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Финансы' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">payments</span>
            <span>Финансы и бюджет</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Кадры'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Кадры' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">badge</span>
            <span>Кадры и штат</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Стипендии'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Стипендии' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">school</span>
            <span>Стипендии</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Интеграция'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Интеграция' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">settings_suggest</span>
            <span>Настройки и аналитика</span>
          </button>
        {:else if selectedRole === 'Teacher'}
          <!-- Навигация для Преподавателя -->
          <button
            type="button"
            onclick={() => activeCategory = 'Нагрузка'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Нагрузка' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">analytics</span>
            <span>Нормативы нагрузки</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Стипендии'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Стипендии' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">school</span>
            <span>Стипендии</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Интеграция'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Интеграция' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">settings_suggest</span>
            <span>Настройки и аналитика</span>
          </button>
        {:else}
          <!-- Навигация для Студента / Аспиранта -->
          <button
            type="button"
            onclick={() => activeCategory = 'Стипендии'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Стипендии' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#dce9ff]'}"
          >
            <span class="material-symbols-outlined">school</span>
            <span>Стипендии</span>
          </button>
        {/if}
      </nav>

      <!-- Информация о роли внизу -->
      <div class="p-4 border-t border-[#c6c6cd] flex flex-col gap-2">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-2xl text-[#515f74]">account_circle</span>
            <div class="flex flex-col">
              <span class="text-sm font-semibold text-[#0b1c30]">Текущий доступ</span>
              <span class="text-xs text-[#45464d]">
                {#if selectedRole === 'Admin'}
                  Администратор
                {:else if selectedRole === 'Economist'}
                  Экономист
                {:else if selectedRole === 'Teacher'}
                  Преподаватель
                {:else}
                  Студент / Аспирант
                {/if}
              </span>
            </div>
          </div>
        </div>
        <button
          type="button"
          onclick={handleLogout}
          class="flex items-center justify-center gap-1.5 text-xs font-bold text-[#ba1a1a] hover:text-[#93000a] mt-2 border border-[#ffdad6] rounded py-1.5 bg-[#fff]"
        >
          <span class="material-symbols-outlined text-sm">logout</span>
          <span>Выйти</span>
        </button>
      </div>
    </aside>

    <!-- Основная область содержимого -->
    <main class="flex-1 flex flex-col min-w-0 pb-16 md:pb-0">

      <!-- Шапка страницы -->
      <header class="w-full sticky top-0 z-50 bg-[#f8f9ff] border-b border-[#c6c6cd] flex items-center justify-between px-6 py-4">
        <div class="flex items-center gap-4">
          <h1 class="text-lg md:text-xl font-bold text-[#0b1c30]">
            {#if selectedRole === 'Admin'}
              Панель администратора
            {:else if selectedRole === 'Economist'}
              Панель управления экономиста
            {:else}
              Кабинет студента
            {/if}
          </h1>
        </div>

        <!-- Селектор роли для тестирования и переключения контекста -->
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <label for="role-select" class="text-xs font-semibold text-[#45464d]">Авторизация:</label>
            <select
              id="role-select"
              bind:value={selectedRole}
              class="bg-[#ffffff] border border-[#76777d] rounded px-3 py-1.5 text-sm text-[#0b1c30] font-semibold cursor-pointer focus:border-[#000000] focus:ring-0"
            >
              <option value="Economist">Экономист</option>
              <option value="Teacher">Преподаватель</option>
              <option value="Postgraduate">Студент / Аспирант</option>
              <option value="Admin">Администратор</option>
            </select>
          </div>
          <button
            type="button"
            onclick={handleLogout}
            class="md:hidden text-xs font-bold text-[#ba1a1a]"
          >
            Выйти
          </button>
        </div>
      </header>

      <!-- Тело страницы -->
      <div class="p-6 flex flex-col gap-6 max-w-5xl mx-auto w-full">

        <!-- Уведомление об ошибке в системе -->
        {#if errorMessage}
          <div class="bg-[#ffdad6] text-[#93000a] p-4 rounded-lg border border-[#ba1a1a] flex items-center gap-3">
            <span class="material-symbols-outlined">error</span>
            <span class="font-semibold text-sm">{errorMessage}</span>
          </div>
        {/if}

        <!-- Загрузка -->
        {#if loading}
          <div class="flex flex-col items-center justify-center py-12 gap-2 text-[#515f74]">
            <span class="material-symbols-outlined animate-spin text-3xl">sync</span>
            <span class="text-sm font-semibold">Идет получение данных из реестра...</span>
          </div>
        {:else if activeCategory === 'База знаний'}
          <KnowledgeBase {selectedRole} {token} />
        {:else if activeCategory === 'Интеграция' && selectedRole !== 'Postgraduate'}
          <SettingsAndAnalytics {token} />
        {:else}

          <!-- Содержимое для Экономиста -->
          {#if selectedRole === 'Economist'}

            {#if activeCategory === 'Финансы'}
              <!-- Финансовый отчет по макету -->
              <div class="flex flex-col gap-6">

                <!-- Фильтры / Вкладки подразделов -->
                <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-xl border border-[#c6c6cd]">
                  <div class="flex gap-2">
                    <button
                      type="button"
                      onclick={() => activeSubTab = 'Бюджет'}
                      class="px-4 py-2 rounded-lg text-sm font-semibold transition-colors border {activeSubTab === 'Бюджет' ? 'bg-[#d5e3fd] border-[#515f74] text-[#0d1c2f]' : 'border-[#c6c6cd] hover:bg-[#eff4ff]'}"
                    >
                      Бюджет ЦНИИ
                    </button>
                    <button
                      type="button"
                      onclick={() => activeSubTab = 'Нагрузка'}
                      class="px-4 py-2 rounded-lg text-sm font-semibold transition-colors border {activeSubTab === 'Нагрузка' ? 'bg-[#d5e3fd] border-[#515f74] text-[#0d1c2f]' : 'border-[#c6c6cd] hover:bg-[#eff4ff]'}"
                    >
                      Распределение нагрузки
                    </button>
                  </div>
                  <div class="flex items-center gap-2 text-sm font-medium text-[#45464d]">
                    <span class="material-symbols-outlined text-sm">calendar_today</span>
                    <span>Период: 2026–2027 учебный год</span>
                  </div>
                </div>

                {#if activeSubTab === 'Бюджет'}
                  <!-- Карточки КПЭ (KPI) для Бюджета -->
                  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <!-- Выручка -->
                    <div class="bg-white p-5 rounded-xl border border-[#c6c6cd] flex flex-col gap-2">
                      <span class="text-xs font-bold tracking-wider text-[#45464d] uppercase">Общая выручка</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-[#0b1c30] whitespace-nowrap">₽&nbsp;12.4&nbsp;млн</span>
                        <span class="text-xs font-semibold text-[#0d1c2f] bg-[#dae2fd] px-2 py-0.5 rounded-full">+8.2%</span>
                      </div>
                      <span class="text-[11px] text-[#45464d]">По сравнению с прошлым кварталом</span>
                    </div>

                    <!-- Чистая прибыль -->
                    <div class="bg-white p-5 rounded-xl border border-[#c6c6cd] flex flex-col gap-2">
                      <span class="text-xs font-bold tracking-wider text-[#45464d] uppercase">Чистая прибыль</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-[#0b1c30] whitespace-nowrap">₽&nbsp;3.1&nbsp;млн</span>
                        <span class="text-xs font-semibold text-[#0d1c2f] bg-[#dae2fd] px-2 py-0.5 rounded-full">+4.5%</span>
                      </div>
                      <span class="text-[11px] text-[#45464d]">Превышает плановый таргет</span>
                    </div>

                    <!-- Опер. расходы -->
                    <div class="bg-white p-5 rounded-xl border border-[#c6c6cd] flex flex-col gap-2">
                      <span class="text-xs font-bold tracking-wider text-[#45464d] uppercase">Опер. расходы</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-[#ba1a1a] whitespace-nowrap">₽&nbsp;8.5&nbsp;млн</span>
                        <span class="text-xs font-semibold text-[#93000a] bg-[#ffdad6] px-2 py-0.5 rounded-full">+2.1%</span>
                      </div>
                      <span class="text-[11px] text-[#ba1a1a]">Незначительный перерасход лимита</span>
                    </div>
                  </div>

                  <!-- Таблица бюджетов -->
                  <div class="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col">
                    <div class="p-4 bg-[#eff4ff] border-b border-[#c6c6cd] flex justify-between items-center">
                      <h3 class="font-bold text-base text-[#0b1c30]">Реестр бюджетных документов</h3>
                      <span class="text-xs font-bold text-[#515f74] bg-[#d5e3fd] px-2.5 py-1 rounded">АКТУАЛЬНО</span>
                    </div>

                    {#if budgetDocs.length === 0}
                      <p class="p-6 text-sm text-[#45464d] text-center">Документы не найдены</p>
                    {:else}
                      <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                          <thead>
                            <tr class="bg-[#f8f9ff] border-b border-[#c6c6cd] text-xs font-bold text-[#45464d]">
                              <th class="p-4">Название документа</th>
                              <th class="p-4">Шифр</th>
                              <th class="p-4 text-right">Сумма</th>
                              <th class="p-4 text-center">Период бюджетирования</th>
                              <th class="p-4">Версия / Класс</th>
                              <th class="p-4">Статус</th>
                            </tr>
                          </thead>
                          <tbody class="text-sm">
                            {#each budgetDocs as doc}
                              <tr class="border-b border-[#c6c6cd] hover:bg-[#f8f9ff] transition-colors">
                                <td class="p-4">
                                  <div class="font-semibold text-[#0b1c30]">{doc.title}</div>
                                  <div class="text-xs text-[#515f74] mt-0.5">{doc.description}</div>
                                </td>
                                <td class="p-4 text-xs font-mono text-[#515f74]">{doc.documentNumber}</td>
                                <td class="p-4 text-right font-mono text-[#0b1c30] whitespace-nowrap">
                                  {doc.budgetCycleMetadata ? '₽\u00a0' + doc.budgetCycleMetadata.estimatedAmount.toLocaleString('ru-RU') : '—'}
                                </td>
                                <td class="p-4 text-center text-xs text-[#0b1c30]">
                                  {#if doc.budgetCycleMetadata}
                                    <div>{getQuarterName(doc.budgetCycleMetadata.quarter)}</div>
                                    <div class="text-[11px] text-[#45464d]">Фин. год: {doc.budgetCycleMetadata.fiscalYear}</div>
                                  {:else}
                                    —
                                  {/if}
                                </td>
                                <td class="p-4 text-xs">
                                  <div class="font-semibold">Версия {doc.version}</div>
                                  <div class="flex flex-wrap gap-1 mt-1">
                                    {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                      <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#eff4ff] text-[#0b1c30] border border-[#c6c6cd]">
                                        {tag === 'Книга' ? '📖 Книга' : tag === 'Глоссарий' ? '📚 Глоссарий' : tag}
                                      </span>
                                    {/each}
                                  </div>
                                </td>
                                <td class="p-4">
                                  {#if doc.budgetCycleMetadata}
                                    <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-[#dae2fd] text-[#131b2e]">
                                      {getStatusName(doc.budgetCycleMetadata.status)}
                                    </span>
                                  {:else}
                                    <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-[#eff4ff] text-[#45464d]">
                                      УТВЕРЖДЕН
                                    </span>
                                  {/if}
                                </td>
                              </tr>
                            {/each}
                          </tbody>
                        </table>
                      </div>
                    {/if}
                  </div>
                {:else if activeSubTab === 'Нагрузка'}
                  <!-- Раздел Нагрузки -->
                  <div class="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col">
                    <div class="p-4 bg-[#eff4ff] border-b border-[#c6c6cd] flex justify-between items-center">
                      <h3 class="font-bold text-base text-[#0b1c30]">Нормативы учебной нагрузки</h3>
                      <span class="text-xs font-bold text-[#515f74] bg-[#d5e3fd] px-2.5 py-1 rounded">ФГОС</span>
                    </div>

                    {#if loadDocs.length === 0}
                      <p class="p-6 text-sm text-[#45464d] text-center">Документы не найдены</p>
                    {:else}
                      <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                          <thead>
                            <tr class="bg-[#f8f9ff] border-b border-[#c6c6cd] text-xs font-bold text-[#45464d]">
                              <th class="p-4">Название регламента</th>
                              <th class="p-4">Учебный год</th>
                              <th class="p-4">Тип документа</th>
                              <th class="p-4">Раздел / Процесс</th>
                              <th class="p-4">Классификация</th>
                              <th class="p-4">Версия</th>
                            </tr>
                          </thead>
                          <tbody class="text-sm">
                            {#each loadDocs as doc}
                              <tr class="border-b border-[#c6c6cd] hover:bg-[#f8f9ff] transition-colors">
                                <td class="p-4 font-semibold text-[#0b1c30]">{doc.title}</td>
                                <td class="p-4 font-mono text-[#515f74]">{doc.academicYear}</td>
                                <td class="p-4 text-xs">{getDocumentTypeName(doc.documentType)}</td>
                                <td class="p-4 text-xs">{getProcessName(doc.process)}</td>
                                <td class="p-4 text-xs">
                                  <div class="flex flex-wrap gap-1">
                                    {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                      <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#eff4ff] text-[#0b1c30] border border-[#c6c6cd]">
                                        {tag === 'Книга' ? '📖 Книга' : tag === 'Глоссарий' ? '📚 Глоссарий' : tag}
                                      </span>
                                    {/each}
                                  </div>
                                </td>
                                <td class="p-4 text-xs font-semibold">Версия {doc.version}</td>
                              </tr>
                            {/each}
                          </tbody>
                        </table>
                      </div>
                    {/if}
                  </div>
                {/if}

              </div>

            {:else if activeCategory === 'Кадры'}
              <!-- Раздел Кадров -->
              <div class="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col">
                <div class="p-4 bg-[#eff4ff] border-b border-[#c6c6cd] flex justify-between items-center">
                  <h3 class="font-bold text-base text-[#0b1c30]">Штатное расписание и должностные оклады</h3>
                  <span class="text-xs font-bold text-[#515f74] bg-[#d5e3fd] px-2.5 py-1 rounded">КОНФИДЕНЦИАЛЬНО</span>
                </div>

                <div class="p-6 border-b border-[#c6c6cd]">
                  <h4 class="font-bold text-lg text-[#0b1c30] mb-2">Штатные единицы ОЦ ЦНИИ Эпидемиологии</h4>
                  <p class="text-sm text-[#45464d]">
                    Ниже приведен список служебных актов и регламентов, касающихся кадрового учета преподавателей и административного персонала.
                  </p>
                </div>

                <!-- Показываем отфильтрованные документы для кадров -->
                <div class="p-4">
                  <div class="grid grid-cols-1 gap-4">
                    {#each budgetDocs.filter(d => d.title.includes('штат') || d.title.includes('оплат')) as doc}
                      <div class="border border-[#c6c6cd] rounded-lg p-4 bg-[#f8f9ff] flex flex-col gap-2">
                        <div class="flex justify-between items-start">
                          <span class="text-sm font-bold text-[#0b1c30]">{doc.title}</span>
                          <span class="text-xs font-mono text-[#515f74]">{doc.documentNumber}</span>
                        </div>
                        <p class="text-xs text-[#45464d]">{doc.description}</p>
                        <div class="flex items-center justify-between text-xs mt-2 border-t border-[#c6c6cd] pt-2">
                          <span>Тип: {getDocumentTypeName(doc.documentType)}</span>
                          <span>Версия: {doc.version}</span>
                        </div>
                      </div>
                    {/each}
                  </div>
                </div>
              </div>

            {:else if activeCategory === 'Стипендии'}
              <!-- Стипендии для Экономиста -->
              <div class="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col">
                <div class="p-4 bg-[#eff4ff] border-b border-[#c6c6cd]">
                  <h3 class="font-bold text-base text-[#0b1c30]">Стипендиальное обеспечение</h3>
                </div>

                {#if stipendDocs.length === 0}
                  <p class="p-6 text-sm text-[#45464d] text-center">Инструкции по стипендиям не найдены</p>
                {:else}
                  <div class="p-4 grid grid-cols-1 gap-4">
                    {#each stipendDocs as doc}
                      <div class="border border-[#c6c6cd] rounded-lg p-4 bg-[#f8f9ff] flex flex-col gap-2">
                        <span class="text-sm font-bold text-[#0b1c30]">{doc.title}</span>
                        <p class="text-xs text-[#45464d]">{doc.description}</p>
                        <div class="flex items-center justify-between text-xs border-t border-[#c6c6cd] pt-2 mt-2">
                          <span>Направление: {getProgramName(doc.program)}</span>
                          <div class="flex gap-1">
                            {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#eff4ff] text-[#0b1c30] border border-[#c6c6cd]">
                                {tag === 'Книга' ? '📖 Книга' : tag === 'Глоссарий' ? '📚 Глоссарий' : tag}
                              </span>
                            {/each}
                          </div>
                          <span class="font-semibold">Версия {doc.version}</span>
                        </div>
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
            {/if}

          {:else if selectedRole === 'Teacher'}
            <!-- Содержимое для Преподавателя -->
            <div class="flex flex-col gap-6">
              <div class="bg-[#eff4ff] border border-[#c6c6cd] p-5 rounded-xl">
                <h3 class="font-bold text-lg text-[#0d1c2f] mb-2">Кабинет преподавателя ЦНИИ Эпидемиологии</h3>
                <p class="text-sm text-[#45464d]">
                  В соответствии с регламентом, вам предоставлен доступ к расчётам нагрузки и стипендиальному обеспечению в режиме просмотра.
                </p>
              </div>

              {#if activeCategory === 'Нагрузка'}
                <div class="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col">
                  <div class="p-4 bg-[#d5e3fd] border-b border-[#c6c6cd] flex justify-between items-center">
                    <h3 class="font-bold text-base text-[#0d1c2f]">Реестр учебной нагрузки и нормативов</h3>
                    <span class="text-xs font-bold text-[#0d1c2f] bg-white px-2 py-0.5 rounded">ФГОС</span>
                  </div>

                  {#if loadDocs.length === 0}
                    <p class="p-6 text-sm text-[#45464d] text-center">Документы нагрузки не найдены</p>
                  {:else}
                    <div class="p-4 flex flex-col gap-4">
                      {#each loadDocs as doc}
                        <div class="border border-[#c6c6cd] rounded-lg p-4 bg-[#f8f9ff] flex flex-col gap-2">
                          <span class="text-base font-bold text-[#0b1c30]">{doc.title}</span>
                          <p class="text-sm text-[#45464d]">{doc.description}</p>
                          <div class="flex flex-wrap items-center gap-4 text-xs border-t border-[#c6c6cd] pt-2 mt-2 text-[#515f74]">
                            <span>Тип: {getDocumentTypeName(doc.documentType)}</span>
                            <span>Процесс: {getProcessName(doc.process)}</span>
                            <span>Год: {doc.academicYear}</span>
                            <div class="flex gap-1">
                              {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white text-[#0b1c30] border border-[#c6c6cd]">
                                  {tag === 'Книга' ? '📖 Книга' : tag === 'Глоссарий' ? '📚 Глоссарий' : tag}
                                </span>
                              {/each}
                            </div>
                            <span class="ml-auto font-semibold">Версия {doc.version}</span>
                          </div>
                        </div>
                      {/each}
                    </div>
                  {/if}
                </div>
              {:else if activeCategory === 'Стипендии'}
                <div class="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col">
                  <div class="p-4 bg-[#d5e3fd] border-b border-[#c6c6cd] flex justify-between items-center">
                    <h3 class="font-bold text-base text-[#0d1c2f]">Справочник стипендий</h3>
                  </div>

                  {#if stipendDocs.length === 0}
                    <p class="p-6 text-sm text-[#45464d] text-center">Документы не найдены</p>
                  {:else}
                    <div class="p-4 flex flex-col gap-4">
                      {#each stipendDocs as doc}
                        <div class="border border-[#c6c6cd] rounded-lg p-4 bg-[#f8f9ff] flex flex-col gap-2">
                          <span class="text-base font-bold text-[#0b1c30]">{doc.title}</span>
                          <p class="text-sm text-[#45464d]">{doc.description}</p>
                          <div class="flex flex-wrap items-center gap-4 text-xs border-t border-[#c6c6cd] pt-2 mt-2 text-[#515f74]">
                            <span>Направление: {getProgramName(doc.program)}</span>
                            <div class="flex gap-1">
                              {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white text-[#0b1c30] border border-[#c6c6cd]">
                                  {tag === 'Книга' ? '📖 Книга' : tag === 'Глоссарий' ? '📚 Глоссарий' : tag}
                                </span>
                              {/each}
                            </div>
                            <span class="ml-auto font-semibold">Версия {doc.version}</span>
                          </div>
                        </div>
                      {/each}
                    </div>
                  {/if}
                </div>
              {/if}
            </div>

          {:else}
            <!-- Содержимое для Студента (Аспиранта) -->
            <div class="flex flex-col gap-6">
              <div class="bg-[#eff4ff] border border-[#c6c6cd] p-5 rounded-xl">
                <h3 class="font-bold text-lg text-[#0d1c2f] mb-2">Добро пожаловать в кабинет обучающегося!</h3>
                <p class="text-sm text-[#45464d]">
                  В соответствии с вашими правами доступа, вам открыт исключительно регламент стипендиального обеспечения. Разделы о бюджете и распределении учебной нагрузки скрыты.
                </p>
              </div>

              <div class="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col">
                <div class="p-4 bg-[#d5e3fd] border-b border-[#c6c6cd] flex justify-between items-center">
                  <h3 class="font-bold text-base text-[#0d1c2f]">Справочник стипендий аспирантов и ординаторов</h3>
                  <span class="text-xs font-bold text-[#0d1c2f] bg-white px-2 py-0.5 rounded">ДОСТУПНО</span>
                </div>

                {#if stipendDocs.length === 0}
                  <p class="p-6 text-sm text-[#45464d] text-center">Документы не загружены</p>
                {:else}
                  <div class="p-4 flex flex-col gap-4">
                    {#each stipendDocs as doc}
                      <div class="border border-[#c6c6cd] rounded-lg p-4 hover:bg-[#eff4ff] transition-colors flex flex-col gap-2">
                        <span class="text-base font-bold text-[#0b1c30]">{doc.title}</span>
                        <p class="text-sm text-[#45464d]">{doc.description}</p>
                        <div class="flex flex-wrap items-center gap-4 text-xs border-t border-[#c6c6cd] pt-2 mt-2 text-[#515f74]">
                          <span>Номер акта: {doc.documentNumber}</span>
                          <span>Утверждено: {doc.approvalDate}</span>
                          <span>Тип: {getDocumentTypeName(doc.documentType)}</span>
                          <span>Направление: {getProgramName(doc.program)}</span>
                          <div class="flex gap-1">
                            {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white text-[#0b1c30] border border-[#c6c6cd]">
                                {tag === 'Книга' ? '📖 Книга' : tag === 'Глоссарий' ? '📚 Глоссарий' : tag}
                              </span>
                            {/each}
                          </div>
                          <span class="ml-auto font-semibold">Актуальная версия: {doc.version}</span>
                        </div>
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
            </div>
          {/if}

        {/if}

      </div>

    </main>

    <!-- Нижняя панель навигации (для мобильных устройств) -->
    <nav class="md:hidden fixed bottom-0 w-full z-50 bg-[#e5eeff] border-t border-[#c6c6cd] flex justify-around items-center h-16 pb-safe shadow-[0_-1px_3px_rgba(0,0,0,0.05)]">
      <!-- База знаний в мобильном меню для всех ролей -->
      <button
        type="button"
        onclick={() => activeCategory = 'База знаний'}
        class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'База знаний' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
      >
        <span class="material-symbols-outlined text-xl">library_books</span>
        <span>База</span>
      </button>

      {#if selectedRole === 'Admin'}
        <button
          type="button"
          onclick={() => activeCategory = 'Интеграция'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Интеграция' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
        >
          <span class="material-symbols-outlined text-xl">settings_suggest</span>
          <span>Интеграция</span>
        </button>
      {:else if selectedRole === 'Economist'}
        <button
          type="button"
          onclick={() => activeCategory = 'Финансы'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Финансы' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
        >
          <span class="material-symbols-outlined text-xl">payments</span>
          <span>Финансы</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Кадры'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Кадры' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
        >
          <span class="material-symbols-outlined text-xl">badge</span>
          <span>Кадры</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Стипендии' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
        >
          <span class="material-symbols-outlined text-xl">school</span>
          <span>Стипендии</span>
        </button>
      {:else if selectedRole === 'Teacher'}
        <button
          type="button"
          onclick={() => activeCategory = 'Нагрузка'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Нагрузка' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
        >
          <span class="material-symbols-outlined text-xl">analytics</span>
          <span>Нагрузка</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Стипендии' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
        >
          <span class="material-symbols-outlined text-xl">school</span>
          <span>Стипендии</span>
        </button>
      {:else}
        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Стипендии' ? 'text-[#0d1c2f] bg-[#d5e3fd] rounded-full px-4 py-1' : 'text-[#45464d]'}"
        >
          <span class="material-symbols-outlined text-xl">school</span>
          <span>Стипендии</span>
        </button>
      {/if}
    </nav>

  </div>
{/if}
