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
  <div class="mx-auto max-w-screen-2xl px-4 py-10 sm:px-8 sm:py-14">
    <div>
      <div class="pb-2">
        <span class="inline-block -rotate-2 rounded-full bg-mantequilla px-4 py-1.5 text-sm font-bold text-tinta shadow-sm">
          Comisiones
        </span>
        <h1 class="mt-5 font-display text-4xl font-extrabold tracking-[-0.03em] text-granate sm:text-5xl">
          Encontrá a tu próximo artista
        </h1>
        <p class="mt-3 text-lg leading-relaxed text-cafe/75">
          Explorá servicios por estilo, precio y tiempo de entrega. Cada propuesta está creada por alguien de la comunidad.
        </p>
      </div>
    </div>

    <div class="mt-8 rounded-[28px] border border-borde/60 bg-superficie p-4 shadow-[0_14px_36px_rgba(74,29,46,0.08)] sm:p-5">
      <div class="flex flex-wrap items-center gap-3">
        <div class="relative min-w-44 flex-1">
          <svg class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-cafe/40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            v-model="filtros.nombre"
            type="text"
            placeholder="Buscar por nombre del servicio..."
            class="block w-full rounded-full border-0 bg-crema py-2.5 pl-11 pr-10 text-sm text-texto shadow-none ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
          <button
            v-if="filtros.nombre"
            type="button"
            class="absolute right-3.5 top-1/2 -translate-y-1/2 text-cafe/40 transition hover:text-granate"
            aria-label="Limpiar búsqueda"
            @click="filtros.nombre = ''"
          >
            ✕
          </button>
        </div>

        <button
          type="button"
          class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
          :aria-expanded="filtrosAbiertos"
          @click="filtrosAbiertos = !filtrosAbiertos"
        >
          Filtros
          <span
            v-if="cantidadFiltrosActivos"
            class="ml-2 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-coral px-1.5 text-xs font-bold text-white"
          >
            {{ cantidadFiltrosActivos }}
          </span>
          <svg class="ml-2 h-3.5 w-3.5 transition-transform" :class="filtrosAbiertos ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clip-rule="evenodd" />
          </svg>
        </button>

        <button
          type="button"
          class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
      </div>

      <div
        v-if="filtrosAbiertos"
        class="mt-4 grid grid-cols-1 gap-4 border-t border-borde/40 pt-4 sm:grid-cols-2 lg:grid-cols-4"
      >
        <div>
          <label class="mb-1.5 block text-xs font-semibold text-cafe/60">Artista</label>
          <input
            v-model="filtros.nombreArtista"
            type="text"
            placeholder="Nombre de usuario..."
            class="block w-full rounded-2xl border-0 bg-crema px-4 py-2.5 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-semibold text-cafe/60">Tipo</label>
          <select
            v-model="filtros.idTipoServicio"
            class="block w-full rounded-2xl border-0 bg-crema py-2.5 pl-4 pr-8 text-sm text-texto ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="">Todos</option>
            <option v-for="t in tipos" :key="t.idTipoServicio" :value="String(t.idTipoServicio)">
              {{ t.nombre }}
            </option>
          </select>
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-semibold text-cafe/60">Contenido sensible</label>
          <select
            v-model="filtros.sensible"
            class="block w-full rounded-2xl border-0 bg-crema py-2.5 pl-4 pr-8 text-sm text-texto ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="">Todos</option>
            <option value="true">Con contenido sensible</option>
            <option value="false">Sin contenido sensible</option>
          </select>
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-semibold text-cafe/60">Tiempo máximo (días)</label>
          <input
            v-model="filtros.tiempoMaxDias"
            type="number"
            min="1"
            placeholder="Sin límite"
            class="block w-full rounded-2xl border-0 bg-crema px-4 py-2.5 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-semibold text-cafe/60">Precio mínimo</label>
          <input
            v-model="filtros.precioMin"
            type="number"
            min="0"
            step="0.01"
            placeholder="0"
            class="block w-full rounded-2xl border-0 bg-crema px-4 py-2.5 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-semibold text-cafe/60">Precio máximo</label>
          <input
            v-model="filtros.precioMax"
            type="number"
            min="0"
            step="0.01"
            placeholder="Sin límite"
            class="block w-full rounded-2xl border-0 bg-crema px-4 py-2.5 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
      </div>
    </div>

    <div class="mt-8">
      <div v-if="cargando" class="flex flex-col items-center justify-center py-20 text-cafe/60">
        <span class="h-8 w-8 animate-spin rounded-full border-2 border-borde border-t-coral" aria-hidden="true"></span>
        <p class="mt-4 text-sm">Cargando servicios...</p>
      </div>
      <p v-else-if="error" class="rounded-3xl border border-rojizo/20 bg-rojizo/5 px-6 py-12 text-center text-rojizo">{{ error }}</p>

      <template v-else>
        <div v-if="servicios.length" class="mb-4 flex items-center justify-between gap-4">
          <p class="text-sm text-cafe/60">
            {{ servicios.length }} {{ servicios.length === 1 ? 'servicio' : 'servicios' }} para explorar
          </p>
          <button
            v-if="hayFiltrosActivos"
            type="button"
            class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
            @click="limpiarFiltros"
          >
            Limpiar filtros
          </button>
        </div>

        <p v-if="!servicios.length && hayFiltrosActivos" class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center text-sm text-cafe/60">
          No hay servicios que coincidan con los filtros.
          <button type="button" class="ml-2 font-semibold text-granate underline decoration-coral/40 underline-offset-4 transition hover:text-coral" @click="limpiarFiltros">
            Limpiar filtros
          </button>
        </p>
        <p v-else-if="!servicios.length" class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center text-sm text-cafe/50">
          No hay servicios registrados en el sistema.
        </p>

        <div v-else class="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
          <TarjetaServicio v-for="s in servicios" :key="s.idServicio" :servicio="s" />
        </div>
      </template>
    </div>
  </div>
</template>
