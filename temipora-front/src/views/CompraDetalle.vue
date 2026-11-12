<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LedgerCompraItem from '../components/LedgerCompraItem.vue'
import { useNotificaciones } from '../composables/useNotificaciones'
import { formatearPrecio } from '../servicios/moneda'
import { guardarArchivo } from '../servicios/descargas'
import {
  cargarCompra,
  confirmarPagoSimulado,
  descargarCompra,
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
const descargandoTodo = ref(false)

const grupos = computed(() => compra.value?.grupos || [])
const estado = computed(() => estadoCompraDe(compra.value?.estado))
const portada = computed(() => {
  for (const grupo of grupos.value) {
    for (const item of grupo.items || []) {
      if (item.imagenUrl) return item.imagenUrl
    }
  }
  return null
})
const totalProductos = computed(() => {
  const productos = new Set()
  for (const grupo of grupos.value) {
    for (const item of grupo.items || []) {
      if (item.idProducto) productos.add(item.idProducto)
    }
  }
  return productos.size
})

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

async function descargarTodo() {
  if (descargandoTodo.value) return
  descargandoTodo.value = true
  try {
    const { blob, nombre } = await descargarCompra(idCompra.value)
    guardarArchivo(blob, nombre)
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo descargar la compra.')
  } finally {
    descargandoTodo.value = false
  }
}

function fecha(valor) {
  if (!valor) return '—'
  try {
    return new Intl.DateTimeFormat('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(valor))
  } catch {
    return valor
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-4 py-8 sm:px-8 sm:py-10">
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
      <section class="mt-4 rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex min-w-0 items-start gap-4">
            <img
              v-if="portada"
              :src="portada"
              alt=""
              class="h-20 w-20 shrink-0 rounded-2xl border border-borde/50 object-cover"
            />
            <div class="min-w-0">
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
              <p class="mt-1 text-xs text-cafe/50">
                {{ totalProductos }} producto(s) · {{ grupos.length }} artista(s)
              </p>
            </div>
          </div>
          <div class="flex flex-col items-start gap-2 sm:items-end">
            <div class="text-left sm:text-right">
              <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Total pagado
              </p>
              <p class="font-display text-2xl font-extrabold text-coral">
                {{ formatearPrecio(compra.total, compra.moneda || 'ARS') }}
              </p>
            </div>
            <div class="flex flex-wrap items-center justify-end gap-2">
              <button
                v-if="compra.puedeDescargar"
                type="button"
                class="rounded-lg bg-coral px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
                :disabled="descargandoTodo"
                @click="descargarTodo"
              >
                {{ descargandoTodo ? 'Preparando...' : 'Descargar todo' }}
              </button>
              <button
                type="button"
                class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-50"
                :disabled="sincronizando"
                @click="sincronizar(false)"
              >
                {{ sincronizando ? 'Actualizando...' : 'Sincronizar estado' }}
              </button>
            </div>
          </div>
        </div>
      </section>

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
                <router-link
                  v-if="grupo.esReventa ? grupo.nombreVendedor : grupo.nombreArtista"
                  :to="{ name: 'perfil', params: { nombreUsuario: grupo.esReventa ? grupo.nombreVendedor : grupo.nombreArtista } }"
                  class="text-sm font-semibold text-cafe/75 transition hover:text-coral"
                >
                  @{{ grupo.esReventa ? grupo.nombreVendedor : grupo.nombreArtista }}
                </router-link>
                <p v-else class="text-sm font-semibold text-cafe/75">
                  {{ grupo.esReventa ? 'Revendedor' : 'Artista original' }}
                </p>
                <p class="text-xs text-cafe/50">
                  <span
                    v-if="grupo.esReventa"
                    class="mr-1 rounded-full bg-coral/15 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-[0.08em] text-coral"
                  >
                    Reventa
                  </span>
                  {{ grupo.esReventa ? 'Revendedor' : 'Artista original' }} · {{ grupo.items.length }} ítem(s)
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
              class="py-4"
            >
              <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div class="flex min-w-0 items-start gap-3">
                  <img
                    v-if="item.imagenUrl"
                    :src="item.imagenUrl"
                    alt=""
                    class="h-20 w-20 shrink-0 rounded-xl border border-borde/50 object-cover"
                  />
                  <span
                    v-else
                    class="flex h-20 w-20 shrink-0 items-center justify-center rounded-xl bg-rosa/30 text-granate"
                    aria-hidden="true"
                  >
                    <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007Z" />
                    </svg>
                  </span>

                  <div class="min-w-0">
                    <p class="font-display text-base font-bold text-granate">
                      {{ item.tituloProducto }}
                    </p>
                    <p class="mt-0.5 text-sm text-cafe/60">
                      {{ item.nombreVariante }} · {{ item.cantidad }} unidad(es)
                    </p>
                    <p v-if="item.esReventa && item.nombreArtistaOriginal" class="mt-0.5 text-[11px] text-cafe/45">
                      Obra original de @{{ item.nombreArtistaOriginal }}
                    </p>
                    <p class="mt-1 text-[11px] text-cafe/45">
                      {{ item.tipoEntrega === 'DIGITAL'
                        ? 'Entrega digital'
                        : (item.esReventa
                          ? 'Pieza física · envío coordinado con el revendedor'
                          : 'Pieza física · envío coordinado con el artista') }}
                      · {{ item.estadoEntrega === 'ENTREGADO' ? 'Entregado' : 'Pendiente de entrega' }}
                    </p>
                    <p
                      v-if="item.tieneLedger"
                      class="mt-2 inline-flex items-center gap-1.5 rounded-full border border-coral/25 bg-coral/10 px-2.5 py-0.5 text-[11px] font-semibold text-coral"
                    >
                      Certificado de procedencia
                    </p>
                  </div>
                </div>

                <div class="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                  <p class="font-display text-base font-bold text-coral">
                    {{ formatearPrecio(item.precioTotalCobro ?? item.precioCobro, compra.moneda || 'ARS') }}
                  </p>
                  <p v-if="item.cantidad > 1" class="text-[11px] text-cafe/45">
                    {{ formatearPrecio(item.precioCobro, compra.moneda || 'ARS') }} c/u
                  </p>
                  <span
                    v-if="item.puedeDescargar"
                    class="inline-flex max-w-48 items-center justify-center rounded-full border border-rosa bg-blanco px-3 py-1 text-[11px] font-semibold text-granate"
                  >
                    Incluye {{ item.tipoEntrega === 'DIGITAL' ? 'archivo digital' : 'imagen de la pieza' }}
                  </span>
                  <span v-else-if="item.motivoNoDescarga" class="max-w-48 text-right text-[11px] text-cafe/45">
                    {{ item.motivoNoDescarga }}
                  </span>
                </div>
              </div>

              <details class="mt-3 rounded-xl border border-rosa/40 bg-rosa/5 px-4 py-3">
                <summary class="cursor-pointer text-xs font-semibold text-granate">
                  Ver detalles de la compra
                </summary>
                <div class="mt-3 grid gap-2 text-xs text-cafe/65 sm:grid-cols-2">
                  <p><span class="font-semibold text-cafe/80">ID de compra:</span> {{ compra.idCompra }}</p>
                  <p><span class="font-semibold text-cafe/80">ID de ítem:</span> {{ item.idCompraItem }}</p>
                  <p><span class="font-semibold text-cafe/80">Producto:</span> #{{ item.idProducto }}</p>
                  <p><span class="font-semibold text-cafe/80">Variante:</span> #{{ item.idVariante }}</p>
                  <p v-if="item.fechaEntrega">
                    <span class="font-semibold text-cafe/80">Entregado:</span> {{ fecha(item.fechaEntrega) }}
                  </p>
                  <p v-if="item.sha256Archivo" class="break-all sm:col-span-2">
                    <span class="font-semibold text-cafe/80">SHA-256 del archivo:</span>
                    <span class="font-mono">{{ item.sha256Archivo }}</span>
                  </p>
                </div>

                <div v-if="item.licencia" class="mt-4 border-t border-rosa/40 pt-3">
                  <p class="text-xs font-semibold text-granate">Licencia adquirida</p>
                  <p class="mt-1 text-xs text-cafe/65">
                    Versión {{ item.licencia.version }} · ID {{ item.licencia.idLicenciaProducto }}
                  </p>
                  <ul v-if="item.licencia.derechos?.length" class="mt-2 flex flex-wrap gap-1.5">
                    <li
                      v-for="derecho in item.licencia.derechos"
                      :key="derecho.idDerechoTermino || derecho.codigo"
                      class="rounded-full border border-rosa/60 bg-blanco px-2 py-0.5 text-[11px] text-cafe/70"
                    >
                      {{ derecho.nombre || derecho.codigo }}
                    </li>
                  </ul>
                  <p class="mt-2 text-xs text-cafe/60">
                    {{ item.licencia.permiteModificaciones ? 'Permite modificaciones' : 'No permite modificaciones' }}
                    · {{ item.licencia.requiereCredito ? 'Requiere crédito' : 'No requiere crédito' }}
                  </p>
                  <p v-if="item.licencia.textoAdicional" class="mt-2 whitespace-pre-line text-xs text-cafe/60">
                    {{ item.licencia.textoAdicional }}
                  </p>
                </div>

                <p v-if="item.tieneLedger" class="mt-4 border-t border-rosa/40 pt-3 text-xs text-cafe/60">
                  Esta pieza tiene un certificado de procedencia verificado.
                </p>
              </details>

              <LedgerCompraItem
                v-if="item.tieneLedger"
                :id-compra="compra.idCompra"
                :item="item"
              />
            </article>
          </div>

          <div v-if="grupo.puedePagar" class="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-rosa/40 pt-4">
            <p class="text-xs text-cafe/55">
              {{ grupo.esReventa
                ? 'El pago se acredita directo a la cuenta del revendedor.'
                : 'El pago se acredita directo a la cuenta del artista.' }}
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

      <section class="mt-6 rounded-[22px] border border-dashed border-rosa/70 bg-blanco p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="font-display text-base font-bold text-granate">Reseña</p>
          </div>
          <button
            type="button"
            disabled
            class="cursor-not-allowed rounded-lg border border-rosa px-4 py-2 text-sm font-semibold text-cafe/40"
          >
            Escribir reseña
          </button>
        </div>
      </section>

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
