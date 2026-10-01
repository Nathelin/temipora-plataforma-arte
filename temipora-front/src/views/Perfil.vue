<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, esImagenPermitida } from '../servicios/archivos'

const route = useRoute()
const router = useRouter()


const perfil = ref({
  idUsuario: null,
  nombreUsuario: '',
  nombreDisplay: '',
  rol: '',
  biografia: '',
  urlFotoPerfil: '',
  urlBanner: '',
  // placeholders hasta que existan sus endpoints
  portfolio: [],
  servicios: [],
  productos: [],
  pedidos: [],
})

const cargandoPerfil = ref(true)
const errorCarga = ref('')

async function cargarPerfil() {
  cargandoPerfil.value = true
  errorCarga.value = ''
  try {
    const nombreUsuario = route.params.nombreUsuario
    const res = await api.get(`/perfil/${nombreUsuario}`)
    const data = res.data

    const portfolioRes = data.rol === 'ARTISTA'
      ? await api.get(`/portfolio/${data.nombreUsuario}/paginado`)
      : { data: { content: [] } }

    perfil.value = {
      ...data,
      portfolio: portfolioRes.data.content || [],
      servicios: [],
      productos: [],
      pedidos: [],
    }
  } catch (e) {
    errorCarga.value = 'No se pudo cargar el perfil.'
  } finally {
    cargandoPerfil.value = false
  }
}

watch(() => route.params.nombreUsuario, cargarPerfil, { immediate: true })

const esArtista = computed(() => perfil.value.rol === 'ARTISTA')
const tabActiva = ref('portfolio')

const nombreParaMostrar = computed(() => perfil.value.nombreDisplay || perfil.value.nombreUsuario)

const esDuenio = computed(() => {
  const actual = localStorage.getItem('nombreUsuario') || ''
  return actual.toLowerCase() === perfil.value.nombreUsuario.toLowerCase()
})

function fotoInicial(nombre) {
  return (nombre || '?').charAt(0).toUpperCase()
}

const portfolioModalAbierto = ref(false)
const imagenSeleccionada = ref(null)
const mostrarImagenSensible = ref(false)
const guardandoImagen = ref(false)
const errorPortfolio = ref('')
const tituloImagen = ref('')
const descripcionImagen = ref('')
const medioImagen = ref('TRADICIONAL')
const materialesImagen = ref('')
const contenidoSensibleImagen = ref('')
const archivoImagen = ref(null)
const previewImagen = ref('')

function abrirAltaImagen() {
  tituloImagen.value = ''
  descripcionImagen.value = ''
  medioImagen.value = 'TRADICIONAL'
  materialesImagen.value = ''
  contenidoSensibleImagen.value = ''
  archivoImagen.value = null
  previewImagen.value = ''
  errorPortfolio.value = ''
  portfolioModalAbierto.value = true
}

function cerrarAltaImagen() {
  portfolioModalAbierto.value = false
}

function onImagenPortfolioChange(event) {
  const archivo = event.target.files[0] || null
  if (archivo && !esImagenPermitida(archivo)) {
    archivoImagen.value = null
    previewImagen.value = ''
    errorPortfolio.value = 'El formato de la imagen es inválido.'
    event.target.value = ''
    return
  }

  errorPortfolio.value = ''
  archivoImagen.value = archivo
  previewImagen.value = archivo ? URL.createObjectURL(archivo) : ''
}

function abrirDetalleImagen(imagen) {
  imagenSeleccionada.value = imagen
  mostrarImagenSensible.value = false
}

function cerrarDetalleImagen() {
  imagenSeleccionada.value = null
  mostrarImagenSensible.value = false
}

function revelarImagenSensible() {
  mostrarImagenSensible.value = true
}

async function guardarImagenPortfolio() {
  if (!archivoImagen.value) {
    errorPortfolio.value = 'Selecciona una imagen.'
    return
  }
  if (!tituloImagen.value.trim()) {
    errorPortfolio.value = 'El título es obligatorio.'
    return
  }
  if (!descripcionImagen.value.trim()) {
    errorPortfolio.value = 'La descripción es obligatoria.'
    return
  }

  guardandoImagen.value = true
  errorPortfolio.value = ''
  try {
    const url = await subirArchivo(archivoImagen.value)
    const res = await api.post(`/portfolio/usuarios/${perfil.value.idUsuario}/imagenes`, {
      titulo: tituloImagen.value,
      descripcion: descripcionImagen.value,
      url,
      medio: medioImagen.value,
      materiales: materialesImagen.value,
      contenidoSensible: contenidoSensibleImagen.value || null,
    })
    perfil.value.portfolio.unshift(res.data)
    cerrarAltaImagen()
  } catch (e) {
    errorPortfolio.value = e.response?.data?.message || 'No se pudo guardar la imagen.'
  } finally {
    guardandoImagen.value = false
  }
}

async function subirArchivo(archivo) {
  const form = new FormData()
  form.append('archivo', archivo)
  const res = await api.post('/upload', form)
  return res.data.url
}

function irAConfiguracion(seccion = 'perfil') {
  router.push({ name: `configuracion-${seccion}` })
}
</script>

<template>
  <div class="w-full">
    <div v-if="cargandoPerfil" class="max-w-screen-2xl mx-auto px-4 sm:px-8 py-16 text-center text-cafe/60">
      Cargando perfil...
    </div>
    <div v-else-if="errorCarga" class="max-w-screen-2xl mx-auto px-4 sm:px-8 py-16 text-center text-rojizo">
      {{ errorCarga }}
    </div>
    <template v-else>
    <!-- Banner -->
    <div class="relative h-48 sm:h-64 bg-coral/20 w-full z-0">
      <img
        v-if="perfil.urlBanner"
        :src="perfil.urlBanner"
        alt="Banner"
        class="w-full h-full object-cover"
      />

    </div>

    <div class="max-w-screen-2xl mx-auto px-4 sm:px-8">
      <div class="flex flex-col sm:flex-row sm:items-end gap-4 sm:gap-6 pb-6 border-b border-rosa pt-2 sm:pt-4">
        <div class="relative w-40 sm:w-48 h-10 sm:h-12 shrink-0 z-10">
          <div class="absolute bottom-0 left-0 w-40 h-40 sm:w-48 sm:h-48 rounded-full overflow-hidden border-4 border-blanco bg-coral/20 shadow-md">
            <img
              v-if="perfil.urlFotoPerfil"
              :src="perfil.urlFotoPerfil"
              alt="Perfil"
              class="w-full h-full object-cover"
            />
            <span
              v-else
              class="w-full h-full flex items-center justify-center text-5xl text-granate"
            >
              {{ fotoInicial(perfil.nombreUsuario) }}
            </span>
          </div>
        </div>

        <div class="flex-1 mt-2 sm:mt-0">
          <h1 class="text-2xl sm:text-3xl font-medium text-granate">{{ nombreParaMostrar }}</h1>
          <p class="text-sm text-cafe/60 mb-3">@{{ perfil.nombreUsuario }}</p>

          <div class="max-w-2xl flex items-start gap-3">
            <p class="text-sm text-cafe leading-relaxed line-clamp-3">
              {{ perfil.biografia || 'Sin biografía.' }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-3 shrink-0 mt-4 sm:mt-0 pb-1 sm:pb-3">
          <button v-if="esDuenio"
            class="px-4 py-2 rounded-md text-sm text-granate border border-rosa bg-blanco hover:bg-rosa/20 transition"
            @click="irAConfiguracion('privacidad')"
          >
            Preferencias
          </button>
          <button
            v-if="esDuenio"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
            @click="irAConfiguracion('perfil')"
          >
            Editar perfil
          </button>
        </div>
      </div>

      <!-- Pestañas -->
      <div class="pb-12 pt-4">
        <template v-if="esArtista">
          <div class="flex items-center gap-6 border-b border-rosa">
            <button
              class="py-3 text-sm transition border-b-2"
              :class="tabActiva === 'portfolio' ? 'border-coral text-granate font-medium' : 'border-transparent text-cafe/60 hover:text-cafe'"
              @click="tabActiva = 'portfolio'"
            >
              Portfolio
            </button>
            <button
              class="py-3 text-sm transition border-b-2"
              :class="tabActiva === 'servicios' ? 'border-coral text-granate font-medium' : 'border-transparent text-cafe/60 hover:text-cafe'"
              @click="tabActiva = 'servicios'"
            >
              Servicios
            </button>
            <button
              class="py-3 text-sm transition border-b-2"
              :class="tabActiva === 'productos' ? 'border-coral text-granate font-medium' : 'border-transparent text-cafe/60 hover:text-cafe'"
              @click="tabActiva = 'productos'"
            >
              Productos
            </button>
          </div>

            <div v-if="tabActiva === 'portfolio'" class="mt-6">
              <div class="flex items-center justify-between gap-4 mb-4">
                <p v-if="!perfil.portfolio.length" class="mr-auto text-sm text-cafe/60">
                  Este portfolio todavía no tiene imágenes.
                </p>
                <button
                  v-if="esDuenio"
                  class="ml-auto px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
                  @click="abrirAltaImagen"
                >
                  Subir imagen
                </button>
              </div>

              <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                <button
                  v-for="obra in perfil.portfolio"
                  :key="obra.idImagen"
                  class="rounded-xl overflow-hidden bg-rosa/20 aspect-square flex flex-col text-left hover:ring-2 hover:ring-coral transition"
                  @click="abrirDetalleImagen(obra)"
                >
                  <div class="min-h-0 flex-1 bg-coral/20">
                    <div class="relative w-full h-full">
                      <img
                        v-if="obra.url"
                        :src="obra.url"
                        :alt="obra.titulo"
                        class="w-full h-full object-cover transition duration-200"
                        :class="obra.requiereAdvertencia ? 'blur-[18px] scale-105' : ''"
                      />
                      <span
                        v-if="obra.requiereAdvertencia"
                        class="absolute inset-0 flex items-center justify-center bg-granate/10 text-3xl text-white drop-shadow"
                        :title="`Contenido sensible: ${obra.contenidoSensible}`"
                        :aria-label="`Contenido sensible: ${obra.contenidoSensible}`"
                      >
                        ⚠
                      </span>
                    </div>
                  </div>
                  <div class="shrink-0 bg-rosa/20 p-3 text-sm text-cafe truncate">{{ obra.titulo }}</div>
                </button>
              </div>
            </div>

          <div v-if="tabActiva === 'servicios'" class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div v-for="s in perfil.servicios" :key="s.id" class="bg-blanco border border-rosa rounded-xl p-5">
              <h3 class="text-granate">{{ s.titulo }}</h3>
              <p class="text-sm text-cafe/70 mt-1">{{ s.descripcion }}</p>
              <p class="text-sm text-rojizo mt-3">{{ s.precio }}</p>
            </div>
          </div>

          <div v-if="tabActiva === 'productos'" class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div v-for="p in perfil.productos" :key="p.id" class="bg-blanco border border-rosa rounded-xl p-5">
              <h3 class="text-granate">{{ p.titulo }}</h3>
              <p class="text-sm text-cafe/70 mt-1">{{ p.descripcion }}</p>
              <p class="text-sm text-rojizo mt-3">{{ p.precio }}</p>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="flex items-center gap-6 border-b border-rosa">
            <button class="py-3 text-sm transition border-b-2 border-coral text-granate font-medium">
              Pedidos
            </button>
          </div>

          <div class="mt-6 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            <div v-for="pedido in perfil.pedidos" :key="pedido.id" class="rounded-xl overflow-hidden bg-rosa/20 aspect-square flex flex-col">
              <div class="flex-1 bg-coral/20">
                <img v-if="pedido.url" :src="pedido.url" alt="" class="w-full h-full object-cover" />
              </div>
              <div class="p-3">
                <div class="text-sm text-cafe">{{ pedido.titulo }}</div>
                <div class="text-xs text-cafe/50">{{ pedido.fecha }}</div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Modal alta de imagen de portfolio -->
    <div
      v-if="portfolioModalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarAltaImagen"
    >
      <div class="w-full max-w-5xl bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">Subir imagen a portfolio</h2>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div class="space-y-5">
            <div>
              <label class="block text-sm text-cafe mb-2">Título <span class="text-rojizo">*</span></label>
              <input v-model="tituloImagen" type="text" maxlength="100" required class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
            </div>

            <div>
              <label class="block text-sm text-cafe mb-2">Descripción <span class="text-rojizo">*</span></label>
              <textarea v-model="descripcionImagen" rows="5" maxlength="1000" required class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"></textarea>
            </div>

            <div>
              <label class="block text-sm text-cafe mb-2">Medio <span class="text-rojizo">*</span></label>
              <select v-model="medioImagen" class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                <option value="TRADICIONAL">Tradicional</option>
                <option value="DIGITAL">Digital</option>
              </select>
            </div>

            <div>
              <label class="block text-sm text-cafe mb-2">Materiales</label>
              <input v-model="materialesImagen" type="text" maxlength="500" placeholder="Ej.: óleo sobre lienzo" class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
            </div>

            <div>
              <label class="block text-sm text-cafe mb-2">Contenido sensible</label>
              <select v-model="contenidoSensibleImagen" class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                <option value="">Sin contenido sensible</option>
                <option value="VIOLENCIA">Violencia</option>
                <option value="DESNUDOS">Desnudos</option>
                <option value="GENERAL">Contenido sensible general</option>
              </select>
            </div>

            <p v-if="errorPortfolio" class="text-rojizo text-sm">{{ errorPortfolio }}</p>
          </div>

          <div class="flex flex-col">
            <label class="block text-sm text-cafe mb-2">Imagen <span class="text-rojizo">*</span></label>
            <div class="flex-1 min-h-72 rounded-xl border border-dashed border-rosa bg-rosa/10 flex items-center justify-center overflow-hidden">
              <img v-if="previewImagen" :src="previewImagen" alt="Vista previa" class="w-full h-full max-h-112 object-contain" />
              <p v-else class="px-6 text-center text-sm text-cafe/50">Selecciona una imagen para ver la vista previa.</p>
            </div>
            <input id="portfolio-imagen" type="file" :accept="acceptImagenes" class="sr-only" @change="onImagenPortfolioChange" />
            <label for="portfolio-imagen" class="mt-4 inline-flex cursor-pointer justify-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm text-granate hover:bg-coral/20 transition">
              {{ archivoImagen ? 'Cambiar imagen' : 'Seleccionar imagen' }}
            </label>
          </div>
        </div>

        <div class="flex items-center justify-end gap-3 mt-6">
          <button class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition" @click="cerrarAltaImagen">Cancelar</button>
          <button :disabled="guardandoImagen || !archivoImagen" class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm" @click="guardarImagenPortfolio">
            {{ guardandoImagen ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal detalle de imagen -->
    <div v-if="imagenSeleccionada" class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4" @click.self="cerrarDetalleImagen">
      <div class="w-full max-w-2xl bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <div class="relative overflow-hidden rounded-lg bg-rosa/20">
          <img
            :src="imagenSeleccionada.url"
            :alt="imagenSeleccionada.titulo"
            class="w-full max-h-[55vh] object-contain transition duration-200"
            :class="imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible ? 'blur-[18px] scale-105' : ''"
          />
          <span
            v-if="imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible"
            class="absolute inset-0 flex items-center justify-center text-5xl text-white drop-shadow"
            :title="`Contenido sensible: ${imagenSeleccionada.contenidoSensible}`"
            :aria-label="`Contenido sensible: ${imagenSeleccionada.contenidoSensible}`"
          >
            ⚠
          </span>
        </div>
        <div v-if="imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible" class="mt-4 rounded-lg bg-rosa/30 px-4 py-3 text-sm text-cafe">
          <label>Esta imagen tiene contenido sensible categorizado como: {{ imagenSeleccionada.contenidoSensible.toLowerCase() }}.    </label>
          <button
            type="button"
            class="mt-3 rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition"
            @click="revelarImagenSensible"
          >
            Ver imagen
          </button>
        </div>
        <h2 class="text-xl text-granate mt-5">{{ imagenSeleccionada.titulo }}</h2>
        <p class="text-sm text-cafe mt-2">{{ imagenSeleccionada.descripcion }}</p>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-4 text-sm text-cafe/70">
          <p><span class="text-rojizo">Medio:</span> {{ imagenSeleccionada.medio }}</p>
          <p><span class="text-rojizo">Materiales:</span> {{ imagenSeleccionada.materiales || 'No especificados' }}</p>
          <p><span class="text-rojizo">Subida:</span> {{ new Date(imagenSeleccionada.fechaSubida).toLocaleString() }}</p>
          <p><span class="text-rojizo">Contenido sensible:</span> {{ imagenSeleccionada.contenidoSensible || 'No' }}</p>
        </div>
        <div class="flex justify-end mt-6">
          <button class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition" @click="cerrarDetalleImagen">Cerrar</button>
        </div>
      </div>
    </div>

    </template>
  </div>
</template>