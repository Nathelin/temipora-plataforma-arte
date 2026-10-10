<script setup>
import { ref, computed, watch } from 'vue'
import api from '../servicios/api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  idMedio: { type: Number, default: 0 },
})
const emit = defineEmits(['update:modelValue'])

const MAXIMO = 5

const disponibles = ref([])
const cargando = ref(false)
const error = ref('')
const busqueda = ref('')
const desplegado = ref(false)

const habilitado = computed(() => !!props.idMedio)

watch(
  () => props.idMedio,
  () => {
    disponibles.value = []
    busqueda.value = ''
    desplegado.value = false
    if (props.idMedio) cargarDisponibles()
  },
  { immediate: true },
)

async function cargarDisponibles() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/materiales', { params: { idMedio: props.idMedio } })
    disponibles.value = res.data
  } catch {
    error.value = 'No se pudieron cargar los materiales.'
  } finally {
    cargando.value = false
  }
}

const seleccionadas = computed(() => props.modelValue)
const lleno = computed(() => seleccionadas.value.length >= MAXIMO)

const sugerencias = computed(() => {
  const texto = busqueda.value.trim().toLowerCase()
  const ids = new Set(seleccionadas.value.map(m => m.idMaterial))
  return disponibles.value
    .filter(m => !ids.has(m.idMaterial))
    .filter(m => !texto || m.nombre.toLowerCase().includes(texto))
    .slice(0, 8)
})

function agregar(material) {
  if (lleno.value) return
  emit('update:modelValue', [...seleccionadas.value, material])
  busqueda.value = ''
}

function quitar(idMaterial) {
  emit('update:modelValue', seleccionadas.value.filter(m => m.idMaterial !== idMaterial))
}
</script>

<template>
  <div>
    <label class="block text-sm text-cafe mb-2">
      Materiales y herramientas <span class="text-cafe/50">(máx. {{ MAXIMO }})</span>
    </label>

    <div class="flex flex-wrap items-center gap-2 mb-2 min-h-8">
      <span
        v-for="m in seleccionadas"
        :key="m.idMaterial"
        class="px-2.5 py-1 rounded-full text-xs flex items-center gap-1.5 bg-durazno/40 text-cafe"
        :title="m.nombreTipoArte ? `${m.nombre} · ${m.nombreTipoArte}` : m.nombre"
      >
        {{ m.nombre }}
        <button
          type="button"
          class="w-4 h-4 leading-none hover:text-rojizo"
          :aria-label="`Quitar material ${m.nombre}`"
          @click="quitar(m.idMaterial)"
        >
          ×
        </button>
      </span>
      <span v-if="!seleccionadas.length" class="text-xs text-cafe/50">
        Sin materiales seleccionados.
      </span>
    </div>

    <div class="relative">
      <input
        v-model="busqueda"
        type="text"
        :disabled="!habilitado || lleno || cargando"
        :placeholder="!habilitado
          ? 'Primero selecciona el tipo de arte'
          : (lleno ? 'Máximo de materiales alcanzado' : 'Buscar material...')"
        class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none disabled:opacity-60"
        @focus="habilitado && (desplegado = true)"
        @blur="desplegado = false"
      />
      <ul
        v-if="habilitado && desplegado && !lleno"
        class="absolute z-10 mt-1 w-full bg-blanco border border-rosa rounded-lg shadow-lg max-h-48 overflow-y-auto"
      >
        <li v-for="m in sugerencias" :key="m.idMaterial">
          <button
            type="button"
            class="w-full text-left px-3 py-2 text-sm text-cafe hover:bg-rosa/20 flex items-center justify-between gap-2"
            @mousedown.prevent="agregar(m)"
          >
            <span>{{ m.nombre }}</span>
            <span class="text-xs text-cafe/40">{{ m.nombreTipoArte }}</span>
          </button>
        </li>
        <li v-if="busqueda.trim() && !sugerencias.length" class="px-3 py-2 text-sm text-cafe/50">
          Sin resultados para "{{ busqueda.trim() }}".
        </li>
        <li v-else-if="!cargando && !disponibles.length" class="px-3 py-2 text-sm text-cafe/50">
          Todavía no hay materiales cargados para este tipo de arte.
        </li>
      </ul>
      <p v-if="habilitado && cargando" class="text-xs text-cafe/50 mt-1">Cargando materiales...</p>
      <p v-if="error" class="text-xs text-rojizo mt-1">{{ error }}</p>
    </div>
  </div>
</template>
