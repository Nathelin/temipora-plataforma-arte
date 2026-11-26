<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatearFecha } from '../servicios/fechas'
import { formatearPrecio } from '../servicios/moneda'
import { guardarArchivo } from '../servicios/descargas'
import {
  descargarDescuentosPromocionesPdf,
  obtenerDescuentosPromociones,
} from '../servicios/adminPromociones'
import { useNotificaciones } from '../composables/useNotificaciones'

const { exito, error: notificarError } = useNotificaciones()

const PRESETS = [
  { clave: '7d', texto: '7 días' },
  { clave: '21d', texto: '3 semanas' },
  { clave: '30d', texto: '30 días' },
  { clave: '90d', texto: '90 días' },
  { clave: 'anio', texto: 'Este año' },
]

const ESTADOS = {
  PENDIENTE_PAGO: { texto: 'Pendiente de pago', clase: 'bg-mantequilla text-tinta' },
  EN_CURSO: { texto: 'En curso', clase: 'bg-coral/10 text-coral' },
  FINALIZADO: { texto: 'Finalizado', clase: 'bg-green-500/10 text-green-700' },
  CANCELADO: { texto: 'Cancelado', clase: 'bg-cafe/10 text-cafe/70' },
  IMPAGO: { texto: 'Impago', clase: 'bg-rojizo/10 text-rojizo' },
  INCUMPLIMIENTO: { texto: 'Incumplimiento', clase: 'bg-rojizo/10 text-rojizo' },
}

const ESTADOS_OFERTA = {
  OFRECIDA: 'Ofrecida',
  RESERVADA: 'Aceptada',
  UTILIZADA: 'Usada',
  VENCIDA: 'Vencida',
  CANCELADA: 'Cancelada',
}

function dosDigitos(valor) {
  return String(valor).padStart(2, '0')
}

function iso(fecha) {
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`
}

function sumarDias(base, dias) {
  const fecha = new Date(base.getFullYear(), base.getMonth(), base.getDate())
  fecha.setDate(fecha.getDate() + dias)
  return fecha
}

const hoy = new Date()
const desde = ref(iso(sumarDias(hoy, -29)))
const hasta = ref(iso(hoy))
const preset = ref('30d')
const q = ref('')
const comisionCero = ref(false)
const pagina = ref(0)
const informe = ref(null)
const cargando = ref(true)
const error = ref('')
const descargando = ref(false)
let debounceTimer
let requestId = 0

const resumen = computed(() => informe.value?.resumen || {})
const ofertas = computed(() => informe.value?.ofertas || {})
const pedidos = computed(() => informe.value?.pedidos || [])
const totalPaginas = computed(() => Number(informe.value?.totalPaginas || 0))
const hayMas = computed(() => Boolean(informe.value?.hayMas))
const moneda = computed(() => informe.value?.moneda || 'ARS')
const totalElementos = computed(() => Number(informe.value?.totalElementos || 0))
const rangoInvalido = computed(
  () => !desde.value || !hasta.value || desde.value > hasta.value,
)

const tarjetas = computed(() => {
  const r = resumen.value
  const conComisionCero = Number(r.pedidosConComisionCero || 0)
  return [
    {
      etiqueta: 'Pedidos con descuento',
      valor: Number(r.pedidos || 0),
      detalle: Number(r.totalPedidosPeriodo || 0)
        ? `${r.porcentajePedidos}% de ${r.totalPedidosPeriodo} pedidos del período`
        : 'Sin pedidos en el período',
      destacada: true,
    },
    {
      etiqueta: 'Descuento financiado',
      valor: formatearPrecio(r.descuentoTotal, moneda.value),
      detalle: 'Lo absorbe Temipora; el artista cobra su neto.',
    },
    {
      etiqueta: 'Ticket promedio',
      valor: formatearPrecio(r.ticketPromedio, moneda.value),
      detalle: 'Importe pagado por el cliente.',
    },
    /*{
      etiqueta: 'Pedidos con comisión 0',
      valor: conComisionCero,
      detalle: `${formatearPrecio(r.descuentoConComisionCero || 0, moneda.value)} de descuento`,
      alerta: conComisionCero > 0,
    },*/
  ]
})

function porcentaje(valor) {
  if (valor === null || valor === undefined) return '—'
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return '—'
  return `${numero.toLocaleString('es-AR', { maximumFractionDigits: 2 })}%`
}

function estadoDe(valor) {
  return ESTADOS[valor] || { texto: valor || '—', clase: 'bg-rosa/30 text-granate' }
}

function estadoOferta(valor) {
  return ESTADOS_OFERTA[valor] || null
}

function aplicarPreset(clave) {
  clearTimeout(debounceTimer)
  const ahora = new Date()
  preset.value = clave

  if (clave === '7d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -6))
  } else if (clave === '21d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -20))
  } else if (clave === '30d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -29))
  } else if (clave === '90d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -89))
  } else if (clave === 'anio') {
    desde.value = iso(new Date(ahora.getFullYear(), 0, 1))
    hasta.value = iso(ahora)
  }

  cargar(0)
}

function cambiarFechas() {
  clearTimeout(debounceTimer)
  preset.value = 'personalizado'
  cargar(0)
}

function parametros() {
  return {
    desde: desde.value,
    hasta: hasta.value,
    q: q.value.trim() || undefined,
    comisionCero: comisionCero.value,
  }
}

async function cargar(nuevaPagina = 0) {
  if (rangoInvalido.value) {
    error.value = 'La fecha de inicio debe ser anterior a la de fin.'
    return
  }
  const actual = ++requestId
  cargando.value = true
  error.value = ''
  try {
    const data = await obtenerDescuentosPromociones({
      ...parametros(),
      pagina: nuevaPagina,
      tamanio: 20,
    })
    if (actual !== requestId) return
    informe.value = data
    pagina.value = Number(data?.pagina || 0)
  } catch (e) {
    if (actual !== requestId) return
    error.value = e.response?.data?.message
      || 'No se pudo generar el informe de descuentos.'
  } finally {
    if (actual === requestId) {
      cargando.value = false
    }
  }
}

async function descargar() {
  if (descargando.value || rangoInvalido.value) return
  descargando.value = true
  try {
    const { blob, nombre } = await descargarDescuentosPromocionesPdf(parametros())
    guardarArchivo(blob, nombre)
    exito('Descargamos el informe en PDF.')
  } catch {
    notificarError('No se pudo descargar el informe.')
  } finally {
    descargando.value = false
  }
}

watch(q, () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => cargar(0), 300)
})

watch(comisionCero, () => cargar(0))

onMounted(() => cargar(0))
onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      eyebrow="Comercio"
      titulo="Descuentos aplicados"
      descripcion="Pedidos en los que se aplicó un descuento de reactivación. Cada fila es un pedido; las ofertas se resumen aparte para no contar dos veces la misma promoción."
    >
      <template #acciones>
        <span
          v-if="!cargando && !error"
          class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral tabular-nums"
        >
          {{ totalElementos }} {{ totalElementos === 1 ? 'pedido' : 'pedidos' }}
        </span>
        <button
          type="button"
          class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
          :disabled="descargando || cargando || rangoInvalido"
          @click="descargar"
        >
          {{ descargando ? 'Generando...' : 'Descargar PDF' }}
        </button>
      </template>
    </AdminPageHeader>

    <div class="mt-6">
      <AdminTabs />
    </div>

    <section class="rounded-2xl border border-rosa/60 bg-blanco p-4 sm:p-5">
      <div class="flex flex-wrap gap-2" role="group" aria-label="Períodos rápidos">
        <button
          v-for="item in PRESETS"
          :key="item.clave"
          type="button"
          class="inline-flex min-h-9 items-center rounded-full border px-4 text-[13px] font-semibold transition-colors motion-reduce:transition-none"
          :class="preset === item.clave
            ? 'border-coral bg-coral text-white'
            : 'border-rosa bg-blanco text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
          :aria-pressed="preset === item.clave"
          @click="aplicarPreset(item.clave)"
        >
          {{ item.texto }}
        </button>
      </div>

      <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-[auto_auto_minmax(0,1fr)]">
        <label class="flex items-center gap-2 text-xs font-semibold text-cafe/60">
          Desde
          <input
            v-model="desde"
            type="date"
            class="min-w-0 rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="cambiarFechas"
          />
        </label>
        <label class="flex items-center gap-2 text-xs font-semibold text-cafe/60">
          Hasta
          <input
            v-model="hasta"
            type="date"
            class="min-w-0 rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="cambiarFechas"
          />
        </label>
        <input
          v-model="q"
          type="search"
          placeholder="Buscar por cliente, artista o servicio..."
          class="min-w-0 rounded-lg border-0 px-4 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        />
      </div>

      <div class="mt-4 flex flex-col gap-2 border-t border-rosa/40 pt-4 sm:flex-row sm:items-center sm:justify-between">
        <!--label class="flex items-start gap-2 text-sm text-cafe">
          <input
            v-model="comisionCero"
            type="checkbox"
            class="mt-0.5 h-4 w-4 rounded border-borde text-coral focus:ring-coral"
          />
          <!--span>
            Ver sólo pedidos con comisión 0
            <span class="block text-xs text-cafe/50">
              Si la comisión estuvo en 0, el descuento aplicado también debería ser 0. Este filtro ayuda a detectar excepciones.
            </span>
          </span>
        </label-->
        <!--p class="text-xs leading-relaxed text-cafe/50">
          El descuento y la comisión se toman del presupuesto congelado al emitirlo.
        </p-->
      </div>
    </section>

    <div v-if="cargando" class="mt-6 space-y-4" role="status" aria-live="polite">
      <span class="sr-only">Cargando informe de descuentos...</span>
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="n in 4" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      </div>
      <div class="h-64 animate-pulse rounded-3xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
    </div>

    <p
      v-else-if="error"
      class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center text-sm text-rojizo"
    >
      {{ error }}
    </p>

    <template v-else>
      <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <article
          v-for="tarjeta in tarjetas"
          :key="tarjeta.etiqueta"
          class="rounded-2xl border border-rosa/60 bg-blanco p-5"
        >
          <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/50">
            {{ tarjeta.etiqueta }}
          </p>
          <p
            class="mt-2 font-display text-2xl font-extrabold tabular-nums"
            :class="tarjeta.alerta ? 'text-rojizo' : 'text-granate'"
          >
            {{ tarjeta.valor }}
          </p>
          <p class="mt-1 text-xs leading-relaxed text-cafe/55">{{ tarjeta.detalle }}</p>
        </article>
      </div>

      <div class="mt-6 flex flex-wrap items-center gap-x-6 gap-y-2 rounded-2xl border border-rosa/50 bg-crema/60 px-5 py-4 text-sm text-cafe/70">
        <span class="font-semibold text-granate">Ofertas generadas en el período</span>
        <span><strong class="text-granate">{{ ofertas.campanas || 0 }}</strong> campañas</span>
        <span><strong class="text-granate">{{ ofertas.ofertas || 0 }}</strong> ofertas</span>
        <span><strong class="text-granate">{{ ofertas.aceptadas || 0 }}</strong> aceptadas</span>
        <span><strong class="text-granate">{{ ofertas.utilizadas || 0 }}</strong> usadas</span>
        <span><strong class="text-granate">{{ ofertas.vencidas || 0 }}</strong> vencidas</span>
        <span class="rounded-full bg-coral/10 px-2.5 py-1 text-xs font-semibold text-coral tabular-nums">
          {{ ofertas.tasaUtilizacion || 0 }}% de uso
        </span>
      </div>

      <section class="mt-6 overflow-hidden rounded-3xl border border-rosa/60 bg-blanco shadow-xl shadow-rosa/30">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[62rem] text-left text-sm">
            <caption class="sr-only">
              Pedidos con descuento de reactivación aplicado en el período
            </caption>
            <thead class="bg-rosa/30 text-granate">
              <tr>
                <th class="px-4 py-3 font-normal">Fecha</th>
                <th class="px-4 py-3 font-normal">Pedido</th>
                <th class="px-4 py-3 font-normal">Cliente</th>
                <th class="px-4 py-3 font-normal">Servicio / artista</th>
                <th class="px-4 py-3 font-normal">Descuento</th>
                <th class="px-4 py-3 font-normal">Precio cliente</th>
                <th class="px-4 py-3 font-normal">Comisión al emitir</th>
                <th class="px-4 py-3 font-normal">Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="pedido in pedidos"
                :key="pedido.idPedido"
                class="border-t border-rosa/40 align-top transition-colors hover:bg-rosa/10"
              >
                <td class="whitespace-nowrap px-4 py-3 text-cafe/70">
                  {{ formatearFecha(pedido.fechaPedido) }}
                </td>
                <td class="whitespace-nowrap px-4 py-3">
                  <router-link
                    :to="{ name: 'admin-pedido-detalle', params: { idPedido: pedido.idPedido } }"
                    class="font-semibold text-coral transition hover:text-rojizo"
                  >
                    #{{ pedido.idPedido }}
                  </router-link>
                </td>
                <td class="px-4 py-3">
                  <p class="font-semibold text-granate">@{{ pedido.clienteUsuario }}</p>
                  <!--p class="text-xs text-cafe/50">{{ pedido.clienteNombre || '—' }}</p-->
                </td>
                <td class="px-4 py-3">
                  <p class="max-w-[18rem] text-cafe">{{ pedido.servicioNombre || '—' }}</p>
                  <p class="text-xs text-cafe/50">@{{ pedido.artistaUsuario }}</p>
                </td>
                <td class="px-4 py-3">
                  <p class="font-semibold text-coral">
                    -{{ porcentaje(pedido.descuentoPorcentaje) }}
                    <span class="text-cafe">{{ formatearPrecio(pedido.descuentoMontoCobro, moneda) }}</span>
                  </p>
                  <p class="mt-0.5 text-[11px] text-cafe/50">
                    <template v-if="pedido.idOferta">
                      Oferta #{{ pedido.idOferta }}
                      <template v-if="estadoOferta(pedido.ofertaEstado)">
                        · {{ estadoOferta(pedido.ofertaEstado) }}
                      </template>
                    </template>
                    <template v-else>Sin oferta asociada</template>
                  </p>
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-cafe">
                  {{ formatearPrecio(pedido.precioFinalClienteCobro, moneda) }}
                </td>
                <td class="whitespace-nowrap px-4 py-3">
                  <p class="text-cafe">{{ porcentaje(pedido.comisionPorcentaje) }}</p>
                  <span
                    v-if="pedido.comisionCero"
                    class="mt-0.5 inline-flex rounded-full bg-rojizo/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-rojizo"
                  >
                    Comisión 0
                  </span>
                </td>
                <td class="whitespace-nowrap px-4 py-3">
                  <span
                    class="inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold"
                    :class="estadoDe(pedido.estado).clase"
                  >
                    {{ estadoDe(pedido.estado).texto }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!pedidos.length" class="border-t border-rosa/40 px-6 py-12 text-center">
          <p class="font-semibold text-granate">No hay pedidos con descuento para estos filtros.</p>
          <p class="mt-1 text-sm text-cafe/60">
            Probá con otro período o quitá la búsqueda.
          </p>
        </div>
      </section>

      <div
        v-if="totalPaginas > 1"
        class="mt-6 flex items-center justify-between gap-3 border-t border-rosa/40 pt-4"
      >
        <p class="text-xs text-cafe/50 tabular-nums">
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

      <p class="mt-6 text-xs leading-relaxed text-cafe/50">
        Cada fila es un pedido. Si una oferta se usó en un presupuesto que no llegó a
        pedido, no aparece en la tabla y sólo se cuenta en el resumen de ofertas.
      </p>
    </template>
  </div>
</template>
