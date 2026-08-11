<script>
  import { onMount } from 'svelte';
  import SettingsAndAnalytics from './components/SettingsAndAnalytics.svelte';
  import KnowledgeBase from './components/KnowledgeBase.svelte';
  import OfflineMaterialSync from './components/OfflineMaterialSync.svelte';
  import Dashboard from './components/Dashboard.svelte';

  // Svelte 5 state runes
  let isStarted = $state(false);
  let selectedRole = $state('Economist'); // 'Economist', 'Teacher', 'Postgraduate'
  let activeCategory = $state('Панель'); // 'Панель' по умолчанию
  let activeSubTab = $state('Бюджет'); // 'Бюджет' или 'Нагрузка' (для экономиста)
  let budgetDocs = $state([]);
  let loadDocs = $state([]);
  let stipendDocs = $state([]);
  let loading = $state(false);
  let errorMessage = $state('');

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

  // Fetch data function
  async function fetchData() {
    loading = true;
    errorMessage = '';
    budgetDocs = [];
    loadDocs = [];
    stipendDocs = [];

    try {
      if (selectedRole === 'Economist') {
        // Fetch budget
        const bRes = await fetch('/api/financial/budget', {
          headers: { 'X-User-Role': 'Economist' }
        });
        if (bRes.ok) {
          budgetDocs = await bRes.json();
        } else {
          const err = await bRes.json().catch(() => ({}));
          errorMessage = translateError(err);
        }

        // Fetch load
        const lRes = await fetch('/api/financial/load', {
          headers: { 'X-User-Role': 'Economist' }
        });
        if (lRes.ok) {
          loadDocs = await lRes.json();
        }

        // Fetch stipends
        const sRes = await fetch('/api/financial/stipends', {
          headers: { 'X-User-Role': 'Economist' }
        });
        if (sRes.ok) {
          stipendDocs = await sRes.json();
        }
      } else if (selectedRole === 'Teacher') {
        // Fetch load
        const lRes = await fetch('/api/financial/load', {
          headers: { 'X-User-Role': 'Teacher' }
        });
        if (lRes.ok) {
          loadDocs = await lRes.json();
        } else {
          const err = await lRes.json().catch(() => ({}));
          errorMessage = translateError(err);
        }

        // Fetch stipends
        const sRes = await fetch('/api/financial/stipends', {
          headers: { 'X-User-Role': 'Teacher' }
        });
        if (sRes.ok) {
          stipendDocs = await sRes.json();
        }
      } else {
        // Fetch only stipends for student
        const sRes = await fetch('/api/financial/stipends', {
          headers: { 'X-User-Role': 'Postgraduate' }
        });
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

  // Fetch when role changes
  $effect(() => {
    if (activeCategory !== 'База знаний' && activeCategory !== 'Панель') {
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
    fetchData();
  });

  onMount(() => {
    fetchData();
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
  .glass-card {
    background: rgba(248, 249, 255, 0.7);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.5);
  }
</style>

{#if !isStarted}
  <div class="min-h-screen bg-surface-bright text-on-surface flex flex-col antialiased w-full">
    <main class="flex-grow flex flex-col justify-center items-center px-[16px] py-[32px] relative overflow-hidden min-h-screen">
      <!-- Decorative Background Element -->
      <div class="absolute top-0 left-0 w-full h-full overflow-hidden z-0 pointer-events-none">
        <div class="absolute -top-1/4 -right-1/4 w-[150%] h-[150%] bg-gradient-to-br from-[#dce9ff]/40 to-transparent rounded-full blur-3xl opacity-60"></div>
      </div>
      <!-- Hero Content Area -->
      <div class="w-full max-w-md z-10 flex flex-col items-center">
        <!-- Brand Illustration Placeholder -->
        <div class="w-full aspect-square mb-[32px] rounded-[0.75rem] overflow-hidden glass-card shadow-lg flex items-center justify-center p-[16px]">
          <img class="w-full h-full object-cover rounded-[0.5rem]" alt="Чистая, абстрактная 3D-иллюстрация" data-alt="Чистая, абстрактная 3D-иллюстрация с геометрическими фигурами, такими как сферы и кубы, в мягком светлом студийном освещении. Цветовая палитра сосредоточена на чистом белом, мягком сером и оттенках глубокого индиго и бирюзового. Композиция подразумевает взаимосвязь, точность и современные технологии. Настроение спокойное, профессиональное и эффективное." src="https://lh3.googleusercontent.com/aida-public/AB6AXuCmE-eHYQh2hkqjCDLnKW2pf88p524oPhaMR3WmnVw9U58Mz7T3zgLNUQDqZdiMhaa6lvI-nBLUMC0OCsbHWenWt8ez7lAZO1tFnMiMGcE6VvTJBOX-j1p9urHJ4xF7wnH9r6kO3G0sfmF_vnUFrqu6Qi7arU8KOTfD0D_vQxCjJr3DH7NB3gWZrcZisau_qpAIzr6YZnIAmAEDekLfE7HtKQuzW1Rmg0nLuf_Dzr_eIp3zY-Ef5yZBhNBnkzaTVeu4Rb7y-s7i3UZO"/>
        </div>
        <!-- Typography & Value Prop -->
        <div class="text-center mb-[32px] space-y-[16px]">
          <h1 class="text-[28px] leading-[36px] font-bold text-primary tracking-tight">
            тест-сорок-третий
          </h1>
          <p class="text-[16px] leading-[24px] font-normal text-on-surface-variant max-w-[280px] mx-auto">
            Раскройте потенциал тест-сорок-третий. Точность и эффективность для современного профессионала.
          </p>
        </div>
        <!-- Call to Action -->
        <div class="w-full space-y-[16px]">
          <button class="w-full bg-primary text-on-primary py-[8px] px-[16px] rounded-[0.5rem] text-[20px] leading-[28px] font-semibold flex items-center justify-center gap-[8px] active:scale-95 transition-transform duration-200 shadow-md" onclick={() => isStarted = true}>
            <span>Приступить</span>
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 0;">arrow_forward</span>
          </button>
          <button class="w-full bg-transparent border-2 border-[#006b5f] text-on-tertiary-container py-[8px] px-[16px] rounded-[0.5rem] text-[20px] leading-[28px] font-semibold flex items-center justify-center active:bg-tertiary-fixed/20 transition-colors duration-200" onclick={() => isStarted = true}>
            <span>Войти</span>
          </button>
        </div>
      </div>
    </main>
  </div>
{:else}
  <div class="min-h-screen flex flex-col md:flex-row bg-surface-bright text-on-secondary-fixed antialiased">

    <!-- Боковая панель навигации (для десктопа) -->
    <aside class="hidden md:flex flex-col w-64 bg-surface-container-high border-r border-outline-variant h-screen sticky top-0 z-40">
      <div class="px-6 py-6 border-b border-outline-variant">
        <h2 class="text-xl font-bold tracking-tight text-on-secondary-fixed">ЦНИИ Эпидемиологии</h2>
        <p class="text-xs text-on-surface-variant mt-1">Информационная система</p>
      </div>

      <nav class="flex-1 py-4 flex flex-col gap-1">
        <!-- Панель управления доступна для всех -->
        <button
          type="button"
          onclick={() => activeCategory = 'Панель'}
          class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Панель' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
        >
          <span class="material-symbols-outlined">dashboard</span>
          <span>Панель управления</span>
        </button>

        <!-- База знаний доступна для всех -->
        <button
          type="button"
          onclick={() => activeCategory = 'База знаний'}
          class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'База знаний' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
        >
          <span class="material-symbols-outlined">library_books</span>
          <span>База знаний</span>
        </button>

        {#if selectedRole === 'Admin'}
          <!-- Навигация для Администратора -->
          <button
            type="button"
            onclick={() => activeCategory = 'Интеграция'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Интеграция' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">settings_suggest</span>
            <span>Настройки и аналитика</span>
          </button>
        {:else if selectedRole === 'Economist'}
          <!-- Навигация для Экономиста -->
          <button
            type="button"
            onclick={() => activeCategory = 'Финансы'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Финансы' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">payments</span>
            <span>Финансы и бюджет</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Кадры'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Кадры' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">badge</span>
            <span>Кадры и штат</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Стипендии'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Стипендии' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">school</span>
            <span>Стипендии</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Интеграция'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Интеграция' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">settings_suggest</span>
            <span>Настройки и аналитика</span>
          </button>
        {:else if selectedRole === 'Teacher'}
          <!-- Навигация для Преподавателя -->
          <button
            type="button"
            onclick={() => activeCategory = 'Нагрузка'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Нагрузка' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">analytics</span>
            <span>Нормативы нагрузки</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Стипендии'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Стипендии' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">school</span>
            <span>Стипендии</span>
          </button>

          <button
            type="button"
            onclick={() => activeCategory = 'Интеграция'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Интеграция' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">settings_suggest</span>
            <span>Настройки и аналитика</span>
          </button>
        {:else}
          <!-- Навигация для Студента / Аспиранта -->
          <button
            type="button"
            onclick={() => activeCategory = 'Стипендии'}
            class="flex items-center gap-3 px-6 py-3 mx-2 rounded-lg text-left transition-colors font-semibold {activeCategory === 'Стипендии' ? 'bg-secondary-container text-on-surface' : 'text-on-surface-variant hover:bg-secondary-fixed'}"
          >
            <span class="material-symbols-outlined">school</span>
            <span>Стипендии</span>
          </button>
        {/if}
      </nav>

      <!-- Информация о роли внизу -->
      <div class="p-4 border-t border-outline-variant">
        <div class="flex items-center gap-3">
          <span class="material-symbols-outlined text-2xl text-on-secondary-container">account_circle</span>
          <div class="flex flex-col">
            <span class="text-sm font-semibold text-on-secondary-fixed">Текущий доступ</span>
            <span class="text-xs text-on-surface-variant">
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
    </aside>

    <!-- Основная область содержимого -->
    <main class="flex-1 flex flex-col min-w-0 pb-16 md:pb-0">

      <!-- Шапка страницы -->
      <header class="w-full sticky top-0 z-50 bg-surface-bright border-b border-outline-variant flex items-center justify-between px-6 py-4">
        <div class="flex items-center gap-4">
          <h1 class="text-lg md:text-xl font-bold text-on-secondary-fixed">
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
        <div class="flex items-center gap-2">
          <label for="role-select" class="text-xs font-semibold text-on-surface-variant">Авторизация:</label>
          <select
            id="role-select"
            bind:value={selectedRole}
            class="bg-surface-container-lowest border border-[#76777d] rounded px-3 py-1.5 text-sm text-on-secondary-fixed font-semibold cursor-pointer focus:border-[#000000] focus:ring-0"
          >
            <option value="Economist">Экономист</option>
            <option value="Teacher">Преподаватель</option>
            <option value="Postgraduate">Студент / Аспирант</option>
            <option value="Admin">Администратор</option>
          </select>
        </div>
      </header>

      <!-- Тело страницы -->
      <div class="p-6 flex flex-col gap-6 max-w-5xl mx-auto w-full">

        <!-- Уведомление об ошибке в системе -->
        {#if errorMessage && errorMessage.trim() !== ''}
          <div class="bg-error-container text-on-error-container p-4 rounded-lg border border-[#ba1a1a] flex items-center gap-3 w-full min-h-[56px] flex-shrink-0">
            <span class="material-symbols-outlined shrink-0">error</span>
            <span class="font-semibold text-sm break-words">{errorMessage}</span>
          </div>
        {/if}

        <!-- Загрузка -->
        {#if loading}
          <div class="flex flex-col items-center justify-center py-12 gap-2 text-on-secondary-container">
            <span class="material-symbols-outlined animate-spin text-3xl">sync</span>
            <span class="text-sm font-semibold">Идет получение данных из реестра...</span>
          </div>
        {:else if activeCategory === 'Панель'}
          <Dashboard />
        {:else if activeCategory === 'База знаний'}
          <OfflineMaterialSync />
          <KnowledgeBase {selectedRole} />
        {:else if activeCategory === 'Интеграция' && selectedRole !== 'Postgraduate'}
          <SettingsAndAnalytics />
        {:else}

          <!-- Содержимое для Экономиста -->
          {#if selectedRole === 'Economist'}

            {#if activeCategory === 'Финансы'}
              <!-- Финансовый отчет по макету -->
              <div class="flex flex-col gap-6">

                <!-- Фильтры / Вкладки подразделов -->
                <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-xl border border-outline-variant">
                  <div class="flex gap-2">
                    <button
                      type="button"
                      onclick={() => activeSubTab = 'Бюджет'}
                      class="px-4 py-2 rounded-lg text-sm font-semibold transition-colors border {activeSubTab === 'Бюджет' ? 'bg-secondary-container border-[#515f74] text-on-surface' : 'border-outline-variant hover:bg-surface-container-low'}"
                    >
                      Бюджет ЦНИИ
                    </button>
                    <button
                      type="button"
                      onclick={() => activeSubTab = 'Нагрузка'}
                      class="px-4 py-2 rounded-lg text-sm font-semibold transition-colors border {activeSubTab === 'Нагрузка' ? 'bg-secondary-container border-[#515f74] text-on-surface' : 'border-outline-variant hover:bg-surface-container-low'}"
                    >
                      Распределение нагрузки
                    </button>
                  </div>
                  <div class="flex items-center gap-2 text-sm font-medium text-on-surface-variant">
                    <span class="material-symbols-outlined text-sm">calendar_today</span>
                    <span>Период: 2026–2027 учебный год</span>
                  </div>
                </div>

                {#if activeSubTab === 'Бюджет'}
                  <!-- Карточки КПЭ (KPI) для Бюджета -->
                  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <!-- Выручка -->
                    <div class="bg-white p-5 rounded-xl border border-outline-variant flex flex-col gap-2">
                      <span class="text-xs font-bold tracking-wider text-on-surface-variant uppercase">Общая выручка</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-on-secondary-fixed whitespace-nowrap">₽&nbsp;12.4&nbsp;млн</span>
                        <span class="text-xs font-semibold text-on-surface bg-primary-fixed px-2 py-0.5 rounded-full">+8.2%</span>
                      </div>
                      <span class="text-[11px] text-on-surface-variant">По сравнению с прошлым кварталом</span>
                    </div>

                    <!-- Чистая прибыль -->
                    <div class="bg-white p-5 rounded-xl border border-outline-variant flex flex-col gap-2">
                      <span class="text-xs font-bold tracking-wider text-on-surface-variant uppercase">Чистая прибыль</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-on-secondary-fixed whitespace-nowrap">₽&nbsp;3.1&nbsp;млн</span>
                        <span class="text-xs font-semibold text-on-surface bg-primary-fixed px-2 py-0.5 rounded-full">+4.5%</span>
                      </div>
                      <span class="text-[11px] text-on-surface-variant">Превышает плановый таргет</span>
                    </div>

                    <!-- Опер. расходы -->
                    <div class="bg-white p-5 rounded-xl border border-outline-variant flex flex-col gap-2">
                      <span class="text-xs font-bold tracking-wider text-on-surface-variant uppercase">Опер. расходы</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-error whitespace-nowrap">₽&nbsp;8.5&nbsp;млн</span>
                        <span class="text-xs font-semibold text-on-error-container bg-error-container px-2 py-0.5 rounded-full">+2.1%</span>
                      </div>
                      <span class="text-[11px] text-error">Незначительный перерасход лимита</span>
                    </div>
                  </div>

                  <!-- Таблица бюджетов -->
                  <div class="bg-white rounded-xl border border-outline-variant overflow-hidden flex flex-col">
                    <div class="p-4 bg-surface-container-low border-b border-outline-variant flex justify-between items-center">
                      <h3 class="font-bold text-base text-on-secondary-fixed">Реестр бюджетных документов</h3>
                      <span class="text-xs font-bold text-on-secondary-container bg-secondary-container px-2.5 py-1 rounded">АКТУАЛЬНО</span>
                    </div>

                    {#if budgetDocs.length === 0}
                      <p class="p-6 text-sm text-on-surface-variant text-center">Документы не найдены</p>
                    {:else}
                      <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                          <thead>
                            <tr class="bg-surface-bright border-b border-outline-variant text-xs font-bold text-on-surface-variant">
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
                              <tr class="border-b border-outline-variant hover:bg-surface-bright transition-colors">
                                <td class="p-4">
                                  <div class="font-semibold text-on-secondary-fixed">{doc.title}</div>
                                  <div class="text-xs text-on-secondary-container mt-0.5">{doc.description}</div>
                                </td>
                                <td class="p-4 text-xs font-mono text-on-secondary-container">{doc.documentNumber}</td>
                                <td class="p-4 text-right font-mono text-on-secondary-fixed whitespace-nowrap">
                                  {doc.budgetCycleMetadata ? '₽\u00a0' + doc.budgetCycleMetadata.estimatedAmount.toLocaleString('ru-RU') : '—'}
                                </td>
                                <td class="p-4 text-center text-xs text-on-secondary-fixed">
                                  {#if doc.budgetCycleMetadata}
                                    <div>{getQuarterName(doc.budgetCycleMetadata.quarter)}</div>
                                    <div class="text-[11px] text-on-surface-variant">Фин. год: {doc.budgetCycleMetadata.fiscalYear}</div>
                                  {:else}
                                    —
                                  {/if}
                                </td>
                                <td class="p-4 text-xs">
                                  <div class="font-semibold">Версия {doc.version}</div>
                                  <div class="flex flex-wrap gap-1 mt-1">
                                    {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                      <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-surface-container-low text-on-secondary-fixed border border-outline-variant">
                                        {tag === 'Книга' ? '📖 Книга' : tag === 'Глоссарий' ? '📚 Глоссарий' : tag}
                                      </span>
                                    {/each}
                                  </div>
                                </td>
                                <td class="p-4">
                                  {#if doc.budgetCycleMetadata}
                                    <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-primary-fixed text-on-primary-container">
                                      {getStatusName(doc.budgetCycleMetadata.status)}
                                    </span>
                                  {:else}
                                    <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-surface-container-low text-on-surface-variant">
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
                  <div class="bg-white rounded-xl border border-outline-variant overflow-hidden flex flex-col">
                    <div class="p-4 bg-surface-container-low border-b border-outline-variant flex justify-between items-center">
                      <h3 class="font-bold text-base text-on-secondary-fixed">Нормативы учебной нагрузки</h3>
                      <span class="text-xs font-bold text-on-secondary-container bg-secondary-container px-2.5 py-1 rounded">ФГОС</span>
                    </div>

                    {#if loadDocs.length === 0}
                      <p class="p-6 text-sm text-on-surface-variant text-center">Документы не найдены</p>
                    {:else}
                      <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                          <thead>
                            <tr class="bg-surface-bright border-b border-outline-variant text-xs font-bold text-on-surface-variant">
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
                              <tr class="border-b border-outline-variant hover:bg-surface-bright transition-colors">
                                <td class="p-4 font-semibold text-on-secondary-fixed">{doc.title}</td>
                                <td class="p-4 font-mono text-on-secondary-container">{doc.academicYear}</td>
                                <td class="p-4 text-xs">{getDocumentTypeName(doc.documentType)}</td>
                                <td class="p-4 text-xs">{getProcessName(doc.process)}</td>
                                <td class="p-4 text-xs">
                                  <div class="flex flex-wrap gap-1">
                                    {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                      <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-surface-container-low text-on-secondary-fixed border border-outline-variant">
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
              <div class="bg-white rounded-xl border border-outline-variant overflow-hidden flex flex-col">
                <div class="p-4 bg-surface-container-low border-b border-outline-variant flex justify-between items-center">
                  <h3 class="font-bold text-base text-on-secondary-fixed">Штатное расписание и должностные оклады</h3>
                  <span class="text-xs font-bold text-on-secondary-container bg-secondary-container px-2.5 py-1 rounded">КОНФИДЕНЦИАЛЬНО</span>
                </div>

                <div class="p-6 border-b border-outline-variant">
                  <h4 class="font-bold text-lg text-on-secondary-fixed mb-2">Штатные единицы ОЦ ЦНИИ Эпидемиологии</h4>
                  <p class="text-sm text-on-surface-variant">
                    Ниже приведен список служебных актов и регламентов, касающихся кадрового учета преподавателей и административного персонала.
                  </p>
                </div>

                <!-- Показываем отфильтрованные документы для кадров -->
                <div class="p-4">
                  <div class="grid grid-cols-1 gap-4">
                    {#each budgetDocs.filter(d => d.title.includes('штат') || d.title.includes('оплат')) as doc}
                      <div class="border border-outline-variant rounded-lg p-4 bg-surface-bright flex flex-col gap-2">
                        <div class="flex justify-between items-start">
                          <span class="text-sm font-bold text-on-secondary-fixed">{doc.title}</span>
                          <span class="text-xs font-mono text-on-secondary-container">{doc.documentNumber}</span>
                        </div>
                        <p class="text-xs text-on-surface-variant">{doc.description}</p>
                        <div class="flex items-center justify-between text-xs mt-2 border-t border-outline-variant pt-2">
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
              <div class="bg-white rounded-xl border border-outline-variant overflow-hidden flex flex-col">
                <div class="p-4 bg-surface-container-low border-b border-outline-variant">
                  <h3 class="font-bold text-base text-on-secondary-fixed">Стипендиальное обеспечение</h3>
                </div>

                {#if stipendDocs.length === 0}
                  <p class="p-6 text-sm text-on-surface-variant text-center">Инструкции по стипендиям не найдены</p>
                {:else}
                  <div class="p-4 grid grid-cols-1 gap-4">
                    {#each stipendDocs as doc}
                      <div class="border border-outline-variant rounded-lg p-4 bg-surface-bright flex flex-col gap-2">
                        <span class="text-sm font-bold text-on-secondary-fixed">{doc.title}</span>
                        <p class="text-xs text-on-surface-variant">{doc.description}</p>
                        <div class="flex items-center justify-between text-xs border-t border-outline-variant pt-2 mt-2">
                          <span>Направление: {getProgramName(doc.program)}</span>
                          <div class="flex gap-1">
                            {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-surface-container-low text-on-secondary-fixed border border-outline-variant">
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
              <div class="bg-surface-container-low border border-outline-variant p-5 rounded-xl">
                <h3 class="font-bold text-lg text-on-surface mb-2">Кабинет преподавателя ЦНИИ Эпидемиологии</h3>
                <p class="text-sm text-on-surface-variant">
                  В соответствии с регламентом, вам предоставлен доступ к расчётам нагрузки и стипендиальному обеспечению в режиме просмотра.
                </p>
              </div>

              {#if activeCategory === 'Нагрузка'}
                <div class="bg-white rounded-xl border border-outline-variant overflow-hidden flex flex-col">
                  <div class="p-4 bg-secondary-container border-b border-outline-variant flex justify-between items-center">
                    <h3 class="font-bold text-base text-on-surface">Реестр учебной нагрузки и нормативов</h3>
                    <span class="text-xs font-bold text-on-surface bg-white px-2 py-0.5 rounded">ФГОС</span>
                  </div>

                  {#if loadDocs.length === 0}
                    <p class="p-6 text-sm text-on-surface-variant text-center">Документы нагрузки не найдены</p>
                  {:else}
                    <div class="p-4 flex flex-col gap-4">
                      {#each loadDocs as doc}
                        <div class="border border-outline-variant rounded-lg p-4 bg-surface-bright flex flex-col gap-2">
                          <span class="text-base font-bold text-on-secondary-fixed">{doc.title}</span>
                          <p class="text-sm text-on-surface-variant">{doc.description}</p>
                          <div class="flex flex-wrap items-center gap-4 text-xs border-t border-outline-variant pt-2 mt-2 text-on-secondary-container">
                            <span>Тип: {getDocumentTypeName(doc.documentType)}</span>
                            <span>Процесс: {getProcessName(doc.process)}</span>
                            <span>Год: {doc.academicYear}</span>
                            <div class="flex gap-1">
                              {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white text-on-secondary-fixed border border-outline-variant">
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
                <div class="bg-white rounded-xl border border-outline-variant overflow-hidden flex flex-col">
                  <div class="p-4 bg-secondary-container border-b border-outline-variant flex justify-between items-center">
                    <h3 class="font-bold text-base text-on-surface">Справочник стипендий</h3>
                  </div>

                  {#if stipendDocs.length === 0}
                    <p class="p-6 text-sm text-on-surface-variant text-center">Документы не найдены</p>
                  {:else}
                    <div class="p-4 flex flex-col gap-4">
                      {#each stipendDocs as doc}
                        <div class="border border-outline-variant rounded-lg p-4 bg-surface-bright flex flex-col gap-2">
                          <span class="text-base font-bold text-on-secondary-fixed">{doc.title}</span>
                          <p class="text-sm text-on-surface-variant">{doc.description}</p>
                          <div class="flex flex-wrap items-center gap-4 text-xs border-t border-outline-variant pt-2 mt-2 text-on-secondary-container">
                            <span>Направление: {getProgramName(doc.program)}</span>
                            <div class="flex gap-1">
                              {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                                <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white text-on-secondary-fixed border border-outline-variant">
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
              <div class="bg-surface-container-low border border-outline-variant p-5 rounded-xl">
                <h3 class="font-bold text-lg text-on-surface mb-2">Добро пожаловать в кабинет обучающегося!</h3>
                <p class="text-sm text-on-surface-variant">
                  В соответствии с вашими правами доступа, вам открыт исключительно регламент стипендиального обеспечения. Разделы о бюджете и распределении учебной нагрузки скрыты.
                </p>
              </div>

              <div class="bg-white rounded-xl border border-outline-variant overflow-hidden flex flex-col">
                <div class="p-4 bg-secondary-container border-b border-outline-variant flex justify-between items-center">
                  <h3 class="font-bold text-base text-on-surface">Справочник стипендий аспирантов и ординаторов</h3>
                  <span class="text-xs font-bold text-on-surface bg-white px-2 py-0.5 rounded">ДОСТУПНО</span>
                </div>

                {#if stipendDocs.length === 0}
                  <p class="p-6 text-sm text-on-surface-variant text-center">Документы не загружены</p>
                {:else}
                  <div class="p-4 flex flex-col gap-4">
                    {#each stipendDocs as doc}
                      <div class="border border-outline-variant rounded-lg p-4 hover:bg-surface-container-low transition-colors flex flex-col gap-2">
                        <span class="text-base font-bold text-on-secondary-fixed">{doc.title}</span>
                        <p class="text-sm text-on-surface-variant">{doc.description}</p>
                        <div class="flex flex-wrap items-center gap-4 text-xs border-t border-outline-variant pt-2 mt-2 text-on-secondary-container">
                          <span>Номер акта: {doc.documentNumber}</span>
                          <span>Утверждено: {doc.approvalDate}</span>
                          <span>Тип: {getDocumentTypeName(doc.documentType)}</span>
                          <span>Направление: {getProgramName(doc.program)}</span>
                          <div class="flex gap-1">
                            {#each doc.schemaTags.map(translateTag).filter(Boolean) as tag}
                              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white text-on-secondary-fixed border border-outline-variant">
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
    <nav class="md:hidden fixed bottom-0 w-full z-50 bg-surface-container-high border-t border-outline-variant flex justify-around items-center h-16 pb-safe shadow-[0_-1px_3px_rgba(0,0,0,0.05)]">
      <!-- Панель в мобильном меню для всех ролей -->
      <button
        type="button"
        onclick={() => activeCategory = 'Панель'}
        class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Панель' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
      >
        <span class="material-symbols-outlined text-xl">dashboard</span>
        <span>Панель</span>
      </button>

      <!-- База знаний в мобильном меню для всех ролей -->
      <button
        type="button"
        onclick={() => activeCategory = 'База знаний'}
        class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'База знаний' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
      >
        <span class="material-symbols-outlined text-xl">library_books</span>
        <span>База</span>
      </button>

      {#if selectedRole === 'Admin'}
        <button
          type="button"
          onclick={() => activeCategory = 'Интеграция'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Интеграция' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
        >
          <span class="material-symbols-outlined text-xl">settings_suggest</span>
          <span>Интеграция</span>
        </button>
      {:else if selectedRole === 'Economist'}
        <button
          type="button"
          onclick={() => activeCategory = 'Финансы'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Финансы' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
        >
          <span class="material-symbols-outlined text-xl">payments</span>
          <span>Финансы</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Кадры'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Кадры' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
        >
          <span class="material-symbols-outlined text-xl">badge</span>
          <span>Кадры</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Стипендии' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
        >
          <span class="material-symbols-outlined text-xl">school</span>
          <span>Стипендии</span>
        </button>
      {:else if selectedRole === 'Teacher'}
        <button
          type="button"
          onclick={() => activeCategory = 'Нагрузка'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Нагрузка' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
        >
          <span class="material-symbols-outlined text-xl">analytics</span>
          <span>Нагрузка</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Стипендии' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
        >
          <span class="material-symbols-outlined text-xl">school</span>
          <span>Стипендии</span>
        </button>
      {:else}
        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex flex-col items-center justify-center text-xs font-bold {activeCategory === 'Стипендии' ? 'text-on-surface bg-secondary-container rounded-full px-4 py-1' : 'text-on-surface-variant'}"
        >
          <span class="material-symbols-outlined text-xl">school</span>
          <span>Стипендии</span>
        </button>
      {/if}
    </nav>

  </div>
{/if}
