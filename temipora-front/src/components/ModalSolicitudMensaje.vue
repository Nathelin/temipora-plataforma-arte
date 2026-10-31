<script setup>
import { nextTick, ref, watch } from 'vue'
import { inicialDe } from '../servicios/mensajes'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  destinatario: { type: Object, default: null },
  motivoMaximo: { type: Number, default: 200 },
  mensajeMaximo: { type: Number, default: 2000 },
  cargando: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['cerrar', 'enviar'])

const motivo = ref('')
const mensaje = ref('')
const errorMotivo = ref('')
const errorMensaje = ref('')
const campoMotivo = ref(null)

watch(
  () => props.abierto,
  async (abierto) => {
    if (!abierto) return
    motivo.value = ''
    mensaje.value = ''
    errorMotivo.value = ''
    errorMensaje.value = ''
    await nextTick()
    campoMotivo.value?.focus()
  },
)

function cerrar() {
  if (props.cargando) return
  emit('cerrar')
}

function validar() {
  errorMotivo.value = ''
  errorMensaje.value = ''
  const motivoLimpio = motivo.value.trim()
  const mensajeLimpio = mensaje.value.trim()

  if (!motivoLimpio) {
    errorMotivo.value = 'Contá brevemente por qué querés contactar a esta persona.'
  } else if (motivoLimpio.length > props.motivoMaximo) {
    errorMotivo.value = `El motivo no puede superar los ${props.motivoMaximo} caracteres.`
  }

  if (!mensajeLimpio) {
    errorMensaje.value = 'Escribí un primer mensaje para que la persona sepa de qué se trata.'
  } else if (mensajeLimpio.length > props.mensajeMaximo) {
    errorMensaje.value = `El mensaje no puede superar los ${props.mensajeMaximo} caracteres.`
  }

  if (errorMotivo.value || errorMensaje.value) return false
  return { motivoContacto: motivoLimpio, primerMensaje: mensajeLimpio }
}

function enviar() {
  if (props.cargando) return
  const datos = validar()
  if (!datos) return
  emit('enviar', datos)
}

function nombreVisible() {
  return (
    props.destinatario?.nombreDisplay ||
    props.destinatario?.nombreUsuario ||
    'esta persona'
  )
}
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-modal-solicitud-mensaje"
    @keydown.esc="cerrar"
  >
    <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="cerrar"></div>

    <div class="relative max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-3xl border border-rosa bg-blanco p-6 shadow-xl shadow-granate/20">
      <h2 id="titulo-modal-solicitud-mensaje" class="font-display text-xl font-bold text-granate">
        Enviar solicitud de mensaje
      </h2>
      <p class="mt-1.5 text-sm leading-relaxed text-cafe/65">
        La persona va a recibir tu motivo y tu primer mensaje. Solo podrán seguir
        escribiéndose cuando acepte la solicitud.
      </p>

      <div v-if="destinatario" class="mt-4 flex items-center gap-3 rounded-2xl border border-rosa/50 bg-superficie px-3.5 py-3">
        <img
          v-if="destinatario.urlFotoPerfil || destinatario.urlFotoPerfilMiniatura"
          :src="destinatario.urlFotoPerfilMiniatura || destinatario.urlFotoPerfil"
          alt=""
          class="h-10 w-10 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
        />
        <span
          v-else
          class="flex h-10 w-10 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate ring-1 ring-white/70"
          aria-hidden="true"
        >
          {{ inicialDe(destinatario.nombreUsuario) }}
        </span>
        <span class="min-w-0">
          <span class="block truncate text-sm font-semibold text-granate">
            {{ nombreVisible() }}
          </span>
          <span class="block truncate text-xs text-cafe/55">
            @{{ destinatario.nombreUsuario }}
          </span>
        </span>
      </div>

      <label for="motivo-contacto" class="mt-5 block text-sm font-medium text-cafe">
        Motivo de contacto <span class="text-rojizo" aria-hidden="true">*</span>
      </label>
      <textarea
        id="motivo-contacto"
        ref="campoMotivo"
        v-model="motivo"
        rows="2"
        :maxlength="motivoMaximo"
        :aria-invalid="Boolean(errorMotivo)"
        aria-describedby="ayuda-motivo-contacto"
        placeholder="Por ejemplo: consulta por una comisión de personaje."
        class="mt-1.5 w-full resize-none rounded-2xl border border-rosa/70 bg-blanco px-3.5 py-2.5 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
        @input="errorMotivo = ''"
      ></textarea>
      <div class="mt-1 flex items-start justify-between gap-3 text-[11px] text-cafe/50">
        <span id="ayuda-motivo-contacto">Se muestra en la bandeja del destinatario.</span>
        <span class="shrink-0">{{ motivo.length }}/{{ motivoMaximo }}</span>
      </div>
      <p v-if="errorMotivo" class="mt-1 text-xs text-rojizo" role="alert">{{ errorMotivo }}</p>

      <label for="primer-mensaje" class="mt-4 block text-sm font-medium text-cafe">
        Primer mensaje <span class="text-rojizo" aria-hidden="true">*</span>
      </label>
      <textarea
        id="primer-mensaje"
        v-model="mensaje"
        rows="5"
        :maxlength="mensajeMaximo"
        :aria-invalid="Boolean(errorMensaje)"
        aria-describedby="ayuda-primer-mensaje"
        placeholder="Contale qué necesitás, para cuándo y cualquier detalle útil."
        class="mt-1.5 w-full resize-none rounded-2xl border border-rosa/70 bg-blanco px-3.5 py-2.5 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
        @input="errorMensaje = ''"
      ></textarea>
      <div class="mt-1 flex items-start justify-between gap-3 text-[11px] text-cafe/50">
        <span id="ayuda-primer-mensaje">Forma parte del historial de la conversación.</span>
        <span class="shrink-0">{{ mensaje.length }}/{{ mensajeMaximo }}</span>
      </div>
      <p v-if="errorMensaje" class="mt-1 text-xs text-rojizo" role="alert">{{ errorMensaje }}</p>

      <p
        v-if="error"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-3.5 py-2.5 text-sm text-rojizo"
        role="alert"
      >
        {{ error }}
      </p>

      <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="min-h-10 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/20 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="cargando"
          @click="cerrar"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="min-h-10 rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="cargando"
          @click="enviar"
        >
          {{ cargando ? 'Enviando...' : 'Enviar solicitud' }}
        </button>
      </div>
    </div>
  </div>
</template>
