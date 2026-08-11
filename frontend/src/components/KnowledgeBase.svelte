<script>
  import { onMount, onDestroy } from 'svelte';
  import DocumentViewer from './DocumentViewer.svelte';
  import { getTypoCorrection, getSuggestions } from '../utils/searchUtils.js';

  // Props
  let { selectedRole = 'Economist' } = $props();

  // Svelte 5 state runes
  let selectedDocument = $state(null);
  let searchQuery = $state('');
  let debouncedSearchQuery = $state('');
  let currentPage = $state(1);
  let pageSize = $state(6);

  function selectDocument(doc) {
    selectedDocument = doc;
  }
  let selectedProgram = $state('all'); // 'all', 'postgraduate', 'residency', 'both'
  let selectedDocType = $state('all'); // 'all', 'Position', 'Procedure', 'Project', 'Other'
  let selectedProcess = $state('all'); // 'all', 'admission', 'certification', 'stipends', 'practice', 'result_tracking', 'other'
  let selectedEduLevel = $state('all'); // 'all', 'higher', 'postgraduate_qualification'
  let selectedDateFilter = $state('all'); // 'all', '7days', '30days', 'year'

  // Favorites & Saved Searches
  let favorites = $state([]);
  let savedSearches = $state([]);
  let activeSuggestionIndex = $state(-1);
  let showSuggestions = $state(false);

  let documents = $state([]);
  let loading = $state(false);
  let errorMessage = $state('');

  // Local/Fallback documents representing the required ЦНИИ documents
  const localDocuments = [
    {
      id: 'local-1',
      title: 'ФГОС ВО по специальности 32.08.12 Эпидемиология',
      description: 'Федеральный государственный образовательный стандарт высшего образования по специальности Эпидемиология.',
      documentType: 'Position',
      academicYear: 'бессрочно',
      program: 'residency',
      process: 'certification',
      status: 'ACTIVE',
      approvalDate: '2024-06-15',
      documentNumber: 'ФГОС-32.08.12',
      version: '1.0',
      schemaTags: ['Load', 'Book'],
      fileType: 'PDF',
      educationLevel: 'postgraduate_qualification'
    },
    {
      id: 'local-2',
      title: 'Регламент проведения ГИА и кандидатских экзаменов ЦНИИ',
      description: 'Инструкции и правила проведения государственной итоговой аттестации и кандидатских экзаменов по профильным дисциплинам.',
      documentType: 'Procedure',
      academicYear: '2026-2027',
      program: 'postgraduate',
      process: 'certification',
      status: 'ACTIVE',
      approvalDate: '2026-05-10',
      documentNumber: 'РЕГ-ГИА-2026',
      version: '2.0',
      schemaTags: ['Book', 'Glossary'],
      fileType: 'Doc',
      educationLevel: 'postgraduate_qualification'
    },
    {
      id: 'local-3',
      title: 'Шаблоны протоколов ГЭК и отчётов по практике',
      description: 'Утверждённые образцы протоколов государственной экзаменационной комиссии, характеристик и отчётов по прохождению учебной и производственной практики.',
      documentType: 'Project',
      academicYear: '2026-2027',
      program: 'both',
      process: 'practice',
      status: 'ACTIVE',
      approvalDate: '2026-04-18',
      documentNumber: 'ШАБ-ГЭК-ПРАК',
      version: '1.0',
      schemaTags: ['Load'],
      fileType: 'Table',
      educationLevel: 'higher'
    },
    {
      id: 'local-4',
      title: 'Вопросы к кандидатским экзаменам по профильным дисциплинам',
      description: 'Полный перечень вопросов к кандидатским экзаменам и ГИА для аспирантов по эпидемиологии и инфекционным болезням.',
      documentType: 'Other',
      academicYear: 'бессрочно',
      program: 'postgraduate',
      process: 'certification',
      status: 'ACTIVE',
      approvalDate: '2025-09-01',
      documentNumber: 'ВОП-КАНД-2025',
      version: '1.0',
      schemaTags: ['Book'],
      fileType: 'PDF',
      educationLevel: 'postgraduate_qualification'
    },
    {
      id: 'local-5',
      title: 'Положение о практике, академическом отпуске и ВСОКО',
      description: 'Регламент прохождения практики, предоставления академического отпуска, поощрения обучающихся и функционирования внутренней системы оценки качества образования (ВСОКО).',
      documentType: 'Position',
      academicYear: 'бессрочно',
      program: 'both',
      process: 'practice',
      status: 'ACTIVE',
      approvalDate: '2026-01-20',
      documentNumber: 'ПОЛ-ВСОКО-01',
      version: '1.2',
      schemaTags: ['Load', 'Glossary'],
      fileType: 'Doc',
      educationLevel: 'higher'
    },
    {
      id: 'local-6',
      title: 'ФГОС ВО по специальности 31.08.35 Инфекционные болезни',
      description: 'Федеральный государственный образовательный стандарт ординатуры по направлению Инфекционные болезни.',
      documentType: 'Position',
      academicYear: 'бессрочно',
      program: 'residency',
      process: 'certification',
      status: 'ACTIVE',
      approvalDate: '2024-08-22',
      documentNumber: 'ФГОС-31.08.35',
      version: '1.0',
      schemaTags: ['Book'],
      fileType: 'PDF',
      educationLevel: 'postgraduate_qualification'
    },
    {
      id: 'local-7',
      title: 'Шаблоны заявлений на академический отпуск и портфолио',
      description: 'Архив документов и бланков заявлений для оформления отпуска, портфолио достижений и свидетельств.',
      documentType: 'Project',
      academicYear: 'проект',
      program: 'both',
      process: 'other',
      status: 'PROJECT',
      approvalDate: '2026-07-02',
      documentNumber: 'ШАБ-ЗАЯВ-ПОРТ',
      version: '0.9',
      schemaTags: ['Glossary'],
      fileType: 'Doc',
      educationLevel: 'higher'
    },
    {
      id: 'local-8',
      title: 'Глоссарий терминов эпидемиологического учёта',
      description: 'Официальный терминологический справочник и список сокращений, используемых в системе эпидемиологического надзора РФ.',
      documentType: 'Other',
      academicYear: 'бессрочно',
      program: 'both',
      process: 'other',
      status: 'ACTIVE',
      approvalDate: '2025-11-15',
      documentNumber: 'СПР-ГЛОС-2025',
      version: '1.5',
      schemaTags: ['Glossary'],
      fileType: 'Table',
      educationLevel: 'higher'
    }
  ];

  // Translation helpers to ensure 100% Russian UI
  function getDocTypeRu(type) {
    const map = {
      Position: 'Положение',
      Procedure: 'Порядок',
      Project: 'Проект',
      Other: 'Служебный акт'
    };
    return map[type] || 'Документ';
  }

  function getProgramRu(prog) {
    const map = {
      postgraduate: 'Аспирантура',
      residency: 'Ординатура',
      both: 'Все программы'
    };
    return map[prog] || 'Общий';
  }

  function getProcessRu(proc) {
    const map = {
      admission: 'Приём',
      certification: 'Аттестация',
      stipends: 'Стипендии',
      practice: 'Практика',
      result_tracking: 'Учёт результатов',
      other: 'Иное'
    };
    return map[proc] || 'Иной процесс';
  }

  function getStatusRu(status) {
    const map = {
      ACTIVE: 'АКТУАЛЬНО',
      PROJECT: 'ПРОЕКТ',
      ARCHIVED: 'АРХИВ'
    };
    return map[status] || 'АКТУАЛЬНО';
  }

  function getFileTypeIcon(fileType, title) {
    const lowerTitle = title.toLowerCase();
    if (fileType === 'PDF' || lowerTitle.includes('фгос') || lowerTitle.includes('регламент')) {
      return { icon: 'picture_as_pdf', color: 'text-[#E53E3E]', bg: 'bg-[#FFF5F5]', border: 'border-[#FED7D7]', label: 'ПДФ' };
    }
    if (fileType === 'Table' || lowerTitle.includes('таблиц') || lowerTitle.includes('протокол') || lowerTitle.includes('оплат') || lowerTitle.includes('бюджет')) {
      return { icon: 'table_chart', color: 'text-[#38A169]', bg: 'bg-[#F0FFF4]', border: 'border-[#C6F6D5]', label: 'Таблица' };
    }
    return { icon: 'article', color: 'text-[#3182CE]', bg: 'bg-[#EBF8FF]', border: 'border-[#BEE3F8]', label: 'Документ' };
  }

  // Combined unfiltered list
  let combinedUnfiltered = $derived.by(() => {
    let combined = [...documents];
    const combinedIds = new Set(combined.map(d => d.id));

    for (const localDoc of localDocuments) {
      if (!combinedIds.has(localDoc.id)) {
        combined.push(localDoc);
      }
    }
    return combined;
  });

  // Filter logic helper for dates relative to "2026-09-20"
  function matchesDateFilter(approvalDateStr, filter) {
    if (filter === 'all') return true;
    if (!approvalDateStr) return false;
    const approvalDate = new Date(approvalDateStr);
    const refDate = new Date('2026-09-20'); // stable anchor date
    const diffTime = Math.abs(refDate - approvalDate);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (filter === '7days') {
      return diffDays <= 7;
    }
    if (filter === '30days') {
      return diffDays <= 30;
    }
    if (filter === 'year') {
      return approvalDate.getFullYear() === 2026;
    }
    return true;
  }

  // Reactive derived filtered list
  let filteredDocuments = $derived.by(() => {
    return combinedUnfiltered.filter(doc => {
      // 1. Search Query Filter
      if (searchQuery.trim() !== '') {
        const q = searchQuery.toLowerCase();
        const matchesTitle = doc.title && doc.title.toLowerCase().includes(q);
        const matchesDesc = doc.description && doc.description.toLowerCase().includes(q);
        const matchesNum = doc.documentNumber && doc.documentNumber.toLowerCase().includes(q);
        if (!matchesTitle && !matchesDesc && !matchesNum) return false;
      }

      // 2. Program Filter
      if (selectedProgram !== 'all') {
        if (doc.program !== selectedProgram && doc.program !== 'both') return false;
      }

      // 3. Document Type Filter
      if (selectedDocType !== 'all') {
        if (doc.documentType !== selectedDocType) return false;
      }

      // 4. Process Filter
      if (selectedProcess !== 'all') {
        if (doc.process !== selectedProcess) return false;
      }

      // 5. Education Level Filter
      if (selectedEduLevel !== 'all') {
        const eduLevel = doc.educationLevel || 'higher';
        if (eduLevel !== selectedEduLevel) return false;
      }

      // 6. Date Filter
      if (selectedDateFilter !== 'all') {
        if (!matchesDateFilter(doc.approvalDate, selectedDateFilter)) return false;
      }

      return true;
    });
  });

  // Filtered favorite documents
  let favoriteDocuments = $derived.by(() => {
    return combinedUnfiltered.filter(doc => favorites.includes(doc.id));
  });

  // Derived pagination variables
  let totalPages = $derived.by(() => {
    return Math.max(1, Math.ceil(filteredDocuments.length / pageSize));
  });

  let paginatedDocuments = $derived.by(() => {
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return filteredDocuments.slice(startIndex, endIndex);
  });

  // Fuzzy search and Typo Correction calculation
  let typoCorrection = $derived.by(() => {
    return getTypoCorrection(searchQuery, combinedUnfiltered);
  });

  // Live Auto-suggestions as user types
  let suggestionsList = $derived.by(() => {
    return getSuggestions(searchQuery, combinedUnfiltered);
  });

  async function fetchBackendDocuments(queryVal, roleVal) {
    loading = true;
    errorMessage = '';
    try {
      const res = await fetch(`/api/documents/search?q=${encodeURIComponent(queryVal)}`, {
        headers: {
          'X-User-Role': roleVal
        }
      });
      if (res.ok) {
        const data = await res.json();
        documents = data.map(item => ({
          id: item.document.id,
          title: item.document.title,
          description: item.document.description,
          documentType: item.document.documentType,
          academicYear: item.document.academicYear,
          program: item.document.program,
          process: item.document.process,
          status: item.document.status,
          approvalDate: item.document.approvalDate,
          documentNumber: item.document.documentNumber,
          version: item.document.version,
          schemaTags: item.document.schemaTags || [],
          educationLevel: item.document.academicYear && item.document.academicYear.includes('2026') ? 'postgraduate_qualification' : 'higher'
        }));
      } else {
        console.warn('Backend documents loaded from local storage due to API status');
      }
    } catch (err) {
      console.error('Ошибка сети при загрузке документов:', err);
    } finally {
      loading = false;
    }
  }

  // Toggle Favorites
  function toggleFavorite(id, event) {
    if (event) event.stopPropagation();
    if (favorites.includes(id)) {
      favorites = favorites.filter(favId => favId !== id);
    } else {
      favorites = [...favorites, id];
    }
    try {
      localStorage.setItem('kb_favorites_v1', JSON.stringify(favorites));
    } catch (e) {
      console.error(e);
    }
  }

  // Save Searches
  function saveCurrentSearch() {
    const q = searchQuery.trim();
    if (q && !savedSearches.includes(q)) {
      savedSearches = [q, ...savedSearches].slice(0, 8); // limit to 8
      try {
        localStorage.setItem('kb_saved_searches_v1', JSON.stringify(savedSearches));
      } catch (e) {
        console.error(e);
      }
    }
  }

  function deleteSavedSearch(q, event) {
    if (event) event.stopPropagation();
    savedSearches = savedSearches.filter(item => item !== q);
    try {
      localStorage.setItem('kb_saved_searches_v1', JSON.stringify(savedSearches));
    } catch (e) {
      console.error(e);
    }
  }

  function selectSavedSearch(q) {
    searchQuery = q;
    showSuggestions = false;
  }

  // Handle keys for suggestions
  function handleKeyDown(event) {
    if (suggestionsList.length > 0) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        activeSuggestionIndex = (activeSuggestionIndex + 1) % suggestionsList.length;
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        activeSuggestionIndex = (activeSuggestionIndex - 1 + suggestionsList.length) % suggestionsList.length;
      } else if (event.key === 'Enter') {
        if (activeSuggestionIndex >= 0 && activeSuggestionIndex < suggestionsList.length) {
          event.preventDefault();
          searchQuery = suggestionsList[activeSuggestionIndex];
          showSuggestions = false;
          activeSuggestionIndex = -1;
        } else {
          saveCurrentSearch();
        }
      } else if (event.key === 'Escape') {
        showSuggestions = false;
        activeSuggestionIndex = -1;
      }
    } else {
      if (event.key === 'Enter') {
        saveCurrentSearch();
      }
    }
  }

  // Debouncing searchQuery to debouncedSearchQuery
  let debounceTimer;
  $effect(() => {
    // Depend on searchQuery
    const currentQuery = searchQuery;
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      debouncedSearchQuery = currentQuery;
    }, 300);

    return () => {
      if (debounceTimer) clearTimeout(debounceTimer);
    };
  });

  // Fetch backend documents when debouncedSearchQuery or selectedRole changes
  $effect(() => {
    const query = debouncedSearchQuery;
    const role = selectedRole;
    fetchBackendDocuments(query, role);
  });

  // Reset page when search or filters change
  $effect(() => {
    // Read the filter state dependencies
    const _q = searchQuery;
    const _p = selectedProgram;
    const _t = selectedDocType;
    const _pr = selectedProcess;
    const _e = selectedEduLevel;
    const _d = selectedDateFilter;

    currentPage = 1;
  });

  function handleMaterialsSynced() {
    fetchBackendDocuments(debouncedSearchQuery, selectedRole);
  }

  onMount(() => {
    try {
      const storedFavs = localStorage.getItem('kb_favorites_v1');
      if (storedFavs) {
        favorites = JSON.parse(storedFavs);
      }
    } catch (e) {
      console.error(e);
    }

    try {
      const storedSearches = localStorage.getItem('kb_saved_searches_v1');
      if (storedSearches) {
        savedSearches = JSON.parse(storedSearches);
      } else {
        // Preseed some searches
        savedSearches = ['ФГОС Эпидемиология', 'Кандидатские экзамены', 'Положение о практике'];
      }
    } catch (e) {
      console.error(e);
    }

    window.addEventListener('materials-synced', handleMaterialsSynced);
  });

  onDestroy(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('materials-synced', handleMaterialsSynced);
    }
  });
</script>

{#if selectedDocument}
  <DocumentViewer {selectedDocument} {selectedRole} onBack={() => selectedDocument = null} />
{:else}
  <div class="flex flex-col gap-6 w-full max-w-[1200px] mx-auto px-5 md:px-0 bg-[#F9F9FF] min-h-screen text-[#0b1c30]">

  <!-- Заголовок базы знаний -->
  <div class="flex flex-col gap-1.5 mt-4">
    <h2 class="text-2xl font-bold text-[#1A365D] tracking-tight font-sans">База знаний центра</h2>
    <p class="text-sm text-[#515f74] font-sans">
      Официальный реестр нормативно-правовых и методических материалов ЦНИИ Эпидемиологии. Только актуальные версии документов.
    </p>
  </div>

  <!-- Крупная строка поиска с кнопкой сохранения и автоподсказками -->
  <div class="relative w-full">
    <div class="sticky top-0 z-10 bg-[#F8FAFC]/95 backdrop-blur border border-[#E2E8F0] p-1.5 rounded-[8px] h-[48px] flex items-center shadow-sm w-full transition-all duration-200">
      <span class="material-symbols-outlined text-[#1A365D] px-3">search</span>
      <input
        type="text"
        bind:value={searchQuery}
        onfocus={() => showSuggestions = true}
        onblur={() => setTimeout(() => showSuggestions = false, 200)}
        onkeydown={handleKeyDown}
        placeholder="Поиск по названию, аннотации или шифру документа..."
        class="flex-1 bg-transparent border-0 ring-0 focus:ring-0 focus:outline-none text-sm text-[#0b1c30] placeholder-slate-400 font-sans h-full"
      />
      {#if searchQuery}
        <button
          type="button"
          onclick={() => { searchQuery = ''; activeSuggestionIndex = -1; }}
          class="text-slate-400 hover:text-[#3182CE] p-1 flex items-center justify-center mr-1"
          aria-label="Очистить поиск"
        >
          <span class="material-symbols-outlined text-lg">close</span>
        </button>
        <button
          type="button"
          onclick={saveCurrentSearch}
          class="bg-[#3182CE] text-white hover:bg-[#2b72b5] px-3 py-1 rounded-[6px] text-xs font-semibold mr-1 transition-colors"
          title="Сохранить поисковый запрос"
        >
          Сохранить запрос
        </button>
      {/if}
    </div>

    <!-- Список автоподсказок -->
    {#if showSuggestions && suggestionsList.length > 0}
      <div class="absolute left-0 right-0 top-[52px] bg-white border border-[#E2E8F0] rounded-[8px] shadow-lg z-50 overflow-hidden max-h-60 overflow-y-auto">
        {#each suggestionsList as suggestion, idx}
          <button
            type="button"
            onclick={() => { searchQuery = suggestion; showSuggestions = false; }}
            class="w-full text-left px-4 py-2.5 text-sm hover:bg-slate-50 transition-colors flex items-center gap-2 font-sans {idx === activeSuggestionIndex ? 'bg-slate-100' : ''}"
          >
            <span class="material-symbols-outlined text-slate-400 text-sm">history</span>
            <span class="text-[#0b1c30] truncate">{suggestion}</span>
          </button>
        {/each}
      </div>
    {/if}
  </div>

  <!-- Исправление опечаток (Баннер) -->
  {#if typoCorrection}
    <div class="bg-amber-50 border border-amber-200 rounded-[8px] p-3 text-sm text-amber-800 flex items-center gap-2 font-sans">
      <span class="material-symbols-outlined text-amber-600">info</span>
      <span>Возможно, вы имели в виду:</span>
      <button
        type="button"
        onclick={() => searchQuery = typoCorrection}
        class="font-bold underline text-[#3182CE] hover:text-[#2b72b5] text-left"
      >
        {typoCorrection}
      </button>
    </div>
  {/if}

  <!-- Панель сохраненных запросов -->
  {#if savedSearches.length > 0}
    <section class="space-y-2 bg-white p-4 border border-[#E2E8F0] rounded-[8px] shadow-sm">
      <h3 class="text-xs font-bold text-slate-400 uppercase tracking-wider font-sans">Сохраненные поисковые запросы</h3>
      <div class="flex flex-wrap gap-2">
        {#each savedSearches as q}
          <span
            role="button"
            tabindex="0"
            onclick={() => selectSavedSearch(q)}
            onkeydown={(e) => e.key === 'Enter' && selectSavedSearch(q)}
            class="inline-flex items-center gap-1.5 px-3 py-1 bg-[#EBF8FF] text-[#2B6CB0] rounded-full text-xs font-medium cursor-pointer hover:bg-[#E2E8F0] transition-colors"
          >
            <span class="material-symbols-outlined text-xs">history</span>
            <span>{q}</span>
            <button
              type="button"
              onclick={(e) => deleteSavedSearch(q, e)}
              class="text-[#2B6CB0] hover:text-red-500 rounded-full flex items-center justify-center p-0.5"
              aria-label="Удалить сохраненный запрос"
            >
              <span class="material-symbols-outlined text-[12px]">close</span>
            </button>
          </span>
        {/each}
      </div>
    </section>
  {/if}

  <!-- Карусель избранных материалов -->
  {#if favoriteDocuments.length > 0}
    <section class="space-y-3">
      <h2 class="text-lg font-bold text-[#1A365D] font-sans">Избранные материалы</h2>
      <div class="flex overflow-x-auto no-scrollbar space-x-4 pb-4 snap-x">
        {#each favoriteDocuments as doc}
          {@const fileMeta = getFileTypeIcon(doc.fileType, doc.title)}
          <div
            role="button"
            tabindex="0"
            onclick={() => selectDocument(doc)}
            onkeydown={(e) => e.key === 'Enter' && selectDocument(doc)}
            class="snap-start min-w-[200px] w-[200px] bg-white border border-[#E2E8F0] rounded-lg p-3 shrink-0 flex flex-col gap-2 relative group cursor-pointer hover:border-[#3182CE] transition-colors shadow-sm"
          >
            <!-- Иконка и звезда в Избранном -->
            <div class="flex items-center justify-between">
              <span class="material-symbols-outlined text-2xl {fileMeta.color}">{fileMeta.icon}</span>
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  onclick={(e) => {
                    e.stopPropagation();
                    window.dispatchEvent(new CustomEvent('edit-document', { detail: doc }));
                  }}
                  class="text-slate-400 hover:text-[#3182CE] p-1 flex items-center justify-center rounded-full active:scale-90 transition-transform"
                  title="Редактировать"
                  aria-label="Редактировать"
                >
                  <span class="material-symbols-outlined text-[18px]">edit</span>
                </button>
                <button
                  type="button"
                  onclick={(e) => toggleFavorite(doc.id, e)}
                  class="text-amber-500 hover:text-slate-400 p-1 flex items-center justify-center rounded-full active:scale-90 transition-transform"
                  aria-label="Убрать из избранного"
                >
                  <span class="material-symbols-outlined text-[18px]" style="font-variation-settings: 'FILL' 1;">star</span>
                </button>
              </div>
            </div>
            <!-- Название и мета-информация -->
            <div class="flex flex-col gap-1 mt-1">
              <span class="text-[9px] font-bold text-[#3182CE] uppercase tracking-wider font-mono">
                {getDocTypeRu(doc.documentType)}
              </span>
              <span class="text-xs font-bold text-[#1A365D] line-clamp-2 leading-snug font-sans group-hover:text-[#3182CE] transition-colors">
                {doc.title}
              </span>
              <span class="text-[9px] text-slate-400 font-mono mt-1">Шифр: {doc.documentNumber || 'Н/Д'}</span>
            </div>
          </div>
        {/each}
      </div>
    </section>
  {/if}

  <!-- Панель фильтров: Пилюли (Pill-shaped) -->
  <div class="flex flex-col gap-4 bg-white p-5 border border-[#E2E8F0] rounded-[8px] shadow-sm">
    <h3 class="text-xs font-bold text-slate-500 uppercase tracking-wider font-sans mb-1">Расширенные фильтры поиска</h3>

    <!-- Направление / Специальность -->
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-xs font-bold text-[#1A365D] min-w-[120px] uppercase tracking-wider font-sans">Специальность:</span>
      <button
        type="button"
        onclick={() => selectedProgram = 'all'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProgram === 'all' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Все специальности
      </button>
      <button
        type="button"
        onclick={() => selectedProgram = 'postgraduate'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProgram === 'postgraduate' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Аспирантура (Эпидемиология)
      </button>
      <button
        type="button"
        onclick={() => selectedProgram = 'residency'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProgram === 'residency' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Ординатура (Инфекционные болезни)
      </button>
    </div>

    <!-- Уровень образования -->
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-xs font-bold text-[#1A365D] min-w-[120px] uppercase tracking-wider font-sans">Уровень образования:</span>
      <button
        type="button"
        onclick={() => selectedEduLevel = 'all'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedEduLevel === 'all' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Все уровни
      </button>
      <button
        type="button"
        onclick={() => selectedEduLevel = 'higher'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedEduLevel === 'higher' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Высшее образование
      </button>
      <button
        type="button"
        onclick={() => selectedEduLevel = 'postgraduate_qualification'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedEduLevel === 'postgraduate_qualification' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Кадры высшей квалификации
      </button>
    </div>

    <!-- Тип документа -->
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-xs font-bold text-[#1A365D] min-w-[120px] uppercase tracking-wider font-sans">Тип документа:</span>
      <button
        type="button"
        onclick={() => selectedDocType = 'all'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDocType === 'all' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Все типы
      </button>
      <button
        type="button"
        onclick={() => selectedDocType = 'Position'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDocType === 'Position' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Положения
      </button>
      <button
        type="button"
        onclick={() => selectedDocType = 'Procedure'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDocType === 'Procedure' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Порядки
      </button>
      <button
        type="button"
        onclick={() => selectedDocType = 'Project'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDocType === 'Project' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Проекты
      </button>
    </div>

    <!-- Дата обновления -->
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-xs font-bold text-[#1A365D] min-w-[120px] uppercase tracking-wider font-sans">Дата обновления:</span>
      <button
        type="button"
        onclick={() => selectedDateFilter = 'all'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDateFilter === 'all' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Все время
      </button>
      <button
        type="button"
        onclick={() => selectedDateFilter = '7days'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDateFilter === '7days' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        За последние 7 дней
      </button>
      <button
        type="button"
        onclick={() => selectedDateFilter = '30days'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDateFilter === '30days' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        За последние 30 дней
      </button>
      <button
        type="button"
        onclick={() => selectedDateFilter = 'year'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDateFilter === 'year' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        За этот год (2026)
      </button>
    </div>

    <!-- Процесс -->
    <div class="flex flex-wrap items-center gap-2 border-t border-slate-100 pt-3">
      <span class="text-xs font-bold text-[#1A365D] min-w-[120px] uppercase tracking-wider font-sans">Раздел / Процесс:</span>
      <button
        type="button"
        onclick={() => selectedProcess = 'all'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProcess === 'all' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Все разделы
      </button>
      <button
        type="button"
        onclick={() => selectedProcess = 'certification'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProcess === 'certification' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Аттестация
      </button>
      <button
        type="button"
        onclick={() => selectedProcess = 'practice'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProcess === 'practice' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Практика
      </button>
      <button
        type="button"
        onclick={() => selectedProcess = 'stipends'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProcess === 'stipends' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Стипендии
      </button>
    </div>
  </div>

  <!-- Сетка документов (Mobile-first, 4 колонки на мобильном, 12 колонок на десктопе) -->
  <div class="grid grid-cols-4 gap-[16px] px-[20px] md:grid-cols-12 md:max-w-[1200px] md:mx-auto md:px-0 w-full pb-8">
    {#if loading}
      <div class="col-span-4 md:col-span-12 flex flex-col items-center justify-center py-16 gap-2 text-[#515f74]">
        <span class="material-symbols-outlined animate-spin text-3xl">sync</span>
        <span class="text-sm font-semibold">Загрузка базы знаний...</span>
      </div>
    {:else if filteredDocuments.length === 0}
      <div class="col-span-4 md:col-span-12 bg-white border border-[#E2E8F0] rounded-[8px] p-12 text-center flex flex-col items-center gap-3">
        <span class="material-symbols-outlined text-4xl text-slate-300">find_in_page</span>
        <h3 class="text-lg font-bold text-[#1A365D]">Ничего не найдено</h3>
        <p class="text-sm text-slate-500 max-w-md">
          По вашему запросу документы отсутствуют. Попробуйте изменить параметры поиска или сбросить фильтры.
        </p>
      </div>
    {:else}
      {#each paginatedDocuments as doc}
        {@const fileMeta = getFileTypeIcon(doc.fileType, doc.title)}
        <!-- Карточка документа (Белый фон, тонкая рамка, 0.25rem скругления, hover ambient-shadow, Inter) -->
        <div
          role="button"
          tabindex="0"
          onclick={() => selectDocument(doc)}
          onkeydown={(e) => e.key === 'Enter' && selectDocument(doc)}
          class="col-span-4 md:col-span-4 bg-[#FFFFFF] border border-[#E2E8F0] rounded-[0.25rem] p-4 flex flex-col justify-between h-56 transition-all duration-200 hover:shadow-[0_4px_12px_rgba(15,23,42,0.05)] cursor-pointer hover:border-slate-300 relative group text-left"
        >

          <!-- Звезда избранного и Иконка типа документа сверху справа -->
          <div class="absolute top-4 right-4 flex items-center gap-2">
            <!-- Кнопка редактирования -->
            <button
              type="button"
              onclick={(e) => {
                e.stopPropagation();
                window.dispatchEvent(new CustomEvent('edit-document', { detail: doc }));
              }}
              class="hover:text-[#3182CE] text-slate-400 p-1 flex items-center justify-center rounded-full active:scale-90 transition-transform"
              title="Редактировать документ"
              aria-label="Редактировать документ"
            >
              <span class="material-symbols-outlined text-lg">edit</span>
            </button>
            <button
              type="button"
              onclick={(e) => toggleFavorite(doc.id, e)}
              class="hover:text-amber-500 p-1 flex items-center justify-center rounded-full active:scale-90 transition-transform {favorites.includes(doc.id) ? 'text-amber-500' : 'text-slate-300'}"
              aria-label="Добавить в избранное"
            >
              <span class="material-symbols-outlined text-lg" style="font-variation-settings: 'FILL' {favorites.includes(doc.id) ? '1' : '0'};">star</span>
            </button>
            <span class="text-[10px] font-mono font-bold uppercase tracking-wider px-1.5 py-0.5 rounded-[4px] {fileMeta.bg} {fileMeta.color} border {fileMeta.border}">
              {fileMeta.label}
            </span>
            <span class="material-symbols-outlined text-lg {fileMeta.color}">{fileMeta.icon}</span>
          </div>

          <!-- Мета-информация верхняя (Тип документа, Программа) -->
          <div class="flex flex-col gap-1 pr-16">
            <span class="text-[10px] font-bold text-[#3182CE] uppercase tracking-wider font-sans">
              {getDocTypeRu(doc.documentType)} • {getProgramRu(doc.program)}
            </span>
            <!-- Название документа (Использует #1A365D и Inter font) -->
            <h4 class="text-sm font-bold text-[#1A365D] leading-snug font-sans group-hover:text-[#3182CE] transition-colors line-clamp-2 mt-1">
              {doc.title}
            </h4>
          </div>

          <!-- Аннотация (Максимум 2 строки) -->
          <p class="text-xs text-slate-500 leading-relaxed font-sans line-clamp-2 my-2 overflow-hidden">
            {doc.description || 'Аннотация документа отсутствует в системе.'}
          </p>

          <!-- Нижний блок (Дата/размер/шифр мелким капслоком в JetBrains Mono) -->
          <div class="border-t border-slate-100 pt-3 flex items-center justify-between mt-auto">
            <div class="flex flex-col gap-0.5">
              <span class="font-mono text-[9px] tracking-wider uppercase text-slate-400 leading-none">
                Шифр: {doc.documentNumber || 'Н/Д'}
              </span>
              <span class="font-mono text-[9px] tracking-wider uppercase text-slate-400 leading-none mt-1">
                Дата: {doc.approvalDate || '01.09.2026'}
              </span>
            </div>

            <!-- Версия и статус -->
            <div class="flex flex-col items-end gap-1">
              <span class="font-mono text-[9px] font-bold text-slate-600 uppercase leading-none">
                ВЕРСИЯ {doc.version || '1.0'}
              </span>
              <span class="font-mono text-[8px] font-bold px-1 py-0.5 rounded-[4px] bg-slate-100 text-slate-600 border border-slate-200">
                {getStatusRu(doc.status)}
              </span>
            </div>
          </div>

        </div>
      {/each}
    {/if}
  </div>

  <!-- Пагинация (в соответствии с дизайн-системой Lexicon Flux: закругление 4px (0.25rem)) -->
  {#if filteredDocuments.length > 0}
    <section class="max-w-[1200px] w-full flex justify-center items-center py-4 border-t border-[#E2E8F0] mt-2 mb-8 mx-auto px-5 md:px-0">
      <nav aria-label="Пагинация" class="flex items-center gap-2">
        <button
          type="button"
          onclick={() => currentPage = Math.max(1, currentPage - 1)}
          disabled={currentPage === 1}
          class="flex items-center gap-1.5 px-4 py-2 border border-[#E2E8F0] rounded-[0.25rem] text-[#0b1c30] bg-[#FFFFFF] hover:bg-[#F9F9FF] disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-sans text-xs font-semibold"
        >
          <span class="material-symbols-outlined text-sm">chevron_left</span>
          <span>Назад</span>
        </button>

        <div class="hidden md:flex items-center gap-2">
          {#each Array(totalPages) as _, i}
            {@const pageNum = i + 1}
            <button
              type="button"
              onclick={() => currentPage = pageNum}
              class="w-10 h-10 flex items-center justify-center rounded-[0.25rem] border text-xs font-semibold font-sans transition-colors {currentPage === pageNum ? 'bg-[#3182CE] text-white border-[#3182CE]' : 'border-[#E2E8F0] text-[#0b1c30] bg-[#FFFFFF] hover:bg-[#F9F9FF]'}"
            >
              {pageNum}
            </button>
          {/each}
        </div>

        <div class="md:hidden text-xs font-sans text-slate-500">
          Страница {currentPage} из {totalPages}
        </div>

        <button
          type="button"
          onclick={() => currentPage = Math.min(totalPages, currentPage + 1)}
          disabled={currentPage === totalPages}
          class="flex items-center gap-1.5 px-4 py-2 border border-[#E2E8F0] rounded-[0.25rem] text-[#0b1c30] bg-[#FFFFFF] hover:bg-[#F9F9FF] disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-sans text-xs font-semibold"
        >
          <span>Вперед</span>
          <span class="material-symbols-outlined text-sm">chevron_right</span>
        </button>
      </nav>
    </section>
  {/if}

</div>
{/if}

<style>
  .no-scrollbar::-webkit-scrollbar {
    display: none;
  }
  .no-scrollbar {
    -ms-overflow-style: none;
    scrollbar-width: none;
  }
</style>
