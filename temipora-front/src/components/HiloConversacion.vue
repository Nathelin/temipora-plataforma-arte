<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { formatearFechaMensaje } from '../servicios/mensajes'

const props = defineProps({
  mensajes: { type: Array, default: () => [] },
  texto: { type: String, default: '' },
  maxMensaje: { type: Number, default: 2000 },
  enviando: { type: Boolean, default: false },
  errorEnvio: { type: String, default: '' },
  hayMas: { type: Boolean, default: false },
  cargandoMas: { type: Boolean, default: false },
  contraparteEscribiendo: { type: Boolean, default: false },
  nombreContraparte: { type: String, default: 'Usuario' },
  puedeEnviar: { type: Boolean, default: true },
  motivoNoEnviable: { type: String, default: '' },
  altura: { type: String, default: '58vh' },
  cargarAnteriores: { type: Function, default: null },
})

const emit = defineEmits(['update:texto', 'enviar', 'escribiendo'])

const sufijo = Math.random().toString(36).slice(2, 9)
const idCampo = `cuerpo-mensaje-${sufijo}`
const idContador = `contador-mensaje-${sufijo}`

const lista = ref(null)
const campo = ref(null)
const cercaDelFinal = ref(true)

const textoModel = computed({
  get: () => props.texto,
  set: (valor) => emit('update:texto', valor),
})

const alturaEstilo = computed(() => ({ maxHeight: props.altura }))

const enviarDeshabilitado = computed(
  () => props.enviando || !props.puedeEnviar || !props.texto.trim(),
)

const ultimoMensajePropioId = computed(() => {
  for (let i = props.mensajes.length - 1; i >= 0; i--) {
    if (props.mensajes[i].esPropio) return props.mensajes[i].idMensaje
  }
  return null
})

function mostrarHora(indice) {
  const actual = props.mensajes[indice]
  const siguiente = props.mensajes[indice + 1]
  if (!actual) return false
  if (!siguiente) return true
  if (siguiente.esPropio !== actual.esPropio) return true
  return formatearFechaMensaje(actual.fechaEnvio)
    !== formatearFechaMensaje(siguiente.fechaEnvio)
}

function esUltimoPropio(mensaje) {
  return mensaje.esPropio && mensaje.idMensaje === ultimoMensajePropioId.value
}

function estaCercaDelFinal() {
  const el = lista.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 140
}

function alScrollear() {
  cercaDelFinal.value = estaCercaDelFinal()
}

function scrollAlFinal() {
  const el = lista.value
  if (el) el.scrollTop = el.scrollHeight
}

function alEscribir() {
  emit('escribiendo')
}

function manejarEnter(event) {
  if (event.shiftKey) return
  event.preventDefault()
  if (enviarDeshabilitado.value) return
  emit('enviar')
}

async function cargarAnteriores() {
  if (!props.cargarAnteriores) return
  const el = lista.value
  const alturaAnterior = el?.scrollHeight || 0
  await props.cargarAnteriores()
  await nextTick()
  if (el) el.scrollTop = el.scrollHeight - alturaAnterior
}

function enfocar() {
  campo.value?.focus()
}

watch(
  () => props.mensajes.length,
  async (nuevo, anterior) => {
    if (nuevo > anterior && cercaDelFinal.value) {
      await nextTick()
      scrollAlFinal()
    }
  },
)

watch(
  () => props.enviando,
  (valor) => {
    if (!valor) campo.value?.focus()
  },
)

defineExpose({ enfocar })
</script>

<template>
  <div
    ref="lista"
    class="space-y-3 overflow-y-auto rounded-3xl border border-rosa/50 bg-superficie px-4 py-5 sm:px-6"
    :style="alturaEstilo"
    aria-live="polite"
    @scroll="alScrollear"
  >
    <div v-if="hayMas" class="pb-1 text-center">
      <button
        type="button"
        class="min-h-9 rounded-full border border-rosa bg-blanco px-4 text-xs font-semibold text-granate transition hover:bg-rosa/20 disabled:cursor-wait disabled:opacity-60"
        :disabled="cargandoMas"
        @click="cargarAnteriores"
      >
        {{ cargandoMas ? 'Cargando...' : 'Cargar mensajes anteriores' }}
      </button>
    </div>

    <p v-if="!mensajes.length" class="py-10 text-center text-sm text-cafe/55">
      Todavía no hay mensajes. Escribí el primero.
    </p>

    <div
      v-for="(mensaje, indice) in mensajes"
      :key="mensaje.idMensaje"
      class="flex"
      :class="mensaje.esPropio ? 'justify-end' : 'justify-start'"
    >
      <div class="max-w-[85%] sm:max-w-[75%]">
        <div
          class="whitespace-pre-line rounded-2xl px-4 py-2.5 text-sm leading-relaxed shadow-sm"
          :class="mensaje.esPropio
            ? 'rounded-br-md bg-coral text-white'
            : 'rounded-bl-md border border-rosa/50 bg-blanco text-cafe'"
        >
          {{ mensaje.contenido }}
        </div>
        <p
          v-if="mostrarHora(indice)"
          class="mt-1 text-[10px] text-cafe/45"
          :class="mensaje.esPropio ? 'text-right' : 'text-left'"
        >
          {{ formatearFechaMensaje(mensaje.fechaEnvio) }}
          <span
            v-if="esUltimoPropio(mensaje) && mensaje.leido"
            class="font-semibold text-coral"
          >
            · Leído
          </span>
        </p>
      </div>
    </div>
  </div>

  <div class="mt-3 min-h-5 px-1 text-xs text-cafe/55" role="status" aria-live="polite">
    <span v-if="contraparteEscribiendo" class="inline-flex items-center gap-1.5">
      <span class="inline-flex items-center gap-0.5" aria-hidden="true">
        <span class="h-1 w-1 animate-bounce rounded-full bg-coral motion-reduce:animate-none" style="animation-delay: -0.3s"></span>
        <span class="h-1 w-1 animate-bounce rounded-full bg-coral motion-reduce:animate-none" style="animation-delay: -0.15s"></span>
        <span class="h-1 w-1 animate-bounce rounded-full bg-coral motion-reduce:animate-none"></span>
      </span>
      {{ nombreContraparte }} está escribiendo…
    </span>
  </div>

  <form
    class="mt-4 rounded-3xl border border-rosa/50 bg-blanco p-3 sm:p-4"
    @submit.prevent="emit('enviar')"
  >
    <label :for="idCampo" class="sr-only">Escribí un mensaje</label>
    <textarea
      :id="idCampo"
      ref="campo"
      v-model="textoModel"
      rows="3"
      :maxlength="maxMensaje"
      :disabled="!puedeEnviar || enviando"
      :aria-describedby="idContador"
      placeholder="Escribí un mensaje..."
      class="w-full resize-none rounded-2xl border border-rosa/70 bg-blanco px-3.5 py-2.5 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20 disabled:cursor-not-allowed disabled:opacity-60"
      @input="alEscribir"
      @keydown.enter.exact="manejarEnter"
    ></textarea>

    <div class="mt-2 flex flex-wrap items-center justify-between gap-3">
      <p :id="idContador" class="text-[11px] text-cafe/50">
        {{ texto.length }}/{{ maxMensaje }} · Enter envía, Shift+Enter hace un salto de línea.
      </p>
      <button
        type="submit"
        class="inline-flex min-h-10 items-center gap-2 rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="enviarDeshabilitado"
      >
        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 19.5 15-7.5-15-7.5 0 6 9 1.5-9 1.5v6Z" />
        </svg>
        {{ enviando ? 'Enviando...' : 'Enviar' }}
      </button>
    </div>

    <p
      v-if="errorEnvio"
      class="mt-2 rounded-xl border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-xs text-rojizo"
      role="alert"
    >
      {{ errorEnvio }}
    </p>
    <p v-else-if="motivoNoEnviable" class="mt-2 text-xs text-cafe/55">
      {{ motivoNoEnviable }}
    </p>
  </form>
</template>
