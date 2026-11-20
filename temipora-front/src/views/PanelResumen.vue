<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { estadoServicioDe } from '../servicios/servicios'
import { datosContraparte, estadoDe, formatearFechaSolicitud } from '../servicios/solicitudes'
import { hayContenidoTerminos } from '../servicios/terminos'
import { formatearPrecio } from '../servicios/moneda'
import { usePanelArtista } from '../composables/usePanelArtista'
import ItemAtencionPanel from '../components/ItemAtencionPanel.vue'
import FilaPedidoActivo from '../components/FilaPedidoActivo.vue'
import ReputacionChip from '../components/ReputacionChip.vue'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const {
  metricas,
  dinero,
  pedidosActivos,
  hitosAtencion,
  solicitudesRecientes,
  solicitudesPorResponder: solicitudesPendientes,
  presupuestosPorVencer,
  mensajesNoLeidos,
  solicitudesMensajePendientes,
  cuentaCobrosConectada,
  cargando: cargandoPanel,
  error: errorPanel,
  cargar: cargarPanel,
} = usePanelArtista()

const cargandoServicios = ref(true)
const errorServicios = ref('')
const servicios = ref([])
const terminos = ref(null)
const formulario = ref(null)
const terminosConsultados = ref(false)

const ICONOS = {
  servicios: 'M2.25 7.125C2.25 6.504 2.754 6 3.375 6h6c.621 0 1.125.504 1.125 1.125v3.75c0 .621-.504 1.125-1.125 1.125h-6a1.125 1.125 0 0 1-1.125-1.125v-3.75ZM14.25 8.625c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-8.25ZM3.75 16.125c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v2.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-2.25Z',
  pedidos: 'M2.25 13.5h3.86a2.25 2.25 0 0 1 2.012 1.244l.256.512a2.25 2.25 0 0 0 2.013 1.244h3.218a2.25 2.25 0 0 0 2.013-1.244l.256-.512a2.25 2.25 0 0 1 2.013-1.244h3.859m-19.5.338V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18v-4.162c0-.224-.034-.447-.1-.661L19.24 5.338a2.25 2.25 0 0 0-2.15-1.588H6.911a2.25 2.25 0 0 0-2.15 1.588L2.35 13.177a2.25 2.25 0 0 0-.1.661Z',
  solicitudes: 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z',
  entregas: 'M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5',
  dinero: 'M2.25 18.75a60.07 60.07 0 0 1 15.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 0 1 3 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 0 0-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 0 1-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 0 0 3 15h-.75M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Zm3 0h.008v.008H18V10.5Zm-12 0h.008v.008H6V10.5Z',
  presupuesto: 'M12 6v12m-3-2.818.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z',
  terminos: 'M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m5.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z',
  formulario: 'M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25Z',
  alerta: 'M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z',
  mensajes: 'M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z',
  cobros: 'M2.25 8.25h19.5M2.25 9h19.5m-16.5 5.25h6m-6 2.25h3m-3.75 3h15a2.25 2.25 0 0 0 2.25-2.25V6.75A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25v10.5A2.25 2.25 0 0 0 4.5 19.5Z',
}

const perfilPublico = computed(() =>
  nombreUsuario
    ? { name: 'perfil', params: { nombreUsuario } }
    : { name: 'inicio' },
)

const cargando = computed(() => cargandoServicios.value || cargandoPanel.value)
const error = computed(() => errorServicios.value || errorPanel.value)

const serviciosPublicados = computed(() =>
  servicios.value.filter((servicio) => servicio.estado === 'PUBLICADO'),
)
const serviciosPausados = computed(() =>
  servicios.value.filter((servicio) => servicio.estado === 'PAUSADO'),
)
const terminosListos = computed(() => hayContenidoTerminos({ form: terminos.value || {} }))
const usaFormularioSistema = computed(() => formulario.value?.esSistema === true)

const resumenTexto = computed(() => {
  const m = metricas.value
  const partes = []
  if (m.pedidosEnCurso) {
    partes.push(`${m.pedidosEnCurso} ${m.pedidosEnCurso === 1 ? 'pedido en curso' : 'pedidos en curso'}`)
  }
  if (m.entregasPendientes) {
    partes.push(`${m.entregasPendientes} ${m.entregasPendientes === 1 ? 'entrega pendiente' : 'entregas pendientes'}`)
  }
  if (m.solicitudesPorResponder) {
    partes.push(`${m.solicitudesPorResponder} ${m.solicitudesPorResponder === 1 ? 'solicitud por responder' : 'solicitudes por responder'}`)
  }
  if (partes.length) {
    return `Tenés ${partes.join(', ')}.`
  }
  if (!servicios.value.length) {
    return 'Empezá creando tu primer servicio para recibir solicitudes.'
  }
  const publicados = serviciosPublicados.value.length
  return `Tenés ${publicados} ${publicados === 1 ? 'servicio publicado' : 'servicios publicados'} y todo al día.`
})

const tarjetas = computed(() => {
  const m = metricas.value
  return [
    {
      etiqueta: 'Pedidos en curso',
      valor: m.pedidosEnCurso,
      detalle: m.pedidosPendientesPago
        ? `${m.pedidosPendientesPago} pendiente(s) de pago`
        : m.pedidosEnCurso
          ? 'Todo en marcha'
          : 'Sin pedidos activos',
      to: { name: 'panel-pedidos' },
      icono: ICONOS.pedidos,
      monto: false,
    },
    {
      etiqueta: 'Entregas pendientes',
      valor: m.entregasPendientes,
      detalle: m.entregasConRevision
        ? `${m.entregasConRevision} con revisión del cliente`
        : m.entregasPendientes
          ? 'Esperando tu entrega'
          : 'Nada para entregar',
      to: { name: 'panel-pedidos', query: { grupo: 'accion' } },
      icono: ICONOS.entregas,
      monto: false,
    },
    {
      etiqueta: 'Por responder',
      valor: m.solicitudesPorResponder,
      detalle: m.solicitudesEnEspera
        ? `${m.solicitudesEnEspera} en lista de espera`
        : m.solicitudesPorResponder
          ? 'Pendientes de tu presupuesto'
          : 'Sin solicitudes pendientes',
      to: { name: 'panel-solicitudes', query: { grupo: 'por-responder' } },
      icono: ICONOS.solicitudes,
      monto: false,
    },
    {
      etiqueta: 'Por cobrar',
      valor: formatearPrecio(dinero.value.porCobrar, dinero.value.monedaMostrada),
      detalle: `Recibido este mes: ${formatearPrecio(dinero.value.cobradoMes, dinero.value.monedaMostrada)}`,
      to: '/panel/ventas',
      icono: ICONOS.dinero,
      monto: true,
    },
  ]
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

function tiempoDesde(valor) {
  if (!valor) return ''
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  const horas = Math.floor((Date.now() - fecha.getTime()) / 3600000)
  if (horas < 1) return 'hace instantes'
  if (horas < 24) return `hace ${horas} ${horas === 1 ? 'hora' : 'horas'}`
  const dias = Math.floor(horas / 24)
  return `hace ${dias} ${dias === 1 ? 'día' : 'días'}`
}

const pedidosPorId = computed(() => {
  const mapa = new Map()
  for (const pedido of pedidosActivos.value) {
    mapa.set(pedido.idPedido, pedido)
  }
  return mapa
})

const atenciones = computed(() => {
  const items = []
  const prioridad = { revision: 0, vencido: 1, entrega: 2 }
  const grupos = new Map()

  for (const hito of hitosAtencion.value) {
    const motivo = hito.estadoEntrega === 'CON_REVISION'
      ? 'revision'
      : hito.estadoPago === 'VENCIDO'
        ? 'vencido'
        : hito.puedeEntregar
          ? 'entrega'
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
    const to = { name: 'panel-pedido-detalle', params: { idPedido: grupo.idPedido } }

    if (grupo.motivo === 'revision') {
      items.push({
        clave: `revision-${grupo.idPedido}`,
        titulo: `${contraparte?.nombre || 'El cliente'} pidió cambios${extra}`,
        detalle: `${servicio} · ${primero.nombre}`,
        to,
        icono: ICONOS.alerta,
        tono: 'alerta',
        meta: 'Espera tu nueva versión',
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
        meta: 'Plazo vencido',
      })
      continue
    }

    const limite = primero.fechaLimitePago || pedido?.limiteEntrega
    items.push({
      clave: `entrega-${grupo.idPedido}`,
      titulo: `Falta subir tu entrega${extra}`,
      detalle: `${servicio} · ${primero.nombre}`,
      to,
      icono: ICONOS.entregas,
      tono: diasHasta(limite) <= 3 ? 'alerta' : 'info',
      meta: textoVencimiento(limite),
    })
  }

  for (const solicitud of presupuestosPorVencer.value) {
    const cliente = datosContraparte(solicitud.cliente)
    items.push({
      clave: `presupuesto-${solicitud.idSolicitud}`,
      titulo: `Tu presupuesto para ${cliente?.nombre || 'un cliente'} está por vencer`,
      detalle: `${solicitud.nombreServicio || 'Servicio'} · ${formatearPrecio(solicitud.precioFinalMostrado, solicitud.monedaMostrada)}`,
      to: { name: 'panel-solicitud-detalle', params: { idSolicitud: solicitud.idSolicitud } },
      icono: ICONOS.presupuesto,
      tono: 'alerta',
      meta: textoVencimiento(solicitud.fechaVencimiento),
    })
  }

  const porResponder = metricas.value.solicitudesPorResponder
  if (porResponder) {
    const masVieja = solicitudesPendientes.value[0]
    const cliente = datosContraparte(masVieja?.cliente)
    items.push({
      clave: 'solicitudes',
      titulo: porResponder === 1
        ? 'Una solicitud espera tu respuesta'
        : `${porResponder} solicitudes esperan tu respuesta`,
      detalle: masVieja
        ? `La más antigua es de ${cliente?.nombre || 'un cliente'}.`
        : 'Revisalas para presupuestar o rechazar.',
      to: { name: 'panel-solicitudes', query: { grupo: 'por-responder' } },
      icono: ICONOS.solicitudes,
      tono: 'info',
      meta: masVieja ? `Llegó ${tiempoDesde(masVieja.fechaCreacion)}` : '',
    })
  }

  if (mensajesNoLeidos.value) {
    items.push({
      clave: 'mensajes',
      titulo: mensajesNoLeidos.value === 1
        ? 'Tenés un mensaje sin leer'
        : `Tenés ${mensajesNoLeidos.value} mensajes sin leer`,
      detalle: 'Respondé para no frenar tus pedidos.',
      to: { name: 'panel-mensajes' },
      icono: ICONOS.mensajes,
      tono: 'info',
      meta: '',
    })
  }

  if (solicitudesMensajePendientes.value) {
    items.push({
      clave: 'solicitudes-mensaje',
      titulo: solicitudesMensajePendientes.value === 1
        ? 'Una persona quiere contactarte'
        : `${solicitudesMensajePendientes.value} personas quieren contactarte`,
      detalle: 'Aceptá la solicitud para habilitar la conversación.',
      to: { name: 'panel-mensajes' },
      icono: ICONOS.mensajes,
      tono: 'info',
      meta: '',
    })
  }

  if (!cuentaCobrosConectada.value) {
    items.push({
      clave: 'cobros',
      titulo: 'Conectá tu cuenta de cobros',
      detalle: 'Sin MercadoPago no vas a poder cobrar tus pedidos.',
      to: '/configuracion/pagos-y-cobros',
      icono: ICONOS.cobros,
      tono: 'alerta',
      meta: 'Acción requerida',
    })
  }

  return items
})

const atencionesVisibles = computed(() => atenciones.value.slice(0, 5))
const atencionesRestantes = computed(() => Math.max(0, atenciones.value.length - 5))

const configuracion = computed(() => {
  const items = []

  if (!servicios.value.length) {
    items.push({
      clave: 'primer-servicio',
      titulo: 'Creá tu primer servicio',
      detalle: 'Definí precio, tiempos y cupos para empezar a recibir pedidos.',
      to: '/panel/servicios/crear',
      icono: ICONOS.servicios,
      tono: 'info',
      meta: '',
    })
  }

  if (terminosConsultados.value && !terminosListos.value) {
    items.push({
      clave: 'terminos',
      titulo: 'Completá tus términos y condiciones',
      detalle: 'Los necesitás para publicar servicios y recibir solicitudes.',
      to: '/panel/terminos',
      icono: ICONOS.terminos,
      tono: 'info',
      meta: '',
    })
  }

  if (serviciosPausados.value.length) {
    const pausados = serviciosPausados.value.length
    items.push({
      clave: 'pausados',
      titulo: pausados === 1
        ? 'Tenés un servicio pausado'
        : `Tenés ${pausados} servicios pausados`,
      detalle: 'Mientras estén pausados no reciben solicitudes nuevas.',
      to: '/panel/servicios',
      icono: ICONOS.servicios,
      tono: 'info',
      meta: '',
    })
  }

  if (usaFormularioSistema.value) {
    items.push({
      clave: 'formulario',
      titulo: 'Estás usando el formulario del sistema',
      detalle: 'Personalizalo para pedir exactamente lo que necesitás.',
      to: '/panel/formulario-pedidos',
      icono: ICONOS.formulario,
      tono: 'info',
      meta: '',
    })
  }

  return items
})

const pedidosVisibles = computed(() => pedidosActivos.value.slice(0, 5))

const serviciosDestacados = computed(() =>
  [...servicios.value]
    .sort((a, b) => {
      if (a.estado !== b.estado) return a.estado === 'PUBLICADO' ? -1 : 1
      return (b.solicitudesActivas || 0) - (a.solicitudesActivas || 0)
    })
    .slice(0, 4),
)

function clienteDe(solicitud) {
  return datosContraparte(solicitud?.cliente)
}

async function cargarServicios() {
  cargandoServicios.value = true
  errorServicios.value = ''
  try {
    const [serviciosRes, perfilRes] = await Promise.all([
      api.get('/mis-servicios'),
      nombreUsuario ? api.get(`/perfil/${nombreUsuario}`).catch(() => null) : Promise.resolve(null),
    ])

    servicios.value = serviciosRes.data || []

    const idUsuario = perfilRes?.data?.idUsuario
    if (idUsuario) {
      const [terminosRes, formularioRes] = await Promise.all([
        api.get(`/usuarios/${idUsuario}/terminos`).catch(() => null),
        api.get(`/usuarios/${idUsuario}/formulario-pedidos`).catch(() => null),
      ])
      terminos.value = terminosRes?.data || null
      formulario.value = formularioRes?.data || null
      terminosConsultados.value = true
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorServicios.value = e.response?.data?.message || 'No se pudo cargar el resumen.'
  } finally {
    cargandoServicios.value = false
  }
}

function recargar() {
  cargarServicios()
  cargarPanel()
}

onMounted(() => {
  cargarServicios()
  cargarPanel()
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="Tu espacio de trabajo"
      :titulo="nombreUsuario ? `Hola, @${nombreUsuario}` : 'Mi panel'"
      :descripcion="resumenTexto"
      :nivel="2"
    >
      <template #acciones>
        <router-link
          to="/panel/servicios/crear"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
        >
          Crear servicio
        </router-link>
        <router-link
          :to="perfilPublico"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        >
          Ver mi perfil público
        </router-link>
      </template>
    </PageHeader>

    <div v-if="cargando" class="mt-8 min-h-[calc(100vh-4rem)] space-y-6" role="status" aria-live="polite">
      <span class="sr-only">Cargando resumen...</span>
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="n in 4" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      </div>
      <div class="h-40 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      <div class="grid gap-6 lg:grid-cols-3">
        <div class="h-64 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70 lg:col-span-2" aria-hidden="true"></div>
        <div class="h-64 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      </div>
    </div>

    <div v-else-if="error" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="recargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <div class="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <router-link
          v-for="tarjeta in tarjetas"
          :key="tarjeta.etiqueta"
          :to="tarjeta.to"
          class="group rounded-2xl border border-rosa/60 bg-blanco p-5 transition hover:-translate-y-0.5 hover:border-coral/40 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)]"
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
            class="mt-3 font-display font-extrabold text-granate"
            :class="tarjeta.monto ? 'text-2xl leading-tight' : 'text-3xl'"
          >
            {{ tarjeta.valor }}
          </p>
          <p class="mt-1 text-xs text-cafe/55">{{ tarjeta.detalle }}</p>
        </router-link>
      </div>

      <section class="mt-8 rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-xl font-bold text-granate">Requiere tu atención</h2>
          <span
            v-if="atenciones.length"
            class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
          >
            {{ atenciones.length }}
          </span>
        </div>

        <ul v-if="atencionesVisibles.length" class="mt-4 space-y-2">
          <li v-for="atencion in atencionesVisibles" :key="atencion.clave">
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

        <p v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-6 text-center text-sm text-cafe/60">
          Todo al día. No hay nada pendiente por ahora.
        </p>

        <p v-if="atencionesRestantes" class="mt-3 text-xs text-cafe/55">
          y {{ atencionesRestantes }} más para revisar en tus solicitudes y pedidos.
        </p>
      </section>

      <div class="mt-8 grid gap-6 lg:grid-cols-3">
        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6 lg:col-span-2">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-xl font-bold text-granate">Pedidos activos</h2>
            <router-link
              :to="{ name: 'panel-pedidos' }"
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
            <p class="mt-1 text-xs text-cafe/50">Cuando un cliente pague un presupuesto, el pedido aparece acá.</p>
          </div>
        </section>

        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-xl font-bold text-granate">Solicitudes recientes</h2>
            <router-link
              :to="{ name: 'panel-solicitudes' }"
              class="text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Ver todas
            </router-link>
          </div>

          <ul v-if="solicitudesRecientes.length" class="mt-4 space-y-2">
            <li v-for="solicitud in solicitudesRecientes" :key="solicitud.idSolicitud">
              <router-link
                :to="{ name: 'panel-solicitud-detalle', params: { idSolicitud: solicitud.idSolicitud } }"
                class="group block rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10"
              >
                <div class="flex items-center gap-3">
                  <img
                    v-if="clienteDe(solicitud)?.foto"
                    :src="clienteDe(solicitud).foto"
                    alt=""
                    class="h-9 w-9 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
                  />
                  <span
                    v-else
                    class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate"
                    aria-hidden="true"
                  >
                    {{ clienteDe(solicitud)?.inicial || '?' }}
                  </span>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-semibold text-granate">
                      {{ clienteDe(solicitud)?.nombre || 'Usuario no disponible' }}
                    </p>
                    <p class="truncate text-xs text-cafe/60">
                      {{ solicitud.nombreServicio || 'Servicio' }} · {{ solicitud.cantidadDibujos }}
                      {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }}
                    </p>
                  </div>
                  <span
                    class="shrink-0 whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                    :class="estadoDe(solicitud.estado).clase"
                  >
                    {{ estadoDe(solicitud.estado).texto }}
                  </span>
                </div>
                <div class="mt-2 flex flex-wrap items-center justify-between gap-2">
                  <ReputacionChip :reputacion="solicitud.reputacionCliente" />
                  <span class="text-[11px] text-cafe/45">
                    {{ formatearFechaSolicitud(solicitud.fechaCreacion, { corta: true }) }}
                  </span>
                </div>
              </router-link>
            </li>
          </ul>

          <div v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center">
            <p class="text-sm text-cafe/60">Todavía no recibiste solicitudes.</p>
            <router-link
              to="/panel/servicios"
              class="mt-2 inline-block text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Revisá tus servicios
            </router-link>
          </div>
        </section>
      </div>

      <section class="mt-6 rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-xl font-bold text-granate">Tus servicios</h2>
          <router-link
            to="/panel/servicios"
            class="text-sm font-semibold text-coral transition hover:text-rojizo"
          >
            Ver todos
          </router-link>
        </div>

        <div v-if="serviciosDestacados.length" class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <router-link
            v-for="servicio in serviciosDestacados"
            :key="servicio.idServicio"
            :to="`/panel/servicios/${servicio.idServicio}/solicitudes`"
            class="group rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10"
          >
            <div class="flex items-center gap-2">
              <img
                v-if="servicio.imagenPortada"
                :src="servicio.imagenPortada"
                alt=""
                class="h-9 w-9 shrink-0 rounded-lg object-cover ring-1 ring-rosa/50"
              />
              <span
                v-else
                class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-rosa/30 text-granate"
                aria-hidden="true"
              >
                <svg class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="ICONOS.servicios" />
                </svg>
              </span>
              <p class="min-w-0 flex-1 truncate text-sm font-semibold text-granate">{{ servicio.nombre }}</p>
            </div>
            <div class="mt-2 flex items-center justify-between gap-2">
              <span class="text-xs text-cafe/60">
                {{ servicio.cuposDisponibles ?? servicio.cupos ?? 0 }} cupos ·
                {{ servicio.solicitudesActivas || 0 }} activas
              </span>
              <span
                class="shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-semibold"
                :class="estadoServicioDe(servicio.estado).clase"
              >
                {{ estadoServicioDe(servicio.estado).texto }}
              </span>
            </div>
          </router-link>
        </div>

        <div v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center">
          <p class="text-sm text-cafe/60">Todavía no creaste servicios.</p>
          <router-link
            to="/panel/servicios/crear"
            class="mt-2 inline-block text-sm font-semibold text-coral transition hover:text-rojizo"
          >
            Crear mi primer servicio
          </router-link>
        </div>
      </section>

      <details
        v-if="configuracion.length"
        class="group mt-6 rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6"
      >
        <summary class="flex cursor-pointer list-none items-center justify-between gap-3 [&::-webkit-details-marker]:hidden">
          <span class="font-display text-lg font-bold text-granate">Configuración pendiente</span>
          <span class="flex items-center gap-2">
            <span class="rounded-full bg-mantequilla/60 px-3 py-1 text-xs font-semibold text-cafe">
              {{ configuracion.length }}
            </span>
            <svg
              class="h-4 w-4 text-cafe/40 transition group-open:rotate-180"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
            </svg>
          </span>
        </summary>

        <ul class="mt-4 space-y-2">
          <li v-for="item in configuracion" :key="item.clave">
            <ItemAtencionPanel
              :titulo="item.titulo"
              :detalle="item.detalle"
              :to="item.to"
              :icono="item.icono"
              :tono="item.tono"
              :meta="item.meta"
            />
          </li>
        </ul>
      </details>
    </template>
  </div>
</template>
