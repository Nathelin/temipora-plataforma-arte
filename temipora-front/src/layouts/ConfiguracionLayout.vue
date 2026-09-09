<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const secciones = [
  { nombre: 'Perfil', to: '/configuracion/perfil' },
  { nombre: 'Privacidad y contenido', to: '/configuracion/privacidad' },
  { nombre: 'Términos de uso', to: '/configuracion/terminos' },
  { nombre: 'Cuenta y seguridad', to: '/configuracion/cuenta' },
]

const esActiva = (to) => route.path === to
</script>

<template>
  <div class="w-full bg-crema text-cafe">
    <div class="max-w-screen-2xl mx-auto px-4 sm:px-8 py-8 lg:flex lg:gap-8">
      <!-- Sidebar -->
      <aside class="lg:w-64 shrink-0 mb-6 lg:mb-0">
        <h1 class="text-xl font-normal text-granate mb-4">Configuración</h1>
        <nav class="space-y-1">
          <router-link
            v-for="s in secciones"
            :key="s.to"
            :to="s.to"
            class="block px-4 py-2 rounded-lg text-sm transition"
            :class="esActiva(s.to) ? 'bg-rosa/40 text-granate font-medium' : 'text-cafe hover:bg-rosa/20'"
          >
            {{ s.nombre }}
          </router-link>
        </nav>
      </aside>

      <!-- Contenido -->
      <div class="flex-1 min-w-0">
        <router-view />
      </div>
    </div>
  </div>
</template>