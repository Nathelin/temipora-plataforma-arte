<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '../servicios/api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue'])

const MAXIMO = 5

const disponibles = ref([])
const cargando = ref(false)
const error = ref('')
const busqueda = ref('')
const desplegado = ref(false)
const indiceActivo = ref(-1)

const categorias = ref([])
const categoriasCargadas = ref(false)
const modalPropuestaAbierto = ref(false)
const proponiendo = ref(false)
const errorPropuesta = ref('')
const exitoPropuesta = ref('')
const propNombre = ref('')
const propDescripcion = ref('')
const propObservaciones = ref('')
const propIdCategoria = ref('')

onMounted(cargarDisponibles)

async function cargarDisponibles() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/etiquetas/selector')
    disponibles.value = res.data
  } catch {
    error.value = 'No se pudieron cargar las etiquetas.'
  } finally {
    cargando.value = false
  }
}

const seleccionadas = computed(() => props.modelValue)
const lleno = computed(() => seleccionadas.value.length >= MAXIMO)

const sugerencias = computed(() => {
  const texto = busqueda.value.trim().toLowerCase()
  const ids = new Set(seleccionadas.value.map(e => e.idEtiqueta))
  return disponibles.value
    .filter(e => !ids.has(e.idEtiqueta))
    .filter(e => !texto || e.nombre.toLowerCase().includes(texto))
    .slice(0, 8)
})

watch(busqueda, () => {
  indiceActivo.value = -1
})

watch(sugerencias, (lista) => {
  if (!lista.length) {
    indiceActivo.value = -1
  } else if (indiceActivo.value >= lista.length) {
    indiceActivo.value = lista.length - 1
  }
})

function moverActivo(delta) {
  const total = sugerencias.value.length
  if (!total) return
  desplegado.value = true
  if (indiceActivo.value === -1) {
    indiceActivo.value = delta > 0 ? 0 : total - 1
    return
  }
  const siguiente = indiceActivo.value + delta
  indiceActivo.value = siguiente < 0 ? total - 1 : siguiente >= total ? 0 : siguiente
}

function onKeydown(event) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moverActivo(1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    moverActivo(-1)
  } else if (event.key === 'Enter') {
    event.preventDefault()
    if (indiceActivo.value >= 0 && sugerencias.value[indiceActivo.value]) {
      agregar(sugerencias.value[indiceActivo.value])
    } else if (sugerencias.value.length) {
      agregar(sugerencias.value[0])
    }
  } else if (event.key === 'Escape') {
    desplegado.value = false
    indiceActivo.value = -1
  }
}

function agregar(etiqueta) {
  if (lleno.value) return
  emit('update:modelValue', [...seleccionadas.value, etiqueta])
  busqueda.value = ''
  exitoPropuesta.value = ''
}

function quitar(idEtiqueta) {
  emit('update:modelValue', seleccionadas.value.filter(e => e.idEtiqueta !== idEtiqueta))
  exitoPropuesta.value = ''
}

async function abrirPropuesta() {
  propNombre.value = busqueda.value.trim()
  propDescripcion.value = ''
  propObservaciones.value = ''
  propIdCategoria.value = ''
  errorPropuesta.value = ''
  exitoPropuesta.value = ''
  desplegado.value = false
  modalPropuestaAbierto.value = true

  if (!categoriasCargadas.value) {
    try {
      const res = await api.get('/etiquetas/categorias')
      categorias.value = res.data
      categoriasCargadas.value = true
    } catch {
      errorPropuesta.value = 'No se pudieron cargar las categorías.'
    }
  }
}

function cerrarPropuesta() {
  modalPropuestaAbierto.value = false
}

async function proponerEtiqueta() {
  if (!propNombre.value.trim()) {
    errorPropuesta.value = 'El nombre es obligatorio.'
    return
  }
  if (!propIdCategoria.value) {
    errorPropuesta.value = 'Selecciona una categoría.'
    return
  }
  if (!propDescripcion.value.trim()) {
    errorPropuesta.value = 'La descripción es obligatoria.'
    return
  }

  proponiendo.value = true
  errorPropuesta.value = ''
  try {
    const res = await api.post('/etiquetas', {
      nombre: propNombre.value.trim(),
      descripcion: propDescripcion.value.trim(),
      observaciones: propObservaciones.value.trim() || null,
      idCategoria: Number(propIdCategoria.value),
    })
    modalPropuestaAbierto.value = false
    const mensaje = `Etiqueta "${res.data.nombre}" registrada; quedará pendiente de evaluación por los administradores.`
    await cargarDisponibles()
    if (!lleno.value) {
      agregar(res.data)
    }
    exitoPropuesta.value = mensaje
  } catch (e) {
    errorPropuesta.value = e.response?.data?.message || 'No se pudo proponer la etiqueta.'
  } finally {
    proponiendo.value = false
  }
}
</script>

<template>
  <div>
    <label class="block text-sm text-cafe mb-2">
      Etiquetas <span class="text-cafe/50">(máx. {{ MAXIMO }})</span>
    </label>

    <div class="flex flex-wrap items-center gap-2 mb-2 min-h-8">
      <span
        v-for="e in seleccionadas"
        :key="e.idEtiqueta"
        class="px-2.5 py-1 rounded-full text-xs flex items-center gap-1.5"
        :class="e.estado === 'PENDIENTE' ? 'bg-coral/15 text-coral' : 'bg-durazno/40 text-cafe'"
        :title="e.estado === 'PENDIENTE' ? 'Pendiente de evaluación' : 'Etiqueta disponible'"
      >
        {{ e.nombre }}
        <span v-if="e.estado === 'PENDIENTE'" class="opacity-70">· pendiente</span>
        <button
          type="button"
          class="w-4 h-4 leading-none hover:text-rojizo"
          :aria-label="`Quitar etiqueta ${e.nombre}`"
          @click="quitar(e.idEtiqueta)"
        >
          ×
        </button>
      </span>
      <span v-if="!seleccionadas.length" class="text-xs text-cafe/50">
        Sin etiquetas seleccionadas.
      </span>
    </div>

    <div class="relative">
      <input
        v-model="busqueda"
        type="text"
        :disabled="lleno || cargando"
        :placeholder="lleno ? 'Máximo de etiquetas alcanzado' : 'Buscar etiqueta...'"
        class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none disabled:opacity-60"
        @focus="desplegado = true"
        @blur="desplegado = false"
        @keydown="onKeydown"
      />
      <ul
        v-if="desplegado && !lleno"
        class="absolute z-10 mt-1 w-full bg-blanco border border-rosa rounded-lg shadow-lg max-h-48 overflow-y-auto"
      >
        <li v-for="(e, i) in sugerencias" :key="e.idEtiqueta">
          <button
            type="button"
            class="w-full text-left px-3 py-2 text-sm text-cafe hover:bg-rosa/20 flex items-center justify-between gap-2"
            :class="i === indiceActivo ? 'bg-rosa/30' : ''"
            @mousedown.prevent="agregar(e)"
            @mouseenter="indiceActivo = i"
          >
            <span>{{ e.nombre }}</span>
            <span class="text-xs text-cafe/40">{{ e.nombreCategoria }}</span>
          </button>
        </li>
        <li v-if="busqueda.trim() && !sugerencias.length" class="px-3 py-2 text-sm text-cafe/50">
          Sin resultados para "{{ busqueda.trim() }}".
        </li>
        <li v-else-if="!cargando && !disponibles.length" class="px-3 py-2 text-sm text-cafe/50">
          Todavía no hay etiquetas disponibles.
        </li>
        <li v-if="busqueda.trim() && !lleno" class="border-t border-rosa/60">
          <button
            type="button"
            class="w-full text-left px-3 py-2 text-sm text-granate hover:bg-rosa/20 flex items-center gap-2"
            @mousedown.prevent="abrirPropuesta"
          >
            <span class="text-coral">＋</span>
            Proponer "{{ busqueda.trim() }}" como etiqueta nueva
          </button>
        </li>
      </ul>
      <p v-if="cargando" class="text-xs text-cafe/50 mt-1">Cargando etiquetas...</p>
      <p v-if="error" class="text-xs text-rojizo mt-1">{{ error }}</p>
      <p v-if="exitoPropuesta && !error" class="text-xs text-green-700 mt-1">{{ exitoPropuesta }}</p>
    </div>

    <!-- Modal: proponer etiqueta nueva (UC-54) -->
    <div
      v-if="modalPropuestaAbierto"
      class="fixed inset-0 z-[70] flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarPropuesta"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">Proponer etiqueta</h2>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2">Nombre <span class="text-rojizo">*</span></label>
            <input
              v-model="propNombre"
              type="text"
              maxlength="50"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Categoría <span class="text-rojizo">*</span></label>
            <select
              v-model="propIdCategoria"
              class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            >
              <option value="" disabled>Selecciona una categoría</option>
              <option v-for="c in categorias" :key="c.idCategoriaEtiqueta" :value="String(c.idCategoriaEtiqueta)">
                {{ c.nombre }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Descripción <span class="text-rojizo">*</span></label>
            <textarea
              v-model="propDescripcion"
              rows="3"
              maxlength="250"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
            <p class="text-right text-xs text-cafe/50 mt-1">{{ propDescripcion.length }}/250</p>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Observaciones (opcional)</label>
            <textarea
              v-model="propObservaciones"
              rows="3"
              maxlength="500"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
          </div>

          <p class="text-xs text-cafe/50">
            Las etiquetas propuestas por artistas quedan pendientes de evaluación por un administrador.
          </p>
          <p v-if="errorPropuesta" class="text-rojizo text-sm">{{ errorPropuesta }}</p>
        </div>

        <div class="flex items-center justify-end gap-3 mt-6">
          <button
            type="button"
            class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
            @click="cerrarPropuesta"
          >
            Cancelar
          </button>
          <button
            type="button"
            :disabled="proponiendo"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
            @click="proponerEtiqueta"
          >
            {{ proponiendo ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
