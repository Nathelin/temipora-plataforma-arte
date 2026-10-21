<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, validarImagen, procesarImagen, LIMITES_IMAGEN } from '../servicios/archivos'
import { formatearPrecio } from '../servicios/moneda'
import { useMonedas } from '../composables/useMonedas'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import SelectorPortfolio from '../components/SelectorPortfolio.vue'
import SelectorEtiquetas from '../components/SelectorEtiquetas.vue'
import EditorFormularioPedidos from '../components/EditorFormularioPedidos.vue'
import EditorTerminos from '../components/EditorTerminos.vue'
import VistaPreviaFormulario from '../components/VistaPreviaFormulario.vue'
import VistaPreviaTerminos from '../components/VistaPreviaTerminos.vue'

const route = useRoute()
const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const idServicio = computed(() => (route.params.id ? Number(route.params.id) : null))
const esEdicion = computed(() => idServicio.value !== null)

const PASOS = [
  {
    n: 1,
    label: 'Datos',
    titulo: 'Datos básicos',
    descripcion: 'Contanos qué ofrecés, cuánto cuesta y en qué medio.',
  },
  {
    n: 2,
    label: 'Entrega',
    titulo: 'Entrega y clasificación',
    descripcion: 'Definí plazos, cupos, etiquetas y si hay contenido sensible.',
  },
  {
    n: 3,
    label: 'Imágenes',
    titulo: 'Imágenes',
    descripcion: 'Elegí una portada atractiva y algunas imágenes de ejemplo.',
  },
  {
    n: 4,
    label: 'Formulario',
    titulo: 'Formulario de pedidos',
    descripcion: 'Decidí si usás tu formulario global o uno propio para este servicio.',
  },
  {
    n: 5,
    label: 'Términos',
    titulo: 'Términos y condiciones',
    descripcion: 'Definí si usás tus términos globales o condiciones específicas.',
  },
  {
    n: 6,
    label: 'Revisión',
    titulo: 'Revisión y publicación',
    descripcion: 'Revisá toda la información antes de publicar.',
  },
]
const TOTAL_PASOS = PASOS.length

const paso = ref(1)
const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const errorGuardado = ref('')
const errores = ref([])
const subiendoPortada = ref(false)
const subiendoEjemplo = ref(false)
const estadoGuardado = ref(null)

const selectorAbierto = ref(false)
const selectorModo = ref('ejemplos')
const arrastrandoPortada = ref(false)
const arrastrandoEjemplos = ref(false)
const indiceImagenArrastrada = ref(null)

const tipos = ref([])
const medios = ref([])
const tiposCampo = ref([])
const temasCatalogo = ref([])
const derechosCatalogo = ref([])
const idUsuario = ref(null)
const { monedas, cargar: cargarMonedas, predeterminada } = useMonedas()

const UNIDADES = [
  { valor: 'DIAS', texto: 'Días' },
  { valor: 'SEMANAS', texto: 'Semanas' },
  { valor: 'MESES', texto: 'Meses' },
]

const SENSIBLES = [
  { valor: 'VIOLENCIA', texto: 'Violencia' },
  { valor: 'DESNUDOS', texto: 'Desnudos' },
  { valor: 'GENERAL', texto: 'Contenido sensible general' },
]

const SECCIONES_TERMINOS = [
  'informacionGeneral',
  'politicaRevisiones',
  'politicaCancelaciones',
  'usoArtista',
  'usoCliente',
]

const form = ref({
  nombre: '',
  idTipoServicio: '',
  idMedio: '',
  descripcion: '',
  precioBase: '',
  idMoneda: '',
  contenidoSensible: false,
  contenidosSensibles: [],
  tiempoEntregaValor: '',
  tiempoEntregaUnidad: 'DIAS',
  cupos: '',
  habilitarCuposTrasEntrega: false,
  imagenPortada: '',
  imagenes: [],
})

const etiquetas = ref([])

const personalizarFormulario = ref(false)
const personalizarTerminos = ref(false)
const formularioInicialPropio = ref(false)
const terminosInicialPropio = ref(false)
const configFormulario = ref({ preguntas: [] })
const configTerminos = ref(crearTerminosVacios())
const configFormularioGlobal = ref({ preguntas: [] })
const configTerminosGlobal = ref(crearTerminosVacios())

const modoFormulario = ref('editar')
const modoTerminos = ref('editar')
const verFormularioGlobal = ref(true)
const verTerminosGlobal = ref(true)
const verFormularioRevision = ref(false)
const verTerminosRevision = ref(false)

const pasoActual = computed(() => PASOS.find((p) => p.n === paso.value) || PASOS[0])
const formularioVisible = computed(
  () => (personalizarFormulario.value ? configFormulario.value : configFormularioGlobal.value),
)
const terminosVisible = computed(
  () => (personalizarTerminos.value ? configTerminos.value : configTerminosGlobal.value),
)
const monedaSeleccionada = computed(
  () => monedas.value.find((m) => String(m.idMoneda) === form.value.idMoneda) || null,
)

const tituloPagina = computed(() => (esEdicion.value ? 'Editar servicio' : 'Crear servicio'))
const subtituloPagina = computed(() => 'Completá los datos de tu servicio paso a paso.')

const tipoSeleccionado = computed(() => tipos.value.find((t) => String(t.idTipoServicio) === form.value.idTipoServicio) || null)
const nombreTipo = computed(() => tipoSeleccionado.value?.nombre || '')
const descripcionTipo = computed(() => tipoSeleccionado.value?.descripcion || '')
const nombreMedio = computed(() => medios.value.find((m) => String(m.idMedio) === form.value.idMedio)?.nombre || '')
const resumenTerminos = computed(() => {
  const f = terminosVisible.value.form
  return f.informacionGeneral
    || f.politicaRevisiones
    || f.politicaCancelaciones
    || f.usoArtista
    || f.usoCliente
    || f.tiempoEntrega
    || 'Sin texto general; se usan los temas y derechos definidos.'
})

function crearTerminosVacios() {
  return {
    form: {
      informacionGeneral: '',
      politicaRevisiones: '',
      politicaCancelaciones: '',
      usoArtista: '',
      usoCliente: '',
      pagoExtra: '',
      tiempoEntrega: '',
    },
    estadosTemas: {},
    temasPersonalizados: [],
    estadoDerechos: {},
  }
}

function mapearPreguntas(data) {
  return (data.preguntas || []).map((p, i) => ({
    uid: i + 1,
    idTipoCampo: p.idTipoCampo,
    codigoTipo: p.codigoTipo,
    enunciado: p.enunciado,
    ayuda: p.ayuda || '',
    obligatoria: !!p.obligatoria,
    opciones: Array.isArray(p.opciones) ? [...p.opciones] : [],
  }))
}

function construirTerminos(data) {
  const resultado = crearTerminosVacios()
  resultado.form = {
    informacionGeneral: data.informacionGeneral || '',
    politicaRevisiones: data.politicaRevisiones || '',
    politicaCancelaciones: data.politicaCancelaciones || '',
    usoArtista: data.usoArtista || '',
    usoCliente: data.usoCliente || '',
    pagoExtra: data.pagoExtra || '',
    tiempoEntrega: data.tiempoEntrega || '',
  }

  const temasGuardados = data.temas || []
  for (const t of temasCatalogo.value) {
    const encontrado = temasGuardados.find((x) => !x.personalizado && x.idTemaTermino === t.idTemaTermino)
    resultado.estadosTemas[t.idTemaTermino] = encontrado?.estado || ''
  }
  resultado.temasPersonalizados = temasGuardados
    .filter((x) => x.personalizado)
    .map((x, i) => ({ uid: i + 1, nombre: x.nombre || '', estado: x.estado || '' }))

  const derechosGuardados = data.derechos || []
  for (const d of derechosCatalogo.value) {
    const encontrado = derechosGuardados.find((x) => x.idDerechoTermino === d.idDerechoTermino)
    resultado.estadoDerechos[d.idDerechoTermino] = {
      disponible: d.codigo === 'PERSONAL' ? true : !!encontrado?.disponible,
      porcentajeExtra: encontrado?.porcentajeExtra ?? null,
    }
  }
  return resultado
}

function instantanea() {
  return JSON.stringify({
    form: form.value,
    etiquetas: etiquetas.value,
    personalizarFormulario: personalizarFormulario.value,
    personalizarTerminos: personalizarTerminos.value,
    formulario: configFormulario.value,
    terminos: configTerminos.value,
  })
}

const hayCambios = computed(() => estadoGuardado.value !== null && estadoGuardado.value !== instantanea())

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [tiposRes, mediosRes, tiposCampoRes, temasRes, derechosRes, perfilRes] = await Promise.all([
      api.get('/tipos-servicio'),
      api.get('/medios'),
      api.get('/tipos-campo'),
      api.get('/temas-termino'),
      api.get('/derechos-termino'),
      api.get(`/perfil/${nombreUsuario}`),
      cargarMonedas(),
    ])
    tipos.value = tiposRes.data
    medios.value = mediosRes.data
    tiposCampo.value = tiposCampoRes.data
    temasCatalogo.value = temasRes.data
    derechosCatalogo.value = derechosRes.data
    idUsuario.value = perfilRes.data.idUsuario

    const [formularioGlobalRes, terminosGlobalRes] = await Promise.all([
      api.get(`/usuarios/${idUsuario.value}/formulario-pedidos`),
      api.get(`/usuarios/${idUsuario.value}/terminos`),
    ])
    configFormularioGlobal.value = { preguntas: mapearPreguntas(formularioGlobalRes.data) }
    configTerminosGlobal.value = construirTerminos(terminosGlobalRes.data)

    if (esEdicion.value) {
      const [servicioRes, formularioRes, terminosRes] = await Promise.all([
        api.get(`/servicios/${idServicio.value}`),
        api.get(`/servicios/${idServicio.value}/formulario-pedidos`),
        api.get(`/servicios/${idServicio.value}/terminos`),
      ])
      const d = servicioRes.data
      form.value = {
        nombre: d.nombre || '',
        idTipoServicio: d.idTipoServicio ? String(d.idTipoServicio) : '',
        idMedio: d.idMedio ? String(d.idMedio) : '',
        descripcion: d.descripcion || '',
        precioBase: d.precioBase ?? '',
        idMoneda: d.idMoneda ? String(d.idMoneda) : '',
        contenidoSensible: !!d.contenidoSensible,
        contenidosSensibles: Array.isArray(d.contenidosSensibles) ? [...d.contenidosSensibles] : [],
        tiempoEntregaValor: d.tiempoEntregaValor ?? '',
        tiempoEntregaUnidad: d.tiempoEntregaUnidad || 'DIAS',
        cupos: d.cupos ?? '',
        habilitarCuposTrasEntrega: !!d.habilitarCuposTrasEntrega,
        imagenPortada: d.imagenPortada || '',
        imagenes: Array.isArray(d.imagenes) ? [...d.imagenes] : [],
      }
      etiquetas.value = Array.isArray(d.etiquetas) ? [...d.etiquetas] : []

      formularioInicialPropio.value = !!formularioRes.data.propio
      personalizarFormulario.value = !!formularioRes.data.propio
      configFormulario.value = { preguntas: mapearPreguntas(formularioRes.data) }

      terminosInicialPropio.value = !!terminosRes.data.propio
      personalizarTerminos.value = !!terminosRes.data.propio
      configTerminos.value = construirTerminos(terminosRes.data)
    } else {
      const pred = predeterminada()
      form.value.idMoneda = pred ? String(pred.idMoneda) : ''
      formularioInicialPropio.value = false
      terminosInicialPropio.value = false
      personalizarFormulario.value = false
      personalizarTerminos.value = false
      configFormulario.value = { preguntas: mapearPreguntas(formularioGlobalRes.data) }
      configTerminos.value = construirTerminos(terminosGlobalRes.data)
    }
    estadoGuardado.value = instantanea()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el servicio.'
  } finally {
    cargando.value = false
  }
}

async function subirArchivo(file) {
  const optimizado = await procesarImagen(file, LIMITES_IMAGEN.banner)
  const fd = new FormData()
  fd.append('archivo', optimizado)
  const res = await api.post('/upload', fd)
  return res.data.url
}

async function subirPortada(file) {
  const problema = validarImagen(file)
  if (problema) {
    errorGuardado.value = problema
    return
  }
  subiendoPortada.value = true
  errorGuardado.value = ''
  try {
    form.value.imagenPortada = await subirArchivo(file)
  } catch {
    errorGuardado.value = 'No se pudo subir la imagen de portada.'
  } finally {
    subiendoPortada.value = false
  }
}

async function onPortadaChange(e) {
  const file = e.target.files[0] || null
  e.target.value = ''
  if (file) await subirPortada(file)
}

async function onPortadaDrop(e) {
  arrastrandoPortada.value = false
  const file = e.dataTransfer?.files?.[0] || null
  if (file) await subirPortada(file)
}

async function agregarEjemplos(files) {
  if (!files.length) return
  subiendoEjemplo.value = true
  errorGuardado.value = ''
  try {
    for (const file of files) {
      const problema = validarImagen(file)
      if (problema) {
        errorGuardado.value = problema
        continue
      }
      form.value.imagenes.push(await subirArchivo(file))
    }
  } catch {
    errorGuardado.value = 'No se pudo subir una de las imágenes de ejemplo.'
  } finally {
    subiendoEjemplo.value = false
  }
}

async function onEjemplosChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  await agregarEjemplos(files)
}

async function onEjemplosDrop(e) {
  arrastrandoEjemplos.value = false
  await agregarEjemplos(Array.from(e.dataTransfer?.files || []))
}

function quitarEjemplo(indice) {
  form.value.imagenes.splice(indice, 1)
}

function onDragStartImagen(indice) {
  indiceImagenArrastrada.value = indice
}

function onDragOverImagen(indice) {
  if (indiceImagenArrastrada.value === null || indiceImagenArrastrada.value === indice) return
  const movida = form.value.imagenes.splice(indiceImagenArrastrada.value, 1)[0]
  form.value.imagenes.splice(indice, 0, movida)
  indiceImagenArrastrada.value = indice
}

function onDragEndImagen() {
  indiceImagenArrastrada.value = null
}

function abrirSelectorPortfolio(modo) {
  selectorModo.value = modo
  selectorAbierto.value = true
}

function cerrarSelectorPortfolio() {
  selectorAbierto.value = false
}

function onSeleccionPortfolio(urls) {
  if (selectorModo.value === 'portada') {
    if (urls[0]) form.value.imagenPortada = urls[0]
  } else {
    for (const url of urls) {
      if (!form.value.imagenes.includes(url)) form.value.imagenes.push(url)
    }
  }
  selectorAbierto.value = false
}

function elegirFormulario(personalizado) {
  personalizarFormulario.value = personalizado
  if (personalizado) modoFormulario.value = 'editar'
  else verFormularioGlobal.value = true
}

function elegirTerminos(personalizado) {
  personalizarTerminos.value = personalizado
  if (personalizado) modoTerminos.value = 'editar'
  else verTerminosGlobal.value = true
}

function validarDatosBasicos() {
  const errs = []
  if (!form.value.nombre.trim()) errs.push('El nombre es obligatorio.')
  if (!form.value.idTipoServicio) errs.push('Elegí un tipo de servicio.')
  if (!form.value.idMedio) errs.push('Elegí un medio.')
  if (!form.value.descripcion.trim()) errs.push('La descripción es obligatoria.')
  if (form.value.precioBase === '' || Number(form.value.precioBase) <= 0) errs.push('Ingresá un precio base mayor a cero.')
  if (!form.value.idMoneda) errs.push('Elegí una moneda.')
  return errs
}

function validarEntrega() {
  const errs = []
  if (!form.value.tiempoEntregaValor || Number(form.value.tiempoEntregaValor) < 1) {
    errs.push('Ingresá el tiempo estimado de entrega.')
  }
  if (form.value.cupos === '' || Number(form.value.cupos) < 0) {
    errs.push('Ingresá la cantidad de cupos.')
  }
  if (form.value.contenidoSensible && !form.value.contenidosSensibles.length) {
    errs.push('Marcá el servicio como sensible y elegí qué tipo de contenido puede aparecer.')
  }
  return errs
}

function validarImagenesPaso() {
  const errs = []
  if (!form.value.imagenPortada) errs.push('Subí o elegí una imagen de portada.')
  if (!form.value.imagenes.length) errs.push('Agregá al menos una imagen de ejemplo.')
  return errs
}

function validarFormulario() {
  if (!personalizarFormulario.value) return []
  const errs = []
  if (!configFormulario.value.preguntas.length) {
    errs.push('El formulario personalizado debe tener al menos una pregunta.')
  } else if (!configFormulario.value.preguntas.some((p) => p.codigoTipo !== 'ENCABEZADO')) {
    errs.push('Agregá al menos una pregunta que el cliente pueda completar.')
  }
  return errs
}

function hayContenidoTerminos() {
  const t = configTerminos.value
  const hayTexto = [...SECCIONES_TERMINOS, 'tiempoEntrega']
    .some((k) => (t.form[k] || '').trim())
  const hayTemas = Object.values(t.estadosTemas).some((v) => v)
    || t.temasPersonalizados.some((x) => x.nombre.trim() && x.estado)
  const hayDerechos = derechosCatalogo.value
    .some((d) => d.codigo !== 'PERSONAL' && t.estadoDerechos[d.idDerechoTermino]?.disponible)
  return hayTexto || hayTemas || hayDerechos
}

function validarTerminos() {
  if (!personalizarTerminos.value) return []
  return hayContenidoTerminos()
    ? []
    : ['Completá al menos una sección de los términos personalizados.']
}

function validarPasoActual() {
  let problemas = []
  if (paso.value === 1) problemas = validarDatosBasicos()
  else if (paso.value === 2) problemas = validarEntrega()
  else if (paso.value === 3) problemas = validarImagenesPaso()
  else if (paso.value === 4) problemas = validarFormulario()
  else if (paso.value === 5) problemas = validarTerminos()

  errores.value = problemas
  return problemas.length === 0
}

function irAlPrincipio() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function siguiente() {
  if (!validarPasoActual()) return
  if (paso.value < TOTAL_PASOS) {
    paso.value++
    irAlPrincipio()
  }
}

function anterior() {
  errores.value = []
  if (paso.value > 1) {
    paso.value--
    irAlPrincipio()
  }
}

function irAPaso(n) {
  if (n < paso.value) {
    errores.value = []
    paso.value = n
    irAlPrincipio()
  }
}

function irAEditar(n) {
  errores.value = []
  errorGuardado.value = ''
  paso.value = n
  irAlPrincipio()
}

function porcentajeExtraDe(derecho, estado) {
  if (derecho.codigo === 'PERSONAL') return null
  const valor = estado?.porcentajeExtra
  if (valor === '' || valor === null || valor === undefined) return null
  const numero = Number(valor)
  return Number.isNaN(numero) ? null : numero
}

function payloadTerminos() {
  const t = configTerminos.value
  return {
    informacionGeneral: t.form.informacionGeneral || null,
    politicaRevisiones: t.form.politicaRevisiones || null,
    politicaCancelaciones: t.form.politicaCancelaciones || null,
    usoArtista: t.form.usoArtista || null,
    usoCliente: t.form.usoCliente || null,
    pagoExtra: t.form.pagoExtra || null,
    tiempoEntrega: t.form.tiempoEntrega || null,
    temas: [
      ...temasCatalogo.value
        .filter((tema) => t.estadosTemas[tema.idTemaTermino])
        .map((tema) => ({ idTemaTermino: tema.idTemaTermino, estado: t.estadosTemas[tema.idTemaTermino] })),
      ...t.temasPersonalizados
        .filter((tema) => tema.nombre.trim() && tema.estado)
        .map((tema) => ({ nombre: tema.nombre.trim(), estado: tema.estado })),
    ],
    derechos: derechosCatalogo.value.map((derecho) => {
      const estado = t.estadoDerechos[derecho.idDerechoTermino]
      return {
        idDerechoTermino: derecho.idDerechoTermino,
        disponible: !!estado?.disponible,
        porcentajeExtra: porcentajeExtraDe(derecho, estado),
      }
    }),
  }
}

function validarTodo() {
  for (const p of PASOS) {
    paso.value = p.n
    if (!validarPasoActual()) return false
  }
  return true
}

async function guardar() {
  errorGuardado.value = ''
  if (!validarTodo()) {
    irAlPrincipio()
    return
  }

  guardando.value = true
  try {
    const payload = {
      idTipoServicio: Number(form.value.idTipoServicio),
      idMedio: Number(form.value.idMedio),
      nombre: form.value.nombre.trim(),
      descripcion: form.value.descripcion.trim(),
      precioBase: Number(form.value.precioBase),
      idMoneda: Number(form.value.idMoneda),
      contenidoSensible: form.value.contenidoSensible,
      contenidosSensibles: form.value.contenidoSensible ? form.value.contenidosSensibles : [],
      tiempoEntregaValor: Number(form.value.tiempoEntregaValor),
      tiempoEntregaUnidad: form.value.tiempoEntregaUnidad,
      cupos: Number(form.value.cupos),
      habilitarCuposTrasEntrega: form.value.habilitarCuposTrasEntrega,
      imagenPortada: form.value.imagenPortada,
      imagenes: form.value.imagenes,
      idsEtiquetas: etiquetas.value.map((e) => e.idEtiqueta),
    }

    if (personalizarFormulario.value) {
      payload.formulario = {
        preguntas: configFormulario.value.preguntas.map((p) => ({
          idTipoCampo: p.idTipoCampo,
          enunciado: p.enunciado,
          ayuda: p.ayuda || null,
          obligatoria: p.obligatoria,
          opciones: (p.codigoTipo === 'CASILLAS' || p.codigoTipo === 'OPCIONES') ? p.opciones : [],
        })),
      }
    }
    if (personalizarTerminos.value) {
      payload.terminos = payloadTerminos()
    }

    let idFinal = idServicio.value
    if (esEdicion.value) {
      await api.put(`/servicios/${idServicio.value}`, payload)
    } else {
      const res = await api.post('/servicios', payload)
      idFinal = res.data.idServicio
    }

    if (esEdicion.value) {
      if (formularioInicialPropio.value && !personalizarFormulario.value) {
        await api.delete(`/servicios/${idFinal}/formulario-pedidos`)
      }
      if (terminosInicialPropio.value && !personalizarTerminos.value) {
        await api.delete(`/servicios/${idFinal}/terminos`)
      }
    }

    estadoGuardado.value = instantanea()
    irAlListado()
  } catch (e) {
    errorGuardado.value = e.response?.data?.message || 'No se pudo guardar el servicio.'
  } finally {
    guardando.value = false
  }
}

function irAlListado() {
  router.push('/mis-servicios')
}

// ---------------------------------------------------------------
// cambios sin guardar
// ---------------------------------------------------------------

const { actualizar: marcarCambios, limpiar: limpiarCambios } = useCambiosSinGuardar()

watch(hayCambios, (pendiente) => {
  marcarCambios(pendiente, 'Tenés cambios sin guardar en el servicio. Si salís, se perderán.')
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
})
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-4 py-8 sm:px-8">
    <button type="button" class="text-sm text-granate hover:text-coral transition" @click="irAlListado">
      ← Volver a mis servicios
    </button>

    <div class="mx-auto mt-4 text-center">
      <h1 class="text-2xl text-granate sm:text-3xl">{{ tituloPagina }}</h1>
      <p class="mt-2 text-sm text-cafe/60">{{ subtituloPagina }}</p>
    </div>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <template v-else>
      <!-- Stepper -->
      <nav aria-label="Pasos del alta de servicio" class="mt-8">
        <ol class="mx-auto flex w-full max-w-4xl items-start">
          <li
            v-for="(p, i) in PASOS"
            :key="p.n"
            class="flex items-start"
            :class="i < PASOS.length - 1 ? 'flex-1' : ''"
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
                  : (p.n < paso ? 'border-coral/40 bg-coral/10 text-coral' : 'border-rosa bg-blanco text-cafe/40')"
              >
                <svg v-if="p.n < paso" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                </svg>
                <span v-else>{{ p.n }}</span>
              </span>
              <span
                class="hidden text-center text-[11px] leading-tight transition sm:block"
                :class="paso === p.n ? 'text-granate' : 'text-cafe/50'"
              >
                {{ p.label }}
              </span>
            </button>
            <span
              v-if="i < PASOS.length - 1"
              class="mt-4.5 h-px flex-1 transition"
              :class="p.n < paso ? 'bg-coral/50' : 'bg-rosa/70'"
            ></span>
          </li>
        </ol>
        <p class="mt-4 text-center text-xs text-cafe/50 sm:hidden">
          Paso {{ paso }} de {{ TOTAL_PASOS }} · {{ pasoActual.label }}
        </p>
      </nav>

      <!-- Contenido -->
      <div
        class="mx-auto mt-8 w-full"
        :class="(paso === 3 || paso === 6) ? 'max-w-5xl' : 'max-w-4xl'"
      >
        <div class="rounded-3xl border border-rosa/50 bg-blanco p-6 shadow-sm sm:p-10">
          <header class="mb-8">
            <p class="text-xs uppercase tracking-[0.18em] text-coral">Paso {{ paso }} de {{ TOTAL_PASOS }}</p>
            <h2 class="mt-2 text-xl text-granate sm:text-2xl">{{ pasoActual.titulo }}</h2>
            <p class="mt-1 text-sm text-cafe/60">{{ pasoActual.descripcion }}</p>
          </header>

          <!-- Paso 1 -->
          <section v-if="paso === 1" class="space-y-6">
            <div>
              <label class="block text-sm text-cafe mb-2">Nombre <span class="text-rojizo">*</span></label>
              <input v-model="form.nombre" type="text" maxlength="50"
                class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm text-cafe mb-2">Tipo de servicio <span class="text-rojizo">*</span></label>
                <select v-model="form.idTipoServicio"
                  class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                  <option value="" disabled>Elegí un tipo</option>
                  <option v-for="t in tipos" :key="t.idTipoServicio" :value="String(t.idTipoServicio)">{{ t.nombre }}</option>
                </select>
                <p v-if="descripcionTipo" class="mt-2 flex items-start gap-1.5 text-xs leading-relaxed text-cafe/60">
                  <svg class="mt-0.5 h-3.5 w-3.5 shrink-0 text-coral" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <circle cx="12" cy="12" r="9" />
                    <path d="M12 11v5m0-8h.01" />
                  </svg>
                  <span>{{ descripcionTipo }}</span>
                </p>
              </div>
              <div>
                <label class="block text-sm text-cafe mb-2">Medio <span class="text-rojizo">*</span></label>
                <select v-model="form.idMedio"
                  class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                  <option value="" disabled>Elegí un medio</option>
                  <option v-for="m in medios" :key="m.idMedio" :value="String(m.idMedio)">{{ m.nombre }}</option>
                </select>
              </div>
            </div>
            <div>
              <label class="block text-sm text-cafe mb-2">Descripción <span class="text-rojizo">*</span></label>
              <textarea v-model="form.descripcion" rows="4" maxlength="250"
                class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"></textarea>
              <p class="text-right text-xs text-cafe/50 mt-1">{{ form.descripcion.length }}/250</p>
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-[minmax(0,1fr)_12rem] gap-6">
              <div>
                <label class="block text-sm text-cafe mb-2">Precio base <span class="text-rojizo">*</span></label>
                <input v-model="form.precioBase" type="number" min="0" step="0.01"
                  class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
              </div>
              <div>
                <label class="block text-sm text-cafe mb-2">Moneda <span class="text-rojizo">*</span></label>
                <select v-model="form.idMoneda"
                  class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                  <option v-for="m in monedas" :key="m.idMoneda" :value="String(m.idMoneda)">{{ m.nombre }} ({{ m.codigo }})</option>
                </select>
                <p class="text-xs text-cafe/50 mt-1">Otros usuarios verán el precio en su moneda preferida.</p>
              </div>
            </div>
          </section>

          <!-- Paso 2 -->
          <section v-else-if="paso === 2" class="space-y-8">
            <div>
              <h3 class="text-sm font-medium text-granate mb-3">Contenido sensible</h3>
              <label class="flex items-start gap-2 text-sm text-cafe">
                <input v-model="form.contenidoSensible" type="checkbox" class="mt-0.5 accent-coral" />
                <span>
                  Este servicio puede contener contenido sensible
                  <span class="block text-xs text-cafe/50 mt-0.5">
                    Si el resultado del servicio puede incluir desnudos, violencia u otro contenido sensible, marcá esta casilla.
                  </span>
                </span>
              </label>

              <div v-if="form.contenidoSensible" class="mt-4 pl-6 border-l-2 border-rosa">
                <p class="text-sm text-cafe mb-2">¿Qué tipo de contenido puede aparecer? <span class="text-rojizo">*</span></p>
                <div class="space-y-2">
                  <label v-for="op in SENSIBLES" :key="op.valor" class="flex items-center gap-2 text-sm text-cafe">
                    <input v-model="form.contenidosSensibles" type="checkbox" :value="op.valor" class="accent-coral" />
                    <span>{{ op.texto }}</span>
                  </label>
                </div>
              </div>
            </div>

            <div class="border-t border-rosa/40 pt-8">
              <h3 class="text-sm font-medium text-granate mb-4">Entrega y cupos</h3>
              <div class="space-y-6">
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  <div>
                    <label class="block text-sm text-cafe mb-2">Tiempo estimado de entrega <span class="text-rojizo">*</span></label>
                    <input v-model="form.tiempoEntregaValor" type="number" min="1"
                      class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
                  </div>
                  <div>
                    <label class="block text-sm text-cafe mb-2">Unidad <span class="text-rojizo">*</span></label>
                    <select v-model="form.tiempoEntregaUnidad"
                      class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                      <option v-for="u in UNIDADES" :key="u.valor" :value="u.valor">{{ u.texto }}</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label class="block text-sm text-cafe mb-2">Cantidad de cupos <span class="text-rojizo">*</span></label>
                  <input v-model="form.cupos" type="number" min="0"
                    class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
                </div>
                <label class="flex items-center gap-2 text-sm text-cafe">
                  <input v-model="form.habilitarCuposTrasEntrega" type="checkbox" class="accent-coral" />
                  <span>Habilitar cupos nuevos tras la entrega de un pedido</span>
                </label>
              </div>
            </div>

            <div class="border-t border-rosa/40 pt-8">
              <h3 class="text-sm font-medium text-granate mb-1">Etiquetas</h3>
              <p class="text-sm text-cafe/60 mb-4">Ayudá a que los clientes encuentren tu servicio. Es opcional.</p>
              <SelectorEtiquetas v-model="etiquetas" />
            </div>
          </section>

          <!-- Paso 3 -->
          <section v-else-if="paso === 3" class="grid gap-8 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)]">
            <div>
              <div class="flex items-end justify-between gap-3 mb-2">
                <h3 class="text-sm font-medium text-granate">Imagen de portada <span class="text-rojizo">*</span></h3>
                <span class="text-xs text-cafe/50">Se muestra en tarjetas y búsquedas</span>
              </div>

              <div
                class="relative overflow-hidden rounded-2xl border-2 border-dashed transition"
                :class="arrastrandoPortada ? 'border-coral bg-coral/5 ring-4 ring-coral/10' : 'border-rosa/70 bg-rosa/5'"
                @dragover.prevent="arrastrandoPortada = true"
                @dragleave.prevent="arrastrandoPortada = false"
                @drop.prevent="onPortadaDrop"
              >
                <template v-if="form.imagenPortada">
                  <img :src="form.imagenPortada" alt="Portada del servicio" class="aspect-video w-full object-cover" />
                  <div class="absolute inset-x-0 bottom-0 flex flex-wrap items-center gap-2 bg-linear-to-t from-granate/75 to-transparent p-4 pt-10">
                    <label for="servicio-portada"
                      class="inline-flex cursor-pointer items-center rounded-md bg-blanco/95 px-3 py-1.5 text-xs font-medium text-granate shadow-sm hover:bg-blanco transition">
                      {{ subiendoPortada ? 'Subiendo…' : 'Cambiar portada' }}
                    </label>
                    <button type="button"
                      class="inline-flex items-center rounded-md bg-blanco/95 px-3 py-1.5 text-xs font-medium text-granate shadow-sm hover:bg-blanco transition"
                      @click="abrirSelectorPortfolio('portada')">
                      Elegir del portfolio
                    </button>
                    <button type="button"
                      class="inline-flex items-center rounded-md bg-blanco/20 px-3 py-1.5 text-xs font-medium text-white hover:bg-blanco/30 transition"
                      @click="form.imagenPortada = ''">
                      Quitar
                    </button>
                  </div>
                </template>
                <div v-else class="flex aspect-video flex-col items-center justify-center gap-3 p-8 text-center">
                  <span class="flex h-12 w-12 items-center justify-center rounded-full bg-coral/10 text-coral">
                    <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 7.5m0 0L7.5 12M12 7.5V21" />
                    </svg>
                  </span>
                  <div>
                    <p class="text-sm text-cafe">Arrastrá tu portada acá</p>
                    <p class="mt-1 text-xs text-cafe/50">JPG, PNG o WebP. Mejor si es horizontal.</p>
                  </div>
                  <div class="flex flex-wrap items-center justify-center gap-2">
                    <label for="servicio-portada"
                      class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate hover:bg-coral/20 transition">
                      {{ subiendoPortada ? 'Subiendo…' : 'Subir nueva imagen' }}
                    </label>
                    <button type="button"
                      class="inline-flex items-center rounded-md border border-rosa px-4 py-2 text-sm font-medium text-cafe hover:bg-rosa/20 transition"
                      @click="abrirSelectorPortfolio('portada')">
                      Elegir del portfolio
                    </button>
                  </div>
                </div>
                <input id="servicio-portada" type="file" :accept="acceptImagenes" class="sr-only" @change="onPortadaChange" />
              </div>
              <p class="mt-2 text-xs text-cafe/50">La portada es la primera impresión de tu servicio.</p>
            </div>

            <div class="min-w-0">
              <div class="flex items-end justify-between gap-3 mb-2">
                <h3 class="text-sm font-medium text-granate">Imágenes de ejemplo <span class="text-rojizo">*</span></h3>
                <span class="text-xs text-cafe/50">{{ form.imagenes.length }} {{ form.imagenes.length === 1 ? 'imagen' : 'imágenes' }}</span>
              </div>
              <p class="text-sm text-cafe/60 mb-3">Mostrá variantes, detalles o trabajos similares.</p>

              <div
                class="grid grid-cols-3 gap-3 rounded-2xl p-1 transition"
                :class="arrastrandoEjemplos ? 'bg-coral/5 ring-4 ring-coral/10' : ''"
                @dragover.prevent="arrastrandoEjemplos = true"
                @dragleave.prevent="arrastrandoEjemplos = false"
                @drop.prevent="onEjemplosDrop"
              >
                <div
                  v-for="(url, i) in form.imagenes"
                  :key="url + i"
                  draggable="true"
                  class="group relative aspect-square cursor-grab overflow-hidden rounded-xl border border-rosa bg-rosa/20 active:cursor-grabbing"
                  :class="indiceImagenArrastrada === i ? 'opacity-60 ring-2 ring-coral' : ''"
                  @dragstart="onDragStartImagen(i)"
                  @dragover.prevent="onDragOverImagen(i)"
                  @dragend="onDragEndImagen"
                >
                  <img :src="url" alt="" class="h-full w-full object-cover" />
                  <span class="absolute left-2 top-2 rounded-full bg-granate/70 px-2 py-0.5 text-[10px] text-white">
                    {{ i + 1 }}
                  </span>
                  <button
                    type="button"
                    class="absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-full bg-blanco/95 text-cafe shadow-sm transition hover:text-rojizo"
                    aria-label="Quitar imagen"
                    @click.stop="quitarEjemplo(i)"
                  >
                    ✕
                  </button>
                </div>

                <label
                  for="servicio-ejemplos"
                  class="flex aspect-square cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed border-rosa/70 bg-rosa/5 text-cafe/50 transition hover:border-coral hover:bg-coral/5 hover:text-coral"
                >
                  <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                  </svg>
                  <span class="text-xs">{{ subiendoEjemplo ? 'Subiendo…' : 'Subir...' }}</span>
                </label>

                <button
                  type="button"
                  class="flex aspect-square flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed border-rosa/70 bg-rosa/5 text-cafe/50 transition hover:border-coral hover:bg-coral/5 hover:text-coral"
                  @click="abrirSelectorPortfolio('ejemplos')"
                >
                  <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909M3.75 21h16.5A1.5 1.5 0 0 0 21.75 19.5V4.5A1.5 1.5 0 0 0 20.25 3H3.75A1.5 1.5 0 0 0 2.25 4.5v15A1.5 1.5 0 0 0 3.75 21Z" />
                  </svg>
                  <span class="text-xs">Portfolio</span>
                </button>
                <input id="servicio-ejemplos" type="file" :accept="acceptImagenes" multiple class="sr-only" @change="onEjemplosChange" />
              </div>
            </div>
          </section>

          <!-- Paso 4 -->
          <section v-else-if="paso === 4" class="space-y-6">
            <div class="grid gap-4 sm:grid-cols-2">
              <button
                type="button"
                class="rounded-2xl border p-5 text-left transition"
                :class="!personalizarFormulario ? 'border-coral bg-coral/5 ring-1 ring-coral' : 'border-rosa/60 hover:border-coral/50'"
                @click="elegirFormulario(false)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium text-granate">Usar mi formulario global</p>
                    <p class="mt-1 text-xs text-cafe/60">Reutilizá la configuración que ya tenés.</p>
                  </div>
                  <span
                    class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border"
                    :class="!personalizarFormulario ? 'border-coral bg-coral text-white' : 'border-rosa text-transparent'"
                  >
                    <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                    </svg>
                  </span>
                </div>
                <p class="mt-3 text-xs text-cafe/50">{{ configFormularioGlobal.preguntas.length }} campos</p>
              </button>

              <button
                type="button"
                class="rounded-2xl border p-5 text-left transition"
                :class="personalizarFormulario ? 'border-coral bg-coral/5 ring-1 ring-coral' : 'border-rosa/60 hover:border-coral/50'"
                @click="elegirFormulario(true)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium text-granate">Personalizar para este servicio</p>
                    <p class="mt-1 text-xs text-cafe/60">Creá un formulario exclusivo para este servicio.</p>
                  </div>
                  <span
                    class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border"
                    :class="personalizarFormulario ? 'border-coral bg-coral text-white' : 'border-rosa text-transparent'"
                  >
                    <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                    </svg>
                  </span>
                </div>
                <p class="mt-3 text-xs text-cafe/50">
                  {{ formularioInicialPropio ? 'Ya tiene un formulario propio.' : 'Se creará una copia de tu formulario global.' }}
                </p>
              </button>
            </div>

            <div v-if="formularioInicialPropio && !personalizarFormulario" class="rounded-xl border border-coral/30 bg-coral/5 px-4 py-3 text-sm text-cafe/80">
              Al guardar se eliminará la personalización que tenía este servicio y se usará tu formulario global.
            </div>

            <div v-if="!personalizarFormulario" class="overflow-hidden rounded-2xl border border-rosa/60">
              <div class="flex items-center justify-between gap-3 border-b border-rosa/50 bg-rosa/10 px-5 py-3">
                <p class="text-sm text-granate">Vista previa del formulario global</p>
                <button type="button" class="text-xs text-coral hover:text-rojizo transition" @click="verFormularioGlobal = !verFormularioGlobal">
                  {{ verFormularioGlobal ? 'Ocultar' : 'Ver vista previa' }}
                </button>
              </div>
              <div v-if="verFormularioGlobal" class="p-5 sm:p-6">
                <VistaPreviaFormulario :preguntas="configFormularioGlobal.preguntas" :mostrar-aviso="false" />
              </div>
            </div>

            <div v-else>
              <div class="inline-flex rounded-lg border border-rosa p-0.5 text-sm">
                <button
                  type="button"
                  class="rounded-md px-3 py-1.5 transition"
                  :class="modoFormulario === 'editar' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'"
                  @click="modoFormulario = 'editar'"
                >
                  Editar
                </button>
                <button
                  type="button"
                  class="rounded-md px-3 py-1.5 transition"
                  :class="modoFormulario === 'preview' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'"
                  @click="modoFormulario = 'preview'"
                >
                  Vista previa
                </button>
              </div>

              <div class="mt-5">
                <EditorFormularioPedidos v-if="modoFormulario === 'editar'" v-model="configFormulario.preguntas" :tipos="tiposCampo" />
                <VistaPreviaFormulario v-else :preguntas="configFormulario.preguntas" :mostrar-aviso="false" />
              </div>
            </div>
          </section>

          <!-- Paso 5 -->
          <section v-else-if="paso === 5" class="space-y-6">
            <div class="grid gap-4 sm:grid-cols-2">
              <button
                type="button"
                class="rounded-2xl border p-5 text-left transition"
                :class="!personalizarTerminos ? 'border-coral bg-coral/5 ring-1 ring-coral' : 'border-rosa/60 hover:border-coral/50'"
                @click="elegirTerminos(false)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium text-granate">Usar mis términos globales</p>
                    <p class="mt-1 text-xs text-cafe/60">Mantené las mismas condiciones para todos tus servicios.</p>
                  </div>
                  <span
                    class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border"
                    :class="!personalizarTerminos ? 'border-coral bg-coral text-white' : 'border-rosa text-transparent'"
                  >
                    <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                    </svg>
                  </span>
                </div>
                <p class="mt-3 text-xs text-cafe/50">Se aplican a este servicio por defecto.</p>
              </button>

              <button
                type="button"
                class="rounded-2xl border p-5 text-left transition"
                :class="personalizarTerminos ? 'border-coral bg-coral/5 ring-1 ring-coral' : 'border-rosa/60 hover:border-coral/50'"
                @click="elegirTerminos(true)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium text-granate">Personalizar para este servicio</p>
                    <p class="mt-1 text-xs text-cafe/60">Definí condiciones específicas para este servicio.</p>
                  </div>
                  <span
                    class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border"
                    :class="personalizarTerminos ? 'border-coral bg-coral text-white' : 'border-rosa text-transparent'"
                  >
                    <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                    </svg>
                  </span>
                </div>
                <p class="mt-3 text-xs text-cafe/50">
                  {{ terminosInicialPropio ? 'Ya tiene términos propios.' : 'Se crearán a partir de tus términos globales.' }}
                </p>
              </button>
            </div>

            <div v-if="terminosInicialPropio && !personalizarTerminos" class="rounded-xl border border-coral/30 bg-coral/5 px-4 py-3 text-sm text-cafe/80">
              Al guardar se eliminarán los términos propios de este servicio y se usarán tus términos globales.
            </div>

            <div v-if="!personalizarTerminos" class="overflow-hidden rounded-2xl border border-rosa/60">
              <div class="flex items-center justify-between gap-3 border-b border-rosa/50 bg-rosa/10 px-5 py-3">
                <p class="text-sm text-granate">Vista previa de tus términos globales</p>
                <button type="button" class="text-xs text-coral hover:text-rojizo transition" @click="verTerminosGlobal = !verTerminosGlobal">
                  {{ verTerminosGlobal ? 'Ocultar' : 'Ver vista previa' }}
                </button>
              </div>
              <div v-if="verTerminosGlobal" class="p-5 sm:p-6">
                <VistaPreviaTerminos
                  :terminos="configTerminosGlobal"
                  :temas-catalogo="temasCatalogo"
                  :derechos-catalogo="derechosCatalogo"
                  :mostrar-aviso="false"
                />
              </div>
            </div>

            <div v-else>
              <div class="inline-flex rounded-lg border border-rosa p-0.5 text-sm">
                <button
                  type="button"
                  class="rounded-md px-3 py-1.5 transition"
                  :class="modoTerminos === 'editar' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'"
                  @click="modoTerminos = 'editar'"
                >
                  Editar
                </button>
                <button
                  type="button"
                  class="rounded-md px-3 py-1.5 transition"
                  :class="modoTerminos === 'preview' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'"
                  @click="modoTerminos = 'preview'"
                >
                  Vista previa
                </button>
              </div>

              <div class="mt-5">
                <EditorTerminos
                  v-if="modoTerminos === 'editar'"
                  v-model="configTerminos"
                  :temas-catalogo="temasCatalogo"
                  :derechos-catalogo="derechosCatalogo"
                />
                <VistaPreviaTerminos
                  v-else
                  :terminos="configTerminos"
                  :temas-catalogo="temasCatalogo"
                  :derechos-catalogo="derechosCatalogo"
                  :mostrar-aviso="false"
                />
              </div>
            </div>
          </section>

          <!-- Paso 6 -->
          <section v-else class="space-y-5">
            <div class="overflow-hidden rounded-2xl border border-rosa/50">
              <div class="grid sm:grid-cols-[15rem_minmax(0,1fr)]">
                <div class="aspect-video bg-rosa/20 sm:aspect-auto">
                  <img v-if="form.imagenPortada" :src="form.imagenPortada" alt="" class="h-full w-full object-cover" />
                  <div v-else class="flex h-full min-h-44 items-center justify-center px-6 text-center text-sm text-cafe/40">
                    Sin portada
                  </div>
                </div>
                <div class="p-5 sm:p-6">
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <p class="text-xs uppercase tracking-wide text-cafe/50">Servicio</p>
                      <h3 class="mt-1 text-xl text-granate">{{ form.nombre || 'Sin nombre' }}</h3>
                      <p class="text-sm text-cafe/60">{{ nombreTipo || 'Tipo' }} · {{ nombreMedio || 'Medio' }}</p>
                      <p v-if="descripcionTipo" class="mt-1 text-xs leading-relaxed text-cafe/50">{{ descripcionTipo }}</p>
                    </div>
                    <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="irAEditar(1)">
                      Editar
                    </button>
                  </div>
                  <p class="mt-3 text-sm text-cafe whitespace-pre-wrap">{{ form.descripcion || '—' }}</p>
                  <div class="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-sm text-cafe/70">
                    <span>Precio: <span class="text-granate">{{ form.precioBase !== '' ? formatearPrecio(form.precioBase, monedaSeleccionada?.codigo || 'ARS') : '—' }}</span></span>
                    <span>Entrega: {{ form.tiempoEntregaValor ? `${form.tiempoEntregaValor} ${UNIDADES.find(u => u.valor === form.tiempoEntregaUnidad)?.texto?.toLowerCase()}` : '—' }}</span>
                    <span>Cupos: {{ form.cupos !== '' ? form.cupos : '—' }}</span>
                    <span v-if="form.contenidoSensible" class="text-rojizo">Contenido sensible</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <article class="rounded-2xl border border-rosa/50 p-5">
                <div class="flex items-start justify-between gap-3">
                  <h3 class="text-sm font-medium text-granate">Entrega y clasificación</h3>
                  <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="irAEditar(2)">Editar</button>
                </div>
                <dl class="mt-3 space-y-1 text-sm text-cafe/70">
                  <div class="flex justify-between gap-3">
                    <dt>Tiempo</dt>
                    <dd class="text-cafe">{{ form.tiempoEntregaValor ? `${form.tiempoEntregaValor} ${UNIDADES.find(u => u.valor === form.tiempoEntregaUnidad)?.texto?.toLowerCase()}` : '—' }}</dd>
                  </div>
                  <div class="flex justify-between gap-3">
                    <dt>Cupos</dt>
                    <dd class="text-cafe">{{ form.cupos !== '' ? form.cupos : '—' }}</dd>
                  </div>
                  <div class="flex justify-between gap-3">
                    <dt>Cupos tras entrega</dt>
                    <dd class="text-cafe">{{ form.habilitarCuposTrasEntrega ? 'Sí' : 'No' }}</dd>
                  </div>
                  <div class="flex justify-between gap-3">
                    <dt>Contenido sensible</dt>
                    <dd class="text-cafe">{{ form.contenidoSensible ? 'Sí' : 'No' }}</dd>
                  </div>
                </dl>
                <div class="mt-4">
                  <p class="text-xs text-cafe/50 mb-1.5">Etiquetas</p>
                  <div class="flex flex-wrap gap-1.5">
                    <span v-for="e in etiquetas" :key="e.idEtiqueta" class="rounded-full bg-durazno/40 px-2.5 py-1 text-xs text-cafe">
                      {{ e.nombre }}
                    </span>
                    <span v-if="!etiquetas.length" class="text-sm text-cafe/40">Sin etiquetas</span>
                  </div>
                </div>
              </article>

              <article class="rounded-2xl border border-rosa/50 p-5">
                <div class="flex items-start justify-between gap-3">
                  <h3 class="text-sm font-medium text-granate">Imágenes</h3>
                  <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="irAEditar(3)">Editar</button>
                </div>
                <div class="mt-3 flex gap-2 overflow-x-auto pb-1">
                  <img v-if="form.imagenPortada" :src="form.imagenPortada" alt="" class="h-20 w-28 shrink-0 rounded-lg border border-rosa object-cover" />
                  <img
                    v-for="(url, i) in form.imagenes.slice(0, 3)"
                    :key="url + i"
                    :src="url"
                    alt=""
                    class="h-20 w-20 shrink-0 rounded-lg border border-rosa object-cover"
                  />
                  <span v-if="form.imagenes.length > 3" class="flex h-20 w-14 shrink-0 items-center justify-center rounded-lg bg-rosa/20 text-xs text-cafe/60">
                    +{{ form.imagenes.length - 3 }}
                  </span>
                </div>
                <p class="mt-2 text-xs text-cafe/50">Portada + {{ form.imagenes.length }} {{ form.imagenes.length === 1 ? 'imagen de ejemplo' : 'imágenes de ejemplo' }}</p>
              </article>

              <article class="rounded-2xl border border-rosa/50 p-5">
                <div class="flex items-start justify-between gap-3">
                  <h3 class="text-sm font-medium text-granate">Formulario de pedidos</h3>
                  <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="irAEditar(4)">Editar</button>
                </div>
                <p class="mt-2 text-sm text-cafe/70">
                  {{ personalizarFormulario ? 'Personalizado para este servicio' : 'Usando tu formulario global' }}
                  · {{ formularioVisible.preguntas.length }} campos
                </p>
                <ul class="mt-3 space-y-1 text-sm text-cafe/80">
                  <li v-for="p in formularioVisible.preguntas.slice(0, 3)" :key="p.uid" class="flex gap-2">
                    <span class="text-coral">·</span>
                    <span class="truncate">{{ p.enunciado }}</span>
                  </li>
                  <li v-if="formularioVisible.preguntas.length > 3" class="text-xs text-cafe/50">
                    y {{ formularioVisible.preguntas.length - 3 }} más
                  </li>
                </ul>
                <button type="button" class="mt-3 text-xs text-coral hover:text-rojizo transition" @click="verFormularioRevision = !verFormularioRevision">
                  {{ verFormularioRevision ? 'Ocultar vista previa' : 'Ver vista previa' }}
                </button>
                <div v-if="verFormularioRevision" class="mt-3 rounded-xl border border-rosa/50 bg-rosa/5 p-4">
                  <VistaPreviaFormulario :preguntas="formularioVisible.preguntas" :mostrar-aviso="false" />
                </div>
              </article>

              <article class="rounded-2xl border border-rosa/50 p-5">
                <div class="flex items-start justify-between gap-3">
                  <h3 class="text-sm font-medium text-granate">Términos y condiciones</h3>
                  <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="irAEditar(5)">Editar</button>
                </div>
                <p class="mt-2 text-sm text-cafe/70">
                  {{ personalizarTerminos ? 'Personalizados para este servicio' : 'Usando tus términos globales' }}
                </p>
                <p class="mt-2 text-sm text-cafe/70 line-clamp-3">{{ resumenTerminos }}</p>
                <button type="button" class="mt-3 text-xs text-coral hover:text-rojizo transition" @click="verTerminosRevision = !verTerminosRevision">
                  {{ verTerminosRevision ? 'Ocultar vista previa' : 'Ver vista previa' }}
                </button>
                <div v-if="verTerminosRevision" class="mt-3 rounded-xl border border-rosa/50 bg-rosa/5 p-4">
                  <VistaPreviaTerminos
                    :terminos="terminosVisible"
                    :temas-catalogo="temasCatalogo"
                    :derechos-catalogo="derechosCatalogo"
                    :mostrar-aviso="false"
                  />
                </div>
              </article>
            </div>

            <div class="rounded-2xl border border-coral/30 bg-coral/5 p-5 text-sm text-cafe/80">
              Revisá que todo esté correcto. Al publicar, el servicio será visible para los clientes.
            </div>
          </section>

          <!-- Errores -->
          <div v-if="errores.length" class="mt-6 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3">
            <p v-for="err in errores" :key="err" class="text-sm text-rojizo">{{ err }}</p>
          </div>

          <div v-if="errorGuardado" class="mt-6 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3">
            <p class="text-sm text-rojizo">{{ errorGuardado }}</p>
          </div>

          <!-- Navegación -->
          <div class="mt-8 flex flex-col-reverse gap-3 border-t border-rosa/60 pt-6 sm:flex-row sm:items-center sm:justify-between">
            <button
              v-if="paso > 1"
              type="button"
              class="px-4 py-2.5 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
              @click="anterior"
            >
              Anterior
            </button>
            <span v-else class="text-sm text-cafe/50">Paso {{ paso }} de {{ TOTAL_PASOS }}</span>

            <button
              v-if="paso < TOTAL_PASOS"
              type="button"
              class="px-6 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm sm:min-w-44"
              @click="siguiente"
            >
              Siguiente
            </button>
            <button
              v-else
              type="button"
              :disabled="guardando"
              class="px-6 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm sm:min-w-44"
              @click="guardar"
            >
              {{ guardando ? 'Guardando...' : (esEdicion ? 'Guardar cambios' : 'Publicar servicio') }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <SelectorPortfolio
      v-if="selectorAbierto"
      :modo="selectorModo"
      :seleccionadas="selectorModo === 'portada' ? (form.imagenPortada ? [form.imagenPortada] : []) : form.imagenes"
      @cerrar="cerrarSelectorPortfolio"
      @seleccionar="onSeleccionPortfolio"
    />
  </div>
</template>
