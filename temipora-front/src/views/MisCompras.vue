<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import FilaCompra from '../components/FilaCompra.vue'
import PageHeader from '../components/PageHeader.vue'
import { useNotificaciones } from '../composables/useNotificaciones'
import {
  GRUPOS_COMPRA,
  contarCompras,
  descargarArchivoCompra,
  grupoCompraDeQuery,
  listarCompras,
} from '../servicios/compras'
import { guardarArchivo } from '../servicios/descargas'

const route = useRoute()
const router = useRouter()
const { error: toastError } = useNotificaciones()

const CONTADORES_VACIOS = { todas: 0, pendientes: 0, pagadas: 0, archivo: 0 }

const compras = ref([])
const contadores = ref({ ...CONTADORES_VACIOS })
const grupoActivo = ref(grupoCompraDeQuery(String(route.query.grupo || '')))
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')
const pagina = ref(0)
const hayMas = ref(false)
const total = ref(0)
const descargandoItem = ref(null)

const emptyState = computed(() => {
  if (grupoActivo.value === 'pagadas') {
    return {
      titulo: 'Todavía no tenés compras pagadas',
      detalle: 'Cuando completes el pago de una compra, va a aparecer acá.',
    }
  }
  if (grupoActivo.value === 'archivo') {
    return {
      titulo: 'El archivo está vacío',
      detalle: 'Acá van a quedar las compras vencidas o canceladas.',
    }
  }
  if (grupoActivo.value === 'pendientes') {
    return {
      titulo: 'No tenés compras pendientes',
      detalle: 'Cuando tengas una compra por pagar, va a aparecer acá.',
    }
  }
  return {
    titulo: 'Todavía no hiciste compras de productos',
    detalle: 'Cuando compres una pieza o licencia, va a aparecer acá.',
  }
})

async function cargar({ append = false } = {}) {
  const siguiente = append ? pagina.value + 1 : 0
  if (append) cargandoMas.value = true
  else cargando.value = true
  error.value = ''
  try {
    const data = await listarCompras({
      pagina: siguiente,
      tamanio: 12,
      grupo: grupoActivo.value,
    })
    const contenido = data.contenido || []
    compras.value = append ? [...compras.value, ...contenido] : contenido
    pagina.value = data.pagina ?? siguiente
    hayMas.value = Boolean(data.hayMas)
    total.value = Number(data.totalElementos || compras.value.length)
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar tus compras.'
  } finally {
    cargando.value = false
    cargandoMas.value = false
  }
}

async function cargarContadores() {
  try {
    const data = await contarCompras()
    contadores.value = {
      todas: Number(data?.todas || 0),
      pendientes: Number(data?.pendientes || 0),
      pagadas: Number(data?.pagadas || 0),
      archivo: Number(data?.archivo || 0),
    }
  } catch {
    contadores.value = { ...CONTADORES_VACIOS }
  }
}

function contarGrupo(grupo) {
  return contadores.value[grupo.clave] || 0
}

function cambiarGrupo(clave) {
  if (grupoActivo.value === clave) return
  grupoActivo.value = clave
  pagina.value = 0
  compras.value = []
  hayMas.value = false
  router.replace({ query: { ...route.query, grupo: clave } })
  cargar()
}

function cargarMas() {
  if (cargandoMas.value || !hayMas.value) return
  cargar({ append: true })
}

async function descargar(preview, compra) {
  if (descargandoItem.value) return
  descargandoItem.value = preview.idCompraItem
  try {
    const blob = await descargarArchivoCompra(compra.idCompra, preview.idCompraItem)
    const nombre = `${preview.tituloProducto}-${preview.nombreVariante || ''}`.trim()
    guardarArchivo(blob, nombre)
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo descargar el archivo.')
  } finally {
    descargandoItem.value = null
  }
}

onMounted(() => {
  cargar()
  cargarContadores()
})
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-4 py-8 sm:px-8 sm:py-10">
    <PageHeader
      eyebrow="Tu actividad"
      titulo="Mis compras"
      descripcion="Tus compras de productos de la tienda."
    >
      <template #acciones>
        <span
          v-if="!cargando && !error"
          class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral"
        >
          {{ total }} {{ total === 1 ? 'compra' : 'compras' }}
        </span>
        <router-link
          to="/productos"
          class="inline-flex items-center text-sm font-semibold text-coral transition hover:text-rojizo"
        >
          Explorar productos →
        </router-link>
      </template>
    </PageHeader>

    <div
      class="mt-5 flex gap-1 overflow-x-auto rounded-xl border border-rosa/60 bg-blanco p-1"
      role="group"
      aria-label="Filtrar compras por estado"
    >
      <button
        v-for="grupo in GRUPOS_COMPRA"
        :key="grupo.clave"
        type="button"
        class="flex shrink-0 items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition"
        :class="grupoActivo === grupo.clave
          ? 'bg-coral text-white shadow-sm'
          : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
        :aria-pressed="grupoActivo === grupo.clave"
        @click="cambiarGrupo(grupo.clave)"
      >
        {{ grupo.texto }}
        <span
          class="rounded-full px-1.5 py-0.5 text-[11px] leading-none"
          :class="grupoActivo === grupo.clave ? 'bg-white/20 text-white' : 'bg-rosa/30 text-granate'"
        >
          {{ contarGrupo(grupo) }}
        </span>
      </button>
    </div>

    <div v-if="cargando" class="mt-6 space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando compras...</span>
      <div
        v-for="n in 3"
        :key="n"
        class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"
      ></div>
    </div>

    <p
      v-else-if="error && !compras.length"
      class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center text-sm text-rojizo"
    >
      {{ error }}
    </p>

    <EmptyState
      v-else-if="!compras.length"
      :titulo="emptyState.titulo"
      :detalle="emptyState.detalle"
    >
      <template #accion>
        <button
          v-if="grupoActivo !== 'todas'"
          type="button"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
          @click="cambiarGrupo('todas')"
        >
          Ver todas
        </button>
        <router-link
          v-else
          to="/productos"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
        >
          Explorar productos
        </router-link>
      </template>
    </EmptyState>

    <p
      v-if="error && compras.length"
      class="mt-6 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
    >
      {{ error }}
    </p>

    <div v-if="!cargando && compras.length" class="mt-6 space-y-3">
      <FilaCompra
        v-for="compra in compras"
        :key="compra.idCompra"
        :compra="compra"
        :descargando="descargandoItem"
        @descargar="descargar($event, compra)"
      />

      <div v-if="hayMas" class="pt-2 text-center">
        <button
          type="button"
          :disabled="cargandoMas"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
          @click="cargarMas"
        >
          {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
        </button>
      </div>
    </div>
  </div>
</template>
