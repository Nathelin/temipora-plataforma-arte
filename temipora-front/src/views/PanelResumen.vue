<script setup>
import { computed } from 'vue'

const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const perfilPublico = computed(() =>
  nombreUsuario
    ? { name: 'perfil', params: { nombreUsuario } }
    : { name: 'inicio' },
)

const accesos = [
  {
    titulo: 'Servicios',
    detalle: 'Publicá, pausá y editá los servicios que ofrecés.',
    to: '/panel/servicios',
    icono: 'M2.25 7.125C2.25 6.504 2.754 6 3.375 6h6c.621 0 1.125.504 1.125 1.125v3.75c0 .621-.504 1.125-1.125 1.125h-6a1.125 1.125 0 0 1-1.125-1.125v-3.75ZM14.25 8.625c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-8.25ZM3.75 16.125c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v2.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-2.25Z',
  },
  {
    titulo: 'Solicitudes',
    detalle: 'Revisá los pedidos que recibiste y hacé seguimiento.',
    to: '/panel/solicitudes',
    icono: 'M2.25 13.5h3.86a2.25 2.25 0 0 1 2.012 1.244l.256.512a2.25 2.25 0 0 0 2.013 1.244h3.218a2.25 2.25 0 0 0 2.013-1.244l.256-.512a2.25 2.25 0 0 1 2.013-1.244h3.859m-19.5.338V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18v-4.162c0-.224-.034-.447-.1-.661L19.24 5.338a2.25 2.25 0 0 0-2.15-1.588H6.911a2.25 2.25 0 0 0-2.15 1.588L2.35 13.177a2.25 2.25 0 0 0-.1.661Z',
  },
  {
    titulo: 'Formulario de pedidos',
    detalle: 'Definí qué información pedís en cada comisión.',
    to: '/panel/formulario-pedidos',
    icono: 'M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25Z',
  },
  {
    titulo: 'Términos y condiciones',
    detalle: 'Establecé las reglas de tus servicios y derechos de uso.',
    to: '/panel/terminos',
    icono: 'M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m5.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z',
  },
  {
    titulo: 'Etiquetas',
    detalle: 'Gestioná las etiquetas que usás en tus obras.',
    to: '/panel/etiquetas',
    icono: 'M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z',
  },
  {
    titulo: 'Portfolio',
    detalle: 'Subí y ordená las obras que mostrás al público.',
    to: '/panel/portfolio',
    icono: 'm2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Z',
  },
]
</script>

<template>
  <div>
    <header>
      <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Tu espacio de trabajo</p>
      <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
        Mi panel
      </h1>
      <p class="mt-2 max-w-2xl text-sm leading-relaxed text-cafe/65">
        Desde acá gestionás tus servicios, pedidos y herramientas. Más adelante vas a ver acá un resumen de tu actividad.
      </p>
    </header>

    <div class="mt-6 flex flex-wrap gap-3">
      <router-link
        to="/panel/servicios/crear"
        class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
      >
        Crear servicio
      </router-link>
      <router-link
        :to="perfilPublico"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
      >
        Ver mi perfil público
      </router-link>
    </div>

    <div class="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <router-link
        v-for="acceso in accesos"
        :key="acceso.to"
        :to="acceso.to"
        class="group flex flex-col rounded-2xl border border-rosa/60 bg-blanco p-5 transition hover:-translate-y-0.5 hover:border-coral/40 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)]"
      >
        <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-rosa/25 text-coral">
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" :d="acceso.icono" />
          </svg>
        </span>
        <span class="mt-4 font-display text-lg font-bold text-granate">{{ acceso.titulo }}</span>
        <span class="mt-1 text-sm leading-relaxed text-cafe/65">{{ acceso.detalle }}</span>
        <span class="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-coral transition group-hover:text-rojizo">
          Entrar
          <svg class="h-4 w-4 transition group-hover:translate-x-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 12h14M13 6l6 6-6 6" />
          </svg>
        </span>
      </router-link>
    </div>
  </div>
</template>
