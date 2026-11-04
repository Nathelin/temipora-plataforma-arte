<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  ArrowRight,
  CircleCheckBig,
  Clock,
  Coins,
  Package,
  Percent,
  Receipt,
  RefreshCw,
  ShieldAlert,
  Tags,
  TriangleAlert,
  Users,
} from '@lucide/vue'
import AdminPageHeader from '../components/admin/AdminPageHeader.vue'
import api from '../servicios/api'

const cargando = ref(true)
const error = ref('')
const errorParcial = ref('')

const comunidad = ref({
  total: 0,
  artistas: 0,
  clientes: 0,
  administradores: 0,
  nuevosSemana: 0,
})

const negocio = ref({
  periodo: '',
  moneda: 'ARS',
  volumen: 0,
  volumenAnterior: 0,
  comision: 0,
  comisionAnterior: 0,
  pedidosPagados: 0,
  pedidosPagadosAnterior: 0,
  ticketPromedio: 0,
  ticketPromedioAnterior: 0,
})

const operacion = ref({
  entregasAtrasadas: 0,
  aprobacionesPendientes: 0,
})

const alertas = ref([])
const reportes = ref(0)
const etiquetas = ref(0)

const SEVERIDAD = {
  ALTA: { etiqueta: 'Alta', clase: 'bg-rojizo/10 text-rojizo' },
  MEDIA: { etiqueta: 'Media', clase: 'bg-coral/10 text-coral' },
  BAJA: { etiqueta: 'Baja', clase: 'bg-rosa/20 text-cafe' },
}

async function cargar() {
  cargando.value = true
  error.value = ''
  errorParcial.value = ''
  try {
    const resultados = await Promise.allSettled([
      api.get('/admin/resumen/comunidad'),
      api.get('/admin/resenias/reportadas', { params: { pagina: 0, tamanio: 1 } }),
      api.get('/admin/etiquetas/conteo-pendientes'),
      api.get('/admin/resumen/alertas'),
      api.get('/admin/resumen/negocio'),
      api.get('/admin/resumen/operacion'),
    ])

    const [comunidadRes, reportesRes, etiquetasRes, alertasRes, negocioRes, operacionRes] =
      resultados

    if (comunidadRes.status === 'fulfilled') {
      const data = comunidadRes.value.data ?? {}
      comunidad.value = {
        total: Number(data.total ?? 0),
        artistas: Number(data.artistas ?? 0),
        clientes: Number(data.clientes ?? 0),
        administradores: Number(data.administradores ?? 0),
        nuevosSemana: Number(data.nuevosSemana ?? 0),
      }
    }
    if (reportesRes.status === 'fulfilled') {
      reportes.value = Number(reportesRes.value.data?.totalElementos ?? 0)
    }
    if (etiquetasRes.status === 'fulfilled') {
      etiquetas.value = Number(etiquetasRes.value.data?.pendientes ?? 0)
    }
    if (alertasRes.status === 'fulfilled') {
      alertas.value = alertasRes.value.data ?? []
    }
    if (negocioRes.status === 'fulfilled') {
      const data = negocioRes.value.data ?? {}
      negocio.value = {
        periodo: data.periodo ?? '',
        moneda: data.moneda ?? 'ARS',
        volumen: Number(data.volumen ?? 0),
        volumenAnterior: Number(data.volumenAnterior ?? 0),
        comision: Number(data.comision ?? 0),
        comisionAnterior: Number(data.comisionAnterior ?? 0),
        pedidosPagados: Number(data.pedidosPagados ?? 0),
        pedidosPagadosAnterior: Number(data.pedidosPagadosAnterior ?? 0),
        ticketPromedio: Number(data.ticketPromedio ?? 0),
        ticketPromedioAnterior: Number(data.ticketPromedioAnterior ?? 0),
      }
    }
    if (operacionRes.status === 'fulfilled') {
      const data = operacionRes.value.data ?? {}
      operacion.value = {
        entregasAtrasadas: Number(data.entregasAtrasadas ?? 0),
        aprobacionesPendientes: Number(data.aprobacionesPendientes ?? 0),
      }
    }

    const fallidos = resultados.filter((resultado) => resultado.status === 'rejected')
    if (fallidos.length === resultados.length) {
      error.value = 'No se pudo cargar el resumen.'
    } else if (fallidos.length > 0) {
      errorParcial.value = 'Algunos datos no se pudieron cargar.'
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el resumen.'
  } finally {
    cargando.value = false
  }
}

const atenciones = computed(() => {
  const items = []
  if (reportes.value) {
    items.push({
      clave: 'reportes',
      icono: TriangleAlert,
      cantidad: reportes.value,
      etiqueta: reportes.value === 1 ? 'reseña reportada' : 'reseñas reportadas',
      detalle: 'Revisalas y decidí si se ocultan o se restauran.',
      to: '/admin/resenas/reportadas',
    })
  }
  if (etiquetas.value) {
    items.push({
      clave: 'etiquetas',
      icono: Tags,
      cantidad: etiquetas.value,
      etiqueta: etiquetas.value === 1 ? 'etiqueta propuesta' : 'etiquetas propuestas',
      detalle: 'Los artistas esperan que las apruebes o rechaces.',
      to: '/admin/etiquetas?estado=PENDIENTE',
    })
  }
  return items
})

const etiquetaPeriodo = computed(() => {
  if (!negocio.value.periodo) return 'Este mes'
  const [anio, mes] = negocio.value.periodo.split('-')
  const fecha = new Date(Number(anio), Number(mes) - 1, 1)
  return new Intl.DateTimeFormat('es', {
    month: 'long',
    year: 'numeric',
  }).format(fecha)
})

const atajos = computed(() => [
  {
    nombre: 'Usuarios',
    to: '/admin/usuarios',
    contador: comunidad.value.total,
  },
  {
    nombre: 'Reseñas reportadas',
    to: '/admin/resenas/reportadas',
    contador: reportes.value,
  },
  {
    nombre: 'Etiquetas pendientes',
    to: '/admin/etiquetas?estado=PENDIENTE',
    contador: etiquetas.value,
  },
  {
    nombre: 'Auditoría',
    to: '/admin/auditoria',
    contador: alertas.value.length,
  },
])

function dinero(valor) {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: negocio.value.moneda || 'ARS',
    maximumFractionDigits: 0,
  }).format(Number(valor ?? 0))
}

function variacion(actual, anterior) {
  const a = Number(actual ?? 0)
  const b = Number(anterior ?? 0)
  if (b <= 0) return null
  return ((a - b) / b) * 100
}

function textoVariacion(valor) {
  if (valor === null) return 'Sin datos del mes anterior'
  const signo = valor > 0 ? '+' : ''
  return `${signo}${valor.toFixed(1)}% vs. mes anterior`
}

function claseVariacion(valor) {
  if (valor === null || valor === 0) return 'text-cafe/45'
  return valor > 0 ? 'text-green-700' : 'text-rojizo'
}

function severidadDe(severidad) {
  return SEVERIDAD[severidad] ?? SEVERIDAD.MEDIA
}

onMounted(cargar)
</script>

<template>
  <div class="px-4 py-8 sm:px-8">
    <AdminPageHeader
      eyebrow="Administración"
      titulo="Resumen"
      descripcion="El pulso del negocio, la operación y lo que requiere tu atención."
    >
      <template #acciones>
        <button
          type="button"
          class="inline-flex min-h-10 items-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
          :disabled="cargando"
          @click="cargar"
        >
          <RefreshCw
            class="h-4 w-4"
            :class="cargando ? 'animate-spin motion-reduce:animate-none' : ''"
            aria-hidden="true"
          />
          {{ cargando ? 'Actualizando...' : 'Actualizar' }}
        </button>
      </template>
    </AdminPageHeader>

    <div v-if="cargando" class="mt-8 space-y-4" role="status" aria-live="polite">
      <span class="sr-only">Cargando resumen...</span>
      <div class="h-44 animate-pulse rounded-3xl border border-rosa/40 bg-blanco/70 motion-reduce:animate-none" aria-hidden="true"></div>
      <div class="grid gap-4 sm:grid-cols-3">
        <div v-for="n in 3" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70 motion-reduce:animate-none" aria-hidden="true"></div>
      </div>
    </div>

    <div v-else-if="error" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 motion-reduce:transition-none"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <p v-if="errorParcial" class="mt-6 rounded-xl border border-coral/30 bg-coral/5 px-4 py-2 text-xs font-medium text-coral">
        {{ errorParcial }}
      </p>

      <section class="mt-6 rounded-3xl border border-coral/25 bg-coral/5 p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-xl font-bold text-granate">Requiere tu atención</h2>
          <span
            v-if="atenciones.length"
            class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral tabular-nums"
          >
            {{ atenciones.length }}
          </span>
        </div>

        <div v-if="atenciones.length" class="mt-4 grid gap-3 sm:grid-cols-2">
          <router-link
            v-for="atencion in atenciones"
            :key="atencion.clave"
            :to="atencion.to"
            class="group flex flex-col rounded-2xl border border-coral/20 bg-blanco/80 p-4 transition duration-150 hover:-translate-y-0.5 hover:border-coral/50 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)] motion-reduce:transition-none motion-reduce:transform-none"
          >
            <div class="flex items-start justify-between gap-3">
              <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-coral/10 text-coral">
                <component :is="atencion.icono" class="h-5 w-5" :stroke-width="1.9" aria-hidden="true" />
              </span>
              <span class="inline-flex items-center gap-1 text-sm font-semibold text-coral">
                Revisar
                <ArrowRight class="h-4 w-4 transition-transform duration-150 group-hover:translate-x-0.5 motion-reduce:transition-none motion-reduce:transform-none" aria-hidden="true" />
              </span>
            </div>

            <p class="mt-3 font-display text-4xl font-extrabold leading-none text-granate tabular-nums">
              {{ atencion.cantidad }}
            </p>
            <p class="mt-1 text-sm font-semibold text-granate">{{ atencion.etiqueta }}</p>
            <p class="mt-1 text-xs leading-relaxed text-cafe/60">{{ atencion.detalle }}</p>
          </router-link>
        </div>

        <div
          v-else
          class="mt-4 flex items-center gap-3 rounded-2xl border border-rosa/40 bg-blanco/70 px-4 py-6"
        >
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-green-500/10 text-green-700">
            <CircleCheckBig class="h-5 w-5" :stroke-width="1.9" aria-hidden="true" />
          </span>
          <div>
            <p class="text-sm font-semibold text-granate">Todo al día</p>
            <p class="text-xs text-cafe/60">No hay reseñas reportadas ni etiquetas pendientes.</p>
          </div>
        </div>
      </section>

      <section class="mt-6">
        <div class="flex items-baseline justify-between gap-3">
          <h2 class="font-display text-lg font-bold text-granate">Negocio</h2>
          <span class="text-xs capitalize text-cafe/50">{{ etiquetaPeriodo }}</span>
        </div>

        <div class="mt-3 grid gap-3 sm:grid-cols-3">
          <div class="rounded-2xl border border-rosa/60 bg-blanco p-4">
            <div class="flex items-center justify-between gap-2">
              <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Volumen procesado</span>
              <Coins class="h-4 w-4 text-cafe/30" aria-hidden="true" />
            </div>
            <p class="mt-2 font-display text-2xl font-extrabold tabular-nums text-granate">
              {{ dinero(negocio.volumen) }}
            </p>
            <p class="mt-1 text-[11px]" :class="claseVariacion(variacion(negocio.volumen, negocio.volumenAnterior))">
              {{ textoVariacion(variacion(negocio.volumen, negocio.volumenAnterior)) }}
            </p>
          </div>

          <div class="rounded-2xl border border-rosa/60 bg-blanco p-4">
            <div class="flex items-center justify-between gap-2">
              <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Comisión generada</span>
              <Percent class="h-4 w-4 text-coral/60" aria-hidden="true" />
            </div>
            <p
              class="mt-2 font-display text-2xl font-extrabold tabular-nums"
              :class="Number(negocio.comision) > 0 ? 'text-coral' : 'text-cafe/40'"
            >
              {{ dinero(negocio.comision) }}
            </p>
            <p class="mt-1 text-[11px]" :class="claseVariacion(variacion(negocio.comision, negocio.comisionAnterior))">
              {{ textoVariacion(variacion(negocio.comision, negocio.comisionAnterior)) }}
            </p>
          </div>

          <div class="rounded-2xl border border-rosa/60 bg-blanco p-4">
            <div class="flex items-center justify-between gap-2">
              <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Promedio de ventas</span>
              <Receipt class="h-4 w-4 text-cafe/30" aria-hidden="true" />
            </div>
            <p class="mt-2 font-display text-2xl font-extrabold tabular-nums text-granate">
              {{ dinero(negocio.ticketPromedio) }}
            </p>
            <p class="mt-1 text-[11px] text-cafe/45">
              {{ negocio.pedidosPagados }} {{ negocio.pedidosPagados === 1 ? 'pedido pagado' : 'pedidos pagados' }}
            </p>
          </div>
        </div>
        <!--p class="mt-2 text-[11px] text-cafe/45">
          Volumen y comisión de pagos aprobados. La comisión no descuenta el costo de MercadoPago.
        </p-->
      </section>

      <div class="mt-6 grid gap-4 lg:grid-cols-3">
        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5 lg:col-span-2">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-lg font-bold text-granate">Seguridad</h2>
            <router-link
              :to="{ path: '/admin/auditoria', query: { critica: 'true' } }"
              class="inline-flex items-center gap-1 text-xs font-semibold text-coral transition-colors hover:text-granate"
            >
              Ver auditoría
              <ArrowRight class="h-3.5 w-3.5" aria-hidden="true" />
            </router-link>
          </div>

          <div v-if="alertas.length" class="mt-3 space-y-2">
            <router-link
              v-for="alerta in alertas.slice(0, 3)"
              :key="`${alerta.codigo}-${alerta.actorNombre || 'global'}`"
              :to="{
                path: '/admin/auditoria',
                query: {
                  rango: 'custom',
                  desde: alerta.desde,
                  hasta: alerta.hasta,
                  ...(alerta.accion ? { accion: alerta.accion } : {}),
                  ...(alerta.actorNombre ? { actor: alerta.actorNombre } : {}),
                },
              }"
              class="group flex items-start gap-3 rounded-xl border border-rosa/40 px-3 py-2.5 transition-colors hover:bg-rosa/10"
            >
              <span
                class="inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide"
                :class="severidadDe(alerta.severidad).clase"
              >
                <ShieldAlert class="h-3 w-3" aria-hidden="true" />
                {{ severidadDe(alerta.severidad).etiqueta }}
              </span>
              <span class="min-w-0 flex-1">
                <span class="block text-sm font-semibold text-granate">{{ alerta.titulo }}</span>
                <span class="mt-0.5 block text-xs text-cafe/60">{{ alerta.detalle }}</span>
              </span>
              <ArrowRight class="mt-1 h-3.5 w-3.5 shrink-0 text-cafe/40 transition-transform group-hover:translate-x-0.5 motion-reduce:transition-none motion-reduce:transform-none" aria-hidden="true" />
            </router-link>
            <p v-if="alertas.length > 3" class="pt-1 text-xs text-cafe/50">
              y {{ alertas.length - 3 }} más.
            </p>
          </div>

          <div v-else class="mt-3 flex items-center gap-3 rounded-xl border border-rosa/40 bg-rosa/5 px-3 py-3">
            <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-green-500/10 text-green-700">
              <CircleCheckBig class="h-4 w-4" :stroke-width="1.9" aria-hidden="true" />
            </span>
            <p class="text-sm text-cafe/60">Sin incidentes de seguridad en las últimas 24 horas.</p>
          </div>
        </section>

        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-lg font-bold text-granate">Comunidad</h2>
            <Users class="h-4 w-4 text-cafe/30" aria-hidden="true" />
          </div>
          <p class="mt-2 font-display text-3xl font-extrabold tabular-nums text-granate">
            {{ comunidad.total }}
          </p>
          <p class="text-xs text-cafe/55">usuarios registrados</p>
          <ul class="mt-3 space-y-1 text-xs text-cafe/65">
            <li class="flex items-center justify-between gap-2">
              <span>Artistas</span>
              <span class="font-semibold tabular-nums text-granate">{{ comunidad.artistas }}</span>
            </li>
            <li class="flex items-center justify-between gap-2">
              <span>Compradores</span>
              <span class="font-semibold tabular-nums text-granate">{{ comunidad.clientes }}</span>
            </li>
            <li class="flex items-center justify-between gap-2">
              <span>Administradores</span>
              <span class="font-semibold tabular-nums text-granate">{{ comunidad.administradores }}</span>
            </li>
          </ul>
          <p class="mt-3 border-t border-rosa/30 pt-2 text-[11px] text-cafe/50">
            +{{ comunidad.nuevosSemana }} en los últimos 7 días
          </p>
        </section>
      </div>

      <div class="mt-4 grid gap-4 lg:grid-cols-2">
        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5">
          <h2 class="font-display text-lg font-bold text-granate">Operación</h2>
          <div class="mt-3 grid gap-3 sm:grid-cols-2">
            <div class="rounded-xl border border-rosa/40 px-3 py-3">
              <div class="flex items-center gap-2 text-cafe/55">
                <Package class="h-4 w-4" aria-hidden="true" />
                <span class="text-[11px] font-semibold uppercase tracking-wide">Entregas atrasadas</span>
              </div>
              <p class="mt-1 font-display text-2xl font-extrabold tabular-nums text-granate">
                {{ operacion.entregasAtrasadas }}
              </p>
            </div>
            <div class="rounded-xl border border-rosa/40 px-3 py-3">
              <div class="flex items-center gap-2 text-cafe/55">
                <Clock class="h-4 w-4" aria-hidden="true" />
                <span class="text-[11px] font-semibold uppercase tracking-wide">Esperando aprobación</span>
              </div>
              <p class="mt-1 font-display text-2xl font-extrabold tabular-nums text-granate">
                {{ operacion.aprobacionesPendientes }}
              </p>
            </div>
          </div>
          <!--p class="mt-3 text-[11px] text-cafe/45">
            Se gestionan desde los paneles de artistas y compradores.
          </p-->
        </section>

        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5">
          <h2 class="font-display text-lg font-bold text-granate">Atajos</h2>
          <ul class="mt-3 space-y-1">
            <li v-for="atajo in atajos" :key="atajo.to">
              <router-link
                :to="atajo.to"
                class="group flex min-h-9 items-center justify-between gap-3 rounded-lg px-2 text-sm text-cafe transition-colors hover:bg-rosa/15 hover:text-granate motion-reduce:transition-none"
              >
                <span class="truncate">{{ atajo.nombre }}</span>
                <span class="flex items-center gap-2">
                  <span
                    class="rounded-full px-2 py-0.5 text-[11px] font-semibold tabular-nums"
                    :class="atajo.contador > 0 ? 'bg-coral/10 text-coral' : 'bg-rosa/20 text-cafe/50'"
                  >
                    {{ atajo.contador }}
                  </span>
                  <ArrowRight class="h-4 w-4 shrink-0 text-cafe/40 transition-transform duration-150 group-hover:translate-x-0.5 motion-reduce:transition-none motion-reduce:transform-none" aria-hidden="true" />
                </span>
              </router-link>
            </li>
          </ul>
        </section>
      </div>
    </template>
  </div>
</template>
