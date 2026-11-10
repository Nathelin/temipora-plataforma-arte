<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, TAMANO_MAXIMO_BYTES, validarImagen } from '../servicios/archivos'
import { formatearPrecio } from '../servicios/moneda'
import {
  esIlimitada,
  etiquetaStock,
  subirImagenProducto,
} from '../servicios/productos'
import { useMonedas } from '../composables/useMonedas'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import SelectorVariantes from '../components/SelectorVariantes.vue'
import SelectorEtiquetas from '../components/SelectorEtiquetas.vue'
import VisorImagenes from '../components/VisorImagenes.vue'

const route = useRoute()
const router = useRouter()

const idProducto = computed(() => (route.params.id ? Number(route.params.id) : null))
const esEdicion = computed(() => idProducto.value !== null)

const PASOS = [
  {
    n: 1,
    label: 'Detalles',
    titulo: 'Detalles',
    descripcion: 'Título, descripción, tipo, medio, imágenes y etiquetas.',
  },
  {
    n: 2,
    label: 'Precio y stock',
    titulo: 'Precio y stock',
    descripcion: 'Moneda de la publicación, presentaciones y disponibilidad.',
  },
  {
    n: 3,
    label: 'Licencia',
    titulo: 'Licencia y reventa',
    descripcion: 'Definí qué puede hacer el comprador con la obra.',
  },
  {
    n: 4,
    label: 'Entregables',
    titulo: 'Entregables',
    descripcion: 'Qué archivo recibe el comprador al pagar.',
  },
  {
    n: 5,
    label: 'Revisión',
    titulo: 'Revisión y publicación',
    descripcion: 'Revisá todo antes de publicar el producto.',
  },
]

const PASOS_VISIBLES = computed(() => (
  esEntregaFisica.value
    ? PASOS.filter((p) => p.n !== 4)
    : PASOS
))
const TOTAL_PASOS = computed(() => PASOS_VISIBLES.value.length)

const paso = ref(1)
const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const guardandoBorrador = ref(false)
const errorGuardado = ref('')
const avisoBorrador = ref('')
const errores = ref([])
const erroresCampo = ref({})
const erroresVariantes = ref({})
const erroresEntregables = ref({})
const MAX_IMAGENES_PRODUCTO = 60
const MAX_MB_IMAGEN = TAMANO_MAXIMO_BYTES / (1024 * 1024)
const subiendoImagen = ref(false)
const progresoImagen = ref('')
const errorImagen = ref('')
const arrastrandoArchivos = ref(false)
const arrastrandoImagen = ref(null)
const visorAbierto = ref(false)
const visorIndice = ref(0)
const modalReventaAbierto = ref(false)

const tipos = ref([])
const medios = ref([])
const derechos = ref([])
const { monedas, cargar: cargarMonedas, predeterminada } = useMonedas()

const SENSIBLES = [
  { valor: 'VIOLENCIA', texto: 'Violencia' },
  { valor: 'DESNUDOS', texto: 'Desnudos' },
  { valor: 'GENERAL', texto: 'Contenido sensible general' },
]

const form = ref({
  titulo: '',
  descripcion: '',
  idTipoProducto: '',
  idMedio: '',
  idMoneda: '',
  contenidoSensible: false,
  contenidosSensibles: [],
  imagenes: [],
})

const variantes = ref([])
const etiquetas = ref([])
const entregablesComunes = ref([])
const estadoProducto = ref(null)
const licencia = ref({
  idsDerechos: [],
  permiteModificaciones: false,
  requiereCredito: false,
  porcentajeRegalia: 0,
  textoAdicional: '',
})
const estadoGuardado = ref(null)

const esBorrador = computed(
  () => !esEdicion.value || estadoProducto.value === 'BORRADOR',
)

const pasoActual = computed(
  () => PASOS.find((p) => p.n === paso.value) || PASOS[0],
)
const indicePaso = computed(
  () => PASOS_VISIBLES.value.findIndex((p) => p.n === paso.value) + 1,
)
const monedaSeleccionada = computed(
  () => monedas.value.find((m) => String(m.idMoneda) === form.value.idMoneda) || null,
)
const monedaCodigo = computed(() => monedaSeleccionada.value?.codigo || 'ARS')
const nombreTipo = computed(
  () => tipos.value.find((t) => String(t.idTipoProducto) === form.value.idTipoProducto)?.nombre || '',
)
const nombreMedio = computed(
  () => medios.value.find((m) => String(m.idMedio) === form.value.idMedio)?.nombre || '',
)
const tipoSeleccionado = computed(
  () => tipos.value.find((t) => String(t.idTipoProducto) === form.value.idTipoProducto) || null,
)
const medioSeleccionado = computed(
  () => medios.value.find((m) => String(m.idMedio) === form.value.idMedio) || null,
)
const esEntregaFisica = computed(
  () => medioSeleccionado.value?.tipoEntrega === 'FISICA',
)

watch(esEntregaFisica, (fisica) => {
  if (fisica && (paso.value === 4 || paso.value === 5)) {
    paso.value = 3
    errores.value = []
    erroresCampo.value = {}
    erroresVariantes.value = {}
  }
})
const esAdoptable = computed(() => esTipoUnico(nombreTipo.value) && nombreTipo.value.toLowerCase().includes('adoptable'))
const hayVarianteUnica = computed(
  () => variantes.value.some(
    (variante) => !esIlimitada(variante) && Number(variante.stockDisponible) === 1,
  ),
)
const derechosSeleccionados = computed(() => derechos.value.filter((derecho) =>
  licencia.value.idsDerechos.some(
    (id) => String(id) === String(derecho.idDerechoTermino),
  ),
))
const derechoReventa = computed(
  () => derechosSeleccionados.value.some((derecho) => derecho.requierePiezaUnica),
)
const resumenUsos = computed(
  () => derechosSeleccionados.value.map((derecho) => derecho.nombre).join(' · ')
    || 'Sin usos definidos',
)
const resumenReventa = computed(() => {
  if (!derechoReventa.value) return 'No permitida'
  const regalia = Number(licencia.value.porcentajeRegalia || 0)
  return regalia > 0
    ? `Permitida con regalía del ${regalia}%`
    : 'Permitida sin regalía'
})
const precioDesde = computed(() => {
  const precios = variantes.value
    .map((variante) => Number(variante.precio))
    .filter((precio) => Number.isFinite(precio) && precio > 0)
  return precios.length ? Math.min(...precios) : null
})
const imagenesUrl = computed(() => form.value.imagenes.map((imagen) => imagen.url))
const tituloPagina = computed(() => {
  if (!esEdicion.value) return 'Crear producto'
  return estadoProducto.value === 'BORRADOR' ? 'Editar borrador' : 'Editar producto'
})

function esTipoUnico(nombre) {
  const texto = String(nombre || '').toLowerCase()
  return texto.includes('adoptable')
    || texto.includes('tradicional')
    || texto.includes('pieza única')
    || texto.includes('pieza unica')
}

function instantanea() {
  return JSON.stringify({
    form: form.value,
    variantes: variantes.value,
    etiquetas: etiquetas.value,
    licencia: licencia.value,
    entregablesComunes: entregablesComunes.value,
  })
}

const hayCambios = computed(
  () => estadoGuardado.value !== null && estadoGuardado.value !== instantanea(),
)

function nuevaPresentacion(overrides = {}) {
  return {
    nombre: 'Única',
    descripcion: '',
    precio: '',
    stockDisponible: null,
    estado: 'DISPONIBLE',
    urlImagen: '',
    sha256: '',
    claveArchivoOriginal: '',
    numeroEdicion: null,
    ...overrides,
  }
}

function seleccionarDerechoPorCodigo(codigo) {
  const derecho = derechos.value.find((item) => item.codigo === codigo)
  if (derecho) {
    licencia.value.idsDerechos = [derecho.idDerechoTermino]
  }
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [tiposRes, mediosRes, derechosRes] = await Promise.all([
      api.get('/tipos-producto'),
      api.get('/medios'),
      api.get('/derechos-termino'),
      cargarMonedas(),
    ])
    tipos.value = tiposRes.data
    medios.value = mediosRes.data
    derechos.value = derechosRes.data

    if (esEdicion.value) {
      const productoRes = await api.get(`/mis-productos/${idProducto.value}`)
      mapearProducto(productoRes.data)
    } else {
      const pred = predeterminada()
      form.value.idMoneda = pred ? String(pred.idMoneda) : ''
      variantes.value = [nuevaPresentacion()]
      seleccionarDerechoPorCodigo('PERSONAL')
    }
    estadoGuardado.value = instantanea()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el producto.'
  } finally {
    cargando.value = false
  }
}

function mapearProducto(d) {
  estadoProducto.value = d.estado || null
  form.value = {
    titulo: d.titulo || '',
    descripcion: d.descripcion || '',
    idTipoProducto: d.idTipoProducto ? String(d.idTipoProducto) : '',
    idMedio: d.idMedio ? String(d.idMedio) : '',
    idMoneda: d.idMoneda ? String(d.idMoneda) : '',
    contenidoSensible: !!d.contenidoSensible,
    contenidosSensibles: Array.isArray(d.contenidosSensibles)
      ? [...d.contenidosSensibles]
      : [],
    imagenes: (d.imagenes || []).map((imagen) => ({
      url: imagen.url,
      sha256: imagen.sha256,
      claveArchivoOriginal: '',
    })),
  }

  const entregables = Array.isArray(d.entregables) ? d.entregables : []
  const entregablesPorVariante = new Map(
    entregables
      .filter((entregable) => entregable.idVariante)
      .map((entregable) => [entregable.idVariante, entregable]),
  )

  variantes.value = (d.variantes || []).map((variante) => {
    const entregable = entregablesPorVariante.get(variante.idVariante)
    return {
      idVariante: variante.idVariante,
      nombre: variante.nombre || 'Única',
      descripcion: variante.descripcion || '',
      precio: Number(variante.precio) || '',
      stockDisponible: variante.stockDisponible === null
        || variante.stockDisponible === undefined
        ? null
        : Number(variante.stockDisponible),
      estado: variante.estado || 'DISPONIBLE',
      urlImagen: variante.imagenUrl || '',
      sha256: variante.sha256 || '',
      claveArchivoOriginal: '',
      numeroEdicion: variante.numeroEdicion || null,
      tieneLedger: !!variante.tieneLedger,
      entregableSha256: entregable?.sha256 || '',
      entregableNombre: entregable?.nombre || '',
    }
  })
  if (!variantes.value.length) {
    variantes.value = [nuevaPresentacion()]
  }

  entregablesComunes.value = entregables
    .filter((entregable) => !entregable.idVariante)
    .map((entregable) => ({
      sha256: entregable.sha256 || '',
      nombre: entregable.nombre || '',
      orden: entregable.orden || 0,
    }))

  etiquetas.value = Array.isArray(d.etiquetas) ? [...d.etiquetas] : []

  const lic = d.licencia || {}
  licencia.value = {
    idsDerechos: (lic.derechos || []).map((derecho) => derecho.idDerechoTermino),
    permiteModificaciones: !!lic.permiteModificaciones,
    requiereCredito: !!lic.requiereCredito,
    porcentajeRegalia: Number(lic.porcentajeRegalia || 0),
    textoAdicional: lic.textoAdicional || '',
  }
  if (!licencia.value.idsDerechos.length) {
    seleccionarDerechoPorCodigo('PERSONAL')
  }
}

watch(() => form.value.idTipoProducto, (nuevo, anterior) => {
  if (!nuevo || nuevo === anterior) return
  if (!esTipoUnico(nombreTipo.value) || variantes.value.length !== 1) return
  const unica = variantes.value[0]
  if (!unica.precio && esIlimitada(unica)) {
    variantes.value = [{ ...unica, stockDisponible: 1 }]
  }
})

async function subirArchivos(archivos) {
  if (!archivos.length) return
  const restantes = MAX_IMAGENES_PRODUCTO - form.value.imagenes.length
  if (restantes <= 0) {
    errorImagen.value = `Máximo ${MAX_IMAGENES_PRODUCTO} imágenes por publicación.`
    return
  }

  const lote = archivos.slice(0, restantes)
  const validos = []
  const problemas = []
  for (const archivo of lote) {
    const problema = validarImagen(archivo)
    if (problema) {
      problemas.push(`${archivo.name}: ${problema}`)
    } else {
      validos.push(archivo)
    }
  }
  if (archivos.length > restantes) {
    problemas.push(`Solo se pueden agregar ${restantes} imágenes más.`)
  }
  errorImagen.value = problemas.join(' · ')
  if (!validos.length) return

  subiendoImagen.value = true
  try {
    for (let i = 0; i < validos.length; i++) {
      progresoImagen.value = `Subiendo ${i + 1} de ${validos.length}...`
      const data = await subirImagenProducto(validos[i])
      form.value.imagenes.push({
        url: data.url,
        sha256: data.sha256,
        claveArchivoOriginal: data.claveOriginal,
      })
    }
  } catch (e) {
    errorImagen.value = e.response?.data?.message
      || 'No se pudieron subir todas las imágenes.'
  } finally {
    subiendoImagen.value = false
    progresoImagen.value = ''
  }
}

function subirImagen(event) {
  const archivos = Array.from(event.target.files || [])
  event.target.value = ''
  subirArchivos(archivos)
}

function soltarArchivos(event) {
  arrastrandoArchivos.value = false
  const archivos = Array.from(event.dataTransfer?.files || [])
  subirArchivos(archivos)
}

function usarComoPortada(indice) {
  if (indice === 0) return
  const copia = [...form.value.imagenes]
  const [portada] = copia.splice(indice, 1)
  copia.unshift(portada)
  form.value.imagenes = copia
}

function iniciarArrastreImagen(indice, event) {
  arrastrandoImagen.value = indice
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(indice))
}

function soltarImagen(destino, event) {
  event.preventDefault()
  const origen = arrastrandoImagen.value
  arrastrandoImagen.value = null
  if (origen === null || origen === destino) return
  const copia = [...form.value.imagenes]
  const [movida] = copia.splice(origen, 1)
  copia.splice(destino, 0, movida)
  form.value.imagenes = copia
}

function terminarArrastreImagen() {
  arrastrandoImagen.value = null
}

function quitarImagen(indice) {
  form.value.imagenes.splice(indice, 1)
}

function agregarImagenDesdeVariante(imagen) {
  if (!imagen?.url) return
  if (!form.value.imagenes.some((item) => item.url === imagen.url)) {
    form.value.imagenes.push({
      url: imagen.url,
      sha256: imagen.sha256,
      claveArchivoOriginal: imagen.claveArchivoOriginal || '',
    })
  }
}

function moverImagen(indice, pasoDireccion) {
  const destino = indice + pasoDireccion
  if (destino < 0 || destino >= form.value.imagenes.length) return
  const copia = [...form.value.imagenes]
  const [movida] = copia.splice(indice, 1)
  copia.splice(destino, 0, movida)
  form.value.imagenes = copia
}

function abrirVisor(indice) {
  visorIndice.value = indice
  visorAbierto.value = true
}

function limpiarErroresCampos() {
  erroresCampo.value = {}
  erroresVariantes.value = {}
}

function validarDatos() {
  const errs = []
  if (!form.value.titulo.trim()) {
    const mensaje = 'El título es obligatorio.'
    errs.push(mensaje)
    erroresCampo.value.titulo = mensaje
  }
  if (!form.value.idTipoProducto) {
    const mensaje = 'Elegí un tipo de producto.'
    errs.push(mensaje)
    erroresCampo.value.idTipoProducto = mensaje
  }
  if (!form.value.idMedio) {
    const mensaje = 'Elegí un medio.'
    errs.push(mensaje)
    erroresCampo.value.idMedio = mensaje
  }
  if (!form.value.descripcion.trim()) {
    const mensaje = 'La descripción es obligatoria.'
    errs.push(mensaje)
    erroresCampo.value.descripcion = mensaje
  }
  if (form.value.contenidoSensible && !form.value.contenidosSensibles.length) {
    const mensaje = 'Elegí qué tipo de contenido sensible puede aparecer.'
    errs.push(mensaje)
    erroresCampo.value.contenidosSensibles = mensaje
  }
  return errs
}

function validarImagenes() {
  const errs = []
  if (!form.value.imagenes.length) {
    const mensaje = 'Agregá al menos una imagen del producto.'
    errs.push(mensaje)
    erroresCampo.value.imagenes = mensaje
  } else if (form.value.imagenes.length > MAX_IMAGENES_PRODUCTO) {
    const mensaje = `Máximo ${MAX_IMAGENES_PRODUCTO} imágenes por publicación.`
    errs.push(mensaje)
    erroresCampo.value.imagenes = mensaje
  }
  return errs
}

function validarMoneda() {
  if (!form.value.idMoneda) {
    const mensaje = 'Elegí la moneda de la publicación.'
    erroresCampo.value.idMoneda = mensaje
    return [mensaje]
  }
  return []
}

function validarVariantes() {
  const errs = []
  erroresVariantes.value = {}

  if (variantes.value.length > 50) {
    const mensaje = 'Un producto puede tener como máximo 50 variantes.'
    errs.push(mensaje)
    erroresCampo.value.variantes = mensaje
    return errs
  }
  if (!variantes.value.length) {
    const mensaje = 'Agregá al menos una presentación.'
    errs.push(mensaje)
    erroresCampo.value.variantes = mensaje
    return errs
  }

  const presentacionUnica = variantes.value.length === 1
  const nombres = new Set()
  variantes.value.forEach((variante, indice) => {
    const campos = {}
    if (!presentacionUnica) {
      const nombre = String(variante.nombre || '').trim()
      if (!nombre) {
        campos.nombre = `La variante ${indice + 1} necesita un nombre.`
        errs.push(campos.nombre)
      } else if (nombres.has(nombre.toLowerCase())) {
        campos.nombre = `El nombre “${nombre}” está repetido.`
        errs.push(campos.nombre)
      } else {
        nombres.add(nombre.toLowerCase())
      }
    }
    if (!variante.precio || Number(variante.precio) <= 0) {
      campos.precio = presentacionUnica
        ? 'El precio debe ser mayor a cero.'
        : `El precio de la variante ${indice + 1} debe ser mayor a cero.`
      errs.push(campos.precio)
    }
    if (variante.stockDisponible !== null
        && variante.stockDisponible !== undefined
        && Number(variante.stockDisponible) < 0) {
      campos.stock = `El stock de la variante ${indice + 1} no puede ser negativo.`
      errs.push(campos.stock)
    }
    const tieneImagen = variante.urlImagen && variante.sha256
    const usaPortada = presentacionUnica && form.value.imagenes[0]?.url
    if (!tieneImagen && !usaPortada) {
      campos.imagen = presentacionUnica
        ? 'Subí una imagen de portada para la presentación.'
        : `Subí la imagen de la variante ${indice + 1}.`
      errs.push(campos.imagen)
    }
    if (Object.keys(campos).length) {
      erroresVariantes.value[indice] = campos
    }
  })

  if (errs.length) {
    erroresCampo.value.variantes = 'Revisá las variantes marcadas.'
  }
  return errs
}

function validarLicencia() {
  const errs = []
  if (!licencia.value.idsDerechos.length) {
    const mensaje = 'Elegí al menos un uso permitido para la licencia.'
    errs.push(mensaje)
    erroresCampo.value.licencia = mensaje
  }
  if (derechoReventa.value && !hayVarianteUnica.value) {
    const mensaje = 'Para habilitar la reventa necesitás al menos una presentación con stock 1.'
    errs.push(mensaje)
    erroresCampo.value.licencia = mensaje
  }
  if (derechoReventa.value) {
    const regalia = Number(licencia.value.porcentajeRegalia || 0)
    if (regalia < 0 || regalia > 10) {
      const mensaje = 'La regalía debe estar entre 0 y 10%.'
      errs.push(mensaje)
      erroresCampo.value.porcentajeRegalia = mensaje
    }
  }
  return errs
}

function validarEntregables() {
  const errs = []
  erroresEntregables.value = {}
  if (esEntregaFisica.value) return errs

  variantes.value.forEach((variante, indice) => {
    const tieneImagen = variante.sha256 || variante.urlImagen
    const tieneEntregable = variante.entregableSha256 || tieneImagen
    if (!tieneEntregable) {
      const mensaje = presentacionUnica.value
        ? 'El comprador tiene que recibir un archivo.'
        : `La variante ${indice + 1} no tiene archivo entregable.`
      erroresEntregables.value[indice] = mensaje
      errs.push(mensaje)
    }
  })

  entregablesComunes.value.forEach((entregable, indice) => {
    if (!entregable.sha256) {
      const mensaje = `Elegí el archivo del entregable común ${indice + 1}.`
      erroresEntregables.value[`comun-${indice}`] = mensaje
      errs.push(mensaje)
    }
  })

  if (errs.length) {
    erroresCampo.value.entregables = 'Revisá los entregables marcados.'
  }
  return errs
}

function validarPasoActual() {
  limpiarErroresCampos()
  let problemas = []
  if (paso.value === 1) problemas = [...validarDatos(), ...validarImagenes()]
  else if (paso.value === 2) problemas = [...validarMoneda(), ...validarVariantes()]
  else if (paso.value === 3) problemas = validarLicencia()
  else if (paso.value === 4) problemas = validarEntregables()
  else if (paso.value === 5) problemas = validarTodo()

  errores.value = problemas
  return problemas.length === 0
}

function validarTodo() {
  limpiarErroresCampos()
  const datos = [...validarDatos(), ...validarImagenes()]
  if (datos.length) {
    errores.value = datos
    paso.value = 1
    return false
  }
  const precio = [...validarMoneda(), ...validarVariantes()]
  if (precio.length) {
    errores.value = precio
    paso.value = 2
    return false
  }
  const lic = validarLicencia()
  if (lic.length) {
    errores.value = lic
    paso.value = 3
    return false
  }
  const entregables = validarEntregables()
  if (entregables.length) {
    errores.value = entregables
    paso.value = 4
    return false
  }
  errores.value = []
  return true
}

function irAlPrincipio() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function siguiente() {
  if (!validarPasoActual()) return
  const orden = PASOS_VISIBLES.value.map((p) => p.n)
  const actual = orden.indexOf(paso.value)
  if (actual >= 0 && actual < orden.length - 1) {
    paso.value = orden[actual + 1]
    irAlPrincipio()
  }
}

function anterior() {
  errores.value = []
  limpiarErroresCampos()
  const orden = PASOS_VISIBLES.value.map((p) => p.n)
  const actual = orden.indexOf(paso.value)
  if (actual > 0) {
    paso.value = orden[actual - 1]
    irAlPrincipio()
  }
}

function irAPaso(n) {
  if (n < paso.value) {
    errores.value = []
    limpiarErroresCampos()
    paso.value = n
    irAlPrincipio()
  }
}

function irAEditar(n) {
  errores.value = []
  limpiarErroresCampos()
  errorGuardado.value = ''
  paso.value = n
  irAlPrincipio()
}

function textoODefecto(valor) {
  const limpio = String(valor || '').trim()
  return limpio === '' ? undefined : limpio
}

function construirPayload() {
  const portada = form.value.imagenes[0]
  const presentacion = variantes.value.length === 1
  return {
    idTipoProducto: form.value.idTipoProducto
      ? Number(form.value.idTipoProducto) : null,
    idMedio: form.value.idMedio ? Number(form.value.idMedio) : null,
    titulo: form.value.titulo.trim(),
    descripcion: form.value.descripcion.trim(),
    idMoneda: form.value.idMoneda ? Number(form.value.idMoneda) : null,
    contenidoSensible: form.value.contenidoSensible,
    contenidosSensibles: form.value.contenidoSensible
      ? form.value.contenidosSensibles
      : [],
    imagenes: form.value.imagenes.map((imagen, indice) => ({
      url: imagen.url,
      sha256: imagen.sha256,
      claveArchivoOriginal: textoODefecto(imagen.claveArchivoOriginal),
      orden: indice,
    })),
    variantes: variantes.value.map((variante, indice) => {
      const usarPortada = presentacion && !variante.urlImagen && portada
      return {
        idVariante: variante.idVariante || undefined,
        nombre: String(variante.nombre || 'Única').trim(),
        descripcion: textoODefecto(variante.descripcion) || null,
        precio: variante.precio === '' || variante.precio === null
          ? null
          : Number(variante.precio),
        stockDisponible: variante.stockDisponible === null
          || variante.stockDisponible === undefined
          ? null
          : Number(variante.stockDisponible),
        orden: indice,
        estado: variante.estado || 'DISPONIBLE',
        urlImagen: textoODefecto(usarPortada ? portada.url : variante.urlImagen),
        sha256: textoODefecto(usarPortada ? portada.sha256 : variante.sha256),
        claveArchivoOriginal: textoODefecto(usarPortada
          ? portada.claveArchivoOriginal
          : variante.claveArchivoOriginal),
        numeroEdicion: variante.numeroEdicion || null,
        entregableSha256: textoODefecto(variante.entregableSha256),
        entregableNombre: textoODefecto(variante.entregableNombre),
      }
    }),
    idsEtiquetas: etiquetas.value.map((etiqueta) => etiqueta.idEtiqueta),
    licencia: {
      idsDerechos: licencia.value.idsDerechos,
      permiteModificaciones: licencia.value.permiteModificaciones,
      requiereCredito: licencia.value.requiereCredito,
      porcentajeRegalia: derechoReventa.value
        ? Number(licencia.value.porcentajeRegalia || 0)
        : 0,
      textoAdicional: licencia.value.textoAdicional?.trim() || null,
    },
    entregablesComunes: entregablesComunes.value.map((entregable, indice) => ({
      sha256: textoODefecto(entregable.sha256),
      nombre: textoODefecto(entregable.nombre),
      orden: indice,
    })),
  }
}

async function guardarBorrador(alSalir = false) {
  errorGuardado.value = ''
  avisoBorrador.value = ''
  guardandoBorrador.value = true
  try {
    const payload = construirPayload()
    const res = esEdicion.value
      ? await api.put(`/mis-productos/${idProducto.value}/borrador`, payload)
      : await api.post('/mis-productos/borradores', payload)

    estadoProducto.value = res.data.estado
    estadoGuardado.value = instantanea()
    limpiarCambios()
    retirarAlternativa()
    avisoBorrador.value = 'Borrador guardado.'

    if (!alSalir && !esEdicion.value && res.data.idProducto) {
      await router.replace(`/panel/productos/${res.data.idProducto}/editar`)
    }
  } catch (e) {
    errorGuardado.value = e.response?.data?.message
      || 'No se pudo guardar el borrador.'
    if (!alSalir) irAlPrincipio()
    throw e
  } finally {
    guardandoBorrador.value = false
  }
}

async function publicar() {
  errorGuardado.value = ''
  avisoBorrador.value = ''
  if (!validarTodo()) {
    irAlPrincipio()
    return
  }

  guardando.value = true
  try {
    const payload = construirPayload()
    if (esEdicion.value && estadoProducto.value === 'BORRADOR') {
      await api.post(`/mis-productos/${idProducto.value}/publicar`, payload)
    } else if (esEdicion.value) {
      await api.put(`/mis-productos/${idProducto.value}`, payload)
    } else {
      await api.post('/mis-productos', payload)
    }
    estadoGuardado.value = instantanea()
    limpiarCambios()
    router.push('/panel/productos')
  } catch (e) {
    errorGuardado.value = e.response?.data?.message
      || 'No se pudo publicar el producto.'
    irAlPrincipio()
  } finally {
    guardando.value = false
  }
}

function irAlListado() {
  router.push('/panel/productos')
}

function imagenVariante(variante) {
  if (variante.urlImagen) return variante.urlImagen
  if (variantes.value.length === 1 && form.value.imagenes[0]) {
    return form.value.imagenes[0].url
  }
  return ''
}

function nombrePresentacion(variante) {
  return variantes.value.length === 1 ? 'Presentación' : variante.nombre
}

const presentacionUnica = computed(() => variantes.value.length === 1)

function imagenDeSha(sha) {
  if (!sha) return null
  const buscado = String(sha).toLowerCase()
  return form.value.imagenes.find(
    (imagen) => String(imagen.sha256 || '').toLowerCase() === buscado,
  ) || null
}

function usarImagenComoEntregable(variante) {
  if (!variante.sha256) return
  variante.entregableSha256 = variante.sha256
  variante.entregableNombre = nombrePresentacion(variante)
}

function elegirEntregableVariante(event, variante) {
  const sha = event.target.value
  if (!sha) return
  variante.entregableSha256 = sha
  variante.entregableNombre = variante.entregableNombre
    || (imagenDeSha(sha)?.url || '').split('/').pop()
    || nombrePresentacion(variante)
}

function asignarTodosLosEntregables() {
  variantes.value.forEach((variante) => {
    if (variante.sha256 && !variante.entregableSha256) {
      usarImagenComoEntregable(variante)
    }
  })
}

function agregarEntregableComun() {
  const portada = form.value.imagenes[0]
  if (!portada) return
  if (entregablesComunes.value.some((item) => item.sha256 === portada.sha256)) return
  entregablesComunes.value.push({
    sha256: portada.sha256,
    nombre: 'Archivo común',
    orden: entregablesComunes.value.length,
  })
}

function quitarEntregableComun(indice) {
  entregablesComunes.value.splice(indice, 1)
}

function cambiarEntregableComun(event, entregable) {
  entregable.sha256 = event.target.value
}

const {
  actualizar: marcarCambios,
  limpiar: limpiarCambios,
  ofrecerAlternativa,
  retirarAlternativa,
} = useCambiosSinGuardar()

watch(hayCambios, (pendiente) => {
  marcarCambios(
    pendiente,
    'Tenés cambios sin guardar en el producto. Podés guardarlos como borrador '
      + 'y continuar más tarde, o salir y perderlos.',
  )
  if (pendiente && esBorrador.value) {
    ofrecerAlternativa('Guardar borrador y salir', async () => {
      await guardarBorrador(true)
    })
  } else {
    retirarAlternativa()
  }
})

function antesDeSalir(event) {
  if (hayCambios.value && !guardando.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

onMounted(() => {
  cargar()
  window.addEventListener('beforeunload', antesDeSalir)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', antesDeSalir)
  limpiarCambios()
  retirarAlternativa()
})
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-4 py-8 sm:px-8">
    <button
      type="button"
      class="text-sm text-granate transition hover:text-coral"
      @click="irAlListado"
    >
      ← Volver a mis productos
    </button>

    <div class="mx-auto mt-4 text-center">
      <div class="flex flex-wrap items-center justify-center gap-2">
        <h1 class="text-2xl text-granate sm:text-3xl">{{ tituloPagina }}</h1>
        <span
          v-if="esEdicion && estadoProducto === 'BORRADOR'"
          class="rounded-full border border-cafe/20 bg-cafe/10 px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-cafe/70"
        >
          Borrador
        </span>
      </div>
      <p class="mt-2 text-sm text-cafe/60">
        Completá la publicación y sus condiciones paso a paso. Podés guardar un
        borrador y continuar más tarde.
      </p>
    </div>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <template v-else>
      <nav aria-label="Pasos del alta de producto" class="mt-8">
        <ol class="mx-auto flex w-full max-w-4xl items-start">
          <li
            v-for="(p, i) in PASOS_VISIBLES"
            :key="p.n"
            class="flex items-start"
            :class="i < PASOS_VISIBLES.length - 1 ? 'flex-1' : ''"
          >
            <button
              type="button"
              class="group flex w-16 shrink-0 flex-col items-center gap-2 sm:w-24"
              :disabled="p.n >= paso"
              @click="irAPaso(p.n)"
            >
              <span
                class="flex h-9 w-9 items-center justify-center rounded-full border text-sm transition"
                :class="paso === p.n
                  ? 'border-coral bg-coral text-white shadow-md shadow-coral/20'
                  : (p.n < paso
                    ? 'border-coral/40 bg-coral/10 text-coral'
                    : 'border-rosa bg-blanco text-cafe/40')"
              >
                <svg
                  v-if="p.n < paso"
                  class="h-4 w-4"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                </svg>
                <span v-else>{{ i + 1 }}</span>
              </span>
              <span
                class="hidden text-center text-[11px] leading-tight transition sm:block"
                :class="paso === p.n ? 'text-granate' : 'text-cafe/50'"
              >
                {{ p.label }}
              </span>
            </button>
            <span
              v-if="i < PASOS_VISIBLES.length - 1"
              class="mt-4.5 h-px flex-1 transition"
              :class="p.n < paso ? 'bg-coral/50' : 'bg-rosa/70'"
            ></span>
          </li>
        </ol>
        <p class="mt-4 text-center text-xs text-cafe/50 sm:hidden">
          Paso {{ indicePaso }} de {{ TOTAL_PASOS }} · {{ pasoActual.label }}
        </p>
      </nav>

      <div class="mx-auto mt-8 w-full max-w-4xl">
        <div class="rounded-3xl border border-rosa/50 bg-blanco p-6 shadow-sm sm:p-10">
          <header class="mb-8">
            <p class="text-xs uppercase tracking-[0.18em] text-coral">
              Paso {{ indicePaso }} de {{ TOTAL_PASOS }}
            </p>
            <h2 class="mt-2 text-xl text-granate sm:text-2xl">{{ pasoActual.titulo }}</h2>
            <p class="mt-1 text-sm text-cafe/60">{{ pasoActual.descripcion }}</p>
          </header>

          <ul
            v-if="errores.length"
            class="mb-6 space-y-1 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
          >
            <li v-for="problema in errores" :key="problema">• {{ problema }}</li>
          </ul>

          <!-- Paso 1: publicación -->
          <section v-if="paso === 1" class="space-y-8">
            <div class="space-y-6">
              <div>
                <label class="mb-2 block text-sm text-cafe">
                  Título <span class="text-rojizo">*</span>
                </label>
                <input
                  v-model="form.titulo"
                  type="text"
                  maxlength="100"
                  placeholder="Ej. Plancha de adoptables de otoño"
                  :aria-invalid="!!erroresCampo.titulo"
                  class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                />
                <p v-if="erroresCampo.titulo" class="mt-1 text-xs font-medium text-rojizo">
                  {{ erroresCampo.titulo }}
                </p>
              </div>

              <div class="grid gap-6 sm:grid-cols-2">
                <div>
                  <label class="mb-2 block text-sm text-cafe">
                    Tipo de producto <span class="text-rojizo">*</span>
                  </label>
                  <select
                    v-model="form.idTipoProducto"
                    class="block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  >
                    <option value="" disabled>Elegí un tipo</option>
                    <option
                      v-for="tipo in tipos"
                      :key="tipo.idTipoProducto"
                      :value="String(tipo.idTipoProducto)"
                    >
                      {{ tipo.nombre }}
                    </option>
                  </select>
                  <p v-if="erroresCampo.idTipoProducto" class="mt-1 text-xs font-medium text-rojizo">
                    {{ erroresCampo.idTipoProducto }}
                  </p>
                  <div
                    v-if="tipoSeleccionado?.descripcion"
                    class="mt-2 rounded-lg bg-crema/60 px-3 py-2 text-xs leading-relaxed text-cafe/65"
                  >
                    <p>{{ tipoSeleccionado.descripcion }}</p>
                    <p v-if="tipoSeleccionado.observaciones" class="mt-1 text-cafe/50">
                      {{ tipoSeleccionado.observaciones }}
                    </p>
                  </div>
                </div>

                <div>
                  <label class="mb-2 block text-sm text-cafe">
                    Medio <span class="text-rojizo">*</span>
                  </label>
                  <select
                    v-model="form.idMedio"
                    class="block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  >
                    <option value="" disabled>Elegí un medio</option>
                    <option
                      v-for="medio in medios"
                      :key="medio.idMedio"
                      :value="String(medio.idMedio)"
                    >
                      {{ medio.nombre }}
                    </option>
                  </select>
                  <p v-if="erroresCampo.idMedio" class="mt-1 text-xs font-medium text-rojizo">
                    {{ erroresCampo.idMedio }}
                  </p>
                  <div
                    v-if="medioSeleccionado?.descripcion"
                    class="mt-2 rounded-lg bg-crema/60 px-3 py-2 text-xs leading-relaxed text-cafe/65"
                  >
                    <p>{{ medioSeleccionado.descripcion }}</p>
                    <p v-if="medioSeleccionado.observaciones" class="mt-1 text-cafe/50">
                      {{ medioSeleccionado.observaciones }}
                    </p>
                  </div>
                  <div
                    v-if="esEntregaFisica"
                    class="mt-2 rounded-xl border border-coral/30 bg-rosa/15 px-3 py-2 text-xs leading-relaxed text-granate"
                  >
                    <p class="font-semibold">Temipora no gestiona envíos.</p>
                    <p class="mt-1">
                      El envío y la entrega se coordinan directamente entre artista y comprador.
                    </p>
                  </div>
                </div>
              </div>

              <div>
                <label class="mb-2 block text-sm text-cafe">
                  Descripción <span class="text-rojizo">*</span>
                </label>
                <textarea
                  v-model="form.descripcion"
                  rows="4"
                  maxlength="500"
                  class="block w-full resize-none rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                ></textarea>
                <p class="mt-1 text-right text-xs text-cafe/50">
                  {{ form.descripcion.length }}/500
                </p>
                <p v-if="erroresCampo.descripcion" class="mt-1 text-xs font-medium text-rojizo">
                  {{ erroresCampo.descripcion }}
                </p>
              </div>

              <div class="rounded-2xl border border-rosa/60 bg-crema/50 p-5">
                <label class="inline-flex items-center gap-2 text-sm font-medium text-granate">
                  <input
                    v-model="form.contenidoSensible"
                    type="checkbox"
                    class="h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                  />
                  La publicación presenta contenido sensible
                </label>
                <div v-if="form.contenidoSensible" class="mt-4 grid gap-2 sm:grid-cols-3">
                  <label
                    v-for="tipo in SENSIBLES"
                    :key="tipo.valor"
                    class="inline-flex items-center gap-2 rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe"
                  >
                    <input
                      v-model="form.contenidosSensibles"
                      type="checkbox"
                      :value="tipo.valor"
                      class="h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                    />
                    {{ tipo.texto }}
                  </label>
                </div>
                <p v-if="erroresCampo.contenidosSensibles" class="mt-2 text-xs font-medium text-rojizo">
                  {{ erroresCampo.contenidosSensibles }}
                </p>
              </div>
            </div>

            <div>
              <div class="flex flex-wrap items-center justify-between gap-2">
                <h3 class="font-display text-lg font-bold text-granate">Portada y galería</h3>
                <button
                  v-if="form.imagenes.length > 3"
                  type="button"
                  class="text-xs font-medium text-coral transition hover:text-rojizo"
                  @click="abrirVisor(0)"
                >
                  Ver galería ({{ form.imagenes.length }})
                </button>
              </div>
              <p class="mt-1 text-sm text-cafe/60">
                La primera imagen es la portada. Podés cambiarla con “Usar como
                portada”. La galería se reutiliza para las variantes sin volver a
                subirlas.
              </p>

              <div
                class="mt-4 rounded-2xl border border-dashed p-6 text-center transition"
                :class="arrastrandoArchivos
                  ? 'border-coral bg-rosa/20'
                  : 'border-rosa/70 bg-crema/40'"
                @dragenter.prevent="arrastrandoArchivos = true"
                @dragover.prevent="arrastrandoArchivos = true"
                @dragleave.prevent="arrastrandoArchivos = false"
                @drop.prevent="soltarArchivos"
              >
                <p class="text-sm font-medium text-granate">
                  Arrastrá y soltá imágenes acá
                </p>
                <p class="mt-1 text-xs text-cafe/55">
                  o elegí archivos desde tu dispositivo. Se guarda el original
                  privado y una preview pública optimizada.
                </p>
                <p class="mt-2 text-[11px] text-cafe/45">
                  JPG, PNG o WebP · hasta {{ MAX_MB_IMAGEN }} MB por imagen ·
                  {{ form.imagenes.length }}/{{ MAX_IMAGENES_PRODUCTO }} imágenes
                </p>
                <label
                  class="mt-4 inline-flex cursor-pointer items-center justify-center rounded-lg bg-coral px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo"
                  :class="subiendoImagen || form.imagenes.length >= MAX_IMAGENES_PRODUCTO
                    ? 'pointer-events-none opacity-50'
                    : ''"
                >
                  {{ subiendoImagen ? (progresoImagen || 'Subiendo...') : 'Elegir imágenes' }}
                  <input
                    type="file"
                    class="sr-only"
                    multiple
                    :accept="acceptImagenes"
                    @change="subirImagen"
                  />
                </label>
                <p v-if="errorImagen" class="mt-2 text-sm text-rojizo">{{ errorImagen }}</p>
              </div>

              <div
                v-if="form.imagenes.length"
                class="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3"
              >
                <article
                  v-for="(imagen, indice) in form.imagenes"
                  :key="`${imagen.url}-${indice}`"
                  draggable="true"
                  class="cursor-move overflow-hidden rounded-2xl border border-rosa/60 bg-blanco shadow-sm transition"
                  :class="arrastrandoImagen === indice ? 'opacity-50 ring-2 ring-coral/40' : ''"
                  @dragstart="iniciarArrastreImagen(indice, $event)"
                  @dragover.prevent
                  @drop="soltarImagen(indice, $event)"
                  @dragend="terminarArrastreImagen"
                >
                  <button
                    type="button"
                    class="block w-full"
                    @click="abrirVisor(indice)"
                  >
                    <img
                      :src="imagen.url"
                      :alt="`Imagen ${indice + 1} del producto`"
                      class="aspect-square w-full object-cover"
                    />
                  </button>
                  <div class="px-3 py-2">
                    <div class="flex items-center justify-between gap-2">
                      <span class="text-[11px] font-semibold text-cafe/55">
                        {{ indice === 0 ? 'Portada' : `Galería ${indice}` }}
                      </span>
                      <div class="flex items-center gap-1">
                        <button
                          type="button"
                          class="rounded px-1.5 py-0.5 text-xs text-cafe/60 transition hover:bg-rosa/20 disabled:opacity-30"
                          :disabled="indice === 0"
                          aria-label="Mover imagen a la izquierda"
                          @click.stop="moverImagen(indice, -1)"
                        >
                          ←
                        </button>
                        <button
                          type="button"
                          class="rounded px-1.5 py-0.5 text-xs text-cafe/60 transition hover:bg-rosa/20 disabled:opacity-30"
                          :disabled="indice === form.imagenes.length - 1"
                          aria-label="Mover imagen a la derecha"
                          @click.stop="moverImagen(indice, 1)"
                        >
                          →
                        </button>
                        <button
                          type="button"
                          class="rounded px-1.5 py-0.5 text-xs text-rojizo/80 transition hover:bg-rojizo/5 hover:text-rojizo"
                          @click.stop="quitarImagen(indice)"
                        >
                          ×
                        </button>
                      </div>
                    </div>
                    <button
                      v-if="indice !== 0"
                      type="button"
                      class="mt-1 text-[11px] font-medium text-coral transition hover:text-rojizo"
                      @click.stop="usarComoPortada(indice)"
                    >
                      Usar como portada
                    </button>
                  </div>
                </article>
              </div>

              <p v-else class="mt-3 text-center text-sm text-cafe/50">
                Todavía no subiste imágenes.
              </p>

              <p v-if="erroresCampo.imagenes" class="mt-2 text-xs font-medium text-rojizo">
                {{ erroresCampo.imagenes }}
              </p>
            </div>

            <div>
              <h3 class="font-display text-lg font-bold text-granate">Etiquetas</h3>
              <p class="mt-1 text-sm text-cafe/60">
                Hasta 5 etiquetas para que encuentren tu publicación.
              </p>
              <div class="mt-4">
                <SelectorEtiquetas v-model="etiquetas" />
              </div>
            </div>
          </section>

          <!-- Paso 2: precio y stock -->
          <section v-else-if="paso === 2" class="space-y-6">
            <div class="rounded-2xl border border-rosa/60 bg-crema/50 p-4">
              <div class="flex items-center gap-2.5">
                <label
                  for="moneda-publicacion"
                  class="shrink-0 whitespace-nowrap text-sm font-medium text-granate"
                >
                  Moneda<span class="hidden sm:inline"> de la publicación</span>
                  <span class="text-rojizo">*</span>
                </label>

                <div class="group relative inline-flex shrink-0">
                  <button
                    type="button"
                    class="flex h-5 w-5 items-center justify-center rounded-full border border-rosa/70 text-cafe/50 transition hover:bg-rosa/20 hover:text-granate"
                    aria-label="Información sobre la moneda de la publicación"
                    aria-describedby="moneda-ayuda"
                  >
                    <svg
                      class="h-3.5 w-3.5"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      aria-hidden="true"
                    >
                      <circle cx="12" cy="12" r="9" />
                      <path stroke-linecap="round" d="M12 11v5" />
                      <path stroke-linecap="round" d="M12 8h.01" />
                    </svg>
                  </button>
                  <span
                    id="moneda-ayuda"
                    role="tooltip"
                    class="pointer-events-none invisible absolute left-0 top-full z-20 mt-2 w-72 max-w-[calc(100vw-72px)] rounded-xl border border-rosa/60 bg-blanco p-3 text-left text-xs font-normal leading-relaxed text-cafe/70 opacity-0 shadow-xl transition group-hover:visible group-hover:opacity-100 group-focus-within:visible group-focus-within:opacity-100"
                  >
                    <span class="block">
                      Todos los precios de esta publicación se expresan en esta moneda.
                    </span>
                    <span v-if="esEdicion" class="mt-1.5 block">
                      La moneda no se puede cambiar después de publicar; si necesitás
                      otra, archivá esta publicación y creá una nueva.
                    </span>
                  </span>
                </div>

                <select
                  id="moneda-publicacion"
                  v-model="form.idMoneda"
                  :disabled="esEdicion"
                  class="ml-auto w-36 min-w-0 rounded-lg border-0 px-3 py-2 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral disabled:bg-cafe/5 disabled:text-cafe/50 sm:w-64"
                >
                  <option value="" disabled>Elegí una moneda</option>
                  <option
                    v-for="moneda in monedas"
                    :key="moneda.idMoneda"
                    :value="String(moneda.idMoneda)"
                  >
                    {{ moneda.nombre }} ({{ moneda.codigo }})
                  </option>
                </select>
              </div>
              <p v-if="erroresCampo.idMoneda" class="mt-1 text-xs font-medium text-rojizo">
                {{ erroresCampo.idMoneda }}
              </p>
            </div>

            <p v-if="erroresCampo.variantes" class="text-xs font-medium text-rojizo">
              {{ erroresCampo.variantes }}
            </p>

            <SelectorVariantes
              v-model="variantes"
              :moneda="monedaCodigo"
              :es-adoptable="esAdoptable"
              :imagen-portada="form.imagenes[0]?.url || ''"
              :imagenes-galeria="form.imagenes"
              :errores-variantes="erroresVariantes"
              @agregar-imagen="agregarImagenDesdeVariante"
            />
          </section>

          <!-- Paso 3: licencia -->
          <section v-else-if="paso === 3" class="space-y-6">
            <div class="rounded-2xl border border-rosa/60 bg-crema/50 p-5">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <h3 class="font-display text-lg font-bold text-granate">Usos permitidos</h3>
                <button
                  type="button"
                  class="text-xs font-medium text-coral transition hover:text-rojizo"
                  @click="modalReventaAbierto = true"
                >
                  ¿Cómo funciona la reventa?
                </button>
              </div>
              <p class="mt-1 text-sm text-cafe/60">
                Elegí qué puede hacer el comprador con la obra. Podés combinar más de uno.
              </p>

              <div class="mt-4 grid gap-3">
                <label
                  v-for="derecho in derechos"
                  :key="derecho.idDerechoTermino"
                  class="flex cursor-pointer items-start gap-3 rounded-xl border border-rosa/60 bg-blanco p-4 transition hover:border-coral/40"
                  :class="(derecho.requierePiezaUnica && !hayVarianteUnica
                    && !licencia.idsDerechos.includes(derecho.idDerechoTermino))
                    ? 'cursor-not-allowed opacity-60'
                    : ''"
                >
                  <input
                    v-model="licencia.idsDerechos"
                    type="checkbox"
                    :value="derecho.idDerechoTermino"
                    :disabled="derecho.requierePiezaUnica && !hayVarianteUnica
                      && !licencia.idsDerechos.includes(derecho.idDerechoTermino)"
                    class="mt-0.5 h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                  />
                  <span class="min-w-0">
                    <span class="flex flex-wrap items-center gap-2">
                      <span class="text-sm font-semibold text-granate">{{ derecho.nombre }}</span>
                      <span
                        v-if="derecho.requierePiezaUnica"
                        class="rounded-full bg-mantequilla/60 px-2 py-0.5 text-[10px] font-bold text-granate"
                      >
                        Solo piezas únicas
                      </span>
                    </span>
                    <span class="mt-1 block text-xs leading-relaxed text-cafe/60">
                      {{ derecho.descripcion }}
                    </span>
                    <span
                      v-if="derecho.requierePiezaUnica && !hayVarianteUnica"
                      class="mt-2 block text-[11px] font-medium text-granate"
                    >
                      Para habilitarlo, volvé al paso 2 y poné una presentación como “Pieza única”.
                    </span>
                  </span>
                </label>
              </div>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <label class="flex cursor-pointer items-start gap-3 rounded-2xl border border-rosa/60 bg-blanco p-5">
                <input
                  v-model="licencia.permiteModificaciones"
                  type="checkbox"
                  class="mt-0.5 h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                />
                <span>
                  <span class="block text-sm font-semibold text-granate">Permitir modificaciones</span>
                  <span class="mt-1 block text-xs leading-relaxed text-cafe/55">
                    El comprador puede editar, recortar o intervenir la obra.
                  </span>
                </span>
              </label>

              <label class="flex cursor-pointer items-start gap-3 rounded-2xl border border-rosa/60 bg-blanco p-5">
                <input
                  v-model="licencia.requiereCredito"
                  type="checkbox"
                  class="mt-0.5 h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                />
                <span>
                  <span class="block text-sm font-semibold text-granate">Requerir crédito</span>
                  <span class="mt-1 block text-xs leading-relaxed text-cafe/55">
                    El comprador debe mencionarte cuando use o comparta la obra.
                  </span>
                </span>
              </label>
            </div>

            <p v-if="erroresCampo.licencia" class="text-xs font-medium text-rojizo">
              {{ erroresCampo.licencia }}
            </p>

            <div v-if="derechoReventa" class="rounded-2xl border border-coral/30 bg-rosa/15 p-5">
              <label class="mb-2 block text-sm font-medium text-granate">
                Regalía por reventa (%)
              </label>
              <input
                v-model.number="licencia.porcentajeRegalia"
                type="number"
                min="0"
                max="10"
                step="0.01"
                class="w-40 rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/55">
                Porcentaje que recibís cada vez que se revende la pieza única.
                0% significa reventa sin regalía. Máximo 10%.
              </p>
              <p v-if="erroresCampo.porcentajeRegalia" class="mt-1 text-xs font-medium text-rojizo">
                {{ erroresCampo.porcentajeRegalia }}
              </p>
            </div>

            <div class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <label class="mb-2 block text-sm font-medium text-granate">
                Condiciones adicionales (opcional)
              </label>
              <textarea
                v-model="licencia.textoAdicional"
                rows="4"
                maxlength="2000"
                placeholder="Ej. La compra habilita el uso personal de la pieza. No permite redistribución digital."
                class="block w-full resize-none rounded-lg border border-rosa/60 bg-blanco px-4 py-2.5 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              ></textarea>
            </div>

            <div class="rounded-2xl border border-rosa/60 bg-crema/40 p-5">
              <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Resumen de la licencia
              </p>
              <p class="mt-2 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Usos:</span> {{ resumenUsos }}
              </p>
              <p class="mt-1 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Modificaciones:</span>
                {{ licencia.permiteModificaciones ? 'Permitidas' : 'No permitidas' }}
              </p>
              <p class="mt-1 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Crédito:</span>
                {{ licencia.requiereCredito ? 'Requerido' : 'No requerido' }}
              </p>
              <p class="mt-1 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Reventa:</span> {{ resumenReventa }}
              </p>
              <p v-if="licencia.textoAdicional.trim()" class="mt-3 whitespace-pre-wrap text-xs leading-relaxed text-cafe/60">
                {{ licencia.textoAdicional }}
              </p>
            </div>
          </section>

          <!-- Paso 4: entregables -->
          <section v-else-if="paso === 4" class="space-y-6">
            <div class="rounded-2xl border border-coral/25 bg-coral/5 p-4">
              <div class="flex items-start gap-3">
                <span
                  class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-coral/15 text-coral"
                  aria-hidden="true"
                >
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v12m0 0 4-4m-4 4-4-4M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
                  </svg>
                </span>
                <div class="min-w-0">
                  <p class="text-sm font-semibold text-granate">Qué recibe el comprador</p>
                  <p class="mt-1 text-xs leading-relaxed text-cafe/65">
                    El archivo que se entrega al acreditarse el pago. Por defecto se usa
                    la imagen original de cada variante; podés elegir otra imagen de la
                    galería si querés entregar algo distinto de la vidriera.
                  </p>
                </div>
              </div>
            </div>

            <p v-if="erroresCampo.entregables" class="text-xs font-medium text-rojizo">
              {{ erroresCampo.entregables }}
            </p>

            <div class="space-y-4">
              <article
                v-for="(variante, indice) in variantes"
                :key="variante.idVariante || `v-${indice}`"
                class="rounded-2xl border border-rosa/60 bg-blanco p-4"
              >
                <div class="flex items-start gap-3">
                  <img
                    v-if="imagenVariante(variante)"
                    :src="imagenVariante(variante)"
                    :alt="nombrePresentacion(variante)"
                    class="h-16 w-16 shrink-0 rounded-xl object-cover ring-1 ring-rosa/50"
                  />
                  <div class="min-w-0 flex-1">
                    <p class="text-sm font-semibold text-granate">
                      {{ nombrePresentacion(variante) }}
                    </p>
                    <p class="mt-0.5 text-xs text-cafe/55">
                      {{ variante.entregableSha256
                        ? 'Entrega la imagen elegida.'
                        : 'Entrega la imagen original de esta presentación.' }}
                    </p>
                    <div class="mt-3 flex flex-wrap items-center gap-2">
                      <button
                        type="button"
                        class="rounded-lg border border-coral/40 bg-coral/10 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-coral/20 disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="!variante.sha256"
                        @click="usarImagenComoEntregable(variante)"
                      >
                        Usar la imagen de la variante
                      </button>
                      <select
                        class="min-w-0 rounded-lg border-0 px-2.5 py-1.5 text-xs text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                        :value="variante.entregableSha256"
                        @change="elegirEntregableVariante($event, variante)"
                      >
                        <option value="">Elegir otra imagen…</option>
                        <option
                          v-for="imagen in form.imagenes"
                          :key="imagen.sha256"
                          :value="imagen.sha256"
                        >
                          {{ (imagen.url || '').split('/').pop() }}
                        </option>
                      </select>
                    </div>
                    <p
                      v-if="erroresEntregables[indice]"
                      class="mt-2 text-xs font-medium text-rojizo"
                    >
                      {{ erroresEntregables[indice] }}
                    </p>
                  </div>
                </div>
              </article>
            </div>

            <div v-if="!presentacionUnica" class="rounded-2xl border border-rosa/60 bg-crema/40 p-4">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-sm font-semibold text-granate">
                    Archivos para todos los compradores
                    <span class="ml-1 text-xs font-normal text-cafe/50">(opcional)</span>
                  </p>
                  <p class="mt-1 text-xs leading-relaxed text-cafe/60">
                    Se entregan además de la variante, a cualquier comprador. Por
                    ejemplo, el manual o la licencia en PDF.
                  </p>
                </div>
                <button
                  type="button"
                  class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-medium text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
                  :disabled="!form.imagenes.length"
                  @click="agregarEntregableComun"
                >
                  Agregar archivo
                </button>
              </div>

              <ul v-if="entregablesComunes.length" class="mt-3 space-y-2">
                <li
                  v-for="(entregable, indice) in entregablesComunes"
                  :key="`comun-${indice}`"
                  class="flex flex-wrap items-center gap-2 rounded-xl border border-rosa/50 bg-blanco px-3 py-2"
                >
                  <select
                    class="min-w-0 flex-1 rounded-lg border-0 px-2.5 py-1.5 text-xs text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                    :value="entregable.sha256"
                    @change="cambiarEntregableComun($event, entregable)"
                  >
                    <option value="">Elegí una imagen…</option>
                    <option
                      v-for="imagen in form.imagenes"
                      :key="imagen.sha256"
                      :value="imagen.sha256"
                    >
                      {{ (imagen.url || '').split('/').pop() }}
                    </option>
                  </select>
                  <button
                    type="button"
                    class="rounded-lg px-2 py-1 text-xs font-medium text-rojizo/80 transition hover:text-rojizo"
                    @click="quitarEntregableComun(indice)"
                  >
                    Quitar
                  </button>
                  <p
                    v-if="erroresEntregables[`comun-${indice}`]"
                    class="w-full text-xs font-medium text-rojizo"
                  >
                    {{ erroresEntregables[`comun-${indice}`] }}
                  </p>
                </li>
              </ul>
              <p v-else class="mt-3 text-xs text-cafe/45">
                Sin archivos comunes: cada comprador recibe solo su variante.
              </p>
            </div>

            <p class="rounded-xl bg-crema/60 px-4 py-3 text-xs leading-relaxed text-cafe/65">
              <span class="font-semibold text-granate">Se entrega el archivo original.</span>
              La vidriera pública usa una copia optimizada; el comprador recibe el
              archivo tal como lo subiste.
            </p>
          </section>

          <!-- Paso 5: revisión -->
          <section v-else-if="paso === 5" class="space-y-5">
            <article class="rounded-2xl border border-rosa/60 bg-crema/40 p-5">
              <div class="flex items-start justify-between gap-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Publicación
                </p>
                <button
                  type="button"
                  class="text-xs font-medium text-coral transition hover:text-rojizo"
                  @click="irAEditar(1)"
                >
                  Editar
                </button>
              </div>
              <h3 class="mt-2 font-display text-lg font-bold text-granate">
                {{ form.titulo || 'Sin título' }}
              </h3>
              <p class="mt-1 text-xs text-cafe/55">
                {{ nombreTipo || 'Sin tipo' }} · {{ nombreMedio || 'Sin medio' }} · {{ monedaCodigo }}
              </p>
              <p
                v-if="esEntregaFisica"
                class="mt-2 rounded-lg border border-coral/20 bg-rosa/20 px-3 py-2 text-xs leading-relaxed text-granate"
              >
                Entrega física: Temipora no gestiona envíos. El envío y la entrega
                se coordinan directamente entre artista y comprador.
              </p>
              <p v-else class="mt-2 text-xs text-cafe/45">
                Entrega digital.
              </p>
              <p class="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-cafe/75">
                {{ form.descripcion }}
              </p>
              <p v-if="form.contenidoSensible" class="mt-2 text-xs font-medium text-granate">
                Contenido sensible: {{ form.contenidosSensibles.join(' · ') }}
              </p>
            </article>

            <article class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <div class="flex items-start justify-between gap-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Precio y stock
                </p>
                <button
                  type="button"
                  class="text-xs font-medium text-coral transition hover:text-rojizo"
                  @click="irAEditar(2)"
                >
                  Editar
                </button>
              </div>
              <p class="mt-2 text-sm text-cafe/65">
                {{ variantes.length === 1
                  ? '1 presentación'
                  : `${variantes.length} variantes` }}
                · desde
                <span class="font-semibold text-granate">
                  {{ precioDesde !== null ? formatearPrecio(precioDesde, monedaCodigo) : 'sin precio' }}
                </span>
              </p>
              <div class="mt-3 divide-y divide-rosa/40">
                <div
                  v-for="(variante, indice) in variantes"
                  :key="variante.idVariante || indice"
                  class="flex items-center gap-3 py-3"
                >
                  <img
                    v-if="imagenVariante(variante)"
                    :src="imagenVariante(variante)"
                    :alt="nombrePresentacion(variante)"
                    class="h-12 w-12 shrink-0 rounded-lg object-cover ring-1 ring-rosa/50"
                  />
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-semibold text-granate">
                      {{ nombrePresentacion(variante) }}
                    </p>
                    <p class="text-xs text-cafe/55">
                      {{ formatearPrecio(Number(variante.precio), monedaCodigo) }}
                      · {{ etiquetaStock(variante) }}
                    </p>
                  </div>
                  <span
                    v-if="!esIlimitada(variante) && Number(variante.stockDisponible) === 1"
                    class="shrink-0 rounded-full bg-mantequilla/60 px-2.5 py-0.5 text-[11px] font-bold text-granate"
                    title="Al publicar se registra el origen de la pieza única"
                  >
                    Se registra al publicar
                  </span>
                </div>
              </div>
            </article>

            <article class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <div class="flex items-start justify-between gap-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Licencia
                </p>
                <button
                  type="button"
                  class="text-xs font-medium text-coral transition hover:text-rojizo"
                  @click="irAEditar(3)"
                >
                  Editar
                </button>
              </div>
              <p class="mt-2 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Usos:</span> {{ resumenUsos }}
              </p>
              <p class="mt-1 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Modificaciones:</span>
                {{ licencia.permiteModificaciones ? 'Permitidas' : 'No permitidas' }}
              </p>
              <p class="mt-1 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Crédito:</span>
                {{ licencia.requiereCredito ? 'Requerido' : 'No requerido' }}
              </p>
              <p class="mt-1 text-sm text-cafe/75">
                <span class="font-semibold text-granate">Reventa:</span> {{ resumenReventa }}
              </p>
              <p v-if="licencia.textoAdicional.trim()" class="mt-3 whitespace-pre-wrap text-xs leading-relaxed text-cafe/60">
                {{ licencia.textoAdicional }}
              </p>
            </article>

            <article class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <div class="flex items-start justify-between gap-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Imágenes y etiquetas
                </p>
                <button
                  type="button"
                  class="text-xs font-medium text-coral transition hover:text-rojizo"
                  @click="irAEditar(1)"
                >
                  Editar
                </button>
              </div>
              <div class="mt-3 flex flex-wrap gap-3">
                <img
                  v-for="(imagen, indice) in form.imagenes"
                  :key="`${imagen.url}-${indice}`"
                  :src="imagen.url"
                  :alt="`Imagen ${indice + 1}`"
                  class="h-20 w-20 rounded-xl object-cover ring-1 ring-rosa/50"
                />
              </div>
              <div class="mt-4 flex flex-wrap gap-2">
                <span
                  v-for="etiqueta in etiquetas"
                  :key="etiqueta.idEtiqueta"
                  class="rounded-full bg-durazno/40 px-3 py-1 text-xs text-cafe"
                >
                  {{ etiqueta.nombre }}
                </span>
                <span v-if="!etiquetas.length" class="text-sm text-cafe/50">
                  Sin etiquetas.
                </span>
              </div>
            </article>

            <article v-if="!esEntregaFisica" class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <div class="flex items-start justify-between gap-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Entregables
                </p>
                <button
                  type="button"
                  class="text-xs font-medium text-coral transition hover:text-rojizo"
                  @click="irAEditar(4)"
                >
                  Editar
                </button>
              </div>
              <p class="mt-2 text-sm text-cafe/65">
                {{ entregablesComunes.length
                  ? `Cada comprador recibe su variante más ${entregablesComunes.length} archivo(s) común(es).`
                  : 'Cada comprador recibe el archivo de su variante.' }}
              </p>
              <div class="mt-3 divide-y divide-rosa/40">
                <div
                  v-for="(variante, indice) in variantes"
                  :key="`entrega-${variante.idVariante || indice}`"
                  class="flex items-center gap-3 py-2.5"
                >
                  <img
                    v-if="imagenVariante(variante)"
                    :src="imagenVariante(variante)"
                    :alt="nombrePresentacion(variante)"
                    class="h-10 w-10 shrink-0 rounded-lg object-cover ring-1 ring-rosa/50"
                  />
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-semibold text-granate">
                      {{ nombrePresentacion(variante) }}
                    </p>
                    <p class="text-xs text-cafe/55">
                      {{ variante.entregableSha256
                        ? 'Archivo elegido'
                        : 'Imagen original de la variante' }}
                    </p>
                  </div>
                  <span
                    class="shrink-0 rounded-full bg-mantequilla/60 px-2.5 py-0.5 text-[11px] font-bold text-granate"
                  >
                    Se entrega al pagar
                  </span>
                </div>
              </div>
            </article>

            <p
              v-if="esEntregaFisica"
              class="rounded-2xl border border-coral/20 bg-rosa/20 px-4 py-3 text-xs leading-relaxed text-granate"
            >
              Producto con entrega física: no hay archivos que entregar por la
              plataforma. El envío se coordina con el comprador.
            </p>

            <p
              v-if="avisoBorrador"
              class="rounded-xl border border-green-600/20 bg-green-500/10 px-4 py-3 text-sm text-green-700"
              role="status"
            >
              {{ avisoBorrador }}
            </p>

            <p
              v-if="errorGuardado"
              class="rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
            >
              {{ errorGuardado }}
            </p>
          </section>

          <footer class="mt-10 flex flex-col-reverse gap-3 border-t border-rosa/50 pt-6 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex flex-wrap items-center gap-3">
              <button
                v-if="indicePaso > 1"
                type="button"
                class="rounded-lg border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
                @click="anterior"
              >
                Anterior
              </button>
              <button
                v-if="esBorrador"
                type="button"
                :disabled="guardandoBorrador || guardando"
                class="rounded-lg border border-rosa px-4 py-2 text-sm font-medium text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
                @click="guardarBorrador"
              >
                {{ guardandoBorrador ? 'Guardando…' : 'Guardar borrador' }}
              </button>
            </div>

            <div class="flex items-center gap-3">
              <button
                v-if="paso !== 5"
                type="button"
                class="rounded-lg bg-coral px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo"
                @click="siguiente"
              >
                Siguiente
              </button>
              <button
                v-else
                type="button"
                :disabled="guardando"
                class="rounded-lg bg-coral px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo disabled:opacity-50"
                @click="publicar"
              >
                {{ guardando
                  ? 'Publicando...'
                  : (esBorrador ? 'Publicar producto' : 'Guardar cambios') }}
              </button>
            </div>
          </footer>
        </div>
      </div>
    </template>

    <VisorImagenes
      :abierto="visorAbierto"
      :imagenes="imagenesUrl"
      :indice="visorIndice"
      etiqueta="Imágenes del producto"
      @cerrar="visorAbierto = false"
      @actualizar:indice="visorIndice = $event"
    />

    <Teleport to="body">
      <div
        v-if="modalReventaAbierto"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-label="Cómo funciona la reventa"
      >
        <div
          class="absolute inset-0 bg-granate/50"
          aria-hidden="true"
          @click="modalReventaAbierto = false"
        ></div>
        <div class="relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-blanco p-6 shadow-xl">
          <header class="flex items-start justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Licencia
              </p>
              <h2 class="mt-1 font-display text-xl font-bold text-granate">
                ¿Cómo funciona la reventa?
              </h2>
            </div>
            <button
              type="button"
              class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20"
              @click="modalReventaAbierto = false"
            >
              Cerrar
            </button>
          </header>

          <div class="mt-5 space-y-4 text-sm leading-relaxed text-cafe/75">
            <p>
              Permitir la reventa significa que quien compre una pieza única puede
              transferirla a otra persona dentro de Temipora. Vos seguís siendo el
              autor: no se transfiere el derecho de autor.
            </p>

            <div>
              <p class="font-semibold text-granate">Solo para piezas únicas</p>
              <p class="mt-1">
                Solo se aplica a variantes con stock 1. Las variantes ilimitadas o
                con varias unidades no se pueden revender.
              </p>
            </div>

            <div>
              <p class="font-semibold text-granate">Qué pasa cuando se revende</p>
              <ul class="mt-1 list-disc space-y-1 pl-5">
                <li>El comprador actual transfiere la pieza a otra persona.</li>
                <li>El nuevo comprador queda como propietario y acepta la licencia vigente.</li>
                <li>El historial de autenticidad registra la transferencia y conserva el origen.</li>
                <li>La regalía se calcula sobre el precio de reventa y queda registrada en la operación.</li>
              </ul>
            </div>

            <div>
              <p class="font-semibold text-granate">Regalía</p>
              <p class="mt-1">
                Podés elegir entre 0% y 10% sobre el precio de reventa. Si elegís
                0%, la reventa se permite sin regalía.
              </p>
            </div>

            <div>
              <p class="font-semibold text-granate">Qué no permite</p>
              <p class="mt-1">
                No permite vender copias, redistribuir la obra ni transferir tus
                derechos de autor. Tampoco se aplica a variantes que no sean piezas
                únicas.
              </p>
            </div>

            <p class="rounded-xl bg-crema/70 px-4 py-3 text-xs text-cafe/65">
              Si no habilitás la reventa, la pieza no podrá transferirse a otra
              persona dentro de Temipora.
            </p>
          </div>

          <div class="mt-6 flex justify-end">
            <button
              type="button"
              class="rounded-lg bg-coral px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo"
              @click="modalReventaAbierto = false"
            >
              Entendido
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
