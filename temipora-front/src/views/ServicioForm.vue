<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, validarImagen, procesarImagen, LIMITES_IMAGEN } from '../servicios/archivos'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import BarraGuardado from '../components/BarraGuardado.vue'

const route = useRoute()
const router = useRouter()

const idServicio = computed(() => (route.params.id ? Number(route.params.id) : null))
const esEdicion = computed(() => idServicio.value !== null)

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const errorGuardado = ref('')
const subiendoPortada = ref(false)
const subiendoEjemplo = ref(false)
const estadoGuardado = ref(null)

const tipos = ref([])
const medios = ref([])

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

const form = ref({
  nombre: '',
  idTipoServicio: '',
  idMedio: '',
  descripcion: '',
  precioBase: '',
  contenidoSensible: false,
  contenidosSensibles: [],
  tiempoEntregaValor: '',
  tiempoEntregaUnidad: 'DIAS',
  cupos: '',
  habilitarCuposTrasEntrega: false,
  imagenPortada: '',
  imagenes: [],
})

const tituloPagina = computed(() => (esEdicion.value ? 'Editar servicio' : 'Crear servicio'))
const subtituloPagina = computed(() => 'Completá los datos de tu servicio.')

const nombreTipo = computed(() => tipos.value.find((t) => String(t.idTipoServicio) === form.value.idTipoServicio)?.nombre || '')
const nombreMedio = computed(() => medios.value.find((m) => String(m.idMedio) === form.value.idMedio)?.nombre || '')

function instantanea() {
  return JSON.stringify(form.value)
}
const hayCambios = computed(() => estadoGuardado.value !== null && estadoGuardado.value !== instantanea())

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [tiposRes, mediosRes] = await Promise.all([
      api.get('/tipos-servicio'),
      api.get('/medios'),
    ])
    tipos.value = tiposRes.data
    medios.value = mediosRes.data

    if (esEdicion.value) {
      const res = await api.get(`/servicios/${idServicio.value}`)
      const d = res.data
      form.value = {
        nombre: d.nombre || '',
        idTipoServicio: d.idTipoServicio ? String(d.idTipoServicio) : '',
        idMedio: d.idMedio ? String(d.idMedio) : '',
        descripcion: d.descripcion || '',
        precioBase: d.precioBase ?? '',
        contenidoSensible: !!d.contenidoSensible,
        contenidosSensibles: Array.isArray(d.contenidosSensibles) ? [...d.contenidosSensibles] : [],
        tiempoEntregaValor: d.tiempoEntregaValor ?? '',
        tiempoEntregaUnidad: d.tiempoEntregaUnidad || 'DIAS',
        cupos: d.cupos ?? '',
        habilitarCuposTrasEntrega: !!d.habilitarCuposTrasEntrega,
        imagenPortada: d.imagenPortada || '',
        imagenes: Array.isArray(d.imagenes) ? [...d.imagenes] : [],
      }
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

async function onPortadaChange(e) {
  const file = e.target.files[0] || null
  e.target.value = ''
  if (!file) return
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

async function onEjemplosChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
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

function quitarEjemplo(indice) {
  form.value.imagenes.splice(indice, 1)
}

function validar() {
  if (!form.value.nombre.trim()) return 'El nombre es obligatorio.'
  if (!form.value.idTipoServicio) return 'Elegí un tipo de servicio.'
  if (!form.value.idMedio) return 'Elegí un medio.'
  if (!form.value.descripcion.trim()) return 'La descripción es obligatoria.'
  if (form.value.precioBase === '' || Number(form.value.precioBase) < 0) return 'Ingresá un precio base válido.'
  if (!form.value.tiempoEntregaValor || Number(form.value.tiempoEntregaValor) < 1) return 'Ingresá el tiempo estimado de entrega.'
  if (form.value.cupos === '' || Number(form.value.cupos) < 0) return 'Ingresá la cantidad de cupos.'
  if (form.value.contenidoSensible && !form.value.contenidosSensibles.length) {
    return 'Marcá el servicio como sensible y elegí qué tipo de contenido puede aparecer.'
  }
  return ''
}

async function guardar() {
  errorGuardado.value = ''
  const problema = validar()
  if (problema) {
    errorGuardado.value = problema
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
      contenidoSensible: form.value.contenidoSensible,
      contenidosSensibles: form.value.contenidoSensible ? form.value.contenidosSensibles : [],
      tiempoEntregaValor: Number(form.value.tiempoEntregaValor),
      tiempoEntregaUnidad: form.value.tiempoEntregaUnidad,
      cupos: Number(form.value.cupos),
      habilitarCuposTrasEntrega: form.value.habilitarCuposTrasEntrega,
      imagenPortada: form.value.imagenPortada || null,
      imagenes: form.value.imagenes,
    }
    if (esEdicion.value) {
      await api.put(`/servicios/${idServicio.value}`, payload)
    } else {
      await api.post('/servicios', payload)
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
  <div class="mx-auto px-4 sm:px-8 py-8">
    <button type="button" class="text-sm text-granate hover:text-coral transition" @click="irAlListado">
      ← Volver a mis servicios
    </button>

    <h1 class="mt-4 text-2xl text-granate">{{ tituloPagina }}</h1>
    <p class="mt-1 mb-6 text-sm pb-5 text-cafe/60">{{ subtituloPagina }}</p>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_22rem] gap-8">
      <!-- Formulario -->
      <div class="space-y-10">
        <!-- Básicos -->
        <section>
          <h2 class="text-base text-granate mb-5">Datos básicos</h2>
          <div class="space-y-6">
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
            <div>
              <label class="block text-sm text-cafe mb-2">Precio base <span class="text-rojizo">*</span></label>
              <input v-model="form.precioBase" type="number" min="0" step="0.01"
                class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
            </div>
          </div>
        </section>

        <!-- Clasificación -->
        <section>
          <h2 class="text-base text-granate mb-5">Clasificación</h2>
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
        </section>

        <!-- Entrega y cupos -->
        <section>
          <h2 class="text-base text-granate mb-5">Entrega y cupos</h2>
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
        </section>

        <!-- Imágenes -->
        <section>
          <h2 class="text-base text-granate mb-5">Imágenes</h2>
          <div class="space-y-6">
            <div>
              <label class="block text-sm text-cafe mb-2">Imagen de portada</label>
              <div v-if="form.imagenPortada" class="mb-2 w-full h-40 rounded-lg overflow-hidden bg-rosa/20">
                <img :src="form.imagenPortada" alt="" class="w-full h-full object-cover" />
              </div>
              <input id="servicio-portada" type="file" :accept="acceptImagenes" class="sr-only" @change="onPortadaChange" />
              <label for="servicio-portada"
                class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate hover:bg-coral/20 transition">
                {{ subiendoPortada ? 'Subiendo…' : (form.imagenPortada ? 'Cambiar portada' : 'Subir portada') }}
              </label>
            </div>

            <div>
              <label class="block text-sm text-cafe mb-2">Imágenes de ejemplo</label>
              <div v-if="form.imagenes.length" class="mb-3 grid grid-cols-3 sm:grid-cols-4 gap-3">
                <div v-for="(url, i) in form.imagenes" :key="i" class="relative aspect-square rounded-lg overflow-hidden bg-rosa/20 border border-rosa">
                  <img :src="url" alt="" class="w-full h-full object-cover" />
                  <button type="button"
                    class="absolute top-1 right-1 w-6 h-6 rounded-full bg-white/90 text-cafe hover:text-rojizo flex items-center justify-center text-xs leading-none transition"
                    aria-label="Quitar imagen" @click="quitarEjemplo(i)">✕</button>
                </div>
              </div>
              <input id="servicio-ejemplos" type="file" :accept="acceptImagenes" multiple class="sr-only" @change="onEjemplosChange" />
              <label for="servicio-ejemplos"
                class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate hover:bg-coral/20 transition">
                {{ subiendoEjemplo ? 'Subiendo…' : 'Agregar imágenes' }}
              </label>
            </div>
          </div>
        </section>

        <!-- Formulario y términos -->
        <section class="rounded-xl border border-rosa/50 bg-rosa/5 p-4">
          <h2 class="text-base text-granate mb-1">Formulario y términos</h2>
          <p class="text-sm text-cafe/70">
            Por defecto este servicio usa tu <strong>formulario de pedidos</strong> y tus <strong>términos y condiciones</strong> globales.
            La personalización por servicio llegará en una próxima actualización.
          </p>
        </section>

        <BarraGuardado :hay-cambios="hayCambios" :guardando="guardando" :texto="esEdicion ? 'Guardar cambios' : 'Crear servicio'" @guardar="guardar">
          <template #aviso>
            <p v-if="errorGuardado" class="text-rojizo text-sm">{{ errorGuardado }}</p>
          </template>
        </BarraGuardado>
      </div>

      <!-- Resumen -->
      <aside class="lg:sticky lg:top-16 h-fit">
        <div class="rounded-2xl border border-rosa/50 bg-blanco overflow-hidden">
          <div class="h-40 bg-rosa/20">
            <img v-if="form.imagenPortada" :src="form.imagenPortada" alt="" class="w-full h-full object-cover" />
          </div>
          <div class="p-4 space-y-2 text-sm">
            <p class="text-lg text-granate">{{ form.nombre || 'Nombre del servicio' }}</p>
            <p class="text-cafe/60">{{ nombreTipo || 'Tipo' }} · {{ nombreMedio || 'Medio' }}</p>
            <p class="text-cafe/80 whitespace-pre-wrap">{{ form.descripcion || 'Descripción…' }}</p>
            <div class="pt-2 space-y-1 text-cafe/70">
              <p>Precio: <span class="text-granate">{{ form.precioBase !== '' ? form.precioBase : '—' }}</span></p>
              <p>Entrega: {{ form.tiempoEntregaValor ? `${form.tiempoEntregaValor} ${UNIDADES.find(u => u.valor === form.tiempoEntregaUnidad)?.texto?.toLowerCase()}` : '—' }}</p>
              <p>Cupos: {{ form.cupos !== '' ? form.cupos : '—' }}</p>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
