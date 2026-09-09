<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'

const router = useRouter()
const usuarios = ref([])
const cargando = ref(true)
const error = ref('')
const abierto = ref(null)
const nombreAdmin = ref(localStorage.getItem('nombreUsuario') || '')
const busqueda = ref('')
const filtroRol = ref('todos')
const filtroEstado = ref('todos')

const usuariosFiltrados = computed(() => {
  const termino = busqueda.value.trim().toLowerCase()

  return [...usuarios.value]
    .filter((usuario) => {
      if (filtroRol.value !== 'todos' && usuario.rol !== filtroRol.value) {
        return false
      }

      if (filtroEstado.value === 'activo' && !usuario.estadoCuenta) {
        return false
      }

      if (filtroEstado.value === 'inactivo' && usuario.estadoCuenta) {
        return false
      }

      if (!termino) {
        return true
      }

      return [
        usuario.nombreUsuario,
        usuario.correo,
        usuario.nombre,
        usuario.apellido,
        usuario.telefono,
        usuario.sexo,
        usuario.rol,
      ].some((campo) => String(campo || '').toLowerCase().includes(termino))
    })
    .sort((a, b) => Number(a.idUsuario) - Number(b.idUsuario))
})

function limpiarFiltros() {
  busqueda.value = ''
  filtroRol.value = 'todos'
  filtroEstado.value = 'todos'
}

async function cargarUsuarios() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/usuarios')
    usuarios.value = res.data
  } catch (e) {
    if (e.response && e.response.status === 401) {
      error.value = 'No autorizado. Solo administradores pueden ver esta vista.'
      router.push({ name: 'login' })
    } else {
      error.value = 'No se pudieron cargar los usuarios.'
    }
  } finally {
    cargando.value = false
  }
}

function alternarDetalle(id) {
  abierto.value = abierto.value === id ? null : id
}

function cerrarSesion() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  router.push({ name: 'login' })
}

onMounted(cargarUsuarios)
</script>

<template>
  <div>

    <main class="flex-1 w-full mx-auto px-4 sm:px-8 py-8 overflow-auto">
      <div class="flex flex-col gap-4 mb-6">
        <div class="flex items-center justify-between gap-4">
          <h1 class="text-xl font-normal text-granate">Usuarios registrados</h1>
          <span class="text-sm text-cafe/60">
            {{ usuariosFiltrados.length }} de {{ usuarios.length }}
          </span>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-[minmax(0,1fr)_auto_auto_auto] gap-3">
          <input
            v-model="busqueda"
            type="search"
            placeholder="Buscar por usuario, correo, nombre..."
            class="min-w-0 rounded-lg border-0 py-2.5 px-4 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />

          <select
            v-model="filtroRol"
            aria-label="Filtrar por rol"
            class="rounded-lg border-0 py-2.5 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="todos">Todos los roles</option>
            <option value="ADMIN">Administradores</option>
            <option value="ARTISTA">Artistas</option>
            <option value="CLIENTE">Clientes</option>
          </select>

          <select
            v-model="filtroEstado"
            aria-label="Filtrar por estado"
            class="rounded-lg border-0 py-2.5 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="todos">Todos los estados</option>
            <option value="activo">Activos</option>
            <option value="inactivo">Inactivos</option>
          </select>

          <button
            class="rounded-lg border border-rosa px-4 py-2 text-sm text-cafe hover:bg-rosa/20 transition"
            @click="limpiarFiltros"
          >
            Limpiar
          </button>
        </div>
      </div>
      
      <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando usuarios...</p>
      <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

        <div v-else class="bg-blanco rounded-3xl overflow-hidden shadow-xl shadow-rosa/40">
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm min-w-160">
            <thead class="bg-rosa/40 text-granate">
              <tr>
                <th class="px-5 py-3 font-normal text-sm">ID</th>
                <th class="px-5 py-3 font-normal text-sm">Usuario</th>
                <th class="px-5 py-3 font-normal text-sm">Rol</th>
                <th class="px-5 py-3 font-normal text-sm">Estado</th>
                <th class="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              <template v-for="u in usuariosFiltrados" :key="u.idUsuario">
                <tr class="border-t border-rosa/50 hover:bg-rosa/20 transition">
                  <td class="px-5 py-3 text-cafe/50 font-mono">{{ u.idUsuario }}</td>
                  <td class="px-5 py-3 text-granate font-normal">{{ u.nombreUsuario }}</td>
                  <td class="px-5 py-3">
                    <span
                      class="px-2.5 py-0.5 rounded-full text-xs font-normal tracking-wide"
                      :class="{
                        'bg-rojizo/10 text-rojizo': u.rol === 'ADMIN',
                        'bg-coral/10 text-coral': u.rol === 'ARTISTA',
                        'bg-durazno/40 text-cafe': u.rol === 'CLIENTE',
                      }"
                    >
                      {{ u.rol }}
                    </span>
                  </td>
                  <td class="px-5 py-3">
                    <span
                      class="px-2.5 py-0.5 rounded-full text-xs font-normal"
                      :class="u.estadoCuenta
                        ? 'bg-green-500/10 text-green-700'
                        : 'bg-rojizo/10 text-rojizo'"
                    >
                      {{ u.estadoCuenta ? 'Activo' : 'Inactivo' }}
                    </span>
                  </td>
                  <td class="px-5 py-3 text-right">
                    <button
                      class="text-rojizo hover:text-coral transition"
                      @click="alternarDetalle(u.idUsuario)"
                    >
                      {{ abierto === u.idUsuario ? 'Cerrar' : 'Detalles' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="abierto === u.idUsuario" class="border-t border-rosa/50 bg-rosa/10">
                  <td colspan="5" class="px-5 py-5">
                    <div class="flex flex-wrap gap-5">
                      <div
                        v-if="u.urlFotoPerfil"
                        class="w-24 h-24 rounded-full overflow-hidden border-2 border-coral shrink-0"
                      >
                        <img
                          :src="u.urlFotoPerfil"
                          :alt="u.nombreUsuario"
                          class="w-full h-full object-cover"
                        />
                      </div>
                      <div
                        v-else
                        class="w-24 h-24 rounded-full bg-coral/20 flex items-center justify-center text-3xl text-granate shrink-0"
                      >
                        {{ (u.nombreUsuario || '?').charAt(0).toUpperCase() }}
                      </div>

                      <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-10 gap-y-2 text-sm">
                        <p><span class="text-rojizo">Nombre:</span> {{ u.nombre || '—' }}</p>
                        <p><span class="text-rojizo">Apellido:</span> {{ u.apellido || '—' }}</p>
                        <p><span class="text-rojizo">Correo:</span> {{ u.correo || '—' }}</p>
                        <p><span class="text-rojizo">Teléfono:</span> {{ u.telefono || '—' }}</p>
                        <p><span class="text-rojizo">Sexo:</span> {{ u.sexo || '—' }}</p>
                        <p><span class="text-rojizo">Nacimiento:</span> {{ u.fechaNacimiento || '—' }}</p>
                        <p>
                          <span class="text-rojizo">Creada:</span>
                          {{ u.fechaCreacion ? new Date(u.fechaCreacion).toLocaleString() : '—' }}
                        </p>
                        <p>
                          <span class="text-rojizo">Última conexión:</span>
                          {{
                            u.fechaUltimaConexion
                              ? new Date(u.fechaUltimaConexion).toLocaleString()
                              : '—'
                          }}
                        </p>
                      </div>
                    </div>

                    <!--div v-if="u.urlBanner" class="mt-4">
                      <span class="text-cafe/50 text-sm">Banner:</span>
                      <img
                        :src="u.urlBanner"
                        alt="banner"
                        class="mt-2 w-full h-36 object-cover rounded-2xl border border-rosa"
                      />
                    </div-->
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
          </div>

          <p v-if="!usuariosFiltrados.length" class="p-8 text-cafe/50 text-center">
            {{ usuarios.length ? 'No hay usuarios que coincidan con los filtros.' : 'No hay usuarios registrados.' }}
          </p>
        </div>
    </main>
  </div>
</template>