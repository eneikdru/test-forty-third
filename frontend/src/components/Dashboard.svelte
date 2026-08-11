<script>
  import { onMount } from 'svelte';

  // State runes for interactive actions
  let alertMessage = $state('');
  let reportCount = $state(0);
  let timeoutId = $state(null);

  function triggerAction(actionName) {
    alertMessage = `Действие "${actionName}" успешно выполнено!`;
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
    timeoutId = setTimeout(() => {
      alertMessage = '';
    }, 4000);
  }

  function handleNewReport() {
    reportCount += 1;
    triggerAction(`Новый отчет #${reportCount}`);
  }

  function handleManageAccess() {
    triggerAction('Управление доступом');
  }

  function handleTriggerSync() {
    triggerAction('Синхронизация');
  }
</script>

{#if alertMessage}
<div class="fixed top-4 left-1/2 transform -translate-x-1/2 z-50 bg-secondary-container text-on-secondary-container p-sm rounded-xl border border-outline-variant flex items-center gap-sm transition-all animate-pulse">
  <span class="material-symbols-outlined">info</span>
  <span class="font-semibold text-sm">{alertMessage}</span>
</div>
{/if}

<main class="max-w-[1200px] mx-auto px-container-margin py-lg grid grid-cols-4 md:grid-cols-12 gap-gutter">
<!-- Welcome Section (Full Width) -->
<section class="col-span-4 md:col-span-12 mb-lg">
<h2 class="font-headline-lg-mobile md:font-headline-lg text-headline-lg-mobile md:text-headline-lg mb-sm">С возвращением, Алекс.</h2>
<p class="font-body-md text-body-md text-on-surface-variant">Вот ваш ежедневный обзор.</p>
</section>
<!-- Bento Grid Layout -->
<!-- Large Metrics Card (Spans 4 cols on mobile, 8 on desktop) -->
<div class="col-span-4 md:col-span-8 bg-surface-container-high border border-outline-variant rounded-xl p-md flex flex-col justify-between min-h-[300px]">
<div class="flex justify-between items-start mb-lg">
<div>
<h3 class="font-headline-md text-headline-md mb-xs">Производительность системы</h3>
<p class="font-body-sm text-body-sm text-on-surface-variant">Агрегированная задержка и пропускная способность.</p>
</div>
<div class="bg-tertiary-container/10 text-tertiary px-sm py-xs rounded-DEFAULT font-label-sm text-label-sm border border-tertiary/20 flex items-center gap-xs">
<span class="material-symbols-outlined text-sm">check_circle</span>
                    Оптимально
                </div>
</div>
<div class="flex-grow w-full rounded-lg overflow-hidden relative">
<!-- Abstract Data Visualization Placeholder -->
<div class="bg-cover bg-center w-full h-full absolute inset-0 opacity-60" data-alt="A highly abstract, dark-themed data visualization chart featuring glowing electric blue and cyan lines charting a complex path across a dark navy grid. The style is modern, cinematic, and sleek, resembling a high-end corporate dashboard rendering with subtle depth of field and soft glowing particles." style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuC2WlqXv54TGDiIDRM4tW0RcqwjDCRysTFHn44i3gK4NNuXE3xxRptSF9lJH0aIgzpSdhO_jXOO4AksK6f9P4hfYwCwK-uEhdjZzQ_B5Rt9ZDI42MxeIUlUot_uU3mNy3gPYVYBPqpVQv0UCwmz13LTJxxAcetQ1IL2zBgaWZASgWQSEeOeNx4_jCatlSVIFFPrOFD6gqE9dPPfYOOiks9lDdCMo3G5djx-utHaTBNpNc8UX9RwMyLqDfxkrcsfsZYugtat8gw7XCk')"></div>
<div class="absolute bottom-md left-md right-md flex justify-between">
<div>
<p class="font-label-md text-label-md text-on-surface-variant mb-xs">Ср. задержка</p>
<p class="font-headline-md text-headline-md text-primary-fixed">24ms</p>
</div>
<div class="text-right">
<p class="font-label-md text-label-md text-on-surface-variant mb-xs">Запросов/сек</p>
<p class="font-headline-md text-headline-md text-primary-fixed">14.2k</p>
</div>
</div>
</div>
</div>
<!-- Quick Actions (Spans 4 cols on mobile, 4 on desktop) -->
<div class="col-span-4 md:col-span-4 bg-surface-container-high border border-outline-variant rounded-xl p-md flex flex-col gap-md min-h-[300px]">
<h3 class="font-headline-md text-headline-md">Быстрые действия</h3>
<button onclick={handleNewReport} aria-label="Новый отчет" class="w-full flex items-center justify-between p-sm rounded-lg hover:bg-surface-variant transition-colors border border-transparent hover:border-outline-variant group">
<div class="flex items-center gap-sm">
<div class="w-10 h-10 rounded-full bg-primary-container/20 flex items-center justify-center text-primary-fixed group-hover:scale-105 transition-transform">
<span class="material-symbols-outlined">add_chart</span>
</div>
<span class="font-body-md text-body-md">Новый отчет</span>
</div>
<span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
</button>
<button onclick={handleManageAccess} aria-label="Управление доступом" class="w-full flex items-center justify-between p-sm rounded-lg hover:bg-surface-variant transition-colors border border-transparent hover:border-outline-variant group">
<div class="flex items-center gap-sm">
<div class="w-10 h-10 rounded-full bg-secondary-container/30 flex items-center justify-center text-secondary-fixed group-hover:scale-105 transition-transform">
<span class="material-symbols-outlined">manage_accounts</span>
</div>
<span class="font-body-md text-body-md">Управление доступом</span>
</div>
<span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
</button>
<button onclick={handleTriggerSync} aria-label="Запустить синхронизацию" class="w-full flex items-center justify-between p-sm rounded-lg hover:bg-surface-variant transition-colors border border-transparent hover:border-outline-variant group">
<div class="flex items-center gap-sm">
<div class="w-10 h-10 rounded-full bg-tertiary-container/20 flex items-center justify-center text-tertiary group-hover:scale-105 transition-transform">
<span class="material-symbols-outlined">sync</span>
</div>
<span class="font-body-md text-body-md">Запустить синхронизацию</span>
</div>
<span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
</button>
</div>
<!-- Status List (Full width below) -->
<div class="col-span-4 md:col-span-12 bg-surface-container-high border border-outline-variant rounded-xl p-md mt-sm">
<div class="flex justify-between items-center mb-md border-b border-surface-variant pb-sm">
<h3 class="font-headline-md text-headline-md">Статус модулей</h3>
<a class="font-label-md text-label-md text-primary hover:text-primary-fixed-dim transition-colors" href="javascript:void(0);">Смотреть все</a>
</div>
<!-- Divideless List -->
<div class="flex flex-col gap-sm">
<div class="flex items-center justify-between p-sm hover:bg-surface-variant/50 rounded-lg transition-colors">
<div class="flex items-center gap-md">
<span class="material-symbols-outlined text-on-surface-variant">database</span>
<div>
<p class="font-body-md text-body-md">Основная база данных</p>
<p class="font-label-sm text-label-sm text-on-surface-variant">Последний бэкап: 2ч назад</p>
</div>
</div>
<div class="bg-tertiary-container/10 text-tertiary px-xs py-[2px] rounded-DEFAULT font-label-sm text-label-sm border border-tertiary/20">В сети</div>
</div>
<div class="flex items-center justify-between p-sm hover:bg-surface-variant/50 rounded-lg transition-colors">
<div class="flex items-center gap-md">
<span class="material-symbols-outlined text-on-surface-variant">api</span>
<div>
<p class="font-body-md text-body-md">Внешний шлюз API</p>
<p class="font-label-sm text-label-sm text-on-surface-variant">Обнаружен высокий объем</p>
</div>
</div>
<div class="bg-primary-container/10 text-primary-fixed-dim px-xs py-[2px] rounded-DEFAULT font-label-sm text-label-sm border border-primary/20">Нагружен</div>
</div>
<div class="flex items-center justify-between p-sm hover:bg-surface-variant/50 rounded-lg transition-colors">
<div class="flex items-center gap-md">
<span class="material-symbols-outlined text-on-surface-variant">shield</span>
<div>
<p class="font-body-md text-body-md">Служба безопасности</p>
<p class="font-label-sm text-label-sm text-on-surface-variant">Определения обновлены</p>
</div>
</div>
<div class="bg-tertiary-container/10 text-tertiary px-xs py-[2px] rounded-DEFAULT font-label-sm text-label-sm border border-tertiary/20">В сети</div>
</div>
</div>
</div>
</main>