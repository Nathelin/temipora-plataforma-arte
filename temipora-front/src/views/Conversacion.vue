<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatearFechaHora, inicialDe } from '../servicios/mensajes'
import { useHiloConversacion } from '../composables/useHiloConversacion'
import { useNotificaciones } from '../composables/useNotificaciones'
import HiloConversacion from '../components/HiloConversacion.vue'
import ModalRechazoConversacion from '../components/ModalRechazoConversacion.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'

const route = useRoute()
const router = useRouter()
const { exito } = useNotificaciones()

const idConversacion = computed(() => Number(route.params.idConversacion))

const {
  conversacion,
  mensajes,
  hayMas,
  cargando,
  cargandoMas,
  error,
  errorEnvio,
  texto,
  enviando,
  accionEnCurso,
  errorAccion,
  contraparteEscribiendo,
  contraparte,
  estadoInfo,
  maxMensaje,
  maxMotivo,
  esPendiente,
  esAceptada,
  esRechazada,
  esCancelada,
  esVencida,
  puedoResponderSolicitud,
  primerMensaje,
  nombreContraparte,
  inicializar,
  cargarAnteriores,
  enviar,
  avisarEscribiendo,
  aceptar,
  rechazar,
  cancelar,
  alternarArchivado,
} = useHiloConversacion(idConversacion)

const modalRechazo = ref(false)
const modalCancelar = ref(false)
const hiloRef = ref(null)

const enPanel = computed(() => route.path.startsWith('/panel'))
const nombreBandeja = computed(() => (enPanel.value ? 'panel-mensajes' : 'mensajes'))
const idPedidoOrigen = computed(() => {
  if (route.query.volverA !== 'pedido') return null
  const id = Number(route.query.idPedido)
  return Number.isInteger(id) && id > 0 ? id : null
})

const idSolicitudOrigen = computed(() => {
  if (route.query.volverA !== 'solicitud') return null
  const id = Number(route.query.idSolicitud)
  return Number.isInteger(id) && id > 0 ? id : null
})

const etiquetaVolver = computed(() => {
  if (idPedidoOrigen.value) return 'Volver al pedido'
  if (idSolicitudOrigen.value) return 'Volver a la solicitud'
  return 'Volver a mensajes'
})

watch(idConversacion, () => {
  modalRechazo.value = false
  modalCancelar.value = false
})

function irAlPerfil() {
  if (!contraparte.value?.nombreUsuario) return
  router.push({
    name: 'perfil',
    params: { nombreUsuario: contraparte.value.nombreUsuario },
  })
}

function volver() {
  if (idPedidoOrigen.value) {
    router.push({
      name: enPanel.value ? 'panel-pedido-detalle' : 'pedido-detalle',
      params: { idPedido: idPedidoOrigen.value },
    })
    return
  }
  if (idSolicitudOrigen.value) {
    router.push({
      name: 'solicitud-detalle',
      params: { idSolicitud: idSolicitudOrigen.value },
    })
    return
  }
  router.push({ name: nombreBandeja.value, query: { vista: 'conversaciones' } })
}

async function aceptarSolicitud() {
  const ok = await aceptar()
  if (!ok) return
  exito('Aceptaste la solicitud. Ya pueden escribirle.')
  await nextTick()
  hiloRef.value?.enfocar()
}

async function rechazarSolicitud(motivo) {
  const ok = await rechazar(motivo)
  if (!ok) return
  modalRechazo.value = false
  exito('Rechazaste la solicitud de mensaje.')
}

async function cancelarSolicitud() {
  const ok = await cancelar()
  if (!ok) return
  modalCancelar.value = false
  exito('Cancelaste la solicitud de mensaje.')
}

async function alternarArchivadoYVolver() {
  const archivar = await alternarArchivado()
  if (archivar === null) return
  exito(archivar
    ? 'Archivaste la conversación.'
    : 'Desarchivaste la conversación.')
  if (archivar) {
    router.push({ name: nombreBandeja.value, query: { vista: 'archivadas' } })
  }
}
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← {{ etiquetaVolver }}
    </button>

    <div v-if="cargando" class="mt-6 space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando conversación...</span>
      <div class="h-20 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"></div>
      <div class="h-64 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"></div>
    </div>

    <div
      v-else-if="error"
      class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-8 text-center"
    >
      <p class="text-sm text-rojizo">{{ error }}</p>
      <button
        type="button"
        class="mt-4 min-h-10 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="inicializar"
      >
        Reintentar
      </button>
    </div>

    <template v-else-if="conversacion">
      <header class="mt-5 flex flex-col gap-4 rounded-3xl border border-rosa/50 bg-blanco p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
        <div class="flex min-w-0 items-center gap-3">
          <img
            v-if="contraparte?.urlFotoPerfil || contraparte?.urlFotoPerfilMiniatura"
            :src="contraparte?.urlFotoPerfilMiniatura || contraparte?.urlFotoPerfil"
            alt=""
            class="h-12 w-12 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
          />
          <span
            v-else
            class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-rosa text-base font-bold text-granate ring-1 ring-white/70"
            aria-hidden="true"
          >
            {{ inicialDe(contraparte?.nombreUsuario) }}
          </span>
          <div class="min-w-0">
            <button
              type="button"
              class="block truncate text-left font-display text-lg font-bold text-granate transition hover:text-coral"
              @click="irAlPerfil"
            >
              {{ nombreContraparte }}
            </button>
            <p class="truncate text-xs text-cafe/55">@{{ contraparte?.nombreUsuario }}</p>
            <span
              class="mt-1 inline-flex rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
              :class="estadoInfo.clase"
            >
              {{ estadoInfo.texto }}
            </span>
          </div>
        </div>

        <div class="flex shrink-0 flex-wrap items-center gap-2">
          <button
            type="button"
            class="inline-flex min-h-9 items-center gap-2 rounded-full border border-rosa bg-blanco px-3.5 text-xs font-semibold text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
            :disabled="accionEnCurso"
            @click="alternarArchivadoYVolver"
          >
            {{ conversacion.archivada ? 'Desarchivar' : 'Archivar' }}
          </button>
        </div>
      </header>

      <section
        v-if="esPendiente"
        class="mt-5 rounded-3xl border border-coral/30 bg-rosa/10 p-5"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-coral">
          Solicitud de mensaje
        </p>
        <p class="mt-2 text-sm text-cafe/80">
          <span class="font-semibold text-granate">Motivo:</span>
          {{ conversacion.motivoContacto }}
        </p>
        <div
          v-if="primerMensaje"
          class="mt-3 whitespace-pre-line rounded-2xl border border-rosa/50 bg-blanco px-4 py-3 text-sm leading-relaxed text-cafe"
        >
          {{ primerMensaje.contenido }}
        </div>
        <p class="mt-2 text-[11px] text-cafe/50">
          {{ formatearFechaHora(primerMensaje?.fechaEnvio || conversacion.fechaSolicitud) }}
        </p>

        <div v-if="puedoResponderSolicitud" class="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="min-h-10 rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="accionEnCurso"
            @click="aceptarSolicitud"
          >
            {{ accionEnCurso ? 'Aceptando...' : 'Aceptar y abrir conversación' }}
          </button>
          <button
            type="button"
            class="min-h-10 rounded-full border border-rojizo/40 bg-blanco px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/5 disabled:opacity-60"
            :disabled="accionEnCurso"
            @click="modalRechazo = true"
          >
            Rechazar
          </button>
        </div>
        <div v-else class="mt-4 flex flex-wrap items-center gap-3">
          <span class="text-sm text-cafe/60">
            Esperando que {{ nombreContraparte }} responda.
          </span>
          <button
            v-if="conversacion.puedeCancelar"
            type="button"
            class="min-h-9 rounded-full border border-rosa bg-blanco px-4 text-xs font-semibold text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
            :disabled="accionEnCurso"
            @click="modalCancelar = true"
          >
            Cancelar solicitud
          </button>
        </div>
      </section>

      <section
        v-else-if="esRechazada"
        class="mt-5 rounded-3xl border border-rojizo/25 bg-rojizo/5 p-5"
      >
        <p class="text-sm font-semibold text-rojizo">
          {{ conversacion.soyIniciador
            ? 'La solicitud fue rechazada.'
            : 'Rechazaste esta solicitud.' }}
        </p>
        <p v-if="conversacion.motivoRechazo" class="mt-2 text-sm text-cafe/75">
          <span class="font-semibold">Motivo:</span>
          {{ conversacion.motivoRechazo }}
        </p>
        <p v-if="conversacion.fechaRechazo" class="mt-1 text-xs text-cafe/55">
          {{ formatearFechaHora(conversacion.fechaRechazo) }}
        </p>
        <p
          v-if="conversacion.soyIniciador && conversacion.fechaProximoEnvio"
          class="mt-3 text-sm text-cafe/70"
        >
          Vas a poder volver a enviar una solicitud el
          <strong>{{ formatearFechaHora(conversacion.fechaProximoEnvio) }}</strong>.
        </p>
      </section>

      <section
        v-else-if="esCancelada"
        class="mt-5 rounded-3xl border border-borde/60 bg-superficie-alt p-5"
      >
        <p class="text-sm font-semibold text-cafe">
          {{ conversacion.soyIniciador
            ? 'Cancelaste esta solicitud.'
            : 'La solicitud fue cancelada.' }}
        </p>
        <p v-if="conversacion.fechaRespuesta" class="mt-1 text-xs text-cafe/55">
          {{ formatearFechaHora(conversacion.fechaRespuesta) }}
        </p>
      </section>

      <section
        v-else-if="esVencida"
        class="mt-5 rounded-3xl border border-borde/60 bg-superficie-alt p-5"
      >
        <p class="text-sm font-semibold text-cafe">
          {{ conversacion.soyIniciador
            ? 'La solicitud venció sin respuesta.'
            : 'La solicitud venció.' }}
        </p>
        <p class="mt-2 text-sm text-cafe/70">
          <span class="font-semibold">Motivo:</span>
          {{ conversacion.motivoContacto }}
        </p>
        <p v-if="conversacion.fechaRespuesta" class="mt-2 text-xs text-cafe/55">
          Venció el {{ formatearFechaHora(conversacion.fechaRespuesta) }}
        </p>
        <p v-if="conversacion.soyIniciador" class="mt-3 text-sm text-cafe/70">
          Podés volver a enviar una solicitud desde el perfil de {{ nombreContraparte }}.
        </p>
        <button
          v-if="conversacion.soyIniciador"
          type="button"
          class="mt-3 min-h-9 rounded-full border border-rosa bg-blanco px-4 text-xs font-semibold text-cafe transition hover:bg-rosa/20"
          @click="irAlPerfil"
        >
          Ir al perfil
        </button>
      </section>

      <div v-if="esAceptada" class="mt-5">
        <HiloConversacion
          ref="hiloRef"
          v-model:texto="texto"
          :mensajes="mensajes"
          :max-mensaje="maxMensaje"
          :enviando="enviando"
          :error-envio="errorEnvio"
          :hay-mas="hayMas"
          :cargando-mas="cargandoMas"
          :contraparte-escribiendo="contraparteEscribiendo"
          :nombre-contraparte="nombreContraparte"
          :puede-enviar="Boolean(conversacion?.puedeEnviar)"
          :motivo-no-enviable="conversacion?.motivoNoEnviable || ''"
          :cargar-anteriores="cargarAnteriores"
          @enviar="enviar"
          @escribiendo="avisarEscribiendo"
        />
      </div>

      <p
        v-if="errorAccion"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-3.5 py-2.5 text-sm text-rojizo"
        role="alert"
      >
        {{ errorAccion }}
      </p>
    </template>

    <ModalRechazoConversacion
      :abierto="modalRechazo"
      :cargando="accionEnCurso"
      :error="errorAccion"
      :maximo="maxMotivo"
      @cerrar="modalRechazo = false"
      @confirmar="rechazarSolicitud"
    />

    <ModalConfirmacion
      :abierto="modalCancelar"
      titulo="¿Cancelar la solicitud?"
      mensaje="La persona va a ver que retiraste la solicitud. No se va a abrir la conversación."
      texto-confirmar="Cancelar solicitud"
      texto-cancelar="Volver"
      destructivo
      @cerrar="modalCancelar = false"
      @confirmar="cancelarSolicitud"
    />
  </div>
</template>
