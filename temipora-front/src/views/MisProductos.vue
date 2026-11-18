<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  estadoProductoDe,
  tieneLedger,
} from '../servicios/productos'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import PageHeader from '../components/PageHeader.vue'

const productos = ref([])
const cargando = ref(true)
const error = ref('')
const filtro = ref('disponibles')
const aviso = ref(null)
const dialogo = ref(null)
const procesandoDialogo = ref(false)
let temporizadorAviso = null

const disponibles = computed(
  () => productos.value.filter(
    (producto) => producto.estado === 'DISPONIBLE' && producto.disponible,
  ),
)
const agotados = computed(
  () => productos.value.filter(
    (producto) => producto.estado === 'DISPONIBLE' && !producto.disponible,
  ),
)
const pausados = computed(
  () => productos.value.filter((producto) => producto.estado === 'PAUSADO'),
)
const borradores = computed(
  () => productos.value.filter((producto) => producto.estado === 'BORRADOR'),
)
const productosFiltrados = computed(() => {
  if (filtro.value === 'pausados') return pausados.value
  if (filtro.value === 'borradores') return borradores.value
  if (filtro.value === 'agotados') return agotados.value
  return disponibles.value
})
const opcionesFiltro = computed(() => [
  { valor: 'disponibles', texto: 'Disponibles', cantidad: disponibles.value.length },
  { valor: 'agotados', texto: 'Sin stock', cantidad: agotados.value.length },
  { valor: 'borradores', texto: 'Borradores', cantidad: borradores.value.length },
  { valor: 'pausados', texto: 'Pausados', cantidad: pausados.value.length },
])

const estadoVacio = computed(() => {
  if (filtro.value === 'borradores') {
    return {
      titulo: 'No tenés borradores.',
      detalle: 'Los productos que guardes sin publicar aparecen acá.',
      accion: null,
    }
  }
  if (filtro.value === 'agotados') {
    return {
      titulo: 'No tenés productos sin stock.',
      detalle: 'Tus publicaciones disponibles todavía tienen unidades para vender.',
      accion: disponibles.value.length
        ? { texto: 'Ver disponibles', tipo: 'disponibles' }
        : null,
    }
  }
  if (filtro.value === 'pausados') {
    return {
      titulo: 'No tenés productos pausados.',
      detalle: 'Todos tus productos están disponibles.',
      accion: disponibles.value.length
        ? { texto: 'Ver disponibles', tipo: 'disponibles' }
        : null,
    }
  }
  return {
    titulo: 'No tenés productos disponibles.',
    detalle: agotados.value.length
      ? 'Revisá los que quedaron sin stock o los que pausaste.'
      : (pausados.value.length
        ? 'Revisá los que pausaste o creá uno nuevo.'
        : 'Creá tu primer producto para empezar a venderlo.'),
    accion: agotados.value.length
      ? { texto: 'Ver sin stock', tipo: 'agotados' }
      : (pausados.value.length
        ? { texto: 'Ver pausados', tipo: 'pausados' }
        : null),
  }
})

const tituloDialogo = computed(() => {
  const d = dialogo.value
  if (!d) return ''
  if (d.tipo === 'pausar') return `¿Pausar “${d.producto.titulo}”?`
  if (d.tipo === 'reactivar') return `¿Reactivar “${d.producto.titulo}”?`
  return `¿Dar de baja “${d.producto.titulo}”?`
})

const mensajeDialogo = computed(() => {
  const d = dialogo.value
  if (!d) return ''
  if (d.tipo === 'pausar') {
    return 'Dejará de aparecer en el catálogo y en tu perfil, pero vas a conservar sus variantes y su registro de autenticidad.'
  }
  if (d.tipo === 'reactivar') {
    return 'Volverá a aparecer en el catálogo y en tu perfil, y sus variantes disponibles podrán venderse.'
  }
  return 'El producto dejará de estar visible para vos y para los clientes. Las piezas únicas ya registradas conservan su historial en el ledger.'
})

const encabezadoDetalle = computed(() => {
  const d = dialogo.value
  if (!d || !d.resumen) return ''
  return d.tipo === 'archivar' ? 'Consecuencias' : 'Vas a conservar'
})

const detallesDialogo = computed(() => {
  const d = dialogo.value
  if (!d || !d.resumen) return []
  const r = d.resumen
  const lineas = []
  if (r.totalVariantes) {
    lineas.push(`${r.totalVariantes} variante(s) en total.`)
  }
  if (r.variantesDisponibles) {
    lineas.push(`${r.variantesDisponibles} variante(s) disponible(s) para vender.`)
  }
  if (r.variantesConLedger) {
    lineas.push(`${r.variantesConLedger} pieza(s) con registro génesis en el ledger.`)
  }
  if (d.tipo === 'archivar' && r.variantesConLedger) {
    lineas.push('El historial de las piezas únicas se conserva y sigue siendo verificable.')
  }
  if (!lineas.length) {
    lineas.push('No hay variantes cargadas.')
  }
  return lineas
})

const textoConfirmarDialogo = computed(() => {
  const d = dialogo.value
  if (!d) return 'Confirmar'
  if (d.tipo === 'pausar') return 'Pausar producto'
  if (d.tipo === 'reactivar') return 'Reactivar producto'
  return 'Dar de baja'
})

function mostrarAviso(tipo, texto) {
  aviso.value = { tipo, texto }
  clearTimeout(temporizadorAviso)
  temporizadorAviso = setTimeout(() => {
    aviso.value = null
  }, 4000)
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/mis-productos')
    productos.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar tus productos.'
  } finally {
    cargando.value = false
  }
}

function imagenDe(producto) {
  return producto.imagenPortada
    || producto.variantes?.find((variante) => variante.imagenUrl)?.imagenUrl
    || ''
}

function precioDe(producto) {
  return formatearPrecio(producto.precioDesde, producto.moneda || 'ARS')
}

function resumenVariantes(producto) {
  const total = producto.cantidadVariantes || 0
  const disponibles = producto.cantidadVariantesDisponibles || 0
  return `${total} variante${total === 1 ? '' : 's'} · ${disponibles} disponible${disponibles === 1 ? '' : 's'}`
}

async function abrirDialogo(tipo, producto) {
  if (tipo === 'reactivar') {
    dialogo.value = { tipo, producto }
    return
  }
  try {
    const res = await api.get(`/mis-productos/${producto.idProducto}/resumen`)
    dialogo.value = { tipo, producto, resumen: res.data }
  } catch (e) {
    mostrarAviso('error', e.response?.data?.message || 'No se pudo consultar el estado.')
  }
}

function cerrarDialogo() {
  if (procesandoDialogo.value) return
  dialogo.value = null
}

async function confirmarDialogo() {
  const d = dialogo.value
  if (!d || procesandoDialogo.value) return
  procesandoDialogo.value = true
  try {
    const res = await api.post(`/mis-productos/${d.producto.idProducto}/${d.tipo}`)
    if (d.tipo === 'archivar') {
      productos.value = productos.value.filter(
        (producto) => producto.idProducto !== d.producto.idProducto,
      )
      mostrarAviso('exito', `“${d.producto.titulo}” se dio de baja.`)
    } else {
      d.producto.estado = res.data.estado
      mostrarAviso(
        'exito',
        d.tipo === 'pausar'
          ? `“${d.producto.titulo}” quedó pausado.`
          : `“${d.producto.titulo}” se reactivó.`,
      )
    }
    dialogo.value = null
  } catch (e) {
    mostrarAviso('error', e.response?.data?.message || 'No se pudo actualizar el estado.')
  } finally {
    procesandoDialogo.value = false
  }
}

function ejecutarAccionVacio() {
  const accion = estadoVacio.value.accion
  if (accion) filtro.value = accion.tipo
}

onMounted(cargar)
onBeforeUnmount(() => clearTimeout(temporizadorAviso))
</script>

<template>
  <div>
    <PageHeader
      eyebrow="Tu espacio de trabajo"
      titulo="Mis productos"
      descripcion="Las publicaciones y variantes que ofrecés a tus clientes."
      :nivel="2"
    >
      <template #acciones>
        <router-link
          to="/panel/productos/crear"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
        >
          Crear producto
        </router-link>
      </template>
    </PageHeader>

    <p v-if="cargando" class="mt-6 py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="mt-6 py-10 text-center text-rojizo">{{ error }}</p>

    <template v-else>
      <div
        v-if="aviso"
        class="mb-5 flex items-start justify-between gap-3 rounded-xl border px-4 py-3 text-sm"
        :class="aviso.tipo === 'error'
          ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
          : 'border-green-600/20 bg-green-500/10 text-green-700'"
        role="status"
      >
        <p>{{ aviso.texto }}</p>
        <button type="button" class="shrink-0 opacity-60 hover:opacity-100" aria-label="Cerrar aviso" @click="aviso = null">
          ×
        </button>
      </div>

      <p
        v-if="!productos.length"
        class="rounded-2xl border border-dashed border-rosa/60 px-4 py-12 text-center text-sm text-cafe/50"
      >
        Todavía no creaste productos. Empezá con “Crear producto”.
      </p>

      <template v-else>
        <div class="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div
            class="flex w-full rounded-xl border border-rosa/60 bg-blanco p-1 sm:inline-flex sm:w-fit"
            role="group"
            aria-label="Filtrar productos por estado"
          >
            <button
              v-for="opcion in opcionesFiltro"
              :key="opcion.valor"
              type="button"
              class="flex flex-1 items-center justify-center gap-2 whitespace-nowrap rounded-lg px-2 py-1.5 text-xs font-medium transition sm:flex-none sm:px-3 sm:text-sm"
              :class="filtro === opcion.valor
                ? 'bg-coral text-white shadow-sm'
                : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
              :aria-pressed="filtro === opcion.valor"
              @click="filtro = opcion.valor"
            >
              {{ opcion.texto }}
              <span
                class="rounded-full px-1.5 py-0.5 text-[11px] leading-none"
                :class="filtro === opcion.valor
                  ? 'bg-white/20 text-white'
                  : 'bg-rosa/30 text-granate'"
              >
                {{ opcion.cantidad }}
              </span>
            </button>
          </div>
          <p class="text-xs text-cafe/50">
            {{ productosFiltrados.length }} de {{ productos.length }}
            {{ productos.length === 1 ? 'producto' : 'productos' }}
          </p>
        </div>

        <div
          v-if="!productosFiltrados.length"
          class="rounded-2xl border border-dashed border-rosa/60 px-4 py-12 text-center"
        >
          <p class="text-sm font-medium text-granate">{{ estadoVacio.titulo }}</p>
          <p class="mt-1 text-sm text-cafe/50">{{ estadoVacio.detalle }}</p>
          <button
            v-if="estadoVacio.accion"
            type="button"
            class="mt-4 text-sm font-medium text-coral transition hover:text-rojizo"
            @click="ejecutarAccionVacio"
          >
            {{ estadoVacio.accion.texto }}
          </button>
        </div>

        <div v-else class="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <article
            v-for="producto in productosFiltrados"
            :key="producto.idProducto"
            class="flex flex-col overflow-hidden rounded-2xl border border-rosa/50 bg-blanco shadow-sm"
          >
            <div class="relative h-40 bg-rosa/20">
              <img
                v-if="imagenDe(producto)"
                :src="imagenDe(producto)"
                :alt="producto.titulo"
                class="h-full w-full object-cover"
              />
              <span
                class="absolute right-3 top-3 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold backdrop-blur-sm"
                :class="estadoProductoDe(producto.estado).clase"
              >
                {{ estadoProductoDe(producto.estado).texto }}
              </span>
              <span
                v-if="!producto.disponible"
                class="absolute bottom-3 left-3 rounded-full bg-superficie/95 px-2.5 py-0.5 text-[11px] font-bold text-granate ring-1 ring-borde/70"
              >
                Sin stock
              </span>
            </div>

            <div class="flex flex-1 flex-col p-4">
              <h2 class="line-clamp-1 font-display text-base font-bold text-granate">
                {{ producto.titulo || 'Borrador sin título' }}
              </h2>
              <p class="mt-1 text-xs text-cafe/60">
                <template v-if="producto.estado === 'BORRADOR'">
                  Sin publicar
                </template>
                <template v-else>
                  {{ producto.nombreTipoProducto }} · {{ producto.nombreMedio }}
                </template>
              </p>
              <p class="mt-2 line-clamp-2 text-sm leading-relaxed text-cafe/70">
                {{ producto.descripcion }}
              </p>

              <div class="mt-3 flex flex-wrap items-center gap-2 text-xs">
                <span class="rounded-full bg-crema px-2.5 py-0.5 font-semibold text-cafe">
                  {{ resumenVariantes(producto) }}
                </span>
                <span
                  v-if="tieneLedger(producto)"
                  class="rounded-full bg-mantequilla/60 px-2.5 py-0.5 font-bold text-granate"
                  title="Las piezas únicas tienen un evento génesis verificable"
                >
                  Registro de autenticidad
                </span>
                <span
                  v-if="producto.permiteReventa"
                  class="rounded-full bg-rosa/30 px-2.5 py-0.5 font-semibold text-granate"
                >
                  Reventa
                </span>
              </div>

              <p class="mt-4 font-display text-lg font-extrabold text-coral">
                <span class="mr-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-cafe/45">Desde</span>
                {{ precioDe(producto) }}
              </p>

              <div class="mt-auto flex flex-wrap items-center justify-end gap-x-4 gap-y-2 border-t border-rosa/40 pt-4">
                <template v-if="producto.estado === 'BORRADOR'">
                  <button
                    type="button"
                    class="text-sm text-rojizo/80 transition hover:text-rojizo"
                    @click="abrirDialogo('archivar', producto)"
                  >
                    Descartar
                  </button>
                  <router-link
                    :to="`/panel/productos/${producto.idProducto}/editar`"
                    class="rounded-lg bg-coral px-3.5 py-1.5 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo"
                  >
                    Continuar
                  </router-link>
                </template>
                <template v-else>
                  <router-link
                    :to="`/panel/productos/${producto.idProducto}`"
                    class="text-sm text-cafe/60 transition hover:text-granate"
                  >
                    Vista previa
                  </router-link>
                  <router-link
                    :to="`/panel/productos/${producto.idProducto}/editar`"
                    class="text-sm text-coral transition hover:text-rojizo"
                  >
                    Editar
                  </router-link>
                  <button
                    type="button"
                    class="text-sm text-cafe/60 transition hover:text-granate"
                    @click="abrirDialogo(producto.estado === 'DISPONIBLE' ? 'pausar' : 'reactivar', producto)"
                  >
                    {{ producto.estado === 'DISPONIBLE' ? 'Pausar' : 'Reactivar' }}
                  </button>
                  <button
                    type="button"
                    class="text-sm text-rojizo/80 transition hover:text-rojizo"
                    @click="abrirDialogo('archivar', producto)"
                  >
                    Dar de baja
                  </button>
                </template>
              </div>
            </div>
          </article>
        </div>
      </template>
    </template>

    <ModalConfirmacion
      :abierto="!!dialogo"
      :titulo="tituloDialogo"
      :mensaje="mensajeDialogo"
      :texto-confirmar="textoConfirmarDialogo"
      :destructivo="dialogo?.tipo === 'archivar'"
      @confirmar="confirmarDialogo"
      @cerrar="cerrarDialogo"
    >
      <div
        v-if="encabezadoDetalle"
        class="mt-4 rounded-xl border border-rosa/50 bg-crema/60 p-3"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/55">
          {{ encabezadoDetalle }}
        </p>
        <ul class="mt-2 space-y-1 text-sm leading-relaxed text-cafe/75">
          <li v-for="detalle in detallesDialogo" :key="detalle" class="flex gap-2">
            <span aria-hidden="true">•</span>
            <span>{{ detalle }}</span>
          </li>
        </ul>
      </div>
    </ModalConfirmacion>
  </div>
</template>
