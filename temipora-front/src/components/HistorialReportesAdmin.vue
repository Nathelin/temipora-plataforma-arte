<script setup>
import { onMounted, ref, watch } from 'vue'
import {
  cargarHistorialReportes,
  etiquetaAccionReporte,
  etiquetaEstadoReporte,
  etiquetaMotivoReporte,
} from '../servicios/resenias'

const filtros = [
  { valor: 'DESCARTADO', etiqueta: 'Descartados' },
  { valor: 'RESUELTO', etiqueta: 'Resueltos' },
  { valor: '', etiqueta: 'Todos' },
]

const estado = ref('DESCARTADO')
const contenido = ref([])
const pagina = ref(0)
const totalPaginas = ref(0)
const totalElementos = ref(0)
const hayMas = ref(false)
const cargando = ref(true)
const error = ref('')

async function cargar(nuevaPagina = 0) {
  cargando.value = true
  error.value = ''
  try {
    const params = { pagina: nuevaPagina, tamanio: 10 }
    if (estado.value) params.estado = estado.value
    const res = await cargarHistorialReportes(params)
    const data = res.data || {}
    contenido.value = data.contenido || []
    pagina.value = data.pagina || 0
    totalPaginas.value = data.totalPaginas || 0
    totalElementos.value = data.totalElementos || 0
    hayMas.value = Boolean(data.hayMas)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar el historial de reportes.'
  } finally {
    cargando.value = false
  }
}

watch(estado, () => {
  totalElementos.value = 0
  cargar(0)
})

onMounted(() => cargar())

function formatearFecha(valor) {
  if (!valor) return '—'
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return '—'
  return new Intl.DateTimeFormat('es', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}

function nombreReportante(reporte) {
  const nombre = reporte.reportante?.nombreUsuario
  return nombre ? `@${nombre}` : 'Usuario'
}

function nombreResolutor(reporte) {
  const nombre = reporte.resueltoPor?.nombreUsuario
  return nombre ? `@${nombre}` : 'Sin datos'
}

function claseEstadoReporte(estadoReporte) {
  if (estadoReporte === 'PENDIENTE') return 'border-durazno bg-durazno/30 text-granate'
  if (estadoReporte === 'RESUELTO') return 'border-green-700/25 bg-green-500/10 text-green-700'
  return 'border-cafe/15 bg-cafe/5 text-cafe/60'
}

function claseAccion(accion) {
  return accion === 'OCULTAR'
    ? 'border-rojizo/30 bg-rojizo/10 text-rojizo'
    : 'border-cafe/15 bg-cafe/5 text-cafe/60'
}
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comunidad"
      titulo="Historial de reportes"
      descripcion="Los reportes descartados o resueltos no se borran: quedan registrados con quién los reportó, qué administrador los resolvió y cuándo."
    />

    <AdminTabs />

    <div class="mb-4 flex flex-wrap gap-2" role="tablist" aria-label="Filtrar historial por estado">
      <button
        v-for="filtro in filtros"
        :key="filtro.valor || 'todos'"
        type="button"
        role="tab"
        :aria-selected="estado === filtro.valor"
        class="inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition"
        :class="estado === filtro.valor
          ? 'border-coral bg-coral/10 font-semibold text-granate'
          : 'border-rosa bg-blanco text-cafe/60 hover:bg-rosa/20'"
        @click="estado = filtro.valor"
      >
        {{ filtro.etiqueta }}
        <span
          v-if="estado === filtro.valor && totalElementos"
          class="rounded-full bg-coral/20 px-2 py-0.5 text-[11px] font-bold text-granate"
        >
          {{ totalElementos }}
        </span>
      </button>
    </div>



    <p v-if="cargando" class="py-10 text-center text-sm text-cafe/55">Cargando historial...</p>
    <p v-else-if="error" class="py-10 text-center text-sm text-rojizo" role="alert">{{ error }}</p>
    <p v-else-if="!contenido.length" class="rounded-2xl border border-dashed border-rosa/60 py-10 text-center text-sm text-cafe/55">
      No hay reportes para este filtro.
    </p>

    <div v-else class="space-y-3">
      <article
        v-for="reporte in contenido"
        :key="reporte.idReporte"
        class="rounded-2xl border border-rosa/50 bg-blanco p-5 shadow-sm"
      >
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                :class="claseEstadoReporte(reporte.estado)"
              >
                {{ etiquetaEstadoReporte(reporte.estado) }}
              </span>
              <span
                v-if="reporte.accion"
                class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                :class="claseAccion(reporte.accion)"
              >
                {{ etiquetaAccionReporte(reporte.accion) }}
              </span>
              <p class="text-sm font-semibold text-granate">
                {{ reporte.etiquetaMotivo || etiquetaMotivoReporte(reporte.motivo) }}
              </p>
            </div>
            <p class="mt-1 text-xs text-cafe/60">
              {{ formatearFecha(reporte.fechaCreacion) }}
              · {{ reporte.nombreProducto || reporte.nombreServicio || 'Reseña' }}
              <template v-if="reporte.idProducto">
                · Producto #{{ reporte.idProducto }}
              </template>
              <template v-else>
                · Pedido #{{ reporte.idPedido }}
              </template>
            </p>
            <p class="mt-1 text-xs text-cafe/60">
              Reseña de
              {{ reporte.autorResenia ? `@${reporte.autorResenia}` : 'usuario' }}
              <span v-if="reporte.reseniaAnonima">(publicada como anónima)</span>
            </p>
          </div>

          <div class="rounded-xl bg-rosa/10 px-3 py-2 text-xs leading-relaxed text-cafe/70">
            <p><span class="text-cafe/45">Reportó:</span> {{ nombreReportante(reporte) }}</p>
            <p>
              <span class="text-cafe/45">Resolvió:</span>
              <strong class="text-granate">{{ nombreResolutor(reporte) }}</strong>
            </p>
            <p>
              <span class="text-cafe/45">Fecha de resolución:</span>
              {{ formatearFecha(reporte.fechaResolucion) }}
            </p>
          </div>
        </div>

        <p
          v-if="reporte.comentario"
          class="mt-3 rounded-lg bg-rosa/5 px-3 py-2 text-sm leading-relaxed text-cafe/75"
        >
          {{ reporte.comentario }}
        </p>
      </article>
    </div>

    <div
      v-if="!cargando && !error && totalPaginas > 1"
      class="mt-6 flex items-center justify-between gap-3 border-t border-rosa/40 pt-4"
    >
      <p class="text-xs text-cafe/50">
        Página {{ pagina + 1 }} de {{ totalPaginas }}
      </p>
      <div class="flex gap-2">
        <button
          type="button"
          class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
          :disabled="pagina <= 0"
          @click="cargar(pagina - 1)"
        >
          Anterior
        </button>
        <button
          type="button"
          class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
          :disabled="!hayMas"
          @click="cargar(pagina + 1)"
        >
          Siguiente
        </button>
      </div>
    </div>
  </div>
</template>
