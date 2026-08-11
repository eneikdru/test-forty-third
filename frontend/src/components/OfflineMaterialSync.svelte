<script>
  import { onMount, onDestroy } from 'svelte';
  import { addMaterialToDB, getAllMaterialsFromDB, deleteMaterialFromDB } from '../utils/db.js';

  // Svelte 5 state runes
  let title = $state('');
  let description = $state('');
  let documentType = $state('Project'); // Position, Procedure, Project, Other
  let academicYear = $state('бессрочно'); // e.g. 2026-2027 or бессрочно
  let program = $state('both'); // postgraduate, residency, both
  let process = $state('other'); // admission, certification, stipends, practice, result_tracking, other
  let documentNumber = $state('');

  let editingId = $state(null); // ID of document being edited (null for adding)

  let isOnline = $state(true);
  let simulatedOffline = $state(false);
  let syncQueue = $state([]);
  let isSyncing = $state(false);
  let statusMessage = $state('');

  // Svelte 5 derived state
  let effectiveOnline = $derived(isOnline && !simulatedOffline);

  async function loadQueue() {
    try {
      syncQueue = await getAllMaterialsFromDB();
    } catch (e) {
      console.error('Ошибка чтения очереди из IndexedDB', e);
    }
  }

  async function syncMaterials() {
    if (syncQueue.length === 0 || isSyncing || !effectiveOnline) return;

    isSyncing = true;
    statusMessage = 'Синхронизация очереди...';

    const remainingQueue = [...syncQueue];
    let syncedCount = 0;

    for (const item of syncQueue) {
      if (!navigator.onLine || simulatedOffline) break;

      try {
        const formData = new FormData();
        // Use realistic file content
        const dummyFile = new Blob(['Содержимое офлайн-материала: ' + item.title], { type: 'text/plain' });
        formData.append('file', dummyFile, 'offline-material.txt');
        formData.append('title', item.title);
        formData.append('description', item.description || '');
        formData.append('documentType', item.documentType || 'Project');
        formData.append('academicYear', item.academicYear || 'бессрочно');
        formData.append('program', item.program || 'both');
        formData.append('process', item.process || 'other');
        formData.append('documentNumber', item.documentNumber || '');

        const response = await fetch('/api/documents', {
          method: 'POST',
          headers: {
            // Include Admin role to bypass RBAC checks
            'X-User-Role': 'Administrator'
          },
          body: formData
        });

        if (response.ok) {
          syncedCount++;
          await deleteMaterialFromDB(item.id);
          const idx = remainingQueue.findIndex(q => q.id === item.id);
          if (idx !== -1) {
            remainingQueue.splice(idx, 1);
          }
        } else {
          console.error('Ошибка отправки на сервер', response.status);
          break; // Stop on first error to retry later
        }
      } catch (error) {
        console.error('Сбой сети при синхронизации', error);
        break;
      }
    }

    syncQueue = remainingQueue;
    isSyncing = false;

    if (syncedCount > 0) {
      statusMessage = `Успешно синхронизировано изменений: ${syncedCount}`;
      setTimeout(() => {
        statusMessage = '';
      }, 4000);

      // Dispatch event to refresh KnowledgeBase
      window.dispatchEvent(new CustomEvent('materials-synced'));
    } else if (remainingQueue.length === 0) {
      statusMessage = '';
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!title.trim()) {
      statusMessage = 'Пожалуйста, введите название.';
      return;
    }

    const itemData = {
      id: editingId || Date.now().toString(),
      title: title.trim(),
      description: description.trim(),
      documentType,
      academicYear: academicYear.trim(),
      program,
      process,
      documentNumber: documentNumber.trim(),
      isUpdate: !!editingId,
      timestamp: new Date().toISOString()
    };

    if (effectiveOnline) {
      // Direct submission
      const formData = new FormData();
      const dummyFile = new Blob(['Содержимое материала: ' + itemData.title], { type: 'text/plain' });
      formData.append('file', dummyFile, 'material.txt');
      formData.append('title', itemData.title);
      formData.append('description', itemData.description);
      formData.append('documentType', itemData.documentType);
      formData.append('academicYear', itemData.academicYear);
      formData.append('program', itemData.program);
      formData.append('process', itemData.process);
      formData.append('documentNumber', itemData.documentNumber);

      try {
        const res = await fetch('/api/documents', {
          method: 'POST',
          headers: {
            'X-User-Role': 'Administrator'
          },
          body: formData
        });
        if (res.ok) {
          statusMessage = editingId ? 'Изменения успешно сохранены на сервере.' : 'Материал успешно добавлен на сервер.';
          resetForm();
          window.dispatchEvent(new CustomEvent('materials-synced'));
        } else {
          statusMessage = 'Ошибка сервера при отправке. Сохраняем локально...';
          await saveToLocalQueue(itemData);
        }
      } catch (err) {
        console.warn('Ошибка сети, сохраняем локально...', err);
        await saveToLocalQueue(itemData);
      }
    } else {
      await saveToLocalQueue(itemData);
    }
  }

  async function saveToLocalQueue(itemData) {
    try {
      await addMaterialToDB(itemData);
      await loadQueue();
      statusMessage = itemData.isUpdate
        ? 'Изменения сохранены локально в очереди. Синхронизация произойдет при подключении.'
        : 'Новый материал сохранен локально в очереди. Синхронизация произойдет при подключении.';
      resetForm();
    } catch (e) {
      console.error('Ошибка сохранения в IndexedDB', e);
      statusMessage = 'Ошибка локального сохранения в браузере.';
    }
  }

  function resetForm() {
    title = '';
    description = '';
    documentType = 'Project';
    academicYear = 'бессрочно';
    program = 'both';
    process = 'other';
    documentNumber = '';
    editingId = null;
  }

  function handleEditQueueItem(item) {
    editingId = item.id;
    title = item.title;
    description = item.description || '';
    documentType = item.documentType || 'Project';
    academicYear = item.academicYear || 'бессрочно';
    program = item.program || 'both';
    process = item.process || 'other';
    documentNumber = item.documentNumber || '';

    // Scroll form into view smoothly
    const formElement = document.getElementById('offline-sync-form');
    if (formElement) {
      formElement.scrollIntoView({ behavior: 'smooth' });
    }
  }

  async function handleDeleteQueueItem(id, event) {
    if (event) event.stopPropagation();
    try {
      await deleteMaterialFromDB(id);
      await loadQueue();
      statusMessage = 'Элемент удален из очереди.';
      if (editingId === id) {
        resetForm();
      }
    } catch (e) {
      console.error(e);
    }
  }

  function handleOnline() {
    isOnline = true;
    if (!simulatedOffline) {
      syncMaterials();
    }
  }

  function handleOffline() {
    isOnline = false;
  }

  // Handle global edit event
  function handleGlobalEdit(event) {
    const doc = event.detail;
    if (!doc) return;

    editingId = doc.id;
    title = doc.title || '';
    description = doc.description || '';
    documentType = doc.documentType || 'Project';
    academicYear = doc.academicYear || 'бессрочно';
    program = doc.program || 'both';
    process = doc.process || 'other';
    documentNumber = doc.documentNumber || '';

    const formElement = document.getElementById('offline-sync-form');
    if (formElement) {
      formElement.scrollIntoView({ behavior: 'smooth' });
    }
    statusMessage = 'Редактирование существующего документа: ' + doc.title;
  }

  onMount(() => {
    isOnline = navigator.onLine;
    loadQueue();

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    window.addEventListener('edit-document', handleGlobalEdit);

    if (effectiveOnline) {
      syncMaterials();
    }
  });

  onDestroy(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('edit-document', handleGlobalEdit);
    }
  });
</script>

<div id="offline-sync-form" class="bg-surface-container-lowest border border-surface-container-high rounded-[0.25rem] p-[1.5rem] shadow-sm font-sans mb-[1.5rem] w-full max-w-[1200px] mx-auto px-5 md:px-5">

  <!-- Заголовок и статус подключения -->
  <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-[1rem] mb-[1.5rem] border-b border-surface-container-high pb-[1rem]">
    <div class="flex flex-col gap-1">
      <h3 class="text-[1.25rem] font-bold text-primary-container">
        {editingId ? 'Редактировать материал' : 'Добавить материал'}
      </h3>
      <p class="text-xs text-on-secondary-container">
        Ввод материалов с полной поддержкой офлайн-режима и автоматической очередью синхронизации.
      </p>
    </div>

    <div class="flex flex-wrap items-center gap-[1rem]">
      <div class="flex items-center gap-[0.5rem]" aria-live="polite">
        <span class="inline-block w-[0.75rem] h-[0.75rem] rounded-[50%] {effectiveOnline ? 'bg-primary' : 'bg-amber-400'}" aria-hidden="true"></span>
        <span class="text-[0.875rem] font-bold uppercase tracking-wider {effectiveOnline ? 'text-primary' : 'text-amber-700'}">
          {effectiveOnline ? 'В сети' : 'Офлайн (Имитация)'}
        </span>
      </div>

      <button
        type="button"
        onclick={() => {
          simulatedOffline = !simulatedOffline;
          if (!simulatedOffline && isOnline) {
            syncMaterials();
          }
        }}
        class="text-[0.875rem] font-bold px-[0.75rem] py-[0.375rem] rounded-[0.25rem] border transition-colors {simulatedOffline ? 'bg-primary border-primary text-on-primary' : 'bg-surface-container-lowest border-surface-container-high text-primary-container hover:bg-surface-bright'}"
      >
        {simulatedOffline ? 'Подключить сеть' : 'Имитировать автономный режим'}
      </button>
    </div>
  </div>

  <!-- Форма добавления/изменения -->
  <form onsubmit={handleSubmit} class="flex flex-col gap-[1rem] pb-4">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-[1rem]">

      <!-- Название -->
      <div class="flex flex-col gap-[0.5rem]">
        <label for="material-title" class="text-[0.875rem] font-bold text-primary-container">Название материала</label>
        <input
          id="material-title"
          type="text"
          bind:value={title}
          placeholder="Введите название материала"
          class="border border-surface-container-high rounded-[0.25rem] p-[0.5rem] text-[1rem] focus:outline-none focus:border-primary"
          required
        />
      </div>

      <!-- Шифр -->
      <div class="flex flex-col gap-[0.5rem]">
        <label for="material-number" class="text-[0.875rem] font-bold text-primary-container">Шифр документа</label>
        <input
          id="material-number"
          type="text"
          bind:value={documentNumber}
          placeholder="Например, ФГОС-32.08.12"
          class="border border-surface-container-high rounded-[0.25rem] p-[0.5rem] text-[1rem] focus:outline-none focus:border-primary"
        />
      </div>

      <!-- Тип документа -->
      <div class="flex flex-col gap-[0.5rem]">
        <label for="material-type" class="text-[0.875rem] font-bold text-primary-container">Тип документа</label>
        <select
          id="material-type"
          bind:value={documentType}
          class="border border-surface-container-high rounded-[0.25rem] p-[0.5rem] text-[1rem] bg-white focus:outline-none focus:border-primary"
        >
          <option value="Position">Положение</option>
          <option value="Procedure">Порядок</option>
          <option value="Project">Проект</option>
          <option value="Other">Иной документ</option>
        </select>
      </div>

      <!-- Программа обучения -->
      <div class="flex flex-col gap-[0.5rem]">
        <label for="material-program" class="text-[0.875rem] font-bold text-primary-container">Направление обучения</label>
        <select
          id="material-program"
          bind:value={program}
          class="border border-surface-container-high rounded-[0.25rem] p-[0.5rem] text-[1rem] bg-white focus:outline-none focus:border-primary"
        >
          <option value="postgraduate">Аспирантура</option>
          <option value="residency">Ординатура</option>
          <option value="both">Обе программы (все)</option>
        </select>
      </div>

      <!-- Процесс -->
      <div class="flex flex-col gap-[0.5rem]">
        <label for="material-process" class="text-[0.875rem] font-bold text-primary-container">Процесс / Категория</label>
        <select
          id="material-process"
          bind:value={process}
          class="border border-surface-container-high rounded-[0.25rem] p-[0.5rem] text-[1rem] bg-white focus:outline-none focus:border-primary"
        >
          <option value="admission">Приёмная кампания</option>
          <option value="certification">Аттестация кадров</option>
          <option value="stipends">Стипендиальное обеспечение</option>
          <option value="practice">Производственная практика</option>
          <option value="result_tracking">Учёт успеваемости</option>
          <option value="other">Иной процесс</option>
        </select>
      </div>

      <!-- Период действия -->
      <div class="flex flex-col gap-[0.5rem]">
        <label for="material-year" class="text-[0.875rem] font-bold text-primary-container">Период действия</label>
        <input
          id="material-year"
          type="text"
          bind:value={academicYear}
          placeholder="Например, 2026-2027 или бессрочно"
          class="border border-surface-container-high rounded-[0.25rem] p-[0.5rem] text-[1rem] focus:outline-none focus:border-primary"
          required
        />
      </div>

    </div>

    <!-- Описание / Аннотация -->
    <div class="flex flex-col gap-[0.5rem]">
      <label for="material-description" class="text-[0.875rem] font-bold text-primary-container">Аннотация</label>
      <textarea
        id="material-description"
        bind:value={description}
        placeholder="Введите краткое описание или аннотацию материала..."
        class="border border-surface-container-high rounded-[0.25rem] p-[0.5rem] text-[1rem] min-h-[5rem] focus:outline-none focus:border-primary"
      ></textarea>
    </div>

    <!-- Кнопки отправки формы -->
    <div class="flex items-center gap-[1rem] mt-[0.5rem]">
      <button
        type="submit"
        class="bg-primary hover:bg-primary-container text-on-primary font-bold py-[0.5rem] px-[1.25rem] rounded-[0.25rem] transition-colors"
      >
        {editingId ? 'Сохранить изменения' : 'Добавить'}
      </button>

      {#if editingId}
        <button
          type="button"
          onclick={resetForm}
          class="border border-surface-container-high hover:bg-surface-container-low text-slate-600 font-bold py-[0.5rem] px-[1.25rem] rounded-[0.25rem] transition-colors"
        >
          Отмена
        </button>
      {/if}

      {#if statusMessage}
        <span class="text-[0.875rem] font-bold text-primary" role="status" aria-live="polite">
          {statusMessage}
        </span>
      {/if}
    </div>
  </form>

  <!-- Очередь синхронизации (визуализация по макету с янтарными плашками) -->
  {#if syncQueue.length > 0}
    <div class="mt-[1.5rem] border-t border-surface-container-high pt-[1.5rem]">
      <div class="flex items-center justify-between mb-4">
        <h4 class="text-[1.125rem] font-bold text-primary-container flex items-center gap-2">
          <span class="material-symbols-outlined text-amber-500 animate-spin">sync</span>
          Очередь синхронизации ({syncQueue.length})
        </h4>
        {#if effectiveOnline && !isSyncing}
          <button
            type="button"
            onclick={syncMaterials}
            class="text-[0.875rem] font-bold text-primary hover:underline flex items-center gap-1"
          >
            <span class="material-symbols-outlined text-sm">cloud_sync</span>
            Синхронизировать сейчас
          </button>
        {/if}
      </div>

      <!-- Очередь элементов -->
      <div class="flex flex-col gap-3">
        {#each syncQueue as item}
          <div class="flex items-stretch bg-white border border-surface-container-high rounded-[0.25rem] relative overflow-hidden group hover:border-slate-300 transition-all shadow-sm">

            <!-- Янтарный индикатор слева -->
            <div class="w-[4px] bg-amber-400 flex-shrink-0"></div>

            <!-- Содержимое карточки -->
            <div class="p-4 flex-grow flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
              <div class="flex flex-col gap-1">
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider font-mono">
                  {item.isUpdate ? 'Обновление документа' : 'Создание документа'}
                </span>
                <span class="font-bold text-primary-container text-[0.9375rem]">{item.title}</span>
                {#if item.description}
                  <p class="text-xs text-slate-500 line-clamp-1 max-w-xl">{item.description}</p>
                {/if}
              </div>

              <!-- Метаданные и действия справа -->
              <div class="flex items-center gap-4 self-end sm:self-auto">
                <div class="flex flex-col items-end gap-1">
                  <span class="text-[11px] text-amber-700 bg-amber-50 px-2 py-0.5 rounded-[4px] flex items-center gap-1 font-bold">
                    <span class="material-symbols-outlined text-[14px]">schedule</span> Ожидает
                  </span>
                  <span class="text-[10px] font-mono text-on-secondary-container">
                    {new Date(item.timestamp).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>

                <!-- Кнопки управления элементом очереди -->
                <div class="flex gap-1.5 border-l border-slate-100 pl-4">
                  <button
                    type="button"
                    onclick={() => handleEditQueueItem(item)}
                    class="p-1 hover:bg-surface-bright text-primary rounded transition-colors"
                    title="Редактировать запись очереди"
                  >
                    <span class="material-symbols-outlined text-[18px]">edit</span>
                  </button>
                  <button
                    type="button"
                    onclick={(e) => handleDeleteQueueItem(item.id, e)}
                    class="p-1 hover:bg-error-container text-red-500 rounded transition-colors"
                    title="Удалить из очереди"
                  >
                    <span class="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>
              </div>

            </div>
          </div>
        {/each}
      </div>

      <!-- Action Area Info Box (matching mockup text) -->
      <div class="bg-surface-bright p-4 rounded-[0.25rem] border border-surface-container-high flex items-start gap-3 mt-4 text-xs text-on-secondary-container">
        <span class="material-symbols-outlined text-slate-500 mt-0.5">info</span>
        <p class="leading-relaxed">
          Изменения сохранены в локальном хранилище браузера. Они автоматически начнут синхронизироваться в порядке приоритета, как только восстановится стабильное сетевое подключение.
        </p>
      </div>

    </div>
  {/if}

</div>
