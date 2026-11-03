<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../servicios/api'
import { useMonedas } from '../composables/useMonedas'

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const refrescando = ref(false)
const mensaje = ref('')
const errorAccion = ref('')

const config = ref(null)
const { monedas, cargar: cargarMonedas } = useMonedas()

const form = ref({
  apiHabilitada: true,
  idMonedaOrigen: '',
  idMonedaDestino: '',
  factorManual: '',
})

function codigoDe(id) {
  return monedas.value.find((m) => String(m.idMoneda) === String(id))?.codigo || ''
}

const codigoOrigenForm = computed(() => codigoDe(form.value.idMonedaOrigen))
const codigoDestinoForm = computed(() => codigoDe(form.value.idMonedaDestino))

function volcar(d) {
  config.value = d
  form.value = {
    apiHabilitada: !!d.apiHabilitada,
    idMonedaOrigen: d.idMonedaOrigen != null ? String(d.idMonedaOrigen) : '',
    idMonedaDestino: d.idMonedaDestino != null ? String(d.idMonedaDestino) : '',
    factorManual: d.factorManual ?? '',
  }
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    await cargarMonedas()
    const res = await api.get('/admin/cotizacion')
    volcar(res.data)
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar la configuración de cotización.'
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

async function guardar() {
  errorAccion.value = ''
  mensaje.value = ''
  if (form.value.idMonedaOrigen === form.value.idMonedaDestino) {
    errorAccion.value = 'Las monedas de origen y destino deben ser distintas.'
    return
  }
  if (form.value.factorManual !== '' && Number(form.value.factorManual) <= 0) {
    errorAccion.value = 'El factor manual debe ser mayor a cero.'
    return
  }
  guardando.value = true
  try {
    const payload = {
      apiHabilitada: form.value.apiHabilitada,
      idMonedaOrigen: Number(form.value.idMonedaOrigen),
      idMonedaDestino: Number(form.value.idMonedaDestino),
      factorManual: form.value.factorManual !== '' ? Number(form.value.factorManual) : null,
    }
    const r = await api.put('/admin/cotizacion', payload)
    volcar(r.data)
    mensaje.value = 'Configuración guardada.'
  } catch (e) {
    errorAccion.value = e.response?.data?.message || 'No se pudo guardar la configuración.'
  } finally {
    guardando.value = false
  }
}

async function refrescar() {
  errorAccion.value = ''
  mensaje.value = ''
  refrescando.value = true
  try {
    const r = await api.post('/admin/cotizacion/refrescar')
    volcar(r.data)
    mensaje.value = 'Cotización actualizada desde la API.'
  } catch (e) {
    errorAccion.value = e.response?.data?.message || 'No se pudo actualizar desde la API.'
  } finally {
    refrescando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="flex-1 w-full mx-auto px-4 sm:px-8 py-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comercio"
      titulo="Cotización"
      descripcion="Par de monedas y factor de conversión vigente."
    />

    <AdminTabs />

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_22rem] gap-8">
      <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8 space-y-6">
        <div>
          <h2 class="text-base text-granate mb-4">Actualización automática</h2>
          <label class="flex items-start gap-2 text-sm text-cafe">
            <input v-model="form.apiHabilitada" type="checkbox" class="mt-0.5 accent-coral" />
            <span>
              Obtener la cotización desde la API
              <span class="block text-xs text-cafe/50 mt-0.5">
                Si está deshabilitada, se usa únicamente el factor manual o el último factor registrado.
              </span>
            </span>
          </label>
        </div>

        <div>
          <h2 class="text-base text-granate mb-4">Par de monedas</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm text-cafe mb-2">Moneda de origen <span class="text-rojizo">*</span></label>
              <select
                v-model="form.idMonedaOrigen"
                class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco"
              >
                <option v-for="m in monedas" :key="m.idMoneda" :value="String(m.idMoneda)">{{ m.nombre }} ({{ m.codigo }})</option>
              </select>
            </div>
            <div>
              <label class="block text-sm text-cafe mb-2">Moneda de destino <span class="text-rojizo">*</span></label>
              <select
                v-model="form.idMonedaDestino"
                class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco"
              >
                <option v-for="m in monedas" :key="m.idMoneda" :value="String(m.idMoneda)">{{ m.nombre }} ({{ m.codigo }})</option>
              </select>
            </div>
          </div>
        </div>

        <div>
          <h2 class="text-base text-granate mb-4">Factor manual</h2>
          <label class="block text-sm text-cafe mb-2">
            Unidades de {{ codigoDestinoForm || 'la moneda destino' }} por 1 {{ codigoOrigenForm || 'la moneda origen' }}
          </label>
          <input
            v-model="form.factorManual"
            type="number"
            min="0"
            step="0.00000001"
            placeholder="Vacío para no modificar el factor actual"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
          <p class="text-xs text-cafe/50 mt-1">
            Al guardar un factor se cierra la vigencia anterior y se registra uno nuevo desde ahora.
          </p>
        </div>

        <p v-if="errorAccion" class="text-rojizo text-sm">{{ errorAccion }}</p>
        <p v-else-if="mensaje" class="text-green-700 text-sm">{{ mensaje }}</p>

        <div class="flex flex-wrap items-center gap-3 pt-2">
          <button
            :disabled="guardando"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
            @click="guardar"
          >
            {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
          </button>
          <button
            :disabled="refrescando || !form.apiHabilitada"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
            :title="!form.apiHabilitada ? 'Habilitá la API para actualizar' : ''"
            @click="refrescar"
          >
            {{ refrescando ? 'Actualizando...' : 'Actualizar desde la API' }}
          </button>
        </div>
      </div>

      <aside class="lg:sticky lg:top-16 h-fit rounded-2xl border border-rosa/50 bg-blanco p-6">
        <h2 class="text-base text-granate mb-4">Estado actual</h2>
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">API</dt>
            <dd>
              <span
                class="px-2.5 py-0.5 rounded-full text-xs"
                :class="config.apiHabilitada ? 'bg-green-500/10 text-green-700' : 'bg-rojizo/10 text-rojizo'"
              >
                {{ config.apiHabilitada ? 'Habilitada' : 'Deshabilitada' }}
              </span>
            </dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Factor vigente</dt>
            <dd class="text-cafe text-right">
              {{ config.factorVigente != null ? `1 ${config.monedaOrigen} = ${config.factorVigente} ${config.monedaDestino}` : 'Sin factor' }}
            </dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Origen</dt>
            <dd class="text-cafe">{{ config.origenFactor || '—' }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Vigente desde</dt>
            <dd class="text-cafe text-right">{{ formatearFecha(config.inicioVigencia) }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Última modificación</dt>
            <dd class="text-cafe text-right">{{ formatearFecha(config.fechaActualizacion) }}</dd>
          </div>
        </dl>
      </aside>
    </div>
  </div>
</template>
