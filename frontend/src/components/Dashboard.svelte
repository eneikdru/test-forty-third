<script>
  import { onMount } from 'svelte';

  // Svelte 5 state runes for props and bindings
  let {
    selectedRole = $bindable('Economist'),
    activeCategory = $bindable('Панель')
  } = $props();

  let alertMessage = $state('');
  let reportCount = $state(0);

  function triggerAction(actionName) {
    alertMessage = `Действие "${actionName}" успешно выполнено!`;
    setTimeout(() => {
      alertMessage = '';
    }, 4000);
  }

  function handleNewReport() {
    reportCount += 1;
    triggerAction(`Новый отчет #${reportCount}`);
  }

  function handleQuickAction(action) {
    if (action === 'New Report') {
      handleNewReport();
    } else if (action === 'Manage Access') {
      triggerAction('Управление доступом');
    } else if (action === 'Trigger Sync') {
      triggerAction('Запуск синхронизации');
    }
  }

  function navigateTo(category) {
    activeCategory = category;
  }
</script>

<div class="bg-[#0b1326] text-[#dae2fd] font-sans min-h-screen w-full pb-24 md:pb-8 flex flex-col antialiased">

  <!-- Alert Banner for actions feedback -->
  {#if alertMessage}
    <div class="fixed top-20 right-6 z-50 bg-[#171f33] text-[#dae2fd] p-4 rounded-xl border border-[#464554] flex items-center gap-3 shadow-lg transition-all animate-pulse">
      <span class="material-symbols-outlined text-[#c0c1ff]">info</span>
      <span class="font-semibold text-sm font-sans">{alertMessage}</span>
    </div>
  {/if}

  <!-- TopAppBar -->
  <header class="bg-[#171f33] w-full sticky top-0 z-50 flex justify-between items-center h-16 px-5 border-b border-[#2d3449]">
    <div class="flex items-center gap-2">
      <div class="w-10 h-10 rounded-full overflow-hidden border border-[#464554] shrink-0">
        <img
          alt="Профиль пользователя"
          class="w-full h-full object-cover"
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuDkSRN5Z2lslMUujIuon-7SbIRauFlkxXOXk6kE5OQgVfBXDm-FcxLk9DP6oAN1iiHi8w7znt5uLJfmDyiOkXbSi8jf0F1mgEJQj25wk1AG2-_FxsDGSt3KamnpoeW76LY_b76leuOA4fUfuLWLh2GlkrnbkasSn6fRLZQcQZ1DUzZP21G2lqnRZcT6gGUlQbRGQOemlNQwmAKFxdlJOA5O5vWFOvJBwMDYkgm2XGRDm8_VCf00lSuiotrZbshvvM-DxszqCYKRmYY"
        />
      </div>
      <h1 class="text-[20px] leading-[28px] font-bold text-[#c0c1ff] font-sans">тест-сорок-третий</h1>
    </div>

    <!-- Desktop Nav (Hidden on Mobile) -->
    <nav class="hidden md:flex items-center gap-6">
      <button
        type="button"
        onclick={() => navigateTo('Панель')}
        class="flex flex-col items-center justify-center text-[#c0c1ff] font-mono text-[12px] leading-[16px] hover:opacity-80 active:scale-95 transition-transform"
      >
        <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' 1;">home</span>
        Главная
      </button>
      <button
        type="button"
        onclick={() => navigateTo('База знаний')}
        class="flex flex-col items-center justify-center text-[#c7c4d7] font-mono text-[12px] leading-[16px] hover:opacity-80 active:scale-95 transition-transform"
      >
        <span class="material-symbols-outlined mb-1">apps</span>
        Модули
      </button>
      <button
        type="button"
        onclick={() => navigateTo('База знаний')}
        class="flex flex-col items-center justify-center text-[#c7c4d7] font-mono text-[12px] leading-[16px] hover:opacity-80 active:scale-95 transition-transform"
      >
        <span class="material-symbols-outlined mb-1">search</span>
        Поиск
      </button>
      <button
        type="button"
        onclick={() => triggerAction('Окно активности временно недоступно')}
        class="flex flex-col items-center justify-center text-[#c7c4d7] font-mono text-[12px] leading-[16px] hover:opacity-80 active:scale-95 transition-transform"
      >
        <span class="material-symbols-outlined mb-1">notifications</span>
        Активность
      </button>

      <!-- Role Switcher Integrated into Header (Clean, styled dark select) -->
      <div class="flex items-center gap-2 border-l border-[#2d3449] pl-6 ml-2">
        <span class="text-[11px] font-bold text-[#c7c4d7] uppercase tracking-wider font-sans">Роль:</span>
        <select
          id="role-select-dark"
          bind:value={selectedRole}
          class="bg-[#1e293b] border border-[#334155] rounded px-2.5 py-1 text-xs text-[#c0c1ff] font-semibold cursor-pointer focus:border-[#c0c1ff] focus:ring-0"
        >
          <option value="Economist">Экономист</option>
          <option value="Teacher">Преподаватель</option>
          <option value="Postgraduate">Студент / Аспирант</option>
          <option value="Admin">Администратор</option>
        </select>
      </div>
    </nav>

    <!-- Mobile Search Trigger -->
    <button
      type="button"
      onclick={() => navigateTo('База знаний')}
      class="text-[#c0c1ff] hover:opacity-80 active:scale-95 transition-transform w-10 h-10 flex items-center justify-center rounded-full md:hidden"
    >
      <span class="material-symbols-outlined">search</span>
    </button>
  </header>

  <!-- Main Content Canvas -->
  <main class="max-w-[1200px] w-full mx-auto px-5 py-6 grid grid-cols-4 md:grid-cols-12 gap-3 flex-grow">

    <!-- Welcome Section (Full Width) -->
    <section class="col-span-4 md:col-span-12 mb-6">
      <h2 class="text-[28px] md:text-[32px] leading-[36px] md:leading-[40px] font-bold text-[#dae2fd] mb-2 font-sans">Доброе утро, Алекс.</h2>
      <p class="text-[16px] leading-[24px] text-[#c7c4d7] font-sans">Вот ваш высокоуровневый обзор всех систем на сегодня.</p>
    </section>

    <!-- Bento Grid Layout -->
    <!-- Large Metrics Card (Spans 4 cols on mobile, 8 on desktop) -->
    <div class="col-span-4 md:col-span-8 bg-[#1E293B] border border-[#334155] rounded-xl p-4 flex flex-col justify-between min-h-[300px]">
      <div class="flex justify-between items-start mb-6">
        <div>
          <h3 class="text-[24px] leading-[32px] font-bold text-[#dae2fd] mb-1 font-sans">Производительность системы</h3>
          <p class="text-[14px] leading-[20px] text-[#c7c4d7] font-sans">Агрегированная задержка и пропускная способность.</p>
        </div>
        <div class="bg-[#00885d]/10 text-[#4edea3] px-2 py-1 rounded border border-[#4edea3]/20 font-mono text-[10px] flex items-center gap-1">
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
        <div class="absolute bottom-4 left-4 right-4 flex justify-between">
          <div>
            <p class="font-mono text-[12px] text-[#c7c4d7] mb-1">Средняя задержка</p>
            <p class="text-[24px] leading-[32px] font-bold text-[#e1e0ff] font-sans">24 мс</p>
          </div>
          <div class="text-right">
            <p class="font-mono text-[12px] text-[#c7c4d7] mb-1">Запросов/сек</p>
            <p class="text-[24px] leading-[32px] font-bold text-[#e1e0ff] font-sans">14.2 тыс</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions (Spans 4 cols on mobile, 4 on desktop) -->
    <div class="col-span-4 md:col-span-4 bg-[#1E293B] border border-[#334155] rounded-xl p-4 flex flex-col gap-4 min-h-[300px]">
      <h3 class="text-[24px] leading-[32px] font-bold text-[#dae2fd] font-sans">Быстрые действия</h3>

      <button
        type="button"
        onclick={() => handleQuickAction('New Report')}
        class="w-full flex items-center justify-between p-3 rounded-lg hover:bg-[#2d3449] transition-colors border border-transparent hover:border-[#464554] group"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-[#8083ff]/20 flex items-center justify-center text-[#e1e0ff] group-hover:scale-105 transition-transform">
            <span class="material-symbols-outlined">add_chart</span>
          </div>
          <span class="text-[16px] leading-[24px] text-[#dae2fd] font-sans font-medium">Новый отчет</span>
        </div>
        <span class="material-symbols-outlined text-[#c7c4d7]">chevron_right</span>
      </button>

      <button
        type="button"
        onclick={() => handleQuickAction('Manage Access')}
        class="w-full flex items-center justify-between p-3 rounded-lg hover:bg-[#2d3449] transition-colors border border-transparent hover:border-[#464554] group"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-[#464a4b]/30 flex items-center justify-center text-[#e0e3e5] group-hover:scale-105 transition-transform">
            <span class="material-symbols-outlined">manage_accounts</span>
          </div>
          <span class="text-[16px] leading-[24px] text-[#dae2fd] font-sans font-medium">Управление доступом</span>
        </div>
        <span class="material-symbols-outlined text-[#c7c4d7]">chevron_right</span>
      </button>

      <button
        type="button"
        onclick={() => handleQuickAction('Trigger Sync')}
        class="w-full flex items-center justify-between p-3 rounded-lg hover:bg-[#2d3449] transition-colors border border-transparent hover:border-[#464554] group"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-[#00885d]/20 flex items-center justify-center text-[#4edea3] group-hover:scale-105 transition-transform">
            <span class="material-symbols-outlined">sync</span>
          </div>
          <span class="text-[16px] leading-[24px] text-[#dae2fd] font-sans font-medium">Запустить синхронизацию</span>
        </div>
        <span class="material-symbols-outlined text-[#c7c4d7]">chevron_right</span>
      </button>
    </div>

    <!-- Status List (Full width below) -->
    <div class="col-span-4 md:col-span-12 bg-[#1E293B] border border-[#334155] rounded-xl p-4 mt-2">
      <div class="flex justify-between items-center mb-4 border-b border-[#2d3449] pb-3">
        <h3 class="text-[24px] leading-[32px] font-bold text-[#dae2fd] font-sans">Статус модулей</h3>
        <button
          type="button"
          onclick={() => navigateTo('База знаний')}
          class="font-mono text-[12px] text-[#c0c1ff] hover:text-[#e1e0ff] transition-colors uppercase tracking-wider"
        >
          Показать все
        </button>
      </div>

      <!-- Divideless List -->
      <div class="flex flex-col gap-2">
        <div class="flex items-center justify-between p-3 hover:bg-[#2d3449]/50 rounded-lg transition-colors">
          <div class="flex items-center gap-4">
            <span class="material-symbols-outlined text-[#c7c4d7]">database</span>
            <div>
              <p class="text-[16px] leading-[24px] text-[#dae2fd] font-sans font-medium">Основная база данных</p>
              <p class="text-[14px] leading-[20px] text-[#c7c4d7] font-sans">Резервная копия: 2 ч назад</p>
            </div>
          </div>
          <div class="bg-[#00885d]/10 text-[#4edea3] px-2 py-1 rounded border border-[#4edea3]/20 font-mono text-[10px]">Онлайн</div>
        </div>

        <div class="flex items-center justify-between p-3 hover:bg-[#2d3449]/50 rounded-lg transition-colors">
          <div class="flex items-center gap-4">
            <span class="material-symbols-outlined text-[#c7c4d7]">api</span>
            <div>
              <p class="text-[16px] leading-[24px] text-[#dae2fd] font-sans font-medium">Внешний шлюз API</p>
              <p class="text-[14px] leading-[20px] text-[#c7c4d7] font-sans">Обнаружен высокий трафик</p>
            </div>
          </div>
          <div class="bg-[#8083ff]/10 text-[#c0c1ff] px-2 py-1 rounded border border-[#c0c1ff]/20 font-mono text-[10px]">Нагружен</div>
        </div>

        <div class="flex items-center justify-between p-3 hover:bg-[#2d3449]/50 rounded-lg transition-colors">
          <div class="flex items-center gap-4">
            <span class="material-symbols-outlined text-[#c7c4d7]">shield</span>
            <div>
              <p class="text-[16px] leading-[24px] text-[#dae2fd] font-sans font-medium">Служба безопасности</p>
              <p class="text-[14px] leading-[20px] text-[#c7c4d7] font-sans">Сигнатуры обновлены</p>
            </div>
          </div>
          <div class="bg-[#00885d]/10 text-[#4edea3] px-2 py-1 rounded border border-[#4edea3]/20 font-mono text-[10px]">Онлайн</div>
        </div>
      </div>
    </div>
  </main>

  <!-- BottomNavBar (Mobile Only) -->
  <nav class="md:hidden fixed bottom-0 left-0 w-full z-50 flex justify-around items-center h-20 px-2 pb-safe bg-[#131b2e] border-t border-[#2d3449]">
    <button
      type="button"
      onclick={() => navigateTo('Панель')}
      class="flex flex-col items-center justify-center bg-[#8083ff] text-[#0d0096] rounded-full px-4 py-1 font-mono text-[10px] leading-[14px] hover:bg-[#2d3449] active:scale-98 transition-all duration-200"
    >
      <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' 1;">home</span>
      Главная
    </button>
    <button
      type="button"
      onclick={() => navigateTo('База знаний')}
      class="flex flex-col items-center justify-center text-[#c7c4d7] px-4 py-1 font-mono text-[10px] leading-[14px] hover:bg-[#2d3449] active:scale-98 transition-all duration-200"
    >
      <span class="material-symbols-outlined mb-1">apps</span>
      Модули
    </button>
    <button
      type="button"
      onclick={() => navigateTo('База знаний')}
      class="flex flex-col items-center justify-center text-[#c7c4d7] px-4 py-1 font-mono text-[10px] leading-[14px] hover:bg-[#2d3449] active:scale-98 transition-all duration-200"
    >
      <span class="material-symbols-outlined mb-1">search</span>
      Поиск
    </button>
    <button
      type="button"
      onclick={() => triggerAction('Окно активности временно недоступно')}
      class="flex flex-col items-center justify-center text-[#c7c4d7] px-4 py-1 font-mono text-[10px] leading-[14px] hover:bg-[#2d3449] active:scale-98 transition-all duration-200"
    >
      <span class="material-symbols-outlined mb-1">notifications</span>
      Активность
    </button>

    <!-- Mobile Role Selection Option integrated into BottomNav or profile button click -->
    <button
      type="button"
      onclick={() => triggerAction(`Текущая роль: ${selectedRole}`)}
      class="flex flex-col items-center justify-center text-[#c7c4d7] px-4 py-1 font-mono text-[10px] leading-[14px] hover:bg-[#2d3449] active:scale-98 transition-all duration-200"
    >
      <span class="material-symbols-outlined mb-1">person</span>
      Профиль
    </button>
  </nav>

</div>
