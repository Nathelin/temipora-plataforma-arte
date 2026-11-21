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
const revisionesMaximasPorHito = ref('2')
const diasReentrega = ref('7')
const diasGraciaEntrega = ref('3')
const diasSancionVentas = ref('14')
const diasReapelacion = ref('7')
const diasCierreTrasBloqueo = ref('30')
const procesandoMails = ref(false)
const enviandoPrueba = ref(false)
const mensajeMails = ref('')
const errorMails = ref('')
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
    revisionesMaximasPorHito.value = res.data?.revisionesMaximasPorHito ?? 2
    diasReentrega.value = res.data?.diasReentrega ?? 7
    diasGraciaEntrega.value = res.data?.diasGraciaEntrega ?? 3
    diasSancionVentas.value = res.data?.diasSancionVentas ?? 14
    diasReapelacion.value = res.data?.diasReapelacion ?? 7
    diasCierreTrasBloqueo.value = res.data?.diasCierreTrasBloqueo ?? 30
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
    entero(diasPagoCuota.value, 1, 14, 'El plazo de pago'),
    entero(revisionesMaximasPorHito.value, 1, 10, 'El máximo de revisiones'),
    entero(diasReentrega.value, 1, 60, 'El plazo de reentrega'),
    entero(diasGraciaEntrega.value, 1, 30, 'La gracia de entrega'),
    entero(diasSancionVentas.value, 1, 60, 'La sanción de ventas'),
    entero(diasReapelacion.value, 1, 90, 'La espera entre apelaciones'),
    entero(diasCierreTrasBloqueo.value, 7, 180, 'El cierre tras el bloqueo'),
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
      revisionesMaximasPorHito: Number(revisionesMaximasPorHito.value),
      diasReentrega: Number(diasReentrega.value),
      diasGraciaEntrega: Number(diasGraciaEntrega.value),
      diasSancionVentas: Number(diasSancionVentas.value),
      diasReapelacion: Number(diasReapelacion.value),
      diasCierreTrasBloqueo: Number(diasCierreTrasBloqueo.value),
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

async function procesarMails() {
  if (procesandoMails.value) return
  procesandoMails.value = true
  mensajeMails.value = ''
  errorMails.value = ''
  try {
    const res = await api.post('/admin/recordatorios/procesar')
    const encolados = Number(res.data?.encolados || 0)
    const enviados = Number(res.data?.enviados || 0)
    mensajeMails.value = `${encolados} recordatorio(s) encolado(s) y `
      + `${enviados} mail(s) enviado(s).`
  } catch (e) {
    errorMails.value = e.response?.data?.message
      || 'No se pudieron procesar los mails pendientes.'
  } finally {
    procesandoMails.value = false
  }
}

async function enviarRecordatoriosDePrueba() {
  if (enviandoPrueba.value) return
  enviandoPrueba.value = true
  mensajeMails.value = ''
  errorMails.value = ''
  try {
    const res = await api.post('/admin/recordatorios/prueba')
    const enviados = Number(res.data?.enviados || 0)
    const destino = res.data?.correo || 'tu correo'
    mensajeMails.value = enviados === 2
      ? `Se enviaron los dos mails de prueba a ${destino}: uno de pagos y otro de entregas.`
      : enviados === 1
        ? `Se envió 1 de los 2 mails de prueba a ${destino}. Revisá Mailpit y la configuración.`
        : 'No se pudo enviar: revisá Mailpit y que el envío de mails esté habilitado.'
  } catch (e) {
    errorMails.value = e.response?.data?.message
      || 'No se pudieron enviar los recordatorios de prueba.'
  } finally {
    enviandoPrueba.value = false
  }
}
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comercio"
      titulo="Reglas de pedidos e hitos"
      descripcion="Límites del plan de hitos, aprobación automática y plazos de pago."
    />

    <AdminTabs />

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
                Plazo máximo para pagar (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-pago"
                v-model="diasPagoCuota"
                type="number"
                min="1"
                max="14"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Aplica a la seña, a cada cuota y a la entrega final. Al vencer, el pedido
                se cierra y el artista conserva lo cobrado. La ventana de una cuota nunca
                supera la entrega prevista del pedido.
              </p>
            </div>
            <div>
              <label for="max-revisiones" class="mb-2 block text-sm text-cafe">
                Revisiones por hito <span class="text-rojizo">*</span>
              </label>
              <input
                id="max-revisiones"
                v-model="revisionesMaximasPorHito"
                type="number"
                min="1"
                max="10"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Rondas de cambios incluidas en cada etapa. Después del tope, el cliente
                solo puede aprobar o cancelar.
              </p>
            </div>
            <div>
              <label for="dias-reentrega" class="mb-2 block text-sm text-cafe">
                Plazo para reentregar (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-reentrega"
                v-model="diasReentrega"
                type="number"
                min="1"
                max="60"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Tiempo del artista para subir la nueva versión después de una revisión.
                Si no reentrega, el pedido queda marcado como atrasado.
              </p>
            </div>
          </div>
        </div>

        <div class="border-t border-rosa/40 pt-6">
          <h2 class="mb-4 text-base text-granate">Sanciones por incumplimiento</h2>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="dias-gracia-entrega" class="mb-2 block text-sm text-cafe">
                Gracia antes de pausar ventas (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-gracia-entrega"
                v-model="diasGraciaEntrega"
                type="number"
                min="1"
                max="30"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Días extra después del plazo de entrega antes de suspender las ventas.
              </p>
            </div>
            <div>
              <label for="dias-sancion-ventas" class="mb-2 block text-sm text-cafe">
                Duración de la suspensión de ventas (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-sancion-ventas"
                v-model="diasSancionVentas"
                type="number"
                min="1"
                max="60"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Tiempo que el catálogo queda pausado. Al terminar, si sigue la mora,
                la cuenta pasa a “solo entregas”.
              </p>
            </div>
            <div>
              <label for="dias-reapelacion" class="mb-2 block text-sm text-cafe">
                Espera entre apelaciones (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-reapelacion"
                v-model="diasReapelacion"
                type="number"
                min="1"
                max="90"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Días que debe esperar el artista para volver a apelar.
              </p>
            </div>
            <div>
              <label for="dias-cierre-tras-bloqueo" class="mb-2 block text-sm text-cafe">
                Cierre tras el bloqueo (días) <span class="text-rojizo">*</span>
              </label>
              <input
                id="dias-cierre-tras-bloqueo"
                v-model="diasCierreTrasBloqueo"
                type="number"
                min="7"
                max="180"
                step="1"
                class="block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
              <p class="mt-2 text-xs leading-relaxed text-cafe/50">
                Días desde el Nivel 2 antes de cerrar los pedidos pendientes por
                incumplimiento.
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

        <div class="border-t border-rosa/40 pt-6">
          <h2 class="mb-2 text-base text-granate">Recordatorios por mail</h2>
          <p class="text-xs leading-relaxed text-cafe/50">
            Encola y envía ahora los avisos de pagos y entregas pendientes, sin
            esperar al programador.
          </p>
          <div class="mt-4 flex flex-wrap items-center gap-3">
            <button
              :disabled="procesandoMails"
              class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-coral px-5 text-sm font-semibold text-coral transition hover:bg-coral/10 disabled:opacity-50"
              @click="procesarMails"
            >
              {{ procesandoMails ? 'Procesando...' : 'Procesar mails pendientes' }}
            </button>
            <button
              :disabled="enviandoPrueba"
              class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
              @click="enviarRecordatoriosDePrueba"
            >
              {{ enviandoPrueba ? 'Enviando...' : 'Enviar recordatorios de prueba' }}
            </button>
          </div>
          <p class="mt-3 text-xs leading-relaxed text-cafe/50">
            La prueba envía dos mails separados a tu correo: uno de pagos y otro
            de entregas. No encola ni modifica pedidos.
          </p>
          <p v-if="mensajeMails" class="mt-3 text-sm text-green-700">{{ mensajeMails }}</p>
          <p v-if="errorMails" class="mt-3 text-sm text-rojizo">{{ errorMails }}</p>
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
            <dt class="text-xs text-cafe/50">Plazo máximo de pago</dt>
            <dd class="text-cafe">{{ config.diasPagoCuota }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Revisiones por hito</dt>
            <dd class="text-cafe">{{ config.revisionesMaximasPorHito }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Plazo de reentrega</dt>
            <dd class="text-cafe">{{ config.diasReentrega }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Gracia de entrega</dt>
            <dd class="text-cafe">{{ config.diasGraciaEntrega }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Suspensión de ventas</dt>
            <dd class="text-cafe">{{ config.diasSancionVentas }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Espera entre apelaciones</dt>
            <dd class="text-cafe">{{ config.diasReapelacion }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Cierre tras el bloqueo</dt>
            <dd class="text-cafe">{{ config.diasCierreTrasBloqueo }} días</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Sanciones automáticas desde</dt>
            <dd class="text-right text-cafe">
              {{ formatearFecha(config.sancionesAutomaticasDesde) }}
            </dd>
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
