<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useSubirImagen } from '../composables/useSubirImagen'
import BrandLogo from '../components/BrandLogo.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

const router = useRouter()
const { abrir } = useSubirImagen()

const token = ref(localStorage.getItem('token') || '')
const rol = ref(localStorage.getItem('rol') || '')
const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')

const menuAbierto = ref(false)
const subirAbierto = ref(false)
const menuMovilAbierto = ref(false)
const contenedorMenu = ref(null)
const contenedorSubir = ref(null)
const contenedorNav = ref(null)

const estaLogueado = computed(() => !!token.value)
const esAdmin = computed(() => rol.value === 'ADMIN')
const esArtista = computed(() => rol.value === 'ARTISTA')

function alternarMenu() {
  menuAbierto.value = !menuAbierto.value
}

function subirImagen() {
  subirAbierto.value = false
  menuMovilAbierto.value = false
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
  if (contenedorNav.value && !contenedorNav.value.contains(event.target)) {
    menuMovilAbierto.value = false
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape') {
    menuAbierto.value = false
    subirAbierto.value = false
    menuMovilAbierto.value = false
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
  menuMovilAbierto.value = false
  router.push({ name: 'perfil', params: { nombreUsuario: nombreUsuario.value } })
}
</script>

<template>
  <div class="min-h-screen w-full flex flex-col bg-crema text-cafe">
    <header class="sticky top-0 z-30 border-b border-rosa/40 bg-crema/85 backdrop-blur-md">
      <div class="mx-auto flex h-14 max-w-screen-2xl items-center justify-between gap-4 px-4 sm:h-16 sm:px-8">
        <!-- Marca y navegación -->
        <div class="flex items-center gap-7">
          <router-link
            to="/"
            class="group inline-flex items-center rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-4 focus-visible:ring-offset-crema"
            aria-label="Temipora, ir al inicio"
          >
            <BrandLogo />
          </router-link>

          <nav class="hidden items-center gap-6 md:flex" aria-label="Navegación principal">
            <router-link
              to="/servicios"
              class="relative text-sm font-medium text-cafe transition hover:text-coral after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-0 after:rounded-full after:bg-coral-vivo after:transition-all after:duration-200 hover:after:w-full focus-visible:text-coral focus-visible:outline-none"
            >
              Explorar
            </router-link>
            <router-link
              v-if="estaLogueado && esArtista"
              to="/mis-servicios"
              class="relative text-sm font-medium text-cafe transition hover:text-coral after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-0 after:rounded-full after:bg-coral-vivo after:transition-all after:duration-200 hover:after:w-full focus-visible:text-coral focus-visible:outline-none"
            >
              Mis servicios
            </router-link>
            <router-link
              v-if="estaLogueado && esAdmin"
              :to="{ name: 'usuarios' }"
              class="relative text-sm font-medium text-cafe transition hover:text-coral after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-0 after:rounded-full after:bg-coral-vivo after:transition-all after:duration-200 hover:after:w-full focus-visible:text-coral focus-visible:outline-none"
            >
              Panel
            </router-link>
          </nav>
        </div>

        <!-- Acciones -->
        <div class="flex items-center gap-2.5 sm:gap-3">
          <ThemeToggle />

          <template v-if="!estaLogueado">
            <router-link
              to="/login"
              class="hidden min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco/60 px-5 text-sm font-semibold text-granate transition hover:border-coral/40 hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 sm:inline-flex"
            >
              Iniciar sesión
            </router-link>
            <router-link
              to="/registrarse"
              class="hidden min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_rgba(194,24,91,0.22)] transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema sm:inline-flex"
            >
              Únete
            </router-link>
          </template>

          <template v-else>
            <!-- Botón Subir (solo artistas) -->
            <div v-if="esArtista" ref="contenedorSubir" class="relative hidden md:block">
              <button
                class="inline-flex min-h-10 items-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_rgba(194,24,91,0.22)] transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
                aria-haspopup="menu"
                :aria-expanded="subirAbierto"
                @click="subirAbierto = !subirAbierto"
              >
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M12 19V5M5 12l7-7 7 7" />
                </svg>
                Subir
                <svg class="h-3.5 w-3.5 transition-transform" :class="subirAbierto ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                  <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clip-rule="evenodd" />
                </svg>
              </button>

              <div
                v-if="subirAbierto"
                class="absolute right-0 z-40 mt-2 w-56 overflow-hidden rounded-2xl border border-rosa/50 bg-blanco p-1.5 shadow-[0_18px_40px_rgba(74,29,46,0.16)]"
                role="menu"
              >
                <button
                  class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  role="menuitem"
                  @click="subirImagen"
                >
                  Imagen
                </button>
                <button
                  disabled
                  class="w-full cursor-not-allowed rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe/40"
                  aria-disabled="true"
                >
                  Producto <span class="text-xs">· Próximamente</span>
                </button>
                <button
                  class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  role="menuitem"
                  @click="subirAbierto = false; router.push('/mis-servicios/crear')"
                >
                  Servicio
                </button>
              </div>
            </div>

            <div ref="contenedorMenu" class="relative">
              <button
                class="group flex min-h-10 items-center gap-2 rounded-full p-0.5 pr-1.5 transition hover:bg-rosa/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
                aria-haspopup="menu"
                :aria-expanded="menuAbierto"
                aria-label="Abrir menú de usuario"
                @click="alternarMenu"
              >
                <img
                  v-if="urlFotoPerfil"
                  :src="urlFotoPerfil"
                  alt=""
                  class="h-8.5 w-8.5 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
                />
                <span
                  v-else
                  class="flex h-8.5 w-8.5 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate shadow-sm ring-1 ring-white/70"
                >
                  {{ (nombreUsuario || '?').charAt(0).toUpperCase() }}
                </span>
                <span class="hidden text-sm font-semibold text-cafe lg:inline">{{ nombreUsuario }}</span>
                <svg class="hidden h-4 w-4 text-cafe/50 transition-transform sm:block" :class="menuAbierto ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                  <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clip-rule="evenodd" />
                </svg>
              </button>

              <div
                v-if="menuAbierto"
                class="absolute right-0 z-40 mt-2 w-56 overflow-hidden rounded-2xl border border-rosa/50 bg-blanco p-1.5 shadow-[0_18px_40px_rgba(74,29,46,0.16)]"
                role="menu"
              >
                <button
                  class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  role="menuitem"
                  @click="menuAbierto = false; irAPerfil()"
                >
                  Perfil
                </button>
                <button
                  v-if="esAdmin"
                  class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  role="menuitem"
                  @click="menuAbierto = false; router.push({ name: 'usuarios' })"
                >
                  Panel de administración
                </button>
                <button
                  class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  role="menuitem"
                  @click="menuAbierto = false; router.push({ name: 'configuracion-perfil' })"
                >
                  Configuración
                </button>
                <button
                  class="mt-1 w-full rounded-xl border-t border-rosa/30 px-3.5 py-2.5 text-left text-sm text-rojizo transition hover:bg-rosa/20"
                  role="menuitem"
                  @click="menuAbierto = false; cerrarSesion()"
                >
                  Cerrar sesión
                </button>
              </div>
            </div>
          </template>

          <!-- Menú móvil -->
          <div ref="contenedorNav" class="relative md:hidden">
            <button
              type="button"
              class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-rosa/60 bg-blanco/70 text-granate transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
              :aria-expanded="menuMovilAbierto"
              aria-label="Abrir menú de navegación"
              @click="menuMovilAbierto = !menuMovilAbierto"
            >
              <svg v-if="!menuMovilAbierto" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="M4 7h16M4 12h16M4 17h16" />
              </svg>
              <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="m6 6 12 12M18 6 6 18" />
              </svg>
            </button>

            <div
              v-if="menuMovilAbierto"
              class="absolute right-0 z-50 mt-3 w-68 origin-top-right rounded-3xl border border-rosa/50 bg-blanco p-2 shadow-[0_18px_40px_rgba(74,29,46,0.18)]"
            >
              <router-link
                to="/servicios"
                class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                @click="menuMovilAbierto = false"
              >
                Explorar
              </router-link>
              <router-link
                v-if="estaLogueado && esArtista"
                to="/mis-servicios"
                class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                @click="menuMovilAbierto = false"
              >
                Mis servicios
              </router-link>
              <router-link
                v-if="estaLogueado && esAdmin"
                :to="{ name: 'usuarios' }"
                class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                @click="menuMovilAbierto = false"
              >
                Panel de administración
              </router-link>

              <div v-if="!estaLogueado" class="mt-1 border-t border-rosa/40 pt-1">
                <router-link
                  to="/login"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  @click="menuMovilAbierto = false"
                >
                  Iniciar sesión
                </router-link>
                <router-link
                  to="/registrarse"
                  class="mt-1 flex w-full items-center justify-center rounded-2xl bg-coral px-4 py-3 text-sm font-semibold text-white transition hover:bg-rojizo"
                  @click="menuMovilAbierto = false"
                >
                  Crear cuenta
                </router-link>
              </div>

              <div v-else class="mt-1 border-t border-rosa/40 pt-1">
                <button
                  type="button"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-left text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  @click="subirImagen"
                >
                  Subir imagen
                </button>
                <button
                  type="button"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-left text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  @click="menuMovilAbierto = false; irAPerfil()"
                >
                  Mi perfil
                </button>
                <button
                  type="button"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-left text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  @click="menuMovilAbierto = false; router.push({ name: 'configuracion-perfil' })"
                >
                  Configuración
                </button>
                <button
                  type="button"
                  class="mt-1 flex w-full items-center rounded-2xl border-t border-rosa/30 px-4 py-3 text-left text-sm font-semibold text-rojizo transition hover:bg-rosa/20"
                  @click="menuMovilAbierto = false; cerrarSesion()"
                >
                  Cerrar sesión
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 w-full">
      <router-view />
    </main>


    
    <footer class="bg-granate text-inverso-contraste mt-auto">
      <div class="mx-auto max-w-screen-2xl px-6 py-8 md:flex md:items-center md:justify-between lg:px-8">
        <div class="flex flex-wrap justify-center gap-x-6 gap-y-2 md:order-2">
          <router-link to="/about" class="text-sm leading-6 hover:text-inverso-suave transition">
            Sobre Nosotros
          </router-link>
          <router-link to="/terminos" class="text-sm leading-6 hover:text-inverso-suave transition">
            Términos de Uso
          </router-link>
          <router-link to="/contacto" class="text-sm leading-6 hover:text-inverso-suave transition">
            Contacto
          </router-link>
        </div>

        <div class="mt-8 md:order-1 md:mt-0 text-center md:text-left">
          <p class="text-xs leading-5 text-inverso-suave/80">
            &copy; 2026 Temipora. Todos los derechos reservados.
          </p>
          <p class="text-xs leading-5 text-inverso-suave/80 mt-1">
            Contacto:
            <a href="mailto:temiporasupport@gmail.com" class="hover:underline text-inverso-contraste">
              temiporasupport@gmail.com
            </a>
          </p>
        </div>
      </div>
    </footer>
  </div>
</template>