import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('nombreUsuario')
      localStorage.removeItem('rol')
    }
    if (error.response?.status === 403
        && error.response?.data?.codigo === 'TOS_NO_ACEPTADOS') {
      window.dispatchEvent(new CustomEvent('tos-no-aceptados'))
    }
    return Promise.reject(error)
  },
)

export default api