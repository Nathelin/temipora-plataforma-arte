<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import api from '../servicios/api'

const ETIQUETAS_ACCION = {
  ETIQUETA_CREAR: 'Creó una etiqueta',
  ETIQUETA_MODIFICAR: 'Modificó una etiqueta',
  ETIQUETA_APROBAR: 'Aprobó una etiqueta',
  ETIQUETA_RECHAZAR: 'Rechazó una etiqueta',
  ETIQUETA_BAJA: 'Dio de baja una etiqueta',
  ETIQUETA_REACTIVAR: 'Reactivó una etiqueta',
  CATEGORIA_CREAR: 'Creó una categoría',
  CATEGORIA_MODIFICAR: 'Modificó una categoría',
  CATEGORIA_BAJA: 'Dio de baja una categoría',
  CATEGORIA_REACTIVAR: 'Reactivó una categoría',
  MEDIO_CREAR: 'Creó un medio',
  MEDIO_MODIFICAR: 'Modificó un medio',
  MEDIO_BAJA: 'Dio de baja un medio',
  MEDIO_REACTIVAR: 'Reactivó un medio',
  MATERIAL_CREAR: 'Creó un material',
  MATERIAL_MODIFICAR: 'Modificó un material',
  MATERIAL_BAJA: 'Dio de baja un material',
  MATERIAL_REACTIVAR: 'Reactivó un material',
  TIPO_ARTE_CREAR: 'Creó un tipo de arte',
  TIPO_ARTE_MODIFICAR: 'Modificó un tipo de arte',
  TIPO_ARTE_BAJA: 'Dio de baja un tipo de arte',
  TIPO_ARTE_REACTIVAR: 'Reactivó un tipo de arte',
  TIPO_PRODUCTO_CREAR: 'Creó un tipo de producto',
  TIPO_PRODUCTO_MODIFICAR: 'Modificó un tipo de producto',
  TIPO_PRODUCTO_BAJA: 'Dio de baja un tipo de producto',
  TIPO_PRODUCTO_REACTIVAR: 'Reactivó un tipo de producto',
  TIPO_SERVICIO_CREAR: 'Creó un tipo de servicio',
  TIPO_SERVICIO_MODIFICAR: 'Modificó un tipo de servicio',
  TIPO_SERVICIO_BAJA: 'Dio de baja un tipo de servicio',
  TIPO_SERVICIO_REACTIVAR: 'Reactivó un tipo de servicio',
  PAIS_CREAR: 'Creó un país',
  PAIS_MODIFICAR: 'Modificó un país',
  PAIS_BAJA: 'Dio de baja un país',
  PAIS_REACTIVAR: 'Reactivó un país',
  MONEDA_CREAR: 'Creó una moneda',
  MONEDA_MODIFICAR: 'Modificó una moneda',
  MONEDA_BAJA: 'Dio de baja una moneda',
  MONEDA_REACTIVAR: 'Reactivó una moneda',
  MONEDA_PREDETERMINADA: 'Marcó la moneda predeterminada',
  COMISION_ACTUALIZAR: 'Actualizó las comisiones',
  PRESUPUESTO_CONFIG_ACTUALIZAR: 'Actualizó la vigencia de presupuestos',
  PEDIDO_CONFIG_ACTUALIZAR: 'Actualizó las reglas de pedidos e hitos',
  MENSAJERIA_CONFIG_ACTUALIZAR: 'Actualizó la configuración de mensajería',
  RESENIA_CONFIG_ACTUALIZAR: 'Actualizó la configuración de reseñas',
  INACTIVIDAD_CONFIG_ACTUALIZAR: 'Actualizó las promociones por inactividad',
  REACTIVACION_EJECUTAR: 'Ejecutó las promociones por inactividad',
  COTIZACION_ACTUALIZAR: 'Actualizó la cotización',
  COTIZACION_REFRESCAR: 'Actualizó la cotización desde la API',
  RESENIA_OCULTAR: 'Ocultó una reseña',
  RESENIA_RESTAURAR: 'Restauró una reseña',
  RESENIA_DESCARTAR_REPORTES: 'Descartó reportes de reseña',
  TERMINOS_PUBLICAR: 'Publicó una versión de los términos',
  SERVICIO_RESTAURAR: 'Restauró un servicio archivado',
  PEDIDO_PROCESAR: 'Procesó hitos de pedidos',
  CRITERIO_CREAR: 'Creó un criterio de evaluación',
  CRITERIO_MODIFICAR: 'Modificó un criterio de evaluación',
  CRITERIO_BAJA: 'Dio de baja un criterio de evaluación',
  CRITERIO_REACTIVAR: 'Reactivó un criterio de evaluación',
  CRITERIO_OPCION_CREAR: 'Agregó una opción a un criterio',
  CRITERIO_OPCION_MODIFICAR: 'Modificó una opción de un criterio',
  CRITERIO_OPCION_BAJA: 'Dio de baja una opción de un criterio',
  CRITERIO_OPCION_REACTIVAR: 'Reactivó una opción de un criterio',
}

const ETIQUETAS_ENTIDAD = {
  Etiqueta: 'Etiqueta',
  CategoriaEtiqueta: 'Categoría de etiqueta',
  Medio: 'Medio',
  Material: 'Material',
  TipoArte: 'Tipo de arte',
  TipoProducto: 'Tipo de producto',
  TipoServicio: 'Tipo de servicio',
  Pais: 'País',
  Moneda: 'Moneda',
  ConfiguracionComision: 'Comisión',
  ConfiguracionPresupuesto: 'Reglas de presupuestos',
  ConfiguracionPedido: 'Reglas de pedidos',
  ConfiguracionMensajeria: 'Mensajería',
  ConfiguracionResenia: 'Configuración de reseñas',
  ConfiguracionInactividad: 'Promociones',
  Resenia: 'Reseña',
  TerminosPlataforma: 'Términos de la plataforma',
  Servicio: 'Servicio',
  CriterioResenia: 'Criterio de evaluación',
  ConfiguracionCotizacion: 'Cotización',
  Pedido: 'Pedido',
}

const eventos = ref([])
const opciones = ref({ entidades: [], acciones: [] })
const cargando = ref(true)
const error = ref('')
const pagina = ref(0)
const totalPaginas = ref(0)
const totalElementos = ref(0)
const hayMas = ref(false)
const abiertos = ref(new Set())

const filtroActor = ref('')
const filtroEntidad = ref('')
const filtroAccion = ref('')
const filtroDesde = ref('')
const filtroHasta = ref('')

let debounceTimer

function params(nuevaPagina = 0) {
  const valores = {
    actor: filtroActor.value.trim() || undefined,
    entidad: filtroEntidad.value || undefined,
    accion: filtroAccion.value || undefined,
    desde: filtroDesde.value || undefined,
    hasta: filtroHasta.value || undefined,
    pagina: nuevaPagina,
    tamanio: 20,
  }
  return valores
}

async function cargar(nuevaPagina = 0) {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/auditoria', { params: params(nuevaPagina) })
    const data = res.data ?? {}
    eventos.value = data.contenido ?? []
    pagina.value = data.pagina ?? 0
    totalPaginas.value = data.totalPaginas ?? 0
    totalElementos.value = data.totalElementos ?? 0
    hayMas.value = Boolean(data.hayMas)
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar la auditoría.'
  } finally {
    cargando.value = false
  }
}

async function cargarOpciones() {
  try {
    const res = await api.get('/admin/auditoria/opciones')
    opciones.value = {
      entidades: res.data?.entidades ?? [],
      acciones: res.data?.acciones ?? [],
    }
  } catch {
    opciones.value = { entidades: [], acciones: [] }
  }
}

watch([filtroEntidad, filtroAccion, filtroDesde, filtroHasta], () => cargar(0))
watch(filtroActor, () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => cargar(0), 300)
})

function limpiar() {
  filtroActor.value = ''
  filtroEntidad.value = ''
  filtroAccion.value = ''
  filtroDesde.value = ''
  filtroHasta.value = ''
  abiertos.value = new Set()
}

function alternar(id) {
  const copia = new Set(abiertos.value)
  if (copia.has(id)) copia.delete(id)
  else copia.add(id)
  abiertos.value = copia
}

const estaAbierto = (id) => abiertos.value.has(id)

function formatearFecha(valor) {
  if (!valor) return '—'
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return '—'
  return new Intl.DateTimeFormat('es', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}

function nombreAccion(codigo) {
  return ETIQUETAS_ACCION[codigo] || codigo
}

function nombreEntidad(codigo) {
  if (!codigo) return '—'
  return ETIQUETAS_ENTIDAD[codigo] || codigo
}

function resumenCambios(evento) {
  const cambios = evento.cambios || []
  if (!cambios.length) return 'Sin cambios de campos'
  if (cambios.length === 1) {
    const [c] = cambios
    return `${c.etiqueta}: ${c.anterior ?? '—'} → ${c.nuevo ?? '—'}`
  }
  return `${cambios.length} campos modificados`
}

function actorVisible(evento) {
  if (!evento.actorNombre) return evento.origen === 'SISTEMA' ? 'Sistema' : 'Sin actor'
  return `@${evento.actorNombre}`
}

onMounted(() => {
  cargar(0)
  cargarOpciones()
})

onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Sistema"
      titulo="Auditoría"
      descripcion="Quién hizo qué, sobre qué registro y con qué valores anteriores y nuevos."
    />

    <div class="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
      <div>
        <label class="mb-1 block text-xs text-cafe/60">Actor</label>
        <input
          v-model="filtroActor"
          type="search"
          placeholder="Usuario"
          class="block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        />
      </div>
      <div>
        <label class="mb-1 block text-xs text-cafe/60">Entidad</label>
        <select
          v-model="filtroEntidad"
          class="block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        >
          <option value="">Todas</option>
          <option v-for="entidad in opciones.entidades" :key="entidad" :value="entidad">
            {{ nombreEntidad(entidad) }}
          </option>
        </select>
      </div>
      <div>
        <label class="mb-1 block text-xs text-cafe/60">Acción</label>
        <select
          v-model="filtroAccion"
          class="block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        >
          <option value="">Todas</option>
          <option v-for="accion in opciones.acciones" :key="accion" :value="accion">
            {{ nombreAccion(accion) }}
          </option>
        </select>
      </div>
      <div>
        <label class="mb-1 block text-xs text-cafe/60">Desde</label>
        <input
          v-model="filtroDesde"
          type="date"
          class="block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        />
      </div>
      <div class="flex items-end gap-2">
        <div class="min-w-0 flex-1">
          <label class="mb-1 block text-xs text-cafe/60">Hasta</label>
          <input
            v-model="filtroHasta"
            type="date"
            class="block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          />
        </div>
        <button
          type="button"
          class="inline-flex min-h-10 shrink-0 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 motion-reduce:transition-none"
          @click="limpiar"
        >
          Limpiar
        </button>
      </div>
    </div>

    <p v-if="cargando" class="py-10 text-center text-sm text-cafe/55">Cargando auditoría...</p>
    <p v-else-if="error" class="py-10 text-center text-sm text-rojizo" role="alert">{{ error }}</p>
    <p
      v-else-if="!eventos.length"
      class="rounded-2xl border border-dashed border-rosa/60 py-10 text-center text-sm text-cafe/55"
    >
      No hay acciones registradas con estos filtros.
    </p>

    <div v-else class="space-y-2">
      <article
        v-for="evento in eventos"
        :key="evento.idEventoAuditoria"
        class="rounded-2xl border border-rosa/50 bg-blanco"
      >
        <button
          type="button"
          class="flex w-full items-start gap-3 rounded-2xl px-4 py-3 text-left transition-colors hover:bg-rosa/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60 motion-reduce:transition-none"
          :aria-expanded="estaAbierto(evento.idEventoAuditoria)"
          :aria-controls="`evento-${evento.idEventoAuditoria}`"
          @click="alternar(evento.idEventoAuditoria)"
        >
          <svg
            class="mt-0.5 h-4 w-4 shrink-0 text-cafe/40 transition-transform duration-200 motion-reduce:transition-none"
            :class="estaAbierto(evento.idEventoAuditoria) ? 'rotate-90' : ''"
            viewBox="0 0 20 20"
            fill="currentColor"
            aria-hidden="true"
          >
            <path
              fill-rule="evenodd"
              d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z"
              clip-rule="evenodd"
            />
          </svg>

          <span class="min-w-0 flex-1">
            <span class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
              <span class="text-sm font-semibold text-granate">{{ evento.resumen }}</span>
              <span class="text-xs text-cafe/45">{{ formatearFecha(evento.ocurridoEn) }}</span>
            </span>
            <span class="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-cafe/60">
              <span class="font-medium text-cafe/75">{{ actorVisible(evento) }}</span>
              <span v-if="evento.actorRol" class="rounded-full bg-rosa/20 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-granate">
                {{ evento.actorRol }}
              </span>
              <span v-if="evento.entidad">· {{ nombreEntidad(evento.entidad) }}<span v-if="evento.entidadId"> #{{ evento.entidadId }}</span></span>
              <span class="text-cafe/40">·</span>
              <span class="text-cafe/50">{{ resumenCambios(evento) }}</span>
            </span>
          </span>
        </button>

        <div
          v-if="estaAbierto(evento.idEventoAuditoria)"
          :id="`evento-${evento.idEventoAuditoria}`"
          class="border-t border-rosa/40 px-4 py-4"
        >
          <p class="mb-1 text-xs font-semibold uppercase tracking-wide text-cafe/50">
            {{ nombreAccion(evento.accion) }}
          </p>

          <div v-if="evento.cambios.length" class="overflow-x-auto">
            <table class="w-full min-w-96 text-left text-sm">
              <thead>
                <tr class="border-b border-rosa/40 text-xs uppercase tracking-wide text-cafe/50">
                  <th class="py-2 pr-4 font-semibold">Campo</th>
                  <th class="py-2 pr-4 font-semibold">Antes</th>
                  <th class="py-2 font-semibold">Después</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="cambio in evento.cambios"
                  :key="cambio.campo"
                  class="border-b border-rosa/25 last:border-0"
                >
                  <td class="py-2 pr-4 text-cafe/70">{{ cambio.etiqueta }}</td>
                  <td class="py-2 pr-4 text-cafe/50">{{ cambio.anterior ?? '—' }}</td>
                  <td class="py-2 font-medium text-granate">{{ cambio.nuevo ?? '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="text-sm text-cafe/55">Esta acción no modificó campos: solo registró el evento.</p>

          <dl class="mt-4 grid gap-2 text-xs text-cafe/50 sm:grid-cols-2">
            <div v-if="evento.motivo">
              <dt class="font-semibold text-cafe/60">Motivo</dt>
              <dd>{{ evento.motivo }}</dd>
            </div>
            <div v-if="evento.ip">
              <dt class="font-semibold text-cafe/60">IP</dt>
              <dd>{{ evento.ip }}</dd>
            </div>
            <div v-if="evento.requestId">
              <dt class="font-semibold text-cafe/60">Identificador de la acción</dt>
              <dd class="break-all">{{ evento.requestId }}</dd>
            </div>
            <div v-if="evento.userAgent">
              <dt class="font-semibold text-cafe/60">Navegador</dt>
              <dd class="break-all">{{ evento.userAgent }}</dd>
            </div>
          </dl>
        </div>
      </article>
    </div>

    <div
      v-if="!cargando && !error && totalPaginas > 1"
      class="mt-6 flex items-center justify-between gap-3 border-t border-rosa/40 pt-4"
    >
      <p class="text-xs text-cafe/50 tabular-nums">
        Página {{ pagina + 1 }} de {{ totalPaginas }} · {{ totalElementos }} eventos
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
