<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import SerieVentas from '../components/SerieVentas.vue'
import { useNotificaciones } from '../composables/useNotificaciones'
import { guardarArchivo } from '../servicios/descargas'
import { descargarInformeVentasPdf, obtenerInformeVentas } from '../servicios/informes'
import { formatearPrecio } from '../servicios/moneda'

const { exito, error: notificarError } = useNotificaciones()
const route = useRoute()

const MAXIMO_DIAS_DIARIO = 366

const PRESETS = [
  { clave: '7d', texto: 'Últimos 7 días' },
  { clave: '30d', texto: 'Últimos 30 días' },
  { clave: '90d', texto: 'Últimos 90 días' },
  { clave: 'mes', texto: 'Este mes' },
  { clave: 'mes-anterior', texto: 'Mes anterior' },
  { clave: 'anio', texto: 'Este año' },
]

function iso(fecha) {
  const anio = fecha.getFullYear()
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${anio}-${mes}-${dia}`
}

function sumarDias(base, dias) {
  const fecha = new Date(base.getFullYear(), base.getMonth(), base.getDate())
  fecha.setDate(fecha.getDate() + dias)
  return fecha
}

const hoy = new Date()
const desde = ref(iso(sumarDias(hoy, -29)))
const hasta = ref(iso(hoy))
const tipo = ref('TODOS')
const agrupacion = ref('DIA')
const preset = ref('30d')

const informe = ref(null)
const cargando = ref(true)
const error = ref('')
const descargando = ref(false)

const dias = computed(() => {
  const inicio = new Date(`${desde.value}T00:00:00`)
  const fin = new Date(`${hasta.value}T00:00:00`)
  if (Number.isNaN(inicio.getTime()) || Number.isNaN(fin.getTime())) return 0
  return Math.max(1, Math.round((fin - inicio) / 86400000) + 1)
})

const rangoInvalido = computed(
  () => !desde.value || !hasta.value || desde.value > hasta.value,
)
const puedeDiario = computed(() => dias.value <= MAXIMO_DIAS_DIARIO)

const moneda = computed(() => informe.value?.moneda || 'ARS')
const totales = computed(() => informe.value?.totales || null)
const items = computed(() => informe.value?.items || [])
const mostrarTipo = computed(() => tipo.value === 'TODOS')
const hayProductos = computed(
  () => items.value.some((item) => item.tipo === 'PRODUCTO'),
)

const tarjetas = computed(() => {
  const t = totales.value
  if (!t) return []
  return [
    {
      etiqueta: 'Neto percibido',
      valor: formatearPrecio(t.netoPercibido, moneda.value),
      detalle: 'Lo que liquidó el medio de pago.',
      destacada: true,
    },
    {
      etiqueta: 'Ventas brutas',
      valor: formatearPrecio(t.bruto, moneda.value),
      detalle: 'Precio pagado por los clientes.',
    },
    {
      etiqueta: 'Ticket promedio',
      valor: formatearPrecio(t.ticketPromedio, moneda.value),
      detalle: 'Neto por operación.',
    },
  ]
})

const variacion = computed(
  () => informe.value?.comparacion?.variacionNetoPorcentaje,
)

const textoVariacion = computed(() => {
  const valor = variacion.value
  if (valor === null || valor === undefined) {
    return 'Sin ventas en el período anterior'
  }
  const numero = Number(valor)
  const signo = numero >= 0 ? '+' : ''
  return `${signo}${numero.toLocaleString('es-AR', { maximumFractionDigits: 2 })}% vs. período anterior`
})

const claseVariacion = computed(() => {
  const valor = variacion.value
  if (valor === null || valor === undefined) return 'text-cafe/55'
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return 'text-cafe/55'
  return numero >= 0 ? 'text-green-700' : 'text-rojizo'
})

const rangoComparacion = computed(() => {
  const comparacion = informe.value?.comparacion
  if (!comparacion) return ''
  return `Del ${formatearFechaSimple(comparacion.desde)} al ${formatearFechaSimple(comparacion.hasta)}`
})

function formatearFechaSimple(valor) {
  if (!valor) return ''
  const [anio, mes, dia] = String(valor).split('-')
  return `${dia}/${mes}/${anio}`
}

function ajustarAgrupacion() {
  if (!puedeDiario.value && agrupacion.value === 'DIA') {
    agrupacion.value = 'SEMANA'
  }
}

function aplicarPreset(clave) {
  const ahora = new Date()
  preset.value = clave

  if (clave === '7d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -6))
  } else if (clave === '30d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -29))
  } else if (clave === '90d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -89))
  } else if (clave === 'mes') {
    desde.value = iso(new Date(ahora.getFullYear(), ahora.getMonth(), 1))
    hasta.value = iso(ahora)
  } else if (clave === 'mes-anterior') {
    desde.value = iso(new Date(ahora.getFullYear(), ahora.getMonth() - 1, 1))
    hasta.value = iso(new Date(ahora.getFullYear(), ahora.getMonth(), 0))
  } else if (clave === 'anio') {
    desde.value = iso(new Date(ahora.getFullYear(), 0, 1))
    hasta.value = iso(ahora)
  }

  ajustarAgrupacion()
  cargar()
}

function cambiarFechas() {
  preset.value = 'personalizado'
  ajustarAgrupacion()
  cargar()
}

const parametros = () => ({
  desde: desde.value,
  hasta: hasta.value,
  tipo: tipo.value,
  agrupacion: agrupacion.value,
})

async function cargar() {
  if (rangoInvalido.value) {
    error.value = 'La fecha de inicio debe ser anterior a la de fin.'
    return
  }
  cargando.value = true
  error.value = ''
  try {
    informe.value = await obtenerInformeVentas(parametros())
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo generar el informe.'
  } finally {
    cargando.value = false
  }
}

async function descargar() {
  if (descargando.value || rangoInvalido.value) return
  descargando.value = true
  try {
    const { blob, nombre } = await descargarInformeVentasPdf(parametros())
    guardarArchivo(blob, nombre)
    exito('Descargamos el informe en PDF.')
  } catch {
    notificarError('No se pudo descargar el informe.')
  } finally {
    descargando.value = false
  }
}

function aplicarQueryInicial() {
  const mes = String(route.query.mes || '')
  if (/^\d{4}-\d{2}$/.test(mes)) {
    const [anio, numeroMes] = mes.split('-').map(Number)
    desde.value = iso(new Date(anio, numeroMes - 1, 1))
    hasta.value = iso(new Date(anio, numeroMes, 0))
    preset.value = 'personalizado'
  }

  const tipoQuery = String(route.query.tipo || '').toUpperCase()
  if (['TODOS', 'PRODUCTOS', 'SERVICIOS'].includes(tipoQuery)) {
    tipo.value = tipoQuery
  }

  ajustarAgrupacion()
  cargar()
}

onMounted(aplicarQueryInicial)
</script>

<template>
  <PageHeader
    eyebrow="Informes"
    titulo="Informe de ventas"
    descripcion="Elegí un período para ver cuánto vendiste por producto y servicio, con el neto que liquidó el medio de pago."
  >
    <template #acciones>
      <router-link
        :to="{ name: 'panel-ventas' }"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
      >
        Volver a ventas
      </router-link>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
        :disabled="descargando || cargando || rangoInvalido"
        @click="descargar"
      >
        {{ descargando ? 'Generando PDF...' : 'Descargar PDF' }}
      </button>
    </template>
  </PageHeader>

  <form
    class="mt-6 rounded-2xl border border-rosa/60 bg-blanco p-4"
    @submit.prevent="cargar"
  >
    <div class="flex flex-wrap gap-2">
      <button
        v-for="opcion in PRESETS"
        :key="opcion.clave"
        type="button"
        class="inline-flex min-h-9 items-center justify-center rounded-full border px-3 text-xs font-semibold transition"
        :class="preset === opcion.clave
          ? 'border-coral bg-coral text-white'
          : 'border-rosa bg-blanco text-granate hover:bg-rosa/20'"
        @click="aplicarPreset(opcion.clave)"
      >
        {{ opcion.texto }}
      </button>
    </div>

    <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
        Desde
        <input
          v-model="desde"
          type="date"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @change="cambiarFechas"
        />
      </label>

      <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
        Hasta
        <input
          v-model="hasta"
          type="date"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @change="cambiarFechas"
        />
      </label>

      <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
        Tipo
        <select
          v-model="tipo"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @change="cargar"
        >
          <option value="TODOS">Productos y servicios</option>
          <option value="PRODUCTOS">Solo productos</option>
          <option value="SERVICIOS">Solo servicios</option>
        </select>
      </label>

      <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
        Agrupación
        <select
          v-model="agrupacion"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @change="cargar"
        >
          <option value="DIA" :disabled="!puedeDiario">Por día</option>
          <option value="SEMANA">Por semana</option>
          <option value="MES">Por mes</option>
        </select>
      </label>
    </div>

    <p v-if="!puedeDiario" class="mt-3 text-xs text-cafe/55">
      Para períodos de más de {{ MAXIMO_DIAS_DIARIO }} días la serie se agrupa por semana o por mes.
    </p>
  </form>

  <div v-if="cargando" class="mt-6 min-h-[calc(100vh-4rem)] space-y-3" role="status" aria-live="polite">
    <span class="sr-only">Generando el informe...</span>
    <div class="grid gap-3 sm:grid-cols-3">
      <div
        v-for="n in 3"
        :key="n"
        class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"
      ></div>
    </div>
    <div class="h-64 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"></div>
  </div>

  <div
    v-else-if="error"
    class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center"
  >
    <p class="text-sm text-rojizo">{{ error }}</p>
    <button
      type="button"
      class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
      @click="cargar"
    >
      Reintentar
    </button>
  </div>

  <template v-else-if="informe">
    <div class="mt-6 grid gap-3 sm:grid-cols-3">
      <div
        v-for="tarjeta in tarjetas"
        :key="tarjeta.etiqueta"
        class="rounded-2xl border border-rosa/60 bg-blanco p-4"
        :class="tarjeta.destacada ? 'ring-1 ring-coral/40' : ''"
      >
        <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">
          {{ tarjeta.etiqueta }}
        </span>
        <p class="mt-2 font-display text-2xl font-extrabold tabular-nums text-granate">
          {{ tarjeta.valor }}
        </p>
        <p class="mt-1 text-[11px] text-cafe/55">{{ tarjeta.detalle }}</p>
      </div>
    </div>

    <div class="mt-4 flex flex-wrap items-baseline gap-x-3 gap-y-1 text-sm">
      <span class="font-semibold" :class="claseVariacion">{{ textoVariacion }}</span>
      <span class="text-xs text-cafe/50">{{ rangoComparacion }}</span>
    </div>

    <div class="mt-6">
      <SerieVentas
        :serie="informe.serie"
        :moneda="moneda"
        :agrupacion="informe.periodo.agrupacion"
      />
    </div>

    <EmptyState
      v-if="!items.length"
      titulo="No hay ventas en el período"
      detalle="Probá con otro rango de fechas o con otro tipo de venta."
    />

    <template v-else>
      <div class="mt-6 hidden overflow-x-auto rounded-2xl border border-rosa/60 bg-blanco md:block">
        <table class="min-w-full text-sm">
          <thead class="bg-rosa/20 text-left text-xs uppercase tracking-wide text-cafe/60">
            <tr>
              <th v-if="mostrarTipo" class="px-4 py-3 font-semibold">Tipo</th>
              <th class="px-4 py-3 font-semibold">Ítem</th>
              <th class="px-4 py-3 text-right font-semibold">Operaciones</th>
              <th v-if="hayProductos" class="px-4 py-3 text-right font-semibold">Unidades</th>
              <th class="px-4 py-3 text-right font-semibold">Bruto</th>
              <th class="px-4 py-3 text-right font-semibold">Neto percibido</th>
              <th class="px-4 py-3 text-right font-semibold">Participación</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in items"
              :key="`${item.tipo}-${item.id}`"
              class="border-t border-rosa/40 transition hover:bg-rosa/10"
            >
              <td v-if="mostrarTipo" class="px-4 py-3">
                <span
                  class="inline-flex whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                  :class="item.tipo === 'PRODUCTO'
                    ? 'border-coral/30 bg-coral/10 text-granate'
                    : 'border-rosa bg-rosa/30 text-granate'"
                >
                  {{ item.tipo === 'PRODUCTO' ? 'Producto' : 'Servicio' }}
                </span>
              </td>
              <td class="px-4 py-3 font-medium text-granate">{{ item.nombre }}</td>
              <td class="px-4 py-3 text-right tabular-nums text-cafe/70">{{ item.operaciones }}</td>
              <td v-if="hayProductos" class="px-4 py-3 text-right tabular-nums text-cafe/70">{{ item.unidades }}</td>
              <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/80">
                {{ formatearPrecio(item.bruto, moneda) }}
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums font-semibold text-granate">
                {{ formatearPrecio(item.netoPercibido, moneda) }}
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/60">
                {{ item.participacion }}%
              </td>
            </tr>
          </tbody>
          <tfoot class="border-t border-rosa/60 bg-rosa/10 text-sm">
            <tr>
              <td class="px-4 py-3 font-semibold text-cafe/60" :colspan="mostrarTipo ? 2 : 1">Total</td>
              <td class="px-4 py-3 text-right tabular-nums font-semibold text-granate">{{ totales.operaciones }}</td>
              <td v-if="hayProductos" class="px-4 py-3 text-right tabular-nums font-semibold text-granate">{{ totales.unidades }}</td>
              <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums font-semibold text-granate">
                {{ formatearPrecio(totales.bruto, moneda) }}
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums font-semibold text-granate">
                {{ formatearPrecio(totales.netoPercibido, moneda) }}
              </td>
              <td class="px-4 py-3"></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <ul class="mt-6 space-y-3 md:hidden">
        <li
          v-for="item in items"
          :key="`movil-${item.tipo}-${item.id}`"
          class="rounded-2xl border border-rosa/60 bg-blanco p-4"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p v-if="mostrarTipo" class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">
                {{ item.tipo === 'PRODUCTO' ? 'Producto' : 'Servicio' }}
              </p>
              <p class="truncate font-semibold text-granate">{{ item.nombre }}</p>
            </div>
            <p class="whitespace-nowrap font-display text-lg font-extrabold tabular-nums text-granate">
              {{ formatearPrecio(item.netoPercibido, moneda) }}
            </p>
          </div>
          <dl class="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
            <div>
              <dt class="text-cafe/45">Bruto</dt>
              <dd class="text-cafe/75">{{ formatearPrecio(item.bruto, moneda) }}</dd>
            </div>
            <div>
              <dt class="text-cafe/45">Operaciones</dt>
              <dd class="text-cafe/75">{{ item.operaciones }}</dd>
            </div>
            <div v-if="hayProductos">
              <dt class="text-cafe/45">Unidades</dt>
              <dd class="text-cafe/75">{{ item.unidades }}</dd>
            </div>
            <div>
              <dt class="text-cafe/45">Participación</dt>
              <dd class="text-cafe/75">{{ item.participacion }}%</dd>
            </div>
          </dl>
        </li>
      </ul>
    </template>
  </template>
</template>
