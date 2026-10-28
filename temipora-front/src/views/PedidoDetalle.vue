<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  descripcionHito,
  estadoPedidoDe,
  etiquetaEntrega,
  etiquetaPago,
  etiquetaTipoHito,
  formatearFecha,
  formatearFechaHora,
  pagoPendiente,
} from '../servicios/pedidos'

const route = useRoute()
const router = useRouter()
const idPedido = computed(() => Number(route.params.idPedido))

const pedido = ref(null)
const cargando = ref(true)
const error = ref('')
const errorAccion = ref('')
const aviso = ref('')
const procesando = ref(false)
const subiendo = ref(false)
const revisionHitoId = ref(null)
const motivoRevision = ref('')
const archivoEntrega = ref('')
const entrega = reactive({ descripcion: '', imagen: '' })
const visor = reactive({ abierto: false, imagen: '' })

const esArtista = computed(() => pedido.value?.rolVista === 'ARTISTA')
const esCliente = computed(() => pedido.value?.rolVista === 'CLIENTE')
const estadoInfo = computed(() => estadoPedidoDe(pedido.value?.estado))
const hitos = computed(() => pedido.value?.hitos || [])
const moneda = computed(
  () => pedido.value?.monedaMostrada || pedido.value?.moneda || 'ARS',
)
const monedaCobro = computed(() => pedido.value?.monedaCobro || 'ARS')
const total = computed(() => formatearPrecio(
  pedido.value?.precioFinalMostrado ?? pedido.value?.precioFinal,
  moneda.value,
))
const cobroDistinto = computed(
  () => pedido.value?.precioFinalCobro != null
    && moneda.value !== monedaCobro.value,
)

function formatearMontoHito(hito) {
  return formatearPrecio(
    hito.montoMostrado ?? hito.monto,
    moneda.value,
  )
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/pedidos/${idPedido.value}`)
    pedido.value = res.data
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value = e.response?.status === 403 || e.response?.status === 404
      ? 'No podés ver este pedido o ya no existe.'
      : (e.response?.data?.message || 'No se pudo cargar el pedido.')
  } finally {
    cargando.value = false
  }
}

async function ejecutar(accion, mensaje) {
  if (procesando.value) return false
  procesando.value = true
  errorAccion.value = ''
  aviso.value = ''
  try {
    const res = await accion()
    if (res?.data) pedido.value = res.data
    aviso.value = mensaje
    return true
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return false
    }
    errorAccion.value = e.response?.data?.message
      || 'No se pudo completar la acción.'
    return false
  } finally {
    procesando.value = false
  }
}

function pagarHito(hito) {
  ejecutar(
    () => api.post(`/pedidos/${idPedido.value}/hitos/${hito.idHitoPedido}/pagar`),
    'Pago iniciado. Confirmalo para activar el hito.',
  )
}

function confirmarPago(pago) {
  ejecutar(
    () => api.post(`/pagos/simulado/${pago.idPago}/confirmar`),
    'Pago confirmado.',
  )
}

function aprobar(hito) {
  ejecutar(
    () => api.post(`/pedidos/${idPedido.value}/hitos/${hito.idHitoPedido}/aprobar`),
    'Aprobaste el entregable.',
  )
}

function abrirRevision(hito) {
  revisionHitoId.value = hito.idHitoPedido
  motivoRevision.value = ''
}

function cerrarRevision() {
  revisionHitoId.value = null
  motivoRevision.value = ''
}

async function solicitarRevision(hito) {
  if (!motivoRevision.value.trim()) {
    errorAccion.value = 'Escribí el motivo de la revisión.'
    return
  }
  const ok = await ejecutar(
    () => api.post(
      `/pedidos/${idPedido.value}/hitos/${hito.idHitoPedido}/solicitar-revision`,
      { motivo: motivoRevision.value.trim() },
    ),
    'Pediste una revisión al artista.',
  )
  if (ok) cerrarRevision()
}

function cancelar() {
  if (!window.confirm(
    '¿Cancelar el pedido? Los pagos ya realizados no se devuelven automáticamente.',
  )) return
  ejecutar(
    () => api.post(`/pedidos/${idPedido.value}/cancelar`),
    'El pedido fue cancelado.',
  )
}

async function subirImagen(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!String(file.type).startsWith('image/')) {
    errorAccion.value = 'El archivo debe ser una imagen.'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    errorAccion.value = 'La imagen no puede superar los 5 MB.'
    return
  }
  subiendo.value = true
  errorAccion.value = ''
  try {
    const formData = new FormData()
    formData.append('archivo', file)
    const res = await api.post('/upload', formData)
    entrega.imagen = res.data?.url || ''
    archivoEntrega.value = file.name
  } catch (e) {
    errorAccion.value = e.response?.data?.message || 'No se pudo subir la imagen.'
  } finally {
    subiendo.value = false
  }
}

async function entregar(hito) {
  if (!entrega.imagen) {
    errorAccion.value = 'Subí una imagen del entregable.'
    return
  }
  const ok = await ejecutar(
    () => api.post(
      `/pedidos/${idPedido.value}/hitos/${hito.idHitoPedido}/entregar`,
      {
        descripcion: entrega.descripcion?.trim() || null,
        imagen: entrega.imagen,
      },
    ),
    'Entregable subido. El cliente fue notificado.',
  )
  if (ok) {
    entrega.descripcion = ''
    entrega.imagen = ''
    archivoEntrega.value = ''
  }
}

function abrirVisor(imagen) {
  visor.imagen = imagen
  visor.abierto = true
}

function cerrarVisor() {
  visor.abierto = false
}

function volver() {
  const enPanel = route.path.startsWith('/panel')
  router.push({ name: enPanel ? 'panel-pedidos' : 'pedidos' })
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a pedidos
    </button>

    <p v-if="cargando" class="mt-8 py-16 text-center text-sm text-cafe/60">
      Cargando pedido...
    </p>
    <p v-else-if="error" class="mt-8 py-16 text-center text-sm text-rojizo">
      {{ error }}
    </p>

    <template v-else>
      <header class="mt-6 flex flex-wrap items-start justify-between gap-4">
        <div class="min-w-0">
          <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">
            Pedido #{{ pedido.idPedido }}
          </p>
          <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
            {{ pedido.nombreServicio || 'Servicio' }}
          </h1>
          <p class="mt-2 text-sm text-cafe/65">
            {{ esArtista ? 'Cliente' : 'Artista' }}:
            @{{ (esArtista ? pedido.cliente : pedido.artista)?.nombreUsuario || '—' }}
          </p>
        </div>
        <span
          class="rounded-full border px-3 py-1.5 text-xs font-semibold"
          :class="estadoInfo.clase"
        >
          {{ estadoInfo.etiqueta }}
        </span>
      </header>

      <section class="mt-6 rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
        <dl class="grid gap-3 text-sm sm:grid-cols-3">
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Total</dt>
            <dd class="mt-0.5 text-xl font-bold text-granate">{{ total }}</dd>
            <dd v-if="cobroDistinto" class="text-xs text-cafe/50">
              A pagar: {{ formatearPrecio(pedido.precioFinalCobro, monedaCobro) }}
            </dd>
          </div>
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Inicio</dt>
            <dd class="mt-0.5 font-medium text-cafe">
              {{ pedido.fechaInicio ? formatearFecha(pedido.fechaInicio) : 'Al confirmar el pago' }}
            </dd>
          </div>
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Límite de entrega</dt>
            <dd class="mt-0.5 font-medium text-cafe">
              {{ pedido.limiteEntrega ? formatearFecha(pedido.limiteEntrega) : 'A confirmar' }}
            </dd>
          </div>
        </dl>

        <div
          v-if="esArtista && pedido.comisionMonto != null"
          class="mt-4 rounded-xl border border-cafe/15 bg-cafe/5 p-4"
        >
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
            Comisión de la plataforma
          </p>
          <dl class="mt-2 space-y-1.5 text-sm">
            <div class="flex items-center justify-between gap-3">
              <dt class="text-cafe/60">
                Comisión ({{ pedido.comisionPorcentaje }}%)
              </dt>
              <dd class="text-cafe">
                − {{ formatearPrecio(pedido.comisionMontoMostrado ?? pedido.comisionMonto, moneda) }}
              </dd>
            </div>
            <div class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5">
              <dt class="font-semibold text-granate">Recibís</dt>
              <dd class="text-base font-bold text-granate">
                {{ formatearPrecio(pedido.montoNetoArtistaMostrado ?? pedido.montoNetoArtista, moneda) }}
              </dd>
            </div>
          </dl>
        </div>
      </section>

      <p
        v-if="errorAccion"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        role="alert"
      >
        {{ errorAccion }}
      </p>
      <p
        v-else-if="aviso"
        class="mt-4 rounded-xl border border-green-700/25 bg-green-700/5 px-4 py-3 text-sm text-green-700"
        role="status"
      >
        {{ aviso }}
      </p>

      <section class="mt-6 space-y-4">
        <h2 class="text-xl font-semibold text-granate">Plan de pagos</h2>

        <article
          v-for="hito in hitos"
          :key="hito.idHitoPedido"
          class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                {{ etiquetaTipoHito(hito.tipo) }}
              </p>
              <h3 class="mt-1 text-lg font-semibold text-granate">{{ hito.nombre }}</h3>
              <p class="mt-1 text-xs text-cafe/55">{{ descripcionHito(hito) }}</p>
            </div>
            <div class="text-right">
              <p class="text-lg font-bold text-granate">{{ formatearMontoHito(hito) }}</p>
              <div class="mt-1 flex flex-wrap justify-end gap-1.5">
                <span class="rounded-full bg-rosa/20 px-2.5 py-1 text-[11px] font-semibold text-cafe/70">
                  Pago: {{ etiquetaPago(hito.estadoPago) }}
                </span>
                <span class="rounded-full bg-coral/10 px-2.5 py-1 text-[11px] font-semibold text-coral">
                  Entrega: {{ etiquetaEntrega(hito.estadoEntrega) }}
                </span>
              </div>
            </div>
          </div>

          <dl class="mt-4 grid gap-2 text-xs sm:grid-cols-3">
            <div v-if="hito.fechaLimitePago">
              <dt class="text-cafe/50">Pagar antes de</dt>
              <dd class="mt-0.5 text-cafe">{{ formatearFechaHora(hito.fechaLimitePago) }}</dd>
            </div>
            <div v-if="hito.fechaPago">
              <dt class="text-cafe/50">Pagado</dt>
              <dd class="mt-0.5 text-cafe">{{ formatearFechaHora(hito.fechaPago) }}</dd>
            </div>
            <div v-if="hito.fechaLimiteAprobacion">
              <dt class="text-cafe/50">Aprobación automática</dt>
              <dd class="mt-0.5 text-cafe">{{ formatearFechaHora(hito.fechaLimiteAprobacion) }}</dd>
            </div>
          </dl>

          <div
            v-if="hito.entregaDescripcion || hito.entregaImagen"
            class="mt-4 rounded-xl border border-rosa/40 bg-rosa/5 p-4"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
              Entregable
            </p>
            <p
              v-if="hito.entregaDescripcion"
              class="mt-2 whitespace-pre-line text-sm leading-relaxed text-cafe/75"
            >
              {{ hito.entregaDescripcion }}
            </p>
            <button
              v-if="hito.entregaImagen"
              type="button"
              class="mt-3 block overflow-hidden rounded-xl border border-rosa/50"
              @click="abrirVisor(hito.entregaImagen)"
            >
              <img
                :src="hito.entregaImagen"
                alt="Entregable del hito"
                class="max-h-72 w-full object-contain"
              />
            </button>
          </div>
          <p
            v-else-if="hito.tipo === 'ENTREGA' && hito.estadoEntrega === 'ENTREGADO'"
            class="mt-4 rounded-xl border border-coral/25 bg-coral/5 px-4 py-3 text-sm text-cafe/70"
          >
            El artista subió la entrega final. Pagá esta cuota para desbloquear el archivo.
          </p>

          <div
            v-if="esArtista && hito.comisionMonto != null"
            class="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-lg bg-cafe/5 px-3 py-2 text-xs text-cafe/65"
          >
            <span>Comisión: − {{ formatearPrecio(hito.comisionMontoMostrado ?? hito.comisionMonto, moneda) }}</span>
            <span class="font-semibold text-granate">
              Recibís: {{ formatearPrecio(hito.montoNetoMostrado ?? hito.montoNeto, moneda) }}
            </span>
          </div>

          <div
            v-if="pagoPendiente(hito)"
            class="mt-4 rounded-xl border border-coral/30 bg-coral/5 px-4 py-3"
          >
            <p class="text-sm text-cafe/75">
              {{ pagoPendiente(hito).instruccion || 'Pago pendiente de confirmación.' }}
            </p>
            <button
              v-if="esCliente && pagoPendiente(hito).proveedor === 'SIMULADO'"
              type="button"
              :disabled="procesando"
              class="mt-3 inline-flex min-h-9 items-center justify-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="confirmarPago(pagoPendiente(hito))"
            >
              Confirmar pago (simulado)
            </button>
          </div>

          <div class="mt-4 flex flex-wrap gap-2">
            <button
              v-if="hito.puedePagar"
              type="button"
              :disabled="procesando"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="pagarHito(hito)"
            >
              {{
                hito.tipo === 'ENTREGA'
                  ? 'Pagar y desbloquear entrega'
                  : `Pagar ${hito.nombre}`
              }}
            </button>
          </div>

          <div v-if="hito.puedeAprobar" class="mt-4 flex flex-wrap gap-2">
            <button
              type="button"
              :disabled="procesando"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="aprobar(hito)"
            >
              Aprobar entregable
            </button>
            <button
              type="button"
              class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10"
              @click="abrirRevision(hito)"
            >
              Solicitar revisión
            </button>
          </div>

          <div
            v-if="revisionHitoId === hito.idHitoPedido"
            class="mt-3 rounded-xl border border-rojizo/25 bg-rojizo/5 p-3"
          >
            <label :for="`motivo-revision-${hito.idHitoPedido}`" class="block text-xs text-cafe/60">
              ¿Qué querés que revise el artista?
            </label>
            <textarea
              :id="`motivo-revision-${hito.idHitoPedido}`"
              v-model="motivoRevision"
              rows="3"
              maxlength="500"
              class="mt-2 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            ></textarea>
            <div class="mt-2 flex flex-wrap gap-2">
              <button
                type="button"
                :disabled="procesando"
                class="inline-flex min-h-9 items-center justify-center rounded-full bg-rojizo px-4 text-sm font-semibold text-white transition hover:bg-granate disabled:opacity-50"
                @click="solicitarRevision(hito)"
              >
                Enviar revisión
              </button>
              <button
                type="button"
                class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
                @click="cerrarRevision"
              >
                Cancelar
              </button>
            </div>
          </div>

          <div
            v-if="esArtista && hito.puedeEntregar"
            class="mt-4 rounded-xl border border-rosa/50 bg-rosa/5 p-4"
          >
            <h4 class="text-sm font-semibold text-granate">
              {{ hito.tipo === 'ENTREGA' ? 'Subir entrega final' : 'Subir entregable' }}
            </h4>
            <p
              v-if="hito.tipo === 'ENTREGA'"
              class="mt-1 text-xs leading-relaxed text-cafe/55"
            >
              El cliente verá la entrega recién cuando pague la última cuota.
            </p>
            <label :for="`archivo-${hito.idHitoPedido}`" class="mt-3 block text-xs text-cafe/60">
              Imagen <span class="text-rojizo">*</span>
            </label>
            <input
              :id="`archivo-${hito.idHitoPedido}`"
              type="file"
              accept="image/*"
              class="mt-1 block w-full text-sm text-cafe file:mr-3 file:rounded-full file:border-0 file:bg-coral/10 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-coral hover:file:bg-coral/20"
              @change="subirImagen"
            />
            <p v-if="subiendo" class="mt-1 text-xs text-cafe/50">Subiendo imagen...</p>
            <p v-else-if="archivoEntrega" class="mt-1 text-xs text-green-700">
              {{ archivoEntrega }} subida correctamente.
            </p>
            <img
              v-if="entrega.imagen"
              :src="entrega.imagen"
              alt="Previsualización del entregable"
              class="mt-3 max-h-48 rounded-xl border border-rosa/50 object-contain"
            />
            <label :for="`descripcion-${hito.idHitoPedido}`" class="mt-3 block text-xs text-cafe/60">
              Descripción <span class="text-cafe/40">(opcional)</span>
            </label>
            <textarea
              :id="`descripcion-${hito.idHitoPedido}`"
              v-model="entrega.descripcion"
              rows="2"
              maxlength="500"
              class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            ></textarea>
            <button
              type="button"
              :disabled="procesando || subiendo || !entrega.imagen"
              class="mt-3 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
              @click="entregar(hito)"
            >
              {{ procesando ? 'Enviando...' : 'Entregar' }}
            </button>
          </div>
        </article>
      </section>

      <div v-if="pedido.puedeCancelar" class="mt-6">
        <button
          type="button"
          :disabled="procesando"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:opacity-50"
          @click="cancelar"
        >
          Cancelar pedido
        </button>
      </div>
    </template>

    <div
      v-if="visor.abierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/80 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label="Entregable"
      @click.self="cerrarVisor"
    >
      <div class="relative max-h-full max-w-4xl overflow-auto rounded-2xl bg-blanco p-3">
        <button
          type="button"
          class="absolute right-3 top-3 rounded-full bg-blanco/90 px-3 py-1 text-sm font-semibold text-granate shadow"
          @click="cerrarVisor"
        >
          Cerrar
        </button>
        <img :src="visor.imagen" alt="Entregable ampliado" class="max-h-[80vh] w-auto" />
      </div>
    </div>
  </div>
</template>
