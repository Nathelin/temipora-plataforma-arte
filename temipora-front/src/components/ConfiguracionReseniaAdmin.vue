<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../servicios/api'
import { formatearPromedio } from '../servicios/resenias'

const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)

const diasVentanaCreacion = ref('30')
const diasEdicion = ref('3')
const minimoReseniasEtiqueta = ref('3')
const maxImagenesResenia = ref('4')
const umbralExcelente = ref('4.5')
const umbralBuena = ref('3.5')

const reglaEtiquetas = computed(() => {
  const excelente = Number(String(umbralExcelente.value).replace(',', '.'))
  const buena = Number(String(umbralBuena.value).replace(',', '.'))
  if (!Number.isFinite(excelente) || !Number.isFinite(buena)) return ''
  return `≥ ${formatearPromedio(excelente)} Excelente · ≥ ${formatearPromedio(buena)} Buena · < ${formatearPromedio(buena)} Regular`
})

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/configuracion-resenia')
    config.value = res.data
    diasVentanaCreacion.value = res.data?.diasVentanaCreacion ?? 30
    diasEdicion.value = res.data?.diasEdicion ?? 3
    minimoReseniasEtiqueta.value = res.data?.minimoReseniasEtiqueta ?? 3
    maxImagenesResenia.value = res.data?.maxImagenesResenia ?? 4
    umbralExcelente.value = res.data?.umbralExcelente ?? 4.5
    umbralBuena.value = res.data?.umbralBuena ?? 3.5
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la configuración de reseñas.'
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
    entero(diasVentanaCreacion.value, 0, 365, 'La ventana para dejar reseñas'),
    entero(diasEdicion.value, 1, 30, 'La ventana de edición'),
    entero(minimoReseniasEtiqueta.value, 1, 50, 'El mínimo de reseñas'),
    entero(maxImagenesResenia.value, 0, 10, 'El máximo de imágenes por reseña'),
    decimal(umbralExcelente.value, 1, 5, 'El umbral de excelencia'),
    decimal(umbralBuena.value, 1, 5, 'El umbral de buena reputación'),
  ].filter(Boolean)

  if (!errores.length) {
    const excelente = Number(String(umbralExcelente.value).replace(',', '.'))
    const buena = Number(String(umbralBuena.value).replace(',', '.'))
    if (excelente <= buena) {
      errores.push('El umbral de excelencia debe ser mayor que el de buena reputación.')
    }
  }

  if (errores.length) {
    errorAccion.value = errores[0]
    return
  }

  guardando.value = true
  try {
    const res = await api.put('/admin/configuracion-resenia', {
      diasVentanaCreacion: Number(diasVentanaCreacion.value),
      diasEdicion: Number(diasEdicion.value),
      minimoReseniasEtiqueta: Number(minimoReseniasEtiqueta.value),
      maxImagenesResenia: Number(maxImagenesResenia.value),
      umbralExcelente: Number(String(umbralExcelente.value).replace(',', '.')),
      umbralBuena: Number(String(umbralBuena.value).replace(',', '.')),
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
    <h1 class="mb-6 text-xl font-normal text-granate">Reseñas</h1>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <div>
          <h2 class="mb-4 text-base text-granate">Plazos</h2>
          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="dias-ventana" class="mb-2 block text-sm text-cafe">
                Días para dejar una reseña <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-ventana"
                v-model="diasVentanaCreacion"
                type="number"
                min="0"
                max="365"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Se cuenta desde que el pedido se finaliza. Con 0 no hay límite.
              </p>
            </div>
            <div>
              <label for="dias-edicion" class="mb-2 block text-sm text-cafe">
                Días para editar una reseña <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-edicion"
                v-model="diasEdicion"
                type="number"
                min="1"
                max="30"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                La reseña se puede editar una sola vez dentro de este plazo.
              </p>
            </div>
          </div>
        </div>

        <div class="border-t border-rosa/40 pt-6">
          <h2 class="mb-4 text-base text-granate">Imágenes en reseñas</h2>
          <p class="mb-4 text-xs leading-relaxed text-cafe/55">
            El cliente puede adjuntar imágenes de la entrega final a su reseña del servicio.
            Con 0 se desactiva la función.
          </p>
          <div class="max-w-xs">
            <label for="max-imagenes" class="mb-2 block text-sm text-cafe">
              Máximo de imágenes por reseña <span class="text-rojizo">*</span>
            </label>
            <input
              id="max-imagenes"
              v-model="maxImagenesResenia"
              type="number"
              min="0"
              max="10"
              step="1"
              class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <p class="mt-2 text-xs leading-relaxed text-cafe/50">
              Entre 0 y 10. Solo se pueden elegir imágenes de la entrega final del pedido
              y el pedido debe estar autorizado para publicarse.
            </p>
          </div>
        </div>

        <div class="border-t border-rosa/40 pt-6">
          <h2 class="mb-4 text-base text-granate">Reputación del cliente</h2>
          <p class="mb-4 text-xs leading-relaxed text-cafe/55">
            La etiqueta se calcula con el promedio de las calificaciones generales que los
            artistas dejan a un cliente. Es información interna: el cliente no la ve.
          </p>

          <div class="grid gap-4 sm:grid-cols-3">
            <div>
              <label for="minimo-resenias" class="mb-2 block text-sm text-cafe">
                Mínimo de reseñas <span class="text-rojizo">*</span>
              </label>
              <input
                id="minimo-resenias"
                v-model="minimoReseniasEtiqueta"
                type="number"
                min="1"
                max="50"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="umbral-excelente" class="mb-2 block text-sm text-cafe">
                Umbral Excelente <span class="text-rojizo">*</span>
              </label>
              <input
                id="umbral-excelente"
                v-model="umbralExcelente"
                type="number"
                min="1"
                max="5"
                step="0.01"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="umbral-buena" class="mb-2 block text-sm text-cafe">
                Umbral Buena <span class="text-rojizo">*</span>
              </label>
              <input
                id="umbral-buena"
                v-model="umbralBuena"
                type="number"
                min="1"
                max="5"
                step="0.01"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
          </div>

          <p class="mt-3 rounded-lg bg-rosa/10 px-3 py-2 text-xs leading-relaxed text-cafe/70">
            {{ reglaEtiquetas }}
          </p>
        </div>

        <p v-if="errorAccion" class="text-sm text-rojizo">{{ errorAccion }}</p>
        <p v-else-if="mensaje" class="text-sm text-green-700">{{ mensaje }}</p>

        <div class="flex flex-wrap items-center gap-3 pt-2">
          <button
            type="button"
            :disabled="guardando"
            class="rounded-md bg-coral px-4 py-2 text-sm text-white shadow-sm transition hover:bg-rojizo disabled:opacity-50"
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
            <dt class="text-xs text-cafe/50">Ventana para reseñar</dt>
            <dd class="text-cafe">
              {{ config.diasVentanaCreacion === 0 ? 'Sin límite' : `${config.diasVentanaCreacion} días` }}
            </dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Ventana de edición</dt>
            <dd class="text-cafe">{{ config.diasEdicion }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Mínimo de reseñas</dt>
            <dd class="text-cafe">{{ config.minimoReseniasEtiqueta }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Máximo de imágenes</dt>
            <dd class="text-cafe">
              {{ config.maxImagenesResenia === 0 ? 'Desactivado' : config.maxImagenesResenia }}
            </dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Excelente desde</dt>
            <dd class="text-cafe">{{ formatearPromedio(config.umbralExcelente) }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Buena desde</dt>
            <dd class="text-cafe">{{ formatearPromedio(config.umbralBuena) }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Regular</dt>
            <dd class="text-cafe">Por debajo de {{ formatearPromedio(config.umbralBuena) }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Última modificación</dt>
            <dd class="text-right text-cafe">{{ formatearFecha(config.fechaActualizacion) }}</dd>
          </div>
        </dl>
        <p class="mt-4 border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          Los cambios aplican a las reseñas nuevas y a las etiquetas que se calculen desde ahora.
        </p>
      </aside>
    </div>
  </div>
</template>
