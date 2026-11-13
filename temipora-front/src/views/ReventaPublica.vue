<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cargarReventa } from '../servicios/reventas'
import { formatearPrecio } from '../servicios/moneda'
import { useCarrito } from '../composables/useCarrito'
import { useNotificaciones } from '../composables/useNotificaciones'
import ResumenPropiedad from '../components/ResumenPropiedad.vue'
import HistorialLedgerModal from '../components/HistorialLedgerModal.vue'
import ListaResenias from '../components/ListaResenias.vue'
import { cargarReseniasProducto } from '../servicios/resenias'

const props = defineProps({
  id: { type: Number, required: true },
})

const route = useRoute()
const router = useRouter()
const { agregar } = useCarrito()
const { error: notificarError } = useNotificaciones()

const reventa = ref(null)
const cargando = ref(true)
const error = ref('')
const agregando = ref(false)
const revelado = ref(false)
const historialAbierto = ref(null)
const reseniasProducto = ref([])
const resumenReseniasProducto = ref(null)
const paginaReseniasProducto = ref(null)
const cargandoReseniasProducto = ref(false)
const errorReseniasProducto = ref('')
const filtrosReseniasProducto = ref({ calificacion: '', orden: 'RECIENTES' })

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const titulo = computed(() => reventa.value?.titulo || '')
const estado = computed(() => reventa.value?.estado || '')
const disponible = computed(() => estado.value === 'PUBLICADA')
const esPropia = computed(() => !!reventa.value?.esPropia)
const puedeComprar = computed(() => disponible.value && !esPropia.value)
const requiereAdvertencia = computed(
  () => !!reventa.value?.contenidoSensible && !revelado.value,
)
const tiposSensibles = computed(() => (reventa.value?.contenidosSensibles || [])
  .map((tipo) => ETIQUETAS_SENSIBLE[tipo] || tipo)
  .join(' · '))
const estadoMensaje = computed(() => {
  if (esPropia.value) return 'Esta es tu publicación.'
  if (estado.value === 'RESERVADA') {
    return 'Hay una compra en curso. Si la reserva vence, la pieza vuelve a estar disponible.'
  }
  if (estado.value === 'VENDIDA') return 'Esta pieza ya fue vendida.'
  if (estado.value === 'PUBLICADA') return ''
  return 'Esta publicación ya no está disponible.'
})

const ESTADOS_REVENTA = {
  PUBLICADA: 'En reventa',
  RESERVADA: 'Compra en curso',
  VENDIDA: 'Vendida',
  ARCHIVADA: 'No disponible',
  SUSPENDIDA: 'Suspendida',
  VENCIDA: 'Vencida',
}
const estadoReventa = computed(
  () => ESTADOS_REVENTA[estado.value] || estado.value,
)

function formatear(valor) {
  return valor != null
    ? formatearPrecio(valor, reventa.value?.moneda || 'ARS')
    : '—'
}

async function cargarResenias() {
  const idProducto = reventa.value?.idProducto
  if (!idProducto) {
    reseniasProducto.value = []
    resumenReseniasProducto.value = null
    paginaReseniasProducto.value = null
    return
  }
  cargandoReseniasProducto.value = true
  errorReseniasProducto.value = ''
  try {
    const { data } = await cargarReseniasProducto(idProducto, {
      calificacion: filtrosReseniasProducto.value.calificacion || undefined,
      orden: filtrosReseniasProducto.value.orden || 'RECIENTES',
      pagina: 0,
      tamanio: 5,
    })
    reseniasProducto.value = data?.pagina?.contenido || []
    paginaReseniasProducto.value = data?.pagina || null
    resumenReseniasProducto.value = data?.resumen || null
  } catch (e) {
    errorReseniasProducto.value = e.response?.data?.message
      || 'No se pudieron cargar las reseñas del producto.'
  } finally {
    cargandoReseniasProducto.value = false
  }
}

function cambiarFiltrosResenias(nuevos) {
  filtrosReseniasProducto.value = nuevos
  void cargarResenias()
}

async function cargar() {
  cargando.value = true
  error.value = ''
  revelado.value = false
  try {
    reventa.value = await cargarReventa(props.id)
    await cargarResenias()
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la publicación de reventa.'
  } finally {
    cargando.value = false
  }
}

function volver() {
  if (window.history.state && window.history.state.back) {
    router.back()
    return
  }
  router.push('/productos')
}

async function comprar() {
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!puedeComprar.value || agregando.value) return
  agregando.value = true
  try {
    await agregar(
      reventa.value.idVariante,
      1,
      reventa.value.idPublicacionReventa,
    )
    router.push('/carrito')
  } catch (e) {
    notificarError(e.response?.data?.message
      || 'No se pudo agregar la pieza al carrito.')
  } finally {
    agregando.value = false
  }
}

onMounted(cargar)
watch(() => props.id, cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-screen-xl px-4 py-8 sm:px-8">
    <button
      type="button"
      class="inline-flex items-center gap-1.5 text-sm font-medium text-cafe/60 transition hover:text-granate"
      @click="volver"
    >
      ← Volver
    </button>

    <p v-if="cargando" class="py-16 text-center text-cafe/60">Cargando publicación...</p>
    <p v-else-if="error" role="alert" class="py-16 text-center text-rojizo">{{ error }}</p>

    <div v-else-if="reventa" class="mt-5 grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
      <section class="space-y-4">
        <div class="relative overflow-hidden rounded-[22px] border border-borde/60 bg-rosa/20 shadow-sm">
          <img
            v-if="reventa.imagenUrl"
            :src="reventa.imagenUrl"
            :alt="titulo"
            class="aspect-square w-full object-cover"
            :class="requiereAdvertencia ? 'scale-105 blur-[18px]' : ''"
          />
          <div
            v-else
            class="flex aspect-square items-center justify-center text-sm text-cafe/50"
          >
            Sin imagen
          </div>

          <span
            v-if="requiereAdvertencia"
            class="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-granate/30 px-6 text-center text-white"
          >
            <span class="text-sm font-medium">
              Contenido sensible{{ tiposSensibles ? `: ${tiposSensibles}` : '' }}
            </span>
            <button
              type="button"
              class="rounded-full bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-crema"
              @click="revelado = true"
            >
              Ver de todos modos
            </button>
          </span>
        </div>
      </section>

      <section class="space-y-5">
        <div>
          <div class="flex flex-wrap items-center gap-2">
            <span class="rounded-full bg-coral px-3 py-1 text-[11px] font-bold uppercase tracking-[0.12em] text-white">
              Reventa
            </span>
            <span class="rounded-full bg-mantequilla/60 px-3 py-1 text-[11px] font-bold text-granate">
              Origen registrado
            </span>
            <span
              v-if="estado !== 'PUBLICADA'"
              class="rounded-full bg-cafe/10 px-3 py-1 text-[11px] font-semibold text-cafe/70"
            >
              {{ estado.charAt(0) + estado.slice(1).toLowerCase() }}
            </span>
          </div>

          <h1 class="mt-3 font-display text-3xl font-extrabold text-granate">
            {{ titulo }}
          </h1>
          <p v-if="reventa.tituloOriginal" class="mt-1 text-sm text-cafe/60">
            Publicación original: {{ reventa.tituloOriginal }}
          </p>
        </div>

        <div class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <p class="font-display text-3xl font-extrabold text-coral">
              {{ formatear(reventa.precio) }}
            </p>
            <span class="rounded-full bg-crema px-3 py-1 text-xs font-semibold text-cafe">
              Pago único
            </span>
          </div>

          <p class="mt-3 text-sm leading-relaxed text-cafe/70">
            El pago se acredita a
            <strong class="text-granate">@{{ reventa.nombreVendedor }}</strong>,
            el revendedor de esta pieza. Temipora procesa el cobro a través de
            MercadoPago.
          </p>
          <p v-if="reventa.tipoEntrega" class="mt-2 text-sm leading-relaxed text-cafe/60">
            {{ reventa.tipoEntrega === 'DIGITAL'
              ? 'Entrega digital: al confirmarse el pago vas a poder descargar el archivo original.'
              : 'Pieza física: el envío y la entrega se coordinan con el revendedor.' }}
          </p>

          <template v-if="puedeComprar">
            <button
              type="button"
              :disabled="agregando"
              class="mt-5 flex min-h-11 w-full items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="comprar"
            >
              {{ agregando ? 'Agregando...' : 'Comprar esta pieza' }}
            </button>
            <p class="mt-2 text-center text-xs text-cafe/50">
              Te redirigimos al checkout de MercadoPago para completar el pago.
            </p>
          </template>

          <p
            v-else
            class="mt-5 rounded-xl bg-rosa/20 px-4 py-3 text-sm text-cafe/75"
          >
            {{ estadoMensaje }}
            <router-link
              v-if="esPropia"
              to="/reventa"
              class="ml-1 font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
            >
              Gestionar mi publicación →
            </router-link>
          </p>
        </div>

        <ResumenPropiedad
          v-if="reventa.ledger?.tieneLedger"
          :nombre="reventa.ledger.nombre || reventa.nombreVariante"
          :propietario="reventa.nombreVendedor"
          :estado="estadoReventa"
          :eventos="reventa.ledger.eventos"
          :integridad="reventa.ledger.integridad"
          :anclado="reventa.ledger.anclado"
          :bitcoin-altura="reventa.ledger.bitcoinAltura"
          :fecha-anclaje="reventa.ledger.fechaAnclaje"
          :codigo-certificado="reventa.ledger.codigoCertificado"
          @ver-historial="historialAbierto = reventa.ledger"
        />

        <div class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm">
          <h2 class="font-display text-base font-bold text-granate">Autoría</h2>
          <router-link
            v-if="reventa.nombreArtista"
            :to="`/perfil/${reventa.nombreArtista}`"
            class="mt-3 inline-flex items-center gap-2"
          >
            <img
              v-if="reventa.urlFotoArtista"
              :src="reventa.urlFotoArtista"
              alt=""
              class="h-8 w-8 rounded-full object-cover ring-1 ring-borde/50"
            />
            <span
              v-else
              class="flex h-8 w-8 items-center justify-center rounded-full bg-rosa/40 text-xs font-bold text-granate"
            >
              {{ (reventa.nombreArtista || '?').charAt(0).toUpperCase() }}
            </span>
            <span class="text-sm text-cafe/75 transition hover:text-coral">
              Obra original de
              <strong class="text-granate">
                {{ reventa.nombreDisplayArtista || `@${reventa.nombreArtista}` }}
              </strong>
            </span>
          </router-link>
          <p class="mt-3 text-sm text-cafe/55">
            Publicada en reventa por
            <router-link
              :to="`/perfil/${reventa.nombreVendedor}`"
              class="font-medium text-cafe/75 underline decoration-cafe/30 underline-offset-2 transition hover:text-coral"
            >
              @{{ reventa.nombreVendedor }}
            </router-link>.
          </p>
        </div>

        <div v-if="reventa.descripcion" class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm">
          <h2 class="font-display text-base font-bold text-granate">Sobre la pieza</h2>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-cafe/75">
            {{ reventa.descripcion }}
          </p>
        </div>

        <div v-if="reventa.licencia" class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm">
          <h2 class="font-display text-base font-bold text-granate">
            Licencia de la obra
          </h2>
          <p class="mt-1 text-xs text-cafe/50">
            Versión {{ reventa.licencia.version }} · la licencia viaja con la pieza
          </p>
          <ul v-if="reventa.licencia.derechos?.length" class="mt-3 space-y-2">
            <li
              v-for="derecho in reventa.licencia.derechos"
              :key="derecho.codigo || derecho.nombre"
              class="flex gap-2 text-sm text-cafe/75"
            >
              <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-coral"></span>
              <span>
                <strong class="text-granate">{{ derecho.nombre }}</strong>
                <span v-if="derecho.descripcion" class="text-cafe/60">
                  · {{ derecho.descripcion }}
                </span>
              </span>
            </li>
          </ul>
          <p class="mt-3 text-sm text-cafe/70">
            {{ reventa.licencia.permiteModificaciones
              ? 'Permite modificaciones.'
              : 'No permite modificaciones.' }}
            {{ reventa.licencia.requiereCredito
              ? 'Requiere crédito al artista.'
              : 'No requiere crédito al artista.' }}
          </p>
          <p v-if="reventa.licencia.textoAdicional" class="mt-2 text-sm italic leading-relaxed text-cafe/60">
            {{ reventa.licencia.textoAdicional }}
          </p>
        </div>
      </section>
    </div>

    <section v-if="reventa" id="resenias" class="mt-8 border-t border-borde/50 pt-6">
      <h2 class="font-display text-2xl font-bold text-granate">
        Reseñas del producto
      </h2>
      <p class="mt-1 text-sm text-cafe/55">
        Opiniones de personas que compraron esta pieza.
      </p>
      <div class="mt-5">
        <ListaResenias
          :resenias="reseniasProducto"
          :resumen="resumenReseniasProducto"
          :pagina="paginaReseniasProducto"
          :cargando="cargandoReseniasProducto"
          :error="errorReseniasProducto"
          :filtros="filtrosReseniasProducto"
          @cambiar-filtros="cambiarFiltrosResenias"
        />
      </div>
    </section>

    <HistorialLedgerModal
      :abierto="!!historialAbierto"
      :titulo="historialAbierto?.nombre || reventa?.nombreVariante || 'Historial de la pieza'"
      :eventos="historialAbierto?.eventos || []"
      :integridad="historialAbierto?.integridad"
      :mensaje="historialAbierto?.mensaje"
      @cerrar="historialAbierto = null"
    />
  </div>
</template>
