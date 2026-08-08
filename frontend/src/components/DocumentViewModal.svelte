<script>
  import { onMount } from 'svelte';

  // Props using Svelte 5 runes
  let { document: doc = null, onClose = () => {}, selectedRole = 'Economist' } = $props();

  // Component state
  let comments = $state([]);
  let showComments = $state(false);
  let showCommentForm = $state(false);
  let showActualizationForm = $state(false);

  let commentText = $state('');
  let actualizationReason = $state('');

  let toastMessage = $state('');
  let toastType = $state(''); // 'success' or 'error'
  let loadingComments = $state(false);

  function showToast(msg, type = 'success') {
    toastMessage = msg;
    toastType = type;
    setTimeout(() => {
      toastMessage = '';
    }, 4000);
  }

  // Fetch comments
  async function loadComments() {
    if (!doc) return;
    loadingComments = true;
    try {
      const res = await fetch(`/api/documents/${doc.id}/comments`, {
        headers: {
          'X-User-Role': selectedRole
        }
      });
      if (res.ok) {
        comments = await res.json();
      } else {
        console.error('Не удалось загрузить комментарии');
      }
    } catch (err) {
      console.error('Ошибка сети при загрузке комментариев:', err);
    } finally {
      loadingComments = false;
    }
  }

  // Post comment
  async function submitComment(e) {
    e.preventDefault();
    if (!commentText.trim()) {
      showToast('Текст комментария не может быть пустым', 'error');
      return;
    }

    try {
      const res = await fetch(`/api/documents/${doc.id}/comments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Role': selectedRole
        },
        body: JSON.stringify({ text: commentText.trim() })
      });

      if (res.ok) {
        const newComment = await res.json();
        comments = [...comments, newComment];
        commentText = '';
        showCommentForm = false;
        showToast('Комментарий успешно добавлен и зарегистрирован в системе');
      } else {
        const err = await res.json().catch(() => ({}));
        showToast(err.message || 'Ошибка при отправке комментария', 'error');
      }
    } catch (err) {
      showToast('Сбой сети при отправке комментария', 'error');
    }
  }

  // Post actualization request
  async function submitActualization(e) {
    e.preventDefault();
    if (!actualizationReason.trim()) {
      showToast('Укажите причину для актуализации документа', 'error');
      return;
    }

    try {
      const res = await fetch(`/api/documents/${doc.id}/actualization-requests`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Role': selectedRole
        },
        body: JSON.stringify({ reason: actualizationReason.trim() })
      });

      if (res.ok) {
        actualizationReason = '';
        showActualizationForm = false;
        showToast('Запрос на актуализацию успешно отправлен ответственным менеджерам');
      } else {
        const err = await res.json().catch(() => ({}));
        showToast(err.message || 'Ошибка при отправке запроса', 'error');
      }
    } catch (err) {
      showToast('Сбой сети при отправке запроса', 'error');
    }
  }

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

  onMount(() => {
    loadComments();
  });
</script>

<div class="fixed inset-0 z-50 flex flex-col bg-[#F8FAFC] overflow-hidden animate-fade-in" aria-modal="true" role="dialog" aria-labelledby="doc-title">
  <!-- TopAppBar (Шапка просмотра документа) -->
  <header class="bg-white border-b border-[#E2E8F0] px-4 py-3 flex items-center justify-between h-14 sticky top-0 z-50">
    <button
      onclick={onClose}
      class="text-[#45464d] hover:bg-slate-100 rounded-full p-2 flex items-center justify-center transition-colors"
      aria-label="Назад к реестру"
    >
      <span class="material-symbols-outlined">arrow_back</span>
    </button>
    <span id="doc-title" class="font-bold text-base md:text-lg text-[#1A365D] text-center flex-1 truncate px-4">
      {doc ? doc.title : 'Просмотр документа'}
    </span>
    <button class="text-[#45464d] hover:bg-slate-100 rounded-full p-2 flex items-center justify-center transition-colors">
      <span class="material-symbols-outlined">more_vert</span>
    </button>
  </header>

  <!-- Тело документа и боковая панель/раздел комментариев -->
  <div class="flex-1 flex flex-col md:flex-row overflow-hidden relative">

    <!-- Основной контент документа (Прокручиваемый) -->
    <main class="flex-1 overflow-y-auto p-6 bg-white border-r border-[#E2E8F0]">
      {#if doc}
        <!-- Метаданные -->
        <div class="mb-6">
          <div class="flex flex-wrap items-center gap-2 mb-3">
            <span class="bg-[#E2E8F0] text-[#131b2e] px-2.5 py-1 rounded text-xs font-bold font-mono tracking-wider">
              {doc.status || 'ACTIVE'}
            </span>
            <span class="text-xs text-slate-500 font-sans">
              Версия {doc.version || '1.0'} • Обновлено {doc.updatedAt ? new Date(doc.updatedAt).toLocaleDateString('ru-RU') : 'недавно'}
            </span>
          </div>

          <h2 class="text-xl md:text-2xl font-bold text-[#1A365D] tracking-tight leading-tight mb-4">
            {doc.title}
          </h2>

          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 bg-[#F8FAFC] border border-[#E2E8F0] p-4 rounded-lg text-xs">
            <div>
              <span class="block text-slate-400 uppercase tracking-wider font-semibold mb-1">Шифр:</span>
              <span class="font-mono font-bold text-slate-700">{doc.documentNumber || 'Н/Д'}</span>
            </div>
            <div>
              <span class="block text-slate-400 uppercase tracking-wider font-semibold mb-1">Тип акта:</span>
              <span class="font-bold text-slate-700">{getDocTypeRu(doc.documentType)}</span>
            </div>
            <div>
              <span class="block text-slate-400 uppercase tracking-wider font-semibold mb-1">Программа:</span>
              <span class="font-bold text-slate-700">{getProgramRu(doc.program)}</span>
            </div>
            <div>
              <span class="block text-slate-400 uppercase tracking-wider font-semibold mb-1">Процесс:</span>
              <span class="font-bold text-slate-700">{getProcessRu(doc.process)}</span>
            </div>
          </div>
        </div>

        <!-- Тело документа (Текст регламента) -->
        <div class="text-slate-700 space-y-4 leading-relaxed text-sm md:text-base">
          <p class="font-medium text-slate-800 bg-blue-50 border-l-4 border-blue-500 p-4 rounded-r">
            <strong>Аннотация документа:</strong> <br/>
            {doc.description || 'Аннотация документа отсутствует в реестре.'}
          </p>

          <h3 class="text-base md:text-lg font-bold text-[#1A365D] mt-6 mb-2">1. Общие положения и регламентация</h3>
          <p>
            Настоящий документ устанавливает единые требования к организации процессов в рамках ЦНИИ Эпидемиологии. Он является обязательным для исполнения всеми задействованными сотрудниками, аспирантами и ординаторами соответствующих направлений.
          </p>
          <p>
            Любые несоответствия, выявленные в ходе текущего учебного или финансового периода, подлежат своевременному исправлению. Для предложения правок используйте специальный механизм отправки запросов на актуализацию, расположенный в нижней панели действий.
          </p>

          <h3 class="text-base md:text-lg font-bold text-[#1A365D] mt-6 mb-2">2. Порядок применения нормативных требований</h3>
          <p>
            Все изменения в ФГОС и сопутствующие приказы Минобрнауки РФ автоматически транслируются во внутреннюю документацию ЦНИИ. Корректировка лимитов финансирования, расчет учебной нагрузки и выплата поощрительных выплат осуществляются строго в соответствии с утвержденными алгоритмами.
          </p>
        </div>
      {:else}
        <div class="flex flex-col items-center justify-center py-20 text-slate-400">
          <span class="material-symbols-outlined text-4xl mb-2">error</span>
          <span>Документ не загружен</span>
        </div>
      {/if}
    </main>

    <!-- Слайдер / Панель комментариев (Desktop: справа, Mobile: снизу/поверх) -->
    <section
      class="w-full md:w-[400px] border-l border-[#E2E8F0] bg-[#F8FAFC] flex flex-col transition-all duration-300 {showComments ? 'h-[400px] md:h-auto' : 'h-0 md:w-0 overflow-hidden border-0'}"
      aria-label="Комментарии и обратная связь"
    >
      <div class="px-4 py-3 bg-white border-b border-[#E2E8F0] flex items-center justify-between">
        <h3 class="font-bold text-[#1A365D] text-sm md:text-base flex items-center gap-1.5">
          <span class="material-symbols-outlined text-lg text-blue-500">forum</span>
          Комментарии ({comments.length})
        </h3>
        <button
          onclick={() => showComments = false}
          class="text-slate-400 hover:text-slate-600 p-1 flex items-center justify-center"
          aria-label="Закрыть панель"
        >
          <span class="material-symbols-outlined text-lg">close</span>
        </button>
      </div>

      <!-- Список комментариев -->
      <div class="flex-1 overflow-y-auto p-4 space-y-3">
        {#if loadingComments}
          <div class="flex justify-center items-center py-10">
            <span class="material-symbols-outlined animate-spin text-slate-400 text-2xl">sync</span>
          </div>
        {:else if comments.length === 0}
          <div class="text-center py-10 text-slate-400 text-xs flex flex-col items-center gap-2">
            <span class="material-symbols-outlined text-2xl text-slate-300">chat_bubble_outline</span>
            <span>Комментариев пока нет. Напишите первый отзыв!</span>
          </div>
        {:else}
          {#each comments as comment}
            <div class="bg-white p-3 rounded-lg border border-[#E2E8F0] shadow-sm text-xs">
              <div class="flex justify-between items-center mb-1 text-slate-400">
                <span class="font-bold text-[#1A365D]">{comment.userName}</span>
                <span>{new Date(comment.createdAt).toLocaleDateString('ru-RU')}</span>
              </div>
              <p class="text-slate-700 leading-relaxed font-sans">{comment.text}</p>
            </div>
          {/each}
        {/if}
      </div>

      <!-- Область ввода нового комментария или запроса на актуализацию -->
      <div class="p-4 bg-white border-t border-[#E2E8F0]">
        {#if showCommentForm}
          <form onsubmit={submitComment} class="flex flex-col gap-2">
            <label for="comment-textarea" class="text-xs font-bold text-[#1A365D] mb-1">Ваш комментарий:</label>
            <textarea
              id="comment-textarea"
              bind:value={commentText}
              placeholder="Введите текст комментария..."
              class="w-full border border-slate-300 rounded p-2 text-xs focus:ring-1 focus:ring-blue-500 focus:outline-none min-h-[60px]"
            ></textarea>
            <div class="flex justify-end gap-2 mt-1">
              <button
                type="button"
                onclick={() => showCommentForm = false}
                class="px-3 py-1 border border-slate-300 rounded text-xs hover:bg-slate-100 transition-colors"
              >
                Отмена
              </button>
              <button
                type="submit"
                class="px-3 py-1 bg-slate-950 text-white rounded text-xs hover:bg-slate-800 transition-colors"
              >
                Отправить
              </button>
            </div>
          </form>
        {:else if showActualizationForm}
          <form onsubmit={submitActualization} class="flex flex-col gap-2">
            <label for="actualization-textarea" class="text-xs font-bold text-[#93000a] mb-1">Обоснование актуализации:</label>
            <textarea
              id="actualization-textarea"
              bind:value={actualizationReason}
              placeholder="Укажите, что именно устарело или требует исправления..."
              class="w-full border border-red-200 rounded p-2 text-xs focus:ring-1 focus:ring-red-500 focus:outline-none min-h-[60px]"
            ></textarea>
            <div class="flex justify-end gap-2 mt-1">
              <button
                type="button"
                onclick={() => showActualizationForm = false}
                class="px-3 py-1 border border-slate-300 rounded text-xs hover:bg-slate-100 transition-colors"
              >
                Отмена
              </button>
              <button
                type="submit"
                class="px-3 py-1 bg-red-600 text-white rounded text-xs hover:bg-red-700 transition-colors"
              >
                Отправить запрос
              </button>
            </div>
          </form>
        {:else}
          <div class="flex gap-2">
            <button
              onclick={() => { showCommentForm = true; showActualizationForm = false; }}
              class="flex-1 py-2 bg-slate-900 text-white rounded text-xs font-bold hover:bg-slate-800 transition-colors flex items-center justify-center gap-1"
            >
              <span class="material-symbols-outlined text-sm">add_comment</span>
              Комментировать
            </button>
            <button
              onclick={() => { showActualizationForm = true; showCommentForm = false; }}
              class="flex-1 py-2 border border-red-600 text-red-600 rounded text-xs font-bold hover:bg-red-50 transition-colors flex items-center justify-center gap-1"
            >
              <span class="material-symbols-outlined text-sm">edit_note</span>
              Актуализировать
            </button>
          </div>
        {/if}
      </div>
    </section>
  </div>

  <!-- Уведомления Toasts -->
  {#if toastMessage}
    <div class="fixed bottom-20 left-1/2 transform -translate-x-1/2 z-50 px-4 py-3 rounded-lg shadow-lg border text-sm font-semibold max-w-sm text-center flex items-center gap-2 transition-all duration-300 {toastType === 'error' ? 'bg-[#ffdad6] text-[#93000a] border-[#ba1a1a]' : 'bg-green-50 text-green-800 border-green-500'}">
      <span class="material-symbols-outlined text-lg">
        {toastType === 'error' ? 'error' : 'check_circle'}
      </span>
      <span>{toastMessage}</span>
    </div>
  {/if}

  <!-- Нижняя панель действий (Фиксированная) -->
  <footer class="bg-slate-50 border-t border-[#E2E8F0] p-4 flex justify-between items-center gap-4 sticky bottom-0 z-40 h-16">
    <!-- Кнопка "Посмотреть комментарии" -->
    <button
      onclick={() => { showComments = !showComments; loadComments(); }}
      class="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded-lg text-[#1A365D] text-xs font-bold hover:bg-slate-100 transition-colors"
    >
      <span class="material-symbols-outlined text-lg">forum</span>
      <span>Комментарии ({comments.length})</span>
    </button>

    <!-- Основные действия -->
    <div class="flex gap-2">
      <button
        onclick={() => { showComments = true; showActualizationForm = true; showCommentForm = false; }}
        class="flex items-center justify-center gap-1 px-4 py-2 border border-red-600 text-red-600 rounded-lg text-xs font-bold hover:bg-red-50 transition-colors"
      >
        <span class="material-symbols-outlined text-lg">edit_note</span>
        <span>Запросить актуализацию</span>
      </button>

      <button
        onclick={() => { showComments = true; showCommentForm = true; showActualizationForm = false; }}
        class="flex items-center justify-center gap-1 px-4 py-2 bg-[#1A365D] text-white rounded-lg text-xs font-bold hover:bg-blue-800 transition-colors"
      >
        <span class="material-symbols-outlined text-lg">add_comment</span>
        <span>Добавить комментарий</span>
      </button>
    </div>
  </footer>
</div>

<style>
  .animate-fade-in {
    animation: fadeIn 0.2s ease-out;
  }
  @keyframes fadeIn {
    from { opacity: 0; transform: scale(0.98); }
    to { opacity: 1; transform: scale(1); }
  }
</style>
