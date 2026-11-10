<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { cargarCatalogoProductos } from '../servicios/productos'
import { formatearPrecio } from '../servicios/moneda'
import { formatearPromedio } from '../servicios/resenias'

const route = useRoute()
const router = useRouter()

const LIMITE = 8
const MAX_CHIPS = 6

const servicios = ref([])
const productos = ref([])
const tiposProducto = ref([])
const personalizado = ref(false)
const cargando = ref(true)
const error = ref('')

const favoritosServicios = ref(new Set())
const favoritosProductos = ref(new Set())
const actualizando = ref(new Set())

const nombreUsuario = computed(() => localStorage.getItem('nombreUsuario') || '')

const heroServicio = computed(() => servicios.value[0] || null)
const heroMiniServicios = computed(() => servicios.value.slice(1, 2))
const heroMiniProducto = computed(() => productos.value[0] || null)
const tieneHero = computed(
  () => !!heroServicio.value || !!heroMiniProducto.value,
)

const serviciosGrilla = computed(() => (
  tieneHero.value ? servicios.value.slice(2) : servicios.value
))
const productosGrilla = computed(() => (
  tieneHero.value ? productos.value.slice(1) : productos.value
))

const chips = computed(() => tiposProducto.value.slice(0, MAX_CHIPS))

// Carrusel de servicios: una sola fila con flechas en desktop.
// En mobile el scroll es táctil y las flechas se ocultan.
const carruselServicios = ref(null)
const puedeIzquierda = ref(false)
const puedeDerecha = ref(false)

function actualizarFlechas() {
  const el = carruselServicios.value
  if (!el) {
    puedeIzquierda.value = false
    puedeDerecha.value = false
    return
  }
  puedeIzquierda.value = el.scrollLeft > 4
  puedeDerecha.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 4
}

function desplazarServicios(direccion) {
  const el = carruselServicios.value
  if (!el) return
  const paso = Math.max(el.clientWidth * 0.8, 240)
  el.scrollBy({ left: paso * direccion, behavior: 'smooth' })
}

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

function haySesion() {
  return !!localStorage.getItem('token')
}

function textoEntrega(servicio) {
  if (servicio.tiempoEntregaValor == null || !servicio.tiempoEntregaUnidad) {
    return 'A convenir'
  }
  return `${servicio.tiempoEntregaValor} ${servicio.tiempoEntregaUnidad.toLowerCase()}`
}

function precioServicio(servicio) {
  return formatearPrecio(
    servicio.precioBaseMostrado ?? servicio.precioBase,
    servicio.monedaMostrada ?? servicio.moneda,
  )
}

function precioProducto(producto) {
  return formatearPrecio(producto.precioDesde, producto.moneda || 'ARS')
}

function ratingServicio(servicio) {
  if (!servicio.cantidadResenias) return 'Nuevo'
  return `★ ${formatearPromedio(servicio.calificacionPromedio)} (${servicio.cantidadResenias})`
}

function tiposSensibles(contenidos) {
  return (contenidos || []).map((t) => ETIQUETAS_SENSIBLE[t] || t).join(' · ')
}

function descripcionAccesibleSensible(item) {
  const tipos = tiposSensibles(item?.contenidosSensibles)
  return tipos ? `Contenido sensible: ${tipos}` : 'Contenido sensible'
}

function irAPerfil(nombre) {
  if (!nombre) return
  router.push({ name: 'perfil', params: { nombreUsuario: nombre } })
}

function irAServicio(id) {
  router.push({ name: 'servicio-detalle', params: { id } })
}

function irAProducto(id) {
  router.push({ name: 'producto-publico', params: { id } })
}

function irAServicios() {
  router.push({ name: 'servicios' })
}

function irAProductos() {
  router.push({ name: 'productos' })
}

function filtrarPorTipo(tipo) {
  router.push({
    name: 'productos',
    query: { idTipoProducto: String(tipo.idTipoProducto) },
  })
}

async function cargar() {
  cargando.value = true
  error.value = ''
  const pedidos = [
    api.get('/servicios', {
      params: { orden: 'RELEVANCIA', pagina: 0, tamanio: LIMITE },
    }),
    cargarCatalogoProductos({ orden: 'RELEVANCIA', pagina: 0, tamanio: LIMITE }),
    api.get('/tipos-producto'),
  ]
  const [serviciosRes, productosRes, tiposRes] =
    await Promise.allSettled(pedidos)

  if (serviciosRes.status === 'fulfilled') {
    servicios.value = serviciosRes.value.data?.contenido || []
    personalizado.value = !!serviciosRes.value.data?.personalizado
  } else {
    servicios.value = []
  }

  if (productosRes.status === 'fulfilled') {
    productos.value = productosRes.value.data?.contenido || []
  } else {
    productos.value = []
  }

  tiposProducto.value = tiposRes.status === 'fulfilled'
    ? (tiposRes.value.data || [])
    : []

  if (serviciosRes.status === 'rejected' && productosRes.status === 'rejected') {
    error.value = 'No pudimos cargar el contenido. Probá de nuevo más tarde.'
  }
  cargando.value = false
}

async function cargarFavoritos() {
  if (!haySesion()) return
  const [serviciosRes, productosRes] = await Promise.allSettled([
    api.get('/favoritos/servicios/ids'),
    api.get('/favoritos/productos/ids'),
  ])
  favoritosServicios.value = serviciosRes.status === 'fulfilled'
    ? new Set(serviciosRes.value.data || [])
    : new Set()
  favoritosProductos.value = productosRes.status === 'fulfilled'
    ? new Set(productosRes.value.data || [])
    : new Set()
}

function clave(tipo, id) {
  return `${tipo}-${id}`
}

async function alternarFavorito(tipo, item) {
  if (!haySesion()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const esServicio = tipo === 'servicio'
  const id = esServicio ? item.idServicio : item.idProducto
  const marca = clave(tipo, id)
  if (actualizando.value.has(marca)) return

  actualizando.value = new Set(actualizando.value).add(marca)
  const favoritos = esServicio ? favoritosServicios : favoritosProductos
  try {
    if (favoritos.value.has(id)) {
      await api.delete(`/favoritos/${esServicio ? 'servicios' : 'productos'}/${id}`)
      const siguientes = new Set(favoritos.value)
      siguientes.delete(id)
      favoritos.value = siguientes
    } else {
      await api.post(`/favoritos/${esServicio ? 'servicios' : 'productos'}/${id}`)
      favoritos.value = new Set(favoritos.value).add(id)
    }
  } catch {
    // Se mantiene el estado anterior si la llamada falla.
  } finally {
    const siguientes = new Set(actualizando.value)
    siguientes.delete(marca)
    actualizando.value = siguientes
  }
}

onMounted(() => {
  cargarFavoritos()
  cargar()
})

watch(serviciosGrilla, async () => {
  await nextTick()
  actualizarFlechas()
})
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 pb-16 pt-8 sm:px-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="font-display text-3xl font-extrabold tracking-tight text-granate sm:text-4xl">
          <template v-if="nombreUsuario">Hola, @{{ nombreUsuario }}</template>
          <template v-else>Descubrí lo que se está creando</template>
        </h1>
        <p class="mt-2 max-w-2xl text-sm text-cafe/65">
          Servicios a medida y piezas listas para llevar, elegidos para vos según
          lo que guardaste y visitaste.
        </p>
      </div>
      <router-link
        to="/servicios"
        class="hidden rounded-full border border-rosa px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20 sm:inline-flex"
      >
        Buscar un servicio
      </router-link>
    </header>

    <p v-if="cargando" class="py-24 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" role="alert" class="py-24 text-center text-rojizo">{{ error }}</p>

    <template v-else>
      <!-- Hero: una pieza grande y dos miniaturas -->
      <section v-if="tieneHero" class="mt-8 grid gap-4 lg:grid-cols-[2fr_1fr]" aria-label="Destacados">
        <article
          v-if="heroServicio"
          class="group relative flex min-h-[320px] cursor-pointer items-end overflow-hidden rounded-3xl border border-borde/60 bg-superficie shadow-sm sm:min-h-[440px]"
          @click="irAServicio(heroServicio.idServicio)"
        >
          <img
            v-if="heroServicio.imagenPortada"
            :src="heroServicio.imagenPortada"
            :alt="heroServicio.nombre"
            class="absolute inset-0 h-full w-full object-cover transition duration-500 group-hover:scale-[1.03]"
          />
          <div
            class="absolute inset-0 bg-gradient-to-t from-granate via-granate/55 to-transparent"
            aria-hidden="true"
          ></div>
          <div class="relative w-full p-6 sm:p-9">
            <span
              v-if="personalizado"
              class="mb-3 inline-flex items-center gap-1.5 rounded-full border border-inverso-suave/40 bg-granate/70 px-3 py-1 text-xs font-semibold text-inverso-contraste backdrop-blur-sm"
            >
              <span aria-hidden="true">✦</span>
              Elegido para vos
            </span>
            <h2 class="font-display text-2xl font-extrabold leading-tight text-inverso-contraste sm:text-4xl">
              {{ heroServicio.nombre }}
            </h2>
            <div class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-inverso-suave">
              <span>@{{ heroServicio.nombreArtista }}</span>
              <span>{{ ratingServicio(heroServicio) }}</span>
              <span>{{ textoEntrega(heroServicio) }}</span>
            </div>
            <span
              class="mt-5 inline-flex items-center gap-2 rounded-full bg-coral px-5 py-2.5 text-sm font-bold text-white shadow-sm transition group-hover:bg-rojizo"
            >
              Ver servicio
              <span aria-hidden="true">→</span>
            </span>
          </div>
        </article>

        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-1">
          <article
            v-for="mini in heroMiniServicios"
            :key="`mini-s-${mini.idServicio}`"
            class="group relative flex min-h-[200px] cursor-pointer items-end overflow-hidden rounded-2xl border border-borde/60 bg-superficie shadow-sm"
            @click="irAServicio(mini.idServicio)"
          >
            <img
              v-if="mini.imagenPortada"
              :src="mini.imagenPortada"
              :alt="mini.nombre"
              class="absolute inset-0 h-full w-full object-cover transition duration-500 group-hover:scale-[1.03]"
            />
            <div
              class="absolute inset-0 bg-gradient-to-t from-granate via-granate/50 to-transparent"
              aria-hidden="true"
            ></div>
            <div class="relative w-full p-5">
              <span
                class="mb-2 inline-flex rounded-full bg-granate/70 px-2.5 py-0.5 text-[11px] font-semibold text-inverso-contraste backdrop-blur-sm"
              >
                Servicio destacado
              </span>
              <h3 class="font-display text-lg font-bold leading-snug text-inverso-contraste">
                {{ mini.nombre }}
              </h3>
              <p class="mt-1 text-xs text-inverso-suave">
                Desde {{ precioServicio(mini) }}
              </p>
            </div>
          </article>

          <article
            v-if="heroMiniProducto"
            class="group relative flex min-h-[200px] cursor-pointer items-end overflow-hidden rounded-2xl border border-borde/60 bg-superficie shadow-sm"
            @click="irAProducto(heroMiniProducto.idProducto)"
          >
            <img
              v-if="heroMiniProducto.imagenPortada"
              :src="heroMiniProducto.imagenPortada"
              :alt="heroMiniProducto.titulo"
              class="absolute inset-0 h-full w-full object-cover transition duration-500 group-hover:scale-[1.03]"
            />
            <div
              class="absolute inset-0 bg-gradient-to-t from-granate via-granate/50 to-transparent"
              aria-hidden="true"
            ></div>
            <div class="relative w-full p-5">
              <span
                class="mb-2 inline-flex rounded-full bg-granate/70 px-2.5 py-0.5 text-[11px] font-semibold text-inverso-contraste backdrop-blur-sm"
              >
                Recién publicado
              </span>
              <h3 class="font-display text-lg font-bold leading-snug text-inverso-contraste">
                {{ heroMiniProducto.titulo }}
              </h3>
              <p class="mt-1 text-xs text-inverso-suave">
                {{ precioProducto(heroMiniProducto) }}
                <template v-if="heroMiniProducto.tipoEntrega === 'DIGITAL'">
                  · Descarga inmediata
                </template>
              </p>
            </div>
          </article>
        </div>
      </section>

      <!-- Chips: acceso rápido al catálogo de productos por tipo -->
      <div
        v-if="chips.length"
        class="mt-8 flex flex-wrap items-center gap-2"
        role="group"
        aria-label="Explorar por tipo de producto"
      >
        <button
          type="button"
          class="inline-flex min-h-10 items-center rounded-full bg-acento px-4 text-sm font-bold text-acento-contraste transition hover:opacity-90"
          @click="irAProductos"
        >
          Todo
        </button>
        <button
          v-for="tipo in chips"
          :key="tipo.idTipoProducto"
          type="button"
          class="inline-flex min-h-10 items-center rounded-full border border-borde/70 bg-superficie px-4 text-sm text-texto transition hover:border-acento/40 hover:bg-superficie-alt"
          @click="filtrarPorTipo(tipo)"
        >
          {{ tipo.nombre }}
        </button>
        <button
          type="button"
          class="ml-auto hidden min-h-10 items-center rounded-full border border-dashed border-borde px-4 text-sm font-semibold text-acento transition hover:bg-superficie-alt sm:inline-flex"
          @click="irAServicios"
        >
          Ver servicios →
        </button>
      </div>

      <!-- Servicios -->
      <section v-if="serviciosGrilla.length" class="mt-12" aria-labelledby="titulo-servicios">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 id="titulo-servicios" class="font-display text-2xl font-extrabold tracking-tight text-granate">
              Comisiones abiertas
            </h2>
            <p class="mt-1 text-sm text-cafe/60">
              Encargá una pieza hecha a tu medida.
            </p>
          </div>
        </div>

        <div class="relative mt-5">
          <button
            v-if="puedeIzquierda"
            type="button"
            class="absolute -left-3 top-1/2 z-10 hidden h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full border border-borde/60 bg-superficie text-granate shadow-md transition hover:bg-superficie-alt focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-acento/60 lg:flex"
            aria-label="Ver servicios anteriores"
            @click="desplazarServicios(-1)"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m15 18-6-6 6-6" />
            </svg>
          </button>

          <div
            ref="carruselServicios"
            class="-mx-1 flex snap-x snap-mandatory gap-5 overflow-x-auto px-1 pb-3 lg:scroll-smooth lg:pb-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
            role="list"
            aria-label="Servicios destacados"
            @scroll.passive="actualizarFlechas"
          >
            <article
              v-for="servicio in serviciosGrilla"
              :key="servicio.idServicio"
              role="listitem"
              class="group w-[240px] shrink-0 snap-start cursor-pointer overflow-hidden rounded-2xl border border-borde/60 bg-superficie shadow-sm transition hover:-translate-y-1 hover:border-acento/40 sm:w-[260px]"
              @click="irAServicio(servicio.idServicio)"
            >
              <div class="relative aspect-[4/5] overflow-hidden bg-rosa/20">
                <img
                  v-if="servicio.imagenPortada"
                  :src="servicio.imagenPortada"
                  :alt="servicio.nombre"
                  class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
                  :class="servicio.requiereAdvertencia ? 'scale-105 blur-[18px]' : ''"
                />
                <span
                  v-if="servicio.requiereAdvertencia"
                  class="pointer-events-none absolute inset-0 flex items-center justify-center bg-granate/35 px-3 text-center text-[11px] font-semibold text-inverso-contraste"
                  :title="descripcionAccesibleSensible(servicio)"
                >
                  Contenido sensible
                </span>
                <button
                  type="button"
                  class="absolute right-3 top-3 z-10 flex h-9 w-9 items-center justify-center rounded-full border shadow-sm backdrop-blur-sm transition hover:scale-105"
                  :class="favoritosServicios.has(servicio.idServicio)
                    ? 'border-acento bg-acento text-acento-contraste'
                    : 'border-white/70 bg-superficie/95 text-acento hover:bg-acento hover:text-acento-contraste'"
                  :disabled="actualizando.has(clave('servicio', servicio.idServicio))"
                  :aria-pressed="favoritosServicios.has(servicio.idServicio)"
                  :aria-label="favoritosServicios.has(servicio.idServicio)
                    ? `Quitar ${servicio.nombre} de tus favoritos`
                    : `Agregar ${servicio.nombre} a tus favoritos`"
                  @click.stop="alternarFavorito('servicio', servicio)"
                >
                  <svg
                    class="h-4 w-4"
                    viewBox="0 0 24 24"
                    :fill="favoritosServicios.has(servicio.idServicio) ? 'currentColor' : 'none'"
                    stroke="currentColor"
                    stroke-width="1.8"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
                  </svg>
                </button>
                <span
                  v-if="servicio.descuentoOfertaPorcentaje > 0"
                  class="absolute bottom-3 left-3 rounded-full bg-coral px-2.5 py-1 text-[11px] font-extrabold text-white shadow-sm"
                >
                  -{{ servicio.descuentoOfertaPorcentaje }}%
                </span>
              </div>

              <div class="flex flex-col p-4">
                <h3 class="line-clamp-1 font-display text-base font-bold leading-snug text-granate">
                  {{ servicio.nombre }}
                </h3>
                <button
                  type="button"
                  class="mt-2 flex w-fit items-center gap-2 text-xs font-medium text-cafe/70 transition hover:text-acento"
                  @click.stop="irAPerfil(servicio.nombreArtista)"
                >
                  <span class="flex h-6 w-6 items-center justify-center rounded-full bg-rosa/40 text-[10px] font-bold text-granate">
                    {{ (servicio.nombreArtista || '?').charAt(0).toUpperCase() }}
                  </span>
                  @{{ servicio.nombreArtista }}
                </button>

                <div class="mt-3 flex items-baseline gap-2">
                  <span class="text-[10px] font-semibold uppercase tracking-[0.12em] text-cafe/45">Desde</span>
                  <span class="font-display text-lg font-extrabold text-acento">{{ precioServicio(servicio) }}</span>
                </div>
                <div class="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-cafe/55">
                  <span>{{ ratingServicio(servicio) }}</span>
                  <span>· {{ textoEntrega(servicio) }}</span>
                </div>
              </div>
            </article>

            <router-link
              to="/servicios"
              role="listitem"
              class="flex w-[200px] shrink-0 snap-start flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-borde bg-superficie/60 p-6 text-center transition hover:border-acento/50 hover:bg-superficie-alt sm:w-[220px]"
            >
              <span class="flex h-11 w-11 items-center justify-center rounded-full bg-rosa/30 text-acento">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </svg>
              </span>
              <span class="font-display text-base font-bold text-granate">
                Ver todos los servicios
              </span>
              <span class="text-xs text-cafe/60">
                Filtrá por estilo, presupuesto y cupos.
              </span>
            </router-link>
          </div>

          <button
            v-if="puedeDerecha"
            type="button"
            class="absolute -right-3 top-1/2 z-10 hidden h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full border border-borde/60 bg-superficie text-granate shadow-md transition hover:bg-superficie-alt focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-acento/60 lg:flex"
            aria-label="Ver más servicios"
            @click="desplazarServicios(1)"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m9 18 6-6-6-6" />
            </svg>
          </button>
        </div>
      </section>

      <!-- Productos -->
      <section v-if="productosGrilla.length" class="mt-14" aria-labelledby="titulo-productos">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 id="titulo-productos" class="font-display text-2xl font-extrabold tracking-tight text-granate">
              Listos para descargar
            </h2>
            <p class="mt-1 text-sm text-cafe/60">
              Fondos, emotes, adoptables y más. Sin esperas.
            </p>
          </div>
          <router-link
            to="/productos"
            class="text-sm font-bold text-acento underline decoration-acento/40 underline-offset-4 transition hover:text-acento-hover"
          >
            Ver todos los productos →
          </router-link>
        </div>

        <div class="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4">
          <article
            v-for="producto in productosGrilla"
            :key="producto.idProducto"
            class="group flex cursor-pointer flex-col overflow-hidden rounded-2xl border border-borde/60 bg-superficie shadow-sm transition hover:-translate-y-1 hover:border-acento/40"
            @click="irAProducto(producto.idProducto)"
          >
            <div class="relative aspect-square overflow-hidden bg-rosa/20">
              <img
                v-if="producto.imagenPortada"
                :src="producto.imagenPortada"
                :alt="producto.titulo"
                class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
                :class="producto.contenidoSensible ? 'scale-105 blur-[18px]' : ''"
              />
              <span
                v-if="producto.contenidoSensible"
                class="pointer-events-none absolute inset-0 flex items-center justify-center bg-granate/35 px-2 text-center text-[10px] font-semibold text-inverso-contraste"
                :title="descripcionAccesibleSensible(producto)"
              >
                Sensible
              </span>
              <button
                type="button"
                class="absolute right-2.5 top-2.5 z-10 flex h-8 w-8 items-center justify-center rounded-full border shadow-sm backdrop-blur-sm transition hover:scale-105"
                :class="favoritosProductos.has(producto.idProducto)
                  ? 'border-acento bg-acento text-acento-contraste'
                  : 'border-white/70 bg-superficie/95 text-acento hover:bg-acento hover:text-acento-contraste'"
                :disabled="actualizando.has(clave('producto', producto.idProducto))"
                :aria-pressed="favoritosProductos.has(producto.idProducto)"
                :aria-label="favoritosProductos.has(producto.idProducto)
                  ? `Quitar ${producto.titulo} de tus favoritos`
                  : `Agregar ${producto.titulo} a tus favoritos`"
                @click.stop="alternarFavorito('producto', producto)"
              >
                <svg
                  class="h-3.5 w-3.5"
                  viewBox="0 0 24 24"
                  :fill="favoritosProductos.has(producto.idProducto) ? 'currentColor' : 'none'"
                  stroke="currentColor"
                  stroke-width="1.8"
                  aria-hidden="true"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
                </svg>
              </button>
              <span
                v-if="producto.nombreTipoProducto"
                class="absolute bottom-2.5 left-2.5 rounded-full bg-granate/80 px-2.5 py-0.5 text-[11px] font-semibold text-inverso-contraste backdrop-blur-sm"
              >
                {{ producto.nombreTipoProducto }}
              </span>
            </div>

            <div class="flex flex-1 flex-col p-3.5">
              <h3 class="line-clamp-2 font-display text-sm font-bold leading-snug text-granate">
                {{ producto.titulo }}
              </h3>
              <button
                type="button"
                class="mt-1.5 flex w-fit items-center gap-1.5 text-[11px] text-cafe/65 transition hover:text-acento"
                @click.stop="irAPerfil(producto.nombreArtista)"
              >
                <span class="flex h-5 w-5 items-center justify-center rounded-full bg-rosa/40 text-[9px] font-bold text-granate">
                  {{ (producto.nombreArtista || '?').charAt(0).toUpperCase() }}
                </span>
                @{{ producto.nombreArtista }}
              </button>
              <div class="mt-auto flex items-baseline gap-1.5 pt-2.5">
                <span class="font-display text-base font-extrabold text-acento">{{ precioProducto(producto) }}</span>
                <span v-if="producto.esPiezaUnica" class="text-[10px] font-semibold text-cafe/55">· Pieza única</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <!-- Cierre: ir a los catálogos con filtros completos -->
      <section class="mt-14 grid gap-4 lg:grid-cols-2" aria-label="Explorar catálogos">
        <article class="flex flex-col items-start gap-3 rounded-2xl border border-borde/60 bg-superficie p-7">
          <h2 class="font-display text-xl font-extrabold text-granate">¿Buscás algo específico?</h2>
          <p class="text-sm leading-relaxed text-cafe/70">
            En <span class="font-semibold text-granate">Servicios</span> filtrás por estilo,
            presupuesto, plazo y cupos disponibles.
          </p>
          <button
            type="button"
            class="mt-1 inline-flex items-center gap-2 rounded-full bg-coral px-5 py-2.5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
            @click="irAServicios"
          >
            Explorar servicios
            <span aria-hidden="true">→</span>
          </button>
        </article>

        <article class="flex flex-col items-start gap-3 rounded-2xl border border-borde/60 bg-superficie p-7">
          <h2 class="font-display text-xl font-extrabold text-granate">Recursos al instante</h2>
          <p class="text-sm leading-relaxed text-cafe/70">
            En <span class="font-semibold text-granate">Productos</span> filtrás por tipo,
            medio, precio y disponibilidad.
          </p>
          <button
            type="button"
            class="mt-1 inline-flex items-center gap-2 rounded-full border border-rosa px-5 py-2.5 text-sm font-bold text-granate transition hover:bg-rosa/20"
            @click="irAProductos"
          >
            Explorar productos
            <span aria-hidden="true">→</span>
          </button>
        </article>
      </section>
    </template>
  </div>
</template>
