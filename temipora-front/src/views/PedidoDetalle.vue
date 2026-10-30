<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import EntregasHito from '../components/EntregasHito.vue'
import IconoDescarga from '../components/IconoDescarga.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import SubirEntregable from '../components/SubirEntregable.vue'
import FormularioResenia from '../components/FormularioResenia.vue'
import TarjetaResenia from '../components/TarjetaResenia.vue'
import { useSubirImagen } from '../composables/useSubirImagen'
import {
  cargarCriterios,
  cargarEstadoResenia,
  crearResenia,
  editarResenia,
  eliminarResenia,
} from '../servicios/resenias'
import {
  descripcionHito,
  estadoPedidoDe,
  etiquetaEntrega,
  etiquetaPago,
  etiquetaTipoHito,
  formatearFecha,
  formatearFechaHora,
  pagoPendiente,
} from '../servicios/pedidos'

const route = useRoute()
const router = useRouter()
const idPedido = computed(() => Number(route.params.idPedido))

const pedido = ref(null)
const cargando = ref(true)
const error = ref('')
const errorAccion = ref('')
const aviso = ref('')
const procesando = ref(false)
const avisoPago = ref('')
const sondeandoPago = ref(false)
const descargandoConstancia = ref(false)
const revisionHitoId = ref(null)
const motivoRevision = ref('')
const visor = reactive({ abierto: false, imagen: '' })
const { abrirVerificada } = useSubirImagen()
const idsEnPortfolio = ref([])
const confirmacion = reactive({
  abierto: false,
  titulo: '',
  mensaje: '',
  textoConfirmar: 'Confirmar',
  accion: null,
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
  pedido.value?.precioFinalMostrado ?? pedido.value?.precioFinal,
  moneda.value,
))
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

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/pedidos/${idPedido.value}`)
    pedido.value = res.data
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
    aviso.value = eraEdicion ? 'Reseña actualizada.' : 'Reseña publicada.'
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
      aviso.value = 'Eliminaste tu reseña.'
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
  errorAccion.value = ''
  aviso.value = ''
  try {
    const res = await accion()
    if (res?.data) pedido.value = res.data
    aviso.value = mensaje
    return true
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return false
    }
    errorAccion.value = e.response?.data?.message
      || 'No se pudo completar la acción.'
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
  errorAccion.value = ''
  aviso.value = ''
  try {
    await sincronizarPagoPendiente()
    const actualizado = (pedido.value?.hitos || [])
      .find((item) => item.idHitoPedido === hito.idHitoPedido)
    if (actualizado && actualizado.estadoPago !== 'PENDIENTE') {
      aviso.value = 'El pago ya fue acreditado.'
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
    aviso.value = 'Pago iniciado. Confirmalo para activar el hito.'
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorAccion.value = e.response?.data?.message
      || 'No se pudo completar la acción.'
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
    errorAccion.value = 'Escribí el motivo de la revisión.'
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
  if (!window.confirm(
    '¿Cancelar el pedido? Los pagos ya realizados no se devuelven automáticamente.',
  )) return
  ejecutar(
    () => api.post(`/pedidos/${idPedido.value}/cancelar`),
    'El pedido fue cancelado.',
  )
}

function alEntregar(data) {
  if (data) pedido.value = data
  errorAccion.value = ''
  aviso.value = 'Entregable subido. El cliente fue notificado.'
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
  errorAccion.value = ''
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
  } catch (e) {
    errorAccion.value = 'No se pudo descargar la constancia.'
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

      <section class="mt-6 rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
        <dl class="grid gap-3 text-sm sm:grid-cols-3">
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Total</dt>
            <dd class="mt-0.5 text-xl font-bold text-granate">{{ total }}</dd>
            <dd v-if="cobroDistinto" class="text-xs text-cafe/50">
              A pagar: {{ formatearPrecio(pedido.precioFinalCobro, monedaCobro) }}
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
            <div class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5">
              <dt class="font-semibold text-granate">Recibís</dt>
              <dd class="text-base font-bold text-granate">
                {{ formatearPrecio(pedido.montoNetoArtistaMostrado ?? pedido.montoNetoArtista, moneda) }}
              </dd>
            </div>
          </dl>
        </div>
      </section>

      <div class="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-coral/25 bg-coral/5 p-4">
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

      <p
        v-if="errorAccion"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        role="alert"
      >
        {{ errorAccion }}
      </p>
      <p
        v-else-if="aviso"
        class="mt-4 rounded-xl border border-green-700/25 bg-green-700/5 px-4 py-3 text-sm text-green-700"
        role="status"
      >
        {{ aviso }}
      </p>

      <section
        v-if="esArtista || esCliente"
        class="mt-4 rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
      >
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

      <section class="mt-6 space-y-4">
        <h2 class="text-xl font-semibold text-granate">Plan de pagos</h2>

        <article
          v-for="hito in hitos"
          :key="hito.idHitoPedido"
          class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                {{ etiquetaTipoHito(hito.tipo) }}
              </p>
              <h3 class="mt-1 text-lg font-semibold text-granate">{{ hito.nombre }}</h3>
              <p v-if="hito.descripcion" class="mt-1 text-sm leading-relaxed text-cafe/70">
                {{ hito.descripcion }}
              </p>
              <p class="mt-1 text-xs text-cafe/55">{{ descripcionHito(hito) }}</p>
            </div>
            <div class="text-right">
              <p class="text-lg font-bold text-granate">{{ formatearMontoHito(hito) }}</p>
              <div class="mt-1 flex flex-wrap justify-end gap-1.5">
                <span class="rounded-full bg-rosa/20 px-2.5 py-1 text-[11px] font-semibold text-cafe/70">
                  Pago: {{ etiquetaPago(hito.estadoPago) }}
                </span>
                <span class="rounded-full bg-coral/10 px-2.5 py-1 text-[11px] font-semibold text-coral">
                  Entrega: {{ etiquetaEntrega(hito.estadoEntrega) }}
                </span>
              </div>
            </div>
          </div>

          <dl class="mt-4 grid gap-2 text-xs sm:grid-cols-3">
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
            class="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-lg bg-cafe/5 px-3 py-2 text-xs text-cafe/65"
          >
            <span>Comisión: − {{ formatearPrecio(hito.comisionMontoMostrado ?? hito.comisionMonto, moneda) }}</span>
            <span class="font-semibold text-granate">
              Recibís: {{ formatearPrecio(hito.montoNetoMostrado ?? hito.montoNeto, moneda) }}
            </span>
          </div>

          <div
            v-if="pagoPendiente(hito)"
            class="mt-4 rounded-xl border border-coral/30 bg-coral/5 px-4 py-3"
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

          <div class="mt-4 flex flex-wrap gap-2">
            <button
              v-if="hito.puedePagar"
              type="button"
              :disabled="procesando"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="pagarHito(hito)"
            >
              {{
                hito.tipo === 'ENTREGA'
                  ? 'Pagar y desbloquear entrega'
                  : `Pagar ${hito.nombre}`
              }}
            </button>
          </div>

          <div v-if="hito.puedeAprobar" class="mt-4 flex flex-wrap gap-2">
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

          <SubirEntregable
            v-if="esArtista && hito.puedeEntregar"
            :id-pedido="idPedido"
            :hito="hito"
            :dibujos="pedido.dibujos || []"
            @entregado="alEntregar"
          />
        </article>
      </section>

      <section v-if="puedeVerResenias" class="mt-6 space-y-4">
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

      <div v-if="pedido.puedeCancelar" class="mt-6">
        <button
          type="button"
          :disabled="procesando"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:opacity-50"
          @click="cancelar"
        >
          Cancelar pedido
        </button>
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
