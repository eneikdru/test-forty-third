<script>
  import { onMount } from 'svelte';

  // Svelte 5 state runes
  let telegramChatId = $state('');
  let maxChatId = $state('');
  let notifyOnDocumentUpdate = $state(true);

  let stats = $state({ daily: [], popular: [] });
  let loadingPrefs = $state(false);
  let loadingStats = $state(false);
  let savingPrefs = $state(false);
  let saveSuccessMessage = $state('');
  let errorMessage = $state('');

  // Fetch initial preferences
  async function fetchPreferences() {
    loadingPrefs = true;
    errorMessage = '';
    try {
      const res = await fetch('/api/v1/notifications/preferences');
      if (res.ok) {
        const data = await res.json();
        telegramChatId = data.telegramChatId || '';
        maxChatId = data.maxChatId || '';
        notifyOnDocumentUpdate = data.notifyOnDocumentUpdate !== false;
      } else {
        errorMessage = 'Ошибка при получении настроек уведомлений.';
      }
    } catch (err) {
      errorMessage = 'Ошибка подключения к серверу при загрузке настроек.';
    } finally {
      loadingPrefs = false;
    }
  }

  // Fetch statistics
  async function fetchStats() {
    loadingStats = true;
    try {
      const res = await fetch('/api/v1/analytics/download-stats');
      if (res.ok) {
        stats = await res.json();
      }
    } catch (err) {
      console.error('Ошибка при получении аналитики', err);
    } finally {
      loadingStats = false;
    }
  }

  // Save preferences
  async function savePreferences(event) {
    event.preventDefault();
    savingPrefs = true;
    saveSuccessMessage = '';
    errorMessage = '';

    try {
      const res = await fetch('/api/v1/notifications/preferences', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          telegramChatId,
          maxChatId,
          notifyOnDocumentUpdate
        })
      });

      if (res.ok) {
        saveSuccessMessage = 'Настройки уведомлений успешно сохранены!';
        setTimeout(() => {
          saveSuccessMessage = '';
        }, 4000);
      } else {
        errorMessage = 'Не удалось сохранить настройки на сервере.';
      }
    } catch (err) {
      errorMessage = 'Ошибка сети при попытке сохранить настройки.';
    } finally {
      savingPrefs = false;
    }
  }

  // Trigger file download
  function downloadReport(format) {
    window.open(`/api/v1/analytics/export?format=${format}`, '_blank');
  }

  onMount(() => {
    fetchPreferences();
    fetchStats();
  });

  // Calculate the maximum value to scale our SVG chart nicely
  let maxDownloads = $derived.by(() => {
    if (!stats.daily || stats.daily.length === 0) return 10;
    return Math.max(...stats.daily.map(d => d.downloads), 10);
  });
</script>

<div class="flex flex-col gap-8 w-full max-w-5xl mx-auto px-4 md:px-0">

  <!-- Заголовок раздела -->
  <div class="flex flex-col gap-2">
    <h2 class="text-2xl font-bold text-[#1A365D] tracking-tight">Интеграция, уведомления и аналитика</h2>
    <p class="text-sm text-[#515f74]">
      Управление параметрами интеграции с мессенджерами Telegram/Max и просмотр статистики использования базы знаний ЦНИИ Эпидемиологии.
    </p>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">

    <!-- Левая колонка: Настройки уведомлений (7 колонок) -->
    <section class="lg:col-span-5 bg-white p-6 rounded-lg border border-[#E2E8F0] shadow-sm flex flex-col gap-6" aria-labelledby="notification-settings-title">
      <div class="border-b border-[#E2E8F0] pb-4">
        <h3 id="notification-settings-title" class="text-lg font-bold text-[#1A365D] flex items-center gap-2">
          <span class="material-symbols-outlined text-[#3182CE]">settings_suggest</span>
          <span>Настройки уведомлений</span>
        </h3>
        <p class="text-xs text-[#515f74] mt-1">Настройка отправки алертов о публикации и пересмотре нормативных актов.</p>
      </div>

      {#if loadingPrefs}
        <div class="flex flex-col items-center justify-center py-8 gap-2 text-[#515f74]">
          <span class="material-symbols-outlined animate-spin">sync</span>
          <span class="text-xs font-semibold">Загрузка параметров...</span>
        </div>
      {:else}
        <form onsubmit={savePreferences} class="flex flex-col gap-5">

          <!-- Оповещения и статусы успехов/ошибок -->
          {#if saveSuccessMessage}
            <div class="p-3 bg-[#EBF8FF] text-[#2B6CB0] border border-[#BEE3F8] text-xs font-semibold rounded-md flex items-center gap-2" role="status">
              <span class="material-symbols-outlined text-sm">check_circle</span>
              <span>{saveSuccessMessage}</span>
            </div>
          {/if}

          {#if errorMessage}
            <div class="p-3 bg-[#FFF5F5] text-[#C53030] border border-[#FED7D7] text-xs font-semibold rounded-md flex items-center gap-2" role="alert">
              <span class="material-symbols-outlined text-sm">error</span>
              <span>{errorMessage}</span>
            </div>
          {/if}

          <!-- Поле Telegram -->
          <div class="flex flex-col gap-1.5">
            <label for="telegram-id" class="text-xs font-bold text-[#1A365D] uppercase tracking-wider">Канал Telegram (Идентификатор)</label>
            <div class="relative flex items-center">
              <span class="absolute left-3 text-[#515f74] text-sm font-mono">@</span>
              <input
                id="telegram-id"
                type="text"
                bind:value={telegramChatId}
                placeholder="cniiep_edu_updates"
                class="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-md pl-8 pr-3 py-2 text-sm text-[#1A365D] placeholder:text-[#94A3B8] focus:border-[#3182CE] focus:ring-1 focus:ring-[#3182CE] outline-none transition-all"
              />
            </div>
            <span class="text-[10px] text-[#515f74]">Идентификатор публичного канала или чата для публикации алертов.</span>
          </div>

          <!-- Поле Max -->
          <div class="flex flex-col gap-1.5">
            <label for="max-id" class="text-xs font-bold text-[#1A365D] uppercase tracking-wider">Чат Max (Идентификатор чата)</label>
            <input
              id="max-id"
              type="text"
              bind:value={maxChatId}
              placeholder="Введите ID чата Max"
              class="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-md px-3 py-2 text-sm text-[#1A365D] placeholder:text-[#94A3B8] focus:border-[#3182CE] focus:ring-1 focus:ring-[#3182CE] outline-none transition-all"
            />
            <span class="text-[10px] text-[#515f74]">Идентификатор группы или ответственного лица в мессенджере Max.</span>
          </div>

          <!-- Чекбокс активности -->
          <div class="flex items-start gap-3 mt-1">
            <input
              id="notify-updates"
              type="checkbox"
              bind:checked={notifyOnDocumentUpdate}
              class="w-4 h-4 text-[#3182CE] border-[#E2E8F0] rounded focus:ring-[#3182CE] cursor-pointer mt-0.5"
            />
            <div class="flex flex-col">
              <label for="notify-updates" class="text-xs font-bold text-[#1A365D] cursor-pointer">Автоматическая рассылка</label>
              <span class="text-[11px] text-[#515f74]">Отправлять уведомления в мессенджеры при каждом утверждении или обновлении документов.</span>
            </div>
          </div>

          <!-- Кнопка сохранения -->
          <button
            type="submit"
            disabled={savingPrefs}
            class="mt-2 w-full flex items-center justify-center gap-2 bg-[#3182CE] hover:bg-[#2B6CB0] text-white text-sm font-semibold py-2 px-4 rounded-md focus:outline-none focus:ring-2 focus:ring-[#1A365D] focus:ring-offset-2 disabled:bg-[#94A3B8] disabled:cursor-not-allowed transition-all"
          >
            {#if savingPrefs}
              <span class="material-symbols-outlined animate-spin text-sm">sync</span>
              <span>Сохранение изменений...</span>
            {:else}
              <span class="material-symbols-outlined text-sm">save</span>
              <span>Сохранить настройки</span>
            {/if}
          </button>

        </form>
      {/if}
    </section>

    <!-- Правая колонка: Аналитика скачиваний (7 колонок) -->
    <section class="lg:col-span-7 bg-white p-6 rounded-lg border border-[#E2E8F0] shadow-sm flex flex-col gap-6" aria-labelledby="analytics-dashboard-title">
      <div class="border-b border-[#E2E8F0] pb-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div>
          <h3 id="analytics-dashboard-title" class="text-lg font-bold text-[#1A365D] flex items-center gap-2">
            <span class="material-symbols-outlined text-[#3182CE]">bar_chart</span>
            <span>Статистика и аналитика</span>
          </h3>
          <p class="text-xs text-[#515f74] mt-1">Контроль востребованности нормативной базы образовательного центра.</p>
        </div>

        <!-- Кнопки экспорта -->
        <div class="flex gap-1.5">
          <button
            onclick={() => downloadReport('CSV')}
            class="flex items-center gap-1 bg-[#F8FAFC] border border-[#E2E8F0] hover:bg-[#EFF6FF] text-xs font-bold text-[#1A365D] px-2.5 py-1.5 rounded transition-all focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
            title="Экспортировать отчет в формате CSV"
          >
            <span class="material-symbols-outlined text-xs">download</span>
            <span>CSV</span>
          </button>
          <button
            onclick={() => downloadReport('PDF')}
            class="flex items-center gap-1 bg-[#F8FAFC] border border-[#E2E8F0] hover:bg-[#EFF6FF] text-xs font-bold text-[#1A365D] px-2.5 py-1.5 rounded transition-all focus:outline-none focus:ring-2 focus:ring-[#3182CE]"
            title="Экспортировать отчет в формате PDF"
          >
            <span class="material-symbols-outlined text-xs">picture_as_pdf</span>
            <span>PDF</span>
          </button>
        </div>
      </div>

      {#if loadingStats}
        <div class="flex flex-col items-center justify-center py-16 gap-2 text-[#515f74]">
          <span class="material-symbols-outlined animate-spin text-2xl">sync</span>
          <span class="text-xs font-semibold">Загрузка данных аналитики...</span>
        </div>
      {:else}
        <div class="flex flex-col gap-6">

          <!-- График Lexicon Flux -->
          <div class="flex flex-col gap-2">
            <h4 class="text-xs font-bold text-[#1A365D] uppercase tracking-wider">Динамика скачиваний (по дням)</h4>

            <!-- SVG Chart -->
            <div class="bg-[#F8FAFC] p-4 rounded border border-[#E2E8F0] flex flex-col gap-3">
              <div class="h-40 w-full relative flex items-end justify-between px-2 pt-4 border-b border-[#E2E8F0]">
                {#if stats.daily && stats.daily.length > 0}
                  {#each stats.daily as day}
                    {@const barHeight = (day.downloads / maxDownloads) * 100}
                    <div class="flex flex-col items-center flex-1 group relative">
                      <!-- Значение над столбиком -->
                      <span class="text-[10px] font-mono font-bold text-[#1A365D] opacity-0 group-hover:opacity-100 absolute -top-4 transition-opacity bg-white px-1.5 py-0.5 rounded border border-[#E2E8F0] shadow-sm z-10">
                        {day.downloads}
                      </span>
                      <!-- Столбик -->
                      <div
                        style="height: {barHeight}%;"
                        class="w-8 bg-[#3182CE] hover:bg-[#1A365D] rounded-t-[4px] transition-all cursor-pointer"
                        role="img"
                        aria-label="Загрузки за день {day.day}: {day.downloads}"
                      ></div>
                    </div>
                  {/each}
                {:else}
                  <div class="absolute inset-0 flex items-center justify-center text-xs text-[#515f74]">
                    Нет данных для отображения графика
                  </div>
                {/if}
              </div>

              <!-- Подписи оси X -->
              <div class="flex justify-between px-2 text-xs font-semibold text-[#1A365D]">
                {#each stats.daily || [] as day}
                  <div class="w-8 text-center">{day.day}</div>
                {/each}
              </div>
            </div>
          </div>

          <!-- Популярные нормативные акты -->
          <div class="flex flex-col gap-2">
            <h4 class="text-xs font-bold text-[#1A365D] uppercase tracking-wider">Популярные документы</h4>
            <div class="border border-[#E2E8F0] rounded overflow-hidden">
              <table class="w-full text-left border-collapse text-xs">
                <thead>
                  <tr class="bg-[#F8FAFC] border-b border-[#E2E8F0] text-[#1A365D] font-bold">
                    <th class="p-3">Название нормативного акта</th>
                    <th class="p-3 text-right">Скачиваний</th>
                  </tr>
                </thead>
                <tbody class="text-[#1A365D]">
                  {#if stats.popular && stats.popular.length > 0}
                    {#each stats.popular as doc}
                      <tr class="border-b border-[#E2E8F0] hover:bg-[#F8FAFC] transition-colors">
                        <td class="p-3 font-medium">{doc.title}</td>
                        <td class="p-3 text-right font-mono font-bold text-[#3182CE]">{doc.downloads}</td>
                      </tr>
                    {/each}
                  {:else}
                    <tr>
                      <td colspan="2" class="p-4 text-center text-[#515f74]">Статистика по документам отсутствует</td>
                    </tr>
                  {/if}
                </tbody>
              </table>
            </div>
          </div>

        </div>
      {/if}
    </section>

  </div>

</div>
