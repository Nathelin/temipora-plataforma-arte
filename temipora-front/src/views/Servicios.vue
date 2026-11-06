<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import TarjetaServicio from '../components/TarjetaServicio.vue'
import { useNotificaciones } from '../composables/useNotificaciones'

const router = useRouter()
const { exito, error: notificarError } = useNotificaciones()

const servicios = ref([])
const tipos = ref([])
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')
const favoritos = ref(new Set())
const actualizandoFavorito = ref(new Set())
const orden = ref('RELEVANCIA')
const soloConCupos = ref(false)
const pagina = ref(0)
const tamanio = 24
const totalElementos = ref(0)
const hayMas = ref(false)
const personalizado = ref(false)

const filtros = ref({
  nombre: '',
  nombreArtista: '',
  idTipoServicio: '',
  sensible: '',
  precioMin: '',
  precioMax: '',
  tiempoMaxDias: '',
})

const descripcionTipoSeleccionado = computed(() => {
  if (!filtros.value.idTipoServicio) return ''
  return tipos.value.find((t) => String(t.idTipoServicio) === filtros.value.idTipoServicio)?.descripcion || ''
})

let secuencia = 0

async function cargar({ reiniciar = true } = {}) {
  const miSecuencia = ++secuencia
  if (reiniciar) {
    pagina.value = 0
    cargando.value = true
  } else {
    cargandoMas.value = true
  }
  error.value = ''
  try {
    const f = filtros.value
    const params = {
      orden: orden.value,
      pagina: pagina.value,
      tamanio,
    }
    if (f.nombre.trim()) params.nombre = f.nombre.trim()
    if (f.nombreArtista.trim()) params.nombreArtista = f.nombreArtista.trim()
    if (f.idTipoServicio) params.idTipoServicio = f.idTipoServicio
    if (f.sensible !== '') params.sensible = f.sensible
    if (f.precioMin !== '') params.precioMin = f.precioMin
    if (f.precioMax !== '') params.precioMax = f.precioMax
    if (f.tiempoMaxDias !== '') params.tiempoMaxDias = f.tiempoMaxDias
    if (soloConCupos.value) params.incluirSinCupos = false

    const res = await api.get('/servicios', { params })
    if (miSecuencia !== secuencia) return
    const data = res.data || {}
    const contenido = Array.isArray(data.contenido) ? data.contenido : []
    servicios.value = reiniciar ? contenido : [...servicios.value, ...contenido]
    totalElementos.value = data.totalElementos ?? contenido.length
    hayMas.value = !!data.hayMas
    personalizado.value = !!data.personalizado
  } catch (e) {
    if (miSecuencia !== secuencia) return
    if (reiniciar) {
      servicios.value = []
      totalElementos.value = 0
      hayMas.value = false
      personalizado.value = false
    }
    error.value = e.response?.data?.message || 'No se pudieron cargar los servicios.'
  } finally {
    if (miSecuencia === secuencia) {
      cargando.value = false
      cargandoMas.value = false
    }
  }
}

async function cargarMas() {
  if (!hayMas.value || cargandoMas.value || cargando.value) return
  pagina.value += 1
  await cargar({ reiniciar: false })
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

watch(orden, () => {
  if (temporizador) clearTimeout(temporizador)
  void cargar()
})

watch(soloConCupos, () => {
  if (temporizador) clearTimeout(temporizador)
  void cargar()
})

async function cargarFavoritos() {
  if (!localStorage.getItem('token')) return
  try {
    const res = await api.get('/favoritos/servicios/ids')
    favoritos.value = new Set(res.data || [])
  } catch {
    favoritos.value = new Set()
  }
}

async function alternarFavorito(servicio) {
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login' })
    return
  }

  const id = servicio.idServicio
  if (actualizandoFavorito.value.has(id)) return

  actualizandoFavorito.value = new Set(actualizandoFavorito.value).add(id)
  try {
    if (favoritos.value.has(id)) {
      await api.delete(`/favoritos/servicios/${id}`)
      const siguientes = new Set(favoritos.value)
      siguientes.delete(id)
      favoritos.value = siguientes
      exito(`Quitaste "${servicio.nombre}" de tus favoritos.`)
    } else {
      await api.post(`/favoritos/servicios/${id}`)
      favoritos.value = new Set(favoritos.value).add(id)
      exito(`Agregaste "${servicio.nombre}" a tus favoritos.`)
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    notificarError(e.response?.data?.message || 'No se pudieron actualizar tus favoritos.')
  } finally {
    const siguientes = new Set(actualizandoFavorito.value)
    siguientes.delete(id)
    actualizandoFavorito.value = siguientes
  }
}

onMounted(async () => {
  void cargarFavoritos()
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

        <label class="inline-flex items-center">
          <span class="sr-only">Ordenar resultados</span>
          <select
            v-model="orden"
            class="min-h-10 rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-cafe transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
          >
            <option value="RELEVANCIA">Para vos</option>
            <option value="NOMBRE">Nombre</option>
            <option value="PRECIO_ASC">Precio: menor a mayor</option>
            <option value="PRECIO_DESC">Precio: mayor a menor</option>
          </select>
        </label>

        <label class="inline-flex min-h-10 items-center gap-2 rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/30 focus-within:ring-2 focus-within:ring-coral/60">
          <input
            v-model="soloConCupos"
            type="checkbox"
            class="h-4 w-4 rounded border-borde text-coral focus:ring-coral"
          />
          Solo con cupos
        </label>

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
          <p v-if="descripcionTipoSeleccionado" class="mt-2 text-xs leading-relaxed text-cafe/60">
            {{ descripcionTipoSeleccionado }}
          </p>
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
        <div v-if="servicios.length" class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div class="flex flex-wrap items-center gap-3">
            <p class="text-sm text-cafe/60">
              {{ totalElementos }} {{ totalElementos === 1 ? 'servicio' : 'servicios' }} para explorar
            </p>
            <span
              v-if="personalizado && orden === 'RELEVANCIA'"
              class="inline-flex items-center gap-1.5 rounded-full bg-mantequilla px-3 py-1 text-xs font-bold text-tinta"
              title="Usamos tus favoritos, pedidos y visitas para ordenar los resultados."
            >
              <span aria-hidden="true">✦</span>
              Ordenado para vos
            </span>
          </div>
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
          No hay servicios de otros artistas para explorar por ahora.
        </p>

        <div v-else class="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
          <TarjetaServicio
            v-for="s in servicios"
            :key="s.idServicio"
            :servicio="s"
            mostrar-favorito
            :es-favorito="favoritos.has(s.idServicio)"
            :actualizando-favorito="actualizandoFavorito.has(s.idServicio)"
            @alternar-favorito="alternarFavorito"
          />
        </div>

        <div v-if="hayMas" class="mt-8 flex justify-center">
          <button
            type="button"
            class="inline-flex min-h-11 items-center rounded-full bg-coral px-6 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="cargandoMas"
            @click="cargarMas"
          >
            {{ cargandoMas ? 'Cargando...' : 'Cargar más servicios' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>
