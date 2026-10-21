<script setup>
import { ref } from 'vue'

const props = defineProps({
  temasCatalogo: { type: Array, default: () => [] },
  derechosCatalogo: { type: Array, default: () => [] },
})

const datos = defineModel({ type: Object, required: true })

const secciones = [
  { key: 'informacionGeneral', label: 'Información general' },
  { key: 'politicaRevisiones', label: 'Política de revisiones' },
  { key: 'politicaCancelaciones', label: 'Política de cancelaciones' },
  { key: 'usoArtista', label: 'Uso del artista' },
  { key: 'usoCliente', label: 'Uso del cliente' },
]

const PLANTILLA = {
  informacionGeneral: 'Antes de comisionar, contame tu idea con la mayor cantidad de detalles posible (personajes, estilo, colores, referencias). Te confirmo si puedo tomarlo y el presupuesto final.',
  politicaRevisiones: 'Incluyo hasta 2 rondas de revisiones (boceto y color). Cambios grandes fuera de esas etapas pueden tener un costo extra.',
  politicaCancelaciones: 'Si cancelás antes de empezar, devuelvo el 100%. Si el pedido ya está en boceto, devuelvo el 50%. Con el lineart o color avanzado no hay reembolso.',
  usoArtista: 'Me reservo el derecho de mostrar la obra terminada en mi portafolio y redes, salvo que acordemos lo contrario.',
  usoCliente: 'El uso por defecto es personal. Para uso comercial (contenido o merchandising) hay un costo extra detallado en los derechos.',
}

const OPCIONES_ESTADO = [
  { valor: 'SI', texto: 'Sí' },
  { valor: 'NO', texto: 'No' },
  { valor: 'CONSULTAR', texto: 'Consultar' },
]

const modal = ref({ abierto: false, titulo: '', descripcion: '' })

function siguienteUid() {
  return datos.value.temasPersonalizados.reduce(
    (max, t) => Math.max(max, Number(t.uid) || 0),
    0,
  ) + 1
}

function setEstado(idTema, opcion) {
  const actual = datos.value.estadosTemas[idTema]
  datos.value.estadosTemas[idTema] = actual === opcion ? '' : opcion
}

function setEstadoPersonalizado(tema, opcion) {
  tema.estado = tema.estado === opcion ? '' : opcion
}

function agregarTema() {
  datos.value.temasPersonalizados.push({ uid: siguienteUid(), nombre: '', estado: '' })
}

function quitarTema(uidTema) {
  datos.value.temasPersonalizados = datos.value.temasPersonalizados.filter((t) => t.uid !== uidTema)
}

function chipActivo(valor) {
  if (valor === 'SI') return 'bg-green-600 text-white'
  if (valor === 'NO') return 'bg-rojizo text-white'
  return 'bg-coral text-white'
}

function abrirDescripcion(titulo, descripcion) {
  modal.value = { abierto: true, titulo, descripcion }
}

function usarPlantilla() {
  for (const [key, texto] of Object.entries(PLANTILLA)) {
    if (!datos.value.form[key].trim()) datos.value.form[key] = texto
  }
}
</script>

<template>
  <div class="space-y-8">
    <div class="flex justify-end">
      <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="usarPlantilla">
        Usar plantilla sugerida
      </button>
    </div>

    <div class="space-y-6">
      <div v-for="s in secciones" :key="s.key">
        <label class="block text-sm text-cafe mb-2">{{ s.label }}</label>
        <textarea
          v-model="datos.form[s.key]"
          rows="3"
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
        ></textarea>
      </div>

      <div>
        <label class="block text-sm text-cafe mb-2">Tiempo estimado de entrega</label>
        <input
          v-model="datos.form.tiempoEntrega"
          type="text"
          maxlength="120"
          placeholder="Ej.: 2 a 4 semanas"
          class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        />
      </div>
    </div>

    <div>
      <h3 class="text-base text-granate mb-1">Derechos de uso</h3>
      <p class="text-sm text-cafe/60 mb-4">Elegí qué permitís y, si cobrás extra, el porcentaje sobre el precio base.</p>
      <div class="space-y-3">
        <div v-for="d in derechosCatalogo" :key="d.idDerechoTermino" class="rounded-lg border border-rosa/50 px-4 py-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="flex items-center gap-2">
              <span class="text-sm text-cafe">{{ d.nombre }}</span>
              <button
                v-if="d.descripcion"
                type="button"
                class="flex h-5 w-5 items-center justify-center rounded-full text-cafe/40 hover:bg-rosa/20 hover:text-cafe transition"
                :aria-label="`Qué incluye ${d.nombre}`"
                @click="abrirDescripcion(d.nombre, d.descripcion)"
              >
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" />
                </svg>
              </button>
            </div>

            <div v-if="d.codigo === 'PERSONAL'" class="rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs text-green-700">
              Incluido
            </div>
            <div v-else-if="datos.estadoDerechos[d.idDerechoTermino]" class="flex items-center gap-3">
              <label class="flex items-center gap-2 text-sm text-cafe cursor-pointer">
                <input v-model="datos.estadoDerechos[d.idDerechoTermino].disponible" type="checkbox" class="accent-coral" />
                <span>Ofrezco</span>
              </label>
              <div v-if="datos.estadoDerechos[d.idDerechoTermino].disponible" class="flex items-center gap-1">
                <span class="text-sm text-cafe/60">+</span>
                <input
                  v-model.number="datos.estadoDerechos[d.idDerechoTermino].porcentajeExtra"
                  type="number"
                  min="0"
                  max="1000"
                  class="w-20 rounded-lg border-0 py-1.5 px-2 text-sm text-cafe text-right shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
                />
                <span class="text-sm text-cafe/60">%</span>
              </div>
            </div>
          </div>
          <p v-if="d.requierePiezaUnica" class="mt-1 text-xs text-cafe/50">Solo aplica a piezas únicas (adoptables o pintura tradicional única).</p>
        </div>
      </div>
    </div>

    <div>
      <h3 class="text-base text-granate mb-1">¿Qué hago y qué no?</h3>
      <p class="text-sm text-cafe/60 mb-4">Marcá tu disponibilidad para cada tema y sumá los que quieras.</p>
      <div class="space-y-3">
        <div v-for="t in temasCatalogo" :key="t.idTemaTermino" class="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-rosa/50 px-4 py-3.5">
          <div class="flex items-center gap-2">
            <span class="text-sm text-cafe">{{ t.nombre }}</span>
            <button
              v-if="t.descripcion"
              type="button"
              class="flex h-5 w-5 items-center justify-center rounded-full text-cafe/40 hover:bg-rosa/20 hover:text-cafe transition"
              :aria-label="`Qué significa ${t.nombre}`"
              @click="abrirDescripcion(t.nombre, t.descripcion)"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" />
              </svg>
            </button>
          </div>
          <div class="inline-flex rounded-md border border-rosa p-0.5 text-xs">
            <button
              v-for="op in OPCIONES_ESTADO"
              :key="op.valor"
              type="button"
              class="rounded px-2.5 py-1 transition"
              :class="datos.estadosTemas[t.idTemaTermino] === op.valor ? chipActivo(op.valor) : 'text-cafe/60 hover:bg-rosa/20'"
              @click="setEstado(t.idTemaTermino, op.valor)"
            >
              {{ op.texto }}
            </button>
          </div>
        </div>
      </div>

      <div v-for="t in datos.temasPersonalizados" :key="t.uid" class="mt-2 flex flex-wrap items-center gap-3 rounded-lg border border-rosa/50 bg-rosa/5 px-4 py-2.5">
        <input
          v-model="t.nombre"
          type="text"
          maxlength="120"
          placeholder="Tema propio"
          class="min-w-40 flex-1 rounded-lg border-0 py-1.5 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        />
        <div class="inline-flex rounded-md border border-rosa p-0.5 text-xs">
          <button
            v-for="op in OPCIONES_ESTADO"
            :key="op.valor"
            type="button"
            class="rounded px-2.5 py-1 transition"
            :class="t.estado === op.valor ? chipActivo(op.valor) : 'text-cafe/60 hover:bg-rosa/20'"
            @click="setEstadoPersonalizado(t, op.valor)"
          >
            {{ op.texto }}
          </button>
        </div>
        <button type="button" class="text-cafe/50 hover:text-rojizo transition" aria-label="Quitar tema" @click="quitarTema(t.uid)">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <button type="button" class="mt-2 text-sm text-coral hover:text-rojizo transition" @click="agregarTema">
        + Agregar tema propio
      </button>
    </div>

    <div>
      <label class="block text-sm text-cafe mb-2">Nota sobre precios (opcional)</label>
      <input
        v-model="datos.form.pagoExtra"
        type="text"
        maxlength="250"
        placeholder="Ej.: ajustes por complejidad, urgencias, etc."
        class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
      />
    </div>

    <div v-if="modal.abierto" class="fixed inset-0 z-[70] flex items-center justify-center bg-granate/40 px-4" @click.self="modal.abierto = false">
      <div class="w-full max-w-md bg-blanco rounded-2xl shadow-xl p-6">
        <h3 class="text-lg text-granate mb-2">{{ modal.titulo }}</h3>
        <p class="text-sm text-cafe/80 whitespace-pre-wrap">{{ modal.descripcion }}</p>
        <div class="mt-6 flex justify-end">
          <button type="button" class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm" @click="modal.abierto = false">
            Entendido
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
