<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, esImagenPermitida } from '../servicios/archivos'

const route = useRoute()

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

    perfil.value = {
      ...data,
      // placeholders
      portfolio: [],
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

cargarPerfil()

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

// --- Edición de perfil ---
const modalAbierto = ref(false)
const guardando = ref(false)

const editNombreDisplay = ref('')
const editBiografia = ref('')
const editFoto = ref(null)
const editBanner = ref(null)
const previewFoto = ref('')
const previewBanner = ref('')
const errorEdicion = ref('')

function abrirModal() {
  editNombreDisplay.value = perfil.value.nombreDisplay || ''
  editBiografia.value = perfil.value.biografia || ''
  editFoto.value = null
  editBanner.value = null
  previewFoto.value = ''
  previewBanner.value = ''
  errorEdicion.value = ''
  modalAbierto.value = true
}

function cerrarModal() {
  modalAbierto.value = false
}

function onFotoChange(e) {
  const f = e.target.files[0] || null
  if (f && !esImagenPermitida(f)) {
    editFoto.value = null
    previewFoto.value = ''
    errorEdicion.value = 'El formato es inválido.'
    e.target.value = ''
    return
  }

  errorEdicion.value = ''
  editFoto.value = f
  previewFoto.value = f ? URL.createObjectURL(f) : ''
}

function onBannerChange(e) {
  const f = e.target.files[0] || null
  if (f && !esImagenPermitida(f)) {
    editBanner.value = null
    previewBanner.value = ''
    errorEdicion.value = 'El formato es inválido.'
    e.target.value = ''
    return
  }

  errorEdicion.value = ''
  editBanner.value = f
  previewBanner.value = f ? URL.createObjectURL(f) : ''
}

async function subirArchivo(archivo) {
  const form = new FormData()
  form.append('archivo', archivo)
  const res = await api.post('/upload', form)
  return res.data.url
}

async function guardarPerfil() {
  guardando.value = true
  errorEdicion.value = ''
  try {
    let urlFoto = null
    let urlBanner = null

    if (editFoto.value) {
      urlFoto = await subirArchivo(editFoto.value)
    }
    if (editBanner.value) {
      urlBanner = await subirArchivo(editBanner.value)
    }

    const payload = {
      nombreDisplay: editNombreDisplay.value,
      biografia: editBiografia.value,
      ...(urlFoto ? { urlFotoPerfil: urlFoto } : {}),
      ...(urlBanner ? { urlBanner } : {}),
    }

    const res = await api.put(`/usuarios/${perfil.value.idUsuario}/perfil`, payload)

    perfil.value.nombreDisplay = res.data.nombreDisplay
    perfil.value.biografia = res.data.biografia
    perfil.value.urlFotoPerfil = res.data.urlFotoPerfil
    perfil.value.urlBanner = res.data.urlBanner

    localStorage.setItem('urlFotoPerfil', res.data.urlFotoPerfil || '')
    window.dispatchEvent(new CustomEvent('perfil-foto-actualizada', {
      detail: { url: res.data.urlFotoPerfil || '' },
    }))

    modalAbierto.value = false
  } catch (e) {
    errorEdicion.value = e.response?.data?.message || 'No se pudo guardar el perfil. Intentalo de nuevo.'
  } finally {
    guardando.value = false
  }
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
        <div class="relative w-40 sm:w-48 h-10 sm:h-12 shrink-0 z-20">
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
          >
            Preferencias
          </button>
          <button
            v-if="esDuenio"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
            @click="abrirModal"
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

          <div v-if="tabActiva === 'portfolio'" class="mt-6 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            <div v-for="obra in perfil.portfolio" :key="obra.id" class="rounded-xl overflow-hidden bg-rosa/20 aspect-square flex flex-col">
              <div class="flex-1 bg-coral/20">
                <img v-if="obra.url" :src="obra.url" alt="" class="w-full h-full object-cover" />
              </div>
              <div class="p-3 text-sm text-cafe">{{ obra.titulo }}</div>
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

    <!-- Modal editar perfil -->
    <div
      v-if="modalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">Editar perfil</h2>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2">Nombre de display</label>
            <input
              v-model="editNombreDisplay"
              type="text"
              maxlength="25"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Biografía</label>
            <textarea
              v-model="editBiografia"
              rows="4"
              maxlength="500"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
            <p class="text-right text-xs text-cafe/50 mt-1">{{ editBiografia.length }}/500</p>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Foto de perfil</label>
            <div class="flex items-center gap-4">
              <div class="w-16 h-16 rounded-full overflow-hidden border-2 border-rosa bg-rosa/20 shrink-0">
                <img v-if="previewFoto" :src="previewFoto" alt="" class="w-full h-full object-cover" />
                <img v-else-if="perfil.urlFotoPerfil" :src="perfil.urlFotoPerfil" alt="" class="w-full h-full object-cover" />
                <span v-else class="w-full h-full flex items-center justify-center text-granate">{{ fotoInicial(perfil.nombreUsuario) }}</span>
              </div>
              <div>
                <input id="foto-perfil" type="file" :accept="acceptImagenes" @change="onFotoChange" class="sr-only" />
                <label
                  for="foto-perfil"
                  class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20 focus-within:outline-none focus-within:ring-2 focus-within:ring-coral focus-within:ring-offset-2"
                >
                  {{ editFoto ? 'Foto seleccionada' : 'Cambiar foto' }}
                </label>
                <p class="mt-1 text-xs text-cafe/60">JPG, PNG o WEBP</p>
              </div>
            </div>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Banner</label>
            <div v-if="previewBanner" class="mb-2 w-full h-24 rounded-lg overflow-hidden bg-rosa/20">
              <img :src="previewBanner" alt="" class="w-full h-full object-cover" />
            </div>
            <input id="banner-perfil" type="file" :accept="acceptImagenes" @change="onBannerChange" class="sr-only" />
            <label
              for="banner-perfil"
              class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20 focus-within:outline-none focus-within:ring-2 focus-within:ring-coral focus-within:ring-offset-2"
            >
              {{ editBanner ? 'Banner seleccionado' : 'Cambiar banner' }}
            </label>
            <p class="mt-1 text-xs text-cafe/60">JPG, PNG o WEBP</p>
          </div>

          <p v-if="errorEdicion" class="text-rojizo text-sm">{{ errorEdicion }}</p>
        </div>

        <div class="flex items-center justify-end gap-3 mt-6">
          <button
            class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
            @click="cerrarModal"
          >
            Cancelar
          </button>
          <button
            :disabled="guardando"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
            @click="guardarPerfil"
          >
            {{ guardando ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>