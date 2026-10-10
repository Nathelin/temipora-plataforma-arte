<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="min-h-screen w-full bg-crema text-cafe flex flex-col">
    <header class="bg-blanco border-b border-rosa px-6 py-4 flex items-center justify-between sticky top-0 z-10">
      <h1 class="text-xl font-normal text-granate">Términos y condiciones</h1>
      <button
        class="bg-coral hover:bg-rojizo text-white px-3 py-1.5 rounded-md text-sm transition shadow-sm"
        @click="router.back()"
      >
        Volver
      </button>
    </header>

    <main class="flex-1 px-6 py-8 overflow-auto min-h-screen w-full">
      <article class=" bg-blanco text-cafe rounded-3xl p-8 shadow-xl shadow-rosa/40 leading-relaxed">
        <h2 class="text-lg font-normal text-granate mb-4">Términos y condiciones de Temipora</h2>

        <p class="mb-4 text-sm text-cafe/50">Versión de los términos y condiciones: 0.0.1</p> <!--TODO: conseguir valores del back-->
        <p class="mb-4 text-sm text-cafe/50">Última actualización: 06/09/2026</p>

        <p class="mb-4">
          Este es un texto de ejemplo. Aquí irá el contenido real de los términos y condiciones
          de la plataforma.
        </p>

        <h3 class="font-normal text-granate mb-2 mt-6">1. Aceptación de los términos</h3>
        <p class="mb-4">
          Al utilizar Temipora, aceptás estos términos. Si no estás de acuerdo, no utilices la
          plataforma.
        </p>

        <h3 class="font-normal text-granate mb-2 mt-6">2. Uso de la plataforma</h3>
        <p class="mb-4">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla facilisi. Sed ut purus
          vitae nisl tincidunt tincidunt.
        </p>

        <h3 class="font-normal text-granate mb-2 mt-6">3. Contenido y propiedad intelectual</h3>
        <p class="mb-4">
          Los artistas conservan los derechos sobre sus obras. Al subir contenido, garantizás que
          tenés los derechos necesarios.
        </p>

        <h3 class="font-normal text-granate mb-2 mt-6">4. Responsabilidad</h3>
        <p class="mb-4">
          La plataforma se ofrece "tal cual". No nos hacemos responsables por daños derivados del
          uso de la misma.
        </p>
      </article>
    </main>
  </div>
</template>