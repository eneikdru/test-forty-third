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

<div class="flex flex-col gap-6 w-full max-w-7xl mx-auto px-4 md:px-8 py-6 text-on-surface font-sans antialiased">

  <!-- Alert Banner for actions feedback -->
  {#if alertMessage}
    <div role="alert" class="bg-primary-fixed text-on-primary-container p-3 rounded-xl border border-outline-variant flex items-center gap-3 transition-all animate-pulse">
      <span class="material-symbols-outlined">info</span>
      <span class="font-semibold text-sm">{alertMessage}</span>
    </div>
  {/if}

  <!-- Welcome & Hero Section -->
  <section class="mb-8 bg-surface-container-lowest rounded-xl p-6 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-surface-variant/50 flex flex-col md:flex-row items-center gap-6">
    <div class="flex-1 space-y-4">
      <h2 class="text-2xl md:text-3xl font-bold text-on-surface font-sans leading-tight">С возвращением, Инженер.</h2>
      <p class="text-sm md:text-base text-on-surface-variant leading-relaxed">Работа системы оптимальна. Все узлы сообщают о номинальном статусе. Просмотрите ваши ежедневные метрики и ожидающие задачи ниже.</p>
      <div class="flex gap-3">
        <button
          type="button"
          onclick={() => triggerAction('Быстрое сканирование')}
          class="bg-primary text-on-primary px-4 py-2 rounded text-sm font-semibold transition-all hover:shadow-lg active:scale-95 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-primary"
        >
          <span class="material-symbols-outlined text-[18px]">bolt</span> Быстрое сканирование
        </button>
        <button
          type="button"
          onclick={() => triggerAction('Просмотр журналов')}
          class="border border-outline text-on-surface-variant px-4 py-2 rounded text-sm font-semibold transition-colors hover:bg-surface-container-low active:scale-95 focus:outline-none focus:ring-2 focus:ring-primary"
        >
          Просмотр журналов
        </button>
      </div>
    </div>
    <div class="w-full md:w-1/3 h-48 rounded-lg overflow-hidden bg-surface-container-low relative flex items-center justify-center border border-surface-variant/40">
      <div class="absolute inset-0 bg-gradient-to-br from-[#003ec7]/10 to-transparent"></div>
      <span class="material-symbols-outlined text-[64px] text-primary/40 relative z-10">monitoring</span>
    </div>
  </section>

  <!-- Bento Grid Metrics -->
  <section class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
    <!-- Metric 1: Throughput -->
    <button
      type="button"
      onclick={() => triggerAction('Пропускная способность')}
      class="bg-surface-container-lowest text-left rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-surface-variant/50 flex flex-col justify-between hover:scale-[1.02] hover:shadow-[0px_8px_24px_rgba(15,23,42,0.08)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary"
    >
      <div class="flex justify-between items-start mb-4 w-full">
        <div class="bg-primary/10 p-2 rounded-full flex items-center justify-center">
          <span class="material-symbols-outlined text-primary">speed</span>
        </div>
        <span class="text-[12px] font-mono tracking-wider bg-surface-container px-2 py-1 rounded text-on-surface-variant font-medium uppercase">В ЭФИРЕ</span>
      </div>
      <div class="w-full">
        <div class="text-xs text-on-surface-variant mb-1">Пропускная способность</div>
        <div class="text-2xl font-semibold text-on-surface">94.2 тыс. <span class="text-sm text-on-surface-variant font-normal">запр./с</span></div>
      </div>
      <div class="mt-4 h-1 w-full bg-surface-variant rounded-full overflow-hidden">
        <div class="h-full bg-primary w-3/4 rounded-full"></div>
      </div>
    </button>

    <!-- Metric 2: Active Nodes -->
    <button
      type="button"
      onclick={() => triggerAction('Активные узлы')}
      class="bg-surface-container-lowest text-left rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-surface-variant/50 flex flex-col justify-between hover:scale-[1.02] hover:shadow-[0px_8px_24px_rgba(15,23,42,0.08)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary"
    >
      <div class="flex justify-between items-start mb-4 w-full">
        <div class="bg-primary/10 p-2 rounded-full flex items-center justify-center">
          <span class="material-symbols-outlined text-primary">dns</span>
        </div>
        <span class="text-[12px] font-mono tracking-wider bg-error-container text-on-error-container px-2 py-1 rounded font-medium uppercase">2 ОПОВЕЩЕНИЯ</span>
      </div>
      <div class="w-full">
        <div class="text-xs text-on-surface-variant mb-1">Активные узлы</div>
        <div class="text-2xl font-semibold text-on-surface">1,024 <span class="text-sm text-on-surface-variant font-normal">/ 1,028</span></div>
      </div>
      <div class="mt-4 h-1 w-full bg-surface-variant rounded-full overflow-hidden">
        <div class="h-full bg-primary w-[98%] rounded-full"></div>
      </div>
    </button>

    <!-- Metric 3: CPU Load -->
    <button
      type="button"
      onclick={() => triggerAction('Нагрузка на ЦП')}
      class="bg-surface-container-lowest text-left rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-surface-variant/50 flex flex-col justify-between hover:scale-[1.02] hover:shadow-[0px_8px_24px_rgba(15,23,42,0.08)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary"
    >
      <div class="flex justify-between items-start mb-4 w-full">
        <div class="bg-primary/10 p-2 rounded-full flex items-center justify-center">
          <span class="material-symbols-outlined text-primary">memory</span>
        </div>
      </div>
      <div class="w-full">
        <div class="text-xs text-on-surface-variant mb-1">Нагрузка на ЦП</div>
        <div class="text-2xl font-semibold text-on-surface">42%</div>
      </div>
      <div class="mt-4 h-1 w-full bg-surface-variant rounded-full overflow-hidden">
        <div class="h-full bg-primary w-[42%] rounded-full"></div>
      </div>
    </button>
  </section>

  <!-- Activity Feed & Tasks -->
  <section class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <!-- Pending Tasks -->
    <div class="bg-surface-container-lowest rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-surface-variant/50 flex flex-col justify-between">
      <div>
        <div class="flex justify-between items-center mb-4 pb-2 border-b border-surface-variant">
          <h3 class="text-lg font-bold text-on-surface">Ожидающие задачи</h3>
          <button
            type="button"
            onclick={() => triggerAction('Дополнительные действия с задачами')}
            class="text-on-surface-variant hover:text-primary transition-colors focus:outline-none focus:ring-2 focus:ring-primary rounded"
          >
            <span class="material-symbols-outlined">more_horiz</span>
          </button>
        </div>
        <ul class="space-y-2">
          <li>
            <button
              type="button"
              onclick={() => triggerAction('Проверить обновление безопасности версии 2.4')}
              class="flex items-start text-left gap-3 p-2 hover:bg-surface-container-low rounded-lg transition-colors w-full focus:outline-none focus:bg-surface-container-low group"
            >
              <span class="material-symbols-outlined text-outline mt-1 group-hover:text-primary transition-colors">radio_button_unchecked</span>
              <div class="flex-1">
                <div class="text-sm font-semibold text-on-surface group-hover:text-primary transition-colors">Проверить обновление безопасности версии 2.4</div>
                <div class="text-xs text-on-surface-variant">Срок через 2 часа</div>
              </div>
            </button>
          </li>
          <li>
            <button
              type="button"
              onclick={() => triggerAction('Оптимизировать запросы к базе данных')}
              class="flex items-start text-left gap-3 p-2 hover:bg-surface-container-low rounded-lg transition-colors w-full focus:outline-none focus:bg-surface-container-low group"
            >
              <span class="material-symbols-outlined text-outline mt-1 group-hover:text-primary transition-colors">radio_button_unchecked</span>
              <div class="flex-1">
                <div class="text-sm font-semibold text-on-surface group-hover:text-primary transition-colors">Оптимизировать запросы к базе данных</div>
                <div class="text-xs text-on-surface-variant">Высокий приоритет</div>
              </div>
            </button>
          </li>
          <li class="opacity-60">
            <button
              type="button"
              onclick={() => triggerAction('Развернуть микросервис А')}
              class="flex items-start text-left gap-3 p-2 hover:bg-surface-container-low rounded-lg transition-colors w-full focus:outline-none focus:bg-surface-container-low group"
            >
              <span class="material-symbols-outlined text-primary mt-1 filled">check_circle</span>
              <div class="flex-1 line-through">
                <div class="text-sm font-semibold text-on-surface">Развернуть микросервис А</div>
                <div class="text-xs text-on-surface-variant">Завершено вчера</div>
              </div>
            </button>
          </li>
        </ul>
      </div>
      <button
        type="button"
        onclick={() => triggerAction('Показать все задачи')}
        class="w-full mt-4 py-2 border border-outline text-on-surface-variant font-semibold text-sm rounded hover:bg-surface-container-low transition-colors focus:outline-none focus:ring-2 focus:ring-primary"
      >
        Показать все задачи
      </button>
    </div>

    <!-- Recent Activity -->
    <div class="bg-surface-container-lowest rounded-xl p-4 shadow-[0px_4px_20px_rgba(10,25,47,0.05)] border border-surface-variant/50 flex flex-col">
      <div class="flex justify-between items-center mb-4 pb-2 border-b border-surface-variant">
        <h3 class="text-lg font-bold text-on-surface">Последняя активность</h3>
        <button
          type="button"
          onclick={() => triggerAction('История активности')}
          class="text-on-surface-variant hover:text-primary transition-colors focus:outline-none focus:ring-2 focus:ring-primary rounded"
        >
          <span class="material-symbols-outlined">history</span>
        </button>
      </div>
      <div class="relative pl-3 before:absolute before:left-[15px] before:top-2 before:bottom-2 before:w-0.5 before:bg-surface-variant">
        <!-- Activity Item 1 -->
        <button
          type="button"
          onclick={() => triggerAction('Резервное копирование системы завершено')}
          class="mb-4 relative pl-6 text-left w-full block focus:outline-none group"
        >
          <span class="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-primary outline outline-4 outline-surface-container-lowest"></span>
          <div class="text-xs text-on-surface-variant mb-0.5 group-hover:text-primary transition-colors">10:42</div>
          <div class="text-sm font-semibold text-on-surface group-hover:text-primary transition-colors">Резервное копирование системы завершено</div>
        </button>

        <!-- Activity Item 2 -->
        <button
          type="button"
          onclick={() => triggerAction('Неудачная попытка входа (Узел 4)')}
          class="mb-4 relative pl-6 text-left w-full block focus:outline-none group"
        >
          <span class="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-error outline outline-4 outline-surface-container-lowest"></span>
          <div class="text-xs text-on-surface-variant mb-0.5 group-hover:text-primary transition-colors">09:15</div>
          <div class="text-sm font-semibold text-on-surface group-hover:text-primary transition-colors">Неудачная попытка входа (Узел 4)</div>
        </button>

        <!-- Activity Item 3 -->
        <button
          type="button"
          onclick={() => triggerAction('Конфигурация обновлена администратором')}
          class="relative pl-6 text-left w-full block focus:outline-none group"
        >
          <span class="absolute left-[-2px] top-1.5 w-2 h-2 rounded-full bg-on-surface-variant outline outline-4 outline-surface-container-lowest"></span>
          <div class="text-xs text-on-surface-variant mb-0.5 group-hover:text-primary transition-colors">Вчера</div>
          <div class="text-sm font-semibold text-on-surface group-hover:text-primary transition-colors">Конфигурация обновлена администратором</div>
        </button>
      </div>
    </div>
  </section>
</div>
