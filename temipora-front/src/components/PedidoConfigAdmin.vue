<script setup>
import { onMounted, ref } from 'vue'
import api from '../servicios/api'
import { formatearMonto } from '../servicios/moneda'

const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)
const diasAprobacionAutomatica = ref('7')
const diasPagoCuota = ref('7')
const maxHitos = ref('4')
const porcentajeMaxPrimerHito = ref('50')
const montoMinimoCuota = ref('1')

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/configuracion-pedido')
    config.value = res.data
    diasAprobacionAutomatica.value = res.data?.diasAprobacionAutomatica ?? 7
    diasPagoCuota.value = res.data?.diasPagoCuota ?? 7
    maxHitos.value = res.data?.maxHitos ?? 4
    porcentajeMaxPrimerHito.value = res.data?.porcentajeMaxPrimerHito ?? 50
    montoMinimoCuota.value = res.data?.montoMinimoCuota ?? 1
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la configuración de pedidos.'
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

function entero(valor, minimo, maximo, etiqueta) {
  const numero = Number(valor)
  if (!Number.isInteger(numero) || numero < minimo || numero > maximo) {
    return `${etiqueta} debe ser un entero entre ${minimo} y ${maximo}.`
  }
  return ''
}

function decimal(valor, minimo, maximo, etiqueta) {
  const numero = Number(String(valor).replace(',', '.'))
  if (!Number.isFinite(numero)) {
    return `${etiqueta} debe ser un número.`
  }
  if (numero < minimo || numero > maximo) {
    return `${etiqueta} debe estar entre ${minimo} y ${maximo}.`
  }
  if (Math.round(numero * 100) !== numero * 100) {
    return `${etiqueta} admite hasta 2 decimales.`
  }
  return ''
}

async function guardar() {
  errorAccion.value = ''
  mensaje.value = ''
  const errores = [
    entero(diasAprobacionAutomatica.value, 1, 60, 'La aprobación automática'),
    entero(diasPagoCuota.value, 1, 90, 'El plazo de pago'),
    entero(maxHitos.value, 1, 6, 'El máximo de hitos'),
    decimal(porcentajeMaxPrimerHito.value, 1, 100, 'El porcentaje máximo del primer hito'),
    decimal(montoMinimoCuota.value, 0, 10000000, 'El monto mínimo por cuota'),
  ].filter(Boolean)

  if (errores.length) {
    errorAccion.value = errores[0]
    return
  }

  guardando.value = true
  try {
    const res = await api.put('/admin/configuracion-pedido', {
      diasAprobacionAutomatica: Number(diasAprobacionAutomatica.value),
      diasPagoCuota: Number(diasPagoCuota.value),
      maxHitos: Number(maxHitos.value),
      porcentajeMaxPrimerHito: Number(String(porcentajeMaxPrimerHito.value).replace(',', '.')),
      montoMinimoCuota: Number(String(montoMinimoCuota.value).replace(',', '.')),
    })
    config.value = res.data
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
      titulo="Reglas de pedidos e hitos"
      descripcion="Límites del plan de hitos, aprobación automática y plazos de pago."
    />

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <div>
          <h2 class="mb-4 text-base text-granate">Límites del plan de hitos</h2>

          <div class="grid gap-4 sm:grid-cols-3">
            <div>
              <label for="max-hitos" class="mb-2 block text-sm text-cafe">
                Máximo de hitos <span class="text-rojizo">*</span>
              </label>
              <input
                id="max-hitos"
                v-model="maxHitos"
                type="number"
                min="1"
                max="6"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="max-primer-hito" class="mb-2 block text-sm text-cafe">
                Máx. primera cuota (%) <span class="text-rojizo">*</span>
              </label>
              <input
                id="max-primer-hito"
                v-model="porcentajeMaxPrimerHito"
                type="number"
                min="1"
                max="100"
                step="0.01"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="monto-minimo" class="mb-2 block text-sm text-cafe">
                Mínimo por cuota (ARS) <span class="text-rojizo">*</span>
              </label>
              <input
                id="monto-minimo"
                v-model="montoMinimoCuota"
                type="number"
                min="0"
                step="0.01"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
          </div>
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            El mínimo por cuota solo se exige cuando hay más de un hito. Evita cuotas
            demasiado chicas frente a los costos fijos del procesamiento del pago.
          </p>
        </div>

        <div class="border-t border-rosa/40 pt-6">
          <h2 class="mb-4 text-base text-granate">Plazos</h2>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="dias-aprobacion" class="mb-2 block text-sm text-cafe">
                Aprobación automática (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-aprobacion"
                v-model="diasAprobacionAutomatica"
                type="number"
                min="1"
                max="60"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Si el cliente no responde, el entregable se aprueba solo.
              </p>
            </div>
            <div>
              <label for="dias-pago" class="mb-2 block text-sm text-cafe">
                Plazo para pagar una cuota (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-pago"
                v-model="diasPagoCuota"
                type="number"
                min="1"
                max="90"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Si el cliente no paga a tiempo, el pedido se cierra y el artista conserva lo cobrado.
              </p>
            </div>
          </div>
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
            <dt class="text-xs text-cafe/50">Máximo de hitos</dt>
            <dd class="text-cafe">{{ config.maxHitos }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Primera cuota</dt>
            <dd class="text-cafe">hasta {{ formatearMonto(config.porcentajeMaxPrimerHito) }}%</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Mínimo por cuota</dt>
            <dd class="text-cafe">$ {{ formatearMonto(config.montoMinimoCuota) }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Aprobación automática</dt>
            <dd class="text-cafe">{{ config.diasAprobacionAutomatica }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Plazo de pago</dt>
            <dd class="text-cafe">{{ config.diasPagoCuota }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Última modificación</dt>
            <dd class="text-right text-cafe">
              {{ formatearFecha(config.fechaActualizacion) }}
            </dd>
          </div>
        </dl>
        <p class="mt-4 border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          Los presupuestos ya enviados conservan el plan de hitos con el que se emitieron.
        </p>
      </aside>
    </div>
  </div>
</template>
