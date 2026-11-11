<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNotificaciones } from '../composables/useNotificaciones'
import { formatearPrecio } from '../servicios/moneda'
import {
  cargarCompra,
  confirmarPagoSimulado,
  descargarArchivoCompra,
  estadoCompraDe,
  estadoGrupoDe,
  pagarGrupo,
  rechazarPagoSimulado,
  sincronizarCompra,
} from '../servicios/compras'

const route = useRoute()
const router = useRouter()
const { exito, error: toastError } = useNotificaciones()

const idCompra = computed(() => Number(route.params.id))
const compra = ref(null)
const cargando = ref(true)
const error = ref('')
const pagando = ref(null)
const sincronizando = ref(false)
const pagoSimulado = ref(null)
const descargando = ref(null)

const grupos = computed(() => compra.value?.grupos || [])
const estado = computed(() => estadoCompraDe(compra.value?.estado))

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    compra.value = await cargarCompra(idCompra.value)
    if (route.query.pago) {
      await sincronizar(true)
      const limpia = { ...route.query }
      delete limpia.pago
      router.replace({ query: limpia })
      if (compra.value?.estado === 'PAGA') {
        exito('Pago acreditado. La compra ya está completa.')
      }
    }
  } catch (e) {
    error.value = e.response?.status === 403
      ? 'No tenés acceso a esta compra.'
      : (e.response?.data?.message || 'No se pudo cargar la compra.')
  } finally {
    cargando.value = false
  }
}

async function pagar(grupo) {
  if (pagando.value) return
  pagando.value = grupo.idCompraGrupo
  try {
    const pago = await pagarGrupo(idCompra.value, grupo.idCompraGrupo)
    if (pago.instruccion && /^https?:\/\//.test(pago.instruccion)) {
      window.location.href = pago.instruccion
      return
    }
    pagoSimulado.value = pago
    exito('Pago simulado creado. Confirmalo desde esta pantalla.')
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo iniciar el pago.')
  } finally {
    pagando.value = null
  }
}

async function confirmarSimulado() {
  if (!pagoSimulado.value) return
  try {
    compra.value = await confirmarPagoSimulado(pagoSimulado.value.idIntentoCobro)
    pagoSimulado.value = null
    exito('Pago simulado confirmado.')
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo confirmar el pago.')
  }
}

async function rechazarSimulado() {
  if (!pagoSimulado.value) return
  try {
    compra.value = await rechazarPagoSimulado(pagoSimulado.value.idIntentoCobro)
    pagoSimulado.value = null
    toastError('Pago simulado rechazado.')
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo rechazar el pago.')
  }
}

async function sincronizar(silencioso = false) {
  if (sincronizando.value) return
  sincronizando.value = true
  try {
    compra.value = await sincronizarCompra(idCompra.value)
    if (!silencioso) exito('Estado actualizado.')
  } catch (e) {
    if (!silencioso) {
      toastError(e.response?.data?.message || 'No se pudo actualizar el estado.')
    }
  } finally {
    sincronizando.value = false
  }
}

async function descargar(item) {
  if (descargando.value) return
  descargando.value = item.idCompraItem
  try {
    const blob = await descargarArchivoCompra(idCompra.value, item.idCompraItem)
    const url = URL.createObjectURL(blob)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = `${item.tituloProducto}-${item.nombreVariante || ''}`.trim()
    document.body.appendChild(enlace)
    enlace.click()
    enlace.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo descargar el archivo.')
  } finally {
    descargando.value = null
  }
}

function fecha(valor) {
  if (!valor) return '—'
  try {
    return new Intl.DateTimeFormat('es-AR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(valor))
  } catch {
    return valor
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-screen-xl px-4 py-8 sm:px-8 sm:py-10">
    <router-link
      to="/compras"
      class="text-sm text-granate transition hover:text-coral"
    >
      ← Volver a mis compras
    </router-link>

    <p v-if="cargando" class="py-16 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="mt-8 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo">
      {{ error }}
    </p>

    <template v-else-if="compra">
      <div class="mt-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div class="flex flex-wrap items-center gap-3">
            <h1 class="font-display text-2xl font-bold text-granate">
              Compra #{{ compra.idCompra }}
            </h1>
            <span
              class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
              :class="estado.clase"
            >
              {{ estado.texto }}
            </span>
          </div>
          <p class="mt-1 text-sm text-cafe/55">
            {{ fecha(compra.fechaCreacion) }}
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button
            type="button"
            class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-50"
            :disabled="sincronizando"
            @click="sincronizar(false)"
          >
            {{ sincronizando ? 'Actualizando...' : 'Sincronizar estado' }}
          </button>
          <p class="font-display text-2xl font-extrabold text-coral">
            {{ formatearPrecio(compra.total, compra.moneda || 'ARS') }}
          </p>
        </div>
      </div>

      <div class="mt-6 space-y-5">
        <section
          v-for="grupo in grupos"
          :key="grupo.idCompraGrupo"
          class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm"
        >
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="flex items-center gap-2">
              <img
                v-if="grupo.urlFotoArtista"
                :src="grupo.urlFotoArtista"
                alt=""
                class="h-8 w-8 rounded-full object-cover ring-1 ring-borde/50"
              />
              <div>
                <p class="text-sm font-semibold text-cafe/75">
                  @{{ grupo.nombreArtista }}
                </p>
                <p class="text-xs text-cafe/50">
                  {{ grupo.items.length }} ítem(s)
                </p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <span
                class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                :class="estadoGrupoDe(grupo.estado).clase"
              >
                {{ estadoGrupoDe(grupo.estado).texto }}
              </span>
              <p class="font-display text-lg font-bold text-granate">
                {{ formatearPrecio(grupo.subtotal, grupo.moneda || 'ARS') }}
              </p>
            </div>
          </div>

          <p v-if="grupo.mensajePago" class="mt-2 text-xs text-cafe/60">
            {{ grupo.mensajePago }}
          </p>

          <div class="mt-4 divide-y divide-rosa/40">
            <article
              v-for="item in grupo.items"
              :key="item.idCompraItem"
              class="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div class="min-w-0">
                <p class="font-display text-base font-bold text-granate">
                  {{ item.tituloProducto }}
                </p>
                <p class="mt-0.5 text-sm text-cafe/60">
                  {{ item.nombreVariante }} · {{ item.cantidad }} unidad(es)
                </p>
                <p class="mt-1 text-[11px] text-cafe/45">
                  {{ item.tipoEntrega === 'DIGITAL' ? 'Entrega digital' : 'Pieza física · envío coordinado con el artista' }}
                  · {{ item.estadoEntrega === 'ENTREGADO' ? 'Entregado' : 'Pendiente de entrega' }}
                </p>
              </div>

              <div class="flex shrink-0 items-center gap-3">
                <p class="font-display text-base font-bold text-coral">
                  {{ formatearPrecio(item.precioCobro, compra.moneda || 'ARS') }}
                </p>
                <button
                  v-if="item.puedeDescargar"
                  type="button"
                  class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
                  :disabled="descargando === item.idCompraItem"
                  @click="descargar(item)"
                >
                  {{ descargando === item.idCompraItem ? 'Descargando...' : 'Descargar archivo' }}
                </button>
                <span v-else-if="item.motivoNoDescarga" class="max-w-48 text-right text-[11px] text-cafe/45">
                  {{ item.motivoNoDescarga }}
                </span>
              </div>
            </article>
          </div>

          <div v-if="grupo.puedePagar" class="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-rosa/40 pt-4">
            <p class="text-xs text-cafe/55">
              El pago se acredita directo a la cuenta del artista.
            </p>
            <button
              type="button"
              class="rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              :disabled="pagando === grupo.idCompraGrupo"
              @click="pagar(grupo)"
            >
              {{ pagando === grupo.idCompraGrupo ? 'Iniciando...' : 'Pagar este grupo' }}
            </button>
          </div>
        </section>
      </div>

      <div
        v-if="pagoSimulado"
        class="fixed inset-x-4 bottom-4 z-50 mx-auto max-w-xl rounded-2xl border border-coral/40 bg-superficie p-5 shadow-2xl sm:inset-x-auto sm:right-6"
      >
        <p class="font-display text-base font-bold text-granate">
          Pago simulado
        </p>
        <p class="mt-1 text-sm text-cafe/65">
          {{ pagoSimulado.instruccion }}
        </p>
        <div class="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-lg bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
            @click="confirmarSimulado"
          >
            Confirmar pago
          </button>
          <button
            type="button"
            class="rounded-lg border border-rojizo/40 px-4 py-2 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10"
            @click="rechazarSimulado"
          >
            Rechazar
          </button>
        </div>
      </div>
    </template>
  </div>
</template>
