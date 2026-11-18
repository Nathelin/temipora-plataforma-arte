<script setup>
defineProps({
  compact: { type: Boolean, default: false },
  soloNombre: { type: Boolean, default: false },
})
</script>

<template>
  <span class="inline-flex items-center gap-2.5">
    <img
      src="/icon.ico"
      alt=""
      width="30"
      height="30"
      aria-hidden="true"
      class="h-[30px] w-[30px] shrink-0 object-contain transition-transform duration-300 group-hover:-rotate-6"
      :class="soloNombre ? 'hidden sm:block' : ''"
    />
    <span
      v-if="!compact"
      class="font-display font-extrabold leading-none tracking-[-0.03em] text-granate"
      :class="soloNombre ? 'text-xl sm:text-[24px]' : 'text-[24px]'"
    >
      Temipora
    </span>
  </span>
</template>
