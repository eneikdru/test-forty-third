<script>
  import FinancialModule from './FinancialModule.svelte';
  import KnowledgeBaseModule from './KnowledgeBaseModule.svelte';

  // Svelte 5 state runes
  let activeTab = $state('knowledge_base'); // 'financial' или 'knowledge_base'
</script>

<style>
  /* Header wrapper styling following Russian and modernist minimalist guidelines */
  .switch-header {
    font-family: 'Inter', sans-serif;
    background-color: #1A365D; /* Deep dark blue */
  }
</style>

<div class="min-h-screen flex flex-col">
  <!-- Top Switch Navigation Bar (strict Russian) -->
  <div class="switch-header text-white border-b border-[#1A365D]/30 py-3 px-5">
    <div class="max-w-[1200px] mx-auto flex flex-col sm:flex-row justify-between items-center gap-3">
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-[#3182CE]">lan</span>
        <span class="text-xs font-bold uppercase tracking-wider">ЦНИИ Эпидемиологии · Единый портал</span>
      </div>

      <!-- Navigation buttons in strict Russian -->
      <div class="flex gap-2">
        <button
          type="button"
          onclick={() => activeTab = 'knowledge_base'}
          class="px-4 py-2 rounded text-xs font-bold uppercase tracking-wider transition-colors
                 {activeTab === 'knowledge_base'
                  ? 'bg-[#3182CE] text-white'
                  : 'bg-transparent text-white/80 hover:bg-white/10'}"
        >
          База знаний
        </button>

        <button
          type="button"
          onclick={() => activeTab = 'financial'}
          class="px-4 py-2 rounded text-xs font-bold uppercase tracking-wider transition-colors
                 {activeTab === 'financial'
                  ? 'bg-[#3182CE] text-white'
                  : 'bg-transparent text-white/80 hover:bg-white/10'}"
        >
          Финансовый кабинет
        </button>
      </div>
    </div>
  </div>

  <!-- Dynamic view container -->
  <div class="flex-1 flex flex-col">
    {#if activeTab === 'knowledge_base'}
      <KnowledgeBaseModule />
    {:else}
      <FinancialModule />
    {/if}
  </div>
</div>
