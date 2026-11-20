<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminPageHeader from '../components/admin/AdminPageHeader.vue'
import AdminTabs from '../components/admin/AdminTabs.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import SerieAdmin from '../components/SerieAdmin.vue'
import { useNotificaciones } from '../composables/useNotificaciones'
import { conciliarMercadoPago } from '../servicios/adminPagos'
import { descargarInformeAdminPdf, obtenerInformeAdmin } from '../servicios/adminInformes'
import { guardarArchivo } from '../servicios/descargas'
import { formatearPrecio } from '../servicios/moneda'

const { exito, error: notificarError } = useNotificaciones()

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
const modalConciliacion = ref(false)
const conciliando = ref(false)

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
const tipos = computed(() => informe.value?.tipos || [])
const vendedores = computed(() => informe.value?.vendedores || [])
const items = computed(() => informe.value?.items || [])
const comparacion = computed(() => informe.value?.comparacion || null)

const tarjetas = computed(() => {
  const t = totales.value
  if (!t) return []
  return [
    {
      etiqueta: 'Ingresos de la plataforma',
      valor: formatearPrecio(t.comision, moneda.value),
      detalle: 'Comisión Temipora sobre las ventas.',
      destacada: true,
    },
    {
      etiqueta: 'Volumen procesado',
      valor: formatearPrecio(t.volumen, moneda.value),
      detalle: 'Total pagado por los clientes.',
    },
    {
      etiqueta: 'Neto a artistas',
      valor: formatearPrecio(t.netoArtistas, moneda.value),
      detalle: 'Antes del arancel del medio de pago.',
    },
    {
      etiqueta: 'Arancel MercadoPago',
      valor: formatearPrecio(t.arancel, moneda.value),
      detalle: 'Informativo, informado por el medio de pago.',
    },
  ]
})

const secundarias = computed(() => {
  const t = totales.value
  if (!t) return []
  return [
    { etiqueta: 'Operaciones', valor: String(t.operaciones) },
    {
      etiqueta: 'Ticket promedio',
      valor: formatearPrecio(t.ticketPromedio, moneda.value),
    },
    {
      etiqueta: 'Neto liquidado a artistas',
      valor: formatearPrecio(t.netoPercibido, moneda.value),
    },
  ]
})

const rangoComparacion = computed(() => {
  const valor = comparacion.value
  if (!valor) return ''
  return `vs. ${formatearFechaSimple(valor.desde)} – ${formatearFechaSimple(valor.hasta)}`
})

function formatearFechaSimple(valor) {
  if (!valor) return ''
  const [anio, mes, dia] = String(valor).split('-')
  return `${dia}/${mes}/${anio}`
}

function variacionTexto(valor, etiqueta) {
  if (valor === null || valor === undefined) {
    return `${etiqueta}: sin ventas en el período anterior`
  }
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return `${etiqueta}: —`
  const signo = numero > 0 ? '+' : ''
  return `${etiqueta}: ${signo}${numero.toLocaleString('es-AR', { maximumFractionDigits: 2 })}%`
}

function variacionClase(valor) {
  if (valor === null || valor === undefined) return 'text-cafe/45'
  const numero = Number(valor)
  if (!Number.isFinite(numero) || numero === 0) return 'text-cafe/45'
  return numero > 0 ? 'text-green-700' : 'text-rojizo'
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
    informe.value = await obtenerInformeAdmin(parametros())
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo generar el informe.'
  } finally {
    cargando.value = false
  }
}

async function descargar() {
  if (descargando.value || rangoInvalido.value) return
  descargando.value = true
  try {
    const { blob, nombre } = await descargarInformeAdminPdf(parametros())
    guardarArchivo(blob, nombre)
    exito('Descargamos el informe en PDF.')
  } catch {
    notificarError('No se pudo descargar el informe.')
  } finally {
    descargando.value = false
  }
}

function abrirConciliacion() {
  if (conciliando.value) return
  modalConciliacion.value = true
}

function resumenConciliacion(resultado) {
  const partes = [
    `${resultado.revisados} revisadas`,
    `${resultado.completados} completadas`,
  ]
  if (resultado.sinDatos) {
    partes.push(`${resultado.sinDatos} sin datos en MercadoPago`)
  }
  if (resultado.errores) {
    partes.push(`${resultado.errores} con error`)
  }
  return `Conciliación: ${partes.join(', ')}.`
}

async function conciliar() {
  conciliando.value = true
  try {
    const resultado = await conciliarMercadoPago()
    modalConciliacion.value = false
    exito(resultado.revisados
      ? resumenConciliacion(resultado)
      : 'No había operaciones pendientes de conciliar.')
    await cargar()
  } catch (e) {
    notificarError(e.response?.data?.message
      || 'No se pudo conciliar con MercadoPago.')
  } finally {
    conciliando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Informes"
      titulo="Ventas y comisiones"
      descripcion="Ventas de la plataforma en un período y las comisiones que generaron, con cortes por tipo, vendedor e ítem."
    >
      <template #acciones>
        <div class="flex flex-wrap items-center justify-end gap-2">
          <button
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
            :disabled="conciliando || cargando"
            @click="abrirConciliacion"
          >
            {{ conciliando ? 'Conciliando...' : 'Conciliar MercadoPago' }}
          </button>
          <button
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
            :disabled="descargando || cargando || rangoInvalido"
            @click="descargar"
          >
            {{ descargando ? 'Generando PDF...' : 'Descargar PDF' }}
          </button>
        </div>
      </template>
    </AdminPageHeader>

    <AdminTabs />

    <form
      class="rounded-2xl border border-rosa/60 bg-blanco p-4"
      @submit.prevent="cargar"
    >
      <div class="flex flex-wrap gap-2">
        <button
          v-for="opcion in PRESETS"
          :key="opcion.clave"
          type="button"
          class="inline-flex min-h-9 items-center justify-center rounded-full border px-3 text-xs font-semibold transition-colors motion-reduce:transition-none"
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
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div
          v-for="n in 4"
          :key="n"
          class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco motion-reduce:animate-none"
        ></div>
      </div>
      <div class="h-64 animate-pulse rounded-2xl border border-rosa/40 bg-blanco motion-reduce:animate-none"></div>
    </div>

    <div
      v-else-if="error"
      class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center"
    >
      <p class="text-sm text-rojizo">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 motion-reduce:transition-none"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else-if="informe">
      <div class="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
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

      <div class="mt-3 grid gap-3 sm:grid-cols-3">
        <div
          v-for="dato in secundarias"
          :key="dato.etiqueta"
          class="rounded-xl border border-rosa/40 bg-blanco px-3 py-2.5"
        >
          <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/50">
            {{ dato.etiqueta }}
          </span>
          <p class="mt-1 font-display text-lg font-bold tabular-nums text-granate">
            {{ dato.valor }}
          </p>
        </div>
      </div>

      <div class="mt-4 flex flex-wrap items-baseline gap-x-4 gap-y-1 text-sm">
        <span :class="variacionClase(comparacion?.variacionVolumenPorcentaje)">
          {{ variacionTexto(comparacion?.variacionVolumenPorcentaje, 'Volumen') }}
        </span>
        <span :class="variacionClase(comparacion?.variacionComisionPorcentaje)">
          {{ variacionTexto(comparacion?.variacionComisionPorcentaje, 'Ingresos') }}
        </span>
        <span class="text-xs text-cafe/50">{{ rangoComparacion }}</span>
      </div>

      <div class="mt-6">
        <SerieAdmin :serie="informe.serie" :moneda="moneda" />
      </div>

      <div
        v-if="!items.length"
        class="mt-6 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
      >
        <p class="text-lg font-semibold text-granate">
          No hay ventas en el período
        </p>
        <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/60">
          Probá con otro rango de fechas o con otro tipo de venta.
        </p>
      </div>

      <template v-else>
        <section class="mt-6">
          <h2 class="font-display text-lg font-bold text-granate">Por tipo</h2>
          <div class="mt-3 overflow-x-auto rounded-2xl border border-rosa/60 bg-blanco">
            <table class="min-w-full text-sm">
              <thead class="bg-superficie-alt text-left text-xs uppercase tracking-wide text-cafe">
                <tr>
                  <th class="px-4 py-3 font-semibold">Tipo</th>
                  <th class="px-4 py-3 text-right font-semibold">Operaciones</th>
                  <th class="px-4 py-3 text-right font-semibold">Unidades</th>
                  <th class="px-4 py-3 text-right font-semibold">Volumen</th>
                  <th class="px-4 py-3 text-right font-semibold">Comisión</th>
                  <th class="px-4 py-3 text-right font-semibold">Neto artistas</th>
                  <th class="px-4 py-3 text-right font-semibold">Arancel</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="fila in tipos"
                  :key="fila.tipo"
                  class="border-t border-rosa/40"
                >
                  <td class="px-4 py-3">
                    <span
                      class="inline-flex whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                      :class="fila.tipo === 'PRODUCTO'
                        ? 'border-coral/30 bg-coral/10 text-granate'
                        : 'border-rosa bg-rosa/30 text-granate'"
                    >
                      {{ fila.tipo === 'PRODUCTO' ? 'Producto' : 'Servicio' }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-right tabular-nums text-cafe/90">{{ fila.operaciones }}</td>
                  <td class="px-4 py-3 text-right tabular-nums text-cafe/90">{{ fila.unidades }}</td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe">
                    {{ formatearPrecio(fila.volumen, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums font-semibold text-granate">
                    {{ formatearPrecio(fila.comision, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/90">
                    {{ formatearPrecio(fila.netoArtistas, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/90">
                    {{ formatearPrecio(fila.arancel, moneda) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="mt-6">
          <h2 class="font-display text-lg font-bold text-granate">Por vendedor y artista</h2>
          <div class="mt-3 max-h-[30rem] overflow-auto rounded-2xl border border-rosa/60 bg-blanco">
            <table class="min-w-full text-sm">
              <thead class="sticky top-0 z-10 border-b border-rosa/50 bg-superficie-alt text-left text-xs uppercase tracking-wide text-cafe">
                <tr>
                  <th class="px-4 py-3 font-semibold">Vendedor / artista</th>
                  <th class="px-4 py-3 text-right font-semibold">Operaciones</th>
                  <th class="px-4 py-3 text-right font-semibold">Unidades</th>
                  <th class="px-4 py-3 text-right font-semibold">Volumen</th>
                  <th class="px-4 py-3 text-right font-semibold">Comisión</th>
                  <th class="px-4 py-3 text-right font-semibold">Neto artistas</th>
                  <th class="px-4 py-3 text-right font-semibold">Arancel</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="fila in vendedores"
                  :key="fila.idUsuario"
                  class="border-t border-rosa/40"
                >
                  <td class="px-4 py-3 font-medium text-granate">@{{ fila.nombre }}</td>
                  <td class="px-4 py-3 text-right tabular-nums text-cafe/90">{{ fila.operaciones }}</td>
                  <td class="px-4 py-3 text-right tabular-nums text-cafe/90">{{ fila.unidades }}</td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe">
                    {{ formatearPrecio(fila.volumen, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums font-semibold text-granate">
                    {{ formatearPrecio(fila.comision, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/90">
                    {{ formatearPrecio(fila.netoArtistas, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/90">
                    {{ formatearPrecio(fila.arancel, moneda) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="mt-6">
          <h2 class="font-display text-lg font-bold text-granate">Por ítem del catálogo</h2>
          <div class="mt-3 max-h-[32rem] overflow-auto rounded-2xl border border-rosa/60 bg-blanco">
            <table class="w-full min-w-[58rem] text-sm">
              <thead class="sticky top-0 z-10 border-b border-rosa/50 bg-superficie-alt text-left text-xs uppercase tracking-wide text-cafe">
                <tr>
                  <th class="px-4 py-3 font-semibold">Tipo</th>
                  <th class="px-4 py-3 font-semibold">Ítem</th>
                  <th class="px-4 py-3 text-right font-semibold">Operaciones</th>
                  <th class="px-4 py-3 text-right font-semibold">Unidades</th>
                  <th class="px-4 py-3 text-right font-semibold">Volumen</th>
                  <th class="px-4 py-3 text-right font-semibold">Comisión</th>
                  <th class="px-4 py-3 text-right font-semibold">Neto artistas</th>
                  <th class="px-4 py-3 text-right font-semibold">Arancel</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="fila in items"
                  :key="`${fila.tipo}-${fila.id}`"
                  class="border-t border-rosa/40"
                >
                  <td class="px-4 py-3">
                    <span
                      class="inline-flex whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                      :class="fila.tipo === 'PRODUCTO'
                        ? 'border-coral/30 bg-coral/10 text-granate'
                        : 'border-rosa bg-rosa/30 text-granate'"
                    >
                      {{ fila.tipo === 'PRODUCTO' ? 'Producto' : 'Servicio' }}
                    </span>
                  </td>
                  <td class="px-4 py-3 font-medium text-granate">{{ fila.nombre }}</td>
                  <td class="px-4 py-3 text-right tabular-nums text-cafe/90">{{ fila.operaciones }}</td>
                  <td class="px-4 py-3 text-right tabular-nums text-cafe/90">{{ fila.unidades }}</td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe">
                    {{ formatearPrecio(fila.volumen, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums font-semibold text-granate">
                    {{ formatearPrecio(fila.comision, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/90">
                    {{ formatearPrecio(fila.netoArtistas, moneda) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap tabular-nums text-cafe/90">
                    {{ formatearPrecio(fila.arancel, moneda) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>

      <p class="mt-4 text-xs leading-relaxed text-cafe/50">
        Los ingresos de la plataforma son la comisión Temipora. El arancel es el
        informado por el medio de pago y se muestra como referencia.
      </p>
    </template>

    <ModalConfirmacion
      :abierto="modalConciliacion"
      titulo="Conciliar con MercadoPago"
      mensaje="Se consultan en MercadoPago las operaciones aprobadas que no tienen informado el arancel o el neto liquidado. Solo se completan esos datos: no se modifican montos, comisiones ni estados. Puede tardar unos segundos."
      texto-confirmar="Conciliar"
      :confirmar-habilitado="!conciliando"
      @confirmar="conciliar"
      @cerrar="modalConciliacion = false"
    />
  </div>
</template>
