import {
  computed,
  isRef,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue'
import { useRouter } from 'vue-router'
import {
  aceptarConversacion,
  archivarConversacion,
  cancelarConversacion,
  cargarConfiguracionMensajeria,
  cargarMensajes,
  enviarMensaje,
  estadoConversacionDe,
  marcarConversacionLeida,
  marcarEscribiendo,
  obtenerConversacion,
  rechazarConversacion,
} from '../servicios/mensajes'
import { useMensajes } from './useMensajes'
import { useMensajeriaSocket } from './useMensajeriaSocket'

const INTERVALO_MS = 30000
const TAMANIO_HISTORIAL = 30
const INTERVALO_LATIDO_MS = 2000
const DURACION_ESCRITURA_MS = 6000

export function useHiloConversacion(idConversacion, { activo } = {}) {
  const router = useRouter()
  const { cargarContador } = useMensajes()
  const {
    conectado: socketConectado,
    enviarEscribiendo,
    suscribir: suscribirSocket,
  } = useMensajeriaSocket()

  const id = isRef(idConversacion)
    ? idConversacion
    : computed(() => idConversacion)
  const activoRef = activo || ref(true)

  const conversacion = ref(null)
  const mensajes = ref([])
  const cursor = ref(null)
  const hayMas = ref(false)
  const cargando = ref(false)
  const cargandoMas = ref(false)
  const error = ref('')
  const errorEnvio = ref('')
  const texto = ref('')
  const enviando = ref(false)
  const accionEnCurso = ref(false)
  const errorAccion = ref('')
  const config = ref({
    longitudMaximaMensaje: 2000,
    longitudMaximaMotivo: 200,
  })
  const contraparteEscribiendo = ref(false)

  let intervalo = null
  let ultimoLatido = 0
  let quitarSuscripcion = null
  let temporizadorEscritura = null
  let iniciado = false
  let idCargado = null

  const contraparte = computed(() => conversacion.value?.contraparte || null)
  const estadoInfo = computed(() =>
    estadoConversacionDe(conversacion.value?.estado))
  const maxMensaje = computed(
    () => Number(config.value?.longitudMaximaMensaje || 2000))
  const maxMotivo = computed(
    () => Number(config.value?.longitudMaximaMotivo || 200))
  const esPendiente = computed(
    () => conversacion.value?.estado === 'PENDIENTE')
  const esAceptada = computed(
    () => conversacion.value?.estado === 'ACEPTADA')
  const esRechazada = computed(
    () => conversacion.value?.estado === 'RECHAZADA')
  const esCancelada = computed(
    () => conversacion.value?.estado === 'CANCELADA')
  const esVencida = computed(
    () => conversacion.value?.estado === 'VENCIDA')
  const puedoResponderSolicitud = computed(() =>
    Boolean(conversacion.value?.puedeAceptar || conversacion.value?.puedeRechazar))
  const primerMensaje = computed(() => {
    if (!mensajes.value.length) return null
    const inicio = conversacion.value?.fechaSolicitud
    if (!inicio) return mensajes.value[0]
    const desde = new Date(inicio).getTime()
    return (
      mensajes.value.find(
        (mensaje) => new Date(mensaje.fechaEnvio).getTime() >= desde,
      ) || mensajes.value[0]
    )
  })
  const nombreContraparte = computed(
    () =>
      contraparte.value?.nombreDisplay ||
      contraparte.value?.nombreUsuario ||
      'Usuario')

  function cargarConfiguracion() {
    return cargarConfiguracionMensajeria()
      .then((res) => {
        if (res.data) config.value = res.data
      })
      .catch(() => {})
  }

  async function cargarDetalle({ silencioso = false } = {}) {
    try {
      const res = await obtenerConversacion(id.value)
      conversacion.value = res.data
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return
      }
      if (!silencioso) throw e
    }
  }

  async function cargarHistorial({ reset = true, mostrarCargando = true } = {}) {
    if (reset) {
      if (mostrarCargando) cargando.value = true
      mensajes.value = []
      cursor.value = null
      hayMas.value = false
    } else {
      cargandoMas.value = true
    }
    try {
      const params = { tamanio: TAMANIO_HISTORIAL }
      if (!reset && cursor.value) params.antesDe = cursor.value
      const res = await cargarMensajes(id.value, params)
      const nuevos = res.data?.mensajes || []
      if (reset) {
        mensajes.value = nuevos
      } else {
        mensajes.value = [...nuevos, ...mensajes.value]
      }
      cursor.value = res.data?.idCursorAnterior || null
      hayMas.value = Boolean(res.data?.hayMas)
      if (conversacion.value) {
        conversacion.value = {
          ...conversacion.value,
          contraparteUltimoLeidoEn:
            res.data?.contraparteUltimoLeidoEn ??
            conversacion.value.contraparteUltimoLeidoEn,
        }
      }
    } finally {
      cargando.value = false
      cargandoMas.value = false
    }
  }

  async function sincronizarNuevos() {
    try {
      const res = await cargarMensajes(id.value, {
        tamanio: TAMANIO_HISTORIAL,
      })
      const nuevos = res.data?.mensajes || []
      if (!nuevos.length) return

      if (nuevos.some((mensaje) => !mensaje.esPropio)) {
        contraparteEscribiendo.value = false
      }

      const mapa = new Map()
      for (const mensaje of mensajes.value) {
        mapa.set(mensaje.idMensaje, mensaje)
      }
      for (const mensaje of nuevos) {
        mapa.set(mensaje.idMensaje, mensaje)
      }
      mensajes.value = [...mapa.values()].sort(
        (a, b) => a.idMensaje - b.idMensaje,
      )
      if (!cursor.value && res.data?.idCursorAnterior) {
        cursor.value = res.data.idCursorAnterior
      }
      hayMas.value = hayMas.value || Boolean(res.data?.hayMas)
      if (conversacion.value) {
        conversacion.value = {
          ...conversacion.value,
          contraparteUltimoLeidoEn:
            res.data?.contraparteUltimoLeidoEn ??
            conversacion.value.contraparteUltimoLeidoEn,
        }
      }
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
      }
    }
  }

  async function marcarLeida() {
    if (!esAceptada.value || !activoRef.value) return
    if (document.visibilityState !== 'visible') return
    try {
      await marcarConversacionLeida(id.value)
      await cargarContador({ silencioso: true })
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
      }
    }
  }

  async function inicializar() {
    if (!id.value) return
    idCargado = id.value
    cargando.value = true
    error.value = ''
    try {
      await Promise.all([cargarDetalle(), cargarConfiguracion()])
      await cargarHistorial({ reset: true })
      await marcarLeida()
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return
      }
      error.value =
        e.response?.data?.message || 'No se pudo cargar la conversación.'
    } finally {
      cargando.value = false
    }
  }

  function reiniciar() {
    if (!activoRef.value || !id.value) {
      reset()
      if (iniciado) detener()
      return
    }
    if (!iniciado) {
      reset()
      iniciar()
      return
    }
    if (id.value === idCargado) return
    reset()
    inicializar()
  }

  function reset() {
    conversacion.value = null
    mensajes.value = []
    cursor.value = null
    hayMas.value = false
    cargando.value = false
    texto.value = ''
    error.value = ''
    errorEnvio.value = ''
    errorAccion.value = ''
    detenerTemporizadorEscritura()
    ultimoLatido = 0
  }

  async function cargarAnteriores() {
    if (cargandoMas.value || !hayMas.value) return
    await cargarHistorial({ reset: false })
  }

  async function enviar() {
    const contenido = texto.value.trim()
    if (!contenido || enviando.value || !conversacion.value?.puedeEnviar) {
      return false
    }
    if (contenido.length > maxMensaje.value) {
      errorEnvio.value =
        `El mensaje no puede superar los ${maxMensaje.value} caracteres.`
      return false
    }

    enviando.value = true
    errorEnvio.value = ''
    try {
      const res = await enviarMensaje(id.value, contenido)
      if (res.data && !mensajes.value.some(
        (mensaje) => mensaje.idMensaje === res.data.idMensaje)) {
        mensajes.value = [...mensajes.value, res.data]
      }
      texto.value = ''
      await cargarContador({ silencioso: true })
      return true
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return false
      }
      errorEnvio.value =
        e.response?.data?.message || 'No se pudo enviar el mensaje.'
      return false
    } finally {
      enviando.value = false
      ultimoLatido = 0
    }
  }

  function avisarEscribiendo() {
    if (!conversacion.value?.puedeEnviar) return
    if (!texto.value.trim()) return
    const ahora = Date.now()
    if (ahora - ultimoLatido < INTERVALO_LATIDO_MS) return
    ultimoLatido = ahora
    if (socketConectado.value && enviarEscribiendo(id.value)) {
      return
    }
    marcarEscribiendo(id.value).catch(() => {})
  }

  function detenerTemporizadorEscritura() {
    if (temporizadorEscritura) {
      window.clearTimeout(temporizadorEscritura)
      temporizadorEscritura = null
    }
    contraparteEscribiendo.value = false
  }

  function manejarEventoSocket(evento) {
    if (!evento || Number(evento.idConversacion) !== Number(id.value)) {
      return
    }

    if (evento.tipo === 'MENSAJE' && evento.mensaje) {
      const mensaje = evento.mensaje
      if (mensajes.value.some((m) => m.idMensaje === mensaje.idMensaje)) {
        return
      }
      mensajes.value = [...mensajes.value, mensaje]
        .sort((a, b) => a.idMensaje - b.idMensaje)
      if (!mensaje.esPropio) {
        detenerTemporizadorEscritura()
        void marcarLeida()
        void cargarContador({ silencioso: true })
      }
      return
    }

    if (evento.tipo === 'ESCRITURA') {
      if (!esAceptada.value) return
      contraparteEscribiendo.value = true
      if (temporizadorEscritura) {
        window.clearTimeout(temporizadorEscritura)
      }
      temporizadorEscritura = window.setTimeout(() => {
        contraparteEscribiendo.value = false
        temporizadorEscritura = null
      }, DURACION_ESCRITURA_MS)
      return
    }

    if (evento.tipo === 'LEIDO' && evento.fechaLectura) {
      const fecha = new Date(evento.fechaLectura)
      if (conversacion.value) {
        conversacion.value = {
          ...conversacion.value,
          contraparteUltimoLeidoEn: evento.fechaLectura,
        }
      }
      mensajes.value = mensajes.value.map((mensaje) => mensaje.esPropio
        ? { ...mensaje, leido: new Date(mensaje.fechaEnvio) <= fecha }
        : mensaje)
      return
    }

    if (evento.tipo === 'CONVERSACION' && activoRef.value) {
      void recargarPorEvento()
    }
  }

  async function recargarPorEvento() {
    await cargarDetalle({ silencioso: true })
    if (esAceptada.value) {
      await cargarHistorial({ reset: true, mostrarCargando: false })
      await marcarLeida()
    }
  }

  async function aceptar() {
    if (accionEnCurso.value) return false
    accionEnCurso.value = true
    errorAccion.value = ''
    try {
      const res = await aceptarConversacion(id.value)
      conversacion.value = res.data
      await cargarHistorial({ reset: true, mostrarCargando: false })
      await marcarLeida()
      return true
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return false
      }
      errorAccion.value =
        e.response?.data?.message || 'No se pudo aceptar la solicitud.'
      return false
    } finally {
      accionEnCurso.value = false
    }
  }

  async function rechazar(motivo) {
    if (accionEnCurso.value) return false
    accionEnCurso.value = true
    errorAccion.value = ''
    try {
      const res = await rechazarConversacion(id.value, motivo)
      conversacion.value = res.data
      return true
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return false
      }
      errorAccion.value =
        e.response?.data?.message || 'No se pudo rechazar la solicitud.'
      return false
    } finally {
      accionEnCurso.value = false
    }
  }

  async function cancelar() {
    if (accionEnCurso.value) return false
    accionEnCurso.value = true
    errorAccion.value = ''
    try {
      const res = await cancelarConversacion(id.value)
      conversacion.value = res.data
      return true
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return false
      }
      errorAccion.value =
        e.response?.data?.message || 'No se pudo cancelar la solicitud.'
      return false
    } finally {
      accionEnCurso.value = false
    }
  }

  async function alternarArchivado() {
    if (accionEnCurso.value) return null
    accionEnCurso.value = true
    errorAccion.value = ''
    const archivar = !conversacion.value?.archivada
    try {
      const res = await archivarConversacion(id.value, archivar)
      conversacion.value = res.data
      return archivar
    } catch (e) {
      if (e.response?.status === 401) {
        router.push({ name: 'login' })
        return null
      }
      errorAccion.value =
        e.response?.data?.message || 'No se pudo archivar la conversación.'
      return null
    } finally {
      accionEnCurso.value = false
    }
  }

  function iniciarPolling() {
    detenerPolling()
    intervalo = window.setInterval(async () => {
      if (!activoRef.value) return
      if (document.visibilityState !== 'visible' || !conversacion.value) return
      await cargarDetalle({ silencioso: true })
      if (esAceptada.value) {
        await sincronizarNuevos()
        if ((conversacion.value?.noLeidos || 0) > 0) {
          await marcarLeida()
        }
      }
    }, INTERVALO_MS)
  }

  function detenerPolling() {
    if (intervalo) {
      window.clearInterval(intervalo)
      intervalo = null
    }
  }

  function iniciar() {
    if (!id.value || iniciado) return
    iniciado = true
    quitarSuscripcion = suscribirSocket(manejarEventoSocket)
    inicializar()
    iniciarPolling()
  }

  function detener() {
    if (!iniciado) return
    iniciado = false
    quitarSuscripcion?.()
    quitarSuscripcion = null
    detenerPolling()
    detenerTemporizadorEscritura()
  }

  watch(activoRef, (valor) => {
    if (valor) iniciar()
    else detener()
  })

  watch(id, (nuevo, anterior) => {
    if (nuevo !== anterior) reiniciar()
  })

  onMounted(() => {
    if (activoRef.value) iniciar()
  })

  onBeforeUnmount(() => {
    detener()
  })

  return {
    conversacion,
    mensajes,
    hayMas,
    cargando,
    cargandoMas,
    error,
    errorEnvio,
    texto,
    enviando,
    accionEnCurso,
    errorAccion,
    contraparteEscribiendo,
    contraparte,
    estadoInfo,
    maxMensaje,
    maxMotivo,
    esPendiente,
    esAceptada,
    esRechazada,
    esCancelada,
    esVencida,
    puedoResponderSolicitud,
    primerMensaje,
    nombreContraparte,
    inicializar,
    reiniciar,
    cargarConfiguracion,
    cargarAnteriores,
    enviar,
    avisarEscribiendo,
    aceptar,
    rechazar,
    cancelar,
    alternarArchivado,
  }
}
