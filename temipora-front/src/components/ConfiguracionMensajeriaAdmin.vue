<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../servicios/api'

const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)

const longitudMaximaMensaje = ref('2000')
const longitudMaximaMotivo = ref('200')
const horasEsperaReenvio = ref('168')
const maxSolicitudesPendientes = ref('5')
const maxSolicitudesPorDia = ref('10')
const maxMensajesPorDia = ref('100')
const diasVencimientoSolicitud = ref('30')

function formatearHoras(valor) {
  const horas = Number(valor)
  if (!Number.isInteger(horas) || horas < 0) return '—'
  if (horas === 0) return 'Reenvío inmediato'
  if (horas % 24 === 0) {
    const dias = horas / 24
    return `${horas} h (${dias} ${dias === 1 ? 'día' : 'días'})`
  }
  return `${horas} h`
}

const horasTexto = computed(() =>
  formatearHoras(config.value?.horasEsperaReenvio))

const diasVencimientoTexto = computed(() => {
  const dias = Number(config.value?.diasVencimientoSolicitud)
  if (!Number.isInteger(dias) || dias < 0) return '—'
  if (dias === 0) return 'Nunca'
  return `${dias} ${dias === 1 ? 'día' : 'días'}`
})

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/configuracion-mensajeria')
    config.value = res.data
    longitudMaximaMensaje.value = res.data?.longitudMaximaMensaje ?? 2000
    longitudMaximaMotivo.value = res.data?.longitudMaximaMotivo ?? 200
    horasEsperaReenvio.value = res.data?.horasEsperaReenvio ?? 168
    maxSolicitudesPendientes.value = res.data?.maxSolicitudesPendientes ?? 5
    maxSolicitudesPorDia.value = res.data?.maxSolicitudesPorDia ?? 10
    maxMensajesPorDia.value = res.data?.maxMensajesPorDia ?? 100
    diasVencimientoSolicitud.value = res.data?.diasVencimientoSolicitud ?? 30
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la configuración de mensajería.'
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

async function guardar() {
  errorAccion.value = ''
  mensaje.value = ''

  const errores = [
    entero(longitudMaximaMensaje.value, 1, 4000, 'La longitud máxima del mensaje'),
    entero(longitudMaximaMotivo.value, 1, 200, 'La longitud máxima del motivo'),
    entero(horasEsperaReenvio.value, 0, 8760, 'Las horas de espera para reenviar'),
    entero(maxSolicitudesPendientes.value, 1, 100, 'El máximo de solicitudes pendientes'),
    entero(maxSolicitudesPorDia.value, 1, 1000, 'El máximo de solicitudes por día'),
    entero(maxMensajesPorDia.value, 1, 10000, 'El máximo de mensajes por día'),
    entero(
      diasVencimientoSolicitud.value,
      0,
      3650,
      'Los días para vencer una solicitud',
    ),
  ].filter(Boolean)

  if (errores.length) {
    errorAccion.value = errores[0]
    return
  }

  guardando.value = true
  try {
    const res = await api.put('/admin/configuracion-mensajeria', {
      longitudMaximaMensaje: Number(longitudMaximaMensaje.value),
      longitudMaximaMotivo: Number(longitudMaximaMotivo.value),
      horasEsperaReenvio: Number(horasEsperaReenvio.value),
      maxSolicitudesPendientes: Number(maxSolicitudesPendientes.value),
      maxSolicitudesPorDia: Number(maxSolicitudesPorDia.value),
      maxMensajesPorDia: Number(maxMensajesPorDia.value),
      diasVencimientoSolicitud: Number(diasVencimientoSolicitud.value),
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
      eyebrow="Sistema"
      titulo="Mensajería"
      descripcion="Límites de mensajes, solicitudes de contacto y anti-spam."
    />

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <div>
          <h2 class="mb-4 text-base text-granate">Límites de los mensajes</h2>
          <p class="mb-4 text-xs leading-relaxed text-cafe/55">
            Se validan en el servidor y también en el formulario del chat. Los mensajes
            ya enviados conservan su contenido.
          </p>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="longitud-mensaje" class="mb-2 block text-sm text-cafe">
                Caracteres por mensaje <span class="text-rojizo">*</span>
              </label>
              <input
                id="longitud-mensaje"
                v-model="longitudMaximaMensaje"
                type="number"
                min="1"
                max="4000"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Entre 1 y 4000. Por defecto, 2000.
              </p>
            </div>
            <div>
              <label for="longitud-motivo" class="mb-2 block text-sm text-cafe">
                Caracteres del motivo <span class="text-rojizo">*</span>
              </label>
              <input
                id="longitud-motivo"
                v-model="longitudMaximaMotivo"
                type="number"
                min="1"
                max="200"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Motivo de contacto y motivo de rechazo. Por defecto, 200.
              </p>
            </div>
          </div>
        </div>

        <div class="border-t border-rosa/40 pt-6">
          <h2 class="mb-4 text-base text-granate">Solicitudes y anti-spam</h2>
          <div class="grid gap-4 sm:grid-cols-3">
            <div>
              <label for="horas-reenvio" class="mb-2 block text-sm text-cafe">
                Horas para reenviar <span class="text-rojizo">*</span>
              </label>
              <input
                id="horas-reenvio"
                v-model="horasEsperaReenvio"
                type="number"
                min="0"
                max="8760"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Después de un rechazo. Con 0 se puede reenviar enseguida.
              </p>
            </div>
            <div>
              <label for="max-pendientes" class="mb-2 block text-sm text-cafe">
                Solicitudes pendientes <span class="text-rojizo">*</span>
              </label>
              <input
                id="max-pendientes"
                v-model="maxSolicitudesPendientes"
                type="number"
                min="1"
                max="100"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Máximo de solicitudes sin responder que puede acumular un usuario.
              </p>
            </div>
            <div>
              <label for="max-solicitudes-dia" class="mb-2 block text-sm text-cafe">
                Solicitudes por día <span class="text-rojizo">*</span>
              </label>
              <input
                id="max-solicitudes-dia"
                v-model="maxSolicitudesPorDia"
                type="number"
                min="1"
                max="1000"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Incluye las solicitudes nuevas y los reenvíos.
              </p>
            </div>
          </div>

          <div class="mt-4 sm:w-1/3">
            <label for="max-mensajes-dia" class="mb-2 block text-sm text-cafe">
              Mensajes por día <span class="text-rojizo">*</span>
            </label>
            <input
              id="max-mensajes-dia"
              v-model="maxMensajesPorDia"
              type="number"
              min="1"
              max="10000"
              step="1"
              class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <p class="mt-2 text-xs leading-relaxed text-cafe/50">
              Cupo diario por usuario para enviar mensajes.
            </p>
          </div>
        </div>

        <div class="border-t border-rosa/40 pt-6">
          <h2 class="mb-4 text-base text-granate">Vencimiento de solicitudes</h2>
          <div class="sm:w-1/3">
            <label for="dias-vencimiento" class="mb-2 block text-sm text-cafe">
              Días para vencer una solicitud <span class="text-rojizo">*</span>
            </label>
            <input
              id="dias-vencimiento"
              v-model="diasVencimientoSolicitud"
              type="number"
              min="0"
              max="3650"
              step="1"
              class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <p class="mt-2 text-xs leading-relaxed text-cafe/50">
              Si nadie responde en este plazo, la solicitud pasa a “Vencida” y
              quien la envió recibe un aviso. Con 0, no vence nunca.
            </p>
          </div>
        </div>

        <p v-if="errorAccion" class="text-sm text-rojizo">{{ errorAccion }}</p>
        <p v-else-if="mensaje" class="text-sm text-green-700">{{ mensaje }}</p>

        <div class="flex flex-wrap items-center gap-3 pt-2">
          <button
            type="button"
            :disabled="guardando"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
            @click="guardar"
          >
            {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
          </button>
          <button
            type="button"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
            @click="cargar"
          >
            Descartar cambios
          </button>
        </div>
      </div>

      <aside class="h-fit rounded-2xl border border-rosa/50 bg-blanco p-6 lg:sticky lg:top-16">
        <h2 class="mb-4 text-base text-granate">Estado actual</h2>
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Mensaje</dt>
            <dd class="text-cafe">{{ config.longitudMaximaMensaje }} caracteres</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Motivo</dt>
            <dd class="text-cafe">{{ config.longitudMaximaMotivo }} caracteres</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Reenvío tras rechazo</dt>
            <dd class="text-right text-cafe">{{ horasTexto }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Pendientes por usuario</dt>
            <dd class="text-cafe">{{ config.maxSolicitudesPendientes }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Solicitudes por día</dt>
            <dd class="text-cafe">{{ config.maxSolicitudesPorDia }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Mensajes por día</dt>
            <dd class="text-cafe">{{ config.maxMensajesPorDia }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Vencimiento de solicitudes</dt>
            <dd class="text-right text-cafe">{{ diasVencimientoTexto }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Última modificación</dt>
            <dd class="text-right text-cafe">{{ formatearFecha(config.fechaActualizacion) }}</dd>
          </div>
        </dl>
        <p class="mt-4 border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          Los cambios aplican a las solicitudes y mensajes nuevos. Las conversaciones
          existentes conservan sus mensajes.
        </p>
      </aside>
    </div>
  </div>
</template>
