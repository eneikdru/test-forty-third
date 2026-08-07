<script>
  import { onMount } from 'svelte';

  // Svelte 5 state runes for props and reactive states
  let { role = 'Economist' } = $props();

  let searchQuery = $state('');
  let selectedProgram = $state(''); // '', 'postgraduate', 'residency', 'both'
  let selectedDocumentType = $state(''); // '', 'Position', 'Procedure', 'Project', 'Other'
  let selectedProcess = $state(''); // '', 'admission', 'certification', 'stipends', 'practice', 'result_tracking', 'other'

  let documents = $state([]);
  let loading = $state(false);
  let errorMessage = $state('');

  // Translations
  function getDocumentTypeName(type) {
    const map = {
      Position: 'Положение',
      Procedure: 'Порядок',
      Project: 'Проект',
      Other: 'Иной документ'
    };
    return map[type] || 'Документ';
  }

  function getProgramName(program) {
    const map = {
      postgraduate: 'Аспирантура',
      residency: 'Ординатура',
      both: 'Аспирантура и ординатура'
    };
    return map[program] || 'Все программы';
  }

  function getProcessName(process) {
    const map = {
      admission: 'Приём',
      certification: 'Аттестация',
      stipends: 'Стипендии',
      practice: 'Практика',
      result_tracking: 'Учёт результатов',
      other: 'Учебный процесс'
    };
    return map[process] || 'Иной процесс';
  }

  function getDocumentSize(docId) {
    if (!docId) return '1.2 МБ';
    let hash = 0;
    for (let i = 0; i < docId.length; i++) {
      hash = docId.charCodeAt(i) + ((hash << 5) - hash);
    }
    const size = 0.5 + Math.abs(hash % 40) / 10;
    return `${size.toFixed(1)} МБ`;
  }

  function formatRussianDateCaps(dateStr) {
    if (!dateStr) return '01 СЕНТЯБРЯ 2026';
    try {
      const date = new Date(dateStr);
      const months = [
        'ЯНВАРЯ', 'ФЕВРАЛЯ', 'МАРТА', 'АПРЕЛЯ', 'МАЯ', 'ИЮНЯ',
        'ИЮЛЯ', 'АВГУСТА', 'СЕНТЯБРЯ', 'ОКТЯБРЯ', 'НОЯБРЯ', 'ДЕКАБРЯ'
      ];
      const day = String(date.getDate()).padStart(2, '0');
      const month = months[date.getMonth()];
      const year = date.getFullYear();
      return `${day} ${month} ${year}`;
    } catch (e) {
      return '01 СЕНТЯБРЯ 2026';
    }
  }

  // Get card type style mapping (PDF: red, DOC: blue, Spreadsheets: green)
  function getCardTypeStyle(docType) {
    if (docType === 'Procedure') {
      return {
        label: 'ПДФ',
        bg: 'bg-[#FFF5F5]',
        border: 'border-[#FEB2B2]',
        text: 'text-[#E53E3E]',
        icon: 'picture_as_pdf'
      };
    } else if (docType === 'Project') {
      return {
        label: 'ТАБЛИЦА',
        bg: 'bg-[#F0FFF4]',
        border: 'border-[#9AE6B4]',
        text: 'text-[#38A169]',
        icon: 'table_view'
      };
    } else {
      return {
        label: 'ДОК',
        bg: 'bg-[#EBF8FF]',
        border: 'border-[#90CDF4]',
        text: 'text-[#3182CE]',
        icon: 'description'
      };
    }
  }

  // Fetch documents from backend API based on search query and current role
  async function loadData() {
    loading = true;
    errorMessage = '';
    try {
      let url;
      if (searchQuery.trim() !== '') {
        url = `/api/documents/search?q=${encodeURIComponent(searchQuery)}`;
        if (selectedProgram) url += `&program=${selectedProgram}`;
        if (selectedDocumentType) url += `&documentType=${selectedDocumentType}`;
      } else {
        url = '/api/documents';
        let params = [];
        if (selectedProgram) params.push(`program=${selectedProgram}`);
        if (selectedProcess) params.push(`process=${selectedProcess}`);
        if (params.length > 0) {
          url += '?' + params.join('&');
        }
      }

      const res = await fetch(url, {
        headers: {
          'X-User-Role': role
        }
      });

      if (res.ok) {
        const data = await res.json();
        if (searchQuery.trim() !== '') {
          // Search endpoint returns [{ document: ..., rank: ... }]
          let rawDocs = data.map(item => item.document);
          if (selectedProcess) {
            rawDocs = rawDocs.filter(d => d.process === selectedProcess);
          }
          documents = rawDocs;
        } else {
          // List endpoint returns DocumentResponseDTO[]
          let rawDocs = data;
          if (selectedDocumentType) {
            rawDocs = rawDocs.filter(d => d.documentType === selectedDocumentType);
          }
          documents = rawDocs;
        }
      } else {
        errorMessage = 'Произошла ошибка при загрузке документов базы знаний.';
      }
    } catch (err) {
      errorMessage = 'Ошибка сетевого соединения с сервером базы знаний.';
    } finally {
      loading = false;
    }
  }

  // Svelte 5 reactive effects for state updates
  $effect(() => {
    // Track dependencies to trigger data reload on any state changes
    const r = role;
    const q = searchQuery;
    const p = selectedProgram;
    const dt = selectedDocumentType;
    const pr = selectedProcess;
    loadData();
  });

  onMount(() => {
    loadData();
  });
</script>

<style>
  /* Inter is the main font, JetBrains Mono is for technical metadata */
  .lexicon-card {
    font-family: 'Inter', sans-serif;
    border: 1px solid #E2E8F0;
    border-radius: 0.25rem; /* Exactly 0.25rem rounded corners as required by AC */
    background-color: #FFFFFF;
  }

  .lexicon-card:hover {
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05); /* Weak ambient-shadow as required */
    border-color: #3182CE;
  }

  .technical-metadata {
    font-family: 'JetBrains Mono', monospace;
  }
</style>

<div class="flex-1 flex flex-col bg-[#F9F9FF] text-[#0b1c30]">

  <!-- Sticky header search bar (48px high) fixed at top with tinted background -->
  <div class="sticky top-0 z-10 bg-[#F9F9FF]/90 backdrop-blur border-b border-[#E2E8F0] px-6 py-3 flex items-center justify-between">
    <div class="relative w-full max-w-xl group h-12 flex items-center">
      <span class="material-symbols-outlined text-[#76777d] absolute left-3 pointer-events-none">search</span>
      <input
        type="text"
        bind:value={searchQuery}
        placeholder="Поиск по статьям, регламентам и документам..."
        class="w-full h-12 pl-10 pr-4 bg-[#F8FAFC] border border-[#E2E8F0] rounded-lg text-sm text-[#0b1c30] placeholder-[#76777d] focus:outline-none focus:border-[#3182CE] focus:ring-1 focus:ring-[#3182CE] transition-all"
      />
    </div>

    <div class="hidden sm:flex items-center gap-2 text-xs font-semibold text-[#515f74]">
      <span class="material-symbols-outlined text-sm">menu_book</span>
      <span>СИСТЕМА ЛЕКСИКОН ФЛАКС</span>
    </div>
  </div>

  <div class="p-6 flex flex-col gap-6 max-w-[1200px] w-full mx-auto">

    <!-- Title Area -->
    <div class="flex flex-col gap-1">
      <h2 class="text-2xl font-bold text-[#1A365D] tracking-tight">База знаний ЦНИИ Эпидемиологии</h2>
      <p class="text-sm text-[#515f74]">Регламентирующие акты, учебные материалы, положения и государственные стандарты.</p>
    </div>

    <!-- Filter Pills Section -->
    <div class="flex flex-col gap-4 bg-white p-5 rounded-lg border border-[#E2E8F0]">

      <!-- Program Filters -->
      <div class="flex flex-wrap items-center gap-2">
        <span class="text-xs font-bold text-[#515f74] mr-2">Программа:</span>
        <button
          type="button"
          onclick={() => selectedProgram = ''}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProgram === '' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Все программы
        </button>
        <button
          type="button"
          onclick={() => selectedProgram = 'postgraduate'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProgram === 'postgraduate' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Аспирантура
        </button>
        <button
          type="button"
          onclick={() => selectedProgram = 'residency'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProgram === 'residency' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Ординатура
        </button>
      </div>

      <!-- Document Type Filters -->
      <div class="flex flex-wrap items-center gap-2">
        <span class="text-xs font-bold text-[#515f74] mr-2">Тип документа:</span>
        <button
          type="button"
          onclick={() => selectedDocumentType = ''}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedDocumentType === '' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Все типы
        </button>
        <button
          type="button"
          onclick={() => selectedDocumentType = 'Position'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedDocumentType === 'Position' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Положение
        </button>
        <button
          type="button"
          onclick={() => selectedDocumentType = 'Procedure'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedDocumentType === 'Procedure' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Порядок
        </button>
        <button
          type="button"
          onclick={() => selectedDocumentType = 'Project'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedDocumentType === 'Project' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Проект
        </button>
        <button
          type="button"
          onclick={() => selectedDocumentType = 'Other'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedDocumentType === 'Other' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Иное
        </button>
      </div>

      <!-- Process Filters -->
      <div class="flex flex-wrap items-center gap-2">
        <span class="text-xs font-bold text-[#515f74] mr-2">Процесс:</span>
        <button
          type="button"
          onclick={() => selectedProcess = ''}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProcess === '' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Все процессы
        </button>
        <button
          type="button"
          onclick={() => selectedProcess = 'admission'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProcess === 'admission' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Приём
        </button>
        <button
          type="button"
          onclick={() => selectedProcess = 'certification'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProcess === 'certification' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Аттестация
        </button>
        <button
          type="button"
          onclick={() => selectedProcess = 'stipends'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProcess === 'stipends' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Стипендии
        </button>
        <button
          type="button"
          onclick={() => selectedProcess = 'practice'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProcess === 'practice' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Практика
        </button>
        <button
          type="button"
          onclick={() => selectedProcess = 'result_tracking'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProcess === 'result_tracking' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Учёт результатов
        </button>
        <button
          type="button"
          onclick={() => selectedProcess = 'other'}
          class="px-3 py-1.5 rounded-full text-xs font-semibold transition-all {selectedProcess === 'other' ? 'bg-[#3182CE] text-white' : 'bg-[#F8FAFC] text-[#515f74] border border-[#E2E8F0] hover:bg-[#F1F5F9]'}"
        >
          Учебный процесс
        </button>
      </div>

    </div>

    <!-- Error Display -->
    {#if errorMessage}
      <div class="bg-[#ffdad6] text-[#93000a] p-4 rounded border border-[#ba1a1a] flex items-center gap-3">
        <span class="material-symbols-outlined">error</span>
        <span class="font-semibold text-sm">{errorMessage}</span>
      </div>
    {/if}

    <!-- Loading State -->
    {#if loading}
      <div class="flex flex-col items-center justify-center py-16 gap-3 text-[#515f74]">
        <span class="material-symbols-outlined animate-spin text-4xl text-[#3182CE]">sync</span>
        <span class="text-sm font-semibold">Идет поиск и загрузка документов...</span>
      </div>
    {:else}

      <!-- Document Cards Grid (4 cols mobile, 12 cols desktop, 16px gutters) -->
      <div class="grid grid-cols-4 md:grid-cols-12 gap-4">
        {#each documents as doc}
          {@const cardStyle = getCardTypeStyle(doc.documentType)}

          <div class="lexicon-card col-span-4 md:col-span-4 p-5 flex flex-col justify-between h-56 transition-all duration-200">
            <!-- Header of card -->
            <div class="flex justify-between items-start gap-2">
              <span class="technical-metadata text-[10px] tracking-wider text-[#76777d] uppercase font-bold">
                {doc.documentNumber || 'БЕЗ НОМЕРА'}
              </span>
              <!-- Document type icon on top-right -->
              <span class="material-symbols-outlined text-xl {cardStyle.text}" title={getDocumentTypeName(doc.documentType)}>
                {cardStyle.icon}
              </span>
            </div>

            <!-- Content Area of card -->
            <div class="my-3 flex-1 flex flex-col justify-start">
              <h4 class="text-base font-bold text-[#1A365D] leading-snug line-clamp-2" style="font-family: 'Inter', sans-serif;">
                {doc.title}
              </h4>
              <p class="text-xs text-[#515f74] mt-2 line-clamp-2">
                {doc.description || 'Описание документа отсутствует.'}
              </p>
            </div>

            <!-- Footer of card (small caps date and size) -->
            <div class="border-t border-[#E2E8F0] pt-3 flex flex-col gap-1">
              <div class="flex justify-between items-center text-[10px] text-[#76777d] uppercase tracking-wider font-mono">
                <span>{formatRussianDateCaps(doc.updatedAt)}</span>
                <span>{getDocumentSize(doc.id)}</span>
              </div>
              <div class="flex justify-between items-center text-[10px] text-[#76777d] mt-1">
                <span>Программа: {getProgramName(doc.program)}</span>
                <span>Версия: {doc.version || '1.0'}</span>
              </div>
            </div>
          </div>
        {:else}
          <div class="col-span-4 md:col-span-12 py-16 bg-white border border-[#E2E8F0] rounded flex flex-col items-center justify-center gap-2 text-[#76777d]">
            <span class="material-symbols-outlined text-4xl">search_off</span>
            <span class="text-sm font-semibold">Документы не найдены в соответствии с выбранными фильтрами</span>
          </div>
        {/each}
      </div>

    {/if}

  </div>

</div>
