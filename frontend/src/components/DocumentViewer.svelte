<script>
  import { onMount } from 'svelte';

  // Props in Svelte 5
  let { selectedDocument, selectedRole = 'Economist', onBack } = $props();

  // Component state
  let comments = $state([]);
  let showCommentsList = $state(true); // default true to display them
  let loadingComments = $state(false);

  // Modal states
  let showCommentModal = $state(false);
  let showRequestModal = $state(false);

  // Form states
  let commentText = $state('');
  let requestReason = $state('');
  let commentError = $state('');
  let requestError = $state('');

  // Toast notifications
  let toastMessage = $state('');
  let showToast = $state(false);

  function triggerToast(message) {
    toastMessage = message;
    showToast = true;
    setTimeout(() => {
      showToast = false;
    }, 3000);
  }

  // Translation helpers
  function getDocTypeRu(type) {
    const map = {
      Position: 'Положение',
      Procedure: 'Порядок',
      Project: 'Проект',
      Other: 'Иной документ'
    };
    return map[type] || 'Документ';
  }

  // Check if role is authorized to edit
  let canEdit = $derived(
    selectedRole === 'Admin' || selectedRole === 'Economist'
  );

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
      other: 'Иной процесс'
    };
    return map[proc] || 'Иное';
  }

  function getStatusRu(status) {
    const map = {
      ACTIVE: 'АКТУАЛЬНО',
      PROJECT: 'ПРОЕКТ',
      ARCHIVED: 'АРХИВ'
    };
    return map[status] || 'АКТУАЛЬНО';
  }

  // Fetch comments
  async function loadComments() {
    loadingComments = true;
    try {
      const res = await fetch(`/api/documents/${selectedDocument.id}/comments`, {
        headers: {
          'X-User-Role': selectedRole
        }
      });
      if (res.ok) {
        comments = await res.json();
      } else {
        // Fallback to local comments
        const localData = localStorage.getItem(`comments_${selectedDocument.id}`);
        if (localData) {
          comments = JSON.parse(localData);
        } else {
          comments = [
            {
              id: 'c-1',
              userName: 'Преподаватель',
              text: 'Пожалуйста, проверьте соответствие ФГОС последней редакции.',
              createdAt: '2026-09-18T10:30:00Z'
            },
            {
              id: 'c-2',
              userName: 'Экономист',
              text: 'Все финансовые расчеты в приложении 2 проверены.',
              createdAt: '2026-09-19T08:15:00Z'
            }
          ];
          localStorage.setItem(`comments_${selectedDocument.id}`, JSON.stringify(comments));
        }
      }
    } catch (e) {
      console.error('Ошибка загрузки комментариев', e);
    } finally {
      loadingComments = false;
    }
  }

  // Submit comment
  async function submitComment() {
    if (!commentText.trim()) {
      commentError = 'Текст комментария не может быть пустым.';
      return;
    }
    commentError = '';

    try {
      const res = await fetch(`/api/documents/${selectedDocument.id}/comments`, {
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
        // Also save to localStorage fallback
        localStorage.setItem(`comments_${selectedDocument.id}`, JSON.stringify(comments));
      } else {
        // Mock fallback
        const newComment = {
          id: Math.random().toString(),
          userName: selectedRole === 'Economist' ? 'Экономист' : selectedRole === 'Teacher' ? 'Преподаватель' : 'Пользователь',
          text: commentText.trim(),
          createdAt: new Date().toISOString()
        };
        comments = [...comments, newComment];
        localStorage.setItem(`comments_${selectedDocument.id}`, JSON.stringify(comments));
      }

      commentText = '';
      showCommentModal = false;
      triggerToast('Комментарий успешно добавлен!');
    } catch (e) {
      console.error(e);
      triggerToast('Сбой сети при отправке комментария.');
    }
  }

  // Submit actualization request
  async function submitActualizationRequest() {
    if (!requestReason.trim()) {
      requestError = 'Пожалуйста, укажите причину запроса.';
      return;
    }
    requestError = '';

    try {
      const res = await fetch(`/api/documents/${selectedDocument.id}/actualization-requests`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Role': selectedRole
        },
        body: JSON.stringify({ reason: requestReason.trim() })
      });

      if (res.ok) {
        showRequestModal = false;
        requestReason = '';
        triggerToast('Запрос на актуализацию успешно отправлен!');
      } else {
        // Mock success
        showRequestModal = false;
        requestReason = '';
        triggerToast('Запрос на актуализацию отправлен (режим эмуляции).');
      }
    } catch (e) {
      console.error(e);
      triggerToast('Сбой сети при отправке запроса.');
    }
  }

  onMount(() => {
    loadComments();
  });
</script>

<div class="flex flex-col bg-[#F9F9FF] min-h-screen text-[#0b1c30] w-full max-w-[1200px] mx-auto border-x border-[#E2E8F0]">

  <!-- Верхняя панель (Header) -->
  <header class="bg-white border-b border-[#E2E8F0] h-14 w-full sticky top-0 z-30 flex items-center justify-between px-4">
    <button
      onclick={onBack}
      class="text-[#1A365D] hover:bg-slate-100 rounded-full p-2 flex items-center justify-center transition-colors"
      aria-label="Назад к списку документов"
    >
      <span class="material-symbols-outlined">arrow_back</span>
    </button>
    <span class="font-sans font-bold text-sm md:text-base text-[#1A365D] text-center flex-1 truncate px-4">
      {selectedDocument.title}
    </span>
    <div class="w-10"></div> <!-- spacing balance -->
  </header>

  <!-- Canvas с контентом документа -->
  <main class="flex-1 overflow-y-auto px-4 md:px-8 py-6 bg-white">
    <!-- Метаданные документа -->
    <div class="mb-6 pb-6 border-b border-[#E2E8F0]">
      <div class="flex flex-wrap items-center gap-2 mb-3">
        <span class="bg-[#3182CE] text-white px-2.5 py-0.5 rounded-[0.25rem] font-mono text-[10px] font-bold">
          {getStatusRu(selectedDocument.status)}
        </span>
        <span class="text-slate-400 text-xs font-sans">
          Шифр: <strong class="text-slate-600 font-mono">{selectedDocument.documentNumber || 'Н/Д'}</strong>
        </span>
        <span class="text-slate-400 text-xs font-sans">•</span>
        <span class="text-slate-400 text-xs font-sans">
          Версия: <strong class="text-slate-600 font-mono">{selectedDocument.version || '1.0'}</strong>
        </span>
      </div>

      <h1 class="font-sans font-bold text-xl md:text-2xl text-[#1A365D] leading-tight mb-4">
        {selectedDocument.title}
      </h1>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs text-[#515f74] bg-[#F9F9FF] p-3 rounded-[0.25rem] border border-[#E2E8F0]">
        <div><strong>Тип документа:</strong> {getDocTypeRu(selectedDocument.documentType)}</div>
        <div><strong>Программа:</strong> {getProgramRu(selectedDocument.program)}</div>
        <div><strong>Процесс / Тема:</strong> {getProcessRu(selectedDocument.process)}</div>
        <div><strong>Период:</strong> {selectedDocument.academicYear || 'бессрочно'}</div>
      </div>
    </div>

    <!-- Аннотация / Описание -->
    <div class="mb-6 p-4 bg-slate-50 border-l-4 border-[#3182CE] rounded-r-[0.25rem]">
      <h3 class="text-xs font-bold text-[#1A365D] uppercase tracking-wider mb-1 font-sans">Краткое описание</h3>
      <p class="text-sm text-slate-600 leading-relaxed font-sans">
        {selectedDocument.description || 'Аннотация документа отсутствует.'}
      </p>
    </div>

    <!-- Текстовое тело документа (Эмуляция) -->
    <div class="space-y-4 text-sm text-slate-700 leading-relaxed font-sans border-b border-[#E2E8F0] pb-8 mb-6">
      <h2 class="text-base font-bold text-[#1A365D] pt-2 mb-2">1. Общие положения регламента</h2>
      <p>
        Настоящий нормативный акт ЦНИИ Эпидемиологии разработан в целях систематизации учебного процесса,
        распределения учебной и научной нагрузки преподавательского состава, а также регламентации выплат
        и поощрений аспирантов и ординаторов.
      </p>
      <p>
        Регламент является обязательным к исполнению всеми структурными подразделениями ОЦ, включая
        кафедру эпидемиологии, инфекционных болезней и смежных дисциплин. Все изменения вносятся исключительно
        при согласовании с администрацией и планово-экономическим отделом института.
      </p>

      <h2 class="text-base font-bold text-[#1A365D] pt-4 mb-2">2. Ключевые требования и стандарты</h2>
      <p>
        В рамках обеспечения внутренней системы оценки качества образования (ВСОКО) проводятся регулярные
        контрольные замеры успеваемости и мониторинг удовлетворенности качеством преподавания.
        Все учебно-методические комплексы должны быть приведены в полное соответствие с действующими ФГОС ВО.
      </p>
      <ul class="list-disc pl-5 space-y-1.5 mt-2 text-slate-600">
        <li>Своевременная фиксация академической задолженности.</li>
        <li>Проверка портфолио достижений обучающихся не менее двух раз в семестр.</li>
        <li>Автоматизированный учёт учебной нагрузки в системе LMS ЦНИИ.</li>
      </ul>
    </div>

    <!-- Секция комментариев -->
    {#if showCommentsList}
      <section class="mt-6 mb-12">
        <div class="flex items-center justify-between border-b border-[#E2E8F0] pb-2 mb-4">
          <h3 class="text-base font-bold text-[#1A365D] font-sans flex items-center gap-2">
            <span class="material-symbols-outlined text-slate-500">forum</span>
            Комментарии и отзывы ({comments.length})
          </h3>
        </div>

        {#if loadingComments}
          <div class="text-center py-4 text-slate-400 text-xs">Загрузка комментариев...</div>
        {:else if comments.length === 0}
          <div class="text-center py-6 bg-slate-50 border border-dashed border-[#E2E8F0] rounded-[0.25rem] text-slate-400 text-xs">
            Комментариев пока нет. Напишите первый отзыв!
          </div>
        {:else}
          <div class="space-y-3">
            {#each comments as c}
              <div class="bg-[#F9F9FF] border border-[#E2E8F0] rounded-[0.25rem] p-3.5 flex flex-col gap-1 shadow-sm">
                <div class="flex items-center justify-between">
                  <span class="text-xs font-bold text-[#1A365D] font-sans">
                    {c.userName === 'Economist' ? 'Экономист' : c.userName === 'Teacher' ? 'Преподаватель' : c.userName}
                  </span>
                  <span class="text-[10px] text-slate-400 font-mono">
                    {new Date(c.createdAt).toLocaleDateString('ru-RU')} {new Date(c.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
                <p class="text-xs text-slate-600 leading-normal font-sans break-words whitespace-pre-wrap">{c.text}</p>
              </div>
            {/each}
          </div>
        {/if}
      </section>
    {/if}
  </main>

  <!-- Нижняя панель действий (Bottom Action Bar) -->
  <div class="bg-white/95 backdrop-blur border-t border-[#E2E8F0] p-4 flex flex-col sm:flex-row justify-between items-center gap-3 sticky bottom-0 z-20">
    <button
      onclick={() => showCommentsList = !showCommentsList}
      class="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2 border border-[#E2E8F0] hover:bg-slate-50 rounded-[0.25rem] text-[#1A365D] text-xs font-bold font-sans transition-colors"
    >
      <span class="material-symbols-outlined text-[18px]">forum</span>
      <span>{showCommentsList ? 'Скрыть комментарии' : 'Показать комментарии'} ({comments.length})</span>
    </button>

    <div class="flex w-full sm:w-auto gap-2">
      <!-- Edit button triggered here -->
      {#if canEdit}
        <button
          onclick={() => {
            window.dispatchEvent(new CustomEvent('edit-document', { detail: selectedDocument }));
            onBack();
          }}
          class="flex-1 sm:flex-initial flex items-center justify-center gap-1.5 px-4 py-2 bg-[#1A365D] hover:bg-slate-800 text-white rounded-[0.25rem] text-xs font-bold font-sans transition-colors shadow-sm"
        >
          <span class="material-symbols-outlined text-[18px]">edit</span>
          <span>Редактировать</span>
        </button>
      {/if}

      <button
        onclick={() => showRequestModal = true}
        class="flex-1 sm:flex-initial flex items-center justify-center gap-1.5 px-4 py-2 border border-[#3182CE] hover:bg-blue-50 text-[#3182CE] rounded-[0.25rem] text-xs font-bold font-sans transition-colors"
      >
        <span class="material-symbols-outlined text-[18px]">edit_note</span>
        <span>Запросить обновление</span>
      </button>

      <button
        onclick={() => showCommentModal = true}
        class="flex-1 sm:flex-initial flex items-center justify-center gap-1.5 px-4 py-2 bg-[#3182CE] hover:bg-blue-600 text-white rounded-[0.25rem] text-xs font-bold font-sans transition-colors shadow-sm"
      >
        <span class="material-symbols-outlined text-[18px]">add_comment</span>
        <span>Добавить комментарий</span>
      </button>
    </div>
  </div>

  <!-- Модальное окно: Добавить комментарий -->
  {#if showCommentModal}
    <div class="fixed inset-0 bg-[#0b1c30]/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-lg border border-[#E2E8F0] shadow-xl w-full max-w-md overflow-hidden flex flex-col">
        <div class="p-4 border-b border-[#E2E8F0] bg-[#F9F9FF] flex justify-between items-center">
          <h3 class="font-sans font-bold text-sm text-[#1A365D]">Добавить комментарий</h3>
          <button onclick={() => showCommentModal = false} class="text-slate-400 hover:text-slate-600" aria-label="Закрыть">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-4 space-y-3">
          <label for="comment-text" class="text-xs font-bold text-[#515f74] block">ВАШ ОТЗЫВ ИЛИ ВОПРОС</label>
          <textarea
            id="comment-text"
            bind:value={commentText}
            rows="4"
            placeholder="Напишите ваш комментарий здесь..."
            class="w-full border border-slate-300 rounded-[0.25rem] p-2.5 text-sm focus:outline-none focus:border-[#3182CE] focus:ring-1 focus:ring-[#3182CE] font-sans"
          ></textarea>
          {#if commentError}
            <p class="text-red-500 text-xs">{commentError}</p>
          {/if}
        </div>
        <div class="p-4 border-t border-[#E2E8F0] bg-[#F9F9FF] flex justify-end gap-2">
          <button
            onclick={() => showCommentModal = false}
            class="px-4 py-2 border border-slate-300 rounded-[0.25rem] text-slate-600 text-xs font-bold hover:bg-slate-100 transition-colors"
          >
            Отмена
          </button>
          <button
            onclick={submitComment}
            class="px-4 py-2 bg-[#3182CE] hover:bg-blue-600 text-white rounded-[0.25rem] text-xs font-bold transition-colors shadow-sm"
          >
            Опубликовать
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Модальное окно: Запросить актуализацию -->
  {#if showRequestModal}
    <div class="fixed inset-0 bg-[#0b1c30]/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-lg border border-[#E2E8F0] shadow-xl w-full max-w-md overflow-hidden flex flex-col">
        <div class="p-4 border-b border-[#E2E8F0] bg-[#F9F9FF] flex justify-between items-center">
          <h3 class="font-sans font-bold text-sm text-[#1A365D]">Запросить актуализацию</h3>
          <button onclick={() => showRequestModal = false} class="text-slate-400 hover:text-slate-600" aria-label="Закрыть">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-4 space-y-3">
          <label for="request-reason" class="text-xs font-bold text-[#515f74] block">ОБОСНОВАНИЕ НЕОБХОДИМОСТИ ОБНОВЛЕНИЯ</label>
          <textarea
            id="request-reason"
            bind:value={requestReason}
            rows="4"
            placeholder="Опишите причину необходимости актуализации (например, изменения в законодательстве, новые ФГОС)..."
            class="w-full border border-slate-300 rounded-[0.25rem] p-2.5 text-sm focus:outline-none focus:border-[#3182CE] focus:ring-1 focus:ring-[#3182CE] font-sans"
          ></textarea>
          {#if requestError}
            <p class="text-red-500 text-xs">{requestError}</p>
          {/if}
        </div>
        <div class="p-4 border-t border-[#E2E8F0] bg-[#F9F9FF] flex justify-end gap-2">
          <button
            onclick={() => showRequestModal = false}
            class="px-4 py-2 border border-slate-300 rounded-[0.25rem] text-slate-600 text-xs font-bold hover:bg-slate-100 transition-colors"
          >
            Отмена
          </button>
          <button
            onclick={submitActualizationRequest}
            class="px-4 py-2 bg-[#3182CE] hover:bg-blue-600 text-white rounded-[0.25rem] text-xs font-bold transition-colors shadow-sm"
          >
            Отправить запрос
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Toast всплывающее уведомление -->
  {#if showToast}
    <div class="fixed bottom-20 left-1/2 transform -translate-x-1/2 z-50 bg-[#1A365D] text-white px-5 py-3 rounded-md shadow-lg border border-[#3182CE] flex items-center gap-2 font-sans text-xs animate-bounce">
      <span class="material-symbols-outlined text-[#3182CE] text-sm">check_circle</span>
      <span>{toastMessage}</span>
    </div>
  {/if}

</div>
