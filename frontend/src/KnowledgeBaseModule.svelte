<script>
  import { onMount } from 'svelte';

  // Svelte 5 runes
  let selectedRole = $state('Economist'); // 'Economist', 'Postgraduate', 'Teacher', 'Administrator'
  let searchQuery = $state('');
  let activeFilter = $state('Все'); // 'Все', 'ФГОС', 'Аттестации', 'Практика', 'Шаблоны', 'Справочники', 'Архив'
  let documents = $state([]);
  let loading = $state(false);
  let errorMessage = $state('');

  // Built-in list of knowledge base documents to satisfy the 80% seeded documents requirements
  const seededKbDocuments = [
    {
      id: 'kb-1',
      title: 'ФГОС ВО по направлению подготовки «Эпидемиология»',
      description: 'Федеральный государственный образовательный стандарт высшего образования для ординатуры и аспирантуры по эпидемиологии.',
      documentType: 'PDF',
      academicYear: '2026–2027',
      category: 'ФГОС',
      size: '2.4 МБ',
      updatedAt: '01.07.2026',
      number: 'ФГОС-32.08.12',
      author: 'Минобрнауки РФ',
      isArchive: false
    },
    {
      id: 'kb-2',
      title: 'Регламент проведения государственной итоговой аттестации (ГИА)',
      description: 'Порядок организации и проведения государственной итоговой аттестации выпускников аспирантуры ЦНИИ Эпидемиологии.',
      documentType: 'DOC',
      academicYear: '2026–2027',
      category: 'Аттестации',
      size: '850 КБ',
      updatedAt: '15.06.2026',
      number: 'РЕГ-ГИА-2026',
      author: 'Учебный отдел',
      isArchive: false
    },
    {
      id: 'kb-3',
      title: 'Шаблон протокола Государственной экзаменационной комиссии (ГЭК)',
      description: 'Официальная форма протокола заседания ГЭК по приёму государственного экзамена и защите выпускной квалификационной работы.',
      documentType: 'ТАБЛИЦА',
      academicYear: '2026–2027',
      category: 'Шаблоны',
      size: '120 КБ',
      updatedAt: '20.05.2026',
      number: 'Ф-ГЭК-04',
      author: 'Департамент образования',
      isArchive: false
    },
    {
      id: 'kb-4',
      title: 'Положение о производственной практике ординаторов',
      description: 'Инструкции по прохождению клинической практики, формы отчётов, дневников и шаблоны отзывов руководителей.',
      documentType: 'PDF',
      academicYear: '2026–2027',
      category: 'Практика',
      size: '1.2 МБ',
      updatedAt: '10.05.2026',
      number: 'ПОЛ-ПР-02',
      author: 'Кафедра эпидемиологии',
      isArchive: false
    },
    {
      id: 'kb-5',
      title: 'Глоссарий терминов и список сокращений в эпидемиологии',
      description: 'Справочный материал, содержащий определения базовых терминов, классификаций и принятых в ЦНИИ сокращений.',
      documentType: 'DOC',
      academicYear: 'Бессрочно',
      category: 'Справочники',
      size: '420 КБ',
      updatedAt: '12.04.2026',
      number: 'СПР-ГЛ-01',
      author: 'Научно-методический совет',
      isArchive: false
    },
    {
      id: 'kb-6',
      title: 'Вопросы к кандидатскому экзамену по инфекционным болезням',
      description: 'Полный перечень теоретических вопросов и практических задач для сдачи кандидатского минимума.',
      documentType: 'PDF',
      academicYear: '2026–2027',
      category: 'Аттестации',
      size: '1.8 МБ',
      updatedAt: '28.05.2026',
      number: 'КАНД-ИНФ-2026',
      author: 'Учебный совет',
      isArchive: false
    },
    {
      id: 'kb-7',
      title: '[АРХИВ] ФГОС ВО по специальности «Эпидемиология» (Редакция 2021 года)',
      description: 'Устаревшая версия государственного стандарта, выведенная из эксплуатации. Использовать только для архивных сопоставлений.',
      documentType: 'PDF',
      academicYear: '2021–2022',
      category: 'Архив',
      size: '2.1 МБ',
      updatedAt: '01.09.2021',
      number: 'АРХ-ФГОС-2021',
      author: 'Минобрнауки РФ',
      isArchive: true
    }
  ];

  // Fetch real documents from API to blend in
  async function fetchApiDocuments() {
    loading = true;
    errorMessage = '';
    let fetchedDocs = [];

    try {
      // Build headers
      const headers = {
        'X-User-Role': selectedRole
      };

      // Depending on role, fetch stipend/budget/load
      const endpoints = [];
      if (selectedRole === 'Economist') {
        endpoints.push({ url: '/api/financial/budget', category: 'Справочники', tag: 'Бюджет' });
        endpoints.push({ url: '/api/financial/load', category: 'Практика', tag: 'Нагрузка' });
        endpoints.push({ url: '/api/financial/stipends', category: 'Справочники', tag: 'Стипендии' });
      } else if (selectedRole === 'Postgraduate' || selectedRole === 'Resident') {
        endpoints.push({ url: '/api/financial/stipends', category: 'Справочники', tag: 'Стипендии' });
      }

      for (const endpoint of endpoints) {
        try {
          const res = await fetch(endpoint.url, { headers });
          if (res.ok) {
            const data = await res.json();
            if (Array.isArray(data)) {
              data.forEach(d => {
                fetchedDocs.push({
                  id: d.id || `api-${d.documentNumber}`,
                  title: d.title,
                  description: d.description || 'Финансово-нормативный регламент ЦНИИ Эпидемиологии.',
                  documentType: d.documentType === 'Procedure' ? 'DOC' : d.documentType === 'Project' ? 'ТАБЛИЦА' : 'PDF',
                  academicYear: d.academicYear || '2026–2027',
                  category: endpoint.category,
                  size: '450 КБ',
                  updatedAt: '01.07.2026',
                  number: d.documentNumber || '123-P',
                  author: 'Финансовый отдел',
                  isArchive: false
                });
              });
            }
          }
        } catch (e) {
          console.error('Ошибка запроса к API:', e);
        }
      }
      documents = [...seededKbDocuments, ...fetchedDocs];
    } catch (err) {
      errorMessage = 'Сбой сети при получении дополнительных документов.';
      documents = [...seededKbDocuments];
    } finally {
      loading = false;
    }
  }

  // Refetch when selected role changes
  $effect(() => {
    fetchApiDocuments();
  });

  onMount(() => {
    fetchApiDocuments();
  });

  // Derived filtered documents using Svelte 5 reactive getters
  let filteredDocuments = $derived.by(() => {
    let list = documents;

    // Filter by active category tab (category/pill)
    if (activeFilter !== 'Все') {
      if (activeFilter === 'Архив') {
        list = list.filter(d => d.isArchive);
      } else {
        list = list.filter(d => d.category === activeFilter && !d.isArchive);
      }
    } else {
      // "Все" shows all non-archived by default
      list = list.filter(d => !d.isArchive);
    }

    // Filter by search query (case-insensitive)
    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase().trim();
      list = list.filter(d => {
        // Synonym expansion simulation for Russian terms
        let matchesSynonym = false;
        if (q.includes('фгос') && d.title.toLowerCase().includes('федеральный государственный образовательный стандарт')) matchesSynonym = true;
        if (q.includes('гиа') && d.title.toLowerCase().includes('государственная итоговая аттестация')) matchesSynonym = true;
        if (q.includes('гэк') && d.title.toLowerCase().includes('государственная экзаменационная комиссия')) matchesSynonym = true;

        return d.title.toLowerCase().includes(q) ||
               d.description.toLowerCase().includes(q) ||
               d.number.toLowerCase().includes(q) ||
               d.author.toLowerCase().includes(q) ||
               matchesSynonym;
      });
    }

    return list;
  });

  // Switch role and update UI state
  function changeRole(role) {
    selectedRole = role;
  }
</script>

<style>
  /* Inter font applied across the layout */
  .lexicon-container {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    background-color: #F8FAFC; /* Cold light background */
    color: #1A365D; /* Deep dark blue for text */
  }

  /* JetBrains Mono for technical metadata */
  .tech-meta {
    font-family: 'JetBrains Mono', monospace;
  }

  /* Custom shapes per Lexicon Flux rules: 0.25rem (4px for pills/badges, 8px for cards, 12px for modal) */
  .shape-badge {
    border-radius: 4px;
  }

  .shape-card-input {
    border-radius: 8px;
  }

  /* Custom subtle ambient shadow for hover */
  .ambient-shadow-hover {
    transition: box-shadow 0.15s ease-in-out, border-color 0.15s ease-in-out;
  }
  .ambient-shadow-hover:hover {
    border-color: #3182CE; /* Clear blue active border color */
    box-shadow: 0 4px 12px rgba(49, 130, 206, 0.08); /* One weak ambient-shadow for active state */
  }

  /* Line clamp custom classes to strictly respect "max 2 lines annotation" */
  .line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
</style>

<div class="lexicon-container min-h-screen pb-16 flex flex-col">
  <!-- Top Navigation Bar (strictly Russian) -->
  <header class="bg-white border-b border-[#E2E8F0] sticky top-0 z-50">
    <div class="max-w-[1200px] mx-auto px-5 py-4 flex flex-col md:flex-row justify-between items-center gap-4">
      <div class="flex items-center gap-3">
        <span class="material-symbols-outlined text-[#3182CE] text-3xl">menu_book</span>
        <div>
          <h1 class="text-xl font-bold tracking-tight text-[#1A365D]">БАЗА ЗНАНИЙ</h1>
          <p class="text-xs text-[#515F74]">ЦНИИ Эпидемиологии · Лексикон Флакс</p>
        </div>
      </div>

      <!-- User Role Selector and Module Toggle (strictly Russian) -->
      <div class="flex items-center gap-4 flex-wrap">
        <div class="flex items-center gap-2">
          <label for="kb-role-select" class="text-xs font-semibold text-[#515F74]">Роль доступа:</label>
          <select
            id="kb-role-select"
            bind:value={selectedRole}
            class="bg-white border border-[#E2E8F0] shape-card-input px-3 py-1.5 text-sm text-[#1A365D] font-medium cursor-pointer focus:border-[#3182CE] focus:ring-0"
          >
            <option value="Economist">Экономист</option>
            <option value="Postgraduate">Аспирант</option>
            <option value="Resident">Ординатор</option>
            <option value="Teacher">Преподаватель</option>
          </select>
        </div>
      </div>
    </div>
  </header>

  <!-- Large top search bar (height 48px, slightly tinted background) -->
  <section class="bg-[#F1F5F9] border-b border-[#E2E8F0] py-6 px-5 sticky top-[73px] z-40">
    <div class="max-w-[1200px] mx-auto">
      <div class="relative max-w-2xl mx-auto">
        <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
          <span class="material-symbols-outlined text-[#515F74]">search</span>
        </div>
        <input
          type="text"
          bind:value={searchQuery}
          placeholder="Поиск по ФГОС, ГИА, регламентам, шаблонам и кодам..."
          class="w-full h-12 pl-12 pr-10 bg-white border border-[#E2E8F0] shape-card-input text-base text-[#1A365D] placeholder-[#515F74]/60 focus:outline-none focus:border-[#3182CE] focus:ring-2 focus:ring-[#3182CE]/10 transition-all"
        />
        {#if searchQuery}
          <button
            type="button"
            onclick={() => searchQuery = ''}
            class="absolute inset-y-0 right-0 pr-4 flex items-center text-[#515F74] hover:text-[#1A365D]"
            title="Очистить запрос"
          >
            <span class="material-symbols-outlined text-sm">close</span>
          </button>
        {/if}
      </div>
    </div>
  </section>

  <!-- Main Content Layout (strictly mobile-first layout: 4 cols mobile, 12 cols desktop) -->
  <main class="max-w-[1200px] w-full mx-auto px-5 py-8 flex-1 flex flex-col gap-8">

    <!-- Pills Filtering Tabs -->
    <div class="flex flex-wrap gap-2 pb-2 border-b border-[#E2E8F0]">
      {#each ['Все', 'ФГОС', 'Аттестации', 'Практика', 'Шаблоны', 'Справочники', 'Архив'] as pill}
        <button
          type="button"
          onclick={() => activeFilter = pill}
          class="px-4 py-2 shape-badge text-xs font-semibold uppercase tracking-wider transition-all border
                 {activeFilter === pill
                  ? 'bg-[#1A365D] text-white border-[#1A365D]'
                  : 'bg-white text-[#515F74] border-[#E2E8F0] hover:bg-[#F8FAFC]'}"
        >
          {pill}
        </button>
      {/each}
    </div>

    <!-- Page Title / Results Count -->
    <div class="flex justify-between items-center">
      <div class="flex flex-col gap-1">
        <h2 class="text-lg font-bold text-[#1A365D] uppercase tracking-wide">Документы базы знаний</h2>
        <p class="text-xs text-[#515F74]">
          {filteredDocuments.length} документов найдено
        </p>
      </div>
    </div>

    <!-- Loading State -->
    {#if loading}
      <div class="flex flex-col items-center justify-center py-24 gap-3 text-[#515F74]">
        <span class="material-symbols-outlined animate-spin text-4xl text-[#3182CE]">autorenew</span>
        <span class="text-sm font-semibold">Загрузка документов базы знаний...</span>
      </div>
    {:else if filteredDocuments.length === 0}
      <!-- Empty State -->
      <div class="bg-white border border-[#E2E8F0] shape-card-input p-12 text-center flex flex-col items-center gap-4 max-w-md mx-auto my-8">
        <span class="material-symbols-outlined text-[#515F74] text-5xl">folder_off</span>
        <h3 class="text-base font-bold text-[#1A365D]">Документы не найдены</h3>
        <p class="text-xs text-[#515F74] leading-relaxed">
          По запросу "{searchQuery}" ничего не найдено. Проверьте правильность написания или сбросьте фильтры.
        </p>
        <button
          type="button"
          onclick={() => { searchQuery = ''; activeFilter = 'Все'; }}
          class="px-4 py-2 bg-[#3182CE] text-white text-xs font-bold shape-badge uppercase tracking-wider hover:bg-[#2B6CB0] transition-colors"
        >
          Сбросить фильтры
        </button>
      </div>
    {:else}
      <!-- Documents Grid: 4 columns mobile, 12 columns desktop.
           We map grid-cols-1 md:grid-cols-3 (which aligns with a 12-column grid system where each card spans 4 columns) -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {#each filteredDocuments as doc}
          <!-- Document Card component per rules: white background, thin border, icon on top-right, title, annotation max 2 lines, size/date at bottom -->
          <article class="bg-white border border-[#E2E8F0] shape-card-input p-5 flex flex-col gap-4 relative ambient-shadow-hover h-full">

            <!-- Top Tag & Type Icon Badge (PDF - Red, DOC - Blue, Table - Green) -->
            <div class="flex justify-between items-start gap-4">
              <span class="tech-meta text-[10px] font-bold text-[#3182CE] tracking-wider uppercase bg-[#3182CE]/10 px-2 py-0.5 shape-badge">
                {doc.number}
              </span>

              <!-- Document type icon/badge with strict Russian translation -->
              {#if doc.documentType === 'PDF'}
                <span class="tech-meta text-[10px] font-bold text-white tracking-widest bg-[#E53E3E] px-2 py-0.5 shape-badge" title="Документ ПДФ">
                  ПДФ
                </span>
              {:else if doc.documentType === 'DOC'}
                <span class="tech-meta text-[10px] font-bold text-white tracking-widest bg-[#3182CE] px-2 py-0.5 shape-badge" title="Документ Ворд">
                  ДОК
                </span>
              {:else}
                <span class="tech-meta text-[10px] font-bold text-white tracking-widest bg-[#38A169] px-2 py-0.5 shape-badge" title="Документ Таблица">
                  ТАБ
                </span>
              {/if}
            </div>

            <!-- Title -->
            <h3 class="text-sm font-bold text-[#1A365D] leading-snug">
              {doc.title}
            </h3>

            <!-- Annotation (maximum 2 lines) -->
            <p class="text-xs text-[#515F74] leading-relaxed line-clamp-2">
              {doc.description}
            </p>

            <!-- Bottom technical metadata: JetBrains Mono, small cap dates/size -->
            <div class="mt-auto pt-3 border-t border-[#F1F5F9] flex justify-between items-center text-[10px] font-bold text-[#515F74] uppercase tracking-wider tech-meta">
              <span>{doc.size}</span>
              <span>{doc.updatedAt}</span>
            </div>
          </article>
        {/each}
      </div>
    {/if}
  </main>
</div>
