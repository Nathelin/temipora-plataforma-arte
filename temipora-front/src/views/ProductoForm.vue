<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, validarImagen } from '../servicios/archivos'
import { formatearPrecio } from '../servicios/moneda'
import {
  estadoVarianteDe,
  etiquetaStock,
  hayTerminos,
  subirImagenProducto,
} from '../servicios/productos'
import { useMonedas } from '../composables/useMonedas'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import SelectorVariantes from '../components/SelectorVariantes.vue'
import SelectorEtiquetas from '../components/SelectorEtiquetas.vue'
import VisorImagenes from '../components/VisorImagenes.vue'

const route = useRoute()
const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const idProducto = computed(() => (route.params.id ? Number(route.params.id) : null))
const esEdicion = computed(() => idProducto.value !== null)

const PASOS = [
  {
    n: 1,
    label: 'Datos',
    titulo: 'Datos del producto',
    descripcion: 'Título, clasificación, precio base y condiciones de reventa.',
  },
  {
    n: 2,
    label: 'Variantes',
    titulo: 'Variantes y stock',
    descripcion: 'Cada adoptable, tamaño o presentación es una variante vendible.',
  },
  {
    n: 3,
    label: 'Imágenes',
    titulo: 'Imágenes del producto',
    descripcion: 'La primera imagen será la portada de la publicación.',
  },
  {
    n: 4,
    label: 'Etiquetas',
    titulo: 'Etiquetas y términos',
    descripcion: 'Clasificá la publicación y definí las condiciones de uso.',
  },
  {
    n: 5,
    label: 'Revisión',
    titulo: 'Revisión y publicación',
    descripcion: 'Revisá todo antes de publicar el producto.',
  },
]
const TOTAL_PASOS = PASOS.length

const paso = ref(1)
const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const errorGuardado = ref('')
const errores = ref([])
const subiendoImagen = ref(false)
const errorImagen = ref('')
const visorAbierto = ref(false)
const visorIndice = ref(0)

const tipos = ref([])
const medios = ref([])
const idUsuario = ref(null)
const globalTerminos = ref(null)
const terminosInicialPropio = ref(false)
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
  permiteReventa: false,
  porcentajeRegalia: 0,
  imagenes: [],
})

const variantes = ref([])
const etiquetas = ref([])
const terminosPropios = ref(false)
const terminosTexto = ref('')
const estadoGuardado = ref(null)

const pasoActual = computed(() => PASOS.find((p) => p.n === paso.value) || PASOS[0])
const monedaSeleccionada = computed(
  () => monedas.value.find((m) => String(m.idMoneda) === form.value.idMoneda) || null,
)
const monedaCodigo = computed(() => monedaSeleccionada.value?.codigo || 'ARS')
const hayTerminosGlobales = computed(() => hayTerminos(globalTerminos.value))
const hayVarianteUnica = computed(
  () => variantes.value.some((v) => !esIlimitada(v) && Number(v.stockDisponible) === 1),
)
const precioDesde = computed(() => {
  const precios = variantes.value
    .map((v) => Number(v.precio))
    .filter((precio) => Number.isFinite(precio) && precio > 0)
  return precios.length ? Math.min(...precios) : null
})
const imagenesUrl = computed(() => form.value.imagenes.map((imagen) => imagen.url))
const tituloPagina = computed(() => (esEdicion.value ? 'Editar producto' : 'Crear producto'))
const nombreTipo = computed(
  () => tipos.value.find((t) => String(t.idTipoProducto) === form.value.idTipoProducto)?.nombre || '',
)
const nombreMedio = computed(
  () => medios.value.find((m) => String(m.idMedio) === form.value.idMedio)?.nombre || '',
)
const resumenTerminosGlobal = computed(() => {
  const t = globalTerminos.value
  return t?.informacionGeneral || t?.usoCliente || t?.usoArtista
    || 'Tus términos globales están configurados.'
})

function esIlimitada(variante) {
  return variante?.stockDisponible === null || variante?.stockDisponible === undefined
}

function instantanea() {
  return JSON.stringify({
    form: form.value,
    variantes: variantes.value,
    etiquetas: etiquetas.value,
    terminosPropios: terminosPropios.value,
    terminosTexto: terminosTexto.value,
  })
}

const hayCambios = computed(
  () => estadoGuardado.value !== null && estadoGuardado.value !== instantanea(),
)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [tiposRes, mediosRes, perfilRes] = await Promise.all([
      api.get('/tipos-producto'),
      api.get('/medios'),
      api.get(`/perfil/${nombreUsuario}`),
      cargarMonedas(),
    ])
    tipos.value = tiposRes.data
    medios.value = mediosRes.data
    idUsuario.value = perfilRes.data.idUsuario

    const terminosRes = await api.get(`/usuarios/${idUsuario.value}/terminos`)
    globalTerminos.value = terminosRes.data

    if (esEdicion.value) {
      const [productoRes, terminosProductoRes] = await Promise.all([
        api.get(`/mis-productos/${idProducto.value}`),
        api.get(`/mis-productos/${idProducto.value}/terminos`),
      ])
      mapearProducto(productoRes.data)
      const terminosProducto = terminosProductoRes.data
      terminosInicialPropio.value = !!terminosProducto?.idProducto
      terminosPropios.value = terminosInicialPropio.value || !hayTerminosGlobales.value
      if (terminosInicialPropio.value) {
        terminosTexto.value = terminosProducto.usoCliente
          || terminosProducto.informacionGeneral
          || ''
      }
    } else {
      const pred = predeterminada()
      form.value.idMoneda = pred ? String(pred.idMoneda) : ''
      terminosPropios.value = !hayTerminosGlobales.value
    }
    estadoGuardado.value = instantanea()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el producto.'
  } finally {
    cargando.value = false
  }
}

function mapearProducto(d) {
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
    permiteReventa: !!d.permiteReventa,
    porcentajeRegalia: Number(d.porcentajeRegalia || 0),
    imagenes: (d.imagenes || []).map((imagen) => ({
      url: imagen.url,
      sha256: imagen.sha256,
      claveArchivoOriginal: '',
    })),
  }
  variantes.value = (d.variantes || []).map((variante) => ({
    idVariante: variante.idVariante,
    nombre: variante.nombre || '',
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
  }))
  etiquetas.value = Array.isArray(d.etiquetas) ? [...d.etiquetas] : []
}

async function subirImagen(event) {
  const archivo = event.target.files?.[0] || null
  event.target.value = ''
  if (!archivo) return

  const problema = validarImagen(archivo)
  if (problema) {
    errorImagen.value = problema
    return
  }

  subiendoImagen.value = true
  errorImagen.value = ''
  try {
    const data = await subirImagenProducto(archivo)
    form.value.imagenes.push({
      url: data.url,
      sha256: data.sha256,
      claveArchivoOriginal: data.claveOriginal,
    })
  } catch (e) {
    errorImagen.value = e.response?.data?.message
      || 'No se pudo subir la imagen del producto.'
  } finally {
    subiendoImagen.value = false
  }
}

function quitarImagen(indice) {
  form.value.imagenes.splice(indice, 1)
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

function validarDatos() {
  const errs = []
  if (!form.value.titulo.trim()) errs.push('El título es obligatorio.')
  if (!form.value.idTipoProducto) errs.push('Elegí un tipo de producto.')
  if (!form.value.idMedio) errs.push('Elegí un medio.')
  if (!form.value.descripcion.trim()) errs.push('La descripción es obligatoria.')
  if (!form.value.idMoneda) errs.push('Elegí una moneda.')
  if (form.value.contenidoSensible && !form.value.contenidosSensibles.length) {
    errs.push('Elegí qué tipo de contenido sensible puede aparecer.')
  }
  const regalia = Number(form.value.porcentajeRegalia || 0)
  if (regalia < 0 || regalia > 10) {
    errs.push('La regalía debe estar entre 0 y 10%.')
  }
  return errs
}

function validarReventa() {
  if (form.value.permiteReventa && !hayVarianteUnica.value) {
    return ['Para habilitar la reventa necesitás al menos una variante con stock 1.']
  }
  return []
}

function validarVariantes() {
  const errs = []
  if (!variantes.value.length) {
    errs.push('Agregá al menos una variante.')
    return errs
  }
  const nombres = new Set()
  variantes.value.forEach((variante, indice) => {
    const nombre = String(variante.nombre || '').trim()
    if (!nombre) {
      errs.push(`La variante ${indice + 1} necesita un nombre.`)
    } else if (nombres.has(nombre.toLowerCase())) {
      errs.push(`El nombre “${nombre}” está repetido.`)
    } else {
      nombres.add(nombre.toLowerCase())
    }
    if (!variante.precio || Number(variante.precio) <= 0) {
      errs.push(`El precio de la variante ${indice + 1} debe ser mayor a cero.`)
    }
    if (variante.stockDisponible !== null
        && Number(variante.stockDisponible) < 0) {
      errs.push(`El stock de la variante ${indice + 1} no puede ser negativo.`)
    }
    if (!variante.urlImagen || !variante.sha256) {
      errs.push(`Subí la imagen de la variante ${indice + 1}.`)
    }
  })
  return errs
}

function validarImagenes() {
  return form.value.imagenes.length
    ? []
    : ['Agregá al menos una imagen del producto.']
}

function validarTerminos() {
  if (terminosPropios.value) {
    return terminosTexto.value.trim()
      ? []
      : ['Escribí los términos de uso del producto.']
  }
  return hayTerminosGlobales.value
    ? []
    : ['Tus términos globales están vacíos. Completalos o definí términos propios para este producto.']
}

function validarPasoActual() {
  let problemas = []
  if (paso.value === 1) problemas = validarDatos()
  else if (paso.value === 2) problemas = validarVariantes()
  else if (paso.value === 3) problemas = validarImagenes()
  else if (paso.value === 4) problemas = validarTerminos()
  else if (paso.value === 5) problemas = validarReventa()

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

function validarTodo() {
  for (const p of PASOS) {
    paso.value = p.n
    if (!validarPasoActual()) return false
  }
  return true
}

function construirPayload() {
  const payload = {
    idTipoProducto: Number(form.value.idTipoProducto),
    idMedio: Number(form.value.idMedio),
    titulo: form.value.titulo.trim(),
    descripcion: form.value.descripcion.trim(),
    idMoneda: Number(form.value.idMoneda),
    contenidoSensible: form.value.contenidoSensible,
    contenidosSensibles: form.value.contenidoSensible
      ? form.value.contenidosSensibles
      : [],
    permiteReventa: form.value.permiteReventa,
    porcentajeRegalia: form.value.permiteReventa
      ? Number(form.value.porcentajeRegalia || 0)
      : 0,
    imagenes: form.value.imagenes.map((imagen, indice) => ({
      url: imagen.url,
      sha256: imagen.sha256,
      claveArchivoOriginal: imagen.claveArchivoOriginal || undefined,
      orden: indice,
    })),
    variantes: variantes.value.map((variante, indice) => ({
      idVariante: variante.idVariante || undefined,
      nombre: variante.nombre.trim(),
      descripcion: variante.descripcion?.trim() || null,
      precio: Number(variante.precio),
      stockDisponible: variante.stockDisponible === null
        ? null
        : Number(variante.stockDisponible),
      orden: indice,
      estado: variante.estado,
      urlImagen: variante.urlImagen,
      sha256: variante.sha256,
      claveArchivoOriginal: variante.claveArchivoOriginal || undefined,
      numeroEdicion: variante.numeroEdicion || null,
    })),
    idsEtiquetas: etiquetas.value.map((etiqueta) => etiqueta.idEtiqueta),
  }
  if (terminosPropios.value) {
    payload.terminos = {
      usoCliente: terminosTexto.value.trim(),
    }
  }
  return payload
}

async function guardar() {
  errorGuardado.value = ''
  if (!validarTodo()) {
    irAlPrincipio()
    return
  }

  guardando.value = true
  try {
    const payload = construirPayload()
    let idFinal = idProducto.value
    if (esEdicion.value) {
      await api.put(`/mis-productos/${idProducto.value}`, payload)
    } else {
      const res = await api.post('/mis-productos', payload)
      idFinal = res.data.idProducto
    }

    if (esEdicion.value
        && terminosInicialPropio.value
        && !terminosPropios.value) {
      await api.delete(`/mis-productos/${idFinal}/terminos`)
    }

    estadoGuardado.value = instantanea()
    router.push('/panel/productos')
  } catch (e) {
    errorGuardado.value = e.response?.data?.message
      || 'No se pudo guardar el producto.'
  } finally {
    guardando.value = false
  }
}

function irAlListado() {
  router.push('/panel/productos')
}

const { actualizar: marcarCambios, limpiar: limpiarCambios } = useCambiosSinGuardar()

watch(hayCambios, (pendiente) => {
  marcarCambios(
    pendiente,
    'Tenés cambios sin guardar en el producto. Si salís, se perderán.',
  )
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
    <button
      type="button"
      class="text-sm text-granate transition hover:text-coral"
      @click="irAlListado"
    >
      ← Volver a mis productos
    </button>

    <div class="mx-auto mt-4 text-center">
      <h1 class="text-2xl text-granate sm:text-3xl">{{ tituloPagina }}</h1>
      <p class="mt-2 text-sm text-cafe/60">
        Completá la publicación y sus variantes paso a paso.
      </p>
    </div>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <template v-else>
      <nav aria-label="Pasos del alta de producto" class="mt-8">
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

      <div class="mx-auto mt-8 w-full max-w-4xl">
        <div class="rounded-3xl border border-rosa/50 bg-blanco p-6 shadow-sm sm:p-10">
          <header class="mb-8">
            <p class="text-xs uppercase tracking-[0.18em] text-coral">
              Paso {{ paso }} de {{ TOTAL_PASOS }}
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

          <!-- Paso 1 -->
          <section v-if="paso === 1" class="space-y-6">
            <div>
              <label class="mb-2 block text-sm text-cafe">
                Título <span class="text-rojizo">*</span>
              </label>
              <input
                v-model="form.titulo"
                type="text"
                maxlength="100"
                placeholder="Ej. Plancha de adoptables de otoño"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
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
            </div>

            <div>
              <label class="mb-2 block text-sm text-cafe">
                Moneda <span class="text-rojizo">*</span>
              </label>
              <select
                v-model="form.idMoneda"
                class="block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
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
            </div>

            <div class="rounded-2xl border border-rosa/60 bg-crema/50 p-5">
              <label class="inline-flex items-center gap-2 text-sm font-medium text-granate">
                <input
                  v-model="form.permiteReventa"
                  type="checkbox"
                  class="h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                />
                Permitir la reventa de piezas únicas
              </label>
              <p class="mt-2 text-xs leading-relaxed text-cafe/55">
                Solo se aplica a variantes con stock 1. El comprador podrá
                revender la pieza y vos vas a recibir una regalía.
              </p>
              <div v-if="form.permiteReventa" class="mt-4 flex flex-wrap items-end gap-4">
                <div class="w-40">
                  <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                    Regalía (%)
                  </label>
                  <input
                    v-model.number="form.porcentajeRegalia"
                    type="number"
                    min="0"
                    max="10"
                    step="0.01"
                    class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  />
                </div>
                <p
                  class="rounded-lg px-3 py-2 text-xs"
                  :class="hayVarianteUnica
                    ? 'bg-green-500/10 text-green-700'
                    : 'bg-mantequilla/50 text-granate'"
                >
                  {{ hayVarianteUnica
                    ? 'Hay al menos una variante única para revender.'
                    : 'Todavía no hay ninguna variante con stock 1.' }}
                </p>
              </div>
            </div>
          </section>

          <!-- Paso 2 -->
          <section v-else-if="paso === 2">
            <SelectorVariantes
              v-model="variantes"
              :moneda="monedaCodigo"
              :permite-reventa="form.permiteReventa"
            />
          </section>

          <!-- Paso 3 -->
          <section v-else-if="paso === 3" class="space-y-5">
            <div class="rounded-2xl border border-dashed border-rosa/70 bg-crema/40 p-6 text-center">
              <p class="text-sm font-medium text-granate">
                Subí las imágenes de la publicación
              </p>
              <p class="mt-1 text-xs text-cafe/55">
                La primera imagen será la portada. Se guarda el original privado
                y una preview pública optimizada.
              </p>
              <label
                class="mt-4 inline-flex cursor-pointer items-center justify-center rounded-lg bg-coral px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo"
                :class="subiendoImagen ? 'pointer-events-none opacity-50' : ''"
              >
                {{ subiendoImagen ? 'Subiendo...' : 'Elegir imagen' }}
                <input
                  type="file"
                  class="sr-only"
                  :accept="acceptImagenes"
                  @change="subirImagen"
                />
              </label>
              <p v-if="errorImagen" class="mt-2 text-sm text-rojizo">{{ errorImagen }}</p>
            </div>

            <div
              v-if="form.imagenes.length"
              class="grid grid-cols-2 gap-4 sm:grid-cols-3"
            >
              <article
                v-for="(imagen, indice) in form.imagenes"
                :key="`${imagen.url}-${indice}`"
                class="overflow-hidden rounded-2xl border border-rosa/60 bg-blanco shadow-sm"
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
                <div class="flex items-center justify-between gap-2 px-3 py-2">
                  <span class="text-[11px] font-semibold text-cafe/55">
                    {{ indice === 0 ? 'Portada' : `Imagen ${indice + 1}` }}
                  </span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      class="rounded px-1.5 py-0.5 text-xs text-cafe/60 transition hover:bg-rosa/20 disabled:opacity-30"
                      :disabled="indice === 0"
                      aria-label="Mover imagen a la izquierda"
                      @click="moverImagen(indice, -1)"
                    >
                      ←
                    </button>
                    <button
                      type="button"
                      class="rounded px-1.5 py-0.5 text-xs text-cafe/60 transition hover:bg-rosa/20 disabled:opacity-30"
                      :disabled="indice === form.imagenes.length - 1"
                      aria-label="Mover imagen a la derecha"
                      @click="moverImagen(indice, 1)"
                    >
                      →
                    </button>
                    <button
                      type="button"
                      class="rounded px-1.5 py-0.5 text-xs text-rojizo/80 transition hover:bg-rojizo/5 hover:text-rojizo"
                      @click="quitarImagen(indice)"
                    >
                      ×
                    </button>
                  </div>
                </div>
              </article>
            </div>

            <p v-else class="text-center text-sm text-cafe/50">
              Todavía no subiste imágenes.
            </p>
          </section>

          <!-- Paso 4 -->
          <section v-else-if="paso === 4" class="space-y-6">
            <SelectorEtiquetas v-model="etiquetas" />

            <div class="rounded-2xl border border-rosa/60 bg-crema/50 p-5">
              <h3 class="text-sm font-semibold text-granate">Términos de uso</h3>

              <div v-if="hayTerminosGlobales" class="mt-3 rounded-xl border border-rosa/50 bg-blanco p-4">
                <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">
                  Tus términos globales
                </p>
                <p class="mt-2 line-clamp-3 text-sm leading-relaxed text-cafe/70">
                  {{ resumenTerminosGlobal }}
                </p>
                <router-link
                  to="/panel/terminos"
                  class="mt-2 inline-block text-xs font-medium text-coral transition hover:text-rojizo"
                >
                  Editar términos globales →
                </router-link>
              </div>

              <p
                v-else
                class="mt-3 rounded-xl border border-mantequilla/70 bg-mantequilla/30 px-4 py-3 text-sm text-granate"
              >
                No tenés términos globales configurados. Definí términos propios
                para este producto o completá tus
                <router-link to="/panel/terminos" class="font-semibold underline">
                  términos globales
                </router-link>.
              </p>

              <label
                v-if="hayTerminosGlobales"
                class="mt-4 inline-flex items-center gap-2 text-sm font-medium text-granate"
              >
                <input
                  v-model="terminosPropios"
                  type="checkbox"
                  class="h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                />
                Definir términos propios para este producto
              </label>

              <div v-if="terminosPropios || !hayTerminosGlobales" class="mt-4">
                <label class="mb-2 block text-xs font-medium text-cafe/70">
                  Términos de uso del producto
                </label>
                <textarea
                  v-model="terminosTexto"
                  rows="5"
                  maxlength="4000"
                  placeholder="Ej. La compra habilita el uso personal de la pieza. No permite reventa salvo que se indique."
                  class="block w-full resize-none rounded-lg border border-rosa/60 bg-blanco px-4 py-2.5 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                ></textarea>
              </div>
            </div>
          </section>

          <!-- Paso 5 -->
          <section v-else-if="paso === 5" class="space-y-6">
            <div class="grid gap-4 sm:grid-cols-2">
              <article class="rounded-2xl border border-rosa/60 bg-crema/40 p-5">
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Publicación
                </p>
                <h3 class="mt-2 font-display text-lg font-bold text-granate">
                  {{ form.titulo }}
                </h3>
                <p class="mt-1 text-xs text-cafe/55">
                  {{ nombreTipo }} · {{ nombreMedio }} · {{ monedaCodigo }}
                </p>
                <p class="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-cafe/75">
                  {{ form.descripcion }}
                </p>
                <p class="mt-3 text-sm font-semibold text-coral">
                  {{ precioDesde !== null ? formatearPrecio(precioDesde, monedaCodigo) : 'Sin precio' }}
                  <span class="text-[11px] font-normal text-cafe/45">desde</span>
                </p>
                <p v-if="form.contenidoSensible" class="mt-2 text-xs font-medium text-granate">
                  Contenido sensible: {{ form.contenidosSensibles.join(' · ') }}
                </p>
              </article>

              <article class="rounded-2xl border border-rosa/60 bg-crema/40 p-5">
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Reventa
                </p>
                <p class="mt-2 text-sm text-cafe/75">
                  {{ form.permiteReventa
                    ? `Habilitada para piezas únicas con regalía del ${form.porcentajeRegalia}%.`
                    : 'No habilitada para este producto.' }}
                </p>
                <p class="mt-4 text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Términos
                </p>
                <p class="mt-2 text-sm text-cafe/75">
                  {{ terminosPropios
                    ? 'Términos propios del producto.'
                    : 'Términos globales de la cuenta.' }}
                </p>
              </article>
            </div>

            <article class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Variantes ({{ variantes.length }})
              </p>
              <div class="mt-3 divide-y divide-rosa/40">
                <div
                  v-for="(variante, indice) in variantes"
                  :key="variante.idVariante || indice"
                  class="flex items-center gap-3 py-3"
                >
                  <img
                    v-if="variante.urlImagen"
                    :src="variante.urlImagen"
                    :alt="variante.nombre"
                    class="h-12 w-12 shrink-0 rounded-lg object-cover ring-1 ring-rosa/50"
                  />
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-semibold text-granate">
                      {{ variante.nombre }}
                    </p>
                    <p class="text-xs text-cafe/55">
                      {{ formatearPrecio(Number(variante.precio), monedaCodigo) }}
                      · {{ etiquetaStock(variante) }}
                    </p>
                  </div>
                  <span
                    class="shrink-0 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                    :class="estadoVarianteDe(variante.estado).clase"
                  >
                    {{ estadoVarianteDe(variante.estado).texto }}
                  </span>
                  <span
                    v-if="!esIlimitada(variante) && Number(variante.stockDisponible) === 1"
                    class="shrink-0 rounded-full bg-mantequilla/60 px-2.5 py-0.5 text-[11px] font-bold text-granate"
                    title="Al publicar se crea el evento génesis en el ledger"
                  >
                    Génesis al publicar
                  </span>
                </div>
              </div>
            </article>

            <article class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Imágenes ({{ form.imagenes.length }})
              </p>
              <div class="mt-3 flex flex-wrap gap-3">
                <img
                  v-for="(imagen, indice) in form.imagenes"
                  :key="`${imagen.url}-${indice}`"
                  :src="imagen.url"
                  :alt="`Imagen ${indice + 1}`"
                  class="h-20 w-20 rounded-xl object-cover ring-1 ring-rosa/50"
                />
              </div>
            </article>

            <article class="rounded-2xl border border-rosa/60 bg-blanco p-5">
              <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Etiquetas
              </p>
              <div class="mt-3 flex flex-wrap gap-2">
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

            <p
              v-if="errorGuardado"
              class="rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
            >
              {{ errorGuardado }}
            </p>
          </section>

          <footer class="mt-10 flex flex-col-reverse gap-3 border-t border-rosa/50 pt-6 sm:flex-row sm:items-center sm:justify-between">
            <button
              v-if="paso > 1"
              type="button"
              class="rounded-lg border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
              @click="anterior"
            >
              Anterior
            </button>
            <span v-else></span>

            <div class="flex items-center gap-3">
              <button
                v-if="paso < TOTAL_PASOS"
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
                @click="guardar"
              >
                {{ guardando
                  ? 'Guardando...'
                  : (esEdicion ? 'Guardar cambios' : 'Publicar producto') }}
              </button>
            </div>
          </footer>
        </div>

        <div class="mt-6 rounded-2xl border border-rosa/50 bg-crema/60 p-5">
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
            Revisión rápida
          </p>
          <div class="mt-3 grid gap-3 sm:grid-cols-3">
            <button
              type="button"
              class="rounded-xl border border-rosa/50 bg-blanco px-4 py-3 text-left transition hover:border-coral/40"
              @click="irAEditar(1)"
            >
              <p class="text-xs text-cafe/50">Datos</p>
              <p class="mt-1 truncate text-sm font-medium text-granate">{{ form.titulo || 'Sin título' }}</p>
            </button>
            <button
              type="button"
              class="rounded-xl border border-rosa/50 bg-blanco px-4 py-3 text-left transition hover:border-coral/40"
              @click="irAEditar(2)"
            >
              <p class="text-xs text-cafe/50">Variantes</p>
              <p class="mt-1 text-sm font-medium text-granate">{{ variantes.length }} variante(s)</p>
            </button>
            <button
              type="button"
              class="rounded-xl border border-rosa/50 bg-blanco px-4 py-3 text-left transition hover:border-coral/40"
              @click="irAEditar(3)"
            >
              <p class="text-xs text-cafe/50">Imágenes</p>
              <p class="mt-1 text-sm font-medium text-granate">{{ form.imagenes.length }} imagen(es)</p>
            </button>
          </div>
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
  </div>
</template>
