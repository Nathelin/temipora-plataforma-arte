<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'

const props = defineProps({
  modo: { type: String, default: 'ejemplos' }, // 'portada' | 'ejemplos'
  seleccionadas: { type: Array, default: () => [] },
})
const emit = defineEmits(['cerrar', 'seleccionar'])

const router = useRouter()

const imagenes = ref([])
const cargando = ref(true)
const error = ref('')
const elegidas = ref([])

const esPortada = computed(() => props.modo === 'portada')
const yaEnFormulario = computed(() => new Set(props.seleccionadas))
const puedeConfirmar = computed(() => elegidas.value.length > 0)

onMounted(cargar)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const nombre = localStorage.getItem('nombreUsuario') || ''
    if (!nombre) {
      error.value = 'No se pudo identificar tu usuario.'
      return
    }
    const res = await api.get(`/portfolio/${nombre}`)
    imagenes.value = (res.data || []).filter((i) => i.estado !== false)
  } catch {
    error.value = 'No se pudo cargar tu portfolio.'
  } finally {
    cargando.value = false
  }
}

function alternar(img) {
  if (yaEnFormulario.value.has(img.url)) return
  if (esPortada.value) {
    elegidas.value = [img.url]
    return
  }
  const indice = elegidas.value.indexOf(img.url)
  if (indice === -1) {
    elegidas.value.push(img.url)
  } else {
    elegidas.value.splice(indice, 1)
  }
}

function estaElegida(url) {
  return elegidas.value.includes(url)
}

function confirmar() {
  if (!puedeConfirmar.value) return
  emit('seleccionar', [...elegidas.value])
}

function cerrar() {
  emit('cerrar')
}

function irASubir() {
  cerrar()
  router.push({ name: 'portfolio-subir' })
}
</script>

<template>
  <div
    class="fixed inset-0 z-[70] flex items-center justify-center bg-granate/40 px-4"
    @click.self="cerrar"
  >
    <div class="w-full max-w-3xl bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] flex flex-col">
      <div class="flex items-start justify-between gap-4 mb-4">
        <div>
          <h2 class="text-lg text-granate">
            {{ esPortada ? 'Elegir portada del portfolio' : 'Elegir imágenes del portfolio' }}
          </h2>
          <p class="text-sm text-cafe/60 mt-1">
            {{
              esPortada
                ? 'Seleccioná una obra para usar como imagen de portada.'
                : 'Seleccioná una o varias obras para usar como imágenes de ejemplo.'
            }}
          </p>
        </div>
        <button
          type="button"
          class="text-cafe/60 hover:text-rojizo transition text-lg leading-none"
          aria-label="Cerrar"
          @click="cerrar"
        >
          ✕
        </button>
      </div>

      <div class="flex-1 min-h-40 overflow-y-auto">
        <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando portfolio...</p>
        <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>
        <p v-else-if="!imagenes.length" class="text-center py-10">
          <span class="block text-cafe/60">Todavía no tenés imágenes en tu portfolio.</span>
          <button
            type="button"
            class="mt-3 text-sm text-granate underline hover:text-coral transition"
            @click="irASubir"
          >
            Subir imágenes al portfolio
          </button>
        </p>

        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
          <button
            v-for="img in imagenes"
            :key="img.idImagen"
            type="button"
            class="relative aspect-square rounded-lg overflow-hidden border-2 transition text-left disabled:cursor-not-allowed"
            :class="{
              'border-coral ring-1 ring-coral': estaElegida(img.url),
              'border-rosa hover:border-coral/60': !estaElegida(img.url) && !yaEnFormulario.has(img.url),
              'border-rosa opacity-40': yaEnFormulario.has(img.url),
            }"
            :disabled="yaEnFormulario.has(img.url)"
            :title="yaEnFormulario.has(img.url) ? 'Ya está incluida en el servicio' : (img.titulo || '')"
            @click="alternar(img)"
          >
            <img :src="img.url" :alt="img.titulo || 'Imagen del portfolio'" class="w-full h-full object-cover" />

            <span
              v-if="estaElegida(img.url)"
              class="absolute top-1 right-1 w-6 h-6 rounded-full bg-coral text-white flex items-center justify-center text-xs leading-none"
            >
              ✓
            </span>
            <span
              v-else-if="yaEnFormulario.has(img.url)"
              class="absolute top-1 left-1 bg-granate/80 text-white text-[11px] px-2 py-0.5 rounded"
            >
              Ya incluida
            </span>

            <span
              v-if="img.titulo"
              class="absolute inset-x-0 bottom-0 bg-granate/60 text-white text-[11px] px-2 py-1 truncate"
            >
              {{ img.titulo }}
            </span>
          </button>
        </div>
      </div>

      <div class="flex items-center justify-between gap-3 mt-6 pt-4 border-t border-rosa/60">
        <p class="text-xs text-cafe/50">
          <template v-if="esPortada && elegidas.length">1 imagen seleccionada</template>
          <template v-else-if="!esPortada && elegidas.length">
            {{ elegidas.length }} imagen{{ elegidas.length === 1 ? '' : 'es' }} seleccionada{{ elegidas.length === 1 ? '' : 's' }}
          </template>
        </p>
        <div class="flex items-center gap-3">
          <button
            type="button"
            class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
            @click="cerrar"
          >
            Cancelar
          </button>
          <button
            type="button"
            :disabled="!puedeConfirmar"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
            @click="confirmar"
          >
            {{ esPortada ? 'Usar como portada' : 'Agregar seleccionadas' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
