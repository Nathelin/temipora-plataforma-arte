<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { cargarCatalogoProductos } from '../servicios/productos'
import TarjetaServicio from '../components/TarjetaServicio.vue'
import TarjetaProducto from '../components/TarjetaProducto.vue'

const route = useRoute()
const router = useRouter()

const SERVICIOS_DESTACADOS = 8
const PRODUCTOS_DESTACADOS = 8

const servicios = ref([])
const productos = ref([])
const cargandoServicios = ref(true)
const cargandoProductos = ref(true)
const errorServicios = ref('')
const errorProductos = ref('')

const favoritosServicios = ref(new Set())
const favoritosProductos = ref(new Set())
const actualizandoServicio = ref(new Set())
const actualizandoProducto = ref(new Set())

const nombreUsuario = computed(() => localStorage.getItem('nombreUsuario') || '')
const hayServicios = computed(() => servicios.value.length > 0)
const hayProductos = computed(() => productos.value.length > 0)
const cargando = computed(
  () => cargandoServicios.value || cargandoProductos.value,
)

function haySesion() {
  return !!localStorage.getItem('token')
}

function irALogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function cargarServicios() {
  cargandoServicios.value = true
  errorServicios.value = ''
  try {
    const res = await api.get('/servicios', {
      params: {
        orden: 'RELEVANCIA',
        pagina: 0,
        tamanio: SERVICIOS_DESTACADOS,
      },
    })
    servicios.value = res.data?.contenido || []
  } catch (e) {
    errorServicios.value = e.response?.data?.message
      || 'No se pudieron cargar los servicios.'
  } finally {
    cargandoServicios.value = false
  }
}

async function cargarProductos() {
  cargandoProductos.value = true
  errorProductos.value = ''
  try {
    const data = await cargarCatalogoProductos({
      orden: 'RELEVANCIA',
      pagina: 0,
      tamanio: PRODUCTOS_DESTACADOS,
    })
    productos.value = data?.contenido || []
  } catch (e) {
    errorProductos.value = e.response?.data?.message
      || 'No se pudieron cargar los productos.'
  } finally {
    cargandoProductos.value = false
  }
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

async function alternarFavoritoServicio(servicio) {
  if (!haySesion()) {
    irALogin()
    return
  }
  const id = servicio.idServicio
  if (actualizandoServicio.value.has(id)) return
  actualizandoServicio.value = new Set(actualizandoServicio.value).add(id)
  try {
    if (favoritosServicios.value.has(id)) {
      await api.delete(`/favoritos/servicios/${id}`)
      const siguientes = new Set(favoritosServicios.value)
      siguientes.delete(id)
      favoritosServicios.value = siguientes
    } else {
      await api.post(`/favoritos/servicios/${id}`)
      favoritosServicios.value = new Set(favoritosServicios.value).add(id)
    }
  } catch {
    // Se mantiene el estado anterior si la llamada falla.
  } finally {
    const siguientes = new Set(actualizandoServicio.value)
    siguientes.delete(id)
    actualizandoServicio.value = siguientes
  }
}

async function alternarFavoritoProducto(producto) {
  if (!haySesion()) {
    irALogin()
    return
  }
  const id = producto.idProducto
  if (actualizandoProducto.value.has(id)) return
  actualizandoProducto.value = new Set(actualizandoProducto.value).add(id)
  try {
    if (favoritosProductos.value.has(id)) {
      await api.delete(`/favoritos/productos/${id}`)
      const siguientes = new Set(favoritosProductos.value)
      siguientes.delete(id)
      favoritosProductos.value = siguientes
    } else {
      await api.post(`/favoritos/productos/${id}`)
      favoritosProductos.value = new Set(favoritosProductos.value).add(id)
    }
  } catch {
    // Se mantiene el estado anterior si la llamada falla.
  } finally {
    const siguientes = new Set(actualizandoProducto.value)
    siguientes.delete(id)
    actualizandoProducto.value = siguientes
  }
}

onMounted(() => {
  cargarFavoritos()
  cargarServicios()
  cargarProductos()
})
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="text-xs font-bold uppercase tracking-[0.18em] text-coral">
          Explorar
        </p>
        <h1 class="mt-2 font-display text-3xl font-extrabold text-granate sm:text-4xl">
          <template v-if="nombreUsuario">Hola, @{{ nombreUsuario }}</template>
          <template v-else>Descubrí lo que se está creando</template>
        </h1>
        <p class="mt-1 text-sm text-cafe/60">
          Servicios a medida y piezas listas para llevar, de artistas de la plataforma.
        </p>
      </div>
    </header>

    <div v-if="cargando" class="mt-10 space-y-10" aria-label="Cargando explorar">
      <div v-for="bloque in 2" :key="bloque" class="animate-pulse">
        <div class="h-6 w-40 rounded-full bg-rosa/40"></div>
        <div class="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-4">
          <div v-for="item in 4" :key="item" class="h-64 rounded-[22px] bg-rosa/25"></div>
        </div>
      </div>
    </div>

    <template v-else>
      <section class="mt-10" aria-labelledby="titulo-servicios">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 id="titulo-servicios" class="font-display text-2xl font-bold text-granate">
              Servicios destacados
            </h2>
            <p class="mt-1 text-sm text-cafe/60">
              Encargá una pieza hecha a tu medida.
            </p>
          </div>
          <router-link
            to="/servicios"
            class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
          >
            Ver todos los servicios →
          </router-link>
        </div>

        <p
          v-if="errorServicios"
          role="alert"
          class="mt-4 rounded-2xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        >
          {{ errorServicios }}
        </p>

        <p
          v-else-if="!hayServicios"
          class="mt-4 rounded-2xl border border-dashed border-rosa/60 bg-blanco px-4 py-10 text-center text-sm text-cafe/55"
        >
          Todavía no hay servicios publicados. Volvé pronto.
        </p>

        <div
          v-else
          class="mt-5 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-4"
        >
          <TarjetaServicio
            v-for="servicio in servicios"
            :key="servicio.idServicio"
            :servicio="servicio"
            mostrar-favorito
            :es-favorito="favoritosServicios.has(servicio.idServicio)"
            :actualizando-favorito="actualizandoServicio.has(servicio.idServicio)"
            @alternar-favorito="alternarFavoritoServicio"
          />
        </div>
      </section>

      <section class="mt-14" aria-labelledby="titulo-productos">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 id="titulo-productos" class="font-display text-2xl font-bold text-granate">
              Productos destacados
            </h2>
            <p class="mt-1 text-sm text-cafe/60">
              Piezas terminadas, adoptables y recursos digitales.
            </p>
          </div>
          <router-link
            to="/productos"
            class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
          >
            Ver todos los productos →
          </router-link>
        </div>

        <p
          v-if="errorProductos"
          role="alert"
          class="mt-4 rounded-2xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        >
          {{ errorProductos }}
        </p>

        <p
          v-else-if="!hayProductos"
          class="mt-4 rounded-2xl border border-dashed border-rosa/60 bg-blanco px-4 py-10 text-center text-sm text-cafe/55"
        >
          Todavía no hay productos publicados. Volvé pronto.
        </p>

        <div
          v-else
          class="mt-5 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-4"
        >
          <TarjetaProducto
            v-for="producto in productos"
            :key="producto.idProducto"
            :producto="producto"
            mostrar-favorito
            :es-favorito="favoritosProductos.has(producto.idProducto)"
            :actualizando-favorito="actualizandoProducto.has(producto.idProducto)"
            @alternar-favorito="alternarFavoritoProducto"
          />
        </div>
      </section>
    </template>
  </div>
</template>
