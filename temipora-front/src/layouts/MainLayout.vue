<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const token = ref(localStorage.getItem('token') || '')
const rol = ref(localStorage.getItem('rol') || '')
const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')

const menuAbierto = ref(false)

const estaLogueado = computed(() => !!token.value)
const esAdmin = computed(() => rol.value === 'ADMIN')

function actualizarFotoPerfil(event) {
  urlFotoPerfil.value = event.detail?.url || ''
}

onMounted(() => {
  window.addEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
})

onBeforeUnmount(() => {
  window.removeEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
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
      <div class="max-w-screen-2xl mx-auto px-4 sm:px-8 h-12 flex items-center justify-between">
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
            <div class="relative" @mouseenter="menuAbierto = true" @mouseleave="menuAbierto = false">
              <div class="flex items-center gap-2">
                <button class="flex items-center gap-2" @click="irAPerfil">
                  <img
                    v-if="urlFotoPerfil"
                    :src="urlFotoPerfil"
                    alt="Perfil"
                    class="w-9 h-9 rounded-full object-cover border border-rosa"
                  />
                  <span
                    v-else
                    class="w-9 h-9 rounded-full bg-coral/20 text-granate flex items-center justify-center text-sm"
                  >
                    {{ (nombreUsuario || '?').charAt(0).toUpperCase() }}
                  </span>
                  <span class="text-sm text-cafe hidden sm:inline">{{ nombreUsuario }}</span>
                </button>
              </div>

              <div
                v-if="menuAbierto"
                class="absolute right-0 mt-2 w-44 rounded-lg bg-blanco shadow-lg border border-rosa overflow-hidden"
              >
                <button
                  class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
                  @click="irAPerfil"
                >
                  Perfil
                </button>
                <button
                  v-if="esAdmin"
                  class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
                  @click="router.push({ name: 'usuarios' })"
                >
                  Panel de administración
                </button>
                <button
                  class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
                  @click="router.push({ name: 'configuracion-perfil' })"
                >
                  Configuración
                </button>
                <button
                  class="w-full text-left px-4 py-2 text-sm text-rojizo hover:bg-rosa/20 transition"
                  @click="cerrarSesion"
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