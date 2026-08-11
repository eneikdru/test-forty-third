<script>
  let {
    searchQuery = $bindable(),
    suggestionsList = [],
    typoCorrection = null,
    activeSuggestionIndex = $bindable(-1),
    showSuggestions = $bindable(false),
    onSaveSearch,
    onClearSearch,
    onSelectSuggestion,
    onKeyDown
  } = $props();

  let inputRef = $state();
</script>

<div class="relative z-20 w-full mt-4">
  <div class="relative bg-surface-container-lowest rounded-xl border border-secondary-container shadow-[0_4px_6px_-1px_rgba(15,23,42,0.05),0_2px_4px_-2px_rgba(15,23,42,0.05)] overflow-visible">
    <div class="flex items-center px-[12px] h-[48px]">
      <span class="material-symbols-outlined text-outline mr-[8px]">search</span>
      <input
        bind:this={inputRef}
        type="text"
        bind:value={searchQuery}
        onfocus={() => showSuggestions = true}
        onblur={() => setTimeout(() => showSuggestions = false, 200)}
        onkeydown={onKeyDown}
        placeholder="Поиск материалов..."
        class="w-full bg-transparent border-none focus:ring-0 focus:outline-none text-on-surface font-sans text-base font-normal p-0 m-0 h-full"
      />
      {#if searchQuery}
        <button
          type="button"
          onclick={onClearSearch}
          class="text-on-surface-variant hover:text-primary p-1 flex items-center justify-center mr-1"
          aria-label="Очистить поиск"
        >
          <span class="material-symbols-outlined text-lg">close</span>
        </button>
        <button
          type="button"
          onclick={onSaveSearch}
          class="bg-primary text-white hover:bg-primary px-3 py-1 rounded-[6px] text-xs font-semibold mr-1 transition-colors"
          title="Сохранить поисковый запрос"
        >
          Сохранить запрос
        </button>
      {/if}
      <span class="text-outline-variant font-sans text-xs tracking-[0.05em] font-semibold ml-[8px] hidden md:block uppercase">⌘K</span>
    </div>

    <!-- Dropdown Suggestions -->
    {#if showSuggestions && suggestionsList.length > 0}
      <div class="absolute top-full left-0 right-0 mt-[4px] bg-surface-container-lowest rounded-xl border border-outline-variant shadow-[0_10px_15px_-3px_rgba(15,23,42,0.1),0_4px_6px_-2px_rgba(15,23,42,0.05)] overflow-hidden z-30">
        <ul class="py-[8px]">
          {#each suggestionsList as suggestion, idx}
            <li>
              <button
                type="button"
                onclick={() => onSelectSuggestion(suggestion)}
                class="w-full px-[16px] py-[8px] hover:bg-surface-container-low cursor-pointer flex items-center gap-[16px] {idx === activeSuggestionIndex ? 'bg-surface-container-high' : ''}"
              >
                <span class="material-symbols-outlined text-outline-variant">history</span>
                <span class="font-sans text-sm font-normal text-on-surface text-left">
                  {suggestion}
                </span>
              </button>
            </li>
          {/each}
        </ul>
      </div>
    {/if}
  </div>
</div>

<!-- Typo Correction -->
{#if typoCorrection}
  <div class="mt-2 bg-error-container border border-error rounded-lg p-3 text-sm text-on-error-container flex items-center gap-2 font-sans">
    <span class="material-symbols-outlined text-error">info</span>
    <span>Возможно, вы имели в виду:</span>
    <button
      type="button"
      onclick={() => onSelectSuggestion(typoCorrection)}
      class="font-bold underline text-primary hover:text-primary text-left"
    >
      {typoCorrection}
    </button>
  </div>
{/if}
