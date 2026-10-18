<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import fondo from '../assets/fondos/prueba.png'
import welcomeLogo from '../assets/welcome logo.png'

const router = useRouter()
const nombreUsuario = ref('')
const contrasenia = ref('')
const error = ref('')
const cargando = ref(false)
const verContrasenia = ref(false)

async function iniciarSesion() {
  error.value = ''
  cargando.value = true
  try {
    const res = await api.post('/auth/login', {
      nombreUsuario: nombreUsuario.value,
      contrasenia: contrasenia.value,
    })

    const token = res.data.token
    const rol = res.data.rol
    const usuario = res.data.NombreUsuario

    localStorage.setItem('token', token)
    localStorage.setItem('rol', rol)
    localStorage.setItem('nombreUsuario', usuario)
    localStorage.setItem('idUsuario', res.data.idUsuario ?? '')
    localStorage.setItem('urlFotoPerfil', res.data.urlFotoPerfil ?? '')

    if (rol === 'ADMIN') {
      router.push({ name: 'usuarios' })
    } else {
      router.push({ name: 'servicios' })
    }
  } catch (e) {
    error.value = 'Nombre de usuario o contraseña incorrecta.'
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div
    class="min-h-screen w-full flex items-stretch justify-end px-4 py-4 sm:px-8 sm:py-6 sm:pr-10 bg-cover bg-center"
    :style="{ backgroundImage: `url(${fondo})` }"
  >
    <div class="w-full max-w-md rounded-3xl bg-white px-8 py-10 shadow-xl flex flex-col justify-start">
      <div class="sm:mx-auto sm:w-full sm:max-w-sm flex flex-col items-center pt-6">
        <img :src="welcomeLogo" alt="Bienvenido a Temipora" class="w-20 h-20 object-contain mb-4" />
        <h2 class="text-center text-2xl font-normal leading-9 tracking-tight text-granate">
          Inicia sesión
        </h2>
        <p class="text-center text-sm text-cafe/60 mt-1">Nos alegra verte de nuevo</p>
      </div>

      <form class="mt-16 space-y-16 sm:pt-5" @submit.prevent="iniciarSesion">
        <div>
          <label for="nombreUsuario" class="block text-sm font-normal leading-6 text-cafe">
            Nombre de usuario
          </label>
          <div class="mt-3">
            <input
              id="nombreUsuario"
              v-model="nombreUsuario"
              name="nombreUsuario"
              type="text"
              autocomplete="username"
              required
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral"
            />
          </div>
        </div>

        <div>
          <label for="contrasenia" class="block text-sm font-normal leading-6 text-cafe">
            Contraseña
          </label>
          <div class="mt-3 relative">
            <input
              id="contrasenia"
              v-model="contrasenia"
              name="contrasenia"
              :type="verContrasenia ? 'text' : 'password'"
              autocomplete="current-password"
              required
              class="pl-4 pr-14 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 flex items-center px-4 text-sm text-cafe/60 hover:text-rojizo transition"
              @click="verContrasenia = !verContrasenia"
            >
              {{ verContrasenia ? 'Ocultar' : 'Ver' }}
            </button>
          </div>
        </div>

        <p v-if="error" class="rounded-md bg-rosa/40 px-3 py-2 text-sm text-rojizo">
          {{ error }}
        </p>

        <div class="pt-2">
          <button
            type="submit"
            :disabled="cargando"
            class="flex w-full justify-center rounded-lg bg-coral px-3 py-3 text-base font-normal leading-6 text-white shadow-sm hover:bg-rojizo disabled:opacity-50 transition"
          >
            {{ cargando ? 'Ingresando...' : 'Iniciar sesión' }}
          </button>
        </div>
      </form>

      <p class="mt-auto pt-8 text-center text-sm text-cafe/70">
        ¿No tenés cuenta?
        <router-link to="/registrarse" class="text-rojizo hover:text-coral underline">
          Registrarse
        </router-link>
      </p>
    </div>
  </div>
</template>