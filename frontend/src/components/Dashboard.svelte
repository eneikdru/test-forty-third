<script>
  import { onMount } from 'svelte';

  // State runes for interactive actions
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
</script>

<div class="flex flex-col gap-lg w-full max-w-7xl mx-auto px-margin-mobile md:px-margin-desktop py-lg md:py-xl text-on-background font-body-md antialiased">

  <!-- Alert Banner for actions feedback -->
  {#if alertMessage}
    <div class="bg-secondary-container text-on-secondary-container p-sm rounded-xl border border-outline-variant flex items-center gap-sm transition-all animate-pulse">
      <span class="material-symbols-outlined">info</span>
      <span class="font-semibold text-sm">{alertMessage}</span>
    </div>
  {/if}

  <!-- Welcome & Hero Section -->
  <section class="flex flex-col md:flex-row justify-between items-start md:items-end gap-sm mb-sm">
    <div>
      <h1 class="font-headline-lg-mobile text-headline-lg-mobile md:font-headline-lg md:text-headline-lg text-on-surface mb-base">С возвращением, Алекс.</h1>
      <p class="font-body-lg text-body-lg text-on-surface-variant">Вот ваш ежедневный обзор.</p>
    </div>
    <div class="flex gap-sm w-full md:w-auto mt-sm md:mt-0">
      <button
        type="button"
        onclick={handleNewReport}
        class="flex-1 md:flex-none flex items-center justify-center gap-base px-sm py-sm bg-primary text-on-primary rounded font-body-sm text-body-sm font-semibold hover:opacity-90 transition-opacity focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
      >
        <span class="material-symbols-outlined" style="font-size: 18px;">add</span>
        <span>Новый отчет</span>
      </button>
    </div>
  </section>

  <!-- Metrics Grid (Bento Style) -->
  <section class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-gutter w-full">
    <!-- Metric 1 -->
    <div class="bento-card flex flex-col justify-between h-32 ambient-shadow">
      <div class="flex justify-between items-start w-full">
        <span class="font-label-caps text-label-caps text-on-surface-variant uppercase">Общий объем</span>
        <span class="material-symbols-outlined text-secondary">monitoring</span>
      </div>
      <div>
        <div class="font-headline-lg-mobile text-headline-lg-mobile text-on-surface">24,592</div>
        <div class="font-body-sm text-[11px] leading-tight text-on-surface-variant flex flex-wrap items-center gap-1 mt-1">
          <span class="material-symbols-outlined text-primary shrink-0" style="font-size: 16px;">trending_up</span>
          <span class="text-primary font-semibold shrink-0">+12.5%</span>
          <span>по сравнению с прошлой неделей</span>
        </div>
      </div>
    </div>

    <!-- Metric 2 -->
    <div class="bento-card flex flex-col justify-between h-32 ambient-shadow">
      <div class="flex justify-between items-start w-full">
        <span class="font-label-caps text-label-caps text-on-surface-variant uppercase">Активные сессии</span>
        <span class="material-symbols-outlined text-secondary">group</span>
      </div>
      <div>
        <div class="font-headline-lg-mobile text-headline-lg-mobile text-on-surface">1,843</div>
        <div class="font-body-sm text-[11px] leading-tight text-on-surface-variant flex flex-wrap items-center gap-1 mt-1">
          <span class="material-symbols-outlined text-secondary shrink-0" style="font-size: 16px;">trending_flat</span>
          <span>Без изменений</span>
        </div>
      </div>
    </div>

    <!-- Metric 3 -->
    <div class="bento-card flex flex-col justify-between h-32 ambient-shadow">
      <div class="flex justify-between items-start w-full">
        <span class="font-label-caps text-label-caps text-on-surface-variant uppercase">Состояние системы</span>
        <span class="material-symbols-outlined text-secondary">health_and_safety</span>
      </div>
      <div>
        <div class="font-headline-lg-mobile text-headline-lg-mobile text-on-surface">99.9%</div>
        <div class="font-body-sm text-[11px] leading-tight text-on-surface-variant flex flex-wrap items-center gap-1 mt-1">
          <span class="material-symbols-outlined text-primary shrink-0" style="font-size: 16px;">check_circle</span>
          <span class="font-semibold text-primary">Оптимально</span>
        </div>
      </div>
    </div>

    <!-- Metric 4 -->
    <div class="bento-card flex flex-col justify-between h-32 ambient-shadow bg-surface-container-low border-none">
      <div class="flex justify-between items-start w-full">
        <span class="font-label-caps text-label-caps text-on-surface-variant uppercase">Ожидающие действия</span>
        <span class="material-symbols-outlined text-secondary">pending_actions</span>
      </div>
      <div>
        <div class="font-headline-lg-mobile text-headline-lg-mobile text-on-surface">14</div>
        <div class="font-body-sm text-[11px] leading-tight text-on-surface-variant flex flex-wrap items-center gap-1 mt-1">
          <span>Требует внимания</span>
        </div>
      </div>
    </div>
  </section>

  <!-- Main Content Area: Activity & Quick Actions -->
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-lg w-full mt-sm">
    <!-- Recent Activity List -->
    <section class="lg:col-span-2 flex flex-col gap-sm">
      <div class="flex justify-between items-center mb-xs">
        <h2 class="font-headline-md text-headline-md text-on-surface">Последняя активность</h2>
        <button
          type="button"
          onclick={() => triggerAction('Показать все активности')}
          class="font-label-caps text-label-caps text-primary hover:underline uppercase focus:outline-none"
        >
          Показать все
        </button>
      </div>
      <div class="bento-card flex flex-col p-0 ambient-shadow overflow-hidden">
        <!-- Activity Item 1 -->
        <button
          type="button"
          onclick={() => triggerAction('Сгенерирован финансовый отчет за 3-й квартал')}
          class="flex items-center p-sm md:p-md border-b border-surface-container w-full min-h-[64px] hover:bg-surface-bright transition-colors cursor-pointer group text-left focus:outline-none focus:bg-surface-bright"
        >
          <div class="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-secondary mr-sm shrink-0">
            <span class="material-symbols-outlined">description</span>
          </div>
          <div class="flex-grow">
            <h3 class="font-body-md text-body-md text-on-surface font-semibold group-hover:text-primary transition-colors">Сгенерирован финансовый отчет за 3-й квартал</h3>
            <p class="font-body-sm text-body-sm text-on-surface-variant">Система автоматизирована</p>
          </div>
          <div class="text-right shrink-0 ml-sm">
            <span class="font-label-caps text-label-caps text-on-surface-variant">10 МИНУТ НАЗАД</span>
          </div>
        </button>

        <!-- Activity Item 2 -->
        <button
          type="button"
          onclick={() => triggerAction('Зарегистрирован новый пользователь Сара Дженкинс')}
          class="flex items-center p-sm md:p-md border-b border-surface-container w-full min-h-[64px] hover:bg-surface-bright transition-colors cursor-pointer group text-left focus:outline-none focus:bg-surface-bright"
        >
          <div class="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-secondary mr-sm shrink-0">
            <span class="material-symbols-outlined">person_add</span>
          </div>
          <div class="flex-grow">
            <h3 class="font-body-md text-body-md text-on-surface font-semibold group-hover:text-primary transition-colors">Зарегистрирован новый пользователь</h3>
            <p class="font-body-sm text-body-sm text-on-surface-variant">Сара Дженкинс (Отдел продаж)</p>
          </div>
          <div class="text-right shrink-0 ml-sm">
            <span class="font-label-caps text-label-caps text-on-surface-variant">2 ЧАСА НАЗАД</span>
          </div>
        </button>

        <!-- Activity Item 3 -->
        <button
          type="button"
          onclick={() => triggerAction('Синхронизация базы данных завершена')}
          class="flex items-center p-sm md:p-md w-full min-h-[64px] hover:bg-surface-bright transition-colors cursor-pointer group text-left focus:outline-none focus:bg-surface-bright"
        >
          <div class="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-secondary mr-sm shrink-0">
            <span class="material-symbols-outlined">cloud_sync</span>
          </div>
          <div class="flex-grow">
            <h3 class="font-body-md text-body-md text-on-surface font-semibold group-hover:text-primary transition-colors">Синхронизация базы данных завершена</h3>
            <p class="font-body-sm text-body-sm text-on-surface-variant">Регион US-East</p>
          </div>
          <div class="text-right shrink-0 ml-sm">
            <span class="font-label-caps text-label-caps text-on-surface-variant">5 ЧАСОВ НАЗАД</span>
          </div>
        </button>
      </div>
    </section>

    <!-- Quick Actions & Context -->
    <section class="lg:col-span-1 flex flex-col gap-sm">
      <h2 class="font-headline-md text-headline-md text-on-surface mb-xs">Быстрые инструменты</h2>
      <div class="bento-card ambient-shadow flex flex-col gap-sm">
        <button
          type="button"
          onclick={() => triggerAction('Экспорт текущего вида')}
          class="w-full flex items-center justify-start gap-sm p-sm rounded hover:bg-surface-container-low transition-colors text-on-surface font-body-md text-body-md border border-transparent hover:border-outline-variant focus:outline-none focus:border-outline-variant"
        >
          <span class="material-symbols-outlined text-secondary">file_download</span>
          <span>Экспорт текущего вида</span>
        </button>
        <button
          type="button"
          onclick={() => triggerAction('Поделиться ссылкой панели')}
          class="w-full flex items-center justify-start gap-sm p-sm rounded hover:bg-surface-container-low transition-colors text-on-surface font-body-md text-body-md border border-transparent hover:border-outline-variant focus:outline-none focus:border-outline-variant"
        >
          <span class="material-symbols-outlined text-secondary">share</span>
          <span>Поделиться ссылкой панели</span>
        </button>
        <button
          type="button"
          onclick={() => triggerAction('Настроить макет')}
          class="w-full flex items-center justify-start gap-sm p-sm rounded hover:bg-surface-container-low transition-colors text-on-surface font-body-md text-body-md border border-transparent hover:border-outline-variant focus:outline-none focus:border-outline-variant"
        >
          <span class="material-symbols-outlined text-secondary">settings_suggest</span>
          <span>Настроить макет</span>
        </button>
      </div>

      <!-- Visual Context Area (Image/Graphic) -->
      <button
        type="button"
        onclick={() => triggerAction('Переход к описанию релиза')}
        class="mt-sm rounded-xl overflow-hidden h-48 ambient-shadow relative group text-left w-full block focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
      >
        <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" alt="Текстурированный фон" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCophFntr39N9H99DAOwWJJ6eWbtYANhjZnqwtf5hctoNi1hDZBdXMaza_O08VoR25EImch4G-evGlYutRzvDsuv7xUT1D7DT08yL2Gw6q5OjPj9ASy4aRQ6yvUYUl6R1SF20JjBrfN3L25OwZ3mfOPsFU1KLZNMlTfShEy0LqeTMOl0dibvMxMlILqQgFoGmzDzusRpJ_M0eUtpNarhimhTyJZWEVWx_hEBbcoAi76jbFF0O2vU9sapnbwzNyBMCTvDgYKlFtc-SGl"/>
        <div class="absolute inset-0 bg-gradient-to-t from-primary-container/80 to-transparent flex items-end p-md">
          <div class="text-on-primary">
            <h4 class="font-body-md text-body-md font-semibold">Обновление системы V2.4</h4>
            <p class="font-body-sm text-body-sm opacity-80">Читать описание релиза</p>
          </div>
        </div>
      </button>
    </section>
  </div>
</div>
