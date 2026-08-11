<script>
  import { onMount } from 'svelte';

  // Svelte 5 state runes for interactive actions
  let alertMessage = $state('');

  function triggerAction(actionName) {
    alertMessage = `Действие "${actionName}" успешно выполнено!`;
    setTimeout(() => {
      alertMessage = '';
    }, 4000);
  }
</script>

<div class="flex flex-col gap-6 w-full max-w-7xl mx-auto px-4 md:px-8 py-6 text-[#191c1e] font-sans antialiased">

  <!-- Alert Banner for actions feedback -->
  {#if alertMessage}
    <div role="alert" class="bg-[#d2e0fe] text-[#0c1c32] p-3 rounded-xl border border-[#c3c5d9] flex items-center gap-3 transition-all animate-pulse">
      <span class="material-symbols-outlined">info</span>
      <span class="font-semibold text-sm">{alertMessage}</span>
    </div>
  {/if}

  <!-- Welcome & Hero Section -->
  <section class="mb-8 bg-[#ffffff] rounded-xl p-6 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-[#e0e3e5]/50 flex flex-col md:flex-row items-center gap-6">
    <div class="flex-1 space-y-4">
      <h2 class="text-2xl md:text-3xl font-bold text-[#191c1e] font-sans leading-tight">С возвращением, Инженер.</h2>
      <p class="text-sm md:text-base text-[#434656] leading-relaxed">Работа системы оптимальна. Все узлы сообщают о номинальном статусе. Просмотрите ваши ежедневные метрики и ожидающие задачи ниже.</p>
      <div class="flex gap-3">
        <button
          type="button"
          onclick={() => triggerAction('Быстрое сканирование')}
          class="bg-[#003ec7] text-[#ffffff] px-4 py-2 rounded text-sm font-semibold transition-all hover:shadow-lg active:scale-95 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
        >
          <span class="material-symbols-outlined text-[18px]">bolt</span> Быстрое сканирование
        </button>
        <button
          type="button"
          onclick={() => triggerAction('Просмотр журналов')}
          class="border border-[#737688] text-[#515f78] px-4 py-2 rounded text-sm font-semibold transition-colors hover:bg-[#f2f4f6] active:scale-95 focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
        >
          Просмотр журналов
        </button>
      </div>
    </div>
    <div class="w-full md:w-1/3 h-48 rounded-lg overflow-hidden bg-[#f2f4f6] relative flex items-center justify-center border border-[#e0e3e5]/40">
      <div class="absolute inset-0 bg-gradient-to-br from-[#003ec7]/10 to-transparent"></div>
      <span class="material-symbols-outlined text-[64px] text-[#003ec7]/40 relative z-10">monitoring</span>
    </div>
  </section>

  <!-- Bento Grid Metrics -->
  <section class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
    <!-- Metric 1: Throughput -->
    <button
      type="button"
      onclick={() => triggerAction('Пропускная способность')}
      class="bg-[#ffffff] text-left rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-[#e0e3e5]/50 flex flex-col justify-between hover:scale-[1.02] hover:shadow-[0px_8px_24px_rgba(15,23,42,0.08)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
    >
      <div class="flex justify-between items-start mb-4 w-full">
        <div class="bg-[#003ec7]/10 p-2 rounded-full flex items-center justify-center">
          <span class="material-symbols-outlined text-[#003ec7]">speed</span>
        </div>
        <span class="text-[12px] font-mono tracking-wider bg-[#eceef0] px-2 py-1 rounded text-[#515f78] font-medium uppercase">В ЭФИРЕ</span>
      </div>
      <div class="w-full">
        <div class="text-xs text-[#434656] mb-1">Пропускная способность</div>
        <div class="text-2xl font-semibold text-[#191c1e]">94.2 тыс. <span class="text-sm text-[#515f78] font-normal">запр./с</span></div>
      </div>
      <div class="mt-4 h-1 w-full bg-[#e0e3e5] rounded-full overflow-hidden">
        <div class="h-full bg-[#003ec7] w-3/4 rounded-full"></div>
      </div>
    </button>

    <!-- Metric 2: Active Nodes -->
    <button
      type="button"
      onclick={() => triggerAction('Активные узлы')}
      class="bg-[#ffffff] text-left rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-[#e0e3e5]/50 flex flex-col justify-between hover:scale-[1.02] hover:shadow-[0px_8px_24px_rgba(15,23,42,0.08)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
    >
      <div class="flex justify-between items-start mb-4 w-full">
        <div class="bg-[#003ec7]/10 p-2 rounded-full flex items-center justify-center">
          <span class="material-symbols-outlined text-[#003ec7]">dns</span>
        </div>
        <span class="text-[12px] font-mono tracking-wider bg-[#ffdad6] text-[#93000a] px-2 py-1 rounded font-medium uppercase">2 ОПОВЕЩЕНИЯ</span>
      </div>
      <div class="w-full">
        <div class="text-xs text-[#434656] mb-1">Активные узлы</div>
        <div class="text-2xl font-semibold text-[#191c1e]">1,024 <span class="text-sm text-[#515f78] font-normal">/ 1,028</span></div>
      </div>
      <div class="mt-4 h-1 w-full bg-[#e0e3e5] rounded-full overflow-hidden">
        <div class="h-full bg-[#003ec7] w-[98%] rounded-full"></div>
      </div>
    </button>

    <!-- Metric 3: CPU Load -->
    <button
      type="button"
      onclick={() => triggerAction('Нагрузка на ЦП')}
      class="bg-[#ffffff] text-left rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-[#e0e3e5]/50 flex flex-col justify-between hover:scale-[1.02] hover:shadow-[0px_8px_24px_rgba(15,23,42,0.08)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
    >
      <div class="flex justify-between items-start mb-4 w-full">
        <div class="bg-[#003ec7]/10 p-2 rounded-full flex items-center justify-center">
          <span class="material-symbols-outlined text-[#003ec7]">memory</span>
        </div>
      </div>
      <div class="w-full">
        <div class="text-xs text-[#434656] mb-1">Нагрузка на ЦП</div>
        <div class="text-2xl font-semibold text-[#191c1e]">42%</div>
      </div>
      <div class="mt-4 h-1 w-full bg-[#e0e3e5] rounded-full overflow-hidden">
        <div class="h-full bg-[#003ec7] w-[42%] rounded-full"></div>
      </div>
    </button>
  </section>

  <!-- Activity Feed & Tasks -->
  <section class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <!-- Pending Tasks -->
    <div class="bg-[#ffffff] rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-[#e0e3e5]/50 flex flex-col justify-between">
      <div>
        <div class="flex justify-between items-center mb-4 pb-2 border-b border-[#e0e3e5]">
          <h3 class="text-lg font-bold text-[#191c1e]">Ожидающие задачи</h3>
          <button
            type="button"
            onclick={() => triggerAction('Дополнительные действия с задачами')}
            class="text-[#515f78] hover:text-[#003ec7] transition-colors focus:outline-none focus:ring-2 focus:ring-[#3182CE] rounded"
          >
            <span class="material-symbols-outlined">more_horiz</span>
          </button>
        </div>
        <ul class="space-y-2">
          <li>
            <button
              type="button"
              onclick={() => triggerAction('Проверить обновление безопасности версии 2.4')}
              class="flex items-start text-left gap-3 p-2 hover:bg-[#f2f4f6] rounded-lg transition-colors w-full focus:outline-none focus:bg-[#f2f4f6] group"
            >
              <span class="material-symbols-outlined text-[#737688] mt-1 group-hover:text-[#003ec7] transition-colors">radio_button_unchecked</span>
              <div class="flex-1">
                <div class="text-sm font-semibold text-[#191c1e] group-hover:text-[#003ec7] transition-colors">Проверить обновление безопасности версии 2.4</div>
                <div class="text-xs text-[#434656]">Срок через 2 часа</div>
              </div>
            </button>
          </li>
          <li>
            <button
              type="button"
              onclick={() => triggerAction('Оптимизировать запросы к базе данных')}
              class="flex items-start text-left gap-3 p-2 hover:bg-[#f2f4f6] rounded-lg transition-colors w-full focus:outline-none focus:bg-[#f2f4f6] group"
            >
              <span class="material-symbols-outlined text-[#737688] mt-1 group-hover:text-[#003ec7] transition-colors">radio_button_unchecked</span>
              <div class="flex-1">
                <div class="text-sm font-semibold text-[#191c1e] group-hover:text-[#003ec7] transition-colors">Оптимизировать запросы к базе данных</div>
                <div class="text-xs text-[#434656]">Высокий приоритет</div>
              </div>
            </button>
          </li>
          <li class="opacity-60">
            <button
              type="button"
              onclick={() => triggerAction('Развернуть микросервис А')}
              class="flex items-start text-left gap-3 p-2 hover:bg-[#f2f4f6] rounded-lg transition-colors w-full focus:outline-none focus:bg-[#f2f4f6] group"
            >
              <span class="material-symbols-outlined text-[#003ec7] mt-1 filled">check_circle</span>
              <div class="flex-1 line-through">
                <div class="text-sm font-semibold text-[#191c1e]">Развернуть микросервис А</div>
                <div class="text-xs text-[#434656]">Завершено вчера</div>
              </div>
            </button>
          </li>
        </ul>
      </div>
      <button
        type="button"
        onclick={() => triggerAction('Показать все задачи')}
        class="w-full mt-4 py-2 border border-[#737688] text-[#515f78] font-semibold text-sm rounded hover:bg-[#f2f4f6] transition-colors focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
      >
        Показать все задачи
      </button>
    </div>

    <!-- Recent Activity -->
    <div class="bg-[#ffffff] rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-[#e0e3e5]/50 flex flex-col">
      <div class="flex justify-between items-center mb-4 pb-2 border-b border-[#e0e3e5]">
        <h3 class="text-lg font-bold text-[#191c1e]">Последняя активность</h3>
        <button
          type="button"
          onclick={() => triggerAction('История активности')}
          class="text-[#515f78] hover:text-[#003ec7] transition-colors focus:outline-none focus:ring-2 focus:ring-[#3182CE] rounded"
        >
          <span class="material-symbols-outlined">history</span>
        </button>
      </div>
      <div class="relative pl-3 before:absolute before:left-[15px] before:top-2 before:bottom-2 before:w-0.5 before:bg-[#e0e3e5]">
        <!-- Activity Item 1 -->
        <button
          type="button"
          onclick={() => triggerAction('Резервное копирование системы завершено')}
          class="mb-4 relative pl-6 text-left w-full block focus:outline-none group"
        >
          <span class="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-[#003ec7] outline outline-4 outline-[#ffffff]"></span>
          <div class="text-xs text-[#434656] mb-0.5 group-hover:text-[#003ec7] transition-colors">10:42</div>
          <div class="text-sm font-semibold text-[#191c1e] group-hover:text-[#003ec7] transition-colors">Резервное копирование системы завершено</div>
        </button>

        <!-- Activity Item 2 -->
        <button
          type="button"
          onclick={() => triggerAction('Неудачная попытка входа (Узел 4)')}
          class="mb-4 relative pl-6 text-left w-full block focus:outline-none group"
        >
          <span class="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-[#ba1a1a] outline outline-4 outline-[#ffffff]"></span>
          <div class="text-xs text-[#434656] mb-0.5 group-hover:text-[#003ec7] transition-colors">09:15</div>
          <div class="text-sm font-semibold text-[#191c1e] group-hover:text-[#003ec7] transition-colors">Неудачная попытка входа (Узел 4)</div>
        </button>

        <!-- Activity Item 3 -->
        <button
          type="button"
          onclick={() => triggerAction('Конфигурация обновлена администратором')}
          class="relative pl-6 text-left w-full block focus:outline-none group"
        >
          <span class="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-[#515f78] outline outline-4 outline-[#ffffff]"></span>
          <div class="text-xs text-[#434656] mb-0.5 group-hover:text-[#003ec7] transition-colors">Вчера</div>
          <div class="text-sm font-semibold text-[#191c1e] group-hover:text-[#003ec7] transition-colors">Конфигурация обновлена администратором</div>
        </button>
      </div>
    </div>
  </section>
</div>
