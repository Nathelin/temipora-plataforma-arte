<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  archivarReventa,
  misReventas,
  piezasRevendibles,
} from '../servicios/reventas'
import { useNotificaciones } from '../composables/useNotificaciones'
import BurbujaInfo from '../components/BurbujaInfo.vue'
import PageHeader from '../components/PageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import ModalReventa from '../components/ModalReventa.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'

const router = useRouter()
const { exito, error: notificarError } = useNotificaciones()

const ESTADOS = {
  PUBLICADA: { texto: 'Publicada', clase: 'border-green-700/30 bg-green-500/10 text-green-700' },
  RESERVADA: { texto: 'Compra en curso', clase: 'border-mantequilla bg-mantequilla/60 text-granate' },
  VENDIDA: { texto: 'Vendida', clase: 'border-coral/30 bg-coral/15 text-coral' },
  ARCHIVADA: { texto: 'Archivada', clase: 'border-cafe/25 bg-cafe/10 text-cafe/60' },
  SUSPENDIDA: { texto: 'Suspendida', clase: 'border-rojizo/30 bg-rojizo/10 text-rojizo' },
  VENCIDA: { texto: 'Vencida', clase: 'border-cafe/25 bg-cafe/10 text-cafe/60' },
}

const cargando = ref(true)
const error = ref('')
const piezas = ref([])
const publicaciones = ref([])
const cuenta = ref(null)
const comisionPorcentaje = ref(null)
const arancelPorcentaje = ref(null)

const modalAbierto = ref(false)
const piezaSeleccionada = ref(null)
const publicacionEditando = ref(null)
const publicacionArchivar = ref(null)
const archivando = ref(false)
const mostrarHistorial = ref(false)

const conectada = computed(() => !!cuenta.value?.conectada)
const activas = computed(() => publicaciones.value.filter(
  (publicacion) => publicacion.estado === 'PUBLICADA'
    || publicacion.estado === 'RESERVADA',
))
const historial = computed(() => publicaciones.value.filter(
  (publicacion) => !['PUBLICADA', 'RESERVADA'].includes(publicacion.estado),
))

function formatear(valor, moneda) {
  return valor != null ? formatearPrecio(valor, moneda || 'ARS') : '—'
}

function estadoTexto(estado) {
  return ESTADOS[estado]?.texto || estado
}

function estadoClase(estado) {
  return ESTADOS[estado]?.clase || 'border-cafe/25 bg-cafe/10 text-cafe/60'
}

function fecha(valor) {
  if (!valor) return ''
  const fechaValor = new Date(valor)
  if (Number.isNaN(fechaValor.getTime())) return ''
  return new Intl.DateTimeFormat('es-AR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(fechaValor)
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [piezasRes, publicacionesRes, estadoRes, comisionesRes, arancelRes] =
      await Promise.all([
        piezasRevendibles(),
        misReventas(),
        api.get('/pagos/mercadopago/oauth/estado').catch(() => null),
        api.get('/comisiones/vigentes').catch(() => null),
        api.get('/pagos/arancel-estimado').catch(() => null),
      ])
    piezas.value = piezasRes || []
    publicaciones.value = publicacionesRes || []
    cuenta.value = estadoRes?.data || null
    const comision = comisionesRes?.data?.porcentajeReventa
      ?? comisionesRes?.data?.porcentajeProducto
    comisionPorcentaje.value = comision != null ? Number(comision) : null
    const arancel = arancelRes?.data?.porcentajeTotal
    arancelPorcentaje.value = arancel != null ? Number(arancel) : null
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar tu actividad de reventa.'
  } finally {
    cargando.value = false
  }
}

function abrirAlta(pieza) {
  if (!conectada.value) {
    router.push('/configuracion/pagos-y-cobros')
    return
  }
  piezaSeleccionada.value = pieza
  publicacionEditando.value = null
  modalAbierto.value = true
}

function abrirEdicion(publicacion) {
  piezaSeleccionada.value = null
  publicacionEditando.value = publicacion
  modalAbierto.value = true
}

function cerrarModal() {
  modalAbierto.value = false
}

async function alGuardar() {
  const eraEdicion = !!publicacionEditando.value
  modalAbierto.value = false
  piezaSeleccionada.value = null
  publicacionEditando.value = null
  await cargar()
  exito(eraEdicion ? 'Precio actualizado.' : 'Reventa publicada.')
}

function pedirArchivar(publicacion) {
  publicacionArchivar.value = publicacion
}

function cerrarArchivar() {
  if (archivando.value) return
  publicacionArchivar.value = null
}

async function confirmarArchivar() {
  if (!publicacionArchivar.value || archivando.value) return
  archivando.value = true
  try {
    await archivarReventa(publicacionArchivar.value.idPublicacionReventa)
    publicacionArchivar.value = null
    await cargar()
    exito('Publicación archivada. La pieza vuelve a «Puedo revender».')
  } catch (e) {
    notificarError(e.response?.data?.message
      || 'No se pudo archivar la publicación.')
  } finally {
    archivando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      eyebrow="Tu actividad"
      titulo="Reventa"
      descripcion="Las piezas únicas que podés volver a poner en venta y las que ya publicaste."
      :nivel="2"
    >
      <template #acciones>
        <BurbujaInfo etiqueta="Cómo funciona la reventa">
          <strong class="block">Publicás, se vende y el dinero se acredita en tu cuenta.</strong>
          <span class="mt-1 block">
            Elegís una pieza única que compraste, fijás un precio hasta el tope
            original y la publicación queda visible para cualquiera. Si la
            archivás, la pieza vuelve a «Puedo revender».
          </span>
        </BurbujaInfo>
      </template>
    </PageHeader>

    <div
      v-if="!cargando && !conectada"
      class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-rosa/60 bg-rosa/15 px-4 py-3"
    >
      <p class="text-sm text-cafe/75">
        Conectá MercadoPago para poder cobrar tus reventas.
      </p>
      <router-link
        to="/configuracion/pagos-y-cobros"
        class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
      >
        Conectar cuenta →
      </router-link>
    </div>

    <CargandoPagina v-if="cargando" tipo="lista" mensaje="Cargando tu actividad de reventa…" :tarjetas="5" />
    <p v-else-if="error" role="alert" class="py-12 text-center text-sm text-rojizo">
      {{ error }}
    </p>

    <template v-else>
      <section>
        <div class="flex flex-wrap items-end justify-between gap-2">
          <div>
            <h3 class="font-display text-lg font-bold text-granate">Puedo revender</h3>
            <p class="mt-1 text-sm text-cafe/60">
              Piezas únicas que compraste, ya te fueron entregadas y todavía no tienen una reventa activa.
            </p>
          </div>
          <span class="rounded-full bg-rosa/30 px-2.5 py-0.5 text-xs font-semibold text-granate">
            {{ piezas.length }}
          </span>
        </div>

        <EmptyState
          v-if="!piezas.length"
          titulo="No tenés piezas para poner en reventa."
          detalle="Cuando compres una pieza única con derecho de reventa, va a aparecer acá."
        />

        <div v-else class="mt-3 space-y-3">
          <article
            v-for="pieza in piezas"
            :key="pieza.idCompraItem"
            class="flex flex-col gap-4 rounded-2xl border border-rosa/50 bg-blanco p-5 transition hover:border-coral/40 hover:shadow-[0_10px_28px_rgba(74,29,46,0.08)] sm:flex-row sm:items-center"
          >
            <img
              v-if="pieza.imagenUrl"
              :src="pieza.imagenUrl"
              :alt="pieza.titulo"
              class="h-20 w-20 shrink-0 rounded-xl border border-rosa/50 object-cover"
            />
            <div class="min-w-0 flex-1">
              <h4 class="font-display text-base font-bold text-granate">
                {{ pieza.titulo }}
              </h4>
              <p class="text-sm text-cafe/60">{{ pieza.nombreVariante }}</p>
              <p class="mt-1 text-xs text-cafe/50">
                Tope: {{ formatear(pieza.precioTope, pieza.moneda) }}
                · Pagaste: {{ formatear(pieza.precioPagado, pieza.moneda) }}
              </p>
              <p v-if="pieza.nombreArtistaOriginal" class="mt-0.5 text-xs text-cafe/45">
                Obra original de @{{ pieza.nombreArtistaOriginal }}
              </p>
            </div>
            <button
              type="button"
              class="shrink-0 rounded-lg bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
              @click="abrirAlta(pieza)"
            >
              Poner en reventa
            </button>
          </article>
        </div>
      </section>

      <section>
        <div class="flex flex-wrap items-end justify-between gap-2">
          <div>
            <h3 class="font-display text-lg font-bold text-granate">Mis publicaciones</h3>
            <p class="mt-1 text-sm text-cafe/60">
              Las reventas activas y el historial de las que ya cerraron.
            </p>
          </div>
          <span class="rounded-full bg-rosa/30 px-2.5 py-0.5 text-xs font-semibold text-granate">
            {{ activas.length }}
          </span>
        </div>

        <EmptyState
          v-if="!activas.length"
          titulo="No tenés reventas activas."
          detalle="Elegí una pieza de «Puedo revender» para publicarla."
        />

        <div v-else class="mt-3 space-y-3">
          <article
            v-for="publicacion in activas"
            :key="publicacion.idPublicacionReventa"
            class="flex flex-col gap-4 rounded-2xl border border-rosa/50 bg-blanco p-5 transition hover:border-coral/40 hover:shadow-[0_10px_28px_rgba(74,29,46,0.08)] sm:flex-row sm:items-center"
          >
            <img
              v-if="publicacion.imagenUrl"
              :src="publicacion.imagenUrl"
              :alt="publicacion.titulo"
              class="h-20 w-20 shrink-0 rounded-xl border border-rosa/50 object-cover"
            />
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2">
                <h4 class="font-display text-base font-bold text-granate">
                  {{ publicacion.titulo }}
                </h4>
                <span
                  class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                  :class="estadoClase(publicacion.estado)"
                >
                  {{ estadoTexto(publicacion.estado) }}
                </span>
              </div>
              <p class="text-sm text-cafe/60">
                {{ publicacion.tituloOriginal || publicacion.nombreVariante }}
              </p>
              <p class="mt-1 font-semibold text-coral">
                {{ formatear(publicacion.precio, publicacion.moneda) }}
              </p>
              <p class="text-xs text-cafe/45">
                Tope: {{ formatear(publicacion.precioTope, publicacion.moneda) }}
                <template v-if="publicacion.fechaPublicacion">
                  · Publicada el {{ fecha(publicacion.fechaPublicacion) }}
                </template>
              </p>
              <p v-if="publicacion.estado === 'RESERVADA'" class="mt-1 text-xs text-cafe/60">
                Hay una compra en curso. Si la reserva vence, la publicación vuelve a estar disponible.
              </p>
            </div>
            <div class="flex shrink-0 flex-wrap gap-2">
              <router-link
                :to="{ name: 'reventa-publica', params: { id: publicacion.idPublicacionReventa } }"
                class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-semibold text-granate transition hover:bg-rosa/20"
              >
                Ver publicación
              </router-link>
              <template v-if="publicacion.estado === 'PUBLICADA'">
                <button
                  type="button"
                  class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-semibold text-granate transition hover:bg-rosa/20"
                  @click="abrirEdicion(publicacion)"
                >
                  Editar precio
                </button>
                <button
                  type="button"
                  class="rounded-lg border border-rojizo/40 px-3 py-1.5 text-xs font-semibold text-rojizo transition hover:bg-rojizo/10"
                  @click="pedirArchivar(publicacion)"
                >
                  Archivar
                </button>
              </template>
            </div>
          </article>
        </div>

        <div v-if="historial.length" class="mt-5">
          <button
            type="button"
            class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
            @click="mostrarHistorial = !mostrarHistorial"
          >
            {{ mostrarHistorial
              ? 'Ocultar historial'
              : `Ver historial (${historial.length})` }}
          </button>

          <div v-if="mostrarHistorial" class="mt-3 space-y-2">
            <article
              v-for="publicacion in historial"
              :key="publicacion.idPublicacionReventa"
              class="rounded-xl border border-borde/50 bg-superficie px-4 py-3"
            >
              <div class="flex flex-wrap items-center justify-between gap-2">
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <h4 class="truncate text-sm font-semibold text-granate">
                      {{ publicacion.titulo }}
                    </h4>
                    <span
                      class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                      :class="estadoClase(publicacion.estado)"
                    >
                      {{ estadoTexto(publicacion.estado) }}
                    </span>
                  </div>
                  <p class="text-xs text-cafe/55">
                    {{ publicacion.tituloOriginal || publicacion.nombreVariante }}
                  </p>
                </div>
                <p class="text-sm font-semibold text-granate">
                  {{ formatear(publicacion.precio, publicacion.moneda) }}
                </p>
              </div>

              <p v-if="publicacion.estado === 'VENDIDA'" class="mt-2 text-xs text-cafe/65">
                Vendida a @{{ publicacion.nombreComprador || 'un usuario' }}
                <template v-if="publicacion.fechaCierre">
                  el {{ fecha(publicacion.fechaCierre) }}
                </template>
                · Neto de la venta {{ formatear(publicacion.montoNeto, publicacion.moneda) }}
                <template v-if="publicacion.montoComision != null">
                  (comisión {{ formatear(publicacion.montoComision, publicacion.moneda) }})
                </template>.
                El arancel de MercadoPago se descuenta en tu cuenta.
              </p>
              <p v-else class="mt-2 text-xs text-cafe/55">
                {{ publicacion.estado === 'ARCHIVADA'
                  ? 'Publicación archivada'
                  : (publicacion.estado === 'SUSPENDIDA'
                    ? 'Suspendida por administración'
                    : 'Publicación cerrada') }}
                <template v-if="publicacion.fechaCierre">
                  el {{ fecha(publicacion.fechaCierre) }}
                </template>
              </p>
              <p v-if="publicacion.estado === 'VENDIDA'" class="mt-1 text-[11px] uppercase tracking-[0.12em] text-cafe/40">
                Comprobante de venta
              </p>
            </article>
          </div>
        </div>
      </section>
    </template>

    <ModalReventa
      :abierto="modalAbierto"
      :pieza="piezaSeleccionada"
      :publicacion="publicacionEditando"
      :comision-porcentaje="comisionPorcentaje"
      :arancel-porcentaje="arancelPorcentaje"
      @cerrar="cerrarModal"
      @guardado="alGuardar"
    />

    <ModalConfirmacion
      :abierto="!!publicacionArchivar"
      titulo="Archivar publicación"
      mensaje="La publicación se da de baja. La pieza vuelve a «Puedo revender» y el historial queda guardado."
      texto-confirmar="Archivar"
      texto-cancelar="Volver"
      destructivo
      @confirmar="confirmarArchivar"
      @cerrar="cerrarArchivar"
    >
      <p v-if="archivando" class="mt-3 text-sm text-cafe/60">Archivando...</p>
    </ModalConfirmacion>
  </div>
</template>
