<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import {
  formatearFecha,
  formatearFechaHora,
  sumarDiasFecha,
  textoRestante,
} from '../servicios/pedidos'

const MAX_MOTIVO = 500

const props = defineProps({
  abierto: { type: Boolean, default: false },
  pedido: { type: Object, default: null },
  prorroga: { type: Object, default: null },
  procesando: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['aceptar', 'rechazar', 'expirada'])

const motivo = ref('')
const mostrarRechazo = ref(false)
const ahora = ref(Date.now())
const panel = ref(null)
const botonAceptar = ref(null)
const campoMotivo = ref(null)
let intervalo = null

const entregaVigente = computed(
  () => props.pedido?.fechaEntregaPrevista || props.pedido?.limiteEntrega || null,
)
const nuevaEntrega = computed(() => sumarDiasFecha(
  entregaVigente.value,
  props.prorroga?.diasSolicitados,
))
const vencida = computed(() => {
  const limite = props.prorroga?.fechaLimiteRespuesta
  return Boolean(limite) && new Date(limite).getTime() <= ahora.value
})
const restante = computed(() => textoRestante(
  props.prorroga?.fechaLimiteRespuesta,
  ahora.value,
))

function bloquearScroll(bloquear) {
  document.body.style.overflow = bloquear ? 'hidden' : ''
}

function detenerIntervalo() {
  if (intervalo) {
    clearInterval(intervalo)
    intervalo = null
  }
}

function iniciarContador() {
  ahora.value = Date.now()
  detenerIntervalo()
  intervalo = setInterval(() => {
    ahora.value = Date.now()
    if (vencida.value) {
      detenerIntervalo()
      emit('expirada')
    }
  }, 15000)
}

watch(
  () => props.abierto,
  async (abierto) => {
    if (!abierto) {
      bloquearScroll(false)
      detenerIntervalo()
      return
    }
    motivo.value = ''
    mostrarRechazo.value = false
    bloquearScroll(true)
    iniciarContador()
    await nextTick()
    botonAceptar.value?.focus()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  bloquearScroll(false)
  detenerIntervalo()
})

function aceptar() {
  if (props.procesando || vencida.value) return
  emit('aceptar')
}

async function abrirRechazo() {
  if (props.procesando || vencida.value) return
  mostrarRechazo.value = true
  await nextTick()
  campoMotivo.value?.focus()
}

function confirmarRechazo() {
  if (props.procesando || vencida.value) return
  emit('rechazar', motivo.value.trim())
}

function alPresionarTecla(evento) {
  if (evento.key !== 'Tab' || !panel.value) return
  const focusables = Array.from(panel.value.querySelectorAll(
    'button:not([disabled]), textarea:not([disabled]), input:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
  ))
  if (!focusables.length) return
  const primero = focusables[0]
  const ultimo = focusables[focusables.length - 1]
  if (evento.shiftKey && document.activeElement === primero) {
    evento.preventDefault()
    ultimo.focus()
  } else if (!evento.shiftKey && document.activeElement === ultimo) {
    evento.preventDefault()
    primero.focus()
  }
}
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[95] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-responder-prorroga"
    aria-describedby="detalle-responder-prorroga"
  >
    <div class="absolute inset-0 bg-granate/60" aria-hidden="true"></div>

    <div
      ref="panel"
      class="relative max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-2xl border border-rosa bg-blanco p-6 shadow-2xl shadow-granate/30"
      @keydown="alPresionarTecla"
    >
      <div class="flex flex-wrap items-center justify-between gap-2">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-coral">
          Decisión pendiente
        </p>
        <span
          class="rounded-full px-3 py-1 text-xs font-semibold"
          :class="vencida
            ? 'bg-rojizo/10 text-rojizo'
            : 'bg-coral/10 text-coral'"
        >
          {{ vencida ? 'Propuesta vencida' : `Respondé en ${restante}` }}
        </span>
      </div>

      <h2 id="titulo-responder-prorroga" class="mt-1 font-display text-2xl font-bold text-granate">
        El artista pidió más plazo
      </h2>
      <p id="detalle-responder-prorroga" class="mt-2 text-sm leading-relaxed text-cafe/70">
        Para el pedido de <strong>{{ pedido?.nombreServicio || 'tu pedido' }}</strong>,
        el artista pidió correr la entrega
        <strong>{{ prorroga?.diasSolicitados }} {{ prorroga?.diasSolicitados === 1 ? 'día' : 'días' }}</strong>.
        Mientras no respondas, no podés hacer otra cosa en este pedido.
      </p>

      <div class="mt-4 grid gap-2 rounded-xl bg-rosa/10 p-4 text-sm sm:grid-cols-2">
        <div>
          <p class="text-xs text-cafe/50">Entrega actual</p>
          <p class="mt-0.5 font-semibold text-cafe">
            {{ entregaVigente ? formatearFecha(entregaVigente) : 'A confirmar' }}
          </p>
        </div>
        <div>
          <p class="text-xs text-cafe/50">Nueva fecha propuesta</p>
          <p class="mt-0.5 font-semibold text-granate">
            {{ nuevaEntrega ? formatearFecha(nuevaEntrega) : '—' }}
          </p>
        </div>
        <div class="sm:col-span-2">
          <p class="text-xs text-cafe/50">Motivo del artista</p>
          <p class="mt-0.5 whitespace-pre-line text-cafe">{{ prorroga?.motivo || '—' }}</p>
        </div>
        <div v-if="prorroga?.fechaLimiteRespuesta" class="sm:col-span-2">
          <p class="text-xs text-cafe/50">Responder antes de</p>
          <p class="mt-0.5 text-cafe">
            {{ formatearFechaHora(prorroga.fechaLimiteRespuesta) }}
          </p>
        </div>
      </div>

      <p
        v-if="vencida"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        role="alert"
      >
        La propuesta venció sin respuesta. Se mantiene el plazo original.
      </p>

      <p
        v-if="error"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        role="alert"
      >
        {{ error }}
      </p>

      <div v-if="mostrarRechazo" class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 p-4">
        <label for="motivo-rechazo-prorroga" class="block text-sm font-medium text-cafe">
          Motivo del rechazo <span class="font-normal text-cafe/50">(opcional)</span>
        </label>
        <textarea
          id="motivo-rechazo-prorroga"
          ref="campoMotivo"
          v-model="motivo"
          rows="3"
          :maxlength="MAX_MOTIVO"
          placeholder="Contale al artista por qué necesitás la fecha original..."
          class="mt-2 w-full resize-none rounded-lg border border-rosa/70 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
        ></textarea>
        <p class="mt-1 text-right text-[11px] text-cafe/50">
          {{ motivo.length }}/{{ MAX_MOTIVO }}
        </p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-full bg-rojizo px-4 py-2 text-sm font-semibold text-white transition hover:bg-granate disabled:opacity-60"
            :disabled="procesando || vencida"
            @click="confirmarRechazo"
          >
            {{ procesando ? 'Rechazando...' : 'Confirmar rechazo' }}
          </button>
          <button
            type="button"
            class="rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-cafe transition hover:bg-rosa/20"
            :disabled="procesando"
            @click="mostrarRechazo = false"
          >
            Volver
          </button>
        </div>
      </div>

      <p class="mt-4 text-xs leading-relaxed text-cafe/50">
        Si no respondés dentro de la ventana, la propuesta vence y se mantienen los plazos
        originales del pedido.
      </p>

      <div v-if="!mostrarRechazo" class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="rounded-full border border-rojizo/40 px-4 py-2.5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="procesando || vencida"
          @click="abrirRechazo"
        >
          Mantener fecha original
        </button>
        <button
          ref="botonAceptar"
          type="button"
          class="rounded-full bg-coral px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="procesando || vencida"
          @click="aceptar"
        >
          {{ procesando ? 'Guardando...' : 'Aceptar nueva fecha' }}
        </button>
      </div>
    </div>
  </div>
</template>
