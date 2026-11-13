<script setup>
import { computed } from 'vue'
import { infoAnclaje } from '../servicios/ledger'

const props = defineProps({
  anclado: { type: Boolean, default: false },
  bitcoinAltura: { type: [Number, String], default: null },
})

const info = computed(() => infoAnclaje(props.anclado, props.bitcoinAltura))
</script>

<template>
  <span
    v-if="info"
    class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[10px] font-bold"
    :class="info.clase"
    :title="info.bitcoinAltura
      ? `Sellado en la blockchain de Bitcoin en el bloque ${info.bitcoinAltura}.`
      : 'Sellado en la blockchain de Bitcoin; esperando la confirmación del bloque.'"
  >
    <span class="h-1.5 w-1.5 rounded-full" :class="info.punto" aria-hidden="true"></span>
    {{ info.texto }}<template v-if="info.bitcoinAltura"> · bloque {{ info.bitcoinAltura }}</template>
  </span>
</template>
