<script>
  import KnowledgeBase from './KnowledgeBase.svelte';
  import FinancialModule from './FinancialModule.svelte';

  // Toggle active module
  let activeTab = $state('knowledge'); // 'knowledge' or 'financial'
</script>

<style>
  /* Ensure full width and reset any default margins */
  :global(#app) {
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 !important;
    border: none !important;
    text-align: left !important;
    min-height: 100vh !important;
    display: flex !important;
    flex-direction: column !important;
  }
</style>

<div class="flex flex-col min-h-screen bg-[#F9F9FF]">

  <!-- Global Module Switcher Header: Styled strictly in Russian -->
  <div class="bg-[#1A365D] text-white py-2.5 px-6 flex flex-col sm:flex-row justify-between items-center gap-2 text-xs font-semibold z-[100] border-b border-[#3182CE]/30">
    <div class="flex items-center gap-2">
      <span class="material-symbols-outlined text-sm text-[#3182CE]">verified_user</span>
      <span class="tracking-wide">ЦНИИ Эпидемиологии Министерства здравоохранения РФ</span>
    </div>
    <div class="flex gap-6">
      <button
        type="button"
        onclick={() => activeTab = 'knowledge'}
        class="transition-all duration-150 flex items-center gap-1.5 hover:text-[#3182CE] {activeTab === 'knowledge' ? 'text-[#3182CE] font-bold' : 'text-gray-300'}"
      >
        <span class="material-symbols-outlined text-sm">menu_book</span>
        <span>База знаний (Лексикон Флакс)</span>
      </button>
      <button
        type="button"
        onclick={() => activeTab = 'financial'}
        class="transition-all duration-150 flex items-center gap-1.5 hover:text-[#3182CE] {activeTab === 'financial' ? 'text-[#3182CE] font-bold' : 'text-gray-300'}"
      >
        <span class="material-symbols-outlined text-sm">payments</span>
        <span>Финансовый модуль</span>
      </button>
    </div>
  </div>

  <!-- Route view -->
  <div class="flex-grow flex flex-col">
    {#if activeTab === 'knowledge'}
      <KnowledgeBase />
    {:else}
      <FinancialModule />
    {/if}
  </div>

</div>
