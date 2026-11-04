<script setup>
import { ref } from 'vue'

defineProps({
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

const PLACEHOLDERS = {
  informacionGeneral: 'Antes de comisionar, contame tu idea con la mayor cantidad de detalles posible (personajes, estilo, colores, referencias). Te confirmo si puedo tomarlo y el presupuesto final.',
  politicaRevisiones: 'Incluyo hasta 2 rondas de revisiones (boceto y color). Cambios grandes fuera de esas etapas pueden tener un costo extra.',
  politicaCancelaciones: 'Si cancelás antes de empezar, devuelvo el 100%. Si el pedido ya está en boceto, devuelvo el 50%. Con el lineart o color avanzado no hay reembolso.',
  usoArtista: 'Me reservo el derecho de mostrar la obra terminada en mi portafolio y redes, salvo que acordemos lo contrario.',
  usoCliente: 'El uso por defecto es personal. Para uso comercial (contenido o merchandising) hay un costo extra detallado en los derechos.',
}

const modal = ref({ abierto: false, titulo: '', descripcion: '' })

function abrirDescripcion(titulo, descripcion) {
  modal.value = { abierto: true, titulo, descripcion }
}
</script>

<template>
  <div class="space-y-8">
    <div class="space-y-6">
      <div v-for="s in secciones" :key="s.key">
        <label class="block text-sm text-cafe mb-2">{{ s.label }}</label>
        <textarea
          v-model="datos.form[s.key]"
          rows="3"
          :placeholder="PLACEHOLDERS[s.key]"
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
        ></textarea>
      </div>

      <div class="border-t border-rosa/40 pt-6">
        <h3 class="text-base text-granate mb-1">¿Qué dibujo?</h3>
        <p class="text-sm text-cafe/60 mb-3">Contale al cliente qué temas, estilos o personajes hacés. Escribilo como una lista simple.</p>
        <textarea
          v-model="datos.form.queDibuja"
          rows="4"
          maxlength="2000"
          placeholder="Ej.: OCs, fanart, ships, furries/anthro, chibi, personajes humanos, fondos simples, gore leve."
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
        ></textarea>
      </div>

      <div>
        <h3 class="text-base text-granate mb-1">¿Qué no dibujo?</h3>
        <p class="text-sm text-cafe/60 mb-3">Sé claro con lo que no hacés para evitar pedidos que no puedas tomar.</p>
        <textarea
          v-model="datos.form.queNoDibuja"
          rows="4"
          maxlength="2000"
          placeholder="Ej.: mecha, gore explícito, contenido sexual, contenido con menores, personas reales, franquicias que no conozco."
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
        ></textarea>
      </div>

    </div>

    <div>
      <h3 class="text-base text-granate mb-1">Derechos de uso</h3>
      <p class="text-sm text-cafe/60 mb-4">Elegí qué permitís y, si cobrás extra, el porcentaje sobre el precio base. El cliente podrá elegir estos derechos al solicitar.</p>
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
