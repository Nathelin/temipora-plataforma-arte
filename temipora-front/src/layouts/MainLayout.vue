<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useSubirImagen } from '../composables/useSubirImagen'

const router = useRouter()
const { abrir } = useSubirImagen()

const token = ref(localStorage.getItem('token') || '')
const rol = ref(localStorage.getItem('rol') || '')
const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')

const menuAbierto = ref(false)
const subirAbierto = ref(false)
const contenedorMenu = ref(null)
const contenedorSubir = ref(null)

const estaLogueado = computed(() => !!token.value)
const esAdmin = computed(() => rol.value === 'ADMIN')
const esArtista = computed(() => rol.value === 'ARTISTA')

function alternarMenu() {
  menuAbierto.value = !menuAbierto.value
}

function subirImagen() {
  subirAbierto.value = false
  abrir('crear')
  router.push({ name: 'portfolio-subir' })
}

function manejarClickExterior(event) {
  if (contenedorMenu.value && !contenedorMenu.value.contains(event.target)) {
    menuAbierto.value = false
  }
  if (contenedorSubir.value && !contenedorSubir.value.contains(event.target)) {
    subirAbierto.value = false
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape') {
    menuAbierto.value = false
    subirAbierto.value = false
  }
}

function actualizarFotoPerfil(event) {
  urlFotoPerfil.value = event.detail?.url || ''
}

onMounted(() => {
  window.addEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
  document.addEventListener('click', manejarClickExterior)
  document.addEventListener('keydown', manejarTecla)
})

onBeforeUnmount(() => {
  window.removeEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
  document.removeEventListener('click', manejarClickExterior)
  document.removeEventListener('keydown', manejarTecla)
})

function cerrarSesion() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
  router.push({ name: 'login' })
}

function irAPerfil() {
  router.push({ name: 'perfil', params: { nombreUsuario: nombreUsuario.value } })
}

function irAInicio() {
  router.push({ name: 'inicio' })
}
</script>

<template>
  <div class="min-h-screen w-full flex flex-col bg-crema text-cafe">
    <header class="bg-blanco border-b border-rosa sticky top-0 z-30">
      <div class="max-w-screen-2xl mx-auto px-4 sm:px-8 h-10 flex items-center justify-between">
        <!-- Izquierda -->
        <div class="flex items-center gap-6">
          <button class="text-xl font-normal text-granate" @click="irAInicio">
            Temipora
          </button>

          <nav v-if="estaLogueado && !esAdmin" class="hidden sm:flex items-center gap-5 text-sm">
            <router-link to="/servicios" class="text-cafe hover:text-rojizo transition">
              Servicios
            </router-link>
            <router-link to="/productos" class="text-cafe hover:text-rojizo transition">
              Productos
            </router-link>
          </nav>
        </div>

        <!-- Derecha -->
        <div class="flex items-center gap-3">
          <template v-if="!estaLogueado">
            <router-link
              to="/login"
              class="px-4 py-2 rounded-md text-sm text-granate border border-rosa hover:bg-rosa/20 transition"
            >
              Iniciar sesión
            </router-link>
            <router-link
              to="/registrarse"
              class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
            >
              Registrarse
            </router-link>
          </template>

          <template v-else>
            <!-- Botón Subir (solo artistas) -->
            <div v-if="esArtista" ref="contenedorSubir" class="relative">
              <button
                class="px-3 py-1.5 rounded-md text-sm text-granate border border-rosa hover:bg-rosa/20 transition"
                aria-haspopup="menu"
                :aria-expanded="subirAbierto"
                @click="subirAbierto = !subirAbierto"
              >
                <span aria-hidden="true">+</span><span class="hidden sm:inline"> Subir</span><span class="hidden sm:inline" aria-hidden="true"> ▾</span>
              </button>

              <div
                v-if="subirAbierto"
                class="absolute right-0 mt-2 w-52 rounded-lg bg-blanco shadow-lg border border-rosa overflow-hidden z-40"
                role="menu"
              >
                <button
                  class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
                  role="menuitem"
                  @click="subirImagen"
                >
                  Imagen
                </button>
                <button
                  disabled
                  class="w-full text-left px-4 py-2 text-sm text-cafe/40 cursor-not-allowed"
                  aria-disabled="true"
                >
                  Producto <span class="text-xs">· Próximamente</span>
                </button>
                <button
                  disabled
                  class="w-full text-left px-4 py-2 text-sm text-cafe/40 cursor-not-allowed"
                  aria-disabled="true"
                >
                  Servicio <span class="text-xs">· Próximamente</span>
                </button>
              </div>
            </div>

            <div ref="contenedorMenu" class="relative">
              <div class="flex items-center gap-2">
                <button class="flex items-center gap-2" aria-haspopup="menu" :aria-expanded="menuAbierto" @click="alternarMenu">
                  <img
                    v-if="urlFotoPerfil"
                    :src="urlFotoPerfil"
                    alt="Perfil"
                    class="w-8 h-8 rounded-full object-cover border border-rosa"
                  />
                  <span
                    v-else
                    class="w-8 h-8 rounded-full bg-coral/20 text-granate flex items-center justify-center text-sm"
                  >
                    {{ (nombreUsuario || '?').charAt(0).toUpperCase() }}
                  </span>
                  <span class="text-sm text-cafe hidden sm:inline">{{ nombreUsuario }}</span>
                </button>
              </div>

              <div
                v-if="menuAbierto"
                class="absolute right-0 mt-2 w-44 rounded-lg bg-blanco shadow-lg border border-rosa overflow-hidden z-40"
                role="menu"
              >
                <button
                  class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
                  role="menuitem"
                  @click="menuAbierto = false; irAPerfil()"
                >
                  Perfil
                </button>
                <button
                  v-if="esAdmin"
                  class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
                  role="menuitem"
                  @click="menuAbierto = false; router.push({ name: 'usuarios' })"
                >
                  Panel de administración
                </button>
                <button
                  class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
                  role="menuitem"
                  @click="menuAbierto = false; router.push({ name: 'configuracion-perfil' })"
                >
                  Configuración
                </button>
                <button
                  class="w-full text-left px-4 py-2 text-sm text-rojizo hover:bg-rosa/20 transition"
                  role="menuitem"
                  @click="menuAbierto = false; cerrarSesion()"
                >
                  Cerrar sesión
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </header>

    <main class="flex-1 w-full">
      <router-view />
    </main>


    
    <footer class="bg-granate text-crema mt-auto">
      <div class="mx-auto max-w-screen-2xl px-6 py-8 md:flex md:items-center md:justify-between lg:px-8">
        <div class="flex flex-wrap justify-center gap-x-6 gap-y-2 md:order-2">
          <router-link to="/about" class="text-sm leading-6 hover:text-rosa transition">
            Sobre Nosotros
          </router-link>
          <router-link to="/terminos" class="text-sm leading-6 hover:text-rosa transition">
            Términos de Uso
          </router-link>
          <router-link to="/contacto" class="text-sm leading-6 hover:text-rosa transition">
            Contacto
          </router-link>
        </div>

        <div class="mt-8 md:order-1 md:mt-0 text-center md:text-left">
          <p class="text-xs leading-5 text-rosa/80">
            &copy; 2026 Temipora. Todos los derechos reservados.
          </p>
          <p class="text-xs leading-5 text-rosa/80 mt-1">
            Contacto:
            <a href="mailto:temiporasupport@gmail.com" class="hover:underline text-crema">
              temiporasupport@gmail.com
            </a>
          </p>
        </div>
      </div>
    </footer>
  </div>
</template>