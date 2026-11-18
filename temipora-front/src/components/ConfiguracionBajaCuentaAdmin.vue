<script setup>
import { onMounted, ref } from 'vue'
import api from '../servicios/api'
import { formatearFechaHora } from '../servicios/fechas'

const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)
const diasParaReactivar = ref('30')

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/configuracion-baja-cuenta')
    config.value = res.data
    diasParaReactivar.value = res.data?.diasParaReactivar ?? 30
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la configuración de bajas de cuenta.'
  } finally {
    cargando.value = false
  }
}

function entero(valor, minimo, maximo, etiqueta) {
  const numero = Number(valor)
  if (!Number.isInteger(numero) || numero < minimo || numero > maximo) {
    return `${etiqueta} debe ser un entero entre ${minimo} y ${maximo}.`
  }
  return ''
}

async function guardar() {
  errorAccion.value = ''
  mensaje.value = ''

  const errorValidacion = entero(
    diasParaReactivar.value,
    1,
    365,
    'El plazo para reactivar',
  )
  if (errorValidacion) {
    errorAccion.value = errorValidacion
    return
  }

  guardando.value = true
  try {
    const res = await api.put('/admin/configuracion-baja-cuenta', {
      diasParaReactivar: Number(diasParaReactivar.value),
    })
    config.value = res.data
    diasParaReactivar.value = res.data?.diasParaReactivar ?? diasParaReactivar.value
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
      eyebrow="Comunidad"
      titulo="Bajas de cuenta"
      descripcion="Plazo durante el cual una persona puede reactivar su cuenta desde el inicio de sesión."
    />

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <div>
          <h2 class="mb-2 text-base text-granate">Plazo de reactivación</h2>
          <p class="mb-4 text-sm leading-relaxed text-cafe/70">
            Después de darse de baja, la persona puede reactivar su cuenta iniciando sesión
            dentro de este plazo. Pasado el plazo, la cuenta sigue bloqueada y solo el equipo
            de soporte puede reactivarla desde Usuarios.
          </p>

          <div class="max-w-xs">
            <label for="dias-reactivar" class="mb-2 block text-sm text-cafe">
              Días para reactivar <span class="text-rojizo">*</span>
            </label>
            <input
              id="dias-reactivar"
              v-model="diasParaReactivar"
              type="number"
              min="1"
              max="365"
              step="1"
              class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            />
          </div>
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            El cambio se aplica de inmediato y también alcanza a las cuentas que ya están dadas de baja.
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
            <dt class="text-xs text-cafe/50">Plazo para reactivar</dt>
            <dd class="text-cafe">{{ config?.diasParaReactivar }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Última modificación</dt>
            <dd class="text-right text-cafe">
              {{ formatearFechaHora(config?.fechaActualizacion) || '—' }}
            </dd>
          </div>
        </dl>
        <p class="mt-4 border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          El plazo se calcula desde la fecha en que la persona dio de baja su cuenta.
        </p>
      </aside>
    </div>
  </div>
</template>
