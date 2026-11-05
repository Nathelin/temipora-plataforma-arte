<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../servicios/api'
import ModalConfirmacion from './ModalConfirmacion.vue'

const cargando = ref(true)
const guardando = ref(false)
const ejecutando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)
const comisionServicio = ref(0)
const modalEjecutar = ref(false)
const resultadoEjecucion = ref(null)

const form = ref({
  diasSinCompra: 60,
  diasSinConexion: 30,
  diasCooldown: 30,
  diasMinimosCuenta: 7,
  porcentajeDescuento: 10,
  vigenciaHoras: 72,
  maxOfertasPorUsuario: 3,
  maxUsuariosPorCorrida: 50,
  horaEjecucion: '09:00',
})

const comisionTexto = computed(() => `${formatearNumero(comisionServicio.value)}%`)
const descuentoEfectivo = computed(() => {
  const valor = Number(form.value.porcentajeDescuento) || 0
  return Math.min(valor, comisionServicio.value)
})
const superaComision = computed(
  () => Number(form.value.porcentajeDescuento) > comisionServicio.value,
)

function formatearNumero(valor) {
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return '0'
  return numero.toLocaleString('es-AR', { maximumFractionDigits: 2 })
}

function formatearFecha(valor) {
  if (!valor) return '—'
  try {
    return new Date(valor).toLocaleString('es-AR')
  } catch {
    return valor
  }
}

function normalizarHora(valor) {
  if (!valor) return '09:00'
  return String(valor).slice(0, 5)
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [configRes, comisionRes] = await Promise.all([
      api.get('/admin/configuracion-inactividad'),
      api.get('/admin/configuracion-comision'),
    ])
    config.value = configRes.data
    comisionServicio.value = Number(comisionRes.data?.porcentajeServicio ?? 0)
    const d = configRes.data || {}
    form.value = {
      diasSinCompra: Number(d.diasSinCompra ?? 60),
      diasSinConexion: Number(d.diasSinConexion ?? 30),
      diasCooldown: Number(d.diasCooldown ?? 30),
      diasMinimosCuenta: Number(d.diasMinimosCuenta ?? 7),
      porcentajeDescuento: Number(d.porcentajeDescuento ?? 10),
      vigenciaHoras: Number(d.vigenciaHoras ?? 72),
      maxOfertasPorUsuario: Number(d.maxOfertasPorUsuario ?? 3),
      maxUsuariosPorCorrida: Number(d.maxUsuariosPorCorrida ?? 50),
      horaEjecucion: normalizarHora(d.horaEjecucion),
    }
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la configuración de promociones.'
  } finally {
    cargando.value = false
  }
}

function validar() {
  const f = form.value
  if (f.diasSinCompra < 0 || f.diasSinConexion < 0
    || f.diasCooldown < 0 || f.diasMinimosCuenta < 0) {
    return 'Los días no pueden ser negativos.'
  }
  if (!/^\d{2}:\d{2}$/.test(f.horaEjecucion)) {
    return 'Ingresá una hora de envío válida.'
  }
  if (f.vigenciaHoras < 1) {
    return 'La vigencia de la oferta debe ser de al menos 1 hora.'
  }
  if (f.maxOfertasPorUsuario < 1) {
    return 'Tenés que ofrecer al menos 1 servicio por usuario.'
  }
  if (f.maxUsuariosPorCorrida < 1) {
    return 'La ejecución debe procesar al menos 1 usuario.'
  }
  if (f.porcentajeDescuento < 0) {
    return 'El descuento no puede ser negativo.'
  }
  if (f.porcentajeDescuento > comisionServicio.value) {
    return `El descuento no puede superar la comisión vigente (${comisionTexto.value}). Cambiala en Comisiones si querés descontar más.`
  }
  return ''
}

function payload() {
  return {
    diasSinCompra: Number(form.value.diasSinCompra),
    diasSinConexion: Number(form.value.diasSinConexion),
    diasCooldown: Number(form.value.diasCooldown),
    diasMinimosCuenta: Number(form.value.diasMinimosCuenta),
    porcentajeDescuento: Number(form.value.porcentajeDescuento),
    vigenciaHoras: Number(form.value.vigenciaHoras),
    maxOfertasPorUsuario: Number(form.value.maxOfertasPorUsuario),
    maxUsuariosPorCorrida: Number(form.value.maxUsuariosPorCorrida),
    horaEjecucion: form.value.horaEjecucion,
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
    const res = await api.put('/admin/configuracion-inactividad', payload())
    config.value = res.data
    mensaje.value = 'Promociones guardadas.'
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudieron guardar las promociones.'
  } finally {
    guardando.value = false
  }
}

function abrirEjecucion() {
  errorAccion.value = ''
  mensaje.value = ''
  resultadoEjecucion.value = null
  modalEjecutar.value = true
}

async function confirmarEjecucion() {
  if (ejecutando.value) return
  ejecutando.value = true
  errorAccion.value = ''
  try {
    const res = await api.post('/admin/reactivacion/ejecutar')
    resultadoEjecucion.value = Number(res.data?.campanas ?? 0)
    modalEjecutar.value = false
    mensaje.value = resultadoEjecucion.value > 0
      ? `Se generaron ${resultadoEjecucion.value} campaña(s).`
      : 'No había usuarios inactivos para procesar.'
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudo ejecutar la campaña.'
  } finally {
    ejecutando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comercio"
      titulo="Promociones por inactividad"
      descripcion="El proceso detecta clientes que no compraron en la ventana configurada o que no se loguearon en la otra, les arma una campaña con servicios recomendados y les envía una oferta."
    />

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div
      v-else
      class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]"
    >
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <section>
          <h2 class="text-base text-granate">Ventana de inactividad</h2>
          <p class="mt-1 text-xs leading-relaxed text-cafe/50">
            Alcanza con que se cumpla una de las dos condiciones: sin compras o
            sin conexión durante los días indicados.
          </p>

          <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="dias-sin-compra" class="mb-1.5 block text-sm text-cafe">
                Días sin compra
              </label>
              <input
                id="dias-sin-compra"
                v-model.number="form.diasSinCompra"
                type="number"
                min="0"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="dias-sin-conexion" class="mb-1.5 block text-sm text-cafe">
                Días sin conexión
              </label>
              <input
                id="dias-sin-conexion"
                v-model.number="form.diasSinConexion"
                type="number"
                min="0"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="dias-minimos-cuenta" class="mb-1.5 block text-sm text-cafe">
                Días mínimos de cuenta
              </label>
              <input
                id="dias-minimos-cuenta"
                v-model.number="form.diasMinimosCuenta"
                type="number"
                min="0"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Evita mandar promociones a cuentas recién creadas.
              </p>
            </div>
            <div>
              <label for="dias-cooldown" class="mb-1.5 block text-sm text-cafe">
                Cooldown entre campañas
              </label>
              <input
                id="dias-cooldown"
                v-model.number="form.diasCooldown"
                type="number"
                min="0"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                No se vuelve a ofrecer al mismo usuario antes de estos días.
              </p>
            </div>
          </div>
        </section>

        <section class="border-t border-rosa/40 pt-6">
          <h2 class="text-base text-granate">Programación</h2>
          <p class="mt-1 text-xs leading-relaxed text-cafe/50">
            El proceso corre todos los días a la hora indicada, en la zona horaria del servidor.
          </p>

          <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="hora-ejecucion" class="mb-1.5 block text-sm text-cafe">
                Hora de envío
              </label>
              <input
                id="hora-ejecucion"
                v-model="form.horaEjecucion"
                type="time"
                required
                class="block w-40 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Si el servidor estuvo caído a esa hora, la ejecución se recupera al volver durante el mismo día.
              </p>
            </div>
          </div>
        </section>

        <section class="border-t border-rosa/40 pt-6">
          <h2 class="text-base text-granate">Oferta</h2>

          <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="porcentaje-descuento" class="mb-1.5 block text-sm text-cafe">
                Descuento
              </label>
              <div class="flex items-center gap-2">
                <input
                  id="porcentaje-descuento"
                  v-model.number="form.porcentajeDescuento"
                  type="number"
                  min="0"
                  :max="comisionServicio"
                  step="0.01"
                  class="block w-32 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                />
                <span class="text-sm text-cafe/60">%</span>
              </div>
              <p class="mt-1.5 text-xs text-cafe/50">
                Máximo: {{ comisionTexto }} (comisión vigente).
              </p>
            </div>
            <div>
              <label for="vigencia-horas" class="mb-1.5 block text-sm text-cafe">
                Vigencia de la oferta
              </label>
              <div class="flex items-center gap-2">
                <input
                  id="vigencia-horas"
                  v-model.number="form.vigenciaHoras"
                  type="number"
                  min="1"
                  step="1"
                  class="block w-32 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                />
                <span class="text-sm text-cafe/60">horas</span>
              </div>
            </div>
            <div>
              <label for="max-ofertas" class="mb-1.5 block text-sm text-cafe">
                Servicios ofrecidos por usuario
              </label>
              <input
                id="max-ofertas"
                v-model.number="form.maxOfertasPorUsuario"
                type="number"
                min="1"
                step="1"
                class="block w-32 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>
            <div>
              <label for="max-usuarios" class="mb-1.5 block text-sm text-cafe">
                Máximo de usuarios por ejecución
              </label>
              <input
                id="max-usuarios"
                v-model.number="form.maxUsuariosPorCorrida"
                type="number"
                min="1"
                step="1"
                class="block w-32 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-1.5 text-xs text-cafe/50">
                Cuántos usuarios inactivos se procesan como máximo cada vez que
                corre el proceso.
              </p>
            </div>
          </div>

          <div
            class="mt-5 rounded-xl border px-4 py-3 text-sm leading-relaxed"
            :class="superaComision
              ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
              : 'border-coral/25 bg-coral/5 text-cafe/75'"
          >
            <p v-if="superaComision" class="font-semibold">
              El descuento supera la comisión vigente ({{ comisionTexto }}).
            </p>
            <p v-else class="font-semibold text-granate">
              El descuento lo financia Temipora.
            </p>
            <p class="mt-1">
              El artista siempre cobra su neto completo. Si querés descontar más
              que {{ comisionTexto }}, primero tenés que subir la comisión en
              <router-link to="/admin/ajustes/comisiones" class="font-semibold underline decoration-coral/40 underline-offset-4">
                Comisiones
              </router-link>.
            </p>
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
            :disabled="ejecutando"
            @click="abrirEjecucion"
          >
            Ejecutar ahora
          </button>
        </div>
      </div>

      <aside class="h-fit space-y-5 rounded-2xl border border-rosa/50 bg-blanco p-6 lg:sticky lg:top-16">
        <div>
          <h2 class="mb-4 text-base text-granate">Estado actual</h2>
          <dl class="space-y-4 text-sm">
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Comisión de servicios</dt>
              <dd class="text-cafe">{{ comisionTexto }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Descuento máximo</dt>
              <dd class="text-cafe">{{ comisionTexto }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Descuento efectivo (lo financia Temipora)</dt>
              <dd class="font-semibold text-granate">{{ formatearNumero(descuentoEfectivo) }}%</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Última modificación</dt>
              <dd class="text-right text-cafe">
                {{ formatearFecha(config?.fechaActualizacion) }}
              </dd>
            </div>
          </dl>
        </div>

        <div class="border-t border-rosa/40 pt-4">
          <h3 class="text-sm font-semibold text-granate">Ejecución</h3>
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            El proceso también corre automáticamente todos los días a las {{ form.horaEjecucion }}.
            “Ejecutar ahora” no espera a esa hora.
          </p>
          <p v-if="resultadoEjecucion != null" class="mt-3 text-xs font-semibold text-cafe">
            Última ejecución: {{ resultadoEjecucion }} campaña(s).
          </p>
        </div>

        <p class="border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          Las ofertas ya enviadas conservan el porcentaje y la vigencia con los
          que se generaron. Los cambios aplican a las próximas campañas.
        </p>
      </aside>
    </div>

    <ModalConfirmacion
      :abierto="modalEjecutar"
      titulo="Ejecutar promociones ahora"
      mensaje="Se van a generar campañas para los usuarios inactivos y se enviarán emails a quienes tengan la preferencia activa. Puede tardar unos segundos. ¿Querés ejecutarlo?"
      texto-confirmar="Ejecutar ahora"
      @confirmar="confirmarEjecucion"
      @cerrar="modalEjecutar = false"
    />
  </div>
</template>
