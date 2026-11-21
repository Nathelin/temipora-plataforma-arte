<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import VistaPreviaTerminos from '../components/VistaPreviaTerminos.vue'
import ResumenResenias from '../components/ResumenResenias.vue'
import TarjetaReseniaPublica from '../components/TarjetaReseniaPublica.vue'
import ModalResenias from '../components/ModalResenias.vue'
import VisorImagenes from '../components/VisorImagenes.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import ImagenOptimizada from '../components/ImagenOptimizada.vue'
import TarjetaArtistaRecomendado from '../components/TarjetaArtistaRecomendado.vue'
import {
  cargarReseniasServicio,
  cargarResumenReseniasArtista,
  formatearPromedio,
} from '../servicios/resenias'
import { esImagenVerificada } from '../servicios/imagenes'
import { buscarAlternativas, datosMotivo } from '../servicios/sustituciones'
import { useNotificaciones } from '../composables/useNotificaciones'
import { useAccesoEnlace } from '../composables/useAccesoEnlace'

const route = useRoute()
const router = useRouter()
const { exito, error: notificarError } = useNotificaciones()
const { consumirSiCorresponde } = useAccesoEnlace()

const servicio = ref(null)
const terminos = ref(null)
const disponibilidad = ref(null)
const terminosValidos = ref(false)
const cargando = ref(true)
const error = ref('')
const noDisponible = ref(false)
const autenticado = ref(!!localStorage.getItem('token'))
const rol = ref(localStorage.getItem('rol') || '')
const esFavorito = ref(false)
const cargandoEstadoFavorito = ref(false)
const actualizandoFavorito = ref(false)

const artista = ref(null)
const resumenArtista = ref(null)
const artistasSimilares = ref([])
const sustitucion = ref(null)
const modalCola = ref(false)

const resumenResenias = ref(null)
const reseniasPreview = ref([])
const cargandoPreview = ref(true)
const errorPreview = ref('')
const reseniasLista = ref([])
const paginaResenias = ref(null)
const cargandoListaResenias = ref(false)
const errorListaResenias = ref('')
const modalReseniasAbierto = ref(false)
const filtroOpcion = ref('')
const filtrosResenias = reactive({ calificacion: '', orden: 'RECIENTES', idOpcion: null })

const seccionResenias = ref(null)
const seccionTerminos = ref(null)

const CAMPOS_TERMINOS = [
  'informacionGeneral',
  'politicaRevisiones',
  'politicaCancelaciones',
  'usoArtista',
  'usoCliente',
  'queDibuja',
  'queNoDibuja',
]

const terminosParaMostrar = computed(() => {
  const t = terminos.value
  if (!t) return null
  const form = {}
  for (const key of CAMPOS_TERMINOS) form[key] = t[key] || ''
  form.pagoExtra = t.pagoExtra || ''
  const estadoDerechos = {}
  for (const d of t.derechos || []) {
    estadoDerechos[d.idDerechoTermino] = {
      disponible: !!d.disponible,
      porcentajeExtra: d.porcentajeExtra,
    }
  }
  return { form, estadoDerechos }
})

const derechosTerminos = computed(() => terminos.value?.derechos || [])
const hayTerminos = computed(() => {
  const t = terminos.value
  if (!t) return false
  const hayTexto = [...CAMPOS_TERMINOS, 'pagoExtra']
    .some((k) => (t[k] || '').trim())
  return hayTexto || (t.derechos || []).some((d) => d.disponible)
})

const mostrarSensible = ref(false)
const indiceImagen = ref(0)
const visorAbierto = ref(false)
const indiceVisor = ref(0)

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const esSensible = computed(() => !!servicio.value?.contenidoSensible)
const requiereAdvertencia = computed(() => !!servicio.value?.requiereAdvertencia)
const tiposSensibles = computed(() => {
  const tipos = servicio.value?.contenidosSensibles || []
  return tipos.map((t) => ETIQUETAS_SENSIBLE[t] || t)
})
const textoSensible = computed(() => tiposSensibles.value.join(' · ') || 'Contenido sensible')

const esDuenio = computed(() => {
  const actual = localStorage.getItem('nombreUsuario') || ''
  return actual.toLowerCase() === (servicio.value?.nombreArtista || '').toLowerCase()
})
const puedeContratar = computed(() => rol.value === 'CLIENTE' || rol.value === 'ARTISTA')
const puedeMarcarFavorito = computed(
  () =>
    autenticado.value &&
    puedeContratar.value &&
    !esDuenio.value &&
    servicio.value?.estado === 'PUBLICADO',
)
const puedeSolicitar = computed(
  () =>
    autenticado.value &&
    puedeContratar.value &&
    !esDuenio.value &&
    terminosValidos.value &&
    !!disponibilidad.value?.puedeSolicitar,
)
const mensajeSolicitud = computed(() => {
  if (esDuenio.value) return ''
  if (!autenticado.value) return 'Iniciá sesión para solicitar este servicio.'
  if (!puedeContratar.value) return 'Solo las cuentas de cliente o artista pueden solicitar servicios.'
  if (!terminosValidos.value) return 'El artista todavía no configuró los términos y condiciones de este servicio.'
  return disponibilidad.value?.mensaje || ''
})

const precioMostrado = computed(() =>
  formatearPrecio(
    servicio.value?.precioBaseMostrado ?? servicio.value?.precioBase,
    servicio.value?.monedaMostrada ?? servicio.value?.moneda,
  ),
)
const precioConvertido = computed(
  () =>
    !!servicio.value?.monedaMostrada &&
    servicio.value.monedaMostrada !== servicio.value.moneda,
)
const descuentoPorcentaje = computed(() => {
  const valor = Number(servicio.value?.descuentoOfertaPorcentaje)
  return Number.isFinite(valor) && valor > 0 ? valor : 0
})
const precioConDescuentoMostrado = computed(() =>
  formatearPrecio(
    servicio.value?.precioConDescuentoEstimado,
    servicio.value?.monedaMostrada ?? servicio.value?.moneda,
  ),
)
const notaDescuento = 'El descuento se aplica sobre el presupuesto final que envíe el artista. Si agrega recargos, también se calculan sobre el total. El precio mostrado es un estimado sobre el precio base.'

const cuposDisponibles = computed(() => {
  if (disponibilidad.value?.cuposDisponibles != null) {
    return disponibilidad.value.cuposDisponibles
  }
  if (servicio.value?.cuposDisponibles != null) return servicio.value.cuposDisponibles
  return servicio.value?.cupos ?? 0
})
const cuposTotales = computed(
  () => disponibilidad.value?.cuposTotales ?? servicio.value?.cupos ?? 0,
)

const noDisponibleAhora = computed(() => {
  const actual = servicio.value
  if (actual && typeof actual.disponibleAhora === 'boolean') {
    return !actual.disponibleAhora
  }
  const d = disponibilidad.value
  if (!d) return false
  return d.modoRecepcion !== 'ABIERTA' || (d.cuposDisponibles ?? 0) <= 0
})

const motivoEstado = computed(() => {
  if (servicio.value?.estado === 'PAUSADO') return 'PAUSADO'
  if (servicio.value?.estado === 'ARCHIVADO') return 'ARCHIVADO'
  if (disponibilidad.value?.modoRecepcion === 'CERRADA') return 'CERRADA'
  if (disponibilidad.value?.modoRecepcion === 'SOLO_ESPERA') return 'SOLO_ESPERA'
  return 'SIN_CUPOS'
})

const detalleEstado = computed(() =>
  datosMotivo(sustitucion.value?.motivo || motivoEstado.value),
)
const puedeEsperar = computed(
  () =>
    sustitucion.value?.puedeQuedarEnCola ??
    disponibilidad.value?.puedeListaEspera ??
    false,
)
const maxDibujos = computed(
  () =>
    disponibilidad.value?.maxDibujosPorSolicitud ??
    servicio.value?.maxDibujosPorSolicitud ??
    null,
)
const modoRecepcionTexto = computed(() => {
  const modo = disponibilidad.value?.modoRecepcion
  if (modo === 'SOLO_ESPERA') return 'Solo lista de espera'
  if (modo === 'CERRADA') return 'Recepción cerrada'
  if (!modo) return ''
  if (cuposDisponibles.value <= 0) return 'Sin cupos · en cola'
  return 'Recepción abierta'
})

const clasesEstadoRecepcion = computed(() => {
  const modo = disponibilidad.value?.modoRecepcion
  if (modo === 'CERRADA') return 'bg-rojizo/10 text-rojizo'
  if (modo === 'ABIERTA' && cuposDisponibles.value <= 0) {
    return 'bg-mantequilla/80 text-granate'
  }
  return 'bg-rosa/30 text-cafe'
})

const esSoloEspera = computed(
  () => disponibilidad.value?.modoRecepcion === 'SOLO_ESPERA',
)

const etiquetaCola = computed(() =>
  esSoloEspera.value ? 'Lista de espera' : 'Lista de espera',
)

const tituloCola = computed(() =>
  esSoloEspera.value
    ? 'Unirte a la lista de espera'
    : 'Unirte a la lista de espera',
)

const mensajeCola = computed(() => {
  const artista = servicio.value?.nombreArtista
    ? `@${servicio.value.nombreArtista}`
    : 'El artista'
  if (esSoloEspera.value) {
    return `${artista} recibe solicitudes solo en lista de espera. Tu solicitud quedará anotada y no se promueve automáticamente. ¿Querés continuar?`
  }
  return `${artista} no tiene cupos libres ahora. Tu solicitud quedará pendiente podrá presupuestarla cuando se libere un cupo. No reserva un cupo todavía. ¿Querés continuar?`
})

const galeria = computed(() => {
  const s = servicio.value
  if (!s) return []
  const portada = s.imagenPortada ? [{ url: s.imagenPortada, portada: true }] : []
  const ejemplos = (s.imagenes || [])
    .filter((url) => url && url !== s.imagenPortada)
    .map((url) => ({ url, portada: false }))
  return [...portada, ...ejemplos]
})

const imagenActiva = computed(() => galeria.value[indiceImagen.value] || null)
const activaEsPortada = computed(() => !!imagenActiva.value?.portada)
const activaEsVerificada = computed(() => esImagenVerificada(imagenActiva.value?.url))
const mostrarBlur = computed(
  () => requiereAdvertencia.value && activaEsPortada.value && !mostrarSensible.value,
)
const imagenesVisor = computed(() => galeria.value.map((img) => img.url))

const etiquetasVisibles = computed(() => servicio.value?.etiquetas || [])
const hayChips = computed(() =>
  Boolean(
    servicio.value?.nombreTipoServicio ||
    servicio.value?.nombreMedio ||
    etiquetasVisibles.value.length,
  ),
)

const nombreArtistaMostrado = computed(
  () => artista.value?.nombreDisplay || servicio.value?.nombreArtista || '',
)
const fotoArtista = computed(
  () => artista.value?.urlFotoPerfilMiniatura || artista.value?.urlFotoPerfil || '',
)
const inicialArtista = computed(() =>
  (nombreArtistaMostrado.value || '?').charAt(0).toUpperCase(),
)
const hayReseniasArtista = computed(
  () =>
    Boolean(resumenArtista.value?.hayDatos) &&
    Number(resumenArtista.value?.cantidad || 0) > 0,
)
const promedioArtista = computed(() => resumenArtista.value?.promedio ?? null)
const cantidadReseniasArtista = computed(() =>
  Number(resumenArtista.value?.cantidad || 0),
)

const hayReseniasServicio = computed(
  () =>
    Boolean(resumenResenias.value?.hayDatos) &&
    Number(resumenResenias.value?.cantidad || 0) > 0,
)
const promedioServicio = computed(() => resumenResenias.value?.promedio ?? null)
const cantidadReseniasServicio = computed(() =>
  Number(resumenResenias.value?.cantidad || 0),
)

function textoEntrega(s) {
  if (!s || s.tiempoEntregaValor == null) return ''
  return `${s.tiempoEntregaValor} ${(s.tiempoEntregaUnidad || '').toLowerCase()}`.trim()
}

function moverImagen(paso) {
  const total = galeria.value.length
  if (total < 2) return
  indiceImagen.value = (indiceImagen.value + paso + total) % total
  if (visorAbierto.value) indiceVisor.value = indiceImagen.value
}

function seleccionarImagen(i) {
  indiceImagen.value = i
  if (visorAbierto.value) indiceVisor.value = i
}

function abrirVisor(indice = indiceImagen.value) {
  if (!galeria.value.length || mostrarBlur.value) return
  indiceVisor.value = Math.min(Math.max(indice, 0), galeria.value.length - 1)
  visorAbierto.value = true
}

function actualizarIndiceVisor(indice) {
  indiceVisor.value = indice
  indiceImagen.value = indice
}

function irAlArtista() {
  router.push({ name: 'perfil', params: { nombreUsuario: servicio.value.nombreArtista } })
}

function irAServicios() {
  router.push({ name: 'servicios' })
}

function irASolicitar() {
  if (!autenticado.value) {
    router.push({ name: 'login' })
    return
  }
  const idOferta = servicio.value?.idOferta ?? route.query.oferta
  const query = {}
  if (idOferta) query.oferta = idOferta
  if (route.query.sustitucion) query.sustitucion = route.query.sustitucion
  router.push({
    name: 'solicitud-servicio',
    params: { id: servicio.value.idServicio },
    query,
  })
}

function irAAlternativas() {
  router.push({
    name: 'servicio-alternativas',
    params: { id: servicio.value.idServicio },
    query: { disparador: 'INTENTO_SOLICITUD' },
  })
}

function irASolicitarEnEspera() {
  if (!autenticado.value) {
    router.push({ name: 'login' })
    return
  }
  const query = {}
  const idOferta = servicio.value?.idOferta ?? route.query.oferta
  if (idOferta) query.oferta = idOferta
  const idSustitucion = sustitucion.value?.idSustitucion
    ?? route.query.sustitucion
  if (idSustitucion) query.sustitucion = idSustitucion
  router.push({
    name: 'solicitud-servicio',
    params: { id: servicio.value.idServicio },
    query,
  })
}

function abrirModalCola() {
  if (!autenticado.value) {
    router.push({ name: 'login' })
    return
  }
  modalCola.value = true
}

function confirmarCola() {
  modalCola.value = false
  irASolicitarEnEspera()
}

async function cargarAlternativas() {
  try {
    sustitucion.value = await buscarAlternativas(
      servicio.value.idServicio,
      'DETALLE',
    )
  } catch {
    sustitucion.value = null
  }
}

function irAResenias() {
  seccionResenias.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function irATerminos() {
  seccionTerminos.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function aplicarSolicitudPublica(data) {
  servicio.value = data.servicio
  terminos.value = data.terminos
  disponibilidad.value = data.disponibilidad
  terminosValidos.value = !!data.terminosValidos
}

async function cargarSimilares() {
  try {
    const res = await api.get(`/servicios/${route.params.id}/similares`)
    artistasSimilares.value = Array.isArray(res.data) ? res.data : []
  } catch {
    artistasSimilares.value = []
  }
}

async function cargarEstadoFavorito() {
  esFavorito.value = false
  if (!puedeMarcarFavorito.value) return

  cargandoEstadoFavorito.value = true
  try {
    const res = await api.get(`/favoritos/servicios/${servicio.value.idServicio}`)
    esFavorito.value = !!res.data?.esFavorito
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
    }
  } finally {
    cargandoEstadoFavorito.value = false
  }
}

async function alternarFavoritoServicio() {
  if (!autenticado.value) {
    router.push({ name: 'login' })
    return
  }
  if (
    !puedeMarcarFavorito.value ||
    actualizandoFavorito.value ||
    cargandoEstadoFavorito.value
  ) {
    return
  }

  actualizandoFavorito.value = true
  try {
    if (esFavorito.value) {
      await api.delete(`/favoritos/servicios/${servicio.value.idServicio}`)
      esFavorito.value = false
      exito(`Quitaste "${servicio.value.nombre}" de tus favoritos.`)
    } else {
      await api.post(`/favoritos/servicios/${servicio.value.idServicio}`)
      esFavorito.value = true
      exito(`Agregaste "${servicio.value.nombre}" a tus favoritos.`)
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    notificarError(e.response?.data?.message || 'No se pudieron actualizar tus favoritos.')
  } finally {
    actualizandoFavorito.value = false
  }
}

async function cargarArtista() {
  const nombre = servicio.value?.nombreArtista
  if (!nombre) return
  const [perfilRes, resumenRes] = await Promise.allSettled([
    api.get(`/perfil/${encodeURIComponent(nombre)}`),
    cargarResumenReseniasArtista(nombre),
  ])
  artista.value = perfilRes.status === 'fulfilled' ? perfilRes.value.data : null
  resumenArtista.value = resumenRes.status === 'fulfilled' ? resumenRes.value.data : null
}

async function cargar() {
  cargando.value = true
  error.value = ''
  noDisponible.value = false
  try {
    const res = await api.get(`/servicios/${route.params.id}/solicitud-publica`)
    aplicarSolicitudPublica(res.data)
  } catch (e) {
    try {
      const servicioRes = await api.get(`/servicios/${route.params.id}`)
      servicio.value = servicioRes.data
      const [terminosRes, disponibilidadRes] = await Promise.all([
        api.get(`/servicios/${route.params.id}/terminos-publicos`).catch(() => null),
        api.get(`/servicios/${route.params.id}/disponibilidad`).catch(() => null),
      ])
      terminos.value = terminosRes?.data || null
      disponibilidad.value = disponibilidadRes?.data || null
      terminosValidos.value =
        !!terminos.value?.idTerminos &&
        Number(terminos.value?.version || 0) > 0
    } catch (e2) {
      if (e2.response?.status === 410) {
        noDisponible.value = true
        error.value = ''
      } else {
        error.value = e2.response?.data?.message || 'No se pudo cargar el servicio.'
      }
    }
  } finally {
    cargando.value = false
  }
  if (!error.value && noDisponibleAhora.value) {
    void cargarAlternativas()
  }
}

async function cargarReseniasPreview() {
  cargandoPreview.value = true
  errorPreview.value = ''
  try {
    const { data } = await cargarReseniasServicio(route.params.id, {
      pagina: 0,
      tamanio: 2,
      orden: 'RECIENTES',
    })
    reseniasPreview.value = data.pagina?.contenido || []
    resumenResenias.value = data.resumen || null
  } catch (e) {
    errorPreview.value = e.response?.data?.message
      || 'No se pudieron cargar las reseñas.'
  } finally {
    cargandoPreview.value = false
  }
}

function parametrosResenias(pagina) {
  const params = {
    pagina,
    tamanio: 10,
    orden: filtrosResenias.orden || 'RECIENTES',
  }
  if (filtrosResenias.calificacion) {
    params.calificacion = Number(filtrosResenias.calificacion)
  }
  if (filtrosResenias.idOpcion) {
    params.idOpcion = Number(filtrosResenias.idOpcion)
  }
  return params
}

async function cargarListaResenias(pagina = 0) {
  cargandoListaResenias.value = true
  errorListaResenias.value = ''
  try {
    const { data } = await cargarReseniasServicio(
      route.params.id,
      parametrosResenias(pagina),
    )
    reseniasLista.value = data.pagina?.contenido || []
    paginaResenias.value = data.pagina || null
    if (data.resumen) resumenResenias.value = data.resumen
  } catch (e) {
    errorListaResenias.value = e.response?.data?.message
      || 'No se pudieron cargar las reseñas.'
  } finally {
    cargandoListaResenias.value = false
  }
}

function abrirResenias(opcion = null) {
  filtrosResenias.calificacion = ''
  filtrosResenias.orden = 'RECIENTES'
  filtrosResenias.idOpcion = opcion?.idOpcion ?? null
  filtroOpcion.value = opcion?.etiqueta || ''
  modalReseniasAbierto.value = true
  void cargarListaResenias(0)
}

function cerrarResenias() {
  modalReseniasAbierto.value = false
}

function quitarFiltroOpcion() {
  filtrosResenias.idOpcion = null
  filtroOpcion.value = ''
  void cargarListaResenias(0)
}

function cambiarFiltrosResenias(filtros) {
  filtrosResenias.calificacion = filtros.calificacion ?? ''
  filtrosResenias.orden = filtros.orden || 'RECIENTES'
  filtrosResenias.idOpcion = filtros.idOpcion ?? null
  void cargarListaResenias(0)
}

function cambiarPaginaResenias(pagina) {
  void cargarListaResenias(pagina)
}

function manejarTecla(event) {
  if (!servicio.value || event.key !== 'Escape') return
  if (mostrarSensible.value) mostrarSensible.value = false
}

onMounted(async () => {
  const acceso = await consumirSiCorresponde()
  if (acceso.habiaEnlace) {
    return
  }
  await cargar()
  if (servicio.value) {
    void cargarArtista()
    void cargarSimilares()
    await cargarReseniasPreview()
  }
  void cargarEstadoFavorito()
  window.addEventListener('keydown', manejarTecla)
})

onBeforeUnmount(() => window.removeEventListener('keydown', manejarTecla))
</script>

<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-8 sm:px-8 sm:py-10">
    <CargandoPagina v-if="cargando" tipo="detalle" mensaje="Cargando servicio…" />
    <div
      v-else-if="noDisponible"
      class="mx-auto max-w-xl rounded-2xl border border-rosa/60 bg-blanco px-6 py-12 text-center"
    >
      <p class="font-display text-lg font-bold text-granate">Este servicio no está disponible.</p>
      <p class="mt-2 text-sm leading-relaxed text-cafe/70">
        El artista dio de baja su cuenta o el servicio ya no está publicado.
      </p>
      <button
        type="button"
        class="mt-4 inline-block text-sm font-medium text-coral transition hover:text-rojizo"
        @click="irAServicios"
      >
        ← Ver otros servicios
      </button>
    </div>
    <p v-else-if="error" class="py-16 text-center text-rojizo">{{ error }}</p>

    <template v-else-if="servicio">
      <nav
        aria-label="Migas de pan"
        class="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-cafe/60"
      >
        <button type="button" class="transition hover:text-coral" @click="irAServicios">
          Explorar
        </button>
        <span aria-hidden="true">›</span>
        <button type="button" class="transition hover:text-coral" @click="irAlArtista">
          Servicios de {{ nombreArtistaMostrado || `@${servicio.nombreArtista}` }}
        </button>
        <span aria-hidden="true">›</span>
        <span class="font-semibold text-granate">{{ servicio.nombre }}</span>
      </nav>

      <div
        v-if="Number(servicio.descuentoOfertaPorcentaje) > 0"
        class="mt-4 flex flex-wrap items-center gap-2 rounded-2xl border border-coral/30 bg-coral/5 px-4 py-3 text-sm text-granate"
      >
        <span class="rounded-full bg-coral px-2.5 py-1 text-xs font-extrabold text-white">
          -{{ Number(servicio.descuentoOfertaPorcentaje) }}%
        </span>
        <span>
          Tenés un descuento de reactivación para este servicio. Se aplica sobre
          el presupuesto final que envíe el artista.
        </span>
      </div>

      <div class="mt-5 grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(22rem,30rem)] lg:items-start">
        <section aria-label="Galería del servicio" class="order-1 min-w-0 space-y-4 lg:col-start-1 lg:row-start-1">
          <div class="relative overflow-hidden rounded-[32px] bg-rosa/20 shadow-[0_14px_34px_rgba(194,24,91,0.14)]">
            <button
              type="button"
              class="block w-full text-left"
              aria-label="Ampliar imagen del servicio"
              @click="abrirVisor()"
            >
              <ImagenOptimizada
                v-if="imagenActiva"
                :src="imagenActiva.url"
                :alt="servicio.nombre"
                sizes="(min-width: 1024px) 60vw, 100vw"
                prioridad
                class="aspect-[3/2] w-full object-cover transition duration-200"
                :class="mostrarBlur ? 'scale-105 blur-[18px]' : ''"
              />
              <span
                v-else
                class="flex aspect-[3/2] w-full items-center justify-center bg-superficie-alt"
              >
                <span class="font-display text-6xl font-extrabold text-cafe/40">
                  {{ (servicio.nombre || '?').charAt(0).toUpperCase() }}
                </span>
              </span>
            </button>

            <div
              v-if="mostrarBlur"
              class="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-granate/30 px-6 text-center"
              @click.stop
            >
              <svg class="h-10 w-10 text-white drop-shadow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
              </svg>
              <p class="text-sm font-medium text-white drop-shadow">Contenido sensible</p>
              <span class="rounded-full bg-blanco/90 px-3 py-0.5 text-xs text-cafe">{{ textoSensible }}</span>
              <button
                type="button"
                class="mt-1 rounded-full bg-coral px-4 py-2 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
                @click="mostrarSensible = true"
              >
                Ver de todos modos
              </button>
            </div>

            <button
              v-if="galeria.length > 1 && !mostrarBlur"
              type="button"
              class="absolute left-4 top-1/2 z-10 flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full bg-white/95 text-tinta shadow-lg transition hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/80"
              aria-label="Imagen anterior"
              @click.stop="moverImagen(-1)"
            >
              <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              v-if="galeria.length > 1 && !mostrarBlur"
              type="button"
              class="absolute right-4 top-1/2 z-10 flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full bg-white/95 text-tinta shadow-lg transition hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/80"
              aria-label="Imagen siguiente"
              @click.stop="moverImagen(1)"
            >
              <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M9 5l7 7-7 7" />
              </svg>
            </button>

            <span
              v-if="galeria.length"
              class="absolute bottom-4 left-4 z-10 rounded-full bg-granate/90 px-3.5 py-1.5 text-[13px] font-bold text-white"
            >
              {{ indiceImagen + 1 }} / {{ galeria.length }}
            </span>

            <span
              v-if="activaEsVerificada"
              class="absolute right-4 top-4 z-10 inline-flex items-center gap-1 rounded-full bg-blanco/95 px-2.5 py-1 text-[11px] font-semibold text-green-700 shadow ring-1 ring-green-700/20"
            >
              <svg class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 0 1 0 1.414l-7.25 7.25a1 1 0 0 1-1.414 0l-3.25-3.25a1 1 0 1 1 1.414-1.414l2.543 2.543 6.543-6.543a1 1 0 0 1 1.414 0Z" clip-rule="evenodd" />
              </svg>
              Verificado
            </span>

            <div
              v-if="requiereAdvertencia && activaEsPortada && mostrarSensible"
              class="absolute left-4 top-4 z-10 rounded-full bg-granate/80 px-3 py-1 text-xs text-white"
            >
              Mostrando contenido sensible
              <button type="button" class="ml-2 underline hover:text-white" @click="mostrarSensible = false">
                Ocultar
              </button>
            </div>
          </div>

          <div v-if="galeria.length > 1" class="flex gap-3 overflow-x-auto pb-1">
            <button
              v-for="(img, i) in galeria"
              :key="i"
              type="button"
              class="relative h-16 w-20 shrink-0 overflow-hidden rounded-2xl border-[3px] transition"
              :class="indiceImagen === i ? 'border-coral' : 'border-transparent hover:border-rosa'"
              :aria-label="img.portada ? 'Ver portada' : `Ver imagen de ejemplo ${i}`"
              :aria-pressed="indiceImagen === i"
              @click="seleccionarImagen(i)"
            >
              <ImagenOptimizada :src="img.url" alt="" sizes="80px" class="h-full w-full object-cover" />
              <span
                v-if="esImagenVerificada(img.url)"
                class="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-green-700 text-[10px] font-bold text-white shadow"
                role="img"
                aria-label="Imagen verificada"
                title="Imagen verificada"
              >
                ✓
              </span>
              <span
                v-if="img.portada"
                class="absolute inset-x-0 bottom-0 bg-granate/60 px-1 py-0.5 text-center text-[10px] text-white"
              >
                Portada
              </span>
            </button>
          </div>

          <!--p class="text-sm text-cafe/60">Ejemplos de trabajos hechos con este servicio.</p-->
        </section>

        <aside class="order-2 h-fit rounded-[32px] border border-rosa/40 bg-superficie p-6 shadow-[0_10px_28px_rgba(194,24,91,0.10)] sm:p-8 lg:sticky lg:top-24 lg:col-start-2 lg:row-span-2 lg:row-start-1">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <h1 class="min-w-0 font-display text-4xl font-extrabold leading-[0.95] tracking-[-0.02em] text-granate sm:text-5xl">
              {{ servicio.nombre }}
            </h1>
            <button
              v-if="hayReseniasServicio"
              type="button"
              class="mt-1 inline-flex shrink-0 items-center gap-1.5 rounded-full bg-mantequilla/70 px-3.5 py-2 text-sm font-bold text-granate transition hover:bg-mantequilla"
              aria-label="Ver las reseñas de este servicio"
              @click="irAResenias"
            >
              <span aria-hidden="true">★</span>
              {{ formatearPromedio(promedioServicio) }}
              <span class="text-xs font-normal text-cafe/90">({{ cantidadReseniasServicio }})</span>
            </button>
            <span
              v-else-if="!cargandoPreview"
              class="mt-1 inline-flex shrink-0 items-center rounded-full border border-rosa/40 px-3.5 py-2 text-xs font-semibold text-cafe/60"
            >
              Sin reseñas
            </span>
          </div>

          <p class="mt-5 whitespace-pre-wrap text-[15px] leading-relaxed text-cafe/85">
            {{ servicio.descripcion }}
          </p>

          <div class="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div class="rounded-[22px] bg-mantequilla/70 px-4 py-4">
              <p class="text-[11px] font-bold uppercase tracking-wide text-cafe/90">
                {{ descuentoPorcentaje ? 'Precio con descuento' : 'Precio base' }}
              </p>
              <div class="mt-1 flex flex-wrap items-baseline gap-x-2 gap-y-1">
                <p class="font-display text-3xl font-extrabold text-granate">
                  {{ descuentoPorcentaje ? precioConDescuentoMostrado : precioMostrado }}
                </p>
                <p
                  v-if="descuentoPorcentaje"
                  class="text-lg font-semibold text-cafe/70 line-through decoration-cafe/50"
                  :title="`Precio base: ${precioMostrado}`"
                >
                  {{ precioMostrado }}
                </p>
              </div>
              <div v-if="descuentoPorcentaje" class="mt-1 flex items-center gap-1.5">
                <p class="text-xs font-bold text-coral">
                  Descuento de reactivación del {{ descuentoPorcentaje }}%
                </p>
                <span class="group relative inline-flex">
                  <button
                    type="button"
                    class="flex h-5 w-5 items-center justify-center rounded-full text-cafe/70 transition hover:bg-white/80 hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
                    :aria-label="notaDescuento"
                    :title="notaDescuento"
                  >
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                      <circle cx="12" cy="12" r="9" />
                      <path stroke-linecap="round" d="M12 11v5m0-8h.01" />
                    </svg>
                  </button>
                  <span class="pointer-events-none absolute bottom-full left-1/2 z-30 mb-2 w-64 -translate-x-1/2 rounded-xl bg-tinta px-3 py-2 text-xs font-normal leading-snug text-white opacity-0 shadow-lg transition group-hover:opacity-100 group-focus-within:opacity-100">
                    {{ notaDescuento }}
                  </span>
                </span>
              </div>
              <p v-if="precioConvertido" class="mt-1 text-xs leading-snug text-cafe/85">
                El artista lo cargó como {{ formatearPrecio(servicio.precioBase, servicio.moneda) }}
              </p>
            </div>
            <div v-if="!noDisponibleAhora" class="rounded-[22px] bg-durazno/60 px-4 py-4">
              <p class="text-[11px] font-bold uppercase tracking-wide text-cafe/90">Cupos disponibles</p>
              <p class="mt-1 font-display text-3xl font-extrabold text-coral">{{ cuposDisponibles }}</p>
              <p v-if="cuposTotales" class="mt-1 text-xs text-cafe/85">de {{ cuposTotales }} en total</p>
            </div>
            <div v-else class="rounded-[22px] border border-borde/60 bg-durazno/60 px-4 py-4">
              <p class="text-[11px] font-bold uppercase tracking-wide text-cafe/90">Disponibilidad</p>
              <p class="mt-1 font-display text-xl font-extrabold leading-tight text-granate">
                {{ detalleEstado.titulo }}
              </p>
              <p class="mt-1 text-xs leading-relaxed text-cafe/85">
                {{ detalleEstado.detalle }}
              </p>
              <p v-if="cuposTotales" class="mt-1 text-xs text-cafe/70">
                {{ cuposDisponibles }} de {{ cuposTotales }} cupos libres
              </p>
            </div>
          </div>

          <div class="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-cafe">
            <span v-if="textoEntrega(servicio)" class="inline-flex items-center gap-2">
              <svg class="h-5 w-5 text-coral" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="9" />
                <path d="M12 7v5l3 2" />
              </svg>
              Entrega estimada: <strong class="font-semibold text-granate">{{ textoEntrega(servicio) }}</strong>
            </span>
            <span
              v-if="modoRecepcionTexto"
              class="rounded-full px-3 py-1 text-xs font-semibold"
              :class="clasesEstadoRecepcion"
            >
              {{ modoRecepcionTexto }}
            </span>
          </div>

          <details class="group mt-5 rounded-2xl border border-rosa/40 bg-fondo/60 px-4 py-3">
            <summary class="flex cursor-pointer list-none items-center justify-between gap-3 text-sm font-bold text-granate">
              Detalles del servicio
              <svg class="h-4 w-4 shrink-0 transition-transform group-open:rotate-180" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clip-rule="evenodd" />
              </svg>
            </summary>
            <dl class="mt-3 space-y-2 text-sm">
              <div v-if="servicio.nombreTipoServicio" class="flex justify-between gap-4">
                <dt class="text-cafe/70">Tipo</dt>
                <dd class="text-right text-cafe">
                  {{ servicio.nombreTipoServicio }}
                  <span v-if="servicio.descripcionTipoServicio" class="mt-1 block text-xs leading-relaxed text-cafe/70">
                    {{ servicio.descripcionTipoServicio }}
                  </span>
                </dd>
              </div>
              <div v-if="servicio.nombreMedio" class="flex justify-between gap-4">
                <dt class="text-cafe/70">Medio</dt>
                <dd class="text-cafe">{{ servicio.nombreMedio }}</dd>
              </div>
              <div v-if="maxDibujos" class="flex justify-between gap-4">
                <dt class="text-cafe/70">Máximo por solicitud</dt>
                <dd class="text-cafe">{{ maxDibujos }} dibujo(s)</dd>
              </div>
              <div v-if="disponibilidad?.permiteListaEspera" class="flex justify-between gap-4">
                <dt class="text-cafe/70">Lista de espera</dt>
                <dd class="text-cafe">Disponible</dd>
              </div>
              <div class="flex justify-between gap-4">
                <dt class="text-cafe/70">Contenido sensible</dt>
                <dd class="text-right" :class="esSensible ? 'text-coral' : 'text-cafe'">
                  {{ esSensible ? `Sí · ${textoSensible}` : 'No admite' }}
                </dd>
              </div>
            </dl>
          </details>

          <div class="mt-6 space-y-3 border-t border-rosa/40 pt-5">
            <div class="flex items-stretch gap-3">
              <template v-if="!esDuenio">
                <template v-if="noDisponibleAhora">
                  <button
                    type="button"
                    class="min-h-14 flex-1 rounded-full bg-coral px-5 text-base font-bold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2"
                    @click="irAAlternativas"
                  >
                    Ver artistas con cupo
                  </button>
                  <button
                    v-if="puedeEsperar"
                    type="button"
                    class="min-h-14 shrink-0 rounded-full border-2 border-coral/60 px-5 text-sm font-bold text-granate transition hover:border-coral hover:bg-coral hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2"
                    @click="abrirModalCola"
                  >
                    {{ etiquetaCola }}
                  </button>
                </template>
                <template v-else>
                  <button
                    v-if="!autenticado"
                    type="button"
                    class="min-h-14 flex-1 rounded-full bg-coral px-5 text-base font-bold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2"
                    @click="irASolicitar"
                  >
                    Iniciá sesión para solicitar
                  </button>
                  <button
                    v-else
                    type="button"
                    class="min-h-14 flex-1 rounded-full bg-coral px-5 text-base font-bold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="!puedeSolicitar"
                    @click="irASolicitar"
                  >
                    Solicitar servicio
                  </button>
                </template>
              </template>
              <router-link
                v-else
                :to="`/panel/servicios/${servicio.idServicio}/editar`"
                class="flex min-h-14 flex-1 items-center justify-center rounded-full border-2 border-coral px-5 text-base font-bold text-coral transition hover:bg-coral hover:text-white"
              >
                Editar servicio
              </router-link>

              <button
                v-if="puedeMarcarFavorito"
                type="button"
                class="flex h-14 w-14 shrink-0 items-center justify-center rounded-full border-2 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 disabled:cursor-wait disabled:opacity-60"
                :class="esFavorito
                  ? 'border-coral bg-coral text-white hover:bg-rojizo'
                  : 'border-coral bg-superficie text-coral hover:bg-rosa/20'"
                :disabled="actualizandoFavorito || cargandoEstadoFavorito"
                :aria-pressed="esFavorito"
                :aria-label="esFavorito
                  ? `Quitar ${servicio.nombre} de tus favoritos`
                  : `Agregar ${servicio.nombre} a tus favoritos`"
                @click="alternarFavoritoServicio"
              >
                <svg
                  class="h-6 w-6"
                  viewBox="0 0 24 24"
                  :fill="esFavorito ? 'currentColor' : 'none'"
                  stroke="currentColor"
                  stroke-width="1.8"
                  aria-hidden="true"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
                </svg>
              </button>
            </div>

            <button
              v-if="hayTerminos && !esDuenio"
              type="button"
              class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
              @click="irATerminos"
            >
              Ver los términos del artista
            </button>

            <p v-if="!esDuenio" class="text-xs leading-relaxed text-cafe/75">
              {{ mensajeSolicitud || 'Revisá los términos y condiciones antes de enviar tu solicitud.' }}
            </p>
          </div>
        </aside>

        <section class="order-3 space-y-4 lg:col-start-1 lg:row-start-2">
          <div v-if="hayChips" class="rounded-[24px] border border-rosa/40 bg-superficie p-4">
            <p class="text-[11px] font-bold uppercase tracking-wide text-cafe/60">
              Categorías y etiquetas
            </p>
            <div class="mt-2.5 flex flex-wrap gap-2">
              <span
                v-if="servicio.nombreTipoServicio"
                class="rounded-full bg-rosa/40 px-3 py-1 text-xs font-bold text-granate"
              >
                {{ servicio.nombreTipoServicio }}
              </span>
              <span
                v-if="servicio.nombreMedio"
                class="rounded-full bg-mantequilla/70 px-3 py-1 text-xs font-bold text-granate"
              >
                {{ servicio.nombreMedio }}
              </span>
              <span
                v-for="etiqueta in etiquetasVisibles"
                :key="etiqueta.idEtiqueta"
                class="rounded-full bg-durazno/50 px-3 py-1 text-xs font-bold text-granate"
              >
                {{ etiqueta.nombre }}
              </span>
            </div>
          </div>

          <div class="rounded-[24px] border border-rosa/40 bg-superficie p-4">
            <p class="text-[11px] font-bold uppercase tracking-wide text-cafe/60">Artista</p>
            <div class="mt-3 flex items-center gap-3">
              <ImagenOptimizada
                v-if="fotoArtista"
                :src="fotoArtista"
                alt=""
                sizes="48px"
                class="h-12 w-12 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/50"
              />
              <span
                v-else
                class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-rosa/40 text-lg font-bold text-granate"
              >
                {{ inicialArtista }}
              </span>
              <div class="min-w-0 flex-1">
                <p class="truncate text-base font-bold text-granate">{{ nombreArtistaMostrado }}</p>
                <p class="text-[13px] text-cafe/70">
                  @{{ servicio.nombreArtista }}
                  <span v-if="hayReseniasArtista">
                    · ★ {{ formatearPromedio(promedioArtista) }} ({{ cantidadReseniasArtista }})
                  </span>
                </p>
              </div>
              <button
                type="button"
                class="inline-flex min-h-10 shrink-0 items-center justify-center rounded-full border-2 border-coral/50 px-4 text-sm font-bold text-granate transition hover:border-coral hover:bg-coral hover:text-white"
                :aria-label="`Ver perfil de ${nombreArtistaMostrado}`"
                @click="irAlArtista"
              >
                Ver perfil
              </button>
            </div>
            <p v-if="artista?.biografia" class="mt-3 line-clamp-3 text-sm leading-relaxed text-cafe/75">
              {{ artista.biografia }}
            </p>
          </div>
        </section>
      </div>

      <section ref="seccionResenias" class="mt-12 scroll-mt-24">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 class="font-display text-3xl font-extrabold text-granate sm:text-4xl">
              Lo que dicen los clientes
            </h2>
            <p class="mt-1 text-sm text-cafe/60">Reseñas verificadas de este servicio.</p>
          </div>
          <button
            v-if="hayReseniasServicio"
            type="button"
            class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
            @click="abrirResenias()"
          >
            Ver todas las reseñas →
          </button>
        </div>

        <div class="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,20rem)_minmax(0,1fr)] lg:items-start">
          <div class="rounded-[32px] border border-rosa/40 bg-superficie p-6 shadow-[0_10px_28px_rgba(194,24,91,0.10)] lg:sticky lg:top-24">
            <p v-if="cargandoPreview && !resumenResenias" class="py-6 text-center text-sm text-cafe/50">
              Cargando resumen...
            </p>
            <ResumenResenias
              v-else
              :resumen="resumenResenias"
              filtrable
              @filtrar-opcion="abrirResenias($event)"
            />
          </div>

          <div class="min-w-0">
            <p v-if="cargandoPreview" class="py-6 text-center text-sm text-cafe/55">
              Cargando reseñas...
            </p>
            <p v-else-if="errorPreview" role="alert" class="py-6 text-center text-sm text-rojizo">
              {{ errorPreview }}
            </p>
            <p
              v-else-if="!reseniasPreview.length"
              class="rounded-2xl border border-dashed border-rosa/50 px-4 py-10 text-center text-sm text-cafe/55"
            >
              Todavía no hay reseñas de este servicio.
            </p>
            <div v-else class="space-y-3">
              <TarjetaReseniaPublica
                v-for="resenia in reseniasPreview"
                :key="resenia.idResenia"
                :resenia="resenia"
              />
              <button
                v-if="cantidadReseniasServicio > reseniasPreview.length"
                type="button"
                class="w-full rounded-full border border-rosa/50 px-4 py-3 text-sm font-bold text-granate transition hover:border-coral hover:bg-rosa/10"
                @click="abrirResenias()"
              >
                Ver todas las reseñas
              </button>
            </div>
          </div>
        </div>
      </section>

      <ModalResenias
        :abierto="modalReseniasAbierto"
        titulo="Reseñas del servicio"
        :resenias="reseniasLista"
        :resumen="resumenResenias"
        :pagina="paginaResenias"
        :cargando="cargandoListaResenias"
        :error="errorListaResenias"
        :filtros="filtrosResenias"
        :filtro-opcion="filtroOpcion"
        @cerrar="cerrarResenias"
        @cambiar-filtros="cambiarFiltrosResenias"
        @cambiar-pagina="cambiarPaginaResenias"
        @quitar-filtro-opcion="quitarFiltroOpcion"
      />

      <section
        v-if="hayTerminos"
        ref="seccionTerminos"
        class="mt-12 scroll-mt-24 rounded-[32px] bg-durazno/40 p-6 sm:p-10"
      >
        <div class="grid gap-6 lg:grid-cols-[minmax(0,20rem)_minmax(0,1fr)]">
          <div>
            <h2 class="font-display text-3xl font-extrabold text-granate">Términos y condiciones</h2>
            <p class="mt-2 text-sm leading-relaxed text-cafe/70">
              Las reglas del artista para este servicio. Leelas antes de enviar tu solicitud.
            </p>
            <span
              class="mt-3 inline-block rounded-full px-3 py-1 text-xs"
              :class="terminos.propio ? 'bg-coral/10 text-granate' : 'bg-blanco/80 text-cafe/70'"
            >
              {{ terminos.propio ? 'De este servicio' : 'Generales del artista' }}
            </span>
          </div>
          <div class="rounded-[24px] bg-superficie p-5 sm:p-6">
            <VistaPreviaTerminos
              :terminos="terminosParaMostrar"
              :derechos-catalogo="derechosTerminos"
              :mostrar-aviso="false"
              :ocultar-vacios="true"
            />
          </div>
        </div>
      </section>

        <section v-if="artistasSimilares.length && !noDisponibleAhora" class="mt-12">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="text-xs font-bold uppercase tracking-[0.18em] text-coral">
              Para seguir explorando
            </p>
            <h2 class="mt-2 font-display text-3xl font-extrabold text-granate sm:text-4xl">
              Artistas similares
            </h2>
            <p class="mt-1 text-sm text-cafe/60">
              Mismo estilo o rango de precio, con cupos disponibles.
            </p>
          </div>
          <router-link
            to="/servicios"
            class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
          >
            Ver todos →
          </router-link>
        </div>

        <div class="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
          <TarjetaArtistaRecomendado
            v-for="recomendado in artistasSimilares"
            :key="recomendado.idArtista"
            :artista="recomendado"
          />
        </div>
      </section>

      <VisorImagenes
        :abierto="visorAbierto"
        :imagenes="imagenesVisor"
        :indice="indiceVisor"
        etiqueta="Ejemplos del servicio"
        :descripcion="servicio.nombre"
        @cerrar="visorAbierto = false"
        @actualizar:indice="actualizarIndiceVisor"
      />

      <ModalConfirmacion
        :abierto="modalCola"
        :titulo="tituloCola"
        :mensaje="mensajeCola"
        :texto-confirmar="etiquetaCola"
        texto-cancelar="Volver"
        @confirmar="confirmarCola"
        @cerrar="modalCola = false"
      />
    </template>
  </div>
</template>
