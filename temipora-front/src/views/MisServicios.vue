<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import { estadoServicioDe } from '../servicios/servicios'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()

const servicios = ref([])
const cargando = ref(true)
const error = ref('')
const filtro = ref('publicados')
const filtrosAbiertos = ref(false)
const aviso = ref(null)
const dialogo = ref(null)
const procesandoDialogo = ref(false)
const filtros = ref({
  texto: '',
  tipo: '',
  medio: '',
  precioMin: '',
  precioMax: '',
  cuposMin: '',
  cuposMax: '',
})
let temporizadorAviso = null

function normalizar(texto) {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

function numero(valor) {
  if (valor === '' || valor === null || valor === undefined) return null
  const n = Number(valor)
  return Number.isFinite(n) ? n : null
}

function precioDe(servicio) {
  return Number(servicio.precioBaseMostrado ?? servicio.precioBase ?? 0)
}

function monedaDe(servicio) {
  return servicio.monedaMostrada || servicio.moneda || ''
}

const monedaFiltro = computed(() => servicios.value.map(monedaDe).find(Boolean) || '')

const serviciosFiltradosPorCampos = computed(() => {
  const texto = normalizar(filtros.value.texto)
  const tipo = normalizar(filtros.value.tipo)
  const medio = normalizar(filtros.value.medio)
  const precioMin = numero(filtros.value.precioMin)
  const precioMax = numero(filtros.value.precioMax)
  const cuposMin = numero(filtros.value.cuposMin)
  const cuposMax = numero(filtros.value.cuposMax)

  return servicios.value.filter((servicio) => {
    if (texto && !normalizar(`${servicio.nombre} ${servicio.descripcion}`).includes(texto)) return false
    if (tipo && !normalizar(servicio.nombreTipoServicio).includes(tipo)) return false
    if (medio && !normalizar(servicio.nombreMedio).includes(medio)) return false

    const precio = precioDe(servicio)
    if (precioMin !== null && precio < precioMin) return false
    if (precioMax !== null && precio > precioMax) return false

    const cupos = Number(servicio.cuposDisponibles ?? servicio.cupos ?? 0)
    if (cuposMin !== null && cupos < cuposMin) return false
    if (cuposMax !== null && cupos > cuposMax) return false

    return true
  })
})

const serviciosPublicados = computed(() => serviciosFiltradosPorCampos.value.filter((s) => s.estado === 'PUBLICADO'))
const serviciosPausados = computed(() => serviciosFiltradosPorCampos.value.filter((s) => s.estado === 'PAUSADO'))
const serviciosFiltrados = computed(() =>
  filtro.value === 'pausados' ? serviciosPausados.value : serviciosPublicados.value,
)
const opcionesFiltro = computed(() => [
  { valor: 'publicados', texto: 'Publicados', cantidad: serviciosPublicados.value.length },
  { valor: 'pausados', texto: 'Pausados', cantidad: serviciosPausados.value.length },
])

const hayFiltros = computed(() => Object.values(filtros.value).some((valor) => String(valor).trim() !== ''))

const filtrosActivos = computed(() => {
  const activos = []
  const actuales = filtros.value

  if (actuales.texto.trim()) {
    activos.push({ clave: 'texto', texto: `“${actuales.texto.trim()}”` })
  }
  if (actuales.tipo.trim()) {
    activos.push({ clave: 'tipo', texto: `Tipo: ${actuales.tipo.trim()}` })
  }
  if (actuales.medio.trim()) {
    activos.push({ clave: 'medio', texto: `Medio: ${actuales.medio.trim()}` })
  }

  const precioMin = actuales.precioMin !== '' ? formatearPrecio(Number(actuales.precioMin), monedaFiltro.value) : null
  const precioMax = actuales.precioMax !== '' ? formatearPrecio(Number(actuales.precioMax), monedaFiltro.value) : null
  if (precioMin || precioMax) {
    let texto = null
    if (precioMin && precioMax) texto = `Precio: ${precioMin} – ${precioMax}`
    else if (precioMin) texto = `Precio desde ${precioMin}`
    else texto = `Precio hasta ${precioMax}`
    activos.push({ clave: 'precio', texto })
  }

  if (actuales.cuposMin !== '' || actuales.cuposMax !== '') {
    let texto = null
    if (actuales.cuposMin !== '' && actuales.cuposMax !== '') {
      texto = `Cupos: ${actuales.cuposMin} – ${actuales.cuposMax}`
    } else if (actuales.cuposMin !== '') {
      texto = `Cupos desde ${actuales.cuposMin}`
    } else {
      texto = `Cupos hasta ${actuales.cuposMax}`
    }
    activos.push({ clave: 'cupos', texto })
  }

  return activos
})

const nombresServicios = computed(() => [
  ...new Set(servicios.value.map((s) => s.nombre).filter(Boolean)),
].sort((a, b) => a.localeCompare(b, 'es')))

const tiposServicio = computed(() => [
  ...new Set(servicios.value.map((s) => s.nombreTipoServicio).filter(Boolean)),
].sort((a, b) => a.localeCompare(b, 'es')))

const mediosServicio = computed(() => [
  ...new Set(servicios.value.map((s) => s.nombreMedio).filter(Boolean)),
].sort((a, b) => a.localeCompare(b, 'es')))

const estadoVacio = computed(() => {
  if (hayFiltros.value) {
    return {
      titulo: 'No hay servicios que coincidan.',
      detalle: 'Probá con otros filtros o limpiá la búsqueda.',
      accion: { texto: 'Limpiar filtros', tipo: 'limpiar' },
    }
  }
  if (filtro.value === 'publicados') {
    return {
      titulo: 'No tenés servicios publicados.',
      detalle: servicios.value.some((s) => s.estado === 'PAUSADO')
        ? 'Revisá los que pausaste o creá uno nuevo.'
        : 'Creá tu primer servicio para empezar a ofrecerlo.',
      accion: servicios.value.some((s) => s.estado === 'PAUSADO')
        ? { texto: 'Ver pausados', tipo: 'estado', valor: 'pausados' }
        : null,
    }
  }
  if (filtro.value === 'pausados') {
    return {
      titulo: 'No tenés servicios pausados.',
      detalle: 'Todos tus servicios están publicados.',
      accion: servicios.value.some((s) => s.estado === 'PUBLICADO')
        ? { texto: 'Ver publicados', tipo: 'estado', valor: 'publicados' }
        : null,
    }
  }
  return {
    titulo: 'No hay servicios para mostrar.',
    detalle: 'Creá tu primer servicio para empezar.',
    accion: null,
  }
})

function ejecutarAccionVacio() {
  const accion = estadoVacio.value.accion
  if (!accion) return
  if (accion.tipo === 'limpiar') limpiarFiltros()
  else if (accion.tipo === 'estado') filtro.value = accion.valor
}

function quitarFiltro(clave) {
  if (clave === 'precio') {
    filtros.value.precioMin = ''
    filtros.value.precioMax = ''
    return
  }
  if (clave === 'cupos') {
    filtros.value.cuposMin = ''
    filtros.value.cuposMax = ''
    return
  }
  filtros.value[clave] = ''
}

function limpiarFiltros() {
  Object.keys(filtros.value).forEach((clave) => {
    filtros.value[clave] = ''
  })
}

function mostrarAviso(tipo, texto) {
  aviso.value = { tipo, texto }
  clearTimeout(temporizadorAviso)
  temporizadorAviso = setTimeout(() => {
    aviso.value = null
  }, 4000)
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/mis-servicios')
    servicios.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar tus servicios.'
  } finally {
    cargando.value = false
  }
}

function textoEntrega(s) {
  return `${s.tiempoEntregaValor} ${(s.tiempoEntregaUnidad || '').toLowerCase()}`
}

const tituloDialogo = computed(() => {
  const d = dialogo.value
  if (!d) return ''
  if (d.bloqueo) return 'No podés dar de baja definitiva el servicio'
  if (d.tipo === 'pausar') return `¿Pausar “${d.servicio.nombre}”?`
  if (d.tipo === 'reactivar') return `¿Reactivar “${d.servicio.nombre}”?`
  return `¿Dar de baja definitiva “${d.servicio.nombre}”?`
})

const mensajeDialogo = computed(() => {
  const d = dialogo.value
  if (!d) return ''
  if (d.bloqueo) return d.bloqueo
  if (d.tipo === 'pausar') {
    return 'Dejará de aparecer en búsquedas y en tu perfil, y no recibirá solicitudes nuevas.'
  }
  if (d.tipo === 'reactivar') {
    return 'Volverá a aparecer en búsquedas y en tu perfil, y podrá recibir solicitudes.'
  }
  return 'El servicio dejará de estar visible para vos y para los clientes, y solo un administrador podrá restaurarlo.'
})

const encabezadoDetalle = computed(() => {
  const d = dialogo.value
  if (!d || d.bloqueo || d.tipo === 'reactivar') return ''
  return d.tipo === 'pausar' ? 'Vas a conservar' : 'Consecuencias'
})

const detallesDialogo = computed(() => {
  const d = dialogo.value
  if (!d || d.bloqueo || !d.resumen) return []
  const r = d.resumen

  if (d.tipo === 'pausar') {
    const conserva = []
    if (r.pendientes) conserva.push(`${r.pendientes} solicitud(es) pendiente(s)`)
    if (r.enEspera) conserva.push(`${r.enEspera} en lista de espera`)
    if (r.presupuestadas) conserva.push(`${r.presupuestadas} presupuestada(s)`)
    if (r.pagadas) conserva.push(`${r.pagadas} pagada(s)`)
    if (r.listaEspera) conserva.push(`${r.listaEspera} en la lista de espera`)
    return conserva.length
      ? conserva.map((texto) => `${texto}: podés seguir atendiéndolas aunque el servicio esté pausado.`)
      : ['No tenés solicitudes ni pedidos en curso.']
  }

  const seCierran = []
  if (r.pendientes) seCierran.push(`${r.pendientes} solicitud(es) pendiente(s) se van a rechazar`)
  if (r.enEspera) seCierran.push(`${r.enEspera} en lista de espera se van a rechazar`)
  if (r.listaEspera) seCierran.push(`${r.listaEspera} en la lista de espera se van a cancelar`)
  return seCierran.length ? seCierran : ['No tenés solicitudes en curso.']
})

const textoConfirmarDialogo = computed(() => {
  const d = dialogo.value
  if (!d) return 'Confirmar'
  if (d.bloqueo) return 'Ver solicitudes'
  if (d.tipo === 'pausar') return 'Pausar servicio'
  if (d.tipo === 'reactivar') return 'Reactivar servicio'
  return 'Dar de baja definitiva'
})

async function abrirDialogo(tipo, servicio) {
  if (tipo === 'reactivar') {
    dialogo.value = { tipo, servicio }
    return
  }
  try {
    const res = await api.get(`/servicios/${servicio.idServicio}/resumen`)
    dialogo.value = { tipo, servicio, resumen: res.data }
  } catch (e) {
    mostrarAviso('error', e.response?.data?.message || 'No se pudo consultar el estado del servicio.')
  }
}

function cerrarDialogo() {
  if (procesandoDialogo.value) return
  dialogo.value = null
}

async function confirmarDialogo() {
  const d = dialogo.value
  if (!d || procesandoDialogo.value) return
  if (d.bloqueo) {
    dialogo.value = null
    router.push(`/panel/servicios/${d.servicio.idServicio}/solicitudes`)
    return
  }
  procesandoDialogo.value = true
  try {
    const res = await api.post(`/servicios/${d.servicio.idServicio}/${d.tipo === 'reactivar' ? 'reactivar' : d.tipo}`)
    if (d.tipo === 'archivar') {
      servicios.value = servicios.value.filter((s) => s.idServicio !== d.servicio.idServicio)
      mostrarAviso('exito', `“${d.servicio.nombre}” se dio de baja definitiva.`)
    } else {
      d.servicio.estado = res.data.estado
      mostrarAviso(
        'exito',
        d.tipo === 'pausar'
          ? `“${d.servicio.nombre}” quedó pausado. Tus solicitudes y pedidos siguen activos.`
          : `“${d.servicio.nombre}” se reactivó y vuelve a estar visible.`,
      )
    }
    dialogo.value = null
  } catch (e) {
    const mensaje = e.response?.data?.message || 'No se pudo actualizar el estado.'
    if (d.tipo === 'archivar' && e.response?.status === 409) {
      dialogo.value = { ...d, bloqueo: mensaje }
    } else {
      mostrarAviso('error', mensaje)
    }
  } finally {
    procesandoDialogo.value = false
  }
}

onMounted(cargar)
onBeforeUnmount(() => clearTimeout(temporizadorAviso))
</script>

<template>
  <div>
    <PageHeader
      eyebrow="Tu espacio de trabajo"
      titulo="Mis servicios"
      descripcion="Los servicios que ofrecés a tus clientes."
      :nivel="2"
    >
      <template #acciones>
        <router-link
          to="/panel/servicios/crear"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
        >
          Crear servicio
        </router-link>
      </template>
    </PageHeader>

    <p v-if="cargando" class="mt-6 text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="mt-6 text-rojizo text-center py-10">{{ error }}</p>

    <template v-else>
      <p v-if="!servicios.length" class="rounded-xl border border-dashed border-rosa/60 px-4 py-12 text-center text-sm text-cafe/50">
        Todavía no creaste servicios. Empezá con “Crear servicio”.
      </p>

      <template v-else>
        <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div
            class="flex w-full rounded-xl border border-rosa/60 bg-blanco p-1 sm:inline-flex sm:w-fit"
            role="group"
            aria-label="Filtrar servicios por estado"
          >
            <button
              v-for="opcion in opcionesFiltro"
              :key="opcion.valor"
              type="button"
              class="flex flex-1 items-center justify-center gap-2 whitespace-nowrap rounded-lg px-2 py-1.5 text-xs font-medium transition sm:flex-none sm:px-3 sm:text-sm"
              :class="filtro === opcion.valor
                ? 'bg-coral text-white shadow-sm'
                : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
              :aria-pressed="filtro === opcion.valor"
              @click="filtro = opcion.valor"
            >
              {{ opcion.texto }}
              <span
                class="rounded-full px-1.5 py-0.5 text-[11px] leading-none"
                :class="filtro === opcion.valor ? 'bg-white/20 text-white' : 'bg-rosa/30 text-granate'"
              >
                {{ opcion.cantidad }}
              </span>
            </button>
          </div>
          <p class="text-xs text-cafe/50">
            {{ serviciosFiltrados.length }} de {{ servicios.length }}
            {{ servicios.length === 1 ? 'servicio' : 'servicios' }}
          </p>
        </div>

        <div class="mb-4 flex flex-col gap-2 sm:flex-row">
          <div class="relative flex-1">
            <svg
              class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-cafe/40"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            <input
              v-model="filtros.texto"
              type="search"
              list="nombres-servicios"
              placeholder="Buscar por nombre o descripción..."
              class="w-full rounded-xl border border-rosa/60 bg-blanco py-2.5 pl-10 pr-4 text-sm text-cafe shadow-sm transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              aria-label="Buscar servicios por nombre o descripción"
            />
            <datalist id="nombres-servicios">
              <option v-for="nombre in nombresServicios" :key="nombre" :value="nombre" />
            </datalist>
          </div>

          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-medium shadow-sm transition"
            :class="filtrosAbiertos || hayFiltros
              ? 'border-coral/40 bg-coral/5 text-granate'
              : 'border-rosa/60 bg-blanco text-cafe/70 hover:border-coral/40 hover:text-granate'"
            :aria-expanded="filtrosAbiertos"
            aria-controls="panel-filtros"
            @click="filtrosAbiertos = !filtrosAbiertos"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            Filtros
            <span
              v-if="filtrosActivos.length"
              class="rounded-full bg-coral px-1.5 py-0.5 text-[11px] leading-none text-white"
            >
              {{ filtrosActivos.length }}
            </span>
            <svg
              class="h-3.5 w-3.5 transition-transform"
              :class="filtrosAbiertos ? 'rotate-180' : ''"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="m6 9 6 6 6-6" />
            </svg>
          </button>
        </div>

        <div
          v-if="filtrosAbiertos"
          id="panel-filtros"
          class="mb-5 rounded-2xl border border-rosa/60 bg-blanco p-4 shadow-sm sm:p-5"
        >
          <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label for="filtro-tipo" class="mb-1.5 block text-xs font-medium text-cafe/70">
                Tipo de servicio
              </label>
              <input
                id="filtro-tipo"
                v-model="filtros.tipo"
                type="text"
                list="tipos-servicio"
                placeholder="Todos los tipos"
                class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              />
              <datalist id="tipos-servicio">
                <option v-for="tipo in tiposServicio" :key="tipo" :value="tipo" />
              </datalist>
            </div>

            <div>
              <label for="filtro-medio" class="mb-1.5 block text-xs font-medium text-cafe/70">
                Medio
              </label>
              <input
                id="filtro-medio"
                v-model="filtros.medio"
                type="text"
                list="medios-servicio"
                placeholder="Todos los medios"
                class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              />
              <datalist id="medios-servicio">
                <option v-for="medio in mediosServicio" :key="medio" :value="medio" />
              </datalist>
            </div>

            <div class="sm:col-span-2 lg:col-span-1">
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Precio{{ monedaFiltro ? ` (${monedaFiltro})` : '' }}
              </label>
              <div class="flex items-center gap-2">
                <input
                  v-model="filtros.precioMin"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="Mín."
                  class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  aria-label="Precio mínimo"
                />
                <span class="text-cafe/40">–</span>
                <input
                  v-model="filtros.precioMax"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="Máx."
                  class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  aria-label="Precio máximo"
                />
              </div>
            </div>

            <div class="sm:col-span-2 lg:col-span-1">
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Cupos disponibles
              </label>
              <div class="flex items-center gap-2">
                <input
                  v-model="filtros.cuposMin"
                  type="number"
                  min="0"
                  step="1"
                  placeholder="Mín."
                  class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  aria-label="Cupos disponibles mínimos"
                />
                <span class="text-cafe/40">–</span>
                <input
                  v-model="filtros.cuposMax"
                  type="number"
                  min="0"
                  step="1"
                  placeholder="Máx."
                  class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  aria-label="Cupos disponibles máximos"
                />
              </div>
            </div>
          </div>

          <div class="mt-4 flex items-center justify-between gap-3 border-t border-rosa/40 pt-3">
            <p class="text-xs text-cafe/50">
              Podés escribir el comienzo de un tipo o medio para ver sugerencias.
            </p>
            <button
              type="button"
              class="shrink-0 text-xs font-medium text-coral transition hover:text-rojizo disabled:cursor-not-allowed disabled:opacity-40"
              :disabled="!hayFiltros"
              @click="limpiarFiltros"
            >
              Limpiar filtros
            </button>
          </div>
        </div>

        <div v-if="filtrosActivos.length" class="mb-5 flex flex-wrap items-center gap-2">
          <span class="text-[11px] font-medium uppercase tracking-wide text-cafe/45">Filtros activos</span>
          <button
            v-for="activo in filtrosActivos"
            :key="activo.clave"
            type="button"
            class="inline-flex items-center gap-1.5 rounded-full border border-rosa/60 bg-blanco px-3 py-1 text-xs text-cafe transition hover:border-coral/40 hover:text-granate"
            @click="quitarFiltro(activo.clave)"
          >
            {{ activo.texto }}
            <span class="text-cafe/40" aria-hidden="true">×</span>
            <span class="sr-only">Quitar filtro</span>
          </button>
          <button
            type="button"
            class="text-xs font-medium text-coral transition hover:text-rojizo"
            @click="limpiarFiltros"
          >
            Limpiar todo
          </button>
        </div>

        <div
          v-if="aviso"
          class="mb-5 flex items-start justify-between gap-3 rounded-xl border px-4 py-3 text-sm"
          :class="aviso.tipo === 'error'
            ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
            : 'border-green-600/20 bg-green-500/10 text-green-700'"
          role="status"
        >
          <p>{{ aviso.texto }}</p>
          <button
            type="button"
            class="shrink-0 opacity-60 transition hover:opacity-100"
            aria-label="Cerrar aviso"
            @click="aviso = null"
          >
            ×
          </button>
        </div>

        <div v-if="!serviciosFiltrados.length" class="rounded-xl border border-dashed border-rosa/60 px-4 py-12 text-center">
          <p class="text-sm font-medium text-granate">{{ estadoVacio.titulo }}</p>
          <p class="mt-1 text-sm text-cafe/50">{{ estadoVacio.detalle }}</p>
          <button
            v-if="estadoVacio.accion"
            type="button"
            class="mt-4 text-sm font-medium text-coral transition hover:text-rojizo"
            @click="ejecutarAccionVacio"
          >
            {{ estadoVacio.accion.texto }}
          </button>
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          <div v-for="s in serviciosFiltrados" :key="s.idServicio" class="rounded-2xl border border-rosa/50 bg-blanco overflow-hidden flex flex-col">
            <div class="h-40 bg-rosa/20">
              <img v-if="s.imagenPortada" :src="s.imagenPortada" :alt="s.nombre" class="w-full h-full object-cover" />
            </div>
            <div class="p-4 flex-1 flex flex-col">
              <div class="flex items-start justify-between gap-2">
                <h2 class="text-granate">{{ s.nombre }}</h2>
                <span
                  class="shrink-0 rounded-full border px-2.5 py-0.5 text-xs"
                  :class="estadoServicioDe(s.estado).clase"
                >
                  {{ estadoServicioDe(s.estado).texto }}
                </span>
              </div>
              <p class="mt-1 text-xs text-cafe/60">{{ s.nombreTipoServicio }} · {{ s.nombreMedio }}</p>
              <!--p v-if="s.descripcionTipoServicio" class="mt-1 line-clamp-2 text-xs leading-relaxed text-cafe/50">
                <span class="font-semibold text-cafe/70">{{ s.nombreTipoServicio }}:</span>
                {{ s.descripcionTipoServicio }}
              </p-->
              <p class="mt-3 text-cafe whitespace-pre-wrap line-clamp-2">{{ s.descripcion }}</p>
              <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-cafe/70">
                <span>Precio: <span class="text-granate">{{ formatearPrecio(s.precioBaseMostrado ?? s.precioBase, s.monedaMostrada ?? s.moneda) }}</span></span>
                <span>Entrega: {{ textoEntrega(s) }}</span>
                <span>Cupos disponibles: {{ s.cuposDisponibles ?? s.cupos }}</span>
              </div>

              <div class="mt-auto pt-4 flex flex-wrap items-center justify-end gap-x-4 gap-y-2 border-t border-rosa/40">
                <router-link
                  :to="`/panel/servicios/${s.idServicio}/solicitudes`"
                  class="text-sm text-coral hover:text-rojizo transition"
                >
                  Solicitudes <span class="text-cafe/50">({{ s.solicitudesActivas || 0 }})</span>
                </router-link>
                <router-link :to="`/panel/servicios/${s.idServicio}/editar`" class="text-sm text-coral hover:text-rojizo transition">
                  Editar
                </router-link>
                <button
                  type="button"
                  class="text-sm text-cafe/60 hover:text-rojizo transition"
                  @click="abrirDialogo(s.estado === 'PUBLICADO' ? 'pausar' : 'reactivar', s)"
                >
                  {{ s.estado === 'PUBLICADO' ? 'Pausar' : 'Reactivar' }}
                </button>
                <button
                  type="button"
                  class="text-sm text-rojizo/80 hover:text-rojizo transition"
                  @click="abrirDialogo('archivar', s)"
                >
                  Dar de baja definitiva
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>

    <ModalConfirmacion
      :abierto="!!dialogo"
      :titulo="tituloDialogo"
      :mensaje="mensajeDialogo"
      :texto-confirmar="textoConfirmarDialogo"
      :texto-cancelar="dialogo?.bloqueo ? 'Cerrar' : 'Cancelar'"
      :destructivo="dialogo?.tipo === 'archivar' && !dialogo?.bloqueo"
      @confirmar="confirmarDialogo"
      @cerrar="cerrarDialogo"
    >
      <div
        v-if="encabezadoDetalle"
        class="mt-4 rounded-xl border border-rosa/50 bg-crema/60 p-3"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/55">
          {{ encabezadoDetalle }}
        </p>
        <ul class="mt-2 space-y-1 text-sm leading-relaxed text-cafe/75">
          <li v-for="detalle in detallesDialogo" :key="detalle" class="flex gap-2">
            <span aria-hidden="true">•</span>
            <span>{{ detalle }}</span>
          </li>
        </ul>
      </div>
      <p v-if="dialogo?.bloqueo" class="mt-3 text-sm leading-relaxed text-cafe/70">
        Podés ver y resolver las solicitudes del servicio antes de volver a intentarlo.
      </p>
    </ModalConfirmacion>
  </div>
</template>
