<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import BarraGuardado from '../components/BarraGuardado.vue'

const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorEdicion = ref('')

const form = ref({
  preferenciaSensible: 'ADVERTIR'
})

const idUsuario = ref(null)
const estadoGuardado = ref(null)

const hayCambios = computed(
  () => estadoGuardado.value !== null && estadoGuardado.value !== form.value.preferenciaSensible,
)

const { actualizar: marcarCambios, limpiar: limpiarCambios } = useCambiosSinGuardar()

watch(hayCambios, (pendiente) => {
  marcarCambios(pendiente, 'Tenés cambios sin guardar en Privacidad y contenido. Si salís, se perderán.')
})

const opciones = [
  { value: 'ADVERTIR', label: 'Mostrar con advertencia'},
  { value: 'MOSTRAR_SIN_ADVERTENCIA', label: 'Mostrar todo sin advertencia'},
  { value: 'OCULTAR', label: 'Ocultar contenido sensible', description: 'Oculta solo imágenes de otros usuarios.' }
]

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/perfil/${nombreUsuario}`)
    const d = res.data
    idUsuario.value = d.idUsuario
    form.value.preferenciaSensible = d.preferenciaSensible || 'ADVERTIR'
    estadoGuardado.value = form.value.preferenciaSensible
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar la configuración.'
  } finally {
    cargando.value = false
  }
}

async function guardar() {
  guardando.value = true
  mensaje.value = ''
  errorEdicion.value = ''
  try {
    const payload = {
      preferenciaSensible: form.value.preferenciaSensible
    }

    const r = await api.put(`/usuarios/${idUsuario.value}/perfil`, payload)

    form.value.preferenciaSensible = r.data.preferenciaSensible
    estadoGuardado.value = form.value.preferenciaSensible
    mensaje.value = 'Cambios guardados correctamente.'
  } catch (e) {
    errorEdicion.value = e.response?.data?.message || 'No se pudo guardar.'
  } finally {
    guardando.value = false
  }
}

onMounted(cargar)

onBeforeUnmount(limpiarCambios)
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8">
    <h2 class="text-lg text-granate mb-6">Privacidad y contenido</h2>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-6">
      <div>
        <h3 class="text-base text-granate mb-4 pt-5">Visualización de contenido sensible</h3>
        <p class="text-sm text-cafe/70 mb-4">
          Elige cómo quieres que se muestren las imágenes marcadas como contenido sensible en toda la plataforma.
        </p>

        <div class="space-y-4 pt-2" v-for="opcion in opciones" :key="opcion.value">
          <label class="relative flex items-start p-4 rounded-xl border-2 cursor-pointer transition hover:bg-rosa/20"
                 :class="form.preferenciaSensible === opcion.value ? 'border-coral bg-coral/10' : 'border-rosa/30'">
            <input
              type="radio"
              :value="opcion.value"
              v-model="form.preferenciaSensible"
              name="preferenciaSensible"
              class="sr-only"
            />
            <div class="flex-1">
              <div class="flex items-center gap-3">
                <div class="w-5 h-5 rounded-full border-2 border-coral flex items-center justify-center">
                  <div v-if="form.preferenciaSensible === opcion.value" class="w-2.5 h-2.5 rounded-full bg-coral"></div>
                </div>
                <span class="text-base font-medium text-granate">{{ opcion.label }}</span>
              </div>
              <p class="mt-1 ml-8 text-sm text-cafe/70">{{ opcion.description }}</p>
            </div>
          </label>
        </div>
      </div>

      <BarraGuardado
        :hay-cambios="hayCambios"
        :guardando="guardando"
        :texto="guardando ? 'Guardando…' : 'Guardar cambios'"
        @guardar="guardar"
      >
        <template #aviso>
          <p v-if="errorEdicion" class="text-rojizo text-sm">{{ errorEdicion }}</p>
          <p v-else-if="mensaje" class="text-green-700 text-sm">{{ mensaje }}</p>
        </template>
      </BarraGuardado>
    </div>
  </div>
</template>