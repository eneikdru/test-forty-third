<script>
  import { onMount } from 'svelte';

  // State runes
  let selectedRole = $state('Economist'); // 'Economist' or 'Postgraduate'
  let activeFilter = $state('Все'); // 'Все', 'Бюджет', 'Нагрузка', 'Стипендии', 'Кадры'
  let searchQuery = $state('');
  let loading = $state(false);
  let errorMessage = $state('');

  // Loaded documents
  let allDocs = $state([]);

  // Fetch all documents from the endpoints depending on the current role
  async function fetchAllDocuments() {
    loading = true;
    errorMessage = '';
    allDocs = [];

    try {
      if (selectedRole === 'Economist') {
        // Fetch budget
        const bRes = await fetch('/api/financial/budget', {
          headers: { 'X-User-Role': 'Economist' }
        });
        let budgetList = [];
        if (bRes.ok) {
          budgetList = await bRes.json();
          budgetList.forEach(d => d.categoryName = 'Бюджет');
        }

        // Fetch load
        const lRes = await fetch('/api/financial/load', {
          headers: { 'X-User-Role': 'Economist' }
        });
        let loadList = [];
        if (lRes.ok) {
          loadList = await lRes.json();
          loadList.forEach(d => d.categoryName = 'Нагрузка');
        }

        // Fetch stipends
        const sRes = await fetch('/api/financial/stipends', {
          headers: { 'X-User-Role': 'Economist' }
        });
        let stipendsList = [];
        if (sRes.ok) {
          stipendsList = await sRes.json();
          stipendsList.forEach(d => d.categoryName = 'Стипендии');
        }

        // Combine
        allDocs = [...budgetList, ...loadList, ...stipendsList];
      } else {
        // Postgraduate role - stipends only
        const sRes = await fetch('/api/financial/stipends', {
          headers: { 'X-User-Role': 'Postgraduate' }
        });
        if (sRes.ok) {
          const stipendsList = await sRes.json();
          stipendsList.forEach(d => d.categoryName = 'Стипендии');
          allDocs = stipendsList;
        } else {
          errorMessage = 'Доступ ограничен: доступен только справочник стипендий.';
        }
      }
    } catch (err) {
      errorMessage = 'Ошибка подключения к серверу баз данных.';
    } finally {
      loading = false;
    }
  }

  // Reactive role transition effect
  $effect(() => {
    fetchAllDocuments();
  });

  onMount(() => {
    fetchAllDocuments();
  });

  // Helper to determine document card metadata deterministically
  function getDocCardMeta(doc) {
    const title = (doc.title || '').toLowerCase();
    const docType = (doc.documentType || '').toLowerCase();

    // PDF - red, Doc - blue, table - green
    if (title.includes('бюджет') || title.includes('расчет') || title.includes('оплат') || docType === 'project') {
      return {
        type: 'ТАБЛИЦА',
        color: '#2F855A', // green
        bgColor: '#F0FDF4',
        borderColor: '#DCFCE7',
        icon: 'table_chart',
        size: '1.4 МБ'
      };
    } else if (title.includes('положен') || title.includes('регламент') || docType === 'procedure') {
      return {
        type: 'ДОКУМЕНТ',
        color: '#2B6CB0', // blue
        bgColor: '#EBF8FF',
        borderColor: '#EBF8FF',
        icon: 'article',
        size: '420 КБ'
      };
    } else {
      return {
        type: 'ПДФ',
        color: '#C53030', // red
        bgColor: '#FFF5F5',
        borderColor: '#FED7D7',
        icon: 'picture_as_pdf',
        size: '2.1 МБ'
      };
    }
  }

  // Reactive search and category filtering
  let filteredDocs = $derived.by(() => {
    let list = allDocs;

    // Filter by category
    if (activeFilter !== 'Все') {
      list = list.filter(d => d.categoryName === activeFilter);
    }

    // Filter by search query
    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase().trim();

      // Synonym mappings for Russian educational / epidemiological abbreviations
      const synonyms = {
        'фгос': 'федеральный государственный образовательный стандарт',
        'гэк': 'государственная экзаменационная комиссия',
        'гиа': 'государственная итоговая аттестация',
        'фбун': 'федеральное бюджетное учреждение науки'
      };

      list = list.filter(d => {
        const titleMatch = d.title.toLowerCase().includes(q);
        const descMatch = (d.description || '').toLowerCase().includes(q);
        const numMatch = (d.documentNumber || '').toLowerCase().includes(q);

        // Check synonym expansions
        let synonymMatch = false;
        for (const [abbr, full] of Object.entries(synonyms)) {
          if (q.includes(abbr) || abbr.includes(q)) {
            if (d.title.toLowerCase().includes(full) || (d.description || '').toLowerCase().includes(full)) {
              synonymMatch = true;
            }
          }
        }

        return titleMatch || descMatch || numMatch || synonymMatch;
      });
    }

    return list;
  });
</script>

<!-- Clean styles scoped to Lexicon Flux design system -->
<style>
  :global(body) {
    background-color: #F9F9FF !important;
    font-family: 'Inter', sans-serif !important;
    color: #1A365D;
  }

  .lexicon-card {
    border-radius: 0.25rem !important; /* Strict 0.25rem rounded corners requirement */
    background-color: #FFFFFF;
    border: 1px solid #E2E8F0;
    transition: all 0.2s ease-in-out;
  }

  .lexicon-card:hover {
    box-shadow: 0 4px 12px rgba(26, 54, 93, 0.05); /* Thin ambient shadow for active states */
    border-color: #3182CE;
  }

  .line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
</style>

<div class="flex flex-col min-h-screen bg-[#F9F9FF] text-[#1A365D] selection:bg-[#3182CE] selection:text-white">

  <!-- Sticky top bar for search -->
  <header class="sticky top-0 z-50 bg-[#F9F9FF]/95 backdrop-blur border-b border-[#E2E8F0] px-6 py-4 flex flex-col gap-4">
    <div class="max-w-7xl w-full mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">

      <!-- App Name & Context -->
      <div class="flex items-center gap-3">
        <span class="material-symbols-outlined text-[#3182CE] text-3xl font-bold">menu_book</span>
        <div>
          <h1 class="text-xl font-bold tracking-tight text-[#1A365D]">Лексикон Флакс</h1>
          <p class="text-xs text-[#515f74] font-medium">База знаний · ЦНИИ Эпидемиологии</p>
        </div>
      </div>

      <!-- Search Input: 48px Height pinned with custom placeholder in Russian -->
      <div class="relative flex-1 max-w-xl w-full">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-[#3182CE]">search</span>
        <input
          type="text"
          bind:value={searchQuery}
          placeholder="Поиск по статьям, регламентам, стандартам..."
          class="w-full pl-11 pr-4 h-[48px] bg-white border border-[#E2E8F0] rounded-lg text-sm text-[#1A365D] placeholder-[#515f74]/70 focus:outline-none focus:border-[#3182CE] focus:ring-1 focus:ring-[#3182CE] hover:border-[#3182CE] transition-all duration-200"
        />
      </div>

      <!-- Role Selector (Strict Russian) -->
      <div class="flex items-center gap-2">
        <label for="role-select" class="text-xs font-semibold text-[#515f74]">Авторизация:</label>
        <select
          id="role-select"
          bind:value={selectedRole}
          class="bg-white border border-[#E2E8F0] rounded px-3 py-1.5 text-sm text-[#1A365D] font-semibold cursor-pointer focus:border-[#3182CE] focus:ring-0"
        >
          <option value="Economist">Экономист</option>
          <option value="Postgraduate">Студент / Аспирант</option>
        </select>
      </div>

    </div>
  </header>

  <!-- Navigation Pill Filters -->
  <section class="border-b border-[#E2E8F0] bg-white py-3 px-6">
    <div class="max-w-7xl w-full mx-auto flex items-center gap-2 overflow-x-auto pb-1 md:pb-0">
      <span class="text-xs font-bold text-[#515f74] uppercase tracking-wider mr-2">Разделы:</span>
      {#each ['Все', 'Бюджет', 'Нагрузка', 'Стипендии'] as filter}
        <button
          type="button"
          onclick={() => activeFilter = filter}
          class="px-4 py-1.5 rounded-full text-xs font-bold transition-all border {activeFilter === filter ? 'bg-[#3182CE] border-[#3182CE] text-white shadow-sm' : 'border-[#E2E8F0] text-[#515f74] hover:bg-[#F9F9FF]'}"
        >
          {filter}
        </button>
      {/each}
    </div>
  </section>

  <!-- Main Content Canvas -->
  <main class="flex-grow max-w-7xl w-full mx-auto px-6 py-8">

    <!-- Loading Indicators & Error Feedback -->
    {#if errorMessage}
      <div class="bg-red-50 text-[#C53030] p-4 rounded-lg border border-[#FED7D7] flex items-center gap-3 mb-6">
        <span class="material-symbols-outlined">error</span>
        <span class="font-semibold text-sm">{errorMessage}</span>
      </div>
    {/if}

    {#if loading}
      <div class="flex flex-col items-center justify-center py-16 gap-3 text-[#3182CE]">
        <span class="material-symbols-outlined animate-spin text-4xl">progress_activity</span>
        <span class="text-sm font-semibold tracking-wide">Получение актуальной нормативной базы...</span>
      </div>
    {:else}

      <!-- Grid Layout for Document Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {#each filteredDocs as doc}
          {@const meta = getDocCardMeta(doc)}
          <!-- Document Card: Uses #1A365D title, Inter font, 0.25rem rounded corners -->
          <div class="lexicon-card p-5 relative flex flex-col justify-between min-h-[180px] hover:scale-[1.01] duration-150">

            <!-- Type Icon on Top Right -->
            <div
              class="absolute top-4 right-4 w-8 h-8 rounded flex items-center justify-center"
              style="background-color: {meta.bgColor}; border: 1px solid {meta.borderColor};"
              title="Формат: {meta.type}"
            >
              <span class="material-symbols-outlined text-sm font-bold" style="color: {meta.color}">
                {meta.icon}
              </span>
            </div>

            <!-- Card Content -->
            <div class="pr-10">
              <span class="inline-block px-2 py-0.5 rounded text-[10px] font-bold text-white bg-[#3182CE] mb-2 uppercase tracking-wide">
                {doc.categoryName || 'ОБЩЕЕ'}
              </span>
              <h3 class="text-base font-bold leading-snug text-[#1A365D] mb-2">
                {doc.title}
              </h3>
              <p class="text-xs text-[#515f74] leading-relaxed line-clamp-2">
                {doc.description || 'Описание регламента отсутствует.'}
              </p>
            </div>

            <!-- Footer: Tech metadata in JetBrains Mono font small uppercase -->
            <div class="border-t border-[#E2E8F0] mt-4 pt-3 flex items-center justify-between text-[11px] font-mono uppercase tracking-wider text-[#515f74]">
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-[14px]">tag</span>
                <span>{doc.documentNumber || '123-P'}</span>
              </div>
              <div class="flex items-center gap-3">
                <span>ВЕРСИЯ {doc.version || '1.0'}</span>
                <span>{meta.size}</span>
              </div>
            </div>

          </div>
        {:else}
          <div class="col-span-full flex flex-col items-center justify-center py-16 gap-3 text-[#515f74]">
            <span class="material-symbols-outlined text-5xl">folder_off</span>
            <p class="text-sm font-semibold">Документы не найдены в выбранном разделе.</p>
          </div>
        {/each}
      </div>

    {/if}

  </main>

  <!-- Footer Info block (Strict Russian) -->
  <footer class="bg-white border-t border-[#E2E8F0] py-6 px-6 text-center text-xs text-[#515f74] font-medium">
    <div class="max-w-7xl w-full mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
      <span>© 2026 ЦНИИ Эпидемиологии РФ. Все права защищены.</span>
      <span>Система эпидемиологического и образовательного учета</span>
    </div>
  </footer>

</div>
