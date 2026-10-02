<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')
const menuAbierto = ref(false)
const contenedorMenu = ref(null)

const secciones = [
  { nombre: 'Gestión de usuarios', to: '/admin/usuarios', activa: true },
  { nombre: 'Gestión de productos', to: '/admin/tipos/producto', activa: true },
  { nombre: 'Gestión de servicios', to: '/admin/tipos/servicio', activa: true },
  { nombre: 'Gestión de etiquetas', to: '/admin/etiquetas', activa: true },
  { nombre: 'Moderación', to: null, activa: false },
  { nombre: 'Sanciones', to: null, activa: false },
  { nombre: 'Notificaciones', to: null, activa: false },
  { nombre: 'Auditoría', to: null, activa: false },
]

const esActiva = (to) => to && route.path === to

function actualizarFotoPerfil(event) {
  urlFotoPerfil.value = event.detail?.url || ''
}

function alternarMenu() {
  menuAbierto.value = !menuAbierto.value
}

function manejarClickExterior(event) {
  if (contenedorMenu.value && !contenedorMenu.value.contains(event.target)) {
    menuAbierto.value = false
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape') {
    menuAbierto.value = false
  }
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

function irAPerfil() {
  router.push({ name: 'perfil', params: { nombreUsuario: nombreUsuario.value } })
}

function cerrarSesion() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="min-h-screen w-full bg-crema text-cafe flex flex-col">
    <header class="bg-blanco border-b border-rosa sticky top-0 z-20">
      <div class="w-full px-4 sm:px-8 h-10 flex items-center justify-between">
        <router-link to="/" class="text-xl font-normal text-granate">
          Temipora
        </router-link>

        <div ref="contenedorMenu" class="relative">
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

          <div
            v-if="menuAbierto"
            class="absolute right-0 mt-2 w-52 rounded-lg bg-blanco shadow-lg border border-rosa overflow-hidden z-40"
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
              class="w-full text-left px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
              role="menuitem"
              @click="menuAbierto = false; router.push({ name: 'inicio' })"
            >
              Ver plataforma
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
      </div>
    </header>

    <div class="flex flex-1">
      <!-- Sidebar -->
      <aside class="w-60 shrink-0 bg-granate text-crema flex flex-col min-h-[calc(100vh-2.5rem)] sticky top-10 h-[calc(100vh-2.5rem)]">
        <div class="px-5 py-3 border-b border-rosa/20">
          <p class="text-xs uppercase tracking-wider text-rosa/70">Panel de administración</p>
        </div>

        <nav class="flex-1 py-3">
          <div class="space-y-1">
            <template v-for="s in secciones" :key="s.nombre">
              <router-link
                v-if="s.activa"
                :to="s.to"
                class="flex items-center gap-3 px-5 py-2 text-sm transition"
                :class="esActiva(s.to) ? 'bg-rosa/20 text-crema font-medium' : 'text-crema/70 hover:bg-rosa/10 hover:text-crema'"
              >
                <span>{{ s.nombre }}</span>
              </router-link>
              <div
                v-else
                class="flex items-center gap-3 px-5 py-2 text-sm text-crema/30 cursor-not-allowed select-none"
                :title="'Próximamente'"
              >
                <span>{{ s.nombre }}</span>
              </div>
            </template>
          </div>
        </nav>
      </aside>

      <!-- Contenido -->
      <main class="flex-1 min-w-0">
        <router-view />
      </main>
    </div>
  </div>
</template>