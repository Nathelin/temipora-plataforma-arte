<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearMonto, formatearPrecio } from '../servicios/moneda'
import { descripcionMedioPago } from '../servicios/ventas'
import EntregasHito from '../components/EntregasHito.vue'
import HiloConversacion from '../components/HiloConversacion.vue'
import IconoDescarga from '../components/IconoDescarga.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import SubirEntregable from '../components/SubirEntregable.vue'
import FormularioResenia from '../components/FormularioResenia.vue'
import TarjetaResenia from '../components/TarjetaResenia.vue'
import { useNotificaciones } from '../composables/useNotificaciones'
import { useHiloConversacion } from '../composables/useHiloConversacion'
import { useSubirImagen } from '../composables/useSubirImagen'
import { estadoConversacionDe } from '../servicios/mensajes'
import {
  cargarCriterios,
  cargarEstadoResenia,
  crearResenia,
  editarResenia,
  eliminarResenia,
} from '../servicios/resenias'
import {
  descripcionHito,
  estadoEntregaDe,
  estadoPagoDe,
  estadoPedidoDe,
  etiquetaTipoHito,
  formatearFecha,
  formatearFechaHora,
  hitoPrioritario,
  pagoPendiente,
  resumenPagos,
} from '../servicios/pedidos'

const route = useRoute()
const router = useRouter()
const idPedido = computed(() => Number(route.params.idPedido))

const pedido = ref(null)
const cargando = ref(true)
const error = ref('')
const procesando = ref(false)
const avisoPago = ref('')
const sondeandoPago = ref(false)
const descargandoConstancia = ref(false)
const revisionHitoId = ref(null)
const motivoRevision = ref('')
const visor = reactive({ abierto: false, imagen: '' })
const { abrirVerificada } = useSubirImagen()
const { exito, error: errorToast } = useNotificaciones()
const idsEnPortfolio = ref([])
const pestanaActiva = ref('resumen')
const tabsRef = ref(null)
const contenidoRef = ref(null)
const hitoAbierto = ref(null)
const confirmacion = reactive({
  abierto: false,
  titulo: '',
  mensaje: '',
  textoConfirmar: 'Confirmar',
  accion: null,
})

const chatMontado = ref(false)
const idConversacionChat = computed(() => pedido.value?.idConversacion ?? null)
const chatActivo = computed(() => chatMontado.value
  && pestanaActiva.value === 'chat'
  && Boolean(idConversacionChat.value))

const {
  conversacion: chatConversacion,
  mensajes: chatMensajes,
  hayMas: chatHayMas,
  cargando: chatCargando,
  cargandoMas: chatCargandoMas,
  error: chatError,
  errorEnvio: chatErrorEnvio,
  texto: chatTexto,
  enviando: chatEnviando,
  contraparteEscribiendo: chatEscribiendo,
  contraparte: chatContraparte,
  maxMensaje: chatMaxMensaje,
  esAceptada: chatAceptada,
  cargarAnteriores: chatCargarAnteriores,
  enviar: chatEnviar,
  avisarEscribiendo: chatAvisarEscribiendo,
} = useHiloConversacion(idConversacionChat, { activo: chatActivo })

const enlaceConversacion = computed(() => {
  const idConversacion = chatConversacion.value?.idConversacion
    ?? idConversacionChat.value
  if (!idConversacion) return null
  return {
    name: route.path.startsWith('/panel') ? 'panel-conversacion' : 'conversacion',
    params: { idConversacion },
    query: { volverA: 'pedido', idPedido: idPedido.value },
  }
})

const reseniaEstado = ref(null)
const criteriosResenia = ref([])
const cargandoResenias = ref(false)
const guardandoResenia = ref(false)
const editandoResenia = ref(false)
const errorResenia = ref('')
const advertenciaCriterios = ref('')

const esArtista = computed(() => pedido.value?.rolVista === 'ARTISTA')
const esCliente = computed(() => pedido.value?.rolVista === 'CLIENTE')
const puedeVerResenias = computed(() => (
  pedido.value?.estado === 'FINALIZADO' && pedido.value?.rolVista !== 'ADMIN'
))
const estadoInfo = computed(() => estadoPedidoDe(pedido.value?.estado))
const hitos = computed(() => pedido.value?.hitos || [])
const moneda = computed(
  () => pedido.value?.monedaMostrada || pedido.value?.moneda || 'ARS',
)
const monedaCobro = computed(() => pedido.value?.monedaCobro || 'ARS')
const total = computed(() => formatearPrecio(
  esCliente.value
    ? (pedido.value?.precioFinalClienteMostrado
        ?? pedido.value?.precioFinalMostrado
        ?? pedido.value?.precioFinal)
    : (pedido.value?.precioFinalMostrado ?? pedido.value?.precioFinal),
  moneda.value,
))
const descuentoMostrado = computed(
  () => pedido.value?.descuentoMostrado
    ?? pedido.value?.descuentoMonto
    ?? 0,
)
const cobroDistinto = computed(
  () => pedido.value?.precioFinalCobro != null
    && moneda.value !== monedaCobro.value,
)

function formatearMontoHito(hito) {
  return formatearPrecio(
    hito.montoMostrado ?? hito.monto,
    moneda.value,
  )
}

function pagoAprobado(hito) {
  return (hito?.pagos || []).find((pago) => pago.estado === 'APROBADO') || null
}

function tieneDatosMp(hito) {
  const pago = pagoAprobado(hito)
  return Boolean(pago && (pago.arancelMp != null
    || pago.netoRecibidoMp != null
    || pago.medioPago))
}

function netoHito(hito) {
  const pago = pagoAprobado(hito)
  if (pago?.netoRecibidoMp != null) {
    return formatearPrecio(pago.netoRecibidoMp, monedaCobro.value)
  }
  return formatearPrecio(
    hito.montoNetoConArancelMostrado ?? hito.montoNetoMostrado ?? hito.montoNeto,
    moneda.value,
  )
}

function arancelMpHito(hito) {
  return formatearPrecio(
    pagoAprobado(hito)?.arancelMp,
    monedaCobro.value,
  )
}

function liberacionHito(hito) {
  const fecha = pagoAprobado(hito)?.fechaLiberacion
  return fecha ? formatearFecha(fecha) : null
}

const pestanas = computed(() => {
  const lista = [
    { id: 'resumen', etiqueta: 'Resumen' },
    { id: 'plan-pagos', etiqueta: 'Plan de pagos' },
  ]
  if (pedido.value?.idConversacion) {
    lista.push({ id: 'chat', etiqueta: 'Chat' })
  }
  if (esArtista.value || esCliente.value) {
    lista.push({ id: 'galeria', etiqueta: 'Galería' })
  }
  if (puedeVerResenias.value) {
    lista.push({ id: 'resenas', etiqueta: 'Reseñas' })
  }
  return lista
})

const resumen = computed(() => resumenPagos(hitos.value))

const proximoPaso = computed(() => {
  if (!pedido.value) return null
  const estado = pedido.value.estado
  if (!['PENDIENTE_PAGO', 'EN_CURSO', 'FINALIZADO'].includes(estado)) return null

  if (esCliente.value && estado !== 'FINALIZADO') {
    const hitoPendiente = hitos.value.find((hito) => pagoPendiente(hito))
    if (hitoPendiente) {
      return {
        titulo: `Hay un pago pendiente en «${hitoPendiente.nombre}»`,
        detalle: 'Completá el pago para activar el hito.',
        accion: 'Ver pago',
        destino: 'plan-pagos',
        ancla: 'pago',
        idHitoPedido: hitoPendiente.idHitoPedido,
      }
    }

    const hitoPagar = hitos.value.find((hito) => hito.puedePagar)
    if (hitoPagar) {
      return {
        titulo: `Tenés que pagar «${hitoPagar.nombre}»`,
        detalle: `${formatearMontoHito(hitoPagar)} para que el pedido siga avanzando.`,
        accion: 'Ir a pagar',
        destino: 'plan-pagos',
        ancla: 'hito',
        idHitoPedido: hitoPagar.idHitoPedido,
      }
    }

    const hitoAprobar = hitos.value.find((hito) => hito.puedeAprobar)
    if (hitoAprobar) {
      return {
        titulo: `Hay una entrega para revisar en «${hitoAprobar.nombre}»`,
        detalle: 'Revisá el entregable y aprobalo o pedí una revisión.',
        accion: 'Revisar entrega',
        destino: 'plan-pagos',
        ancla: 'aprobar',
        idHitoPedido: hitoAprobar.idHitoPedido,
      }
    }
  }

  const hitoEntregar = hitos.value.find((hito) => hito.puedeEntregar)
  if (hitoEntregar) {
    return {
      titulo: `Falta subir la entrega de «${hitoEntregar.nombre}»`,
      detalle: 'Cuando la subas, el cliente va a poder aprobarla.',
      accion: 'Subir entrega',
      destino: 'plan-pagos',
      ancla: 'entrega',
      idHitoPedido: hitoEntregar.idHitoPedido,
    }
  }

  if (puedeVerResenias.value && reseniaEstado.value?.puedeCrear) {
    return {
      titulo: 'El pedido terminó: dejá tu reseña',
      detalle: esArtista.value
        ? 'Tu reseña interna ayuda a la reputación del cliente.'
        : 'Tu opinión ayuda a la reputación del artista.',
      accion: 'Reseñar',
      destino: 'resenas',
      idHitoPedido: null,
    }
  }

  return null
})

const claseFondoBarra = computed(() => (
  route.path.startsWith('/admin') ? 'bg-blanco' : 'bg-crema'
))

function claseNodoHito(hito) {
  if (hito.puedePagar || hito.puedeAprobar || hito.puedeEntregar || pagoPendiente(hito)) {
    return 'border-coral bg-coral text-white'
  }
  if (hito.estadoPago === 'PAGADO') {
    return 'border-green-700/40 bg-green-700/10 text-green-700'
  }
  return 'border-rosa bg-blanco text-cafe/60'
}

function aplicarPestanaDesdeUrl() {
  const ids = pestanas.value.map((pestana) => pestana.id)
  const tab = route.query.tab
  pestanaActiva.value = ids.includes(tab) ? tab : 'resumen'
  if (pestanaActiva.value === 'chat') chatMontado.value = true
}

function sincronizarUrl() {
  const query = { ...route.query }
  if (pestanaActiva.value === 'resumen') {
    delete query.tab
  } else {
    query.tab = pestanaActiva.value
  }
  if (query.tab === route.query.tab) return
  router.replace({ query })
}

function cambiarPestana(pestana) {
  if (pestanaActiva.value === pestana) return
  if (pestana === 'chat') chatMontado.value = true

  const tabs = tabsRef.value
  const alturaHeader = window.innerWidth >= 640 ? 64 : 56
  const estabaFijo = tabs && tabs.getBoundingClientRect().top <= alturaHeader + 1

  pestanaActiva.value = pestana
  sincronizarUrl()

  if (!estabaFijo || !contenidoRef.value) return

  const top = contenidoRef.value.getBoundingClientRect().top
    + window.scrollY
    - alturaHeader
    - tabs.getBoundingClientRect().height
    - 12
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}

function navegarPestanas(event) {
  const ids = pestanas.value.map((pestana) => pestana.id)
  const actual = ids.indexOf(pestanaActiva.value)
  const destino = {
    ArrowRight: ids[(actual + 1) % ids.length],
    ArrowLeft: ids[(actual - 1 + ids.length) % ids.length],
    Home: ids[0],
    End: ids[ids.length - 1],
  }[event.key]
  if (!destino) return
  event.preventDefault()
  cambiarPestana(destino)
  nextTick(() => document.getElementById(`pestana-${destino}`)?.focus())
}

function alternarHito(idHitoPedido) {
  hitoAbierto.value = hitoAbierto.value === idHitoPedido ? null : idHitoPedido
}

function irAlProximoPaso() {
  const paso = proximoPaso.value
  if (!paso) return

  if (paso.destino === 'resenas') {
    cambiarPestana('resenas')
    return
  }

  pestanaActiva.value = 'plan-pagos'
  hitoAbierto.value = paso.idHitoPedido
  sincronizarUrl()

  const ancla = {
    pago: `pago-${paso.idHitoPedido}`,
    aprobar: `aprobar-${paso.idHitoPedido}`,
    entrega: `entrega-${paso.idHitoPedido}`,
    hito: `hito-card-${paso.idHitoPedido}`,
  }[paso.ancla] || `hito-card-${paso.idHitoPedido}`

  nextTick(() => {
    document.getElementById(ancla)
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

watch(() => route.query.tab, aplicarPestanaDesdeUrl)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/pedidos/${idPedido.value}`)
    pedido.value = res.data
    hitoAbierto.value = hitoPrioritario(res.data?.hitos || [])?.idHitoPedido ?? null
    aplicarPestanaDesdeUrl()
    if (res.data?.rolVista === 'CLIENTE') {
      await sincronizarPagoPendiente()
    }
    if (res.data?.rolVista === 'ARTISTA') {
      void cargarPortfolioVerificado(res.data)
    }
    if (res.data?.estado === 'FINALIZADO' && res.data?.rolVista !== 'ADMIN') {
      void cargarResenias()
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value = e.response?.status === 403 || e.response?.status === 404
      ? 'No podés ver este pedido o ya no existe.'
      : (e.response?.data?.message || 'No se pudo cargar el pedido.')
  } finally {
    cargando.value = false
  }
}

async function cargarPortfolioVerificado(data = pedido.value) {
  const nombre = data?.artista?.nombreUsuario
  if (!nombre) {
    idsEnPortfolio.value = []
    return
  }
  try {
    const res = await api.get(`/portfolio/${nombre}/verificadas`)
    idsEnPortfolio.value = (res.data || []).map(
      (verificacion) => verificacion.idArchivo,
    )
  } catch {
    idsEnPortfolio.value = []
  }
}

async function cargarResenias() {
  if (!puedeVerResenias.value) return
  cargandoResenias.value = true
  errorResenia.value = ''
  advertenciaCriterios.value = ''
  try {
    const res = await cargarEstadoResenia(idPedido.value)
    reseniaEstado.value = res.data
    try {
      const criterios = await cargarCriterios(res.data.direccion)
      criteriosResenia.value = criterios.data
    } catch (e) {
      criteriosResenia.value = []
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return
      }
      advertenciaCriterios.value =
        'No se pudieron cargar los criterios de evaluación.'
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorResenia.value = e.response?.data?.message
      || 'No se pudieron cargar las reseñas del pedido.'
  } finally {
    cargandoResenias.value = false
  }
}

async function guardarResenia(payload) {
  if (guardandoResenia.value) return
  const eraEdicion = editandoResenia.value
  guardandoResenia.value = true
  errorResenia.value = ''
  try {
    if (eraEdicion) {
      await editarResenia(idPedido.value, payload)
    } else {
      await crearResenia(idPedido.value, payload)
    }
    editandoResenia.value = false
    exito(eraEdicion ? 'Reseña actualizada.' : 'Reseña publicada.')
    await cargarResenias()
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorResenia.value = e.response?.data?.message
      || 'No se pudo guardar la reseña.'
  } finally {
    guardandoResenia.value = false
  }
}

function eliminarMiResenia() {
  pedirConfirmacion({
    titulo: '¿Eliminar tu reseña?',
    mensaje: 'La reseña dejará de ser visible para el artista y no contará para su reputación. '
      + 'El historial se conserva, pero no vas a poder volver a reseñar este pedido.',
    textoConfirmar: 'Eliminar reseña',
  }, async () => {
    errorResenia.value = ''
    try {
      await eliminarResenia(idPedido.value)
      editandoResenia.value = false
      exito('Eliminaste tu reseña.')
      await cargarResenias()
    } catch (e) {
      errorResenia.value = e.response?.data?.message
        || 'No se pudo eliminar la reseña.'
    }
  })
}

function agregarAlPortfolio({ archivo, entrega }) {
  if (!archivo) return
  abrirVerificada(archivo, entrega?.descripcion || '', pedido.value?.nombreServicio || '')
  router.push({ name: 'portfolio-subir' })
}

async function ejecutar(accion, mensaje) {
  if (procesando.value) return false
  procesando.value = true
  try {
    const res = await accion()
    if (res?.data) pedido.value = res.data
    exito(mensaje)
    return true
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return false
    }
    errorToast(e.response?.data?.message
      || 'No se pudo completar la acción.')
    return false
  } finally {
    procesando.value = false
  }
}

async function sincronizarPagoPendiente() {
  const pendiente = (pedido.value?.hitos || [])
    .flatMap((hito) => hito.pagos || [])
    .find((pago) => pago.estado === 'PENDIENTE'
      && pago.proveedor === 'MERCADOPAGO')
  if (!pendiente) return
  try {
    const res = await api.post(`/pagos/${pendiente.idPago}/sincronizar`)
    if (res.data) pedido.value = res.data
  } catch {
    // Si el proveedor no responde, seguimos con el polling normal.
  }
}

function urlCheckoutMercadoPago(data) {
  const pendiente = (data?.hitos || [])
    .flatMap((hito) => hito.pagos || [])
    .find((pago) => pago.estado === 'PENDIENTE')
  return pendiente?.proveedor === 'MERCADOPAGO'
    ? pendiente.instruccion
    : null
}

async function pagarHito(hito) {
  if (procesando.value) return
  procesando.value = true
  try {
    await sincronizarPagoPendiente()
    const actualizado = (pedido.value?.hitos || [])
      .find((item) => item.idHitoPedido === hito.idHitoPedido)
    if (actualizado && actualizado.estadoPago !== 'PENDIENTE') {
      exito('El pago ya fue acreditado.')
      return
    }
    const res = await api.post(
      `/pedidos/${idPedido.value}/hitos/${hito.idHitoPedido}/pagar`,
    )
    pedido.value = res.data
    const url = urlCheckoutMercadoPago(res.data)
    if (url) {
      window.location.href = url
      return
    }
    exito('Pago iniciado. Confirmalo para activar el hito.')
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorToast(e.response?.data?.message
      || 'No se pudo completar la acción.')
  } finally {
    procesando.value = false
  }
}

async function refrescarPedido() {
  const res = await api.get(`/pedidos/${idPedido.value}`)
  pedido.value = res.data
}

async function procesarRetornoPago() {
  const resultado = route.query.pago
  if (!resultado || !pedido.value) return
  if (resultado === 'failure') {
    avisoPago.value = 'El pago no se completó. Podés intentarlo de nuevo.'
    return
  }
  avisoPago.value = resultado === 'pending'
    ? 'El pago quedó pendiente. Estamos verificando el estado...'
    : 'Pago recibido. Estamos confirmando con MercadoPago...'
  sondeandoPago.value = true
  try {
    await sincronizarPagoPendiente()
    for (let intento = 0; intento < 5; intento += 1) {
      const hayPendiente = (pedido.value?.hitos || [])
        .flatMap((hito) => hito.pagos || [])
        .some((pago) => pago.estado === 'PENDIENTE')
      if (!hayPendiente) {
        avisoPago.value = 'Pago confirmado.'
        return
      }
      await new Promise((resolve) => setTimeout(resolve, 2000))
      await refrescarPedido()
    }
    avisoPago.value = 'El pago todavía se está procesando. Actualizá la página en unos minutos.'
  } catch {
    avisoPago.value = 'No pudimos verificar el pago todavía. Actualizá la página en unos minutos.'
  } finally {
    sondeandoPago.value = false
  }
}

function confirmarPago(pago) {
  ejecutar(
    () => api.post(`/pagos/simulado/${pago.idPago}/confirmar`),
    'Pago confirmado.',
  )
}

function pedirConfirmacion({ titulo, mensaje, textoConfirmar }, accion) {
  Object.assign(confirmacion, {
    abierto: true,
    titulo,
    mensaje,
    textoConfirmar,
    accion,
  })
}

function cerrarConfirmacion() {
  confirmacion.abierto = false
  confirmacion.accion = null
}

function confirmarAccion() {
  const accion = confirmacion.accion
  cerrarConfirmacion()
  if (accion) accion()
}

function aprobar(hito) {
  pedirConfirmacion({
    titulo: '¿Aprobar el entregable?',
    mensaje: 'Confirmás que estás satisfecho con esta entrega y querés que el artista siga trabajando. Se habilita la siguiente cuota del plan de pagos.',
    textoConfirmar: 'Sí, aprobar',
  }, () => ejecutar(
    () => api.post(`/pedidos/${idPedido.value}/hitos/${hito.idHitoPedido}/aprobar`),
    'Aprobaste el entregable.',
  ))
}

function abrirRevision(hito) {
  revisionHitoId.value = hito.idHitoPedido
  motivoRevision.value = ''
}

function cerrarRevision() {
  revisionHitoId.value = null
  motivoRevision.value = ''
}

async function solicitarRevision(hito) {
  if (!motivoRevision.value.trim()) {
    errorToast('Escribí el motivo de la revisión.')
    return
  }
  const ok = await ejecutar(
    () => api.post(
      `/pedidos/${idPedido.value}/hitos/${hito.idHitoPedido}/solicitar-revision`,
      { motivo: motivoRevision.value.trim() },
    ),
    'Pediste una revisión al artista.',
  )
  if (ok) cerrarRevision()
}

function cancelar() {
  pedirConfirmacion({
    titulo: '¿Cancelar el pedido?',
    mensaje: 'El pedido va a quedar cancelado para el cliente y el artista. Los pagos ya realizados no se devuelven automáticamente.',
    textoConfirmar: 'Sí, cancelar pedido',
  }, () => ejecutar(
    () => api.post(`/pedidos/${idPedido.value}/cancelar`),
    'El pedido fue cancelado.',
  ))
}

function alEntregar(data) {
  if (data) pedido.value = data
  exito('Entregable subido. El cliente fue notificado.')
}

async function autorizarPublicacion(autoriza) {
  await ejecutar(
    () => api.put(
      `/pedidos/${idPedido.value}/publicacion/artista`,
      { autoriza },
    ),
    autoriza
      ? 'Autorizaste la publicación en la galería del cliente.'
      : 'Retiraste la autorización de publicación.',
  )
}

async function ocultarPublicacion(ocultar) {
  await ejecutar(
    () => api.put(
      `/pedidos/${idPedido.value}/publicacion/cliente`,
      { ocultar },
    ),
    ocultar
      ? 'Ocultaste este pedido de tu galería.'
      : 'Este pedido vuelve a mostrarse en tu galería.',
  )
}

function abrirVisor(imagen) {
  visor.imagen = imagen
  visor.abierto = true
}

function cerrarVisor() {
  visor.abierto = false
}

async function descargarConstancia() {
  if (descargandoConstancia.value) return
  descargandoConstancia.value = true
  try {
    const res = await api.get(
      `/pedidos/${idPedido.value}/constancia`,
      { responseType: 'blob' },
    )
    const url = URL.createObjectURL(res.data)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = `constancia-pedido-${idPedido.value}.pdf`
    document.body.appendChild(enlace)
    enlace.click()
    enlace.remove()
    URL.revokeObjectURL(url)
    exito('Constancia descargada.')
  } catch (e) {
    errorToast('No se pudo descargar la constancia.')
  } finally {
    descargandoConstancia.value = false
  }
}

function volver() {
  if (route.path.startsWith('/admin')) {
    router.push({ name: 'pedidos-admin' })
    return
  }
  const enPanel = route.path.startsWith('/panel')
  router.push({ name: enPanel ? 'panel-pedidos' : 'pedidos' })
}

function abrirChat() {
  if (!pedido.value?.idConversacion) return
  chatMontado.value = true
  cambiarPestana('chat')
}

onMounted(async () => {
  await cargar()
  void procesarRetornoPago()
})
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a pedidos
    </button>

    <p v-if="cargando" class="mt-8 py-16 text-center text-sm text-cafe/60">
      Cargando pedido...
    </p>
    <p v-else-if="error" class="mt-8 py-16 text-center text-sm text-rojizo">
      {{ error }}
    </p>

    <template v-else>
      <header class="mt-6 flex flex-wrap items-start justify-between gap-4">
        <div class="min-w-0">
          <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">
            Pedido #{{ pedido.idPedido }}
          </p>
          <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
            {{ pedido.nombreServicio || 'Servicio' }}
          </h1>
          <p v-if="pedido.rolVista === 'ADMIN'" class="mt-2 text-sm text-cafe/65">
            Cliente: @{{ pedido.cliente?.nombreUsuario || '—' }}
            <span aria-hidden="true"> · </span>
            Artista: @{{ pedido.artista?.nombreUsuario || '—' }}
          </p>
          <p v-else class="mt-2 text-sm text-cafe/65">
            {{ esArtista ? 'Cliente' : 'Artista' }}:
            @{{ (esArtista ? pedido.cliente : pedido.artista)?.nombreUsuario || '—' }}
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <button
            v-if="pedido.idConversacion"
            type="button"
            class="inline-flex min-h-9 items-center justify-center gap-1.5 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
            @click="abrirChat"
          >
            Chat
            <span
              v-if="pedido.noLeidos > 0"
              class="inline-flex min-w-5 items-center justify-center rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-bold text-white"
            >
              {{ pedido.noLeidos > 9 ? '9+' : pedido.noLeidos }}
            </span>
          </button>
          <span
            v-if="pedido.rolVista === 'ADMIN'"
            class="rounded-full border border-coral/30 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral"
          >
            Vista de administración
          </span>
          <span
            class="rounded-full border px-3 py-1.5 text-xs font-semibold"
            :class="estadoInfo.clase"
          >
            {{ estadoInfo.etiqueta }}
          </span>
        </div>
      </header>

      <p
        v-if="avisoPago"
        class="mt-4 rounded-xl border border-coral/30 bg-coral/5 px-4 py-3 text-sm text-cafe"
        role="status"
      >
        {{ avisoPago }}
        <span v-if="sondeandoPago" class="text-cafe/60"> Actualizando...</span>
      </p>

      <div
        v-if="proximoPaso"
        class="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-coral/30 bg-coral/5 px-4 py-3"
      >
        <div class="min-w-0">
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-coral">
            Próximo paso
          </p>
          <p class="mt-0.5 text-sm font-semibold text-granate">
            {{ proximoPaso.titulo }}
          </p>
          <p class="mt-0.5 text-xs leading-relaxed text-cafe/60">
            {{ proximoPaso.detalle }}
          </p>
        </div>
        <button
          type="button"
          class="inline-flex min-h-10 shrink-0 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral"
          @click="irAlProximoPaso"
        >
          {{ proximoPaso.accion }}
        </button>
      </div>

      <div
        ref="tabsRef"
        class="sticky top-14 z-20 mt-6 -mx-1 flex flex-nowrap items-center gap-1.5 overflow-x-auto border-b border-borde/50 px-1 pb-1.5 pt-1.5 sm:top-16 sm:gap-2"
        :class="claseFondoBarra"
        role="tablist"
        aria-label="Secciones del pedido"
        @keydown="navegarPestanas"
      >
        <button
          v-for="pestana in pestanas"
          :id="`pestana-${pestana.id}`"
          :key="pestana.id"
          type="button"
          role="tab"
          :aria-controls="`panel-${pestana.id}`"
          :aria-selected="pestanaActiva === pestana.id"
          :tabindex="pestanaActiva === pestana.id ? 0 : -1"
          class="shrink-0 whitespace-nowrap rounded-full px-3 py-2 text-[13px] font-semibold transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral sm:px-5 sm:py-2.5 sm:text-sm"
          :class="pestanaActiva === pestana.id
            ? 'bg-coral text-white shadow-sm'
            : 'text-cafe/60 hover:bg-rosa/25 hover:text-granate'"
          @click="cambiarPestana(pestana.id)"
        >
          {{ pestana.etiqueta }}
        </button>
      </div>

      <div ref="contenidoRef" class="mt-6 flex flex-col gap-4">
        <section
          v-show="pestanaActiva === 'resumen'"
          id="panel-resumen"
          role="tabpanel"
          aria-labelledby="pestana-resumen"
          tabindex="0"
          class="space-y-4 focus-visible:outline-none"
        >
          <section class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <dl
              class="grid gap-3 text-sm"
              :class="pedido.finalizadoEn
                ? 'sm:grid-cols-2 lg:grid-cols-4'
                : 'sm:grid-cols-3'"
            >
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Total</dt>
            <dd class="mt-0.5 text-xl font-bold text-granate">{{ total }}</dd>
            <dd
              v-if="esCliente && Number(descuentoMostrado) > 0"
              class="mt-1 text-xs font-semibold text-coral"
            >
              Descuento ({{ Number(pedido.descuentoPorcentaje) }}%):
              − {{ formatearPrecio(descuentoMostrado, moneda) }}
            </dd>
            <dd v-if="cobroDistinto" class="text-xs text-cafe/50">
              A pagar: {{ formatearPrecio(
                esCliente
                  ? (pedido.precioFinalClienteCobro ?? pedido.precioFinalCobro)
                  : pedido.precioFinalCobro,
                monedaCobro,
              ) }}
            </dd>
          </div>
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Inicio</dt>
            <dd class="mt-0.5 font-medium text-cafe">
              {{ pedido.fechaInicio ? formatearFecha(pedido.fechaInicio) : 'Al confirmar el pago' }}
            </dd>
          </div>
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Límite de entrega</dt>
            <dd class="mt-0.5 font-medium text-cafe">
              {{ pedido.limiteEntrega ? formatearFecha(pedido.limiteEntrega) : 'A confirmar' }}
            </dd>
          </div>
          <div v-if="pedido.finalizadoEn" class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Finalizado</dt>
            <dd class="mt-0.5 font-medium text-cafe">
              {{ formatearFechaHora(pedido.finalizadoEn) }}
            </dd>
          </div>
        </dl>

        <div
          v-if="(esArtista || pedido.rolVista === 'ADMIN') && pedido.comisionMonto != null"
          class="mt-4 rounded-xl border border-cafe/15 bg-cafe/5 p-4"
        >
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
            Comisión de la plataforma
          </p>
          <dl class="mt-2 space-y-1.5 text-sm">
            <div class="flex items-center justify-between gap-3">
              <dt class="text-cafe/60">
                Comisión ({{ pedido.comisionPorcentaje }}%)
              </dt>
              <dd class="text-cafe">
                − {{ formatearPrecio(pedido.comisionMontoMostrado ?? pedido.comisionMonto, moneda) }}
              </dd>
            </div>
            <div
              v-if="pedido.arancelMpEstimadoMostrado != null"
              class="flex items-center justify-between gap-3"
            >
              <dt class="text-cafe/60">
                Arancel MercadoPago estimado ({{ formatearMonto(pedido.arancelMpPorcentaje) }}%)
              </dt>
              <dd class="text-cafe">
                − {{ formatearPrecio(pedido.arancelMpEstimadoMostrado, moneda) }}
              </dd>
            </div>
            <div class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5">
              <dt class="font-semibold text-granate">Recibís</dt>
              <dd class="text-base font-bold text-granate">
                {{ formatearPrecio(
                  pedido.montoNetoArtistaConArancelMostrado
                    ?? pedido.montoNetoArtistaMostrado
                    ?? pedido.montoNetoArtista,
                  moneda,
                ) }}
              </dd>
            </div>
          </dl>
          <p
            v-if="pedido.arancelMpEstimadoMostrado != null"
            class="mt-2 text-xs leading-relaxed text-cafe/50"
          >
            El arancel de MercadoPago es una estimación y puede variar según el medio de pago
            y las cuotas.
          </p>
        </div>
          </section>

          <div class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-coral/25 bg-coral/5 p-4">
        <div class="min-w-0">
          <p class="text-sm font-semibold text-granate">Constancia del pedido</p>
          <p class="text-xs leading-relaxed text-cafe/60">
            Presupuesto, hitos, entregas, hashes y pagos en un PDF.
          </p>
        </div>
        <button
          type="button"
          :disabled="descargandoConstancia"
          class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-60"
          @click="descargarConstancia"
        >
          <IconoDescarga />
          {{ descargandoConstancia ? 'Generando...' : 'Descargar PDF' }}
        </button>
          </div>

          <div v-if="pedido.puedeCancelar" class="rounded-2xl border border-rojizo/25 bg-rojizo/5 p-5 sm:p-6">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-rojizo">Zona de riesgo</p>
            <p class="mt-1 text-sm leading-relaxed text-cafe/65">
              Cancelar detiene el trabajo y no devuelve automáticamente los pagos ya realizados.
            </p>
            <button
              type="button"
              :disabled="procesando"
              class="mt-3 inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:opacity-50"
              @click="cancelar"
            >
              Cancelar pedido
            </button>
          </div>
        </section>

        <template v-if="esArtista || esCliente">
          <section
            v-show="pestanaActiva === 'galeria'"
            id="panel-galeria"
            role="tabpanel"
            aria-labelledby="pestana-galeria"
            tabindex="0"
            class="space-y-4 focus-visible:outline-none"
          >
            <section class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
              <div class="flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-granate">
              {{ esArtista ? 'Galería del cliente' : 'Mi galería de pedidos' }}
            </h2>
            <p class="mt-1 max-w-2xl text-sm leading-relaxed text-cafe/65">
              <template v-if="esArtista">
                Si autorizás, la entrega final de este pedido puede aparecer en la galería pública del cliente con tu nombre y el servicio. No se muestran montos ni datos del pedido. Podés retirar la autorización cuando quieras.
              </template>
              <template v-else>
                Solo se muestra si el artista autoriza y el pedido está completo. Se ve la imagen, el servicio, el artista y la fecha; nunca montos ni datos del pedido.
              </template>
            </p>
          </div>
          <span
            v-if="esArtista"
            class="rounded-full border px-3 py-1 text-xs font-semibold"
            :class="pedido.publicacionVisible
              ? 'border-green-700/30 bg-green-700/10 text-green-700'
              : pedido.publicacionAutorizada
                ? 'border-coral/30 bg-coral/10 text-coral'
                : 'border-rosa bg-blanco text-cafe/60'"
          >
            {{ pedido.publicacionVisible
              ? 'Visible en su galería'
              : pedido.publicacionAutorizada
                ? 'Autorizado'
                : 'Sin autorizar' }}
          </span>
        </div>

        <div v-if="esArtista" class="mt-4">
          <div class="flex items-start gap-3 rounded-xl border border-rosa/40 bg-rosa/5 p-4">
            <button
              type="button"
              role="switch"
              :aria-checked="pedido.publicacionAutorizada"
              :disabled="procesando
                || (!pedido.puedeAutorizarPublicacion && !pedido.publicacionAutorizada)"
              class="relative mt-0.5 inline-flex h-6 w-11 shrink-0 items-center rounded-full transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema disabled:cursor-not-allowed disabled:opacity-50"
              :class="pedido.publicacionAutorizada ? 'bg-coral' : 'bg-rosa/70'"
              @click="autorizarPublicacion(!pedido.publicacionAutorizada)"
            >
              <span
                class="inline-block h-4 w-4 rounded-full bg-white shadow transition"
                :class="pedido.publicacionAutorizada ? 'translate-x-6' : 'translate-x-1'"
              ></span>
            </button>
            <div class="min-w-0">
              <p class="text-sm font-semibold text-granate">
                Permitir que esta entrega se muestre en la galería del cliente
              </p>
              <p
                v-if="!pedido.puedeAutorizarPublicacion && !pedido.publicacionAutorizada"
                class="mt-1 text-sm text-cafe/60"
              >
                Se habilita cuando subas la entrega final.
              </p>
              <p
                v-else-if="pedido.publicacionAutorizada && !pedido.galeriaClientePublica"
                class="mt-1 text-sm text-cafe/60"
              >
                El cliente tiene la galería privada. La pieza no se muestra públicamente hasta que la active.
              </p>
              <p
                v-else-if="pedido.publicacionVisible"
                class="mt-1 text-sm text-green-700"
              >
                La entrega final se está mostrando en la galería del cliente.
              </p>
              <p
                v-else-if="pedido.publicacionAutorizada && pedido.estado !== 'FINALIZADO'"
                class="mt-1 text-sm text-cafe/60"
              >
                Se mostrará cuando el pedido se complete.
              </p>
              <p
                v-if="pedido.publicacionAutorizada && pedido.publicacionAutorizadaEn"
                class="mt-1 text-xs text-cafe/50"
              >
                Autorizado el {{ formatearFechaHora(pedido.publicacionAutorizadaEn) }}.
              </p>
              <p
                v-if="pedido.publicacionOcultadaCliente"
                class="mt-1 text-sm text-cafe/60"
              >
                El cliente decidió ocultar este pedido de su galería.
              </p>
            </div>
          </div>
          <router-link
            v-if="pedido.cliente?.nombreUsuario"
            :to="{ name: 'perfil', params: { nombreUsuario: pedido.cliente.nombreUsuario } }"
            class="mt-3 inline-flex text-sm font-semibold text-coral transition hover:text-rojizo"
          >
            Ver la galería del cliente →
          </router-link>
        </div>

        <div v-else class="mt-4">
          <p
            v-if="pedido.estado !== 'FINALIZADO'"
            class="rounded-xl border border-rosa/40 bg-rosa/5 px-4 py-3 text-sm text-cafe/65"
          >
            Cuando el pedido se complete, vas a poder decidir si mostrarlo en tu galería.
          </p>
          <p
            v-else-if="!pedido.publicacionAutorizada"
            class="rounded-xl border border-rosa/40 bg-rosa/5 px-4 py-3 text-sm text-cafe/65"
          >
            El artista todavía no autorizó mostrar este pedido en tu galería.
          </p>
          <div v-else class="rounded-xl border border-rosa/40 bg-rosa/5 p-4">
            <div class="flex items-start gap-3">
              <button
                type="button"
                role="switch"
                :aria-checked="!pedido.publicacionOcultadaCliente"
                :disabled="procesando || !pedido.puedeOcultarPublicacion"
                class="relative mt-0.5 inline-flex h-6 w-11 shrink-0 items-center rounded-full transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema disabled:cursor-not-allowed disabled:opacity-50"
                :class="!pedido.publicacionOcultadaCliente ? 'bg-coral' : 'bg-rosa/70'"
                @click="ocultarPublicacion(!pedido.publicacionOcultadaCliente)"
              >
                <span
                  class="inline-block h-4 w-4 rounded-full bg-white shadow transition"
                  :class="!pedido.publicacionOcultadaCliente ? 'translate-x-6' : 'translate-x-1'"
                ></span>
              </button>
              <div class="min-w-0">
                <p class="text-sm font-semibold text-granate">
                  Mostrar este pedido en mi galería
                </p>
                <p
                  v-if="!pedido.galeriaClientePublica"
                  class="mt-1 text-sm text-cafe/60"
                >
                  Tu galería está privada.
                  <router-link
                    :to="{ name: 'configuracion-privacidad' }"
                    class="font-semibold text-coral transition hover:text-rojizo"
                  >
                    Activala en Privacidad
                  </router-link>
                  para que se vea.
                </p>
                <p
                  v-else-if="pedido.publicacionVisible"
                  class="mt-1 text-sm text-green-700"
                >
                  Visible en tu galería pública.
                </p>
                <p v-else class="mt-1 text-sm text-cafe/60">
                  Oculto en tu galería.
                </p>
              </div>
            </div>
            <router-link
              :to="{ name: 'perfil', params: { nombreUsuario: pedido.cliente?.nombreUsuario } }"
              class="mt-3 inline-flex text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Ver mi galería →
            </router-link>
          </div>
        </div>
            </section>
          </section>
        </template>

        <section
          v-show="pestanaActiva === 'plan-pagos'"
          id="panel-plan-pagos"
          role="tabpanel"
          aria-labelledby="pestana-plan-pagos"
          tabindex="0"
          class="space-y-4 focus-visible:outline-none"
        >
          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <div class="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h2 class="text-xl font-semibold text-granate">Plan de pagos</h2>
                <p class="mt-1 text-sm text-cafe/60">
                  {{ resumen.pagados }} de {{ resumen.total }} hitos pagados
                </p>
              </div>
              <p class="text-sm font-semibold text-granate">
                {{ formatearPrecio(resumen.montoPagado, moneda) }}
                <span class="font-normal text-cafe/55">
                  de {{ formatearPrecio(resumen.montoTotal, moneda) }}
                </span>
              </p>
            </div>
            <div
              class="mt-3 h-2 overflow-hidden rounded-full bg-rosa/30"
              role="progressbar"
              :aria-valuenow="resumen.porcentaje"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-label="`${resumen.pagados} de ${resumen.total} hitos pagados`"
            >
              <div
                class="h-full rounded-full bg-coral transition-all"
                :style="{ width: `${resumen.porcentaje}%` }"
              ></div>
            </div>
          </div>

          <ol class="space-y-3">
            <li
              v-for="(hito, indice) in hitos"
              :key="hito.idHitoPedido"
              class="flex gap-3"
            >
              <div class="flex w-8 shrink-0 flex-col items-center">
                <span
                  class="flex h-8 w-8 items-center justify-center rounded-full border-2 text-xs font-bold"
                  :class="claseNodoHito(hito)"
                >
                  {{ hito.estadoPago === 'PAGADO' ? '✓' : indice + 1 }}
                </span>
                <span
                  v-if="indice < hitos.length - 1"
                  class="mt-2 w-px flex-1 bg-rosa/60"
                ></span>
              </div>

              <article
                :id="`hito-card-${hito.idHitoPedido}`"
                class="min-w-0 flex-1 scroll-mt-28 rounded-2xl border border-rosa/50 bg-blanco"
              >
                <div class="flex flex-wrap items-start gap-3 p-4 sm:p-5">
                  <button
                    type="button"
                    class="min-w-0 flex-1 text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral"
                    :aria-expanded="hitoAbierto === hito.idHitoPedido"
                    :aria-controls="`hito-${hito.idHitoPedido}`"
                    @click="alternarHito(hito.idHitoPedido)"
                  >
                    <span class="block text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                      {{ etiquetaTipoHito(hito.tipo) }}
                    </span>
                    <span class="mt-1 block text-lg font-semibold text-granate">{{ hito.nombre }}</span>
                    <span class="mt-1.5 flex flex-wrap gap-1.5">
                      <span
                        class="rounded-full border px-2.5 py-1 text-[11px] font-semibold"
                        :class="estadoPagoDe(hito.estadoPago).clase"
                      >
                        Pago: {{ estadoPagoDe(hito.estadoPago).etiqueta }}
                      </span>
                      <span
                        class="rounded-full border px-2.5 py-1 text-[11px] font-semibold"
                        :class="estadoEntregaDe(hito.estadoEntrega).clase"
                      >
                        Entrega: {{ estadoEntregaDe(hito.estadoEntrega).etiqueta }}
                      </span>
                    </span>
                  </button>

                  <div class="ml-auto flex items-center gap-2">
                    <div class="flex flex-col items-end gap-2">
                      <p class="text-lg font-bold text-granate">{{ formatearMontoHito(hito) }}</p>
                      <button
                        v-if="hito.puedePagar"
                        type="button"
                        :disabled="procesando"
                        class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
                        @click="pagarHito(hito)"
                      >
                        {{ hito.tipo === 'ENTREGA' ? 'Pagar y desbloquear' : 'Pagar' }}
                      </button>
                    </div>
                    <button
                      type="button"
                      class="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-rosa bg-blanco text-cafe transition hover:bg-rosa/20 hover:text-granate focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral"
                      :aria-expanded="hitoAbierto === hito.idHitoPedido"
                      :aria-controls="`hito-${hito.idHitoPedido}`"
                      :aria-label="hitoAbierto === hito.idHitoPedido
                        ? `Contraer ${hito.nombre}`
                        : `Expandir ${hito.nombre}`"
                      :title="hitoAbierto === hito.idHitoPedido ? 'Contraer detalle' : 'Expandir detalle'"
                      @click="alternarHito(hito.idHitoPedido)"
                    >
                      <svg
                        class="h-4 w-4 transition-transform"
                        :class="hitoAbierto === hito.idHitoPedido ? 'rotate-180' : ''"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                        aria-hidden="true"
                      >
                        <path
                          fill-rule="evenodd"
                          d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z"
                          clip-rule="evenodd"
                        />
                      </svg>
                    </button>
                  </div>
                </div>

                <div
                  class="grid transition-all duration-200"
                  :class="hitoAbierto === hito.idHitoPedido ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'"
                >
                  <div
                    :id="`hito-${hito.idHitoPedido}`"
                    class="overflow-hidden"
                    :inert="hitoAbierto !== hito.idHitoPedido"
                  >
                    <div class="space-y-3 border-t border-rosa/40 px-4 py-4 sm:px-5">
                      <div>
                        <p v-if="hito.descripcion" class="text-sm leading-relaxed text-cafe/70">
                          {{ hito.descripcion }}
                        </p>
                        <p class="mt-1 text-xs text-cafe/55">{{ descripcionHito(hito) }}</p>
                      </div>

                      <dl class="grid gap-2 text-xs sm:grid-cols-3">
            <div v-if="hito.fechaLimitePago">
              <dt class="text-cafe/50">Pagar antes de</dt>
              <dd class="mt-0.5 text-cafe">{{ formatearFechaHora(hito.fechaLimitePago) }}</dd>
            </div>
            <div v-if="hito.fechaPago">
              <dt class="text-cafe/50">Pagado</dt>
              <dd class="mt-0.5 text-cafe">{{ formatearFechaHora(hito.fechaPago) }}</dd>
            </div>
            <div v-if="hito.fechaLimiteAprobacion">
              <dt class="text-cafe/50">Aprobación automática</dt>
              <dd class="mt-0.5 text-cafe">{{ formatearFechaHora(hito.fechaLimiteAprobacion) }}</dd>
            </div>
          </dl>

          <EntregasHito
            v-if="hito.entregas?.length"
            :id-pedido="idPedido"
            :id-hito="hito.idHitoPedido"
            :entregas="hito.entregas"
            :puede-agregar-al-portfolio="esArtista"
            :ids-en-portfolio="idsEnPortfolio"
            @agregar-al-portfolio="agregarAlPortfolio"
          />

          <div
            v-if="!hito.entregas?.length && (hito.entregaDescripcion || hito.entregaImagen)"
            class="mt-4 rounded-xl border border-rosa/40 bg-rosa/5 p-4"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
              Entregable
            </p>
            <p
              v-if="hito.entregaDescripcion"
              class="mt-2 whitespace-pre-line text-sm leading-relaxed text-cafe/75"
            >
              {{ hito.entregaDescripcion }}
            </p>
            <button
              v-if="hito.entregaImagen"
              type="button"
              class="mt-3 block overflow-hidden rounded-xl border border-rosa/50"
              @click="abrirVisor(hito.entregaImagen)"
            >
              <img
                :src="hito.entregaImagen"
                alt="Entregable del hito"
                class="max-h-72 w-full object-contain"
              />
            </button>
          </div>
          <p
            v-else-if="!hito.entregas?.length && hito.tipo === 'ENTREGA' && hito.estadoEntrega === 'ENTREGADO'"
            class="mt-4 rounded-xl border border-coral/25 bg-coral/5 px-4 py-3 text-sm text-cafe/70"
          >
            El artista subió la entrega final. Pagá esta cuota para desbloquear el archivo.
          </p>

          <div
            v-if="(esArtista || pedido.rolVista === 'ADMIN') && hito.comisionMonto != null"
            class="mt-3 rounded-lg bg-cafe/5 px-3 py-2 text-xs text-cafe/65"
          >
            <div class="flex flex-wrap items-center justify-between gap-2">
              <span>
                Comisión Temipora: − {{ formatearPrecio(hito.comisionMontoMostrado ?? hito.comisionMonto, moneda) }}
              </span>
              <span v-if="tieneDatosMp(hito)">
                Arancel MercadoPago: − {{ arancelMpHito(hito) }}
              </span>
              <span
                v-else-if="hito.arancelMpEstimadoMostrado != null"
                class="text-cafe/50"
              >
                Arancel MercadoPago est.: −
                {{ formatearPrecio(hito.arancelMpEstimadoMostrado, moneda) }}
              </span>
              <span class="font-semibold text-granate">
                Recibís{{ tieneDatosMp(hito) ? '' : ' (estimado)' }}: {{ netoHito(hito) }}
              </span>
            </div>
            <p v-if="tieneDatosMp(hito)" class="mt-1 text-[11px] text-cafe/50">
              {{ descripcionMedioPago(pagoAprobado(hito)) }}
              <span v-if="liberacionHito(hito)">
                · Liberación {{ liberacionHito(hito) }}
              </span>
            </p>
          </div>

          <div
            v-if="pagoPendiente(hito)"
            :id="`pago-${hito.idHitoPedido}`"
            class="mt-4 scroll-mt-28 rounded-xl border border-coral/30 bg-coral/5 px-4 py-3"
          >
            <p class="text-sm text-cafe/75">
              <template v-if="pagoPendiente(hito).proveedor === 'MERCADOPAGO'">
                Pago pendiente en MercadoPago. Completalo para activar el hito.
              </template>
              <template v-else>
                {{ pagoPendiente(hito).instruccion || 'Pago pendiente de confirmación.' }}
              </template>
            </p>
            <button
              v-if="esCliente && pagoPendiente(hito).proveedor === 'SIMULADO'"
              type="button"
              :disabled="procesando"
              class="mt-3 inline-flex min-h-9 items-center justify-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="confirmarPago(pagoPendiente(hito))"
            >
              Confirmar pago (simulado)
            </button>
            <a
              v-else-if="esCliente && pagoPendiente(hito).proveedor === 'MERCADOPAGO' && pagoPendiente(hito).instruccion"
              :href="pagoPendiente(hito).instruccion"
              class="mt-3 inline-flex min-h-9 items-center justify-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo"
            >
              Ir a pagar con MercadoPago
            </a>
          </div>

          <div
            v-if="hito.puedeAprobar"
            :id="`aprobar-${hito.idHitoPedido}`"
            class="flex scroll-mt-28 flex-wrap gap-2"
          >
            <button
              type="button"
              :disabled="procesando"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="aprobar(hito)"
            >
              Aprobar entregable
            </button>
            <button
              type="button"
              class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10"
              @click="abrirRevision(hito)"
            >
              Solicitar revisión
            </button>
          </div>

          <div
            v-if="revisionHitoId === hito.idHitoPedido"
            class="mt-3 rounded-xl border border-rojizo/25 bg-rojizo/5 p-3"
          >
            <label :for="`motivo-revision-${hito.idHitoPedido}`" class="block text-xs text-cafe/60">
              ¿Qué querés que revise el artista?
            </label>
            <textarea
              :id="`motivo-revision-${hito.idHitoPedido}`"
              v-model="motivoRevision"
              rows="3"
              maxlength="500"
              class="mt-2 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            ></textarea>
            <div class="mt-2 flex flex-wrap gap-2">
              <button
                type="button"
                :disabled="procesando"
                class="inline-flex min-h-9 items-center justify-center rounded-full bg-rojizo px-4 text-sm font-semibold text-white transition hover:bg-granate disabled:opacity-50"
                @click="solicitarRevision(hito)"
              >
                Enviar revisión
              </button>
              <button
                type="button"
                class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
                @click="cerrarRevision"
              >
                Cancelar
              </button>
            </div>
          </div>

                      <div
                        v-if="esArtista && hito.puedeEntregar"
                        :id="`entrega-${hito.idHitoPedido}`"
                        class="scroll-mt-28"
                      >
                        <SubirEntregable
                          :id-pedido="idPedido"
                          :hito="hito"
                          :dibujos="pedido.dibujos || []"
                          @entregado="alEntregar"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </li>
          </ol>
        </section>

        <section
          v-if="chatMontado"
          v-show="pestanaActiva === 'chat'"
          id="panel-chat"
          role="tabpanel"
          aria-labelledby="pestana-chat"
          tabindex="0"
          class="focus-visible:outline-none"
        >
          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="min-w-0">
                <h2 class="text-xl font-semibold text-granate">Chat del pedido</h2>
                <p class="mt-1 max-w-2xl text-sm leading-relaxed text-cafe/65">
                  Conversación privada con
                  @{{ chatContraparte?.nombreUsuario || 'la otra parte' }}.
                </p>
              </div>
              <p
                v-if="chatConversacion?.estado"
                class="rounded-full border px-3 py-1 text-xs font-semibold"
                :class="estadoConversacionDe(chatConversacion.estado).clase"
              >
                {{ estadoConversacionDe(chatConversacion.estado).texto }}
              </p>
            </div>

            <p v-if="chatCargando" class="mt-4 text-sm text-cafe/60">
              Cargando conversación...
            </p>
            <p v-else-if="chatError" role="alert" class="mt-4 text-sm text-rojizo">
              {{ chatError }}
            </p>
            <div v-else-if="chatAceptada" class="mt-4">
              <HiloConversacion
                v-model:texto="chatTexto"
                :mensajes="chatMensajes"
                :max-mensaje="chatMaxMensaje"
                :enviando="chatEnviando"
                :error-envio="chatErrorEnvio"
                :hay-mas="chatHayMas"
                :cargando-mas="chatCargandoMas"
                :contraparte-escribiendo="chatEscribiendo"
                :nombre-contraparte="chatContraparte?.nombreDisplay || chatContraparte?.nombreUsuario || 'Usuario'"
                :puede-enviar="Boolean(chatConversacion?.puedeEnviar)"
                :motivo-no-enviable="chatConversacion?.motivoNoEnviable || ''"
                :cargar-anteriores="chatCargarAnteriores"
                altura="45vh"
                @enviar="chatEnviar"
                @escribiendo="chatAvisarEscribiendo"
              />
            </div>
            <div
              v-else
              class="mt-4 rounded-xl border border-rosa/40 bg-rosa/5 px-4 py-3"
            >
              <p class="text-sm text-cafe/65">
                La conversación todavía no está activa.
              </p>
              <router-link
                v-if="enlaceConversacion"
                :to="enlaceConversacion"
                class="mt-2 inline-flex text-sm font-semibold text-coral transition hover:text-rojizo"
              >
                Abrirla en Mensajes →
              </router-link>
            </div>
          </div>
        </section>

        <template v-if="puedeVerResenias">
          <section
            v-show="pestanaActiva === 'resenas'"
            id="panel-resenas"
            role="tabpanel"
            aria-labelledby="pestana-resenas"
            tabindex="0"
            class="space-y-4 focus-visible:outline-none"
          >
            <div class="flex flex-wrap items-baseline justify-between gap-2">
          <h2 class="text-xl font-semibold text-granate">Reseñas</h2>
          <p v-if="reseniaEstado" class="text-xs text-cafe/50">
            {{ esArtista
              ? 'Información interna: la reseña del cliente solo la ven los artistas.'
              : 'Tu reseña se publica en el servicio.' }}
          </p>
        </div>

        <p v-if="cargandoResenias" class="text-sm text-cafe/60">Cargando reseñas...</p>
        <p v-else-if="errorResenia" role="alert" class="text-sm text-rojizo">
          {{ errorResenia }}
        </p>

        <template v-else-if="reseniaEstado">
          <p
            v-if="advertenciaCriterios"
            class="rounded-xl border border-durazno bg-durazno/20 px-3 py-2 text-xs text-cafe/70"
          >
            {{ advertenciaCriterios }} Podés ver las reseñas, pero el formulario
            puede quedar incompleto.
          </p>

          <TarjetaResenia
            v-if="esArtista && reseniaEstado.reseniaRecibida"
            :resenia="reseniaEstado.reseniaRecibida"
            titulo="Reseña del servicio (del cliente)"
          />

          <template v-if="reseniaEstado.miResenia && !editandoResenia">
            <TarjetaResenia
              :resenia="reseniaEstado.miResenia"
              :titulo="esArtista ? 'Tu reseña interna del cliente' : 'Tu reseña del servicio'"
              :puede-editar="reseniaEstado.puedeEditar"
              :puede-eliminar="reseniaEstado.puedeEliminar"
              :fecha-limite-edicion="reseniaEstado.fechaLimiteEdicion"
              @editar="editandoResenia = true"
              @eliminar="eliminarMiResenia"
            />
            <p v-if="reseniaEstado.motivoNoDisponible" class="text-xs text-cafe/50">
              {{ reseniaEstado.motivoNoDisponible }}
            </p>
            <p v-if="!esArtista" class="text-xs leading-relaxed text-cafe/50">
              {{ reseniaEstado.anonimoForzado || !reseniaEstado.miResenia.mostrarNombre
                ? 'Se publica en el servicio como “Cliente verificado”. No aparece debajo de las piezas de tu galería.'
                : 'Se publica en el servicio con tu nombre. También aparece debajo de las piezas de tu galería.' }}
            </p>
          </template>

          <template v-else-if="reseniaEstado.puedeCrear || editandoResenia">
            <FormularioResenia
              :criterios="criteriosResenia"
              :resenia="editandoResenia ? reseniaEstado.miResenia : null"
              :guardando="guardandoResenia"
              :anonimo-forzado="!esArtista && reseniaEstado.anonimoForzado"
              :permite-anonimato="!esArtista"
              :titulo="esArtista ? 'Tu reseña interna del cliente' : 'Reseñá este pedido'"
              :descripcion-ayuda="esArtista
                ? 'Solo la vas a ver vos. El resto de artistas ve la reputación agregada y el cliente nunca la ve.'
                : 'Se publica en el servicio. Podés elegir si mostrás tu nombre.'"
              :texto-guardar="editandoResenia ? 'Guardar la única edición' : 'Publicar reseña'"
              @guardar="guardarResenia"
              @cancelar="editandoResenia = false"
            />
          </template>

          <div v-else class="rounded-2xl border border-rosa/40 bg-blanco p-5">
            <p class="text-sm text-cafe/60">
              {{ reseniaEstado.motivoNoDisponible
                || 'No hay acciones disponibles para reseñar este pedido.' }}
            </p>
            <p v-if="reseniaEstado.fechaLimiteCreacion" class="mt-2 text-xs text-cafe/50">
              El plazo para dejar la reseña vencía el
              {{ formatearFechaHora(reseniaEstado.fechaLimiteCreacion) }}.
            </p>
          </div>
        </template>
          </section>
        </template>
      </div>
    </template>

    <div
      v-if="visor.abierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/80 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label="Entregable"
      @click.self="cerrarVisor"
    >
      <div class="relative max-h-full max-w-4xl overflow-auto rounded-2xl bg-blanco p-3">
        <button
          type="button"
          class="absolute right-3 top-3 rounded-full bg-blanco/90 px-3 py-1 text-sm font-semibold text-granate shadow"
          @click="cerrarVisor"
        >
          Cerrar
        </button>
        <img :src="visor.imagen" alt="Entregable ampliado" class="max-h-[80vh] w-auto" />
      </div>
    </div>

    <ModalConfirmacion
      :abierto="confirmacion.abierto"
      :titulo="confirmacion.titulo"
      :mensaje="confirmacion.mensaje"
      :texto-confirmar="confirmacion.textoConfirmar"
      @confirmar="confirmarAccion"
      @cerrar="cerrarConfirmacion"
    />
  </div>
</template>
