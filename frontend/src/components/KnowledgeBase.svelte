<script>
  import { onMount } from 'svelte';

  // Props
  let { selectedRole = 'Economist' } = $props();

  // Svelte 5 state runes
  let searchQuery = $state('');
  let selectedProgram = $state('all'); // 'all', 'postgraduate', 'residency', 'both'
  let selectedDocType = $state('all'); // 'all', 'Position', 'Procedure', 'Project', 'Other'
  let selectedProcess = $state('all'); // 'all', 'admission', 'certification', 'stipends', 'practice', 'result_tracking', 'other'

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
      fileType: 'PDF'
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
      fileType: 'Doc'
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
      fileType: 'Table'
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
      fileType: 'PDF'
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
      fileType: 'Doc'
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
      fileType: 'PDF'
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
      fileType: 'Doc'
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
      fileType: 'Table'
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
      return { icon: 'picture_as_pdf', color: 'text-[#E53E3E]', bg: 'bg-[#FFF5F5]', border: 'border-[#FED7D7]', label: 'PDF' };
    }
    if (fileType === 'Table' || lowerTitle.includes('таблиц') || lowerTitle.includes('протокол') || lowerTitle.includes('оплат') || lowerTitle.includes('бюджет')) {
      return { icon: 'table_chart', color: 'text-[#38A169]', bg: 'bg-[#F0FFF4]', border: 'border-[#C6F6D5]', label: 'Таблица' };
    }
    return { icon: 'article', color: 'text-[#3182CE]', bg: 'bg-[#EBF8FF]', border: 'border-[#BEE3F8]', label: 'Документ' };
  }

  // Reactive derived filtered list
  let filteredDocuments = $derived.by(() => {
    // Merge API-loaded and local documents (avoiding duplicates)
    let combined = [...documents];
    const combinedIds = new Set(combined.map(d => d.id));

    for (const localDoc of localDocuments) {
      if (!combinedIds.has(localDoc.id)) {
        combined.push(localDoc);
      }
    }

    // Apply filtering on combined list
    return combined.filter(doc => {
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

      return true;
    });
  });

  async function fetchBackendDocuments() {
    loading = true;
    errorMessage = '';
    try {
      // Call search with empty query to get all documents matching current role
      const res = await fetch(`/api/documents/search?q=${encodeURIComponent(searchQuery)}`, {
        headers: {
          'X-User-Role': selectedRole
        }
      });
      if (res.ok) {
        const data = await res.json();
        // The endpoint returns list of SearchResultResponse with structure { document, rank }
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
          schemaTags: item.document.schemaTags || []
        }));
      } else {
        // Silent recovery to local data on unauthorized/forbidden
        console.warn('Backend documents loaded from local storage due to API status');
      }
    } catch (err) {
      console.error('Ошибка сети при загрузке документов:', err);
    } finally {
      loading = false;
    }
  }

  // Trigger search on query/role changes
  $effect(() => {
    fetchBackendDocuments();
  });

  onMount(() => {
    fetchBackendDocuments();
  });
</script>

<div class="flex flex-col gap-6 w-full max-w-[1200px] mx-auto px-5 md:px-0 bg-[#F9F9FF] min-h-screen text-[#0b1c30]">

  <!-- Заголовок базы знаний -->
  <div class="flex flex-col gap-1.5 mt-4">
    <h2 class="text-2xl font-bold text-[#1A365D] tracking-tight font-sans">База знаний центра</h2>
    <p class="text-sm text-[#515f74] font-sans">
      Официальный реестр нормативно-правовых и методических материалов ЦНИИ Эпидемиологии. Только актуальные версии документов.
    </p>
  </div>

  <!-- Крупная строка поиска закрепленная сверху -->
  <div class="sticky top-0 z-10 bg-[#F8FAFC]/95 backdrop-blur border border-[#E2E8F0] p-1.5 rounded-[8px] h-[48px] flex items-center shadow-sm w-full transition-all duration-200">
    <span class="material-symbols-outlined text-[#1A365D] px-3">search</span>
    <input
      type="text"
      bind:value={searchQuery}
      placeholder="Поиск по названию, аннотации или шифру документа..."
      class="flex-1 bg-transparent border-0 ring-0 focus:ring-0 focus:outline-none text-sm text-[#0b1c30] placeholder-slate-400 font-sans h-full"
    />
    {#if searchQuery}
      <button
        type="button"
        onclick={() => searchQuery = ''}
        class="text-slate-400 hover:text-[#3182CE] p-1 flex items-center justify-center mr-2"
        aria-label="Очистить поиск"
      >
        <span class="material-symbols-outlined text-lg">close</span>
      </button>
    {/if}
  </div>

  <!-- Панель фильтров: Пилюли (Pill-shaped) -->
  <div class="flex flex-col gap-3 bg-white p-4 border border-[#E2E8F0] rounded-[8px] shadow-sm">
    <!-- Направление -->
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-xs font-bold text-[#1A365D] min-w-[100px] uppercase tracking-wider font-sans">Программа:</span>
      <button
        type="button"
        onclick={() => selectedProgram = 'all'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProgram === 'all' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Все
      </button>
      <button
        type="button"
        onclick={() => selectedProgram = 'postgraduate'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProgram === 'postgraduate' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Аспирантура
      </button>
      <button
        type="button"
        onclick={() => selectedProgram = 'residency'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedProgram === 'residency' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Ординатура
      </button>
    </div>

    <!-- Тип документа -->
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-xs font-bold text-[#1A365D] min-w-[100px] uppercase tracking-wider font-sans">Тип акта:</span>
      <button
        type="button"
        onclick={() => selectedDocType = 'all'}
        class="px-3 py-1 rounded-full text-xs font-medium transition-all duration-150 {selectedDocType === 'all' ? 'bg-[#3182CE] text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700'}"
      >
        Все
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

    <!-- Процесс -->
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-xs font-bold text-[#1A365D] min-w-[100px] uppercase tracking-wider font-sans">Процесс:</span>
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
      {#each filteredDocuments as doc}
        {@const fileMeta = getFileTypeIcon(doc.fileType, doc.title)}
        <!-- Карточка документа (Белый фон, тонкая рамка, 0.25rem скругления, hover ambient-shadow, Inter) -->
        <article class="col-span-4 md:col-span-4 bg-[#FFFFFF] border border-[#E2E8F0] rounded-[0.25rem] p-4 flex flex-col justify-between h-56 transition-all duration-200 hover:shadow-[0_4px_12px_rgba(15,23,42,0.05)] cursor-pointer hover:border-slate-300 relative group">

          <!-- Иконка типа документа сверху справа -->
          <div class="absolute top-4 right-4 flex items-center gap-1">
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
                v{doc.version || '1.0'}
              </span>
              <span class="font-mono text-[8px] font-bold px-1 py-0.5 rounded-[4px] bg-slate-100 text-slate-600 border border-slate-200">
                {getStatusRu(doc.status)}
              </span>
            </div>
          </div>

        </article>
      {/each}
    {/if}
  </div>

</div>
