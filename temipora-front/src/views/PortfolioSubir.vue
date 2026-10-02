<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, validarImagen, procesarImagen, LIMITES_IMAGEN } from '../servicios/archivos'
import SelectorEtiquetas from '../components/SelectorEtiquetas.vue'
import { useSubirImagen } from '../composables/useSubirImagen'
import { useAvisoActualizacion } from '../composables/useAvisoActualizacion'

const router = useRouter()
const { estado, cerrar } = useSubirImagen()
const {
  mostrar: avisoMostrar,
  fase: avisoFase,
  detalle: avisoDetalle,
  exito: avisoExito,
  fallo: avisoFallo,
} = useAvisoActualizacion()

const guardando = ref(false)
const fase = ref('')
const progreso = ref(0)
const error = ref('')
const titulo = ref('')
const descripcion = ref('')
const medio = ref('TRADICIONAL')
const materiales = ref('')
const contenidoSensible = ref('')
const archivo = ref(null)
const preview = ref('')
const etiquetas = ref([])
const imagenEditando = ref(null)

function irAlPortfolio() {
  const nombrePropio = localStorage.getItem('nombreUsuario') || ''
  if (nombrePropio) {
    router.push({ name: 'perfil', params: { nombreUsuario: nombrePropio } })
  } else {
    router.push({ name: 'inicio' })
  }
}

function cancelar() {
  cerrar()
  irAlPortfolio()
}

onMounted(() => {
  error.value = ''
  guardando.value = false
  if (estado.modo === 'editar' && estado.imagen) {
    const img = estado.imagen
    imagenEditando.value = img
    titulo.value = img.titulo
    descripcion.value = img.descripcion
    medio.value = img.medio
    materiales.value = img.materiales || ''
    contenidoSensible.value = img.contenidoSensible || ''
    etiquetas.value = [...(img.etiquetas || [])]
    archivo.value = null
    preview.value = img.url
  } else {
    imagenEditando.value = null
    titulo.value = ''
    descripcion.value = ''
    medio.value = 'TRADICIONAL'
    materiales.value = ''
    contenidoSensible.value = ''
    etiquetas.value = []
    archivo.value = null
    preview.value = ''
  }
})

onBeforeUnmount(() => {
  cerrar()
})

const etiquetaBoton = computed(() => {
  if (fase.value === 'procesando') return 'Procesando imagen…'
  if (fase.value === 'subiendo') return `Subiendo… ${progreso.value}%`
  if (fase.value === 'guardando') return 'Guardando…'
  return 'Guardar'
})

function onImagenChange(event) {
  const seleccionada = event.target.files[0] || null
  const problema = seleccionada ? validarImagen(seleccionada) : ''
  if (problema) {
    archivo.value = null
    preview.value = ''
    error.value = problema
    event.target.value = ''
    return
  }

  error.value = ''
  archivo.value = seleccionada
  preview.value = seleccionada ? URL.createObjectURL(seleccionada) : ''
}

function subirArchivo(aSubir) {
  const form = new FormData()
  form.append('archivo', aSubir)
  return api
    .post('/upload', form, {
      onUploadProgress: (e) => {
        if (e.total) {
          const pct = Math.round((e.loaded * 100) / e.total)
          progreso.value = pct
          avisoFase('subiendo', pct)
        }
      },
    })
    .then((res) => res.data.url)
}

async function guardar() {
  const enEdicion = estado.modo === 'editar'
  const problema = archivo.value
    ? validarImagen(archivo.value)
    : (enEdicion ? '' : 'Selecciona una imagen.')
  if (problema) {
    error.value = problema
    return
  }
  if (!titulo.value.trim()) {
    error.value = 'El título es obligatorio.'
    return
  }
  if (!descripcion.value.trim()) {
    error.value = 'La descripción es obligatoria.'
    return
  }

  guardando.value = true
  error.value = ''
  progreso.value = 0
  const conImagen = !!archivo.value
  if (conImagen) {
    avisoMostrar('Subiendo tu obra…', 'Esto puede tardar un momento. Puedes seguir navegando.')
  }
  try {
    const idUsuario = localStorage.getItem('idUsuario')
    let url = enEdicion ? imagenEditando.value.url : null
    if (archivo.value) {
      fase.value = 'procesando'
      avisoFase('procesando')
      avisoDetalle('Procesando la imagen…')
      const optimizado = await procesarImagen(archivo.value, LIMITES_IMAGEN.obra)
      fase.value = 'subiendo'
      avisoFase('subiendo', 0)
      url = await subirArchivo(optimizado)
    }
    fase.value = 'guardando'
    if (conImagen) {
      avisoFase('guardando')
      avisoDetalle('Publicando tu obra…')
    }
    const cuerpo = {
      titulo: titulo.value,
      descripcion: descripcion.value,
      url: url || preview.value,
      medio: medio.value,
      materiales: materiales.value,
      contenidoSensible: contenidoSensible.value || null,
      idsEtiqueta: etiquetas.value.map(e => e.idEtiqueta),
    }

    if (enEdicion) {
      const res = await api.put(
        `/portfolio/usuarios/${idUsuario}/imagenes/${imagenEditando.value.idImagen}`,
        cuerpo,
      )
      window.dispatchEvent(new CustomEvent('portfolio-actualizado', {
        detail: { accion: 'editar', imagen: res.data },
      }))
    } else {
      const res = await api.post(`/portfolio/usuarios/${idUsuario}/imagenes`, cuerpo)
      window.dispatchEvent(new CustomEvent('portfolio-actualizado', {
        detail: { accion: 'crear', imagen: res.data },
      }))
    }
    cerrar()
    if (conImagen) {
      avisoExito(enEdicion
        ? 'Tu obra se actualizó en el portfolio.'
        : 'Tu obra se publicó en tu portfolio.')
    }
    irAlPortfolio()
  } catch (e) {
    error.value = e.isAxiosError
      ? (e.response?.data?.message || 'Ocurrió un error, inténtalo más tarde.')
      : (e.message || 'No se pudo guardar la imagen.')
    if (conImagen) avisoFallo(error.value)
  } finally {
    guardando.value = false
    fase.value = ''
    progreso.value = 0
  }
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

      <h1 class="mt-4 text-2xl text-granate pt-5">
        {{ estado.modo === 'editar' ? 'Editar imagen del portfolio' : 'Subir imagen a tu portfolio' }}
      </h1>
      <p class="mt-1 mb-6 text-sm text-cafe/60 pt-1 pb-5">
        {{ estado.modo === 'editar'
          ? 'Actualiza los datos de la imagen. La URL se conserva si no seleccionas un archivo nuevo.'
          : 'Completa los datos de la obra y selecciona el archivo de imagen.' }}
      </p>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 bg-blanco border border-rosa rounded-xl p-6">
        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2">Título <span class="text-rojizo">*</span></label>
            <input v-model="titulo" type="text" maxlength="100" required class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Descripción <span class="text-rojizo">*</span></label>
            <textarea v-model="descripcion" rows="5" maxlength="1000" required class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"></textarea>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Medio <span class="text-rojizo">*</span></label>
            <select v-model="medio" class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
              <option value="TRADICIONAL">Tradicional</option>
              <option value="DIGITAL">Digital</option>
            </select>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Materiales</label>
            <input v-model="materiales" type="text" maxlength="500" placeholder="Ej.: óleo sobre lienzo" class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Contenido sensible</label>
            <select v-model="contenidoSensible" class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
              <option value="">Sin contenido sensible</option>
              <option value="VIOLENCIA">Violencia</option>
              <option value="DESNUDOS">Desnudos</option>
              <option value="GENERAL">Contenido sensible general</option>
            </select>
          </div>

          <SelectorEtiquetas v-model="etiquetas" />

          <p v-if="error" class="text-rojizo text-sm">{{ error }}</p>
        </div>

        <div class="flex flex-col">
          <label class="block text-sm text-cafe mb-2">Imagen <span class="text-rojizo">*</span></label>
          <div class="flex-1 min-h-80 rounded-xl border border-dashed border-rosa bg-rosa/10 flex items-center justify-center overflow-hidden">
            <img v-if="preview" :src="preview" alt="Vista previa" class="w-full h-full max-h-128 object-contain" />
            <p v-else class="px-6 text-center text-sm text-cafe/50">Selecciona una imagen para ver la vista previa.</p>
          </div>
          <input id="portfolio-imagen" type="file" :accept="acceptImagenes" class="sr-only" @change="onImagenChange" />
          <label for="portfolio-imagen" class="mt-4 inline-flex cursor-pointer justify-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm text-granate hover:bg-coral/20 transition">
            {{ archivo ? 'Cambiar imagen' : (estado.modo === 'editar' ? 'Cambiar imagen (opcional)' : 'Seleccionar imagen') }}
          </label>
        </div>
      </div>

      <div class="flex items-center justify-end gap-3 mt-6">
        <button
          type="button"
          class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
          :disabled="guardando"
          @click="cancelar"
        >
          Cancelar
        </button>
        <button
          type="button"
          :disabled="guardando || (estado.modo === 'crear' && !archivo)"
          class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm min-w-40"
          @click="guardar"
        >
          {{ etiquetaBoton }}
        </button>
      </div>

      <div v-if="fase === 'subiendo'" class="mt-4 h-1.5 w-full bg-rosa rounded-full overflow-hidden">
        <div class="h-full bg-coral transition-all duration-150" :style="{ width: progreso + '%' }"></div>
      </div>
    </div>
  </div>
</template>
