<script>
  import { onMount, onDestroy } from 'svelte';

  // Svelte 5 state runes for props and bindings
  let {
    activeCategory = $bindable('Панель'),
    selectedRole = $bindable('Economist')
  } = $props();

  // Alert message state
  let alertMessage = $state('');

  function triggerAction(actionName) {
    alertMessage = `Действие "${actionName}" успешно выполнено!`;
    setTimeout(() => {
      alertMessage = '';
    }, 4000);
  }

  // Manage dark mode classes on mount
  onMount(() => {
    document.documentElement.classList.add('dark');
  });

  onDestroy(() => {
    document.documentElement.classList.remove('dark');
  });
</script>

<style>
  /* Custom scrollbar for webkit, matching mockup styles */
  :global(::-webkit-scrollbar) {
    width: 6px;
    height: 6px;
  }
  :global(::-webkit-scrollbar-track) {
    background: transparent;
  }
  :global(::-webkit-scrollbar-thumb) {
    background: #2d3449; /* surface-variant */
    border-radius: 10px;
  }
  :global(::-webkit-scrollbar-thumb:hover) {
    background: #464554; /* outline-variant */
  }

  /* Custom styles for matching mockup.html layout */
  .dark-canvas {
    background-color: #0b1326;
  }
</style>

<div class="dark dark-canvas text-on-background font-body-md min-h-screen pb-24 md:pb-0 w-full flex flex-col antialiased">
  <!-- TopAppBar Header -->
  <header class="bg-surface dark:bg-surface w-full sticky top-0 z-50 bg-surface-container dark:bg-surface-container flat no shadows flex justify-between items-center h-16 px-container-margin border-b border-outline-variant">
    <div class="flex items-center gap-sm">
      <div class="w-10 h-10 rounded-full overflow-hidden border border-outline-variant shrink-0">
        <img
          alt="Профиль пользователя"
          class="w-full h-full object-cover"
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuDkSRN5Z2lslMUujIuon-7SbIRauFlkxXOXk6kE5OQgVfBXDm-FcxLk9DP6oAN1iiHi8w7znt5uLJfmDyiOkXbSi8jf0F1mgEJQj25wk1AG2-_FxsDGSt3KamnpoeW76LY_b76leuOA4fUfuLWLh2GlkrnbkasSn6fRLZQcQZ1DUzZP21G2lqnRZcT6gGUlQbRGQOemlNQwmAKFxdlJOA5O5vWFOvJBwMDYkgm2XGRDm8_VCf00lSuiotrZbshvvM-DxszqCYKRmYY"
        />
      </div>
      <h1 class="font-headline-md text-headline-md-mobile font-bold text-primary dark:text-primary-fixed-dim">
        тест-сорок-третий
      </h1>
    </div>

    <!-- Desktop Nav (Hidden on Mobile) -->
    <nav class="hidden md:flex items-center gap-lg">
      <button
        type="button"
        onclick={() => activeCategory = 'Панель'}
        class="flex flex-col items-center justify-center font-label-md text-label-md hover:opacity-80 active:scale-95 transition-transform focus:outline-none {activeCategory === 'Панель' ? 'text-primary dark:text-primary-fixed-dim' : 'text-on-surface-variant dark:text-on-surface-variant'}"
      >
        <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' {activeCategory === 'Панель' ? 1 : 0};">home</span>
        Главная
      </button>

      <button
        type="button"
        onclick={() => activeCategory = 'База знаний'}
        class="flex flex-col items-center justify-center font-label-md text-label-md hover:opacity-80 active:scale-95 transition-transform focus:outline-none {activeCategory === 'База знаний' ? 'text-primary dark:text-primary-fixed-dim' : 'text-on-surface-variant dark:text-on-surface-variant'}"
      >
        <span class="material-symbols-outlined mb-1">apps</span>
        Модули
      </button>

      <button
        type="button"
        onclick={() => { activeCategory = 'База знаний'; triggerAction('Поиск'); }}
        class="flex flex-col items-center justify-center text-on-surface-variant dark:text-on-surface-variant font-label-md text-label-md hover:opacity-80 active:scale-95 transition-transform focus:outline-none"
      >
        <span class="material-symbols-outlined mb-1">search</span>
        Поиск
      </button>

      <button
        type="button"
        onclick={() => triggerAction('Уведомления и активность')}
        class="flex flex-col items-center justify-center text-on-surface-variant dark:text-on-surface-variant font-label-md text-label-md hover:opacity-80 active:scale-95 transition-transform focus:outline-none"
      >
        <span class="material-symbols-outlined mb-1">notifications</span>
        Активность
      </button>

      <div class="flex items-center gap-2 text-xs font-semibold">
        <select
          id="role-select-desktop"
          bind:value={selectedRole}
          class="bg-[#171f33] border border-[#2d3449] rounded px-3 py-1.5 text-xs text-[#dae2fd] font-semibold cursor-pointer focus:border-[#8083ff] focus:ring-0"
        >
          <option value="Economist">Экономист</option>
          <option value="Teacher">Преподаватель</option>
          <option value="Postgraduate">Студент / Аспирант</option>
          <option value="Admin">Администратор</option>
        </select>
      </div>
    </nav>

    <!-- Mobile Search (Hidden on Desktop) -->
    <button
      type="button"
      onclick={() => { activeCategory = 'База знаний'; triggerAction('Поиск'); }}
      class="text-primary dark:text-primary-fixed-dim hover:opacity-80 active:scale-95 transition-transform w-10 h-10 flex items-center justify-center rounded-full md:hidden focus:outline-none"
    >
      <span class="material-symbols-outlined">search</span>
    </button>
  </header>

  <!-- Alert Banner for Action Feedback -->
  {#if alertMessage}
    <div class="max-w-[1200px] w-full mx-auto px-container-margin mt-lg">
      <div class="bg-surface-container-high border border-outline-variant text-[#dae2fd] p-md rounded-xl flex items-center gap-sm transition-all shadow-lg">
        <span class="material-symbols-outlined text-tertiary">info</span>
        <span class="font-semibold text-sm">{alertMessage}</span>
      </div>
    </div>
  {/if}

  <!-- Main Content Canvas -->
  <main class="max-w-[1200px] mx-auto px-container-margin py-lg grid grid-cols-4 md:grid-cols-12 gap-gutter w-full">
    <!-- Welcome Section (Full Width) -->
    <section class="col-span-4 md:col-span-12 mb-lg">
      <h2 class="font-headline-lg-mobile md:font-headline-lg text-headline-lg-mobile md:text-headline-lg mb-sm text-on-surface">
        Доброе утро, Алекс.
      </h2>
      <p class="font-body-md text-body-md text-on-surface-variant">
        Вот ваш высокоуровневый обзор всех систем на сегодня.
      </p>
    </section>

    <!-- Bento Grid Layout -->

    <!-- Large Metrics Card (Spans 4 cols on mobile, 8 on desktop) -->
    <div class="col-span-4 md:col-span-8 bg-[#1e293b] border border-[#334155] rounded-xl p-md flex flex-col justify-between min-h-[300px] transition-all hover:translate-y-[-2px] hover:shadow-2xl">
      <div class="flex justify-between items-start mb-lg">
        <div>
          <h3 class="font-headline-md text-headline-md mb-xs text-[#dae2fd]">
            Производительность системы
          </h3>
          <p class="font-body-sm text-body-sm text-on-surface-variant">
            Агрегированная задержка и пропускная способность.
          </p>
        </div>
        <div class="bg-tertiary-container/10 text-tertiary px-sm py-xs rounded-DEFAULT font-label-sm text-label-sm border border-tertiary/20 flex items-center gap-xs bg-[#00885d]/10">
          <span class="material-symbols-outlined text-[14px]">check_circle</span>
          Оптимально
        </div>
      </div>

      <div class="flex-grow w-full rounded-lg overflow-hidden relative min-h-[140px]">
        <!-- Abstract Data Visualization Placeholder -->
        <div
          class="bg-cover bg-center w-full h-full absolute inset-0 opacity-60"
          style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuC2WlqXv54TGDiIDRM4tW0RcqwjDCRysTFHn44i3gK4NNuXE3xxRptSF9lJH0aIgzpSdhO_jXOO4AksK6f9P4hfYwCwK-uEhdjZzQ_B5Rt9ZDI42MxeIUlUot_uU3mNy3gPYVYBPqpVQv0UCwmz13LTJxxAcetQ1IL2zBgaWZASgWQSEeOeNx4_jCatlSVIFFPrOFD6gqE9dPPfYOOiks9lDdCMo3G5djx-utHaTBNpNc8UX9RwMyLqDfxkrcsfsZYugtat8gw7XCk')"
        ></div>
        <div class="absolute bottom-md left-md right-md flex justify-between bg-gradient-to-t from-[#1e293b] via-[#1e293b]/70 to-transparent p-2 rounded">
          <div>
            <p class="font-label-md text-label-md text-on-surface-variant mb-xs">Средняя задержка</p>
            <p class="font-headline-md text-headline-md text-[#e1e0ff] font-bold">24 мс</p>
          </div>
          <div class="text-right">
            <p class="font-label-md text-label-md text-on-surface-variant mb-xs">Запросов в секунду</p>
            <p class="font-headline-md text-headline-md text-[#e1e0ff] font-bold">14,2 тыс.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions (Spans 4 cols on mobile, 4 on desktop) -->
    <div class="col-span-4 md:col-span-4 bg-[#1e293b] border border-[#334155] rounded-xl p-md flex flex-col gap-md min-h-[300px] transition-all hover:translate-y-[-2px] hover:shadow-2xl">
      <h3 class="font-headline-md text-headline-md text-[#dae2fd]">
        Быстрые действия
      </h3>

      <button
        type="button"
        onclick={() => triggerAction('Создание нового отчета')}
        class="w-full flex items-center justify-between p-sm rounded-lg hover:bg-[#2d3449] transition-colors border border-transparent hover:border-outline-variant group text-left focus:outline-none focus:bg-[#2d3449]"
      >
        <div class="flex items-center gap-sm">
          <div class="w-10 h-10 rounded-full bg-[#8083ff]/20 flex items-center justify-center text-primary group-hover:scale-105 transition-transform">
            <span class="material-symbols-outlined text-[#c0c1ff]">add_chart</span>
          </div>
          <span class="font-body-md text-body-md text-[#dae2fd]">Новый отчет</span>
        </div>
        <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
      </button>

      <button
        type="button"
        onclick={() => triggerAction('Управление уровнями доступа')}
        class="w-full flex items-center justify-between p-sm rounded-lg hover:bg-[#2d3449] transition-colors border border-transparent hover:border-outline-variant group text-left focus:outline-none focus:bg-[#2d3449]"
      >
        <div class="flex items-center gap-sm">
          <div class="w-10 h-10 rounded-full bg-[#464a4b]/30 flex items-center justify-center text-secondary group-hover:scale-105 transition-transform">
            <span class="material-symbols-outlined text-[#e0e3e5]">manage_accounts</span>
          </div>
          <span class="font-body-md text-body-md text-[#dae2fd]">Управление доступом</span>
        </div>
        <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
      </button>

      <button
        type="button"
        onclick={() => triggerAction('Запуск принудительной синхронизации')}
        class="w-full flex items-center justify-between p-sm rounded-lg hover:bg-[#2d3449] transition-colors border border-transparent hover:border-outline-variant group text-left focus:outline-none focus:bg-[#2d3449]"
      >
        <div class="flex items-center gap-sm">
          <div class="w-10 h-10 rounded-full bg-[#00885d]/20 flex items-center justify-center text-tertiary group-hover:scale-105 transition-transform">
            <span class="material-symbols-outlined text-[#4edea3]">sync</span>
          </div>
          <span class="font-body-md text-body-md text-[#dae2fd]">Запустить синхронизацию</span>
        </div>
        <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
      </button>
    </div>

    <!-- Status List (Full width below) -->
    <div class="col-span-4 md:col-span-12 bg-[#1e293b] border border-[#334155] rounded-xl p-md mt-sm transition-all hover:translate-y-[-2px] hover:shadow-2xl">
      <div class="flex justify-between items-center mb-md border-b border-[#2d3449] pb-sm">
        <h3 class="font-headline-md text-headline-md text-[#dae2fd]">
          Статус модулей
        </h3>
        <button
          type="button"
          onclick={() => activeCategory = 'База знаний'}
          class="font-label-md text-label-md text-[#c0c1ff] hover:text-[#e1e0ff] transition-colors focus:outline-none"
        >
          Показать все
        </button>
      </div>

      <!-- Divideless List -->
      <div class="flex flex-col gap-sm">
        <div class="flex items-center justify-between p-sm hover:bg-[#2d3449]/50 rounded-lg transition-colors">
          <div class="flex items-center gap-md">
            <span class="material-symbols-outlined text-on-surface-variant">database</span>
            <div>
              <p class="font-body-md text-body-md text-[#dae2fd]">Основная база данных</p>
              <p class="font-label-sm text-label-sm text-on-surface-variant">Последняя копия: 2 ч. назад</p>
            </div>
          </div>
          <div class="bg-[#00885d]/10 text-[#4edea3] px-xs py-[2px] rounded-DEFAULT font-label-sm text-label-sm border border-[#4edea3]/20">
            В сети
          </div>
        </div>

        <div class="flex items-center justify-between p-sm hover:bg-[#2d3449]/50 rounded-lg transition-colors">
          <div class="flex items-center gap-md">
            <span class="material-symbols-outlined text-on-surface-variant">api</span>
            <div>
              <p class="font-body-md text-body-md text-[#dae2fd]">Внешний сетевой шлюз</p>
              <p class="font-label-sm text-label-sm text-on-surface-variant">Обнаружена высокая нагрузка</p>
            </div>
          </div>
          <div class="bg-[#8083ff]/10 text-[#e1e0ff] px-xs py-[2px] rounded-DEFAULT font-label-sm text-label-sm border border-[#8083ff]/20">
            Нагрузка
          </div>
        </div>

        <div class="flex items-center justify-between p-sm hover:bg-[#2d3449]/50 rounded-lg transition-colors">
          <div class="flex items-center gap-md">
            <span class="material-symbols-outlined text-on-surface-variant">shield</span>
            <div>
              <p class="font-body-md text-body-md text-[#dae2fd]">Служба безопасности</p>
              <p class="font-label-sm text-label-sm text-on-surface-variant">Базы обновлены</p>
            </div>
          </div>
          <div class="bg-[#00885d]/10 text-[#4edea3] px-xs py-[2px] rounded-DEFAULT font-label-sm text-label-sm border border-[#4edea3]/20">
            В сети
          </div>
        </div>
      </div>
    </div>
  </main>

  <!-- BottomNavBar (Mobile Only) -->
  <!-- Fitts's Law touch targets enhanced to min-h-[48px] and min-w-[64px] -->
  <nav class="md:hidden fixed bottom-0 left-0 w-full z-50 flex justify-around items-center h-20 px-sm pb-safe bg-surface dark:bg-surface bg-surface-container-low dark:bg-surface-container-low flat no shadows border-t border-outline-variant">
    <button
      type="button"
      onclick={() => activeCategory = 'Панель'}
      class="flex flex-col items-center justify-center min-h-[48px] min-w-[64px] px-3 py-2 rounded-full font-label-sm text-label-sm hover:bg-surface-variant dark:hover:bg-surface-variant active:scale-98 transition-all duration-200 focus:outline-none {activeCategory === 'Панель' ? 'bg-[#8083ff] text-on-[#0d0096] dark:bg-[#8083ff] dark:text-[#0d0096]' : 'text-on-surface-variant dark:text-on-surface-variant'}"
    >
      <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' {activeCategory === 'Панель' ? 1 : 0};">home</span>
      Главная
    </button>

    <button
      type="button"
      onclick={() => activeCategory = 'База знаний'}
      class="flex flex-col items-center justify-center min-h-[48px] min-w-[64px] px-3 py-2 rounded-full font-label-sm text-label-sm hover:bg-surface-variant dark:hover:bg-surface-variant active:scale-98 transition-all duration-200 focus:outline-none {activeCategory === 'База знаний' ? 'bg-[#8083ff] text-on-[#0d0096] dark:bg-[#8083ff] dark:text-[#0d0096]' : 'text-on-surface-variant dark:text-on-surface-variant'}"
    >
      <span class="material-symbols-outlined mb-1">apps</span>
      Модули
    </button>

    <button
      type="button"
      onclick={() => { activeCategory = 'База знаний'; triggerAction('Поиск'); }}
      class="flex flex-col items-center justify-center min-h-[48px] min-w-[64px] px-3 py-2 rounded-full text-on-surface-variant dark:text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant dark:hover:bg-surface-variant active:scale-98 transition-all duration-200 focus:outline-none"
    >
      <span class="material-symbols-outlined mb-1">search</span>
      Поиск
    </button>

    <button
      type="button"
      onclick={() => triggerAction('Уведомления и активность')}
      class="flex flex-col items-center justify-center min-h-[48px] min-w-[64px] px-3 py-2 rounded-full text-on-surface-variant dark:text-on-surface-variant font-label-sm text-label-sm hover:bg-surface-variant dark:hover:bg-surface-variant active:scale-98 transition-all duration-200 focus:outline-none"
    >
      <span class="material-symbols-outlined mb-1">notifications</span>
      Активность
    </button>

    <!-- Profile switch for mobile -->
    <div class="flex flex-col items-center justify-center min-h-[48px] min-w-[64px] px-1 py-1 focus-within:ring-2 focus-within:ring-[#8083ff] rounded-lg">
      <select
        id="role-select-mobile"
        bind:value={selectedRole}
        class="bg-[#171f33] border border-[#2d3449] rounded px-1.5 py-1 text-[10px] text-[#dae2fd] font-semibold cursor-pointer focus:border-[#8083ff] focus:ring-0 w-full"
      >
        <option value="Economist">Эконом</option>
        <option value="Teacher">Препод</option>
        <option value="Postgraduate">Студ</option>
        <option value="Admin">Админ</option>
      </select>
    </div>
  </nav>
</div>
