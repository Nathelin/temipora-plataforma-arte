<script setup>
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/PageHeader.vue'
import ItemAtencionPanel from '../components/ItemAtencionPanel.vue'
import FilaPedidoActivo from '../components/FilaPedidoActivo.vue'
import FilaCompra from '../components/FilaCompra.vue'
import { cargarResumenActividad } from '../servicios/miActividad'
import { formatearPrecio } from '../servicios/moneda'
import { formatearFecha } from '../servicios/pedidos'
import { datosContraparte } from '../servicios/solicitudes'

const ICONOS = {
  entregas: 'M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5',
  dinero: 'M2.25 18.75a60.07 60.07 0 0 1 15.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 0 1 3 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 0 0-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 0 1-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 0 0 3 15h-.75M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Zm3 0h.008v.008H18V10.5Zm-12 0h.008v.008H6V10.5Z',
  presupuesto: 'M12 6v12m-3-2.818.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z',
  pedidos: 'M2.25 13.5h3.86a2.25 2.25 0 0 1 2.012 1.244l.256.512a2.25 2.25 0 0 0 2.013 1.244h3.218a2.25 2.25 0 0 0 2.013-1.244l.256-.512a2.25 2.25 0 0 1 2.013-1.244h3.859m-19.5.338V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18v-4.162c0-.224-.034-.447-.1-.661L19.24 5.338a2.25 2.25 0 0 0-2.15-1.588H6.911a2.25 2.25 0 0 0-2.15 1.588L2.35 13.177a2.25 2.25 0 0 0-.1.661Z',
  alerta: 'M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z',
  compras: 'M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z',
  estrella: 'M11.48 3.499a.562.562 0 0 1 1.04 0l2.125 5.111a.563.563 0 0 0 .475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 0 0-.182.557l1.285 5.385a.562.562 0 0 1-.84.61l-4.725-2.885a.562.562 0 0 0-.586 0L6.982 20.54a.562.562 0 0 1-.84-.61l1.285-5.386a.562.562 0 0 0-.182-.557l-4.204-3.602a.562.562 0 0 1 .321-.988l5.518-.442a.563.563 0 0 0 .475-.345L11.48 3.5Z',
}

const cargando = ref(true)
const error = ref('')
const resumen = ref(null)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    resumen.value = await cargarResumenActividad()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar tu resumen.'
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)

const metricas = computed(() => resumen.value?.metricas || {})
const dinero = computed(() => resumen.value?.dinero || {})
const estadoCuenta = computed(() => resumen.value?.estadoCuenta || null)
const pedidosActivos = computed(() => resumen.value?.pedidosActivos || [])
const hitosAtencion = computed(() => resumen.value?.hitosAtencion || [])
const comprasPendientes = computed(() => resumen.value?.comprasPendientes || [])
const presupuestosRecibidos = computed(() => resumen.value?.presupuestosRecibidos || [])
const reseniasPorDejar = computed(() => resumen.value?.reseniasPorDejar || [])

const pedidosPorId = computed(() => {
  const mapa = new Map()
  for (const pedido of pedidosActivos.value) {
    mapa.set(pedido.idPedido, pedido)
  }
  return mapa
})

function diasHasta(valor) {
  if (!valor) return Number.POSITIVE_INFINITY
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return Number.POSITIVE_INFINITY
  return Math.ceil((fecha.getTime() - Date.now()) / 86400000)
}

function textoVencimiento(valor) {
  const dias = diasHasta(valor)
  if (!Number.isFinite(dias)) return ''
  if (dias < 0) {
    const abs = Math.abs(dias)
    return `Venció hace ${abs} ${abs === 1 ? 'día' : 'días'}`
  }
  if (dias === 0) return 'Vence hoy'
  if (dias === 1) return 'Vence mañana'
  return `Vence en ${dias} días`
}

function textoVencimientoConFecha(valor) {
  if (!valor) return 'Sin vencimiento'
  const texto = textoVencimiento(valor)
  return texto ? `${texto} · ${formatearFecha(valor)}` : `Vence el ${formatearFecha(valor)}`
}

function textoAprobacion(valor) {
  if (!valor) return 'Revisala y aprobá'
  const fecha = formatearFecha(valor)
  const dias = diasHasta(valor)
  if (!Number.isFinite(dias)) return `Se aprueba el ${fecha}`
  if (dias < 0) return `El plazo venció el ${fecha}`
  if (dias === 0) return `Se aprueba hoy (${fecha})`
  return `Se aprueba el ${fecha} (en ${dias} ${dias === 1 ? 'día' : 'días'})`
}

function montoDeHito(hito, pedido) {
  if (hito?.montoMostrado == null) return ''
  return formatearPrecio(
    hito.montoMostrado,
    hito.monedaMostrada || pedido?.monedaMostrada || pedido?.monedaCobro,
  )
}

const atenciones = computed(() => {
  const items = []
  const prioridad = { aprobar: 0, vencido: 1, pagar: 2 }
  const grupos = new Map()

  for (const hito of hitosAtencion.value) {
    const motivo = hito.puedeAprobar
      ? 'aprobar'
      : hito.estadoPago === 'VENCIDO'
        ? 'vencido'
        : hito.puedePagar
          ? 'pagar'
          : null
    if (!motivo) continue
    const clave = `${motivo}:${hito.idPedido}`
    if (!grupos.has(clave)) {
      grupos.set(clave, { motivo, idPedido: hito.idPedido, hitos: [] })
    }
    grupos.get(clave).hitos.push(hito)
  }

  const ordenados = [...grupos.values()]
    .sort((a, b) => prioridad[a.motivo] - prioridad[b.motivo])

  for (const grupo of ordenados) {
    const pedido = pedidosPorId.value.get(grupo.idPedido)
    const contraparte = datosContraparte(pedido?.contraparte)
    const servicio = pedido?.nombreServicio || 'tu pedido'
    const primero = grupo.hitos[0]
    const extra = grupo.hitos.length > 1 ? ` (+${grupo.hitos.length - 1})` : ''
    const monto = montoDeHito(primero, pedido)
    const to = { name: 'pedido-detalle', params: { idPedido: grupo.idPedido } }

    if (grupo.motivo === 'aprobar') {
      items.push({
        clave: `aprobar-${grupo.idPedido}`,
        titulo: `Tenés una entrega para aprobar${extra}`,
        detalle: `${servicio} · ${primero.nombre}${monto ? ` · ${monto}` : ''}`,
        to,
        icono: ICONOS.entregas,
        tono: diasHasta(primero.fechaLimiteAprobacion) <= 2 ? 'alerta' : 'info',
        meta: textoAprobacion(primero.fechaLimiteAprobacion),
      })
      continue
    }

    if (grupo.motivo === 'vencido') {
      items.push({
        clave: `vencido-${grupo.idPedido}`,
        titulo: `Se venció el pago de «${primero.nombre}»${extra}`,
        detalle: `${servicio} · el pedido puede quedar sin efecto.`,
        to,
        icono: ICONOS.alerta,
        tono: 'alerta',
        meta: 'Pago vencido',
      })
      continue
    }

    items.push({
      clave: `pagar-${grupo.idPedido}`,
      titulo: `Pago pendiente de «${primero.nombre}»${extra}`,
      detalle: `${servicio}${contraparte?.nombre ? ` · ${contraparte.nombre}` : ''}`,
      to,
      icono: ICONOS.dinero,
      tono: diasHasta(primero.fechaLimitePago) <= 2 ? 'alerta' : 'info',
      meta: textoVencimientoConFecha(primero.fechaLimitePago),
    })
  }

  for (const solicitud of presupuestosRecibidos.value) {
    const artista = datosContraparte(solicitud.artista)
    const precio = solicitud.precioFinalClienteMostrado
      ?? solicitud.precioFinalMostrado
    items.push({
      clave: `presupuesto-${solicitud.idSolicitud}`,
      titulo: `Presupuesto para «${solicitud.nombreServicio || 'tu solicitud'}»`,
      detalle: `${formatearPrecio(precio, solicitud.monedaMostrada)}${artista?.nombre ? ` · ${artista.nombre}` : ''}`,
      to: { name: 'solicitud-detalle', params: { idSolicitud: solicitud.idSolicitud } },
      icono: ICONOS.presupuesto,
      tono: diasHasta(solicitud.fechaVencimiento) <= 1 ? 'alerta' : 'info',
      meta: textoVencimientoConFecha(solicitud.fechaVencimiento),
    })
  }

  for (const resenia of reseniasPorDejar.value) {
    const to = resenia.tipo === 'PEDIDO'
      ? { name: 'pedido-detalle', params: { idPedido: resenia.idPedido } }
      : { name: 'compra-detalle', params: { id: resenia.idCompra } }
    items.push({
      clave: `resenia-${resenia.tipo}-${resenia.idPedido || resenia.idCompra}`,
      titulo: `Dejá tu reseña de «${resenia.titulo || 'tu compra'}»`,
      detalle: `${resenia.tipo === 'PRODUCTO' ? 'Compra de producto' : 'Pedido finalizado'}${resenia.contraparte ? ` · @${resenia.contraparte}` : ''}`,
      to,
      icono: ICONOS.estrella,
      tono: 'info',
      meta: resenia.fechaLimite
        ? `Podés reseñar hasta el ${formatearFecha(resenia.fechaLimite)}`
        : '',
    })
  }

  return items
})

const tarjetas = computed(() => {
  const m = metricas.value
  const d = dinero.value
  const vencidos = Number(m.pagosVencidos || 0)
  return [
    {
      etiqueta: 'Para aprobar',
      valor: Number(m.entregasPorAprobar || 0),
      detalle: m.entregasPorAprobar
        ? 'Entregas que esperan tu revisión'
        : 'No hay entregas pendientes',
      to: { name: 'pedidos', query: { grupo: 'accion' } },
      icono: ICONOS.entregas,
    },
    {
      etiqueta: 'Pagos pendientes',
      valor: Number(m.pagosPendientes || 0) + Number(m.comprasPendientes || 0),
      detalle: Number(d.porPagarTotal || 0)
        ? `${formatearPrecio(d.porPagarTotal, d.moneda)} en total`
        : 'Sin pagos pendientes',
      alerta: vencidos > 0,
      notaAlerta: vencidos
        ? `${vencidos} ${vencidos === 1 ? 'vencido' : 'vencidos'}`
        : '',
      to: { name: 'pedidos', query: { grupo: 'accion' } },
      icono: ICONOS.dinero,
    },
    {
      etiqueta: 'Presupuestos recibidos',
      valor: Number(m.presupuestosRecibidos || 0),
      detalle: m.presupuestosPorVencer
        ? `${m.presupuestosPorVencer} por vencer`
        : 'Sin vencimientos próximos',
      to: { name: 'solicitudes', query: { grupo: 'presupuestadas' } },
      icono: ICONOS.presupuesto,
    },
    {
      etiqueta: 'Pedidos en curso',
      valor: Number(m.pedidosEnCurso || 0),
      detalle: Number(m.pedidosPendientesPago || 0)
        ? `${m.pedidosPendientesPago} esperando el primer pago`
        : 'Todo en marcha',
      to: { name: 'pedidos' },
      icono: ICONOS.pedidos,
    },
  ]
})

const pedidosVisibles = computed(() => pedidosActivos.value.slice(0, 4))

const resumenTexto = computed(() => {
  const m = metricas.value
  const partes = []
  if (m.entregasPorAprobar) {
    partes.push(`${m.entregasPorAprobar} ${m.entregasPorAprobar === 1 ? 'entrega por aprobar' : 'entregas por aprobar'}`)
  }
  if (m.pagosPendientes) {
    partes.push(`${m.pagosPendientes} ${m.pagosPendientes === 1 ? 'pago pendiente' : 'pagos pendientes'}`)
  }
  if (m.presupuestosPorVencer) {
    partes.push(`${m.presupuestosPorVencer} ${m.presupuestosPorVencer === 1 ? 'presupuesto por vencer' : 'presupuestos por vencer'}`)
  }
  if (partes.length) {
    return `Tenés ${partes.join(', ')}.`
  }
  if (m.reseniasPendientes) {
    return 'Tenés reseñas pendientes de dejar.'
  }
  return 'Todo al día: no hay nada urgente por resolver.'
})

function claseDeuda(deuda) {
  return deuda?.puedePagar ? 'text-rojizo' : 'text-cafe'
}
</script>

<template>
  <div>
    <PageHeader
      eyebrow="Tu actividad"
      titulo="Resumen"
      :descripcion="resumenTexto"
      :nivel="2"
    >
      <template #acciones>
        <router-link
          :to="{ name: 'servicios' }"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        >
          Explorar servicios
        </router-link>
        <router-link
          :to="{ name: 'productos' }"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
        >
          Explorar productos
        </router-link>
      </template>
    </PageHeader>

    <div v-if="cargando" class="mt-8 space-y-6" role="status" aria-live="polite">
      <span class="sr-only">Cargando resumen...</span>
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="n in 4" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      </div>
      <div class="h-48 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
    </div>

    <div v-else-if="error" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <section
        v-if="estadoCuenta"
        class="mt-6 rounded-2xl border border-rojizo/30 bg-rojizo/10 p-5 sm:p-6"
      >
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-rojizo">Estado de cuenta</p>
            <h2 class="mt-1 font-display text-xl font-bold text-granate">
              {{ estadoCuenta.sancionado ? 'Tu cuenta tiene una restricción vigente' : 'Tenés pagos pendientes' }}
            </h2>
          </div>
          <p class="font-display text-xl font-extrabold text-rojizo">
            {{ formatearPrecio(estadoCuenta.totalAdeudado, 'ARS') }}
          </p>
        </div>

        <p v-if="estadoCuenta.sancionado && estadoCuenta.fechaFin" class="mt-2 text-sm text-cafe/80">
          La restricción termina el {{ formatearFecha(estadoCuenta.fechaFin) }}.
        </p>

        <ul v-if="estadoCuenta.deudas?.length" class="mt-3 space-y-2">
          <li
            v-for="deuda in estadoCuenta.deudas.slice(0, 3)"
            :key="deuda.idHitoPedido"
            class="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-blanco/70 px-3 py-2"
          >
            <span class="min-w-0 text-sm text-cafe">
              <span class="font-semibold text-granate">{{ deuda.nombreServicio }}</span>
              · {{ deuda.nombreHito }}
            </span>
            <span class="flex items-center gap-3">
              <span class="text-sm font-semibold" :class="claseDeuda(deuda)">
                {{ formatearPrecio(deuda.montoCobro, deuda.monedaCobro || 'ARS') }}
              </span>
              <router-link
                :to="{ name: 'pedido-detalle', params: { idPedido: deuda.idPedido } }"
                class="text-xs font-semibold text-rojizo underline-offset-2 hover:underline"
              >
                Ver pedido
              </router-link>
            </span>
          </li>
        </ul>
        <p v-if="estadoCuenta.deudas?.length > 3" class="mt-2 text-xs text-cafe/60">
          y {{ estadoCuenta.deudas.length - 3 }} más.
        </p>
      </section>

      <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <router-link
          v-for="tarjeta in tarjetas"
          :key="tarjeta.etiqueta"
          :to="tarjeta.to"
          class="group rounded-2xl border border-rosa/60 bg-blanco p-5 transition hover:-translate-y-0.5 hover:border-coral/40 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)] motion-reduce:transition-none motion-reduce:transform-none"
        >
          <div class="flex items-start justify-between gap-3">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">{{ tarjeta.etiqueta }}</p>
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-rosa/25 text-coral">
              <svg class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" :d="tarjeta.icono" />
              </svg>
            </span>
          </div>
          <p
            class="mt-3 font-display text-3xl font-extrabold tabular-nums"
            :class="tarjeta.alerta ? 'text-rojizo' : 'text-granate'"
          >
            {{ tarjeta.valor }}
          </p>
          <p class="mt-1 text-xs text-cafe/55">{{ tarjeta.detalle }}</p>
          <p v-if="tarjeta.notaAlerta" class="mt-1 text-[11px] font-semibold text-rojizo">
            {{ tarjeta.notaAlerta }}
          </p>
        </router-link>
      </div>

      <section class="mt-8 rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="font-display text-xl font-bold text-granate">Requiere tu acción</h2>
            <p class="mt-1 text-sm text-cafe/60">
              Lo urgente primero: aprobaciones, pagos, presupuestos y reseñas.
            </p>
          </div>
          <span
            v-if="atenciones.length"
            class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral tabular-nums"
          >
            {{ atenciones.length }}
          </span>
        </div>

        <ul v-if="atenciones.length" class="mt-4 space-y-2">
          <li v-for="atencion in atenciones" :key="atencion.clave">
            <ItemAtencionPanel
              :titulo="atencion.titulo"
              :detalle="atencion.detalle"
              :to="atencion.to"
              :icono="atencion.icono"
              :tono="atencion.tono"
              :meta="atencion.meta"
            />
          </li>
        </ul>

        <div v-else class="mt-4 flex items-center gap-3 rounded-xl border border-dashed border-rosa/60 px-4 py-6">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-green-500/10 text-green-700">
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
            </svg>
          </span>
          <div>
            <p class="text-sm font-semibold text-granate">Todo al día</p>
            <p class="text-xs text-cafe/60">No tenés aprobaciones, pagos ni presupuestos pendientes.</p>
          </div>
        </div>
      </section>

      <div class="mt-6 grid gap-6 lg:grid-cols-3">
        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6 lg:col-span-2">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-xl font-bold text-granate">Pedidos en curso</h2>
            <router-link
              :to="{ name: 'pedidos' }"
              class="text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Ver todos
            </router-link>
          </div>

          <ul v-if="pedidosVisibles.length" class="mt-4 space-y-2">
            <li v-for="pedido in pedidosVisibles" :key="pedido.idPedido">
              <FilaPedidoActivo :pedido="pedido" />
            </li>
          </ul>

          <div v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center">
            <p class="text-sm text-cafe/60">Todavía no tenés pedidos activos.</p>
            <router-link
              :to="{ name: 'servicios' }"
              class="mt-2 inline-block text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Explorá servicios para contratar
            </router-link>
          </div>
        </section>

        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-xl font-bold text-granate">Compras pendientes</h2>
            <router-link
              :to="{ name: 'compras', query: { grupo: 'pendientes' } }"
              class="text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Ver todas
            </router-link>
          </div>

          <ul v-if="comprasPendientes.length" class="mt-4 space-y-2">
            <li v-for="compra in comprasPendientes" :key="compra.idCompra">
              <FilaCompra :compra="compra" />
            </li>
          </ul>

          <div v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center">
            <p class="text-sm text-cafe/60">No tenés compras pendientes de pago.</p>
            <router-link
              :to="{ name: 'productos' }"
              class="mt-2 inline-block text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Explorá productos
            </router-link>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>
