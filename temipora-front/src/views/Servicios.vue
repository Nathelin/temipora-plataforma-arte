<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import TarjetaServicio from '../components/TarjetaServicio.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelFiltrosServicios from '../components/filtros/PanelFiltrosServicios.vue'
import { useNotificaciones } from '../composables/useNotificaciones'
import {
  aParams,
  aQueryUrl,
  cargarFacetas,
  chipsActivos,
  desdeQuery,
  estadoInicial,
  quitarChip,
} from '../servicios/filtrosServicio'

const route = useRoute()
const router = useRouter()
const { exito, error: notificarError } = useNotificaciones()

const servicios = ref([])
const tipos = ref([])
const medios = ref([])
const etiquetas = ref([])
const categorias = ref([])
const monedaUsuario = ref('ARS')
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')
const favoritos = ref(new Set())
const actualizandoFavorito = ref(new Set())
const pagina = ref(0)
const tamanio = 24
const totalElementos = ref(0)
const hayMas = ref(false)
const personalizado = ref(false)
const filtrosAbiertos = ref(false)
const facetas = ref(null)

const filtros = ref(desdeQuery(route.query))

const chips = computed(() => chipsActivos(filtros.value, {
  tipos: tipos.value,
  medios: medios.value,
  etiquetas: etiquetas.value,
  moneda: monedaUsuario.value,
}))

const hayFiltrosActivos = computed(() => chips.value.length > 0)

const descripcionTiposSeleccionados = computed(() => {
  if (!filtros.value.idsTiposServicio.length) return ''
  return filtros.value.idsTiposServicio
    .map((id) => tipos.value.find((t) => String(t.idTipoServicio) === String(id))?.descripcion)
    .filter(Boolean)
    .join(' · ')
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
    // URLSearchParams serializa las listas como parámetros repetidos,
    // que es como Spring las enlaza a List<Long> / List<Enum>.
    const [res, facetasRes] = await Promise.all([
      api.get('/servicios', {
        params: aParams(filtros.value, { pagina: pagina.value, tamanio }),
      }),
      reiniciar ? cargarFacetas(filtros.value).catch(() => null) : Promise.resolve(null),
    ])
    if (miSecuencia !== secuencia) return
    // Si las facetas fallan, los conteos se conservan: es preferible un número
    // viejo a un panel vacío.
    if (facetasRes) facetas.value = facetasRes
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

function actualizarFiltros(cambios) {
  filtros.value = { ...filtros.value, ...cambios }
}

function limpiarFiltros() {
  const orden = filtros.value.orden
  filtros.value = { ...estadoInicial(), orden }
}

function quitar(chip) {
  filtros.value = quitarChip(filtros.value, chip)
}

let temporizador = null
watch(filtros, () => {
  if (temporizador) clearTimeout(temporizador)
  temporizador = setTimeout(() => {
    sincronizarUrl()
    cargar()
  }, 350)
}, { deep: true })

function sincronizarUrl() {
  const query = aQueryUrl(filtros.value)
  if (JSON.stringify(query) === JSON.stringify(route.query)) return
  router.replace({ query })
}

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

function alTeclear(event) {
  if (event.key === 'Escape' && filtrosAbiertos.value) filtrosAbiertos.value = false
}

onMounted(async () => {
  document.addEventListener('keydown', alTeclear)
  void cargarFavoritos()
  const nombreUsuario = localStorage.getItem('nombreUsuario')
  const [tiposRes, mediosRes, etiquetasRes, categoriasRes, perfilRes] = await Promise.all([
    api.get('/tipos-servicio').catch(() => null),
    api.get('/medios').catch(() => null),
    api.get('/etiquetas/selector').catch(() => null),
    api.get('/etiquetas/categorias').catch(() => null),
    nombreUsuario ? api.get(`/perfil/${nombreUsuario}`).catch(() => null) : Promise.resolve(null),
  ])
  tipos.value = tiposRes?.data || []
  medios.value = (mediosRes?.data || []).filter((m) => m.activo !== false)
  etiquetas.value = etiquetasRes?.data || []
  categorias.value = categoriasRes?.data || []
  const codigo = perfilRes?.data?.monedaPreferida
  if (codigo) monedaUsuario.value = codigo
  await cargar()
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', alTeclear)
})
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8 sm:py-10">
    <PageHeader
      eyebrow="Comisiones"
      titulo="Encontrá a tu próximo artista"
      descripcion="Explorá servicios por estilo, precio y tiempo de entrega."
    />

    <div class="mt-6 lg:grid lg:grid-cols-[18rem_minmax(0,1fr)] lg:items-start lg:gap-8">
      <aside class="hidden lg:sticky lg:top-20 lg:block lg:h-[calc(100vh-6rem)] lg:overflow-y-auto">
        <PanelFiltrosServicios
          :estado="filtros"
          :tipos="tipos"
          :medios="medios"
          :etiquetas="etiquetas"
          :categorias="categorias"
          :moneda="monedaUsuario"
          :cargando="cargando"
          :facetas="facetas"
          @actualizar="actualizarFiltros"
          @limpiar="limpiarFiltros"
        />
      </aside>

      <div class="min-w-0">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex flex-wrap items-center gap-3">
            <button
              type="button"
              class="inline-flex min-h-10 items-center gap-2 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20 lg:hidden"
              :aria-expanded="filtrosAbiertos"
              @click="filtrosAbiertos = true"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M4 6h16M7 12h10M10 18h4" />
              </svg>
              Filtros
              <span
                v-if="chips.length"
                class="rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-bold leading-none text-white"
              >
                {{ chips.length }}
              </span>
            </button>

            <p v-if="!cargando && !error" class="text-sm text-cafe/60">
              {{ totalElementos }} {{ totalElementos === 1 ? 'servicio' : 'servicios' }} para explorar
            </p>
            <span
              v-if="personalizado && filtros.orden === 'RELEVANCIA' && !hayFiltrosActivos"
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

        <ul v-if="chips.length" class="mt-3 flex flex-wrap gap-2">
          <li v-for="chip in chips" :key="`${chip.clave}-${chip.valor}-${chip.texto}`">
            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-full border border-coral/30 bg-coral/10 px-3 py-1 text-xs font-semibold text-granate transition hover:border-rojizo/40 hover:bg-coral/15"
              :aria-label="`Quitar filtro ${chip.texto}`"
              @click="quitar(chip)"
            >
              <span class="max-w-56 truncate">{{ chip.texto }}</span>
              <span aria-hidden="true" class="text-cafe/50">✕</span>
            </button>
          </li>
        </ul>

        <p
          v-if="descripcionTiposSeleccionados"
          class="mt-3 rounded-xl border border-rosa/50 bg-rosa/10 px-3 py-2 text-xs leading-relaxed text-cafe/70"
        >
          {{ descripcionTiposSeleccionados }}
        </p>

        <div class="mt-6">
          <div v-if="cargando" class="flex flex-col items-center justify-center py-20 text-cafe/60">
            <span class="h-8 w-8 animate-spin rounded-full border-2 border-borde border-t-coral" aria-hidden="true"></span>
            <p class="mt-4 text-sm">Cargando servicios...</p>
          </div>
          <p
            v-else-if="error"
            class="rounded-3xl border border-rojizo/20 bg-rojizo/5 px-6 py-12 text-center text-rojizo"
          >
            {{ error }}
          </p>

          <template v-else>
            <p
              v-if="!servicios.length && hayFiltrosActivos"
              class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center text-sm text-cafe/60"
            >
              No hay servicios que coincidan con los filtros.
              <button
                type="button"
                class="ml-2 font-semibold text-granate underline decoration-coral/40 underline-offset-4 transition hover:text-coral"
                @click="limpiarFiltros"
              >
                Limpiar filtros
              </button>
            </p>
            <p
              v-else-if="!servicios.length"
              class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center text-sm text-cafe/50"
            >
              No hay servicios de otros artistas para explorar por ahora.
            </p>

            <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
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
    </div>

    <div
      v-if="filtrosAbiertos"
      class="fixed inset-0 z-50 flex lg:hidden"
      role="dialog"
      aria-modal="true"
      aria-label="Filtros del catálogo"
    >
      <div class="absolute inset-0 bg-granate/40" @click="filtrosAbiertos = false"></div>
      <div class="relative ml-auto flex h-full w-full max-w-sm flex-col bg-crema shadow-2xl">
        <div class="flex items-center justify-between border-b border-borde/50 px-4 py-3">
          <h2 class="font-display text-base font-bold text-granate">Filtros</h2>
          <button
            type="button"
            class="flex h-9 w-9 items-center justify-center rounded-full text-cafe/60 transition hover:bg-rosa/30 hover:text-granate"
            aria-label="Cerrar filtros"
            @click="filtrosAbiertos = false"
          >
            ✕
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-4">
          <PanelFiltrosServicios
            :estado="filtros"
            :tipos="tipos"
            :medios="medios"
            :etiquetas="etiquetas"
            :categorias="categorias"
            :moneda="monedaUsuario"
            :cargando="cargando"
            :facetas="facetas"
            @actualizar="actualizarFiltros"
            @limpiar="limpiarFiltros"
          />
        </div>

        <div class="border-t border-borde/50 p-4">
          <button
            type="button"
            class="w-full rounded-full bg-coral px-5 py-2.5 text-sm font-bold text-white transition hover:bg-rojizo"
            @click="filtrosAbiertos = false"
          >
            {{ cargando
              ? 'Cargando...'
              : `Ver ${totalElementos} ${totalElementos === 1 ? 'servicio' : 'servicios'}` }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
