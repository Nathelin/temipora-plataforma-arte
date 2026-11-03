<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  titulo: { type: String, required: true },
  descripcion: { type: String, default: '' },
})
</script>

<template>
  <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
    <div class="min-w-0">
      <p
        v-if="eyebrow"
        class="text-[11px] font-semibold uppercase tracking-[0.16em] text-coral"
      >
        {{ eyebrow }}
      </p>
      <h1 class="mt-1 font-display text-2xl font-extrabold tracking-[-0.02em] text-granate sm:text-3xl">
        {{ titulo }}
      </h1>
      <p v-if="descripcion" class="mt-2 max-w-2xl text-sm leading-relaxed text-cafe/65">
        {{ descripcion }}
      </p>
    </div>

    <div v-if="$slots.acciones" class="flex flex-wrap items-center gap-2">
      <slot name="acciones" />
    </div>
  </header>
</template>
