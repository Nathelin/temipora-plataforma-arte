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
  preferenciaSensible: 'ADVERTIR',
  pedidosPublicos: false,
  recibirEmailsPromocionales: true,
})

const idUsuario = ref(null)
const estadoGuardado = ref(null)

const hayCambios = computed(
  () => estadoGuardado.value !== null
    && (estadoGuardado.value.preferenciaSensible !== form.value.preferenciaSensible
      || estadoGuardado.value.pedidosPublicos !== form.value.pedidosPublicos
      || estadoGuardado.value.recibirEmailsPromocionales !== form.value.recibirEmailsPromocionales),
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
    form.value.pedidosPublicos = Boolean(d.pedidosPublicos)
    form.value.recibirEmailsPromocionales =
      d.recibirEmailsPromocionales ?? true
    estadoGuardado.value = { ...form.value }
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
      preferenciaSensible: form.value.preferenciaSensible,
      pedidosPublicos: form.value.pedidosPublicos,
      recibirEmailsPromocionales: form.value.recibirEmailsPromocionales,
    }

    const r = await api.put(`/usuarios/${idUsuario.value}/perfil`, payload)

    form.value.preferenciaSensible = r.data.preferenciaSensible
    form.value.pedidosPublicos = Boolean(r.data.pedidosPublicos)
    form.value.recibirEmailsPromocionales =
      Boolean(r.data.recibirEmailsPromocionales)
    estadoGuardado.value = { ...form.value }
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
    <h2 class="text-lg font-bold text-granate">Privacidad y contenido</h2>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-6">
      <div>
        <h3 class="text-base text-granate pt-2">Comunicaciones</h3>
        <p class="text-sm text-cafe/70">
          Elegí si querés recibir correos con ofertas y promociones de Temipora.
        </p>

        <div
          class="mt-3 rounded-xl border-2 p-4 transition"
          :class="form.recibirEmailsPromocionales ? 'border-coral bg-coral/10' : 'border-rosa/30'"
        >
          <div class="flex items-start gap-3">
            <button
              type="button"
              role="switch"
              :aria-checked="form.recibirEmailsPromocionales"
              class="relative mt-0.5 inline-flex h-6 w-11 shrink-0 items-center rounded-full transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
              :class="form.recibirEmailsPromocionales ? 'bg-coral' : 'bg-rosa/70'"
              @click="form.recibirEmailsPromocionales = !form.recibirEmailsPromocionales"
            >
              <span
                class="inline-block h-4 w-4 rounded-full bg-white shadow transition"
                :class="form.recibirEmailsPromocionales ? 'translate-x-6' : 'translate-x-1'"
              ></span>
            </button>
            <div class="min-w-0">
              <p class="text-base font-medium text-granate">
                {{ form.recibirEmailsPromocionales ? 'Recibí correos con ofertas' : 'No recibir correos con ofertas' }}
              </p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">
                Si lo desactivás, vas a seguir viendo las ofertas dentro de la app, pero no te vamos a enviar correos promocionales.
              </p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">
                Podés volver a activarlo cuando quieras.
              </p>
            </div>
          </div>
        </div>
      </div>

      <div>
        <h3 class="text-base text-granate pt-2">Galería de pedidos</h3>
        <p class="text-sm text-cafe/70">
          Elegí si querés que otras personas puedan ver el arte que recibiste.
        </p>

        <div
          class="mt-3 rounded-xl border-2 p-4 transition"
          :class="form.pedidosPublicos ? 'border-coral bg-coral/10' : 'border-rosa/30'"
        >
          <div class="flex items-start gap-3">
            <button
              type="button"
              role="switch"
              :aria-checked="form.pedidosPublicos"
              class="relative mt-0.5 inline-flex h-6 w-11 shrink-0 items-center rounded-full transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
              :class="form.pedidosPublicos ? 'bg-coral' : 'bg-rosa/70'"
              @click="form.pedidosPublicos = !form.pedidosPublicos"
            >
              <span
                class="inline-block h-4 w-4 rounded-full bg-white shadow transition"
                :class="form.pedidosPublicos ? 'translate-x-6' : 'translate-x-1'"
              ></span>
            </button>
            <div class="min-w-0">
              <p class="text-base font-medium text-granate">
                {{ form.pedidosPublicos ? 'Galería pública' : 'Galería privada' }}
              </p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">
                Solo se muestran las entregas finales de pedidos completos que el artista autorizó. Se ve la imagen, el servicio, el artista y la fecha. Nunca montos, estados ni datos del pedido.
              </p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">
                Podés ocultar pedidos puntuales desde tu galería o desde el detalle del pedido.
              </p>
              <router-link
                :to="{ name: 'perfil', params: { nombreUsuario } }"
                class="mt-2 inline-flex text-sm font-semibold text-coral transition hover:text-rojizo"
              >
                Ver mi galería →
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <div>
        <h3 class="text-base text-granate pt-2">Visualización de contenido sensible</h3>
        <p class="text-sm text-cafe/70">
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