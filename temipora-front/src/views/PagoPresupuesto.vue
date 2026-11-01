<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearMonto, formatearPrecio } from '../servicios/moneda'
import { descripcionHito } from '../servicios/pedidos'

const route = useRoute()
const router = useRouter()

const idSolicitud = computed(() => Number(route.params.idSolicitud))
const idActual = Number(localStorage.getItem('idUsuario'))

const solicitud = ref(null)
const pedido = ref(null)
const cargando = ref(true)
const enviando = ref(false)
const confirmando = ref(false)
const error = ref('')
const errorAccion = ref('')

const presupuesto = computed(() => solicitud.value?.presupuesto)
const hitos = computed(() => presupuesto.value?.hitos || [])
const moneda = computed(
  () => presupuesto.value?.monedaMostrada || presupuesto.value?.moneda || 'ARS',
)
const total = computed(() => formatearPrecio(
  presupuesto.value?.precioFinalClienteMostrado
    ?? presupuesto.value?.precioFinalMostrado
    ?? presupuesto.value?.precioFinal,
  moneda.value,
))
const descuentoPorcentaje = computed(() => {
  const valor = Number(presupuesto.value?.descuentoPorcentaje)
  return Number.isFinite(valor) ? valor : 0
})
const descuentoMostrado = computed(
  () => presupuesto.value?.descuentoMostrado
    ?? presupuesto.value?.descuentoMonto
    ?? 0,
)
const esArtista = computed(
  () => solicitud.value?.artista?.idUsuario === idActual,
)
const puedePagar = computed(() => solicitud.value?.estado === 'PRESUPUESTADA'
  && !esArtista.value
  && Boolean(presupuesto.value))
const pago = computed(() => {
  const hito = pedido.value?.hitos?.[0]
  return (hito?.pagos || []).find((item) => item.estado === 'PENDIENTE') || null
})
const primerHito = computed(() => hitos.value[0] || null)
const pagaAlInicio = computed(
  () => primerHito.value?.tipo !== 'ENTREGA',
)
const esPagoUnico = computed(() => hitos.value.length === 1)
const enCursoSinPago = computed(
  () => pedido.value?.estado === 'EN_CURSO' && !pago.value,
)
const textoBotonPago = computed(() => {
  if (!pagaAlInicio.value) return 'Aceptar y comenzar el pedido'
  return esPagoUnico.value ? 'Pagar el total' : 'Pagar primera cuota'
})

function formatear(valor, codigo) {
  return formatearPrecio(valor, codigo || moneda.value)
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const resSolicitud = await api.get(
      `/solicitudes-servicio/${idSolicitud.value}`,
    )
    solicitud.value = resSolicitud.data
    if (resSolicitud.data?.idPedido) {
      router.replace({
        name: 'pedido-detalle',
        params: { idPedido: resSolicitud.data.idPedido },
      })
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value = e.response?.data?.message || 'No se pudo cargar el presupuesto.'
  } finally {
    cargando.value = false
  }
}

async function pagar() {
  if (!puedePagar.value || enviando.value) return
  enviando.value = true
  errorAccion.value = ''
  try {
    const res = await api.post(
      `/solicitudes-servicio/${idSolicitud.value}/pagar`,
    )
    pedido.value = res.data
    if (pago.value?.proveedor === 'MERCADOPAGO' && pago.value.instruccion) {
      window.location.href = pago.value.instruccion
    }
  } catch (e) {
    errorAccion.value = e.response?.data?.message || 'No se pudo iniciar el pago.'
  } finally {
    enviando.value = false
  }
}

async function confirmar() {
  if (!pago.value || confirmando.value) return
  confirmando.value = true
  errorAccion.value = ''
  try {
    const res = await api.post(`/pagos/simulado/${pago.value.idPago}/confirmar`)
    router.push({
      name: 'pedido-detalle',
      params: { idPedido: res.data.idPedido },
    })
  } catch (e) {
    errorAccion.value = e.response?.data?.message || 'No se pudo confirmar el pago.'
  } finally {
    confirmando.value = false
  }
}

function volver() {
  router.push({
    name: 'solicitud-detalle',
    params: { idSolicitud: idSolicitud.value },
  })
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-3xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a la solicitud
    </button>

    <p v-if="cargando" class="mt-8 py-16 text-center text-sm text-cafe/60">
      Cargando presupuesto...
    </p>
    <p v-else-if="error" class="mt-8 py-16 text-center text-sm text-rojizo">
      {{ error }}
    </p>

    <template v-else>
      <header class="mt-6">
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">
          Pagar presupuesto
        </p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          {{ solicitud.nombreServicio || 'Servicio' }}
        </h1>
        <p class="mt-2 text-sm text-cafe/65">
          Artista: {{ solicitud.artista?.nombreUsuario || '—' }}
        </p>
      </header>

      <section class="mt-6 rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
              Total del presupuesto
            </p>
            <p class="mt-1 text-3xl font-bold text-granate">{{ total }}</p>
            <p
              v-if="Number(descuentoMostrado) > 0"
              class="mt-2 inline-flex items-center gap-2 rounded-full bg-mantequilla px-3 py-1 text-xs font-bold text-tinta"
            >
              Descuento de reactivación ({{ descuentoPorcentaje }}%):
              − {{ formatearPrecio(descuentoMostrado, moneda) }}
            </p>
          </div>
        </div>

        <div class="mt-5">
          <h2 class="text-sm font-semibold text-granate">Plan de pagos</h2>
          <ol class="mt-3 space-y-2">
            <li
              v-for="hito in hitos"
              :key="hito.idHitoPresupuesto || hito.orden"
              class="flex items-start justify-between gap-3 rounded-xl bg-rosa/10 px-3 py-2"
            >
              <div class="min-w-0">
                <p class="text-sm font-medium text-cafe">
                  {{ hito.nombre }}
                  <span class="text-xs text-cafe/45">
                    ({{ formatearMonto(hito.porcentaje) }}%)
                  </span>
                </p>
                <p class="text-xs text-cafe/50">{{ descripcionHito(hito) }}</p>
              </div>
              <p class="shrink-0 text-sm font-semibold text-granate">
                {{ formatear(hito.montoMostrado ?? hito.monto) }}
              </p>
            </li>
          </ol>
        </div>
      </section>

      <p
        v-if="errorAccion"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        role="alert"
      >
        {{ errorAccion }}
      </p>

      <div v-if="!pedido" class="mt-5">
        <p v-if="esArtista" class="text-sm text-cafe/60">
          No podés pagar tu propio presupuesto.
        </p>
        <p v-else-if="!puedePagar" class="text-sm text-cafe/60">
          Este presupuesto no está disponible para pagar. Si venció, pedile al artista uno nuevo.
        </p>
        <button
          v-else
          type="button"
          :disabled="enviando"
          class="inline-flex min-h-11 items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
          @click="pagar"
        >
          {{ enviando ? 'Iniciando...' : textoBotonPago }}
        </button>
      </div>

      <section
        v-else-if="enCursoSinPago"
        class="mt-5 rounded-2xl border border-coral/30 bg-coral/5 p-5 sm:p-6"
      >
        <h2 class="text-lg font-semibold text-granate">Pedido en curso</h2>
        <p class="mt-1 text-sm leading-relaxed text-cafe/70">
          El pedido ya está activo. El artista va a subir la entrega; cuando la
          suba, vas a poder pagar y descargar el original.
        </p>
        <router-link
          :to="{ name: 'pedido-detalle', params: { idPedido: pedido.idPedido } }"
          class="mt-4 inline-flex min-h-11 items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo"
        >
          Ir al pedido
        </router-link>
      </section>

      <section
        v-else
        class="mt-5 rounded-2xl border border-coral/30 bg-coral/5 p-5 sm:p-6"
      >
        <h2 class="text-lg font-semibold text-granate">Pago pendiente</h2>
        <p class="mt-1 text-sm text-cafe/70">
          <template v-if="pago?.proveedor === 'MERCADOPAGO'">
            Completá el pago en MercadoPago. Al volver, el pedido se activa
            automáticamente cuando se confirma el pago.
          </template>
          <template v-else>
            {{ pago?.instruccion || 'El pago quedó pendiente de confirmación.' }}
          </template>
        </p>
        <p class="mt-2 text-xs text-cafe/50">
          El pedido se activa cuando el pago se confirma.
        </p>
        <button
          v-if="pago && pago.proveedor === 'SIMULADO'"
          type="button"
          :disabled="confirmando"
          class="mt-4 inline-flex min-h-11 items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
          @click="confirmar"
        >
          {{ confirmando ? 'Confirmando...' : 'Confirmar pago (simulado)' }}
        </button>
        <a
          v-else-if="pago?.proveedor === 'MERCADOPAGO' && pago.instruccion"
          :href="pago.instruccion"
          class="mt-4 inline-flex min-h-11 items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo"
        >
          Ir a pagar con MercadoPago
        </a>
      </section>
    </template>
  </div>
</template>
