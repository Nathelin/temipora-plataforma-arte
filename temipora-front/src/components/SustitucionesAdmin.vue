<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../servicios/api'

const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)
const resumen = ref(null)
const procesandoCierres = ref(false)
const resultadoCierres = ref(null)

const form = ref({
  habilitado: true,
  maxAlternativas: 6,
  maxPorArtista: 1,
  rangoPrecioPct: 40,
  bonusFavorito: 0.1,
  factorTiempoMax: 3,
  mostrarEnExplorar: true,
  minutosDedupe: 30,
  diasRetencion: 180,
  emailCierreHabilitado: true,
  emailCierreMinutos: 10,
  emailCierreMax: 5,
  emailCierreVigenciaHoras: 72,
})

const tasaTexto = computed(() => {
  const valor = Number(resumen.value?.tasaSustitucion ?? 0)
  return `${valor.toLocaleString('es-AR', { maximumFractionDigits: 2 })}%`
})

const bonusTexto = computed(() => {
  const valor = Number(form.value.bonusFavorito) || 0
  return `${(valor * 100).toLocaleString('es-AR', { maximumFractionDigits: 1 })}%`
})

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [configRes, resumenRes] = await Promise.all([
      api.get('/admin/configuracion-sustitucion'),
      api.get('/admin/sustituciones/resumen'),
    ])
    config.value = configRes.data
    resumen.value = resumenRes.data
    const d = configRes.data || {}
    form.value = {
      habilitado: d.habilitado !== false,
      maxAlternativas: Number(d.maxAlternativas ?? 6),
      maxPorArtista: Number(d.maxPorArtista ?? 1),
      rangoPrecioPct: Number(d.rangoPrecioPct ?? 40),
      bonusFavorito: Number(d.bonusFavorito ?? 0.1),
      factorTiempoMax: Number(d.factorTiempoMax ?? 3),
      mostrarEnExplorar: d.mostrarEnExplorar !== false,
      minutosDedupe: Number(d.minutosDedupe ?? 30),
      diasRetencion: Number(d.diasRetencion ?? 180),
      emailCierreHabilitado: d.emailCierreHabilitado !== false,
      emailCierreMinutos: Number(d.emailCierreMinutos ?? 10),
      emailCierreMax: Number(d.emailCierreMax ?? 5),
      emailCierreVigenciaHoras: Number(d.emailCierreVigenciaHoras ?? 72),
    }
  } catch (e) {
    error.value =
      e.response?.data?.message ||
      'No se pudo cargar la configuración de sustituciones.'
  } finally {
    cargando.value = false
  }
}

function validar() {
  const f = form.value
  if (f.maxAlternativas < 1 || f.maxAlternativas > 20) {
    return 'Podés ofrecer entre 1 y 20 alternativas por búsqueda.'
  }
  if (f.maxPorArtista < 1 || f.maxPorArtista > 5) {
    return 'Podés mostrar entre 1 y 5 alternativas del mismo artista.'
  }
  if (f.rangoPrecioPct < 0 || f.rangoPrecioPct > 200) {
    return 'La diferencia de precio admitida debe estar entre 0 y 200%.'
  }
  if (f.bonusFavorito < 0 || f.bonusFavorito > 1) {
    return 'El peso extra para artistas seguidos debe estar entre 0 y 1.'
  }
  if (f.factorTiempoMax < 1 || f.factorTiempoMax > 10) {
    return 'El tope de tiempo de entrega debe estar entre 1 y 10 veces.'
  }
  if (f.minutosDedupe < 0 || f.minutosDedupe > 1440) {
    return 'Los minutos de agrupación deben estar entre 0 y 1440.'
  }
  if (f.diasRetencion < 0) {
    return 'Los días de retención no pueden ser negativos.'
  }
  if (f.emailCierreMinutos < 0 || f.emailCierreMinutos > 1440) {
    return 'La agrupación de mails debe estar entre 0 y 1440 minutos.'
  }
  if (f.emailCierreMax < 1 || f.emailCierreMax > 20) {
    return 'Cada mail puede incluir entre 1 y 20 cierres.'
  }
  if (f.emailCierreVigenciaHoras < 1 || f.emailCierreVigenciaHoras > 720) {
    return 'El enlace del mail debe durar entre 1 y 720 horas.'
  }
  return ''
}

function payload() {
  return {
    habilitado: form.value.habilitado,
    maxAlternativas: Number(form.value.maxAlternativas),
    maxPorArtista: Number(form.value.maxPorArtista),
    rangoPrecioPct: Number(form.value.rangoPrecioPct),
    bonusFavorito: Number(form.value.bonusFavorito),
    factorTiempoMax: Number(form.value.factorTiempoMax),
    mostrarEnExplorar: form.value.mostrarEnExplorar,
    minutosDedupe: Number(form.value.minutosDedupe),
    diasRetencion: Number(form.value.diasRetencion),
    emailCierreHabilitado: form.value.emailCierreHabilitado,
    emailCierreMinutos: Number(form.value.emailCierreMinutos),
    emailCierreMax: Number(form.value.emailCierreMax),
    emailCierreVigenciaHoras: Number(form.value.emailCierreVigenciaHoras),
  }
}

async function guardar() {
  errorAccion.value = ''
  mensaje.value = ''
  const problema = validar()
  if (problema) {
    errorAccion.value = problema
    return
  }
  guardando.value = true
  try {
    const res = await api.put('/admin/configuracion-sustitucion', payload())
    config.value = res.data
    mensaje.value = 'Configuración guardada.'
  } catch (e) {
    errorAccion.value =
      e.response?.data?.message ||
      'No se pudo guardar la configuración de sustituciones.'
  } finally {
    guardando.value = false
  }
}

async function procesarCierres() {
  if (procesandoCierres.value) return
  procesandoCierres.value = true
  errorAccion.value = ''
  mensaje.value = ''
  try {
    const { data } = await api.post('/admin/cierres-email/procesar')
    resultadoCierres.value = data
    mensaje.value = data.enviados
      ? `Se enviaron ${data.enviados} mail(s) de cierre.`
      : 'No había mails listos para enviar.'
    const resumenRes = await api.get('/admin/sustituciones/resumen')
    resumen.value = resumenRes.data
  } catch (e) {
    errorAccion.value =
      e.response?.data?.message ||
      'No se pudieron procesar los mails pendientes.'
  } finally {
    procesandoCierres.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comercio"
      titulo="Sustituciones de artistas"
      descripcion="Cuando un servicio no puede recibir una solicitud, la plataforma ofrece servicios parecidos de otros artistas que sí tienen cupo. Acá definís cuántas alternativas se muestran y con qué criterio se eligen."
    />

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <section>
          <h2 class="text-base text-granate">Estado del proceso</h2>
          <label class="mt-4 flex items-start gap-3 rounded-2xl bg-crema p-4">
            <input
              v-model="form.habilitado"
              type="checkbox"
              class="mt-0.5 h-4 w-4 rounded border-borde text-coral focus:ring-coral"
            />
            <span class="text-sm text-cafe">
              Mostrar artistas con cupo cuando un servicio no puede recibir la solicitud.
              <span class="mt-0.5 block text-xs text-cafe/55">
                Si lo desactivás, el detalle sigue avisando el estado pero no sugiere alternativas.
              </span>
            </span>
          </label>

          <label class="mt-3 flex items-start gap-3 rounded-2xl bg-crema p-4">
            <input
              v-model="form.mostrarEnExplorar"
              type="checkbox"
              class="mt-0.5 h-4 w-4 rounded border-borde text-coral focus:ring-coral"
            />
            <span class="text-sm text-cafe">
              Mostrar los servicios sin cupos en Explorar.
              <span class="mt-0.5 block text-xs text-cafe/55">
                Siempre aparecen después de los disponibles y con la marca “Sin cupos”.
              </span>
            </span>
          </label>
        </section>

        <section class="border-t border-rosa/40 pt-6">
          <h2 class="text-base text-granate">Alternativas</h2>
          <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="max-alternativas" class="mb-1.5 block text-sm text-cafe">
                Alternativas por búsqueda
              </label>
              <input
                id="max-alternativas"
                v-model.number="form.maxAlternativas"
                type="number"
                min="1"
                max="20"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="max-por-artista" class="mb-1.5 block text-sm text-cafe">
                Alternativas por artista
              </label>
              <input
                id="max-por-artista"
                v-model.number="form.maxPorArtista"
                type="number"
                min="1"
                max="5"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Evita que todas las sugerencias sean del mismo artista.
              </p>
            </div>
            <div>
              <label for="rango-precio" class="mb-1.5 block text-sm text-cafe">
                Diferencia de precio admitida (%)
              </label>
              <input
                id="rango-precio"
                v-model.number="form.rangoPrecioPct"
                type="number"
                min="0"
                max="200"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Cuánto puede alejarse el precio de una alternativa del servicio original.
              </p>
            </div>
            <div>
              <label for="factor-tiempo" class="mb-1.5 block text-sm text-cafe">
                Tope de tiempo de entrega (veces)
              </label>
              <input
                id="factor-tiempo"
                v-model.number="form.factorTiempoMax"
                type="number"
                min="1"
                max="10"
                step="0.5"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Se descartan alternativas que tarden mucho más que el servicio original.
              </p>
            </div>
            <div>
              <label for="bonus-favorito" class="mb-1.5 block text-sm text-cafe">
                Peso extra para artistas que el cliente sigue
              </label>
              <input
                id="bonus-favorito"
                v-model.number="form.bonusFavorito"
                type="number"
                min="0"
                max="1"
                step="0.01"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Valor entre 0 y 1. Hoy equivale a {{ bonusTexto }} del puntaje.
              </p>
            </div>
          </div>
        </section>

        <section class="border-t border-rosa/40 pt-6">
          <h2 class="text-base text-granate">Registro</h2>
          <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="minutos-dedupe" class="mb-1.5 block text-sm text-cafe">
                Minutos para agrupar visitas repetidas
              </label>
              <input
                id="minutos-dedupe"
                v-model.number="form.minutosDedupe"
                type="number"
                min="0"
                max="1440"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Si el cliente vuelve a mirar las alternativas dentro de esta ventana,
                se reutiliza el mismo registro. 0 desactiva la agrupación.
              </p>
            </div>
            <div>
              <label for="dias-retencion" class="mb-1.5 block text-sm text-cafe">
                Días de retención del registro
              </label>
              <input
                id="dias-retencion"
                v-model.number="form.diasRetencion"
                type="number"
                min="0"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Reservado para la limpieza automática de eventos antiguos.
              </p>
            </div>
          </div>
        </section>

        <section class="border-t border-rosa/40 pt-6">
          <h2 class="text-base text-granate">Mails de cierre</h2>
          <p class="mt-1 text-xs leading-relaxed text-cafe/50">
            Cuando una solicitud o un pedido se cierra por una causa que el cliente no
            eligió (rechazo, retiro, vencimiento, baja del servicio o impago), se le avisa
            por mail con el resumen y alternativas. Los cierres se agrupan por usuario.
          </p>

          <label class="mt-4 flex items-start gap-3 rounded-2xl bg-crema p-4">
            <input
              v-model="form.emailCierreHabilitado"
              type="checkbox"
              class="mt-0.5 h-4 w-4 rounded border-borde text-coral focus:ring-coral"
            />
            <span class="text-sm text-cafe">
              Enviar mail de cierre al cliente.
              <span class="mt-0.5 block text-xs text-cafe/55">
                No depende de la preferencia de promociones: es un aviso transaccional.
              </span>
            </span>
          </label>

          <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div>
              <label for="email-cierre-minutos" class="mb-1.5 block text-sm text-cafe">
                Agrupar durante (minutos)
              </label>
              <input
                id="email-cierre-minutos"
                v-model.number="form.emailCierreMinutos"
                type="number"
                min="0"
                max="1440"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                0 envía en la próxima corrida del job.
              </p>
            </div>
            <div>
              <label for="email-cierre-max" class="mb-1.5 block text-sm text-cafe">
                Cierres por mail
              </label>
              <input
                id="email-cierre-max"
                v-model.number="form.emailCierreMax"
                type="number"
                min="1"
                max="20"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="email-cierre-vigencia" class="mb-1.5 block text-sm text-cafe">
                Vigencia del enlace (horas)
              </label>
              <input
                id="email-cierre-vigencia"
                v-model.number="form.emailCierreVigenciaHoras"
                type="number"
                min="1"
                max="720"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
          </div>
        </section>

        <p v-if="errorAccion" class="text-sm text-rojizo">{{ errorAccion }}</p>
        <p v-else-if="mensaje" class="text-sm text-green-700">{{ mensaje }}</p>

        <div class="flex flex-wrap items-center gap-3 border-t border-rosa/40 pt-6">
          <button
            :disabled="guardando"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
            @click="guardar"
          >
            {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
          </button>
          <button
            type="button"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
            :disabled="procesandoCierres"
            @click="procesarCierres"
          >
            {{ procesandoCierres ? 'Procesando...' : 'Procesar mails pendientes' }}
          </button>
        </div>
      </div>

      <aside class="h-fit space-y-5 rounded-2xl border border-rosa/50 bg-blanco p-6 lg:sticky lg:top-16">
        <div>
          <h2 class="mb-4 text-base text-granate">Resumen real</h2>
          <dl class="space-y-4 text-sm">
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Búsquedas de alternativas</dt>
              <dd class="font-semibold text-granate">{{ resumen?.totalEventos ?? 0 }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Resueltas con otra artista</dt>
              <dd class="text-cafe">{{ resumen?.resueltasConAlternativa ?? 0 }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Resueltas quedando en cola</dt>
              <dd class="text-cafe">{{ resumen?.resueltasConCola ?? 0 }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Tasa de sustitución</dt>
              <dd class="font-semibold text-granate">{{ tasaTexto }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Abiertas sin resolver</dt>
              <dd class="text-cafe">{{ resumen?.abiertas ?? 0 }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Mails de cierre pendientes</dt>
              <dd class="font-semibold text-granate">{{ resumen?.emailsCierrePendientes ?? 0 }}</dd>
            </div>
          </dl>
        </div>

        <div v-if="resumen?.origenesFrecuentes?.length" class="border-t border-rosa/40 pt-4">
          <h3 class="text-sm font-semibold text-granate">Servicios que más alternativas disparan</h3>
          <ul class="mt-2 space-y-2 text-xs text-cafe/70">
            <li
              v-for="origen in resumen.origenesFrecuentes"
              :key="origen.idServicio"
              class="flex justify-between gap-3"
            >
              <span class="min-w-0 break-words">{{ origen.nombre }}</span>
              <span class="shrink-0 font-semibold text-granate">{{ origen.cantidad }}</span>
            </li>
          </ul>
        </div>

        <p class="border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          Los cambios se aplican a las próximas búsquedas, sin reiniciar el servidor.
          El registro histórico conserva el precio y el motivo con los que se mostró
          cada alternativa.
        </p>
      </aside>
    </div>
  </div>
</template>
