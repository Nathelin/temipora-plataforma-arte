<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { useMonedas } from '../composables/useMonedas'
import { acceptImagenes, validarImagen, procesarImagen, LIMITES_IMAGEN } from '../servicios/archivos'
import { useAvisoActualizacion } from '../composables/useAvisoActualizacion'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import BarraGuardado from '../components/BarraGuardado.vue'

const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorEdicion = ref('')
const fase = ref('')
const progreso = ref(0)

const etiquetaBoton = computed(() => {
  if (fase.value === 'procesando') return 'Procesando imagen…'
  if (fase.value === 'subiendo-foto') return `Subiendo foto… ${progreso.value}%`
  if (fase.value === 'subiendo-banner') return `Subiendo banner… ${progreso.value}%`
  if (fase.value === 'guardando') return 'Guardando…'
  return 'Guardar cambios'
})

const form = ref({
  nombreDisplay: '',
  biografia: '',
  sexo: '',
  idPais: '',
  urlFotoPerfil: '',
  urlBanner: '',
  idMonedaPreferida: '',
})

const paises = ref([])
const { monedas, cargar: cargarMonedas } = useMonedas()

const SEXOS_VALIDOS = ['Masculino', 'Femenino', 'Otro']

function normalizarSexo(valor) {
  const sexo = (valor || '').toString().trim()
  if (!sexo) return null
  if (sexo.toLowerCase() === 'no especificado') return null
  return SEXOS_VALIDOS.includes(sexo) ? sexo : null
}

function sexoParaFormulario(valor) {
  return normalizarSexo(valor) || ''
}

const editFoto = ref(null)
const editBanner = ref(null)
const previewFoto = ref('')
const previewBanner = ref('')
const idUsuario = ref(null)
const arrastrandoFoto = ref(false)
const arrastrandoBanner = ref(false)
const estadoGuardado = ref(null)

function datosComparables() {

  return JSON.stringify({
    nombreDisplay: form.value.nombreDisplay,
    biografia: form.value.biografia,
    sexo: sexoParaFormulario(form.value.sexo),
    idPais: form.value.idPais,
    idMonedaPreferida: form.value.idMonedaPreferida,
  })
}

const hayCambios = computed(() => {
  if (editFoto.value || editBanner.value) return true
  if (estadoGuardado.value === null) return false
  return estadoGuardado.value !== datosComparables()
})

const { actualizar: marcarCambios, limpiar: limpiarCambios } = useCambiosSinGuardar()

watch(hayCambios, (pendiente) => {
  marcarCambios(pendiente, 'Tenés cambios sin guardar en tu perfil. Si salís, se perderán.')
})

const {
  mostrar: avisoMostrar,
  fase: avisoFase,
  detalle: avisoDetalle,
  exito: avisoExito,
  fallo: avisoFallo,
} = useAvisoActualizacion()

function irAPerfil() {
  router.push({ name: 'perfil', params: { nombreUsuario } })
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    await cargarMonedas()
    const res = await api.get(`/perfil/${nombreUsuario}`)
    const d = res.data
    idUsuario.value = d.idUsuario
    form.value = {
      nombreDisplay: d.nombreDisplay || '',
      biografia: d.biografia || '',
      sexo: sexoParaFormulario(d.sexo),
      idPais: d.idPais ? String(d.idPais) : '',
      urlFotoPerfil: d.urlFotoPerfil || '',
      urlBanner: d.urlBanner || '',
      idMonedaPreferida: d.idMonedaPreferida != null
        ? String(d.idMonedaPreferida)
        : (monedas.value.find((m) => m.codigo === d.monedaPreferida)?.idMoneda ?? ''),
    }
    previewFoto.value = ''
    previewBanner.value = ''
    estadoGuardado.value = datosComparables()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el perfil.'
  } finally {
    cargando.value = false
  }
}

async function cargarPaises() {
  try {
    const res = await api.get('/paises')
    paises.value = res.data
  } catch (e) {
    paises.value = []
  }
}

function aplicarFoto(f) {
  const problema = f ? validarImagen(f) : ''
  if (problema) {
    editFoto.value = null
    previewFoto.value = ''
    errorEdicion.value = problema
    return
  }
  errorEdicion.value = ''
  editFoto.value = f
  previewFoto.value = f ? URL.createObjectURL(f) : ''
}

function aplicarBanner(f) {
  const problema = f ? validarImagen(f) : ''
  if (problema) {
    editBanner.value = null
    previewBanner.value = ''
    errorEdicion.value = problema
    return
  }
  errorEdicion.value = ''
  editBanner.value = f
  previewBanner.value = f ? URL.createObjectURL(f) : ''
}

function onFotoChange(e) {
  aplicarFoto(e.target.files[0] || null)
  e.target.value = ''
}

function onBannerChange(e) {
  aplicarBanner(e.target.files[0] || null)
  e.target.value = ''
}

function onDropFoto(e) {
  arrastrandoFoto.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) aplicarFoto(f)
}

function onDropBanner(e) {
  arrastrandoBanner.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) aplicarBanner(f)
}

async function subirArchivo(archivo, limite, faseSubida, etiqueta) {
  fase.value = 'procesando'
  avisoFase('procesando')
  avisoDetalle('Procesando la imagen…')
  const optimizado = await procesarImagen(archivo, limite)
  fase.value = faseSubida
  avisoFase('subiendo', 0)
  avisoDetalle(etiqueta)
  progreso.value = 0
  const fd = new FormData()
  fd.append('archivo', optimizado)
  const res = await api.post('/upload', fd, {
    onUploadProgress: (e) => {
      if (e.total) {
        const pct = Math.round((e.loaded * 100) / e.total)
        progreso.value = pct
        avisoFase('subiendo', pct)
      }
    },
  })
  return res.data.url
}

async function guardar() {
  guardando.value = true
  mensaje.value = ''
  errorEdicion.value = ''
  progreso.value = 0
  const hayImagenes = !!(editFoto.value || editBanner.value)
  if (hayImagenes) {
    avisoMostrar('Actualizando tu perfil…', 'Esto puede tardar un momento. Puedes seguir navegando.')
  }
  try {
    let urlFoto = null
    let urlBanner = null
    if (editFoto.value) urlFoto = await subirArchivo(editFoto.value, LIMITES_IMAGEN.avatar, 'subiendo-foto', 'Subiendo tu foto de perfil…')
    if (editBanner.value) urlBanner = await subirArchivo(editBanner.value, LIMITES_IMAGEN.banner, 'subiendo-banner', 'Subiendo tu banner…')
    fase.value = 'guardando'
    if (hayImagenes) {
      avisoFase('guardando')
      avisoDetalle('Guardando los cambios…')
    }

    const payload = {
      nombreDisplay: form.value.nombreDisplay,
      biografia: form.value.biografia,
      sexo: normalizarSexo(form.value.sexo),
      ...(form.value.idPais ? { idPais: Number(form.value.idPais) } : {}),
      ...(form.value.idMonedaPreferida ? { idMonedaPreferida: Number(form.value.idMonedaPreferida) } : {}),
      ...(urlFoto ? { urlFotoPerfil: urlFoto } : {}),
      ...(urlBanner ? { urlBanner } : {}),
    }

    const r = await api.put(`/usuarios/${idUsuario.value}/perfil`, payload)

    form.value.nombreDisplay = r.data.nombreDisplay
    form.value.biografia = r.data.biografia
    form.value.sexo = sexoParaFormulario(r.data.sexo)
    form.value.idPais = r.data.idPais ? String(r.data.idPais) : ''
    form.value.urlFotoPerfil = r.data.urlFotoPerfil || ''
    form.value.urlBanner = r.data.urlBanner || ''
    form.value.idMonedaPreferida = r.data.idMonedaPreferida != null
      ? String(r.data.idMonedaPreferida)
      : (monedas.value.find((m) => m.codigo === r.data.monedaPreferida)?.idMoneda ?? '')
    localStorage.setItem('urlFotoPerfil', form.value.urlFotoPerfil)
    if (r.data.monedaPreferida) {
      localStorage.setItem('monedaPreferida', r.data.monedaPreferida)
    }
    window.dispatchEvent(new CustomEvent('perfil-foto-actualizada', {
      detail: { url: form.value.urlFotoPerfil },
    }))
    previewFoto.value = ''
    previewBanner.value = ''
    editFoto.value = null
    editBanner.value = null
    estadoGuardado.value = datosComparables()

    mensaje.value = 'Perfil actualizado correctamente.'
    if (hayImagenes) avisoExito('Tu perfil se actualizó correctamente.')
  } catch (e) {
    errorEdicion.value = e.isAxiosError
      ? (e.response?.data?.message || 'Ocurrió un error, inténtalo más tarde.')
      : (e.message || 'No se pudo guardar.')
    if (hayImagenes) avisoFallo(errorEdicion.value)
  } finally {
    guardando.value = false
    fase.value = ''
    progreso.value = 0
  }
}

function antesDeSalir(event) {
  if (hayCambios.value && !guardando.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

onMounted(() => {
  cargar()
  cargarPaises()
  window.addEventListener('beforeunload', antesDeSalir)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', antesDeSalir)
  limpiarCambios()
})
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8">
    <h2 class="text-lg text-granate mb-6 font-bold">Mi perfil</h2>

    <CargandoPagina v-if="cargando" tipo="lista" mensaje="Cargando perfil…" :tarjetas="5" />
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-5">
      <!-- Foto de perfil -->
      <div class="flex items-center gap-5">
        <input id="foto-perfil" type="file" :accept="acceptImagenes" class="sr-only" @change="onFotoChange" />
        <div class="relative shrink-0">
          <label
            for="foto-perfil"
            class="group relative block w-20 h-20 cursor-pointer rounded-full overflow-hidden border-2 border-rosa bg-rosa/20 transition"
            :class="arrastrandoFoto ? 'ring-2 ring-coral ring-offset-2' : ''"
            @dragover.prevent="arrastrandoFoto = true"
            @dragleave.prevent="arrastrandoFoto = false"
            @drop.prevent="onDropFoto"
          >
            <img v-if="previewFoto" :src="previewFoto" alt="" class="w-full h-full object-cover" />
            <img v-else-if="form.urlFotoPerfil" :src="form.urlFotoPerfil" alt="" class="w-full h-full object-cover" />
            <span v-else class="w-full h-full flex items-center justify-center text-2xl text-granate">
              {{ (nombreUsuario || '?').charAt(0).toUpperCase() }}
            </span>
            <span
              class="absolute inset-0 flex items-center justify-center bg-granate/40 text-white opacity-0 transition group-hover:opacity-100"
              aria-hidden="true"
            >
              <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6.827 6.175A2.31 2.31 0 0 1 5.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 0 0-1.134-.175 2.31 2.31 0 0 1-1.64-1.055l-.822-1.316a2.192 2.192 0 0 0-1.736-1.039 48.774 48.774 0 0 0-5.232 0 2.192 2.192 0 0 0-1.736 1.039l-.821 1.316Z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 12.75a4.5 4.5 0 1 1-9 0 4.5 4.5 0 0 1 9 0Z" />
              </svg>
            </span>
          </label>
          <span
            class="pointer-events-none absolute -bottom-0.5 -right-0.5 flex h-7 w-7 items-center justify-center rounded-full bg-coral text-white ring-2 ring-blanco"
            aria-hidden="true"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6.827 6.175A2.31 2.31 0 0 1 5.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 0 0-1.134-.175 2.31 2.31 0 0 1-1.64-1.055l-.822-1.316a2.192 2.192 0 0 0-1.736-1.039 48.774 48.774 0 0 0-5.232 0 2.192 2.192 0 0 0-1.736 1.039l-.821 1.316Z" />
            </svg>
          </span>
        </div>
        <div class="text-sm">
          <p class="text-cafe">Foto de perfil</p>
          <p class="mt-0.5 text-xs text-cafe/60">
            {{ editFoto
              ? 'Foto seleccionada. Guardá para aplicarla.'
              : 'Hacé click o arrastrá una imagen (JPG, PNG o WEBP).' }}
          </p>
        </div>
      </div>

      <!-- Banner -->
      <div>
        <div class="flex items-center justify-between gap-3">
          <label class="block text-sm text-cafe">Banner</label>
          <span class="text-xs text-cafe/50">Hacé click o arrastrá (JPG, PNG o WEBP)</span>
        </div>
        <input id="banner-perfil" type="file" :accept="acceptImagenes" class="sr-only" @change="onBannerChange" />
        <label
          for="banner-perfil"
          class="group relative block w-full h-28 sm:h-36 cursor-pointer rounded-lg overflow-hidden bg-rosa/20 transition"
          :class="arrastrandoBanner ? 'ring-2 ring-coral ring-offset-2' : ''"
          @dragover.prevent="arrastrandoBanner = true"
          @dragleave.prevent="arrastrandoBanner = false"
          @drop.prevent="onDropBanner"
        >
          <img v-if="previewBanner" :src="previewBanner" alt="" class="w-full h-full object-cover" />
          <img v-else-if="form.urlBanner" :src="form.urlBanner" alt="" class="w-full h-full object-cover" />
          <span v-else class="flex h-full w-full items-center justify-center text-sm text-cafe/50">
            Sin banner
          </span>
          <span
            class="absolute right-3 top-3 inline-flex items-center gap-1.5 rounded-md bg-blanco/90 px-3 py-1.5 text-xs font-medium text-granate shadow ring-1 ring-rosa transition group-hover:bg-rosa/30"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909M3.75 21h16.5A1.5 1.5 0 0 0 21.75 19.5V4.5A1.5 1.5 0 0 0 20.25 3H3.75A1.5 1.5 0 0 0 2.25 4.5v15A1.5 1.5 0 0 0 3.75 21Z" />
            </svg>
            {{ editBanner ? 'Banner seleccionado' : 'Cambiar' }}
          </span>
        </label>
        <p v-if="editBanner" class="mt-1 text-xs text-cafe/60">Banner seleccionado. Guardá para aplicarlo.</p>
      </div>


      <!-- Nombre de display -->
      <div>
        <label class="block text-sm text-cafe">Nombre de display</label>
        <input
          v-model="form.nombreDisplay"
          type="text"
          maxlength="25"
          class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        />
      </div>

      <!-- Biografía -->
      <div>
        <label class="block text-sm text-cafe">Biografía</label>
        <textarea
          v-model="form.biografia"
          rows="4"
          maxlength="250"
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
        ></textarea>
        <p class="text-right text-xs text-cafe/50">{{ form.biografia.length }}/250</p>
      </div>

      <!-- Datos personales -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label class="block text-sm text-cafe">Sexo</label>
          <select
            v-model="form.sexo"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="">No especificado</option>
            <option value="Masculino">Masculino</option>
            <option value="Femenino">Femenino</option>
            <option value="Otro">Otro</option>
          </select>
        </div>
        <div>
          <label class="block text-sm text-cafe">País</label>
          <select
            v-model="form.idPais"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="">No especificado</option>
            <option v-for="p in paises" :key="p.idPais" :value="String(p.idPais)">
              {{ p.nombre }}
            </option>
          </select>
        </div>
      </div>

      <!-- Moneda preferida -->
      <div>
        <label class="block text-sm text-cafe">Moneda preferida</label>
        <select
          v-model="form.idMonedaPreferida"
          class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        >
          <option v-for="m in monedas" :key="m.idMoneda" :value="String(m.idMoneda)">{{ m.nombre }} ({{ m.codigo }})</option>
        </select>
        <p class="text-xs text-cafe/50 mt-1">Los precios de los servicios se mostrarán convertidos a esta moneda.</p>
      </div>

      <BarraGuardado
        :hay-cambios="hayCambios"
        :guardando="guardando"
        :texto="etiquetaBoton"
        @guardar="guardar"
      >
        <template #aviso>
          <p v-if="errorEdicion" class="text-rojizo text-sm">{{ errorEdicion }}</p>
          <p v-else-if="mensaje" class="text-green-700 text-sm">{{ mensaje }}</p>
          <div v-if="fase.startsWith('subiendo')" class="mt-2 h-1.5 w-full bg-rosa rounded-full overflow-hidden">
            <div class="h-full bg-coral transition-all duration-150" :style="{ width: progreso + '%' }"></div>
          </div>
        </template>
        <button
          type="button"
          class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20"
          @click="irAPerfil"
        >
          Ver mi perfil
        </button>
      </BarraGuardado>
    </div>
  </div>
</template>