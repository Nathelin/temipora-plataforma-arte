<script setup>
import { computed } from 'vue'
import GrupoFiltro from './GrupoFiltro.vue'
import FiltroCheckbox from './FiltroCheckbox.vue'
import FiltroRangoPrecio from './FiltroRangoPrecio.vue'
import FiltroEtiquetas from './FiltroEtiquetas.vue'
import {
  CONTENIDOS_SENSIBLES,
  ORIGENES,
  ORDENES,
  cantidadActivos,
} from '../../servicios/filtrosProducto'

const props = defineProps({
  estado: { type: Object, required: true },
  tipos: { type: Array, default: () => [] },
  medios: { type: Array, default: () => [] },
  etiquetas: { type: Array, default: () => [] },
  categorias: { type: Array, default: () => [] },
  moneda: { type: String, default: 'ARS' },
  facetas: { type: Object, default: null },
  cargando: { type: Boolean, default: false },
})

const emit = defineEmits(['actualizar', 'limpiar'])

const activos = computed(() => cantidadActivos(props.estado))

const opcionesTipos = computed(() => (props.tipos || []).map((t) => ({
  valor: String(t.idTipoProducto),
  texto: t.nombre,
})))

const opcionesMedios = computed(() => (props.medios || []).map((m) => ({
  valor: String(m.idMedio),
  texto: m.nombre,
})))

const opcionesSensibles = computed(() => CONTENIDOS_SENSIBLES.map((c) => ({
  valor: c.valor,
  texto: c.texto,
})))

const conteosTipos = computed(() => props.facetas?.tipos || {})
const conteosMedios = computed(() => props.facetas?.medios || {})
const conteosEtiquetas = computed(() => props.facetas?.etiquetas || {})
const conteosContenidos = computed(() => props.facetas?.contenidosSensibles || {})

function actualizar(cambios) {
  emit('actualizar', cambios)
}

function cambiarLista(clave, valores) {
  actualizar({ [clave]: valores })
}
</script>

<template>
  <div class="rounded-[22px] border border-borde/60 bg-superficie">
    <div class="flex items-center justify-between gap-2 border-b border-borde/40 px-4 py-3">
      <h2 class="font-display text-base font-bold text-granate">Filtros</h2>
      <button
        v-if="activos > 0"
        type="button"
        class="text-xs font-semibold text-coral transition hover:text-rojizo"
        @click="emit('limpiar')"
      >
        Limpiar ({{ activos }})
      </button>
    </div>

    <div class="px-4" :class="cargando ? 'opacity-60 transition-opacity' : ''">
      <GrupoFiltro
        titulo="Búsqueda"
        :abierto="true"
        :activos="(estado.titulo ? 1 : 0) + (estado.nombreArtista ? 1 : 0) + (estado.textoLibre ? 1 : 0)"
      >
        <div class="space-y-2">
          <input
            :value="estado.textoLibre"
            type="search"
            placeholder="Buscar por título o descripción..."
            class="w-full rounded-lg border-0 bg-crema px-3 py-2 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            @input="actualizar({ textoLibre: $event.target.value })"
          />
          <input
            :value="estado.nombreArtista"
            type="search"
            placeholder="Filtrar por artista (@usuario)"
            class="w-full rounded-lg border-0 bg-crema px-3 py-2 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            @input="actualizar({ nombreArtista: $event.target.value })"
          />
          <label class="flex items-center justify-between gap-2 rounded-lg px-1.5 py-1.5 text-sm text-cafe">
            <span>Ordenar por</span>
            <select
              :value="estado.orden"
              class="rounded-lg border-0 bg-crema px-2 py-1.5 text-sm font-semibold text-cafe ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              @change="actualizar({ orden: $event.target.value })"
            >
              <option v-for="o in ORDENES" :key="o.valor" :value="o.valor">{{ o.texto }}</option>
            </select>
          </label>
        </div>
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Publicación"
        :abierto="true"
        :activos="estado.origen !== 'TODAS' ? 1 : 0"
      >
        <FiltroCheckbox
          :opciones="ORIGENES"
          :seleccionados="estado.origen !== 'TODAS' ? [estado.origen] : []"
          @cambiar="(valores) => actualizar({ origen: valores[0] || 'TODAS' })"
        />
      </GrupoFiltro>

      <GrupoFiltro
        v-if="opcionesTipos.length"
        titulo="Tipo de producto"
        :abierto="true"
        :activos="estado.idsTiposProducto.length"
      >
        <FiltroCheckbox
          :opciones="opcionesTipos"
          :seleccionados="estado.idsTiposProducto"
          :conteos="conteosTipos"
          @cambiar="(valores) => cambiarLista('idsTiposProducto', valores)"
        />
      </GrupoFiltro>

      <GrupoFiltro
        v-if="opcionesMedios.length"
        titulo="Medio"
        :activos="estado.idsMedios.length"
      >
        <FiltroCheckbox
          :opciones="opcionesMedios"
          :seleccionados="estado.idsMedios"
          :conteos="conteosMedios"
          @cambiar="(valores) => cambiarLista('idsMedios', valores)"
        />
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Precio"
        :abierto="true"
        :activos="(estado.precioMin !== '' ? 1 : 0) + (estado.precioMax !== '' ? 1 : 0)"
      >
        <FiltroRangoPrecio
          :precio-min="estado.precioMin"
          :precio-max="estado.precioMax"
          :moneda="moneda"
          @cambiar="actualizar"
        />
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Disponibilidad"
        :activos="(estado.soloPiezaUnica ? 1 : 0) + (estado.soloIlimitado ? 1 : 0)"
      >
        <ul class="space-y-0.5">
          <li>
            <label class="flex cursor-pointer items-center gap-2 rounded-lg px-1.5 py-1.5 text-sm text-cafe transition hover:bg-rosa/20">
              <input
                type="checkbox"
                class="h-4 w-4 rounded border-borde text-coral focus:ring-coral"
                :checked="estado.soloPiezaUnica"
                @change="actualizar({ soloPiezaUnica: $event.target.checked })"
              />
              Solo piezas únicas
            </label>
          </li>
          <li>
            <label class="flex cursor-pointer items-center gap-2 rounded-lg px-1.5 py-1.5 text-sm text-cafe transition hover:bg-rosa/20">
              <input
                type="checkbox"
                class="h-4 w-4 rounded border-borde text-coral focus:ring-coral"
                :checked="estado.soloIlimitado"
                @change="actualizar({ soloIlimitado: $event.target.checked })"
              />
              Solo stock ilimitado
            </label>
          </li>
        </ul>
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Contenido sensible"
        :activos="estado.contenidosSensibles.length + estado.excluirContenidosSensibles.length"
      >
        <FiltroCheckbox
          :opciones="opcionesSensibles"
          :seleccionados="estado.contenidosSensibles"
          :conteos="conteosContenidos"
          @cambiar="(valores) => cambiarLista('contenidosSensibles', valores)"
        />
        <div class="mt-2 border-t border-borde/40 pt-2">
          <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.1em] text-cafe/50">
            Excluir tipos
          </p>
          <FiltroCheckbox
            :opciones="opcionesSensibles"
            :seleccionados="estado.excluirContenidosSensibles"
            @cambiar="(valores) => cambiarLista('excluirContenidosSensibles', valores)"
          />
        </div>
      </GrupoFiltro>

      <FiltroEtiquetas
        v-if="etiquetas.length"
        :etiquetas="etiquetas"
        :categorias="categorias"
        :seleccionadas="estado.idsEtiquetas"
        :excluidas="estado.idsEtiquetasExcluidas"
        :conteos="conteosEtiquetas"
        @cambiar="actualizar"
      />

      <div class="py-3">
        <button
          v-if="activos > 0"
          type="button"
          class="w-full rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="emit('limpiar')"
        >
          Limpiar todos los filtros
        </button>
      </div>
    </div>
  </div>
</template>
