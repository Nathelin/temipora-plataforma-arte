<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const estaLogueado = !!localStorage.getItem('token')

import img1 from '../assets/inicio/img1.webp'
import img2 from '../assets/inicio/img2.webp'
import img3 from '../assets/inicio/img3.webp'

</script>

<template>
  <div class="w-full">
    <!-- Hero Section -->
    <section class="relative min-h-[72svh] overflow-hidden bg-granate border-b border-rosa">
      <div class="max-w-screen-2xl min-h-[72svh] mx-auto px-4 sm:px-8 py-24 sm:py-32 flex flex-col items-center justify-center text-center">

        <h1 class="text-5xl sm:text-7xl lg:text-8xl font-medium leading-tight text-rosa mb-6">
          Bienvenido a Temipora
        </h1>
        <p class="text-lg sm:text-2xl text-durazno mx-auto">
          Un espacio por y para amantes del arte.
        </p>
        
        <template v-if="!estaLogueado">
          <div class="pt-10 sm:pt-14 flex flex-col sm:flex-row items-center justify-center gap-4">
            <router-link
              to="/registrarse"
              class="w-full sm:w-auto px-8 py-3.5 rounded-md text-white bg-coral hover:bg-rojizo transition shadow-sm text-lg font-medium"
            >
              Únete a la comunidad
            </router-link>
            <router-link
              to="/login"
              class="w-full sm:w-auto px-8 py-3.5 rounded-md text-granate border border-rosa bg-blanco hover:bg-rosa/20 transition text-lg"
            >
              Iniciar sesión
            </router-link>
          </div>
        </template>
      </div>
      <a
        href="#descubrir"
        class="absolute bottom-5 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1 text-durazno/80 hover:text-durazno transition"
        aria-label="Descubrir más contenido"
      >
        <span class="text-xs uppercase tracking-[0.2em]">Descubrí</span>
        <span class="text-2xl leading-none" aria-hidden="true">↓</span>
      </a>
    </section>

    <!-- Sección 1: Descubrir -->
    <section id="descubrir" class="py-20 sm:py-28 bg-blanco">
      <div class="mx-auto px-4 sm:px-8 flex flex-col md:flex-row items-center gap-12">
        <div class="flex-1 space-y-6">
          <h2 class="text-3xl sm:text-4xl font-medium text-granate">Descubre nuevos artistas</h2>
          <p class="text-lg text-cafe/70 mx-auto mb-8">
            Explora portfolios y enamórate de distintos estilos de dibujo. Conecta con artistas de todo el mundo para que todas tus ideas se hagan realidad.
          </p>
        </div>
        <!-- Bloque visual (Placeholder para futura imagen o ilustración) -->
        <div class="flex-1 w-full aspect-video md:aspect-square max-h-100 bg-rosa/20 bg-cover bg-no-repeat rounded-2xl border border-rosa flex items-center justify-center shadow-sm"
        :style="{ backgroundImage: `url(${img1})` }">
        </div>
      </div>
    </section>

    <!-- Sección 2: Comisiones (Invertida) -->
    <section class="py-20 sm:py-28 bg-rosa/10 border-y border-rosa/30">
      <div class="mx-auto px-4 sm:px-8 flex flex-col md:flex-row-reverse items-center gap-12">
        <div class="flex-1 space-y-6">
          <h2 class="text-3xl sm:text-4xl font-medium text-granate">Comisiones seguras y sencillas</h2>
          <p class="text-lg text-cafe/70 leading-relaxed">
            Olvidate de los malentendidos. Nos encargamos de proteger cada transacción, asegurando que los artistas reciban su pago y los clientes obtengan lo que pidieron.
          </p>
        </div>
        <!-- Bloque visual -->
        <div class="flex-1 w-full aspect-video md:aspect-square max-h-100 bg-blanco bg-cover bg-center bg-no-repeat rounded-2xl border border-rosa flex items-center justify-center shadow-sm"
        :style="{ backgroundImage: `url(${img2})` }">
        </div>
      </div>
    </section>

    <!-- Sección 3: Comunidad/Crear -->
    <section class="py-20 sm:py-28 bg-blanco">
      <div class="mx-auto px-4 sm:px-8 flex flex-col md:flex-row items-center gap-12">
        <div class="flex-1 space-y-6">
          <h2 class="text-3xl sm:text-4xl font-medium text-granate">Potenciá tu carrera artística</h2>
          <p class="text-lg text-cafe/70 leading-relaxed">
            Mostrá tu arte, definí tus términos de servicio, organizá tus pedidos y cobrá por tu talento. Te damos las herramientas necesarias para que te enfoques en lo que mejor hacés: crear.
          </p>
          <div v-if="!estaLogueado" class="pt-4">
             <router-link
              to="/registrarse"
              class="inline-block px-6 py-3 rounded-md text-granate border border-coral bg-coral/10 hover:bg-coral/20 transition"
            >
              Crea una cuenta
            </router-link>
          </div>
        </div>
        <!-- Bloque visual -->
        <div class="flex-1 w-full aspect-video md:aspect-square max-h-100 bg-rosa/20 bg-cover bg-center bg-no-repeat rounded-2xl border border-rosa flex items-center justify-center shadow-sm"
        :style="{ backgroundImage: `url(${img3})` }">
        </div>
      </div>
    </section>
  </div>
</template>