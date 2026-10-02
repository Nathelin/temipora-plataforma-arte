<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, esImagenPermitida } from '../servicios/archivos'
import SelectorEtiquetas from '../components/SelectorEtiquetas.vue'
import { useSubirImagen } from '../composables/useSubirImagen'

const router = useRouter()
const { estado, cerrar } = useSubirImagen()

const guardando = ref(false)
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

function onImagenChange(event) {
  const seleccionada = event.target.files[0] || null
  if (seleccionada && !esImagenPermitida(seleccionada)) {
    archivo.value = null
    preview.value = ''
    error.value = 'El formato de la imagen es inválido.'
    event.target.value = ''
    return
  }

  error.value = ''
  archivo.value = seleccionada
  preview.value = seleccionada ? URL.createObjectURL(seleccionada) : ''
}

async function subirArchivo(aSubir) {
  const form = new FormData()
  form.append('archivo', aSubir)
  const res = await api.post('/upload', form)
  return res.data.url
}

async function guardar() {
  const enEdicion = estado.modo === 'editar'
  if (!enEdicion && !archivo.value) {
    error.value = 'Selecciona una imagen.'
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
  try {
    const idUsuario = localStorage.getItem('idUsuario')
    let url = enEdicion ? imagenEditando.value.url : null
    if (archivo.value) {
      url = await subirArchivo(archivo.value)
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
    irAlPortfolio()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo guardar la imagen.'
  } finally {
    guardando.value = false
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
          @click="cancelar"
        >
          Cancelar
        </button>
        <button
          type="button"
          :disabled="guardando || (estado.modo === 'crear' && !archivo)"
          class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
          @click="guardar"
        >
          {{ guardando ? 'Guardando...' : 'Guardar' }}
        </button>
      </div>
    </div>
  </div>
</template>
