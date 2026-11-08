<script setup>
import { computed, ref, watch } from 'vue'
import api from '../servicios/api'
import { obtenerContacto } from '../servicios/contacto'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  usuario: { type: Object, default: null },
})

const emit = defineEmits(['cerrar', 'aplicada'])

const tipo = ref('SUSPENSION')
const motivo = ref('')
const fechaFin = ref('')
const guardando = ref(false)
const error = ref('')
const contacto = ref('')

const esTemporal = computed(() => tipo.value === 'SUSPENSION')

const fechaFinTexto = computed(() => {
  if (!fechaFin.value) return ''
  const fecha = new Date(fechaFin.value)
  if (Number.isNaN(fecha.getTime())) return ''
  return new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(fecha)
})

watch(
  () => props.abierto,
  async (abierto) => {
    if (!abierto) return
    tipo.value = 'SUSPENSION'
    motivo.value = ''
    fechaFin.value = ''
    error.value = ''
    if (!contacto.value) {
      contacto.value = await obtenerContacto()
    }
  },
)

async function enviar() {
  error.value = ''
  if (!props.usuario) {
    error.value = 'No se seleccionó un usuario.'
    return
  }
  if (!motivo.value.trim()) {
    error.value = 'El motivo es obligatorio.'
    return
  }
  if (esTemporal.value && !fechaFin.value) {
    error.value = 'Indicá la fecha y hora de fin de la suspensión.'
    return
  }

  guardando.value = true
  try {
    await api.post('/admin/sanciones', {
      idUsuario: props.usuario.idUsuario,
      tipo: tipo.value,
      motivo: motivo.value.trim(),
      fechaFin: esTemporal.value ? fechaFin.value : null,
    })
    emit('aplicada')
    emit('cerrar')
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo aplicar la sanción.'
  } finally {
    guardando.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="abierto"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-granate/50 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-sancion"
      @click.self="emit('cerrar')"
    >
      <div class="w-full max-w-lg overflow-hidden rounded-3xl bg-blanco shadow-2xl">
        <header class="border-b border-rosa/40 px-6 py-4">
          <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-coral">
            Comunidad
          </p>
          <h2 id="titulo-sancion" class="mt-1 font-display text-xl font-extrabold text-granate">
            Inhabilitar cuenta
          </h2>
          <p class="mt-1 text-sm text-cafe/65">
            @{{ usuario?.nombreUsuario }}
            <span v-if="usuario?.correo">· {{ usuario.correo }}</span>
          </p>
        </header>

        <form class="space-y-4 px-6 py-5" @submit.prevent="enviar">
          <fieldset>
            <legend class="mb-2 text-sm font-semibold text-cafe">Tipo de sanción</legend>
            <div class="grid gap-2 sm:grid-cols-2">
              <label
                class="flex cursor-pointer flex-col gap-1 rounded-xl border px-3 py-3 text-sm transition"
                :class="esTemporal ? 'border-coral bg-rosa/20' : 'border-rosa/60 hover:bg-rosa/10'"
              >
                <span class="flex items-center gap-2 font-semibold text-granate">
                  <input v-model="tipo" type="radio" value="SUSPENSION" class="accent-coral" />
                  Suspensión temporal
                </span>
                <span class="text-xs text-cafe/60">Vuelve a entrar sola al vencer.</span>
              </label>
              <label
                class="flex cursor-pointer flex-col gap-1 rounded-xl border px-3 py-3 text-sm transition"
                :class="!esTemporal ? 'border-coral bg-rosa/20' : 'border-rosa/60 hover:bg-rosa/10'"
              >
                <span class="flex items-center gap-2 font-semibold text-granate">
                  <input v-model="tipo" type="radio" value="INHABILITACION" class="accent-coral" />
                  Inhabilitación permanente
                </span>
                <span class="text-xs text-cafe/60">Solo se revierte a mano.</span>
              </label>
            </div>
          </fieldset>

          <div v-if="esTemporal">
            <label for="fecha-fin-sancion" class="block text-sm font-semibold text-cafe">
              Finaliza el
            </label>
            <input
              id="fecha-fin-sancion"
              v-model="fechaFin"
              type="datetime-local"
              class="mt-1 block w-full rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
          </div>

          <div>
            <label for="motivo-sancion" class="block text-sm font-semibold text-cafe">
              Motivo <span class="text-rojizo">*</span>
            </label>
            <textarea
              id="motivo-sancion"
              v-model="motivo"
              rows="3"
              maxlength="500"
              placeholder="Explicá el motivo. El usuario lo va a ver al intentar ingresar."
              class="mt-1 block w-full resize-y rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            ></textarea>
          </div>

          <div class="rounded-xl border border-durazno/50 bg-durazno/20 px-3 py-3 text-sm text-cafe/80">
            <p><span class="font-semibold">La persona será desconectada de inmediato</span> y no podrá volver a iniciar sesión.</p>
            <p class="mt-1">Se conservan su cuenta, sus pedidos y su historial.</p>
            <p v-if="esTemporal && fechaFinTexto" class="mt-1">
              Podrá volver a ingresar el <span class="font-semibold">{{ fechaFinTexto }}</span>.
            </p>
            <p class="mt-1">
              Consultas: <span class="font-semibold">{{ contacto }}</span>
            </p>
          </div>

          <p v-if="error" class="rounded-lg bg-rosa/40 px-3 py-2 text-sm text-rojizo" role="alert">
            {{ error }}
          </p>

          <div class="flex flex-wrap justify-end gap-2 pt-1">
            <button
              type="button"
              class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-cafe transition hover:bg-rosa/20"
              @click="emit('cerrar')"
            >
              Cancelar
            </button>
            <button
              type="submit"
              :disabled="guardando"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-rojizo px-5 text-sm font-semibold text-white transition hover:bg-granate disabled:opacity-50"
            >
              {{ guardando ? 'Inhabilitando…' : 'Inhabilitar cuenta' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
