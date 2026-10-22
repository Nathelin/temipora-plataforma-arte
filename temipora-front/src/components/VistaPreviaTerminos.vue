<script setup>
import { computed } from 'vue'

const props = defineProps({
  terminos: { type: Object, required: true },
  derechosCatalogo: { type: Array, default: () => [] },
  mostrarAviso: { type: Boolean, default: true },
  ocultarVacios: { type: Boolean, default: false },
})

const secciones = [
  { key: 'informacionGeneral', label: 'Información general' },
  { key: 'politicaRevisiones', label: 'Política de revisiones' },
  { key: 'politicaCancelaciones', label: 'Política de cancelaciones' },
  { key: 'usoArtista', label: 'Uso del artista' },
  { key: 'usoCliente', label: 'Uso del cliente' },
]

const form = computed(() => props.terminos?.form || {})
const estadoDerechos = computed(() => props.terminos?.estadoDerechos || {})
const derechosVisibles = computed(() => props.derechosCatalogo.filter((d) => estadoDerechos.value[d.idDerechoTermino]?.disponible))
const seccionesVisibles = computed(() =>
  props.ocultarVacios ? secciones.filter((s) => (form.value[s.key] || '').trim()) : secciones,
)
</script>

<template>
  <div class="space-y-6">
    <p v-if="mostrarAviso" class="text-xs text-cafe/50">Así vería el cliente tus términos.</p>

    <div v-for="s in seccionesVisibles" :key="s.key" class="border-t border-rosa/40 pt-4 first:border-t-0 first:pt-0">
      <h3 class="text-base font-medium text-granate mb-1">{{ s.label }}</h3>
      <p class="text-sm text-cafe whitespace-pre-wrap">{{ form[s.key] || '—' }}</p>
    </div>

    <div v-if="!ocultarVacios || form.queDibuja" class="border-t border-rosa/40 pt-4">
      <h3 class="text-base font-medium text-granate mb-2">¿Qué dibujo?</h3>
      <p class="text-sm text-cafe whitespace-pre-line">{{ form.queDibuja || '—' }}</p>
    </div>

    <div v-if="!ocultarVacios || form.queNoDibuja" class="border-t border-rosa/40 pt-4">
      <h3 class="text-base font-medium text-granate mb-2">¿Qué no dibujo?</h3>
      <p class="text-sm text-cafe whitespace-pre-line">{{ form.queNoDibuja || '—' }}</p>
    </div>

    <div v-if="!ocultarVacios || derechosVisibles.length" class="border-t border-rosa/40 pt-4">
      <h3 class="text-base font-medium text-granate mb-2">Derechos de uso</h3>
      <ul class="space-y-1 text-sm text-cafe">
        <li v-for="d in derechosVisibles" :key="d.idDerechoTermino" class="flex items-center justify-between gap-3">
          <span>{{ d.nombre }}</span>
          <span class="text-cafe/60">
            {{ d.codigo === 'PERSONAL' ? 'Incluido' : `+${estadoDerechos[d.idDerechoTermino].porcentajeExtra ?? 0}%` }}
          </span>
        </li>
      </ul>
      <p v-if="!derechosVisibles.length" class="text-sm text-cafe/40">Sin derechos definidos.</p>
    </div>

    <p v-if="form.pagoExtra" class="border-t border-rosa/40 pt-4 text-sm text-cafe whitespace-pre-wrap">
      <span class="text-xs text-cafe/50 block mb-1">Nota sobre precios</span>
      {{ form.pagoExtra }}
    </p>
  </div>
</template>
