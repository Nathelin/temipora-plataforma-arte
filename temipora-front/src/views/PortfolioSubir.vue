<script setup>
import { ref, computed, watchEffect, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import {
  acceptImagenes,
  validarImagen,
  procesarImagen,
  nombreSinExtension,
  LIMITES_IMAGEN,
  MAX_IMAGENES_POR_LOTE,
} from '../servicios/archivos'
import SelectorEtiquetas from '../components/SelectorEtiquetas.vue'
import SelectorMateriales from '../components/SelectorMateriales.vue'
import { useSubirImagen } from '../composables/useSubirImagen'
import { useAvisoActualizacion } from '../composables/useAvisoActualizacion'

const router = useRouter()
const {
  estado,
  cerrar,
  subidaEnCurso,
  publicacionesPendientes,
  publicadasEnSesion,
} = useSubirImagen()
const {
  estado: avisoEstado,
  mostrar: avisoMostrar,
  fase: avisoFase,
  detalle: avisoDetalle,
  exito: avisoExito,
  fallo: avisoFallo,
  ocultar: avisoOcultar,
} = useAvisoActualizacion()

const esEdicion = computed(() => estado.modo === 'editar')

// ---------------------------------------------------------------
// Selección / lote
// ---------------------------------------------------------------

const vista = ref('seleccion') // 'seleccion' | 'subiendo' | 'datos'
const lote = ref([])
const indiceActual = ref(0)
const errorSeleccion = ref('')
const arrastrando = ref(false)

// ---------------------------------------------------------------
// Estados de formulario
// ---------------------------------------------------------------

const error = ref('')
const guardando = ref(false)
const fase = ref('')
const progreso = ref(0)
const subiendoLote = ref(false)
const subidaActual = ref(0)
const progresoSubida = ref(0)
const imagenEditando = ref(null)
const itemEnGuardado = ref(null)

let siguienteId = 1

const actual = computed(() => lote.value[indiceActual.value] || null)
const guardadas = computed(() => lote.value.filter(i => i.estado === 'guardada').length)
const subidas = computed(() => lote.value.filter(i => !!i.url).length)
const sinSubir = computed(() => lote.value.filter(i => !i.url).length)
const fallidas = computed(() => lote.value.filter(i => i.estado === 'errorSubida').length)
const ultimaImagen = computed(() => indiceActual.value === lote.value.length - 1)
const soloLectura = computed(() => !esEdicion.value && actual.value?.estado === 'guardada')

function datosVacios(tituloPrefill = '') {
  return {
    titulo: tituloPrefill,
    descripcion: '',
    idTipoArte: '',
    contenidoSensible: '',
    etiquetas: [],
    materiales: [],
  }
}

const tiposArte = ref([])
const tiposCargando = ref(false)
const tiposError = ref('')

async function cargarTiposArte() {
  tiposCargando.value = true
  tiposError.value = ''
  try {
    const res = await api.get('/tipos-arte')
    tiposArte.value = res.data
    const idActual = lote.value[0]?.datos?.idTipoArte
    const tipoPropio = imagenEditando.value?.tipoArte
    if (idActual && tipoPropio && !tiposArte.value.some(t => t.idTipoArte === Number(idActual))) {
      tiposArte.value = [...tiposArte.value, { ...tipoPropio, activo: false }]
    }
  } catch (e) {
    tiposError.value = mensajeDeError(e, 'No se pudo cargar el tipo de arte.')
  } finally {
    tiposCargando.value = false
  }
}

function tipoArtePorId(id) {
  return tiposArte.value.find(t => t.idTipoArte === Number(id)) || null
}

const idMedioActual = computed(() => {
  const id = actual.value?.datos?.idTipoArte
  if (!id) return 0
  return Number(tipoArtePorId(id)?.idMedio) || 0
})

function nombreTipoArte(id) {
  return tipoArtePorId(id)?.nombre || '—'
}

function onTipoArteCambiado() {
  const item = actual.value
  if (!item) return
  const idMedio = idMedioActual.value
  item.datos.materiales = item.datos.materiales.filter(m => Number(m.idMedio) === idMedio)
}

function crearItem({ archivo = null, preview = '', url = '', datos = null } = {}) {
  return {
    id: siguienteId++,
    archivo,
    preview,
    url,
    estado: 'pendiente',
    error: '',
    datos: datos || datosVacios(),
  }
}

function mensajeDeError(e, porDefecto = 'No se pudo guardar la imagen.') {
  return e.isAxiosError
    ? (e.response?.data?.message || 'Ocurrió un error, inténtalo más tarde.')
    : (e.message || porDefecto)
}

function irAlPortfolio() {
  const nombrePropio = localStorage.getItem('nombreUsuario') || ''
  if (nombrePropio) {
    router.push({ name: 'perfil', params: { nombreUsuario: nombrePropio } })
  } else {
    router.push({ name: 'inicio' })
  }
}

function cancelar() {
  irAlPortfolio()
}

// ---------------------------------------------------------------
// Textos de cabecera
// ---------------------------------------------------------------

const tituloPagina = computed(() => {
  if (esEdicion.value) return 'Editar imagen del portfolio'
  if (vista.value === 'seleccion') return 'Sube tus obras al portfolio'
  if (vista.value === 'subiendo') return 'Subiendo tus imágenes…'
  return 'Completa los datos de tus obras'
})

const subtituloPagina = computed(() => {
  if (esEdicion.value) {
    return 'Actualiza los datos de la imagen. La URL se conserva si no seleccionas un archivo nuevo.'
  }
  if (vista.value === 'seleccion') {
    return `Elige una o varias imágenes a la vez (hasta ${MAX_IMAGENES_POR_LOTE}). Después completas los datos de cada una.`
  }
  if (vista.value === 'subiendo') {
    return 'Optimizando y subiendo cada imagen. Puedes seguir navegando mientras tanto.'
  }
  if (lote.value.length === 1) {
    return 'Completa los datos de la obra y pulsa Publicar.'
  }
  return 'Revisa cada imagen antes de publicarla. Puedes saltar entre ellas con las miniaturas.'
})

// ---------------------------------------------------------------
// Selección de archivos
// ---------------------------------------------------------------

function agregarArchivos(files) {
  if (!files.length) return
  const problemas = []
  const validos = []
  for (const archivo of files) {
    const problema = validarImagen(archivo)
    if (problema) {
      problemas.push(`${archivo.name}: ${problema}`)
    } else {
      validos.push(archivo)
    }
  }

  const disponibles = MAX_IMAGENES_POR_LOTE - lote.value.length
  let aAgregar = validos
  if (validos.length > disponibles) {
    problemas.push(`Máximo ${MAX_IMAGENES_POR_LOTE} imágenes por lote; se omitieron ${validos.length - disponibles}.`)
    aAgregar = validos.slice(0, Math.max(0, disponibles))
  }

  for (const archivo of aAgregar) {
    lote.value.push(crearItem({
      archivo,
      preview: URL.createObjectURL(archivo),
      datos: datosVacios(nombreSinExtension(archivo)),
    }))
  }

  errorSeleccion.value = problemas.join(' · ')
}

function onSeleccionChange(event) {
  agregarArchivos([...(event.target.files || [])])
  event.target.value = ''
}

function onDrop(event) {
  arrastrando.value = false
  agregarArchivos([...(event.dataTransfer?.files || [])])
}

function quitarDelLote(item) {
  if (item.preview.startsWith('blob:')) URL.revokeObjectURL(item.preview)
  const indice = lote.value.indexOf(item)
  if (indice === -1) return
  lote.value.splice(indice, 1)
  if (!lote.value.length) {
    vista.value = 'seleccion'
    indiceActual.value = 0
    return
  }
  if (indiceActual.value >= lote.value.length) {
    indiceActual.value = lote.value.length - 1
  }
}

// ---------------------------------------------------------------
// Subida de archivos (fase previa al wizard)
// ---------------------------------------------------------------

function subirArchivo(aSubir, alProgreso = () => {}) {
  const form = new FormData()
  form.append('archivo', aSubir)
  return api
    .post('/upload', form, {
      onUploadProgress: (e) => {
        if (e.total) alProgreso(Math.round((e.loaded * 100) / e.total))
      },
    })
    .then((res) => res.data.url)
}

async function iniciarSubida() {
  if (!lote.value.length || subiendoLote.value) return
  vista.value = 'subiendo'
  await subirLote()
}

async function subirLote() {
  if (subiendoLote.value) return
  subiendoLote.value = true
  errorSeleccion.value = ''
  const totalASubir = sinSubir.value
  avisoMostrar('Subiendo tus imágenes…', 'Esto puede tardar un momento. Puedes seguir navegando.')
  try {
    let contador = 0
    for (const item of lote.value) {
      if (item.url) continue
      contador++
      subidaActual.value = contador
      progresoSubida.value = 0
      avisoFase('subiendo', 0)
      avisoDetalle(`Subiendo imagen ${contador} de ${totalASubir}…`)
      try {
        item.estado = 'pendiente'
        item.error = ''
        const optimizado = await procesarImagen(item.archivo, LIMITES_IMAGEN.obra)
        item.url = await subirArchivo(optimizado, (pct) => {
          progresoSubida.value = pct
          avisoFase('subiendo', pct)
        })
      } catch (e) {
        item.estado = 'errorSubida'
        item.error = mensajeDeError(e)
      }
    }

    if (fallidas.value > 0) {
      avisoFallo(`${fallidas.value} de ${lote.value.length} imágenes no se pudieron subir.`)
      return
    }

    avisoOcultar()
    indiceActual.value = 0
    error.value = ''
    vista.value = 'datos'
  } finally {
    subiendoLote.value = false
    progresoSubida.value = 0
    subidaActual.value = 0
  }
}

function volverASeleccion() {
  vista.value = 'seleccion'
}

// ---------------------------------------------------------------
// Wizard
// ---------------------------------------------------------------

function irAIndice(indice) {
  indiceActual.value = indice
  error.value = ''
}

function anterior() {
  if (indiceActual.value > 0) irAIndice(indiceActual.value - 1)
}

function navegarASiguientePendiente() {
  const actualIdx = indiceActual.value
  let destino = -1
  for (let i = actualIdx + 1; i < lote.value.length; i++) {
    if (lote.value[i].estado === 'pendiente') {
      destino = i
      break
    }
  }
  if (destino === -1) {
    destino = lote.value.findIndex((it, i) => i !== actualIdx && it.estado === 'pendiente')
  }
  if (destino !== -1) irAIndice(destino)
}

function validarDatos(datos) {
  if (!datos.titulo.trim()) return 'El título es obligatorio.'
  if (!datos.descripcion.trim()) return 'La descripción es obligatoria.'
  if (!datos.idTipoArte) return 'Selecciona el tipo de arte de la obra.'
  return ''
}

function cuerpoDesde(item) {
  const d = item.datos
  return {
    titulo: d.titulo,
    descripcion: d.descripcion,
    url: item.url,
    idTipoArte: Number(d.idTipoArte),
    contenidoSensible: d.contenidoSensible || null,
    idsEtiqueta: d.etiquetas.map(e => e.idEtiqueta),
    idsMaterial: d.materiales.map(m => m.idMaterial),
  }
}

async function accionPrincipal() {
  if (soloLectura.value) {
    navegarASiguientePendiente()
    return
  }
  if (esEdicion.value) {
    await guardarEdicion()
  } else {
    await guardarPublicar()
  }
}

async function guardarEdicion() {
  const item = actual.value
  if (!item) return
  const problemaArchivo = item.archivo ? validarImagen(item.archivo) : ''
  if (problemaArchivo) {
    error.value = problemaArchivo
    return
  }
  const problemaDatos = validarDatos(item.datos)
  if (problemaDatos) {
    error.value = problemaDatos
    return
  }

  guardando.value = true
  itemEnGuardado.value = item
  error.value = ''
  progreso.value = 0
  const conImagen = !!item.archivo
  if (conImagen) {
    avisoMostrar('Actualizando tu obra…', 'Esto puede tardar un momento. Puedes seguir navegando.')
  }
  try {
    const idUsuario = localStorage.getItem('idUsuario')
    if (item.archivo) {
      fase.value = 'procesando'
      avisoFase('procesando')
      avisoDetalle('Procesando la imagen…')
      const optimizado = await procesarImagen(item.archivo, LIMITES_IMAGEN.obra)
      fase.value = 'subiendo'
      avisoFase('subiendo', 0)
      item.url = await subirArchivo(optimizado, (pct) => {
        progreso.value = pct
        avisoFase('subiendo', pct)
      })
    }
    fase.value = 'guardando'
    if (conImagen) {
      avisoFase('guardando')
      avisoDetalle('Guardando los cambios…')
    }

    const res = await api.put(
      `/portfolio/usuarios/${idUsuario}/imagenes/${imagenEditando.value.idImagen}`,
      cuerpoDesde(item),
    )
    window.dispatchEvent(new CustomEvent('portfolio-actualizado', {
      detail: { accion: 'editar', imagen: res.data },
    }))
    item.estado = 'guardada'
    item.archivo = null
    cerrar()
    if (conImagen) avisoExito('Tu obra se actualizó en el portfolio.')
    irAlPortfolio()
  } catch (e) {
    error.value = mensajeDeError(e)
    if (conImagen) avisoFallo(error.value)
  } finally {
    guardando.value = false
    itemEnGuardado.value = null
    fase.value = ''
    progreso.value = 0
  }
}

async function guardarPublicar() {
  const item = actual.value
  if (!item) return
  const problemaDatos = validarDatos(item.datos)
  if (problemaDatos) {
    error.value = problemaDatos
    return
  }
  if (!item.url) {
    error.value = 'Falta subir la imagen.'
    return
  }

  guardando.value = true
  itemEnGuardado.value = item
  error.value = ''
  avisoMostrar('Publicando tus obras…', '')
  avisoFase('guardando')
  avisoDetalle(`Publicando imagen ${guardadas.value + 1} de ${lote.value.length}…`)
  try {
    const idUsuario = localStorage.getItem('idUsuario')
    const res = await api.post(
      `/portfolio/usuarios/${idUsuario}/imagenes`,
      cuerpoDesde(item),
    )
    item.estado = 'guardada'
    item.error = ''
    window.dispatchEvent(new CustomEvent('portfolio-actualizado', {
      detail: { accion: 'crear', imagen: res.data },
    }))

    const quedanPendientes = lote.value.some(i => i.estado === 'pendiente')
    if (!quedanPendientes) {
      const total = lote.value.length
      cerrar()
      avisoExito(total === 1
        ? 'Tu obra se publicó en tu portfolio.'
        : `¡${total} obras publicadas en tu portfolio!`)
      irAlPortfolio()
      return
    }

    navegarASiguientePendiente()
    error.value = ''
    const faltan = lote.value.length - guardadas.value
    avisoExito(guardadas.value === 1
      ? `Se publicó 1 imagen. Faltan ${faltan} por publicar.`
      : `Se publicaron ${guardadas.value} imágenes. Faltan ${faltan} por publicar.`)
  } catch (e) {
    error.value = mensajeDeError(e)
    item.error = error.value
    avisoFallo(error.value)
  } finally {
    guardando.value = false
    itemEnGuardado.value = null
  }
}

// ---------------------------------------------------------------
// Ciclo de vida
// ---------------------------------------------------------------

function onImagenEditChange(event) {
  const seleccionada = event.target.files[0] || null
  const problema = seleccionada ? validarImagen(seleccionada) : ''
  if (problema) {
    event.target.value = ''
    error.value = problema
    return
  }
  error.value = ''
  const item = actual.value
  if (!item) return
  item.archivo = seleccionada
  item.preview = seleccionada ? URL.createObjectURL(seleccionada) : item.url
}

onMounted(() => {
  error.value = ''
  errorSeleccion.value = ''
  guardando.value = false
  lote.value = []
  indiceActual.value = 0
  vista.value = 'seleccion'
  cargarTiposArte()

  if (estado.modo === 'editar' && estado.imagen) {
    const img = estado.imagen
    imagenEditando.value = img
    lote.value = [crearItem({
      preview: img.url,
      url: img.url,
      datos: {
        titulo: img.titulo,
        descripcion: img.descripcion,
        idTipoArte: img.tipoArte?.idTipoArte || '',
        contenidoSensible: img.contenidoSensible || '',
        etiquetas: [...(img.etiquetas || [])],
        materiales: [...(img.materiales || [])],
      },
    })]
    vista.value = 'datos'
  }
})

onBeforeUnmount(() => {
  if (['procesando', 'subiendo', 'guardando'].includes(avisoEstado.fase)) {
    avisoOcultar()
  }
  const sinPublicar = urlsSinPublicar()
  if (sinPublicar.length) {
    api.delete('/upload', { data: sinPublicar }).catch(() => {})
  }
  lote.value.forEach((item) => {
    if (item.preview.startsWith('blob:')) URL.revokeObjectURL(item.preview)
  })
  cerrar()
  subidaEnCurso.value = false
  publicacionesPendientes.value = 0
})

const etiquetaBoton = computed(() => {
  if (guardando.value) return 'Guardando…'
  if (esEdicion.value) {
    if (fase.value === 'procesando') return 'Procesando imagen…'
    if (fase.value === 'subiendo') return `Subiendo… ${progreso.value}%`
    if (fase.value === 'guardando') return 'Guardando…'
    return 'Guardar'
  }
  if (soloLectura.value) return 'Siguiente →'
  return ultimaImagen.value ? 'Publicar' : 'Guardar y siguiente →'
})

watchEffect(() => {
  subidaEnCurso.value = subiendoLote.value
  if (esEdicion.value) {
    publicacionesPendientes.value = actual.value?.archivo ? 1 : 0
    publicadasEnSesion.value = 0
  } else {
    publicacionesPendientes.value = lote.value.filter(
      i => i.url && i.estado !== 'guardada',
    ).length
    publicadasEnSesion.value = guardadas.value
  }
})

function urlsSinPublicar() {
  const urls = []
  const urlOriginal = imagenEditando.value?.url
  for (const item of lote.value) {
    if (!item.url) continue
    if (item.estado === 'guardada') continue
    if (guardando.value && item === itemEnGuardado.value) continue
    if (esEdicion.value && item.url === urlOriginal) continue
    urls.push(item.url)
  }
  return urls
}
</script>

<template>
  <div class="w-full">
    <div class="mx-auto px-4 sm:px-8 py-8">
      <button
        type="button"
        class="text-sm text-granate hover:text-coral transition"
        @click="cancelar"
      >
        ← Volver al portfolio
      </button>

      <h1 class="mt-4 text-2xl text-granate">{{ tituloPagina }}</h1>
      <p class="mt-1 mb-6 text-sm text-cafe/60">{{ subtituloPagina }}</p>

      <!-- ==================================================== -->
      <!-- Vista: selección (solo creación)                     -->
      <!-- ==================================================== -->
      <template v-if="!esEdicion && vista === 'seleccion'">
        <div
          class="rounded-xl border-2 border-dashed p-10 text-center transition"
          :class="arrastrando ? 'border-coral bg-coral/10' : 'border-rosa bg-rosa/10'"
          @dragover.prevent="arrastrando = true"
          @dragleave.prevent="arrastrando = false"
          @drop.prevent="onDrop"
        >
          <p class="text-sm text-cafe/70 mb-4">Arrastra tus imágenes hasta aquí</p>
          <input
            id="portfolio-lote"
            type="file"
            :accept="acceptImagenes"
            multiple
            class="sr-only"
            @change="onSeleccionChange"
          />
          <label
            for="portfolio-lote"
            class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral px-4 py-2 text-sm text-white transition hover:bg-rojizo"
          >
            Elegir imágenes
          </label>
          <p class="mt-4 text-xs text-cafe/50">
            JPG, PNG o WEBP · hasta 5MB cada una · máximo {{ MAX_IMAGENES_POR_LOTE }} por lote
          </p>
        </div>

        <p v-if="errorSeleccion" class="mt-3 text-sm text-rojizo">{{ errorSeleccion }}</p>

        <div v-if="lote.length" class="mt-6">
          <p class="text-sm text-cafe/70 mb-3">
            {{ lote.length }} imagen{{ lote.length === 1 ? '' : 'es' }} seleccionada{{ lote.length === 1 ? '' : 's' }}
          </p>

          <ul class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-3">
            <li v-for="item in lote" :key="item.id" class="relative">
              <div class="aspect-square rounded-lg overflow-hidden bg-rosa/20 border border-rosa">
                <img :src="item.preview" :alt="item.datos.titulo" class="w-full h-full object-cover" />
              </div>
              <button
                type="button"
                class="absolute top-1 right-1 w-6 h-6 rounded-full bg-white/90 text-cafe hover:text-rojizo flex items-center justify-center text-xs leading-none transition"
                :aria-label="`Quitar ${item.datos.titulo}`"
                @click="quitarDelLote(item)"
              >
                ✕
              </button>
            </li>
          </ul>

          <div class="mt-6 flex items-center justify-end gap-3">
            <button
              type="button"
              class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
              @click="cancelar"
            >
              Cancelar
            </button>
            <button
              type="button"
              :disabled="subiendoLote"
              class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
              @click="iniciarSubida"
            >
              Subir imágenes ({{ lote.length }})
            </button>
          </div>
        </div>
      </template>

      <!-- ==================================================== -->
      <!-- Vista: subida de archivos                             -->
      <!-- ==================================================== -->
      <template v-else-if="!esEdicion && vista === 'subiendo'">
        <div class="bg-blanco border border-rosa rounded-xl p-6">
          <div class="flex items-center justify-between gap-4 mb-3">
            <p class="text-sm text-cafe/70">
              <template v-if="subiendoLote">
                Subiendo imagen {{ subidaActual }} de {{ sinSubir + subidaActual }}…
              </template>
              <template v-else-if="fallidas">
                {{ fallidas }} imagen{{ fallidas === 1 ? '' : 'es' }} no se pudieron subir.
              </template>
              <template v-else>
                {{ subidas }} de {{ lote.length }} imágenes subidas
              </template>
            </p>
            <p v-if="subiendoLote" class="text-sm text-granate font-medium">{{ progresoSubida }}%</p>
          </div>

          <div class="h-1.5 w-full bg-rosa rounded-full overflow-hidden">
            <div
              class="h-full bg-coral transition-all duration-150"
              :style="{ width: (subiendoLote ? progresoSubida : (lote.length ? (subidas * 100) / lote.length : 0)) + '%' }"
            ></div>
          </div>

          <ul class="mt-5 grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-3">
            <li v-for="(item, i) in lote" :key="item.id" class="relative">
              <div class="aspect-square rounded-lg overflow-hidden bg-rosa/20 border border-rosa">
                <img :src="item.preview" :alt="item.datos.titulo" class="w-full h-full object-cover" :class="item.url ? '' : 'opacity-50'" />
              </div>
              <span
                v-if="item.estado === 'errorSubida'"
                class="absolute inset-0 flex items-center justify-center bg-rojizo/20 text-rojizo font-bold"
                :title="item.error"
              >
                ✕
              </span>
              <span
                v-else-if="item.url"
                class="absolute bottom-0.5 right-0.5 text-xs bg-white/90 rounded px-1 text-green-700 leading-none"
              >
                ✓
              </span>
              <span
                v-else-if="subiendoLote && i === lote.findIndex(it => !it.url)"
                class="absolute inset-0 flex items-center justify-center bg-white/60"
              >
                <span class="inline-block w-5 h-5 border-2 border-coral border-t-transparent rounded-full animate-spin"></span>
              </span>
            </li>
          </ul>

          <div v-if="!subiendoLote && fallidas" class="mt-6 space-y-1">
            <p v-for="item in lote.filter(it => it.estado === 'errorSubida')" :key="item.id" class="text-xs text-rojizo">
              {{ item.datos.titulo }}: {{ item.error }}
            </p>
          </div>

          <div class="mt-6 flex items-center justify-end gap-3">
            <button
              v-if="!subiendoLote"
              type="button"
              class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
              @click="volverASeleccion"
            >
              Volver a selección
            </button>
            <button
              v-if="!subiendoLote && fallidas"
              type="button"
              class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
              @click="subirLote"
            >
              Reintentar fallidas
            </button>
          </div>
        </div>
      </template>

      <!-- ==================================================== -->
      <!-- Vista: datos (wizard de creación o edición)          -->
      <!-- ==================================================== -->
      <template v-else-if="actual">
        <!-- Rail de miniaturas (creación con varias imágenes) -->
        <div v-if="!esEdicion && lote.length > 1" class="mb-5">
          <div class="flex flex-wrap items-center justify-between gap-2 mb-2">
            <p class="text-sm text-cafe/70">
              Imagen {{ indiceActual + 1 }} de {{ lote.length }}
              <span v-if="guardadas"> · {{ guardadas }} publicada{{ guardadas === 1 ? '' : 's' }}</span>
            </p>
            <p class="text-xs text-cafe/50">Pulsa una miniatura para saltar a otra imagen</p>
          </div>
          <div class="flex gap-2 overflow-x-auto pb-1">
            <button
              v-for="(item, i) in lote"
              :key="item.id"
              type="button"
              class="relative w-16 h-16 shrink-0 rounded-lg overflow-hidden border-2 transition"
              :class="i === indiceActual ? 'border-coral ring-1 ring-coral' : 'border-rosa hover:border-coral/50'"
              :aria-label="`Ir a la imagen ${i + 1}`"
              @click="irAIndice(i)"
            >
              <img :src="item.preview" :alt="item.datos.titulo" class="w-full h-full object-cover" />
              <span class="absolute top-0.5 left-0.5 text-[10px] bg-white/85 rounded px-1 text-cafe leading-none">
                {{ i + 1 }}
              </span>
              <span
                v-if="item.estado === 'guardada'"
                class="absolute bottom-0.5 right-0.5 text-xs bg-white/90 rounded px-1 text-green-700 leading-none"
              >
                ✓
              </span>
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 bg-blanco border border-rosa rounded-xl p-6">
          <!-- Columna izquierda -->
          <div class="space-y-5 min-w-0">
            <!-- Resumen de solo lectura para imágenes ya publicadas -->
            <template v-if="soloLectura">
              <p class="inline-flex items-center gap-2 text-sm font-medium text-green-700">
                ✓ Publicada
              </p>
              <div>
                <p class="text-xs text-cafe/60">Título</p>
                <p class="text-granate">{{ actual.datos.titulo }}</p>
              </div>
              <div>
                <p class="text-xs text-cafe/60">Descripción</p>
                <p class="text-cafe whitespace-pre-wrap">{{ actual.datos.descripcion }}</p>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <p class="text-xs text-cafe/60">Tipo de arte</p>
                  <p class="text-cafe">{{ nombreTipoArte(actual.datos.idTipoArte) }}</p>
                </div>
                <div>
                  <p class="text-xs text-cafe/60">Contenido sensible</p>
                  <p class="text-cafe">{{ actual.datos.contenidoSensible || 'No' }}</p>
                </div>
              </div>
              <div>
                <p class="text-xs text-cafe/60">Materiales y herramientas</p>
                <p class="text-cafe">
                  {{ actual.datos.materiales.length ? actual.datos.materiales.map(m => m.nombre).join(', ') : '—' }}
                </p>
              </div>
              <div>
                <p class="text-xs text-cafe/60">Etiquetas</p>
                <p class="text-cafe">
                  {{ actual.datos.etiquetas.length ? actual.datos.etiquetas.map(e => e.nombre).join(', ') : '—' }}
                </p>
              </div>
            </template>

            <!-- Formulario editable -->
            <template v-else>
              <div>
                <label class="block text-sm text-cafe mb-2">Título <span class="text-rojizo">*</span></label>
                <input v-model="actual.datos.titulo" type="text" maxlength="100" required class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
              </div>

              <div>
                <label class="block text-sm text-cafe mb-2">Descripción <span class="text-rojizo">*</span></label>
                <textarea v-model="actual.datos.descripcion" rows="5" maxlength="1000" required class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"></textarea>
              </div>

              <div>
                <label class="block text-sm text-cafe mb-2">Tipo de arte <span class="text-rojizo">*</span></label>
                <select
                  v-model="actual.datos.idTipoArte"
                  :disabled="tiposCargando || !!tiposError"
                  class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none disabled:opacity-60"
                  @change="onTipoArteCambiado"
                >
                  <option value="" disabled>{{ tiposCargando ? 'Cargando tipos de arte...' : 'Selecciona un tipo de arte' }}</option>
                  <option v-for="t in tiposArte" :key="t.idTipoArte" :value="t.idTipoArte">
                    {{ t.nombre }}{{ t.activo === false ? ' (inactivo)' : '' }}
                  </option>
                </select>
                <p v-if="tiposError" class="text-xs text-rojizo mt-1">{{ tiposError }}</p>
              </div>

              <div>
                <label class="block text-sm text-cafe mb-2">Contenido sensible</label>
                <select v-model="actual.datos.contenidoSensible" class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                  <option value="">Sin contenido sensible</option>
                  <option value="VIOLENCIA">Violencia</option>
                  <option value="DESNUDOS">Desnudos</option>
                  <option value="GENERAL">Contenido sensible general</option>
                </select>
              </div>

              <SelectorMateriales v-model="actual.datos.materiales" :id-medio="idMedioActual" />

              <SelectorEtiquetas v-model="actual.datos.etiquetas" />

              <p v-if="error" class="text-rojizo text-sm">{{ error }}</p>
            </template>
          </div>

          <!-- Columna derecha: vista previa -->
          <div class="flex flex-col min-w-0">
            <label class="block text-sm text-cafe mb-2">Imagen</label>
            <div class="flex-1 min-h-80 rounded-xl border border-dashed border-rosa bg-rosa/10 flex items-center justify-center overflow-hidden">
              <img
                v-if="actual.preview"
                :src="actual.preview"
                alt="Vista previa"
                class="w-full h-full max-h-128 object-contain"
              />
              <p v-else class="px-6 text-center text-sm text-cafe/50">Selecciona una imagen para ver la vista previa.</p>
            </div>

            <template v-if="esEdicion">
              <input
                id="portfolio-imagen"
                type="file"
                :accept="acceptImagenes"
                class="sr-only"
                @change="onImagenEditChange"
              />
              <label
                for="portfolio-imagen"
                class="mt-4 inline-flex cursor-pointer justify-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm text-granate hover:bg-coral/20 transition"
              >
                {{ actual.archivo ? 'Cambiar imagen' : 'Cambiar imagen (opcional)' }}
              </label>
            </template>
            <p v-else class="mt-3 text-xs text-cafe/50">
              Imagen optimizada y lista para publicar.
            </p>
          </div>
        </div>

        <!-- Acciones -->
        <div class="flex flex-wrap items-center justify-between gap-3 mt-6">
          <button
            type="button"
            class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
            :disabled="guardando"
            @click="cancelar"
          >
            Cancelar
          </button>

          <div class="flex items-center gap-3">
            <button
              v-if="!esEdicion && indiceActual > 0"
              type="button"
              class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
              :disabled="guardando"
              @click="anterior"
            >
              ← Anterior
            </button>
            <button
              type="button"
              :disabled="guardando || subiendoLote"
              class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm min-w-48"
              @click="accionPrincipal"
            >
              {{ etiquetaBoton }}
            </button>
          </div>
        </div>

        <div v-if="fase === 'subiendo' && esEdicion" class="mt-4 h-1.5 w-full bg-rosa rounded-full overflow-hidden">
          <div class="h-full bg-coral transition-all duration-150" :style="{ width: progreso + '%' }"></div>
        </div>
      </template>
    </div>
  </div>
</template>
