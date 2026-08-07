<script>
  import { onMount } from 'svelte';

  // Props
  let { onBack = () => {} } = $props();

  // State
  let activeTab = $state('main'); // 'main', 'analytics', 'events', 'settings'
  let userId = '00000000-0000-0000-0000-000000000001';

  // Notification settings state
  let telegramChatId = $state('');
  let maxChatId = $state('');
  let notifyOnUpdate = $state(true);
  let isSavingSettings = $state(false);
  let showSaveSuccess = $state(false);
  let settingsError = $state('');

  // Analytics stats state
  let analyticsStats = $state({
    totalDownloads: 0,
    documentDownloads: [],
    dailyDownloads: []
  });
  let isLoadingAnalytics = $state(false);
  let analyticsError = $state('');

  // Svelte 5 derived states for the line chart
  let maxVal = $derived(
    analyticsStats.dailyDownloads.length > 0
      ? Math.max(...analyticsStats.dailyDownloads.map(d => d.count), 5)
      : 5
  );

  let chartPoints = $derived(
    analyticsStats.dailyDownloads.map((d, index) => {
      const x = 40 + (index * (440 / 6));
      const y = 170 - (d.count * (150 / maxVal));
      return { x, y, date: d.date, count: d.count };
    })
  );

  let polylinePointsString = $derived(
    chartPoints.map(p => `${p.x},${p.y}`).join(' ')
  );

  let fillPointsString = $derived(
    chartPoints.length > 0 ? `40,170 ${polylinePointsString} 480,170` : ''
  );

  // Mock events log (entirely in Russian)
  const mockEvents = [
    { id: 1, time: '10:45', msg: 'Уведомление отправлено в Telegram канал @cniiep_edu_updates', doc: 'Положение о бюджете на 2026-2027 годы' },
    { id: 2, time: '10:42', msg: 'Синхронизация с Teachbase завершена успешно', details: 'Обработано 24 учебных курса' },
    { id: 3, time: '09:15', msg: 'Уведомление Max отправлено пользователю max_user_1', doc: 'Порядок расчета учебной нагрузки преподавателей' },
    { id: 4, time: '08:00', msg: 'Ежедневная резервная копия базы данных успешно создана', details: 'Размер архива: 14.2 MB' },
    { id: 5, time: 'Вчера', msg: 'Успешная авторизация нового администратора', details: 'Адрес IP: 192.168.1.105' }
  ];

  // Fetch notification preferences from the backend
  async function loadPreferences() {
    try {
      const res = await fetch(`/api/v1/notifications/preferences?userId=${userId}`);
      if (res.ok) {
        const data = await res.json();
        telegramChatId = data.telegramChatId || '';
        maxChatId = data.maxChatId || '';
        notifyOnUpdate = data.notifyOnDocumentUpdate !== false;
      } else {
        settingsError = 'Не удалось загрузить настройки с сервера';
      }
    } catch (err) {
      settingsError = 'Сбой сети при загрузке настроек';
    }
  }

  // Save notification preferences to the backend
  async function savePreferences() {
    isSavingSettings = true;
    showSaveSuccess = false;
    settingsError = '';

    try {
      const res = await fetch('/api/v1/notifications/preferences', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userId,
          telegramChatId: telegramChatId,
          maxChatId: maxChatId,
          notifyOnDocumentUpdate: notifyOnUpdate
        })
      });

      if (res.ok) {
        showSaveSuccess = true;
        setTimeout(() => {
          showSaveSuccess = false;
        }, 3000);
      } else {
        settingsError = 'Не удалось сохранить настройки';
      }
    } catch (err) {
      settingsError = 'Сбой сети при сохранении настроек';
    } finally {
      isSavingSettings = false;
    }
  }

  // Fetch analytics stats from the backend
  async function loadAnalytics() {
    isLoadingAnalytics = true;
    analyticsError = '';

    try {
      const res = await fetch('/api/v1/analytics/stats');
      if (res.ok) {
        analyticsStats = await res.json();
      } else {
        analyticsError = 'Не удалось загрузить статистику аналитики';
      }
    } catch (err) {
      analyticsError = 'Сбой сети при загрузке аналитики';
    } finally {
      isLoadingAnalytics = false;
    }
  }

  onMount(() => {
    loadPreferences();
    loadAnalytics();
  });
</script>

<div class="min-h-screen bg-[#F8FAFC] text-[#191C1E] antialiased flex flex-col font-sans pb-24 md:pb-8 pt-14">

  <!-- TopAppBar conforming to mockup -->
  <header class="fixed top-0 w-full flex items-center justify-between px-4 h-14 z-50 bg-white border-b border-[#E2E8F0] shadow-sm">
    <button
      onclick={onBack}
      class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-slate-100 active:scale-95 transition-all text-[#1A365D]"
      aria-label="Назад к документам"
    >
      <span class="material-symbols-outlined">arrow_back</span>
    </button>
    <h1 class="text-base md:text-lg font-bold text-[#1A365D] truncate max-w-[60%] text-center">
      Управление интеграциями
    </h1>
    <button
      onclick={() => activeTab = 'settings'}
      class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-slate-100 active:scale-95 transition-all text-[#1A365D] {activeTab === 'settings' ? 'bg-slate-100' : ''}"
      aria-label="Открыть настройки"
    >
      <span class="material-symbols-outlined">settings</span>
    </button>
  </header>

  <!-- Desktop Header Tabs -->
  <div class="hidden md:flex bg-white border-b border-[#E2E8F0] py-2 px-6 justify-center gap-4">
    <button
      onclick={() => activeTab = 'main'}
      class="px-4 py-2 text-sm font-semibold rounded-lg transition-all {activeTab === 'main' ? 'bg-[#3182CE] text-white' : 'text-slate-600 hover:bg-slate-100'}"
    >
      Главная
    </button>
    <button
      onclick={() => activeTab = 'analytics'}
      class="px-4 py-2 text-sm font-semibold rounded-lg transition-all {activeTab === 'analytics' ? 'bg-[#3182CE] text-white' : 'text-slate-600 hover:bg-slate-100'}"
    >
      Аналитика
    </button>
    <button
      onclick={() => activeTab = 'events'}
      class="px-4 py-2 text-sm font-semibold rounded-lg transition-all {activeTab === 'events' ? 'bg-[#3182CE] text-white' : 'text-slate-600 hover:bg-slate-100'}"
    >
      События
    </button>
    <button
      onclick={() => activeTab = 'settings'}
      class="px-4 py-2 text-sm font-semibold rounded-lg transition-all {activeTab === 'settings' ? 'bg-[#3182CE] text-white' : 'text-slate-600 hover:bg-slate-100'}"
    >
      Настройки
    </button>
  </div>

  <!-- Main Content View -->
  <main class="flex-1 max-w-5xl w-full mx-auto p-4 md:p-6 flex flex-col gap-6">

    {#if activeTab === 'main'}
      <!-- MAIN TAB -->
      <div class="flex flex-col gap-6">
        <div>
          <h2 class="text-xl md:text-2xl font-bold text-[#1A365D]">Панель интеграций</h2>
          <p class="text-sm text-slate-500 mt-1">Мониторинг синхронизации и событий в реальном времени</p>
        </div>

        <!-- Bento Grid conforming to mockup -->
        <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- Active Syncs Card -->
          <div class="bg-white rounded-xl p-5 border border-[#E2E8F0] flex flex-col justify-between min-h-[120px] transition-all hover:border-[#3182CE]/40">
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Активные синхронизации</span>
              <span class="material-symbols-outlined text-[#3182CE]">sync</span>
            </div>
            <div class="mt-4">
              <div class="text-3xl font-bold text-[#1A365D]">24</div>
              <div class="text-xs text-[#3182CE] mt-1 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">arrow_upward</span> +3 за сутки
              </div>
            </div>
          </div>

          <!-- Total Events Card -->
          <div class="bg-white rounded-xl p-5 border border-[#E2E8F0] flex flex-col justify-between min-h-[120px] transition-all hover:border-[#3182CE]/40">
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Всего событий</span>
              <span class="material-symbols-outlined text-[#3182CE]">hub</span>
            </div>
            <div class="mt-4">
              <div class="text-3xl font-bold text-[#1A365D]">1.2M</div>
              <div class="text-xs text-slate-400 mt-1">За последние 24 часа</div>
            </div>
          </div>

          <!-- Error Rate Card -->
          <div class="bg-white rounded-xl p-5 border border-[#E2E8F0] flex flex-col justify-between min-h-[120px] transition-all hover:border-[#3182CE]/40">
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Доля ошибок</span>
              <span class="material-symbols-outlined text-emerald-600">check_circle</span>
            </div>
            <div class="mt-4">
              <div class="text-3xl font-bold text-emerald-600">0.05%</div>
              <div class="text-xs text-slate-400 mt-1">Система работает в штатном режиме</div>
            </div>
          </div>
        </section>

        <!-- Integrations and Activity Sections -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

          <!-- Active Integrations -->
          <section class="flex flex-col gap-4">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-bold text-[#1A365D]">Подключенные системы</h3>
              <button class="text-xs font-bold uppercase text-[#3182CE] hover:underline">Все</button>
            </div>

            <div class="flex flex-col gap-3">
              <!-- Moodle -->
              <div class="bg-white p-4 rounded-xl border border-[#E2E8F0] flex items-center justify-between transition-all hover:border-[#3182CE]/20">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-[#F9F9FF] rounded-full flex items-center justify-center text-[#1A365D]">
                    <span class="material-symbols-outlined">school</span>
                  </div>
                  <div>
                    <h4 class="text-sm font-bold text-[#1A365D]">Платформа обучения Moodle</h4>
                    <p class="text-xs text-slate-400">Интеграция: Активна • Синхронизация по расписанию</p>
                  </div>
                </div>
                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-[#E5EEFF] text-[#3182CE] border border-[#3182CE]/10">РАБОТАЕТ</span>
              </div>

              <!-- Teachbase -->
              <div class="bg-white p-4 rounded-xl border border-[#E2E8F0] flex items-center justify-between transition-all hover:border-[#3182CE]/20">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-[#F9F9FF] rounded-full flex items-center justify-center text-[#1A365D]">
                    <span class="material-symbols-outlined">menu_book</span>
                  </div>
                  <div>
                    <h4 class="text-sm font-bold text-[#1A365D]">Образовательная среда Teachbase</h4>
                    <p class="text-xs text-slate-400">Интеграция: Активна • Webhook (В реальном времени)</p>
                  </div>
                </div>
                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-[#E5EEFF] text-[#3182CE] border border-[#3182CE]/10">РАБОТАЕТ</span>
              </div>

              <!-- Telegram Dispatcher -->
              <div class="bg-white p-4 rounded-xl border border-[#E2E8F0] flex items-center justify-between transition-all hover:border-[#3182CE]/20">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-[#F9F9FF] rounded-full flex items-center justify-center text-[#1A365D]">
                    <span class="material-symbols-outlined">send</span>
                  </div>
                  <div>
                    <h4 class="text-sm font-bold text-[#1A365D]">Telegram Оповещения</h4>
                    <p class="text-xs text-slate-400">Канал: @cniiep_edu_updates • Статус подключения: ОК</p>
                  </div>
                </div>
                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-[#E5EEFF] text-[#3182CE] border border-[#3182CE]/10">АКТИВНО</span>
              </div>
            </div>
          </section>

          <!-- Recent Activity -->
          <section class="flex flex-col gap-4">
            <h3 class="text-lg font-bold text-[#1A365D]">Последняя активность</h3>
            <div class="bg-white rounded-xl border border-[#E2E8F0] overflow-hidden">
              {#each mockEvents as event}
                <div class="p-4 border-b border-slate-100 last:border-0 hover:bg-[#F9F9FF] transition-all flex gap-3">
                  <span class="material-symbols-outlined text-[#3182CE] text-lg mt-0.5">circle_notifications</span>
                  <div class="flex-1">
                    <div class="text-xs font-semibold text-[#1A365D]">{event.msg}</div>
                    {#if event.doc}
                      <div class="text-[11px] text-slate-400 mt-0.5 font-semibold">Документ: {event.doc}</div>
                    {/if}
                    {#if event.details}
                      <div class="text-[11px] text-slate-400 mt-0.5">{event.details}</div>
                    {/if}
                  </div>
                  <span class="text-[10px] text-slate-400 font-mono whitespace-nowrap">{event.time}</span>
                </div>
              {/each}
            </div>
          </section>

        </div>
      </div>

    {:else if activeTab === 'analytics'}
      <!-- ANALYTICS TAB (featuring Lexicon Flux charts) -->
      <div class="flex flex-col gap-6">
        <div>
          <h2 class="text-xl md:text-2xl font-bold text-[#1A365D]">Аналитика загрузок</h2>
          <p class="text-sm text-slate-500 mt-1">Официальные статистические показатели ЦНИИ Эпидемиологии</p>
        </div>

        {#if isLoadingAnalytics}
          <div class="flex flex-col items-center justify-center py-12 gap-2 text-[#3182CE]">
            <span class="material-symbols-outlined animate-spin text-3xl">sync</span>
            <span class="text-sm font-semibold">Идет получение данных аналитики...</span>
          </div>
        {:else if analyticsError}
          <div class="bg-[#ffdad6] text-[#93000a] p-4 rounded-lg border border-[#ba1a1a] flex items-center gap-3">
            <span class="material-symbols-outlined">error</span>
            <span class="font-semibold text-sm">{analyticsError}</span>
          </div>
        {:else}
          <!-- Total download KPI -->
          <div class="bg-white rounded-xl p-6 border border-[#E2E8F0] flex items-center justify-between">
            <div>
              <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Всего скачиваний за период</span>
              <div class="text-3xl font-bold text-[#1A365D] mt-1 font-mono">{analyticsStats.totalDownloads}</div>
            </div>
            <div class="bg-[#E5EEFF] text-[#3182CE] p-3 rounded-xl">
              <span class="material-symbols-outlined text-2xl">download_for_offline</span>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

            <!-- Chart 1: Daily Downloads (Line Chart) -->
            <div class="bg-white p-5 rounded-xl border border-[#E2E8F0] flex flex-col gap-4">
              <div>
                <h3 class="text-sm font-bold text-[#1A365D] uppercase tracking-wide">Динамика скачиваний за неделю</h3>
                <p class="text-xs text-slate-400">Количество загрузок по дням</p>
              </div>

              <!-- Lexicon Flux Line Chart implemented in custom responsive SVG -->
              <div class="w-full h-48 bg-[#F9F9FF] border border-slate-100 rounded-lg p-2 flex items-center justify-center relative">
                {#if analyticsStats.dailyDownloads.length > 0}
                  <svg viewBox="0 0 500 200" class="w-full h-full overflow-visible">
                    <!-- Grid Lines -->
                    <line x1="40" y1="20" x2="480" y2="20" stroke="#E2E8F0" stroke-dasharray="3" />
                    <line x1="40" y1="70" x2="480" y2="70" stroke="#E2E8F0" stroke-dasharray="3" />
                    <line x1="40" y1="120" x2="480" y2="120" stroke="#E2E8F0" stroke-dasharray="3" />
                    <line x1="40" y1="170" x2="480" y2="170" stroke="#E2E8F0" />

                    <!-- Render Area under Line -->
                    <polygon points={fillPointsString} fill="rgba(49, 130, 206, 0.08)" />

                    <!-- Render Line -->
                    <polyline points={polylinePointsString} fill="none" stroke="#3182CE" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />

                    <!-- Render Data Dots & Text Labels -->
                    {#each chartPoints as pt}
                      <circle cx={pt.x} cy={pt.y} r="4" fill="#1A365D" stroke="#3182CE" stroke-width="1.5" />
                      <!-- Value text above dot -->
                      <text x={pt.x} y={pt.y - 8} text-anchor="middle" font-family="JetBrains Mono" font-size="10" fill="#1A365D" font-weight="bold">
                        {pt.count}
                      </text>
                      <!-- Date text below axis -->
                      <text x={pt.x} y="188" text-anchor="middle" font-family="Inter" font-size="9" fill="#94A3B8">
                        {pt.date.substring(5)}
                      </text>
                    {/each}
                  </svg>
                {:else}
                  <div class="text-xs text-slate-400">Данные отсутствуют</div>
                {/if}
              </div>
            </div>

            <!-- Chart 2: Top Documents (Bar Chart) -->
            <div class="bg-white p-5 rounded-xl border border-[#E2E8F0] flex flex-col gap-4">
              <div>
                <h3 class="text-sm font-bold text-[#1A365D] uppercase tracking-wide">Рейтинг популярных документов</h3>
                <p class="text-xs text-slate-400">Наиболее скачиваемые материалы регламентов ЦНИИ</p>
              </div>

              <!-- Lexicon Flux Horizontal Bar Chart implemented cleanly with HTML/CSS -->
              <div class="flex flex-col gap-3">
                {#if analyticsStats.documentDownloads.length > 0}
                  {@const maxDocVal = Math.max(...analyticsStats.documentDownloads.map(d => d.count), 1)}
                  {#each analyticsStats.documentDownloads.slice(0, 4) as doc}
                    {@const percent = (doc.count / maxDocVal) * 100}
                    <div class="flex flex-col gap-1">
                      <div class="flex justify-between items-center text-xs">
                        <span class="font-semibold text-slate-600 truncate max-w-[80%]" title={doc.title}>{doc.title}</span>
                        <span class="font-mono font-bold text-[#1A365D]">{doc.count} шт.</span>
                      </div>
                      <div class="w-full h-3 bg-slate-100 rounded-sm overflow-hidden border border-slate-200/50">
                        <div class="h-full bg-gradient-to-r from-[#1A365D] to-[#3182CE] rounded-sm transition-all duration-500" style="width: {percent}%"></div>
                      </div>
                    </div>
                  {/each}
                {:else}
                  <div class="text-xs text-slate-400 py-6 text-center">Данные отсутствуют</div>
                {/if}
              </div>
            </div>

          </div>
        {/if}
      </div>

    {:else if activeTab === 'events'}
      <!-- EVENTS LOG TAB -->
      <div class="flex flex-col gap-6">
        <div>
          <h2 class="text-xl md:text-2xl font-bold text-[#1A365D]">Журнал событий оповещений</h2>
          <p class="text-sm text-slate-500 mt-1">Хронологический список отправленных сигналов и триггеров</p>
        </div>

        <div class="bg-white rounded-xl border border-[#E2E8F0] overflow-hidden flex flex-col">
          <div class="p-4 bg-[#F9F9FF] border-b border-[#E2E8F0] flex justify-between items-center">
            <h3 class="font-bold text-sm text-[#1A365D]">Системный аудит отправки</h3>
            <span class="text-[10px] font-bold text-[#3182CE] bg-[#E5EEFF] px-2 py-0.5 rounded border border-[#3182CE]/10">100% ДОСТАВКА</span>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 border-b border-[#E2E8F0] text-xs font-bold text-slate-400 uppercase tracking-wider">
                  <th class="p-4">Время</th>
                  <th class="p-4">Служба оповещения</th>
                  <th class="p-4">Связанный документ</th>
                  <th class="p-4">Информационное сообщение</th>
                  <th class="p-4">Статус</th>
                </tr>
              </thead>
              <tbody class="text-sm text-slate-600">
                <tr class="border-b border-[#E2E8F0] hover:bg-[#F9F9FF]">
                  <td class="p-4 font-mono text-xs">Сегодня 10:45</td>
                  <td class="p-4 text-xs font-semibold text-[#1A365D]">Telegram API</td>
                  <td class="p-4 text-xs font-semibold">Положение о бюджете на 2026-2027 годы</td>
                  <td class="p-4 text-xs">Опубликован новый документ в канале @cniiep_edu_updates</td>
                  <td class="p-4"><span class="text-emerald-600 font-bold text-xs">ОТПРАВЛЕНО</span></td>
                </tr>
                <tr class="border-b border-[#E2E8F0] hover:bg-[#F9F9FF]">
                  <td class="p-4 font-mono text-xs">Сегодня 09:15</td>
                  <td class="p-4 text-xs font-semibold text-[#1A365D]">Max Webhook</td>
                  <td class="p-4 text-xs font-semibold">Порядок расчета нагрузки преподавателей</td>
                  <td class="p-4 text-xs">Уведомление доставлено получателю max_user_1</td>
                  <td class="p-4"><span class="text-emerald-600 font-bold text-xs">ДОСТАВЛЕНО</span></td>
                </tr>
                <tr class="border-b border-[#E2E8F0] hover:bg-[#F9F9FF]">
                  <td class="p-4 font-mono text-xs">Вчера 14:22</td>
                  <td class="p-4 text-xs font-semibold text-[#1A365D]">Telegram API</td>
                  <td class="p-4 text-xs font-semibold">Положение о стипендиальном обеспечении</td>
                  <td class="p-4 text-xs">Обновление сводки изменений отправлено подписчикам</td>
                  <td class="p-4"><span class="text-emerald-600 font-bold text-xs">ОТПРАВЛЕНО</span></td>
                </tr>
                <tr class="hover:bg-[#F9F9FF]">
                  <td class="p-4 font-mono text-xs">Вчера 11:05</td>
                  <td class="p-4 text-xs font-semibold text-[#1A365D]">Max Webhook</td>
                  <td class="p-4 text-xs font-semibold">Положение об оплате труда и штате</td>
                  <td class="p-4 text-xs">Уведомление о новой версии доставлено получателю max_user_2</td>
                  <td class="p-4"><span class="text-emerald-600 font-bold text-xs">ДОСТАВЛЕНО</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

    {:else if activeTab === 'settings'}
      <!-- SETTINGS TAB (100% in Russian) -->
      <div class="flex flex-col gap-6">
        <div>
          <h2 class="text-xl md:text-2xl font-bold text-[#1A365D]">Настройки уведомлений</h2>
          <p class="text-sm text-slate-500 mt-1">Настройка параметров доставки оповещений при обновлении документов ЦНИИ</p>
        </div>

        <div class="bg-white p-6 rounded-xl border border-[#E2E8F0] flex flex-col gap-6">
          {#if settingsError}
            <div class="bg-[#ffdad6] text-[#93000a] p-4 rounded-lg border border-[#ba1a1a] flex items-center gap-3">
              <span class="material-symbols-outlined">error</span>
              <span class="font-semibold text-sm">{settingsError}</span>
            </div>
          {/if}

          {#if showSaveSuccess}
            <div class="bg-emerald-50 text-emerald-800 p-4 rounded-lg border border-emerald-500 flex items-center gap-3">
              <span class="material-symbols-outlined text-emerald-600">check_circle</span>
              <span class="font-semibold text-sm">Настройки успешно сохранены в базе данных!</span>
            </div>
          {/if}

          <!-- Telegram preference input -->
          <div class="flex flex-col gap-2">
            <label for="telegramChat" class="text-xs font-bold uppercase text-slate-400">Идентификатор чата или канала Telegram</label>
            <input
              id="telegramChat"
              type="text"
              bind:value={telegramChatId}
              placeholder="Пример: @cniiep_edu_updates или 123456789"
              class="w-full bg-[#F9F9FF] border border-[#E2E8F0] rounded-lg px-4 py-2 text-sm text-[#191C1E] font-medium focus:border-[#3182CE] focus:ring-0"
            />
            <p class="text-xs text-slate-400">В этот канал будут отправляться публикации о ежеквартальных пересмотрах.</p>
          </div>

          <!-- Max preference input -->
          <div class="flex flex-col gap-2">
            <label for="maxChat" class="text-xs font-bold uppercase text-slate-400">Идентификатор чата корпоративного мессенджера Max</label>
            <input
              id="maxChat"
              type="text"
              bind:value={maxChatId}
              placeholder="Пример: user_max_abc"
              class="w-full bg-[#F9F9FF] border border-[#E2E8F0] rounded-lg px-4 py-2 text-sm text-[#191C1E] font-medium focus:border-[#3182CE] focus:ring-0"
            />
            <p class="text-xs text-slate-400">В этот аккаунт будут отправляться оповещения при публикации новых версий документов.</p>
          </div>

          <!-- Checkbox notification setting -->
          <label class="flex items-start gap-3 cursor-pointer select-none">
            <input
              type="checkbox"
              bind:checked={notifyOnUpdate}
              class="rounded border-[#E2E8F0] text-[#3182CE] focus:ring-0 mt-1 cursor-pointer"
            />
            <div class="flex flex-col">
              <span class="text-sm font-semibold text-[#1A365D]">Активировать автоматическую рассылку уведомлений</span>
              <span class="text-xs text-slate-400 mt-0.5">Включает триггеры отправки сообщений при изменении статусов документов.</span>
            </div>
          </label>

          <!-- Submit save button -->
          <button
            type="button"
            onclick={savePreferences}
            disabled={isSavingSettings}
            class="mt-4 bg-[#3182CE] hover:bg-[#3182CE]/90 disabled:bg-slate-300 text-white font-semibold text-sm py-2.5 px-6 rounded-lg self-start transition-all active:scale-95 flex items-center gap-2"
          >
            {#if isSavingSettings}
              <span class="material-symbols-outlined animate-spin text-sm">sync</span>
              <span>Сохранение...</span>
            {:else}
              <span class="material-symbols-outlined text-sm">save</span>
              <span>Сохранить настройки</span>
            {/if}
          </button>
        </div>
      </div>
    {/if}

  </main>

  <!-- Responsive BottomNavBar (Mobile Only conforming to mockup) -->
  <nav class="md:hidden fixed bottom-0 left-0 w-full flex justify-around items-center px-4 py-2 bg-white border-t border-[#E2E8F0] shadow-lg z-50">
    <!-- Главная tab button -->
    <button
      onclick={() => activeTab = 'main'}
      class="flex flex-col items-center justify-center p-2 min-w-[64px] rounded-xl active:scale-90 transition-all {activeTab === 'main' ? 'bg-[#E5EEFF] text-[#3182CE]' : 'text-slate-400'}"
    >
      <span class="material-symbols-outlined {activeTab === 'main' ? 'filled' : ''}">home</span>
      <span class="text-[10px] font-bold mt-1">Главная</span>
    </button>

    <!-- Аналитика tab button -->
    <button
      onclick={() => activeTab = 'analytics'}
      class="flex flex-col items-center justify-center p-2 min-w-[64px] rounded-xl active:scale-90 transition-all {activeTab === 'analytics' ? 'bg-[#E5EEFF] text-[#3182CE]' : 'text-slate-400'}"
    >
      <span class="material-symbols-outlined {activeTab === 'analytics' ? 'filled' : ''}">analytics</span>
      <span class="text-[10px] font-bold mt-1">Аналитика</span>
    </button>

    <!-- События tab button -->
    <button
      onclick={() => activeTab = 'events'}
      class="flex flex-col items-center justify-center p-2 min-w-[64px] rounded-xl active:scale-90 transition-all {activeTab === 'events' ? 'bg-[#E5EEFF] text-[#3182CE]' : 'text-slate-400'}"
    >
      <span class="material-symbols-outlined {activeTab === 'events' ? 'filled' : ''}">notifications</span>
      <span class="text-[10px] font-bold mt-1">События</span>
    </button>

    <!-- Настройки tab button -->
    <button
      onclick={() => activeTab = 'settings'}
      class="flex flex-col items-center justify-center p-2 min-w-[64px] rounded-xl active:scale-90 transition-all {activeTab === 'settings' ? 'bg-[#E5EEFF] text-[#3182CE]' : 'text-slate-400'}"
    >
      <span class="material-symbols-outlined {activeTab === 'settings' ? 'filled' : ''}">settings</span>
      <span class="text-[10px] font-bold mt-1">Настройки</span>
    </button>
  </nav>

</div>
