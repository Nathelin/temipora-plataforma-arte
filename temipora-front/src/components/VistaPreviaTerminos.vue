<script setup>
import { computed } from 'vue'

const props = defineProps({
  terminos: { type: Object, required: true },
  temasCatalogo: { type: Array, default: () => [] },
  derechosCatalogo: { type: Array, default: () => [] },
  mostrarAviso: { type: Boolean, default: true },
})

const secciones = [
  { key: 'informacionGeneral', label: 'Información general' },
  { key: 'politicaRevisiones', label: 'Política de revisiones' },
  { key: 'politicaCancelaciones', label: 'Política de cancelaciones' },
  { key: 'usoArtista', label: 'Uso del artista' },
  { key: 'usoCliente', label: 'Uso del cliente' },
]

const OPCIONES_ESTADO = [
  { valor: 'SI', texto: 'Sí' },
  { valor: 'NO', texto: 'No' },
  { valor: 'CONSULTAR', texto: 'Consultar' },
]

const form = computed(() => props.terminos?.form || {})
const estadosTemas = computed(() => props.terminos?.estadosTemas || {})
const temasPersonalizados = computed(() => props.terminos?.temasPersonalizados || [])
const estadoDerechos = computed(() => props.terminos?.estadoDerechos || {})

const temasVisibles = computed(() => props.temasCatalogo.filter((t) => estadosTemas.value[t.idTemaTermino]))
const temasPropiosVisibles = computed(() => temasPersonalizados.value.filter((t) => t.nombre.trim() && t.estado))
const derechosVisibles = computed(() => props.derechosCatalogo.filter((d) => estadoDerechos.value[d.idDerechoTermino]?.disponible))
const hayTemas = computed(() => temasVisibles.value.length || temasPropiosVisibles.value.length)

function chipActivo(valor) {
  if (valor === 'SI') return 'bg-green-600 text-white'
  if (valor === 'NO') return 'bg-rojizo text-white'
  return 'bg-coral text-white'
}

function textoEstado(valor) {
  return OPCIONES_ESTADO.find((o) => o.valor === valor)?.texto || valor
}
</script>

<template>
  <div class="space-y-6">
    <p v-if="mostrarAviso" class="text-xs text-cafe/50">Así vería el cliente tus términos.</p>

    <div v-for="s in secciones" :key="s.key" class="border-t border-rosa/40 pt-4 first:border-t-0 first:pt-0">
      <h3 class="text-base font-medium text-granate mb-1">{{ s.label }}</h3>
      <p class="text-sm text-cafe whitespace-pre-wrap">{{ form[s.key] || '—' }}</p>
    </div>

    <div class="border-t border-rosa/40 pt-4">
      <p class="text-xs text-cafe/50">Tiempo estimado de entrega</p>
      <p class="text-sm text-cafe">{{ form.tiempoEntrega || '—' }}</p>
    </div>

    <div class="border-t border-rosa/40 pt-4">
      <h3 class="text-base font-medium text-granate mb-2">¿Qué hago y qué no?</h3>
      <ul class="space-y-1 text-sm text-cafe">
        <li v-for="t in temasVisibles" :key="t.idTemaTermino" class="flex items-center justify-between gap-3">
          <span>{{ t.nombre }}</span>
          <span class="rounded-full px-2.5 py-0.5 text-xs" :class="chipActivo(estadosTemas[t.idTemaTermino])">
            {{ textoEstado(estadosTemas[t.idTemaTermino]) }}
          </span>
        </li>
        <li v-for="t in temasPropiosVisibles" :key="t.uid" class="flex items-center justify-between gap-3">
          <span>{{ t.nombre }}</span>
          <span class="rounded-full px-2.5 py-0.5 text-xs" :class="chipActivo(t.estado)">
            {{ textoEstado(t.estado) }}
          </span>
        </li>
      </ul>
      <p v-if="!hayTemas" class="text-sm text-cafe/40">Sin temas definidos.</p>
    </div>

    <div class="border-t border-rosa/40 pt-4">
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
