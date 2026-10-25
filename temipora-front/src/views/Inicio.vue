<script setup>
import { computed } from 'vue'
import img1 from '../assets/inicio/img1.webp'
import img2 from '../assets/inicio/img2.webp'
import img3 from '../assets/inicio/img3.webp'

const token = localStorage.getItem('token')
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''
const rol = localStorage.getItem('rol') || ''

const estaLogueado = computed(() => !!token)
const esArtista = computed(() => rol === 'ARTISTA')

const perfilDestino = computed(() =>
  nombreUsuario
    ? { name: 'perfil', params: { nombreUsuario } }
    : { name: 'configuracion-perfil' },
)

const ctaFinal = computed(() => {
  if (!estaLogueado.value) {
    return { to: '/registrarse', label: 'Crear mi cuenta' }
  }
  if (esArtista.value) {
    return { to: '/panel/portfolio', label: 'Subir mi primera obra' }
  }
  return { to: '/servicios', label: 'Explorar servicios' }
})
</script>

<template>
  <div class="w-full bg-crema">
    <section class="relative overflow-hidden">
      <div class="pointer-events-none absolute inset-0" aria-hidden="true">
        <div class="absolute -left-24 top-10 h-56 w-56 rounded-full bg-rosa/40 blur-3xl"></div>
        <div class="absolute right-0 top-0 h-72 w-72 rounded-full bg-mantequilla/50 blur-3xl"></div>
        <div class="absolute bottom-0 left-1/3 h-40 w-40 rounded-full bg-durazno/50 blur-2xl"></div>
      </div>

      <div class="relative mx-auto grid items-center gap-10 px-6 pb-16 pt-10 sm:px-8 sm:pb-20 sm:pt-14 lg:grid-cols-[1.3fr_0.95fr] lg:gap-16 lg:px-10 lg:py-20">
        <div class="max-w-2xl">
          <span class="inline-block -rotate-2 rounded-full bg-mantequilla px-4 py-2 text-sm font-bold text-tinta shadow-sm">
            Hecho a mano, con cariño
          </span>

          <h1 class="mt-6 font-display text-[46px] font-extrabold leading-[0.95] tracking-[-0.04em] text-granate sm:text-6xl lg:text-[76px]">
            Arte que se
            <span class="relative inline-block text-coral">
              siente
              <svg class="absolute -bottom-2 left-0 w-full" viewBox="0 0 200 14" preserveAspectRatio="none" aria-hidden="true">
                <path d="M2 8 Q25 0 50 8 T100 8 T150 8 T198 8" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" />
              </svg>
            </span>
            <br class="hidden sm:block" />
            y se comparte.
          </h1>

          <p class="mt-6 max-w-xl text-lg leading-relaxed text-cafe sm:text-xl">
            Descubrí artistas, encargá comisiones y compartí tu trabajo con una comunidad que ama crear.
          </p>

          <div class="mt-8 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
            <router-link
              to="/servicios"
              class="inline-flex min-h-13 items-center justify-center gap-2 rounded-full bg-accion px-7 text-base font-bold text-white shadow-[0_12px_28px_rgba(74,29,46,0.18)] transition hover:-translate-y-0.5 hover:bg-accion-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
            >
              Explorar servicios
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </router-link>

            <router-link
              v-if="!estaLogueado"
              to="/registrarse"
              class="inline-flex min-h-13 items-center justify-center rounded-full bg-durazno px-7 text-base font-bold text-tinta transition hover:-translate-y-0.5 hover:bg-coral hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
            >
              Crear cuenta
            </router-link>

            <router-link
              v-else
              :to="perfilDestino"
              class="inline-flex min-h-13 items-center justify-center rounded-full bg-durazno px-7 text-base font-bold text-tinta transition hover:-translate-y-0.5 hover:bg-coral hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
            >
              Mi perfil
            </router-link>
          </div>

          <p v-if="!estaLogueado" class="mt-4 text-sm text-cafe/70">
            ¿Ya tenés cuenta?
            <router-link to="/login" class="font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo">
              Iniciá sesión
            </router-link>
          </p>


        </div>

        <div class="relative mx-auto h-90 w-full max-w-135 sm:h-110 lg:h-130">
          <div class="absolute left-[8%] top-[8%] h-[78%] w-[78%] rounded-[46%_54%_50%_50%] bg-durazno/70" aria-hidden="true"></div>
          <div class="absolute bottom-[8%] right-[4%] h-24 w-24 rounded-full bg-rosa/70" aria-hidden="true"></div>

          <div class="absolute left-0 top-0 w-[62%] -rotate-6 rounded-3xl border-4 border-blanco bg-blanco p-2 shadow-[0_22px_50px_rgba(74,29,46,0.2)] transition duration-300 hover:rotate-0">
            <img :src="img3" alt="Ilustración digital de una figura entre peces y estrellas" class="aspect-4/3 w-full rounded-2xl object-cover" />
          </div>

          <div class="absolute right-0 top-[22%] w-[55%] rotate-6 rounded-3xl border-4 border-blanco bg-blanco p-2 shadow-[0_22px_50px_rgba(74,29,46,0.2)] transition duration-300 hover:rotate-0">
            <img :src="img1" alt="Ilustración colorida de un personaje con mariposas" class="aspect-4/5 w-full rounded-2xl object-cover" />
          </div>

          <div class="absolute bottom-0 left-[22%] w-[46%] rotate-2 rounded-[22px] border-4 border-blanco bg-blanco p-2 shadow-[0_18px_40px_rgba(74,29,46,0.18)] transition duration-300 hover:rotate-0">
            <img :src="img2" alt="Ilustración cálida de un personaje con capa" class="aspect-4/3 w-full rounded-[14px] object-cover" />
          </div>

          <div class="absolute right-0 top-2 rotate-6 rounded-[18px_18px_18px_4px] bg-coral px-4 py-3 text-sm font-bold text-white shadow-lg">
            ¡Comisiones abiertas!
          </div>

          <svg class="absolute bottom-[22%] left-0 h-14 w-14 text-coral-vivo" viewBox="0 0 70 70" fill="currentColor" aria-hidden="true">
            <path d="M35 4 L41 28 L66 35 L41 42 L35 66 L29 42 L4 35 L29 28Z" />
          </svg>

        </div>
      </div>

      <a
        href="#descubrir"
        class="absolute bottom-5 left-1/2 hidden -translate-x-1/2 flex-col items-center gap-1 text-cafe/60 transition hover:text-coral sm:flex"
        aria-label="Descubrir más contenido"
      >
        <span class="text-xs uppercase tracking-[0.2em]">Descubrí</span>
        <span class="text-2xl leading-none" aria-hidden="true">↓</span>
      </a>
    </section>

    <section id="descubrir" class="scroll-mt-24 bg-superficie py-16 sm:py-24">
      <div class="mx-auto flex max-w-screen-2xl flex-col items-center gap-12 px-4 sm:px-8 lg:flex-row lg:gap-20">
        <div class="w-full max-w-xl">
          <div class="relative">
            <div class="absolute -left-4 -top-4 h-full w-full rounded-4xl border-2 border-coral/30" aria-hidden="true"></div>
            <div class="relative overflow-hidden rounded-4xl bg-superficie p-3 shadow-[0_24px_60px_rgba(74,29,46,0.16)]">
              <img :src="img1" alt="Obra digital llena de color y texturas" class="aspect-4/3 w-full rounded-3xl object-cover" />
            </div>
            <div class="absolute -bottom-5 -right-3 rotate-2 rounded-2xl bg-mantequilla px-4 py-3 font-display text-sm font-extrabold text-tinta shadow-lg">
              Arte con identidad
            </div>
          </div>
        </div>

        <div class="flex-1">
          <span class="inline-block -rotate-1 rounded-full bg-rosa/50 px-4 py-1.5 text-sm font-bold text-granate">Explorá sin límites</span>
          <h2 class="mt-5 font-display text-4xl font-extrabold tracking-[-0.03em] text-granate sm:text-5xl">Descubrí nuevos artistas</h2>
          <p class="mt-5 text-lg leading-relaxed text-cafe/75">
            Recorré portfolios, encontrá estilos que te inspiren y conectá con artistas de toda la comunidad para hacer realidad tus ideas.
          </p>

          <ul class="mt-7 space-y-3">
            <li class="flex items-start gap-3 text-cafe">
              <span class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-rosa/60 text-granate" aria-hidden="true">
                <svg class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m4 10 4 4 8-8" /></svg>
              </span>
              Portfolios con personalidad y proceso creativo.
            </li>
            <li class="flex items-start gap-3 text-cafe">
              <span class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-mantequilla text-tinta" aria-hidden="true">
                <svg class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m4 10 4 4 8-8" /></svg>
              </span>
              Servicios con precios, tiempos y alcance claros.
            </li>
            <li class="flex items-start gap-3 text-cafe">
              <span class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-durazno text-tinta" aria-hidden="true">
                <svg class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m4 10 4 4 8-8" /></svg>
              </span>
              Una comunidad que valora el trabajo hecho a mano.
            </li>
          </ul>

          <router-link
            to="/servicios"
            class="mt-8 inline-flex min-h-12 items-center justify-center rounded-full bg-coral px-7 text-base font-bold text-white shadow-[0_10px_24px_var(--sombra-acento)] transition hover:-translate-y-0.5 hover:bg-rojizo"
          >
            Ver artistas y servicios
          </router-link>
        </div>
      </div>
    </section>

    <section class="border-y border-rosa/30 bg-superficie py-16 sm:py-24">
      <div class="mx-auto flex max-w-screen-2xl flex-col-reverse items-center gap-12 px-4 sm:px-8 lg:flex-row lg:gap-20">
        <div class="flex-1">
          <span class="inline-block rotate-1 rounded-full bg-durazno px-4 py-1.5 text-sm font-bold text-tinta">Sin sorpresas</span>
          <h2 class="mt-5 font-display text-4xl font-extrabold tracking-[-0.03em] text-granate sm:text-5xl">Comisiones seguras y sencillas</h2>
          <p class="mt-5 text-lg leading-relaxed text-cafe/75">
            Olvidate de los malentendidos. La plataforma te ayuda a acordar cada detalle para que artistas y clientes disfruten del proceso.
          </p>

          <div class="mt-8 grid gap-4 sm:grid-cols-2">
            <div class="rounded-2xl border border-borde/60 bg-crema p-5">
              <p class="font-display text-lg font-bold text-granate">Precios claros</p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">Cada servicio muestra su valor y condiciones desde el inicio.</p>
            </div>
            <div class="rounded-2xl border border-borde/60 bg-crema p-5">
              <p class="font-display text-lg font-bold text-granate">Seguimiento</p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">Consultá el estado del pedido y coordiná los detalles con el artista.</p>
            </div>
            <div class="rounded-2xl border border-borde/60 bg-crema p-5">
              <p class="font-display text-lg font-bold text-granate">Acuerdos protegidos</p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">La comunidad cuida que ambas partes reciban lo que esperan.</p>
            </div>
            <div class="rounded-2xl border border-borde/60 bg-crema p-5">
              <p class="font-display text-lg font-bold text-granate">Hecho para crear</p>
              <p class="mt-1 text-sm leading-relaxed text-cafe/70">Menos fricción, más tiempo para lo importante: el arte.</p>
            </div>
          </div>
        </div>

        <div class="w-full max-w-xl">
          <div class="relative">
            <div class="absolute -right-4 -top-4 h-full w-full rounded-4xl border-2 border-mantequilla" aria-hidden="true"></div>
            <div class="relative overflow-hidden rounded-4xl bg-crema p-3 shadow-[0_24px_60px_rgba(74,29,46,0.16)]">
              <img :src="img2" alt="Ilustración cálida de un personaje con una gran capa" class="aspect-4/3 w-full rounded-3xl object-cover" />
            </div>
            <div class="absolute -bottom-5 left-4 -rotate-2 rounded-2xl bg-rosa px-4 py-3 font-display text-sm font-extrabold text-granate shadow-lg">
              Cada detalle, claro
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="bg-crema px-4 py-16 sm:px-8 sm:py-24">
      <div class="relative mx-auto max-w-screen-2xl overflow-hidden rounded-[36px] bg-granate px-6 py-16 text-center shadow-[0_30px_70px_rgba(74,29,46,0.25)] sm:px-12 sm:py-20">
        <div class="pointer-events-none absolute inset-0" aria-hidden="true">
          <div class="absolute -left-10 top-8 h-40 w-40 rounded-full bg-coral/30 blur-2xl"></div>
          <div class="absolute -bottom-16 right-10 h-56 w-56 rounded-full bg-coral-vivo/20 blur-3xl"></div>
          <svg class="absolute left-8 top-8 h-16 w-16 text-coral-vivo" viewBox="0 0 70 70" fill="currentColor">
            <path d="M35 4 L41 28 L66 35 L41 42 L35 66 L29 42 L4 35 L29 28Z" />
          </svg>
          <svg class="absolute bottom-10 right-8 h-20 w-20 text-coral" viewBox="0 0 80 80" fill="currentColor">
            <circle cx="40" cy="40" r="40" />
            <circle cx="30" cy="32" r="10" fill="currentColor" class="text-mantequilla" />
          </svg>
        </div>

        <div class="relative mx-auto flex max-w-3xl flex-col items-center">
          <span class="rounded-full border border-blanco/20 bg-blanco/10 px-4 py-1.5 text-sm font-bold text-durazno">Para artistas</span>
          <h2 class="mt-6 font-display text-4xl font-extrabold leading-[1.05] tracking-[-0.03em] text-inverso-contraste sm:text-5xl lg:text-6xl">
            ¿Dibujás, pintás o creás? Subilo.
          </h2>
          <p class="mt-5 max-w-xl text-lg leading-relaxed text-durazno">
            Abrí tu portfolio, recibí comisiones y compartí tus obras con quienes las valoran.
          </p>
          <router-link
            :to="ctaFinal.to"
            class="mt-8 inline-flex min-h-13.5 items-center justify-center rounded-full bg-mantequilla px-8 text-base font-extrabold text-tinta shadow-lg transition hover:-translate-y-0.5 hover:bg-coral hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-mantequilla focus-visible:ring-offset-2 focus-visible:ring-offset-granate"
          >
            {{ ctaFinal.label }}
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>
