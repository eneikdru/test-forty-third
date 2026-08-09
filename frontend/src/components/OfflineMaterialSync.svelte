<script>
  import { onMount, onDestroy } from 'svelte';
  import { addMaterialToDB, getAllMaterialsFromDB, deleteMaterialFromDB } from '../utils/db.js';

  let title = $state('');
  let description = $state('');
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
    statusMessage = 'Синхронизация...';

    const remainingQueue = [...syncQueue];
    let syncedCount = 0;

    for (const item of syncQueue) {
      // Double check simulated and real network state inside the loop
      if (!navigator.onLine || simulatedOffline) break;

      try {
        const formData = new FormData();
        const dummyFile = new Blob(['содержимое'], { type: 'text/plain' });
        formData.append('file', dummyFile, 'offline-material.txt');
        formData.append('title', item.title);
        formData.append('description', item.description || '');
        formData.append('documentType', 'Project');
        formData.append('program', 'all');
        formData.append('process', 'other');

        const response = await fetch('/api/documents', {
          method: 'POST',
          body: formData
        });

        if (response.ok) {
          syncedCount++;
          await deleteMaterialFromDB(item.id);
          remainingQueue.shift(); // Remove the synced item
        } else {
          break; // Stop on first error to retry later
        }
      } catch (error) {
        console.error('Сбой синхронизации', error);
        break;
      }
    }

    syncQueue = remainingQueue;
    isSyncing = false;

    if (syncedCount > 0) {
      statusMessage = `Успешно синхронизировано материалов: ${syncedCount}`;
      setTimeout(() => {
        statusMessage = '';
      }, 3000);

      // Dispatch an event so parent can refresh if needed
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

    if (effectiveOnline) {
      // Direct submission
      const formData = new FormData();
      const dummyFile = new Blob(['содержимое'], { type: 'text/plain' });
      formData.append('file', dummyFile, 'material.txt');
      formData.append('title', title);
      formData.append('description', description);
      formData.append('documentType', 'Project');
      formData.append('program', 'all');
      formData.append('process', 'other');

      try {
        const res = await fetch('/api/documents', {
          method: 'POST',
          body: formData
        });
        if (res.ok) {
          statusMessage = 'Материал успешно добавлен.';
          title = '';
          description = '';
          window.dispatchEvent(new CustomEvent('materials-synced'));
        } else {
          statusMessage = 'Ошибка при добавлении материала.';
        }
      } catch (err) {
        // Fallback to queue if failed due to network
        await addToQueue();
      }
    } else {
      await addToQueue();
    }
  }

  async function addToQueue() {
    const newItem = {
      id: Date.now().toString(),
      title,
      description,
      timestamp: new Date().toISOString()
    };

    try {
      await addMaterialToDB(newItem);
      syncQueue = [...syncQueue, newItem];
      statusMessage = 'Сохранено локально в IndexedDB. Будет синхронизировано при подключении.';
      title = '';
      description = '';
    } catch (e) {
      console.error('Ошибка сохранения в IndexedDB', e);
      statusMessage = 'Ошибка локального сохранения.';
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

  onMount(() => {
    isOnline = navigator.onLine;
    loadQueue();

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    if (effectiveOnline) {
      syncMaterials();
    }
  });

  onDestroy(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    }
  });
</script>

<div class="bg-[#FFFFFF] border border-[#E2E8F0] rounded-[0.25rem] p-[1.5rem] shadow-sm font-sans mb-[1.5rem] w-full max-w-[1200px] mx-auto px-5 md:px-5">
  <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-[1rem] mb-[1.5rem] border-b border-[#E2E8F0] pb-[1rem]">
    <h3 class="text-[1.25rem] font-bold text-[#1A365D]">Добавить материал</h3>

    <div class="flex flex-wrap items-center gap-[1rem]">
      <div class="flex items-center gap-[0.5rem]" aria-live="polite">
        <span class="inline-block w-[0.75rem] h-[0.75rem] rounded-[50%] {effectiveOnline ? 'bg-[#3182CE]' : 'bg-[#E2E8F0] border border-[#1A365D]'}" aria-hidden="true"></span>
        <span class="text-[0.875rem] font-medium {effectiveOnline ? 'text-[#3182CE]' : 'text-[#515f74]'}">
          {effectiveOnline ? 'В сети' : 'Автономный режим (Офлайн)'}
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
        class="text-[0.875rem] font-bold px-[0.75rem] py-[0.375rem] rounded-[0.25rem] border transition-colors {simulatedOffline ? 'bg-[#3182CE] border-[#3182CE] text-[#FFFFFF]' : 'bg-[#FFFFFF] border-[#E2E8F0] text-[#1A365D] hover:bg-[#F9F9FF]'}"
      >
        {simulatedOffline ? 'Подключить сеть' : 'Имитировать автономный режим'}
      </button>
    </div>
  </div>

  <form onsubmit={handleSubmit} class="flex flex-col gap-[1rem]">
    <div class="flex flex-col gap-[0.5rem]">
      <label for="material-title" class="text-[0.875rem] font-bold text-[#1A365D]">Название материала</label>
      <input
        id="material-title"
        type="text"
        bind:value={title}
        placeholder="Введите название"
        class="border border-[#E2E8F0] rounded-[0.25rem] p-[0.5rem] text-[1rem] focus:outline-none focus:border-[#3182CE]"
        required
      />
    </div>

    <div class="flex flex-col gap-[0.5rem]">
      <label for="material-description" class="text-[0.875rem] font-bold text-[#1A365D]">Аннотация</label>
      <textarea
        id="material-description"
        bind:value={description}
        placeholder="Введите аннотацию"
        class="border border-[#E2E8F0] rounded-[0.25rem] p-[0.5rem] text-[1rem] min-h-[5rem] focus:outline-none focus:border-[#3182CE]"
      ></textarea>
    </div>

    <div class="flex items-center gap-[1rem] mt-[0.5rem]">
      <button
        type="submit"
        class="bg-[#3182CE] hover:bg-[#1A365D] text-[#FFFFFF] font-bold py-[0.5rem] px-[1rem] rounded-[0.25rem] transition-colors"
      >
        Добавить
      </button>
      {#if statusMessage}
        <span class="text-[0.875rem] text-[#3182CE]" role="status" aria-live="polite">{statusMessage}</span>
      {/if}
    </div>
  </form>

  {#if syncQueue.length > 0}
    <div class="mt-[1.5rem] border-t border-[#E2E8F0] pt-[1rem]">
      <h4 class="text-[1rem] font-bold text-[#1A365D] mb-[0.5rem]">Очередь синхронизации ({syncQueue.length})</h4>
      <ul class="flex flex-col gap-[0.5rem]">
        {#each syncQueue as item}
          <li class="flex flex-col bg-[#F9F9FF] p-[0.5rem] rounded-[0.25rem] border border-[#E2E8F0]">
            <span class="text-[0.875rem] font-bold text-[#1A365D]">{item.title}</span>
            <span class="text-[0.75rem] font-mono text-[#515f74]">{new Date(item.timestamp).toLocaleString('ru-RU')}</span>
          </li>
        {/each}
      </ul>
      {#if effectiveOnline && !isSyncing}
        <button
          type="button"
          onclick={syncMaterials}
          class="mt-[0.5rem] text-[0.875rem] font-bold text-[#3182CE] hover:underline"
        >
          Синхронизировать сейчас
        </button>
      {/if}
    </div>
  {/if}
</div>
