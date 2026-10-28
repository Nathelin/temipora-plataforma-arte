<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../servicios/api'
import { formatearMonto } from '../servicios/moneda'

const cargando = ref(true)
const publicando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const versiones = ref([])
const comision = ref(null)
const titulo = ref('Términos y condiciones de Temipora')
const contenido = ref('')

const vigente = computed(() => versiones.value.find((v) => v.vigente) || null)
const proximaVersion = computed(() => {
  const max = versiones.value.reduce((mayor, v) => Math.max(mayor, v.version || 0), 0)
  return max + 1
})

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [resVersiones, resComision] = await Promise.all([
      api.get('/admin/terminos-plataforma'),
      api.get('/admin/configuracion-comision'),
    ])
    versiones.value = resVersiones.data || []
    comision.value = resComision.data
    if (vigente.value) {
      titulo.value = vigente.value.titulo
      contenido.value = vigente.value.contenido
    }
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudieron cargar los términos de la plataforma.'
  } finally {
    cargando.value = false
  }
}

function formatearFecha(valor) {
  if (!valor) return '—'
  try {
    return new Date(valor).toLocaleString('es-AR')
  } catch {
    return valor
  }
}

async function publicar() {
  errorAccion.value = ''
  mensaje.value = ''
  if (!titulo.value.trim()) {
    errorAccion.value = 'El título es obligatorio.'
    return
  }
  if (!contenido.value.trim()) {
    errorAccion.value = 'El contenido es obligatorio.'
    return
  }

  publicando.value = true
  try {
    const res = await api.post('/admin/terminos-plataforma', {
      titulo: titulo.value.trim(),
      contenido: contenido.value.trim(),
    })
    versiones.value = [res.data, ...versiones.value.map((v) => (
      v.idTerminosPlataforma === res.data.idTerminosPlataforma ? v : { ...v, vigente: false }
    ))]
    mensaje.value = `Versión ${res.data.version} publicada.`
    await cargar()
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudo publicar la versión.'
  } finally {
    publicando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <h1 class="mb-6 text-xl font-normal text-granate">Términos de la plataforma</h1>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <div class="rounded-xl border border-coral/25 bg-coral/5 p-4">
          <p class="text-sm text-cafe">
            Se va a publicar la <strong>versión {{ proximaVersion }}</strong> declarando una comisión
            de <strong>{{ formatearMonto(comision?.porcentajeServicio) }}%</strong> para servicios y
            <strong>{{ formatearMonto(comision?.porcentajeProducto) }}%</strong> para productos.
          </p>
          <p class="mt-2 text-xs leading-relaxed text-cafe/60">
            Los porcentajes se congelan al publicar. Cada versión queda inmutable.
          </p>
        </div>

        <div>
          <label for="titulo-terminos" class="mb-2 block text-sm text-cafe">
            Título <span class="text-rojizo">*</span>
          </label>
          <input
            id="titulo-terminos"
            v-model="titulo"
            type="text"
            maxlength="120"
            class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          />
        </div>

        <div>
          <label for="contenido-terminos" class="mb-2 block text-sm text-cafe">
            Contenido <span class="text-rojizo">*</span>
          </label>
          <textarea
            id="contenido-terminos"
            v-model="contenido"
            rows="18"
            class="block w-full rounded-lg border-0 px-4 py-3 text-sm leading-relaxed text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          ></textarea>
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            Se muestra tal cual, respetando los saltos de línea. Incluí explícitamente la comisión y
            el funcionamiento de hitos y devoluciones.
          </p>
        </div>

        <p v-if="errorAccion" class="text-sm text-rojizo">{{ errorAccion }}</p>
        <p v-else-if="mensaje" class="text-sm text-green-700">{{ mensaje }}</p>

        <div class="flex flex-wrap items-center gap-3 pt-2">
          <button
            :disabled="publicando"
            class="rounded-md bg-coral px-4 py-2 text-sm text-white shadow-sm transition hover:bg-rojizo disabled:opacity-50"
            @click="publicar"
          >
            {{ publicando ? 'Publicando...' : `Publicar versión ${proximaVersion}` }}
          </button>
          <router-link
            to="/terminos"
            target="_blank"
            class="text-sm font-medium text-coral transition hover:text-rojizo"
          >
            Ver página pública
          </router-link>
        </div>
      </div>

      <aside class="h-fit rounded-2xl border border-rosa/50 bg-blanco p-6 lg:sticky lg:top-16">
        <h2 class="mb-4 text-base text-granate">Versión publicada</h2>
        <dl v-if="vigente" class="space-y-4 text-sm">
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Versión</dt>
            <dd class="text-cafe">v{{ vigente.version }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Publicada</dt>
            <dd class="text-right text-cafe">{{ formatearFecha(vigente.fechaPublicacion) }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Comisión servicios</dt>
            <dd class="text-cafe">{{ formatearMonto(vigente.comisionServicio) }}%</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Comisión productos</dt>
            <dd class="text-cafe">{{ formatearMonto(vigente.comisionProducto) }}%</dd>
          </div>
        </dl>
        <p v-else class="text-sm text-cafe/60">Todavía no hay una versión publicada.</p>

        <div v-if="versiones.length > 1" class="mt-5 border-t border-rosa/40 pt-4">
          <h3 class="mb-2 text-xs uppercase tracking-wide text-cafe/50">Historial</h3>
          <ul class="space-y-2 text-xs text-cafe/70">
            <li v-for="v in versiones" :key="v.idTerminosPlataforma">
              v{{ v.version }} · {{ formatearFecha(v.fechaPublicacion) }}
              <span v-if="v.vigente" class="text-coral">· vigente</span>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>
