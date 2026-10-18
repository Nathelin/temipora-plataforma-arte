<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import api from '../servicios/api'
import TarjetaServicio from '../components/TarjetaServicio.vue'

const servicios = ref([])
const tipos = ref([])
const cargando = ref(true)
const error = ref('')

const filtros = ref({
  nombre: '',
  nombreArtista: '',
  idTipoServicio: '',
  sensible: '',
  precioMin: '',
  precioMax: '',
  tiempoMaxDias: '',
})

let secuencia = 0

async function cargar() {
  const miSecuencia = ++secuencia
  cargando.value = true
  error.value = ''
  try {
    const f = filtros.value
    const params = {}
    if (f.nombre.trim()) params.nombre = f.nombre.trim()
    if (f.nombreArtista.trim()) params.nombreArtista = f.nombreArtista.trim()
    if (f.idTipoServicio) params.idTipoServicio = f.idTipoServicio
    if (f.sensible !== '') params.sensible = f.sensible
    if (f.precioMin !== '') params.precioMin = f.precioMin
    if (f.precioMax !== '') params.precioMax = f.precioMax
    if (f.tiempoMaxDias !== '') params.tiempoMaxDias = f.tiempoMaxDias

    const res = await api.get('/servicios', { params })
    if (miSecuencia !== secuencia) return
    servicios.value = res.data
  } catch (e) {
    if (miSecuencia !== secuencia) return
    servicios.value = []
    error.value = e.response?.data?.message || 'No se pudieron cargar los servicios.'
  } finally {
    if (miSecuencia === secuencia) cargando.value = false
  }
}

function limpiarFiltros() {
  filtros.value = {
    nombre: '',
    nombreArtista: '',
    idTipoServicio: '',
    sensible: '',
    precioMin: '',
    precioMax: '',
    tiempoMaxDias: '',
  }
}

const filtrosAbiertos = ref(false)

const hayFiltrosActivos = computed(() => {
  const f = filtros.value
  return !!(
    f.nombre.trim() ||
    f.nombreArtista.trim() ||
    f.idTipoServicio ||
    f.sensible !== '' ||
    f.precioMin !== '' ||
    f.precioMax !== '' ||
    f.tiempoMaxDias !== ''
  )
})

const cantidadFiltrosActivos = computed(() => {
  const f = filtros.value
  let total = 0
  if (f.nombre.trim()) total++
  if (f.nombreArtista.trim()) total++
  if (f.idTipoServicio) total++
  if (f.sensible !== '') total++
  if (f.precioMin !== '') total++
  if (f.precioMax !== '') total++
  if (f.tiempoMaxDias !== '') total++
  return total
})

let temporizador = null
watch(filtros, () => {
  if (temporizador) clearTimeout(temporizador)
  temporizador = setTimeout(cargar, 350)
}, { deep: true })

onMounted(async () => {
  try {
    const res = await api.get('/tipos-servicio')
    tipos.value = res.data
  } catch {
    tipos.value = []
  }
  await cargar()
})
</script>

<template>
  <div class="mx-auto px-4 sm:px-8 py-8">
    <h1 class="text-2xl text-granate">Servicios</h1>
    <p class="mt-1 mb-6 text-sm text-cafe/60">Comisiones y servicios ofrecidos por los artistas de la plataforma.</p>

    <!-- Filtros (UC-08) -->
    <div class="mb-6 bg-blanco border border-rosa rounded-xl p-3">
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex-1 min-w-44 relative">
          <input
            v-model="filtros.nombre"
            type="text"
            placeholder="Buscar por nombre del servicio..."
            class="pl-4 pr-9 block w-full rounded-lg border-0 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
          <button
            v-if="filtros.nombre"
            type="button"
            class="absolute right-2.5 top-1/2 -translate-y-1/2 text-cafe/50 hover:text-granate"
            aria-label="Limpiar búsqueda"
            @click="filtros.nombre = ''"
          >
            ✕
          </button>
        </div>

        <button
          type="button"
          class="px-3 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
          :aria-expanded="filtrosAbiertos"
          @click="filtrosAbiertos = !filtrosAbiertos"
        >
          Filtros
          <span
            v-if="cantidadFiltrosActivos"
            class="ml-1.5 inline-flex items-center justify-center min-w-5 h-5 px-1 rounded-full bg-coral text-white text-xs"
          >
            {{ cantidadFiltrosActivos }}
          </span>
          <span class="ml-1" aria-hidden="true">{{ filtrosAbiertos ? '▴' : '▾' }}</span>
        </button>

        <button
          type="button"
          class="px-3 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
      </div>

      <div
        v-if="filtrosAbiertos"
        class="mt-3 pt-3 border-t border-rosa/60 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3"
      >
        <div>
          <label class="block text-xs text-cafe/60 mb-1">Artista</label>
          <input
            v-model="filtros.nombreArtista"
            type="text"
            placeholder="Nombre de usuario del artista..."
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
        <div>
          <label class="block text-xs text-cafe/60 mb-1">Tipo</label>
          <select
            v-model="filtros.idTipoServicio"
            class="block w-full rounded-lg border-0 py-2 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco"
          >
            <option value="">Todos</option>
            <option v-for="t in tipos" :key="t.idTipoServicio" :value="String(t.idTipoServicio)">
              {{ t.nombre }}
            </option>
          </select>
        </div>
        <div>
          <label class="block text-xs text-cafe/60 mb-1">Contenido sensible</label>
          <select
            v-model="filtros.sensible"
            class="block w-full rounded-lg border-0 py-2 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco"
          >
            <option value="">Todos</option>
            <option value="true">Con contenido sensible</option>
            <option value="false">Sin contenido sensible</option>
          </select>
        </div>
        <div>
          <label class="block text-xs text-cafe/60 mb-1">Tiempo máximo de entrega (días)</label>
          <input
            v-model="filtros.tiempoMaxDias"
            type="number"
            min="1"
            placeholder="Sin límite"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
        <div>
          <label class="block text-xs text-cafe/60 mb-1">Precio mínimo</label>
          <input
            v-model="filtros.precioMin"
            type="number"
            min="0"
            step="0.01"
            placeholder="0"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
        <div>
          <label class="block text-xs text-cafe/60 mb-1">Precio máximo</label>
          <input
            v-model="filtros.precioMax"
            type="number"
            min="0"
            step="0.01"
            placeholder="Sin límite"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
      </div>
    </div>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando servicios...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <template v-else>
      <p v-if="!servicios.length && hayFiltrosActivos" class="text-sm text-cafe/60 py-8 text-center">
        No hay servicios que coincidan con los filtros.
        <button type="button" class="ml-2 underline text-granate hover:text-coral transition" @click="limpiarFiltros">
          Limpiar filtros
        </button>
      </p>
      <p v-else-if="!servicios.length" class="rounded-xl border border-dashed border-rosa/60 px-4 py-12 text-center text-sm text-cafe/50">
        No hay servicios registrados en el sistema.
      </p>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <TarjetaServicio v-for="s in servicios" :key="s.idServicio" :servicio="s" />
      </div>
    </template>
  </div>
</template>
