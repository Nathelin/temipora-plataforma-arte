<script setup>
import { onMounted, ref } from 'vue'
import api from '../servicios/api'

const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)
const vigencia = ref('72')

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/configuracion-presupuesto')
    config.value = res.data
    vigencia.value = res.data?.vigenciaHoras ?? 72
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la configuración de presupuestos.'
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
  const horas = Number(vigencia.value)
  if (!Number.isInteger(horas) || horas < 1 || horas > 8760) {
    errorAccion.value = 'La vigencia debe ser un número entero entre 1 y 8760 horas.'
    return
  }

  guardando.value = true
  try {
    const res = await api.put('/admin/configuracion-presupuesto', {
      vigenciaHoras: horas,
    })
    config.value = res.data
    vigencia.value = res.data.vigenciaHoras
    mensaje.value = 'Configuración guardada.'
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudo guardar la configuración.'
  } finally {
    guardando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comercio"
      titulo="Vigencia de presupuestos"
      descripcion="Cuánto tiempo tiene un cliente para aceptar un presupuesto."
    />

    <AdminTabs />

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <div>
          <h2 class="mb-4 text-base text-granate">Vencimiento por defecto</h2>
          <label for="vigencia-horas" class="mb-2 block text-sm text-cafe">
            Vigencia en horas <span class="text-rojizo">*</span>
          </label>
          <input
            id="vigencia-horas"
            v-model="vigencia"
            type="number"
            min="1"
            max="8760"
            step="1"
            class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          />
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            Se usa cuando el artista no define una fecha de vencimiento al enviar el presupuesto.
          </p>
        </div>

        <p v-if="errorAccion" class="text-sm text-rojizo">{{ errorAccion }}</p>
        <p v-else-if="mensaje" class="text-sm text-green-700">{{ mensaje }}</p>

        <div class="flex flex-wrap items-center gap-3 pt-2">
          <button
            :disabled="guardando"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
            @click="guardar"
          >
            {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
          </button>
        </div>
      </div>

      <aside class="h-fit rounded-2xl border border-rosa/50 bg-blanco p-6 lg:sticky lg:top-16">
        <h2 class="mb-4 text-base text-granate">Estado actual</h2>
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Vigencia</dt>
            <dd class="text-cafe">{{ config.vigenciaHoras }} horas</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Última modificación</dt>
            <dd class="text-right text-cafe">
              {{ formatearFecha(config.fechaActualizacion) }}
            </dd>
          </div>
        </dl>
        <p class="mt-4 border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          Los presupuestos ya enviados conservan la fecha de vencimiento con la que se crearon.
        </p>
      </aside>
    </div>
  </div>
</template>
