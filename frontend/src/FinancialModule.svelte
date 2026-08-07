<script>
  import { onMount } from 'svelte';

  // Svelte 5 state runes
  let selectedRole = $state('Economist'); // 'Economist', 'Postgraduate', 'Resident', etc.
  let activeCategory = $state('Финансы'); // 'Финансы', 'Кадры' или 'Стипендии'
  let activeSubTab = $state('Бюджет'); // 'Бюджет' или 'Нагрузка' (для категории Финансы)
  let budgetDocs = $state([]);
  let loadDocs = $state([]);
  let stipendDocs = $state([]);
  let loading = $state(false);
  let errorMessage = $state('');

  // Active session countdown timer for the security banner (translated from English mockup)
  let minutes = $state(14);
  let seconds = $state(59);

  // Translate process types returned by the backend to avoid any English words in the UI
  function getProcessName(process) {
    const mapping = {
      'admission': 'Приёмная комиссия',
      'certification': 'Аттестация (ГИА)',
      'stipends': 'Стипендиальное обеспечение',
      'practice': 'Практика',
      'result_tracking': 'Учет результатов',
      'other': 'Учебная часть'
    };
    return mapping[process] || 'Другое';
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
          const err = await bRes.json();
          errorMessage = err.message || 'Ошибка загрузки бюджетов';
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
      } else {
        // Fetch only stipends for students/postgraduates
        const sRes = await fetch('/api/financial/stipends', {
          headers: { 'X-User-Role': selectedRole }
        });
        if (sRes.ok) {
          stipendDocs = await sRes.json();
        } else {
          errorMessage = 'Доступ ограничен или произошла ошибка на сервере.';
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
    if (selectedRole === 'Economist') {
      activeCategory = 'Финансы';
      activeSubTab = 'Бюджет';
    } else {
      activeCategory = 'Стипендии';
    }
    fetchData();
  });

  onMount(() => {
    // Parse role parameter from URL if provided (e.g. ?role=Postgraduate)
    const params = new URLSearchParams(window.location.search);
    const roleParam = params.get('role');
    if (roleParam) {
      selectedRole = roleParam;
    } else {
      fetchData();
    }

    // Active session timer simulation
    const interval = setInterval(() => {
      if (seconds === 0) {
        if (minutes === 0) {
          clearInterval(interval);
          return;
        }
        minutes--;
        seconds = 59;
      } else {
        seconds--;
      }
    }, 1000);

    return () => clearInterval(interval);
  });
</script>

<style>
  /* Base settings to guarantee WCAG standards & no CLS shift */
  :global(body) {
    background-color: #f7f9fb;
    color: #191c1e;
    font-family: 'IBM Plex Sans', sans-serif;
    margin: 0;
    padding: 0;
    min-height: 100vh;
  }
</style>

<!-- Security Header Banner (strictly in Russian) -->
<div class="fixed top-0 left-0 w-full bg-[#ba1a1a] text-white py-1 px-4 z-[60] flex justify-between items-center text-xs font-mono font-bold tracking-wide" style="font-family: 'JetBrains Mono', monospace;">
  <span>СИСТЕМА ЭПИДЕМИОЛОГИЧЕСКОГО УЧЕТА РФ — ТРЕБУЕТСЯ ДОПУСК 4-ГО УРОВНЯ</span>
  <span>ВРЕМЯ СЕССИИ: <span>{minutes.toString().padStart(2, '0')}:{seconds.toString().padStart(2, '0')}</span></span>
</div>

<div class="min-h-screen flex flex-col md:flex-row bg-[#f7f9fb] text-[#191c1e] antialiased pt-[24px]">

  <!-- Боковая панель навигации (десктоп) -->
  <aside class="hidden md:flex flex-col w-[280px] bg-[#ffffff] border-r border-[#e0e3e5] h-[calc(100vh-24px)] sticky top-[24px] z-40 py-6 px-4">
    <div class="px-4 py-4 mb-6 border-b border-[#eceef0]">
      <h2 class="text-lg font-bold tracking-tight text-[#191c1e] flex items-center gap-2">
        <span class="material-symbols-outlined text-[#ba1a1a]">security</span>
        <span>ЦНИИ Эпидемиологии</span>
      </h2>
      <p class="text-[11px] text-[#45464d] mt-1 font-medium">Государственный учёт</p>
    </div>

    <nav class="flex-1 flex flex-col gap-2">
      {#if selectedRole === 'Economist'}
        <!-- Экономист: доступны все три раздела -->
        <button
          type="button"
          onclick={() => activeCategory = 'Финансы'}
          class="flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-all font-semibold focus:outline-none focus:ring-2 focus:ring-[#004b73] {activeCategory === 'Финансы' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#eceef0]'}"
        >
          <span class="material-symbols-outlined">payments</span>
          <span class="text-sm">Финансы и бюджет</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Кадры'}
          class="flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-all font-semibold focus:outline-none focus:ring-2 focus:ring-[#004b73] {activeCategory === 'Кадры' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#eceef0]'}"
        >
          <span class="material-symbols-outlined">badge</span>
          <span class="text-sm">Кадры и штат</span>
        </button>

        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-all font-semibold focus:outline-none focus:ring-2 focus:ring-[#004b73] {activeCategory === 'Стипендии' ? 'bg-[#d5e3fd] text-[#0d1c2f]' : 'text-[#45464d] hover:bg-[#eceef0]'}"
        >
          <span class="material-symbols-outlined">school</span>
          <span class="text-sm">Стипендии</span>
        </button>
      {:else}
        <!-- Обучающийся: доступен только раздел «Стипендии» -->
        <button
          type="button"
          onclick={() => activeCategory = 'Стипендии'}
          class="flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-all font-semibold focus:outline-none focus:ring-2 focus:ring-[#004b73] bg-[#d5e3fd] text-[#0d1c2f]"
        >
          <span class="material-symbols-outlined">school</span>
          <span class="text-sm">Стипендии</span>
        </button>
      {/if}
    </nav>

    <!-- Информация о правах доступа -->
    <div class="p-4 border-t border-[#eceef0] bg-[#f2f4f6] rounded-xl mt-auto">
      <div class="flex items-center gap-3">
        <span class="material-symbols-outlined text-2xl text-[#515f74]">verified_user</span>
        <div class="flex flex-col">
          <span class="text-xs font-bold text-[#191c1e]">Текущий допуск:</span>
          <span class="text-xs text-[#515f74] font-semibold mt-0.5">
            {#if selectedRole === 'Economist'}
              Экономист ОЦ
            {:else if selectedRole === 'Postgraduate'}
              Аспирант
            {:else if selectedRole === 'Resident'}
              Ординатор
            {:else}
              Обучающийся
            {/if}
          </span>
        </div>
      </div>
    </div>
  </aside>

  <!-- Основная область содержимого -->
  <main class="flex-1 flex flex-col min-w-0 pb-[80px] md:pb-0">

    <!-- Верхняя панель управления и авторизации -->
    <header class="w-full sticky top-[24px] z-30 bg-[#f7f9fb] border-b border-[#e0e3e5] flex flex-col sm:flex-row items-start sm:items-center justify-between px-6 py-4 gap-4">
      <div>
        <h1 class="text-xl font-bold text-[#191c1e] tracking-tight">
          {#if selectedRole === 'Economist'}
            Финансово-кадровый контур экономиста
          {:else}
            Кабинет обучающегося ЦНИИ
          {/if}
        </h1>
        <p class="text-xs text-[#45464d] mt-1 font-medium">Реестр нормативных актов и регламентов</p>
      </div>

      <!-- Селектор роли для проверки допусков (авторизации) -->
      <div class="flex items-center gap-2 bg-[#ffffff] px-3 py-1.5 border border-[#c6c6cd] rounded-lg shadow-sm">
        <label for="role-select" class="text-xs font-bold text-[#45464d]">Авторизация:</label>
        <select
          id="role-select"
          bind:value={selectedRole}
          class="bg-[#ffffff] border-0 text-xs text-[#0b1c30] font-bold cursor-pointer focus:ring-0 p-0"
        >
          <option value="Economist">Экономист</option>
          <option value="Postgraduate">Аспирант</option>
          <option value="Resident">Ординатор</option>
        </select>
      </div>
    </header>

    <!-- Тело контента -->
    <div class="p-6 flex flex-col gap-6 max-w-5xl w-full mx-auto">

      <!-- Системные ошибки -->
      {#if errorMessage}
        <div class="bg-[#ffdad6] text-[#93000a] p-4 rounded-xl border border-[#ba1a1a] flex items-center gap-3 shadow-sm" role="alert">
          <span class="material-symbols-outlined text-xl">error_med</span>
          <span class="font-bold text-sm">{errorMessage}</span>
        </div>
      {/if}

      {#if loading}
        <!-- Лоадер -->
        <div class="flex flex-col items-center justify-center py-20 gap-3 text-[#515f74]">
          <span class="material-symbols-outlined animate-spin text-4xl">sync</span>
          <span class="text-sm font-bold tracking-tight">Загрузка защищенного реестра данных...</span>
        </div>
      {:else}

        <!-- РАЗДЕЛ ЭКОНОМИСТА -->
        {#if selectedRole === 'Economist'}

          {#if activeCategory === 'Финансы'}
            <div class="flex flex-col gap-6">

              <!-- Подразделы (Финансовый макет) -->
              <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-[#ffffff] p-4 rounded-xl border border-[#e0e3e5] shadow-sm">
                <div class="flex gap-2">
                  <button
                    type="button"
                    onclick={() => activeSubTab = 'Бюджет'}
                    class="px-4 py-2 rounded-lg text-xs font-bold transition-all border focus:outline-none focus:ring-2 focus:ring-[#004b73] {activeSubTab === 'Бюджет' ? 'bg-[#d5e3fd] border-[#515f74] text-[#0d1c2f]' : 'border-[#c6c6cd] text-[#45464d] hover:bg-[#f2f4f6]'}"
                  >
                    Сметный бюджет ЦНИИ
                  </button>
                  <button
                    type="button"
                    onclick={() => activeSubTab = 'Нагрузка'}
                    class="px-4 py-2 rounded-lg text-xs font-bold transition-all border focus:outline-none focus:ring-2 focus:ring-[#004b73] {activeSubTab === 'Нагрузка' ? 'bg-[#d5e3fd] border-[#515f74] text-[#0d1c2f]' : 'border-[#c6c6cd] text-[#45464d] hover:bg-[#f2f4f6]'}"
                  >
                    Распределение нагрузки
                  </button>
                </div>
                <div class="flex items-center gap-2 text-xs font-bold text-[#45464d]">
                  <span class="material-symbols-outlined text-sm">calendar_today</span>
                  <span>Бюджетный цикл: 2026–2027</span>
                </div>
              </div>

              {#if activeSubTab === 'Бюджет'}
                <!-- Карточки КПЭ в стиле Bento из макета -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div class="bg-[#ffffff] p-5 rounded-xl border border-[#e0e3e5] flex flex-col gap-2 shadow-sm">
                    <span class="text-[10px] font-bold tracking-wider text-[#45464d] uppercase">Плановый бюджет</span>
                    <div class="flex items-baseline gap-2">
                      <span class="text-xl font-bold text-[#191c1e] font-mono">₽ 12 450 000.00</span>
                      <span class="text-[10px] font-bold text-[#10b981] bg-[#e6f4ea] px-2 py-0.5 rounded-full">+14.5%</span>
                    </div>
                    <span class="text-[11px] text-[#515f74]">Относительно предыдущего квартала</span>
                  </div>

                  <div class="bg-[#ffffff] p-5 rounded-xl border border-[#e0e3e5] flex flex-col gap-2 shadow-sm">
                    <span class="text-[10px] font-bold tracking-wider text-[#ba1a1a] uppercase">Расходная часть</span>
                    <div class="flex items-baseline gap-2">
                      <span class="text-xl font-bold text-[#ba1a1a] font-mono">₽ 8 120 400.00</span>
                      <span class="text-[10px] font-bold text-[#ba1a1a] bg-[#ffdad6] px-2 py-0.5 rounded-full">+2.1%</span>
                    </div>
                    <span class="text-[11px] text-[#ba1a1a]">В пределах установленных лимитов</span>
                  </div>

                  <div class="bg-[#ffffff] p-5 rounded-xl border border-[#e0e3e5] flex flex-col gap-2 border-l-4 border-l-[#004b73] shadow-sm">
                    <span class="text-[10px] font-bold tracking-wider text-[#004b73] uppercase">Свободный остаток</span>
                    <div class="flex items-baseline gap-2">
                      <span class="text-xl font-bold text-[#004b73] font-mono">₽ 4 329 600.00</span>
                      <span class="text-[10px] font-bold text-[#004b73] bg-[#cce5ff] px-2 py-0.5 rounded-full">Успешно</span>
                    </div>
                    <span class="text-[11px] text-[#515f74]">Операции верифицированы</span>
                  </div>
                </div>

                <!-- Реестр бюджетных документов -->
                <div class="bg-[#ffffff] rounded-xl border border-[#e0e3e5] overflow-hidden shadow-sm flex flex-col">
                  <div class="px-5 py-4 bg-[#f2f4f6] border-b border-[#e0e3e5] flex justify-between items-center">
                    <h3 class="font-bold text-sm text-[#191c1e]">Нормативные документы бюджета</h3>
                    <span class="text-[10px] font-bold text-[#0d1c2f] bg-[#d5e3fd] px-2.5 py-1 rounded">ДЕЙСТВУЕТ</span>
                  </div>

                  {#if budgetDocs.length === 0}
                    <p class="p-8 text-xs text-[#515f74] text-center font-semibold">В данном разделе документы не найдены.</p>
                  {:else}
                    <div class="overflow-x-auto">
                      <table class="w-full text-left border-collapse">
                        <thead>
                          <tr class="bg-[#f7f9fb] border-b border-[#e0e3e5] text-[11px] font-bold text-[#45464d] uppercase tracking-wider">
                            <th class="p-4">Название регламента</th>
                            <th class="p-4">Шифр</th>
                            <th class="p-4 text-right">Плановый объем</th>
                            <th class="p-4">Версия</th>
                            <th class="p-4">Состояние</th>
                          </tr>
                        </thead>
                        <tbody class="text-xs">
                          {#each budgetDocs as doc}
                            <tr class="border-b border-[#eceef0] hover:bg-[#f7f9fb] transition-colors">
                              <td class="p-4 font-bold text-[#191c1e]">{doc.title}</td>
                              <td class="p-4 text-xs font-mono text-[#515f74] font-semibold">{doc.documentNumber}</td>
                              <td class="p-4 text-right font-mono text-[#191c1e] font-bold">
                                {doc.budgetCycleMetadata ? '₽ ' + doc.budgetCycleMetadata.estimatedAmount.toLocaleString('ru-RU', { minimumFractionDigits: 2 }) : '—'}
                              </td>
                              <td class="p-4 text-xs font-bold text-[#515f74]">{doc.version}</td>
                              <td class="p-4">
                                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-[#dae2fd] text-[#131b2e]">УТВЕРЖДЕН</span>
                              </td>
                            </tr>
                          {/each}
                        </tbody>
                      </table>
                    </div>
                  {/if}
                </div>

              {:else if activeSubTab === 'Нагрузка'}
                <!-- Нормативы учебной нагрузки -->
                <div class="bg-[#ffffff] rounded-xl border border-[#e0e3e5] overflow-hidden shadow-sm flex flex-col">
                  <div class="px-5 py-4 bg-[#f2f4f6] border-b border-[#e0e3e5] flex justify-between items-center">
                    <h3 class="font-bold text-sm text-[#191c1e]">Нормативы и расчет учебной нагрузки</h3>
                    <span class="text-[10px] font-bold text-[#0d1c2f] bg-[#d5e3fd] px-2.5 py-1 rounded">МИН ОБРНАУКИ</span>
                  </div>

                  {#if loadDocs.length === 0}
                    <p class="p-8 text-xs text-[#515f74] text-center font-semibold">Документы расчета учебной нагрузки отсутствуют.</p>
                  {:else}
                    <div class="overflow-x-auto">
                      <table class="w-full text-left border-collapse">
                        <thead>
                          <tr class="bg-[#f7f9fb] border-b border-[#e0e3e5] text-[11px] font-bold text-[#45464d] uppercase tracking-wider">
                            <th class="p-4">Название документа</th>
                            <th class="p-4">Учебный год</th>
                            <th class="p-4">Процесс / Направление</th>
                            <th class="p-4">Редакция</th>
                          </tr>
                        </thead>
                        <tbody class="text-xs">
                          {#each loadDocs as doc}
                            <tr class="border-b border-[#eceef0] hover:bg-[#f7f9fb] transition-colors">
                              <td class="p-4 font-bold text-[#191c1e]">{doc.title}</td>
                              <td class="p-4 font-mono text-[#515f74] font-bold">{doc.academicYear}</td>
                              <td class="p-4 text-xs font-semibold text-[#004b73]">{getProcessName(doc.process)}</td>
                              <td class="p-4 text-xs font-bold text-[#515f74]">{doc.version}</td>
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
            <!-- Штатное расписание и оклады -->
            <div class="bg-[#ffffff] rounded-xl border border-[#e0e3e5] overflow-hidden shadow-sm flex flex-col">
              <div class="px-5 py-4 bg-[#f2f4f6] border-b border-[#e0e3e5] flex justify-between items-center">
                <h3 class="font-bold text-sm text-[#191c1e]">Кадровый контур и штатное расписание</h3>
                <span class="text-[10px] font-bold text-[#93000a] bg-[#ffdad6] px-2.5 py-1 rounded">СТРОГИЙ КОНТРОЛЬ</span>
              </div>

              <div class="p-6 border-b border-[#e0e3e5]">
                <h4 class="font-bold text-base text-[#191c1e] mb-2">Штатные единицы и оклады профессорско-преподавательского состава</h4>
                <p class="text-xs text-[#515f74] leading-relaxed">
                  Перечень служебных регламентирующих актов ЦНИИ Эпидемиологии, регулирующих ставки, оклады и штатные расписания.
                </p>
              </div>

              <div class="p-6">
                <div class="grid grid-cols-1 gap-4">
                  {#each budgetDocs.filter(d => d.title.toLowerCase().includes('штат') || d.title.toLowerCase().includes('оплат') || d.title.toLowerCase().includes('кадр')) as doc}
                    <div class="border border-[#e0e3e5] rounded-xl p-4 bg-[#f7f9fb] flex flex-col gap-2 hover:border-[#c6c6cd] transition-all">
                      <div class="flex justify-between items-start">
                        <span class="text-sm font-bold text-[#191c1e]">{doc.title}</span>
                        <span class="text-xs font-mono text-[#515f74] bg-[#eceef0] px-2 py-0.5 rounded font-semibold">{doc.documentNumber}</span>
                      </div>
                      <p class="text-xs text-[#515f74] leading-relaxed">{doc.description}</p>
                      <div class="flex items-center gap-4 text-xs mt-2 border-t border-[#eceef0] pt-2 text-[#45464d] font-semibold">
                        <span>Дата утверждения: {doc.approvalDate}</span>
                        <span>Редакция: {doc.version}</span>
                      </div>
                    </div>
                  {:else}
                    <p class="text-xs text-[#515f74] text-center py-4 font-bold">Служебные кадровые документы не загружены.</p>
                  {/each}
                </div>
              </div>
            </div>

          {:else if activeCategory === 'Стипендии'}
            <!-- Стипендиальный контур для экономиста -->
            <div class="bg-[#ffffff] rounded-xl border border-[#e0e3e5] overflow-hidden shadow-sm flex flex-col">
              <div class="px-5 py-4 bg-[#f2f4f6] border-b border-[#e0e3e5]">
                <h3 class="font-bold text-sm text-[#191c1e]">Регламенты стипендиального обеспечения ОЦ</h3>
              </div>

              <div class="p-6">
                {#if stipendDocs.length === 0}
                  <p class="text-xs text-[#515f74] text-center py-6 font-bold">Документы стипендий не найдены.</p>
                {:else}
                  <div class="grid grid-cols-1 gap-4">
                    {#each stipendDocs as doc}
                      <div class="border border-[#e0e3e5] rounded-xl p-4 bg-[#f7f9fb] flex flex-col gap-2 hover:border-[#c6c6cd] transition-all">
                        <span class="text-sm font-bold text-[#191c1e]">{doc.title}</span>
                        <p class="text-xs text-[#515f74] leading-relaxed">{doc.description}</p>
                        <div class="flex items-center justify-between text-xs border-t border-[#eceef0] pt-2 mt-2 text-[#45464d] font-bold">
                          <span>Уровень образования: {doc.program === 'postgraduate' ? 'Аспирантура' : (doc.program === 'resident' ? 'Ординатура' : 'Общий')}</span>
                          <span>Редакция {doc.version}</span>
                        </div>
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
            </div>
          {/if}

        {:else}
          <!-- РАЗДЕЛ СТУДЕНТА / АСПИРАНТА / ОРДИНАТОРА -->
          <div class="flex flex-col gap-6">
            <div class="bg-[#d5e3fd] border border-[#c6c6cd] p-5 rounded-xl shadow-sm">
              <h3 class="font-bold text-base text-[#0d1c2f] mb-1">Добро пожаловать в учебный кабинет!</h3>
              <p class="text-xs text-[#45464d] leading-relaxed font-semibold">
                Вам предоставлен изолированный доступ к нормативной базе стипендиального обеспечения ЦНИИ Эпидемиологии. Кадровые, штатные и операционные бюджетные разделы закрыты.
              </p>
            </div>

            <div class="bg-[#ffffff] rounded-xl border border-[#e0e3e5] overflow-hidden shadow-sm flex flex-col">
              <div class="px-5 py-4 bg-[#f2f4f6] border-b border-[#e0e3e5] flex justify-between items-center">
                <h3 class="font-bold text-sm text-[#191c1e]">Справочник стипендий обучающихся</h3>
                <span class="text-[10px] font-bold text-[#0d1c2f] bg-[#d5e3fd] px-2.5 py-1 rounded">АКТУАЛЬНО</span>
              </div>

              <div class="p-6">
                {#if stipendDocs.length === 0}
                  <p class="text-xs text-[#515f74] text-center py-6 font-semibold">Документы стипендиального фонда отсутствуют.</p>
                {:else}
                  <div class="flex flex-col gap-4">
                    {#each stipendDocs as doc}
                      <div class="border border-[#e0e3e5] rounded-xl p-5 hover:bg-[#eff4ff] hover:border-[#0d1c2f] transition-all flex flex-col gap-2">
                        <span class="text-sm font-bold text-[#191c1e]">{doc.title}</span>
                        <p class="text-xs text-[#515f74] leading-relaxed">{doc.description}</p>
                        <div class="flex flex-wrap items-center gap-4 text-[11px] border-t border-[#eceef0] pt-3 mt-2 text-[#515f74] font-semibold">
                          <span>Шифр документа: <span class="font-mono">{doc.documentNumber}</span></span>
                          <span>Утверждён: {doc.approvalDate}</span>
                          <span>Текущая версия: {doc.version}</span>
                        </div>
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
            </div>
          </div>
        {/if}

      {/if}

    </div>

  </main>

  <!-- Нижняя навигационная панель (мобильная адаптивная) -->
  <nav class="md:hidden fixed bottom-0 left-0 w-full z-50 bg-[#ffffff] border-t border-[#e0e3e5] flex justify-around items-center h-16 pb-safe shadow-[0_-1px_6px_rgba(0,0,0,0.05)]">
    {#if selectedRole === 'Economist'}
      <!-- Кнопки мобильного меню для Экономиста -->
      <button
        type="button"
        onclick={() => activeCategory = 'Финансы'}
        class="flex flex-col items-center justify-center text-[10px] font-bold focus:outline-none {activeCategory === 'Финансы' ? 'text-[#0d1c2f]' : 'text-[#45464d]'}"
      >
        <span class="material-symbols-outlined text-lg">payments</span>
        <span>Финансы</span>
      </button>

      <button
        type="button"
        onclick={() => activeCategory = 'Кадры'}
        class="flex flex-col items-center justify-center text-[10px] font-bold focus:outline-none {activeCategory === 'Кадры' ? 'text-[#0d1c2f]' : 'text-[#45464d]'}"
      >
        <span class="material-symbols-outlined text-lg">badge</span>
        <span>Кадры</span>
      </button>

      <button
        type="button"
        onclick={() => activeCategory = 'Стипендии'}
        class="flex flex-col items-center justify-center text-[10px] font-bold focus:outline-none {activeCategory === 'Стипендии' ? 'text-[#0d1c2f]' : 'text-[#45464d]'}"
      >
        <span class="material-symbols-outlined text-lg">school</span>
        <span>Стипендии</span>
      </button>
    {:else}
      <!-- Мобильная навигация для обучающегося -->
      <button
        type="button"
        onclick={() => activeCategory = 'Стипендии'}
        class="flex flex-col items-center justify-center text-[10px] font-bold text-[#0d1c2f] focus:outline-none"
      >
        <span class="material-symbols-outlined text-lg">school</span>
        <span>Стипендии</span>
      </button>
    {/if}
  </nav>

</div>
