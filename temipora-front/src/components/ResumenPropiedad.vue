<script setup>
import { computed } from 'vue'
import { formatearFechaHora } from '../servicios/fechas'
import { etiquetaEvento, hashCorto } from '../servicios/ledger'
import ChipAnclaje from './ChipAnclaje.vue'

const props = defineProps({
  nombre: { type: String, default: '' },
  propietario: { type: String, default: null },
  estado: { type: String, default: '' },
  eventos: { type: Array, default: () => [] },
  integridad: { type: Boolean, default: true },
  anclado: { type: Boolean, default: false },
  bitcoinAltura: { type: [Number, String], default: null },
  fechaAnclaje: { type: String, default: null },
  codigoCertificado: { type: String, default: null },
})

const emit = defineEmits(['ver-historial'])

const ultimoEvento = computed(() => (
  props.eventos.length ? props.eventos[props.eventos.length - 1] : null
))
const verificado = computed(() => Boolean(ultimoEvento.value?.verificado))
</script>

<template>
  <article class="rounded-xl border border-rosa/50 bg-crema/40 p-3">
    <div class="flex flex-wrap items-start justify-between gap-2">
      <div class="min-w-0">
        <p class="text-sm font-semibold text-granate">{{ nombre }}</p>
        <p class="mt-0.5 text-[11px] text-cafe/55">
          <template v-if="propietario">
            Propietario actual: @{{ propietario }} ·
          </template>
          {{ estado }}
        </p>
      </div>
      <span
        class="rounded-full px-2 py-0.5 text-[10px] font-bold"
        :class="verificado && integridad
          ? 'bg-green-500/10 text-green-700'
          : 'bg-rojizo/10 text-rojizo'"
      >
        {{ verificado && integridad ? 'Verificado' : 'Revisar' }}
      </span>
    </div>

    <div v-if="anclado" class="mt-2 flex flex-wrap items-center gap-2">
      <ChipAnclaje :anclado="anclado" :bitcoin-altura="bitcoinAltura" />
      <span v-if="fechaAnclaje" class="text-[10px] text-cafe/50">
        {{ formatearFechaHora(fechaAnclaje) }}
      </span>
    </div>

    <p v-if="ultimoEvento" class="mt-2 text-[11px] text-cafe/65">
      Último movimiento:
      <strong class="text-granate">{{ etiquetaEvento(ultimoEvento.tipo) }}</strong>
      · {{ formatearFechaHora(ultimoEvento.ocurridoEn) }}
    </p>
    <p v-else class="mt-2 text-[11px] text-cafe/55">
      Sin movimientos registrados.
    </p>

    <div class="mt-3 flex flex-wrap items-center gap-2">
      <button
        v-if="eventos.length"
        type="button"
        class="rounded-full border border-rosa bg-blanco px-3 py-1.5 text-[11px] font-semibold text-granate transition hover:bg-rosa/20"
        @click="emit('ver-historial')"
      >
        Ver historial completo
      </button>
      <router-link
        v-if="codigoCertificado"
        :to="{ name: 'certificado-publico', params: { codigo: codigoCertificado } }"
        class="rounded-full border border-coral/40 bg-coral/10 px-3 py-1.5 text-[11px] font-semibold text-coral transition hover:bg-coral/20"
      >
        Ver certificado
      </router-link>
    </div>
  </article>
</template>
