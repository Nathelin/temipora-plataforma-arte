# Temipora

Trabajo final para la carrera de Analista en Sistemas de Computación de la Universidad Nacional de Misiones. Temipora es una plataforma web para conectar artistas y clientes en torno a obras de arte, servicios y productos artísticos. Los artistas publican su portfolio, ofrecen servicios y productos, y los clientes pueden explorarlos, contactarlos y encargar trabajos.

> **Estado actual**: proyecto en desarrollo. A continuación se detalla lo que ya funciona, el stack utilizado y cómo levantar el entorno local.

---

## Índice

- [De qué trata el proyecto](#de-qué-trata-el-proyecto)
- [Estado actual del proyecto](#estado-actual-del-proyecto)
- [Tecnologías](#tecnologías)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Prerrequisitos](#prerrequisitos)
- [Configuración de variables de entorno](#configuración-de-variables-de-entorno)
- [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
  - [Backend (Spring Boot)](#backend-spring-boot)
  - [Frontend (Vue + Vite)](#frontend-vue--vite)
- [API principal](#api-principal)
- [Roles y permisos](#roles-y-permisos)
- [Decisiones de diseño relevantes](#decisiones-de-diseño-relevantes)
- [Trabajo pendiente / próximos pasos](#trabajo-pendiente--próximos-pasos)

---

## De qué trata el proyecto

Temipora es un marketplace de arte. Los usuarios pueden tener uno de tres roles:

- **`ADMIN`** — administra la plataforma (usuarios, tipos de producto/servicio, y próximamente moderación, sanciones, notificaciones y auditoría).
- **`ARTISTA`** — gesta su perfil público, sube imágenes a su portfolio y (a futuro) ofrecerá servicios y productos.
- **`CLIENTE`** — navega, consume contenido de artistas y (a futuro) gestionará pedidos y compras.

---

## Estado actual del proyecto

### Funcional (frontend + backend)

- **Autenticación y registro**
  - Registro de usuarios con validaciones y rol (artista o cliente).
  - Inicio de sesión con usuario/contraseña + **JWT** firmado con clave secreta.
  - Redirección según rol tras el login (admin → panel; artista/cliente → servicios).
  - Verificación de disponibilidad de nombre de usuario (`/api/checkNombreUsuario`).

- **Perfiles**
  - Página pública de perfil (`/perfil/@usuario`) con banner, foto, nombre de display, biografía y datos del artista.
  - Edición de perfil desde la sección **Configuración → Mi perfil** (nombre de display, biografía, sexo, teléfono, país, foto y banner).

- **Portfolio de artista**
  - Carga de imágenes con título, descripción, medio (tradicional/digital), materiales y contenido sensible.
  - Vista pública del portfolio en el perfil del artista.
  - Se está trabajando en la modificación, baja y filtros de contenido sensible.

- **Panel de administración**
  - Sidebar con módulos: gestión de usuarios, tipos de producto y tipos de servicio (activos) + moderación/sanciones/notificaciones/auditoría (próximamente).
  - **ABMC de tipos de producto y servicio** con baja lógica (activo/inactivo).
  - Listado de usuarios registrados con detalle expandible.

- **Configuración de usuario** (`/configuracion`)
  - Secciones: *Mi perfil*, *Privacidad y contenido*, *Términos de uso*, *Cuenta y seguridad* (las tres últimas aún son placeholders).

- **Subida de archivos**
  - Endpoint de subida de imágenes con validación de formato; almacenamiento local en `uploads/`.

### Parcial / placeholder

- Páginas de **Servicios** y **Productos** del sitio público (todavía sin contenido real).
- Módulos de **moderación**, **sanciones**, **notificaciones** y **auditoría** (placeholders en el panel admin).
- **Privacidad/contenido sensible**, **términos globales de artista** y **cuenta/seguridad** (placeholders en configuración).
- Gestión de **pedidos**, **compras**, **pagos**, **reseñas** y **carrito** (existen modelos de dominio en el backend, sin implementación de endpoints/flujo aún).

---

## Tecnologías

### Backend

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje de programación |
| Spring Boot | 4.1.1 | Framework principal |
| Spring WebMVC | — | API REST |
| Spring Data JPA | — | Persistencia / ORM |
| Spring Security | — | Autenticación y autorización |
| JJWT | 0.13.0 | Generación/validación de tokens JWT |
| PostgreSQL | 42.7.3 | Base de datos |
| H2 | — | Base de datos en memoria (tests) |
| Lombok | — | Reducción de boilerplate |
| webp-imageio | 0.1.6 | Soporte de lectura de imágenes WebP |

### Frontend

| Tecnología | Versión | Uso |
|---|---|---|
| Vue | 3.5.x | Framework de UI |
| Vue Router | 4.6.x | Enrutado SPA |
| Vite | 8.x | Bundler / dev server |
| Tailwind CSS | 4.3.x | Utilidades de estilos |
| Axios | 1.20.x | Cliente HTTP |

---

## Estructura del repositorio

```
temipora/
├── backend-springboot/     # API REST (Spring Boot)
│   ├── src/main/java/com/temipora/backend_springboot/
│   │   ├── controladores/  # Endpoints REST
│   │   ├── servicios/      # Lógica de negocio
│   │   ├── repositorios/   # Acceso a datos (Spring Data JPA)
│   │   ├── modelo/         # Entidades y DTOs
│   │   └── config/         # Seguridad, seeders, filtros JWT, etc.
│   ├── src/main/resources/ # application.properties, banner
│   └── uploads/            # Archivos subidos (storage local)
├── temipora-front/         # SPA (Vue 3 + Vite)
│   └── src/
│       ├── components/     # Componentes (Login, Registro, etc.)
│       ├── views/          # Páginas (Inicio, Perfil, Configuración...)
│       ├── layouts/        # Layouts (MainLayout, AdminLayout, ConfiguraciónLayout)
│       ├── router/         # Rutas y guardas
│       └── servicios/      # Cliente Axios y utilidades
└── documentacion/          # Documentación del análisis/requisitos
```

---

## Prerrequisitos

- **Java 21** (JDK).
- **Maven** (o usar el wrapper `./mvnw` / `mvnw.cmd` incluido).
- **Node.js** `^22.18.0 || >=24.12.0` y **npm**.
- **PostgreSQL** corriendo en `localhost:5432` con una base de datos llamada `temipora`.

---

## Configuración de variables de entorno

El backend lee variables de entorno. Deben definirse las siguientes:

| Variable | Descripción |
|---|---|
| `BD_USUARIO` | Usuario de la base de datos PostgreSQL |
| `BD_CONTRASENIA` | Contraseña de la base de datos |
| `JWT_KEY` | Clave secreta para firmar los tokens JWT |
| `JWT_TIME` | Tiempo de expiración del token (en ms) |
| `ADMIN_CORREO` | Correo del usuario administrador inicial |
| `ADMIN_CONTRASENIA` | Contraseña del usuario administrador inicial |
| `ADMIN_NOMBRE_USUARIO` | Nombre de usuario del administrador inicial |
| `AWS_ACCESS` / `AWS_KEY` | Credenciales AWS (reservadas para storage en S3 a futuro) |
| `AWS_BUCKET` | Nombre del bucket S3 (para implementación futura, no se usará durante la cursada de Trabajo Final) |

> **Nota sobre el administrador**: los roles (`ADMIN`, `ARTISTA`, `CLIENTE`) se crean automáticamente al arrancar mediante `RolSeeder`. El usuario admin solo se crea si están definidas las variables `ADMIN_*`; si no, no se crea ningún admin (por seguridad).

### Ejemplo (PowerShell)

```powershell
$env:BD_USUARIO = "postgres"
$env:BD_CONTRASENIA = "claveBD"
$env:JWT_KEY = "clave_muy_larga_y_secreta"
$env:JWT_TIME = "86400000"
$env:ADMIN_CORREO = "admin@temipora.com"
$env:ADMIN_CONTRASENIA = "admin_password"
$env:ADMIN_NOMBRE_USUARIO = "admin"
```

---

## Cómo ejecutar el proyecto

### Backend (Spring Boot)

```powershell
cd backend-springboot
.\mvnw.cmd spring-boot:run
```

El servidor queda disponible en `http://localhost:8080`.

Para compilar sin ejecutar:

```powershell
.\mvnw.cmd compile
```

Para ejecutar los tests:

```powershell
.\mvnw.cmd test
```

### Frontend (Vue + Vite)

```powershell
cd temipora-front
npm install
npm run dev
```

El dev server queda en `http://localhost:5173`. El proxy de Vite reenvía `/api` y `/uploads` a `http://localhost:8080`, por lo que no es necesario configurar URLs del backend en el frontend.

Para build de producción:

```powershell
npm run build
```

---

## API principal

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/registrarse` | Público | Registro de usuario |
| `POST` | `/api/auth/login` | Público | Login (devuelve JWT) |
| `GET` | `/api/checkNombreUsuario` | Público | Verifica disponibilidad de nombre de usuario |
| `POST` | `/api/upload` | Público | Sube un archivo/imagen |
| `GET` | `/api/perfil/{nombreUsuario}` | Autenticado | Obtiene perfil público |
| `PUT` | `/api/usuarios/{idUsuario}/perfil` | Autenticado | Actualiza perfil propio |
| `GET` | `/api/usuarios` | `ADMIN` | Lista todos los usuarios |
| `GET` | `/api/portfolio/{nombreUsuario}` | Autenticado | Lista imágenes del portfolio |
| `POST` | `/api/portfolio/usuarios/{idUsuario}/imagenes` | `ARTISTA` | Agrega imagen al portfolio propio |
| `GET` | `/api/admin/tipos-producto` | `ADMIN` | Lista tipos de producto |
| `POST` | `/api/admin/tipos-producto` | `ADMIN` | Alta de tipo de producto |
| `PUT` | `/api/admin/tipos-producto/{id}` | `ADMIN` | Modifica tipo de producto |
| `DELETE` | `/api/admin/tipos-producto/{id}` | `ADMIN` | Baja lógica de tipo de producto |
| `GET` | `/api/admin/tipos-servicio` | `ADMIN` | Lista tipos de servicio |
| `POST` | `/api/admin/tipos-servicio` | `ADMIN` | Alta de tipo de servicio |
| `PUT` | `/api/admin/tipos-servicio/{id}` | `ADMIN` | Modifica tipo de servicio |
| `DELETE` | `/api/admin/tipos-servicio/{id}` | `ADMIN` | Baja lógica de tipo de servicio |

---

## Roles y permisos

- La autorización a nivel de endpoint se hace con `@PreAuthorize("hasRole('...')")` y `@EnableMethodSecurity`.
- Los roles están modelados como **enumeración fija** (`ERol`: `ADMIN`, `ARTISTA`, `CLIENTE`) y se insertan automáticamente al inicio mediante `RolSeeder`.
- El frontend usa guardas de `vue-router` (`requiresAdmin`) para proteger las rutas del panel de administración.
- Los **permisos granulares por módulo** para el admin (moderación, catálogo, etc.) están planificados pero aún no implementados.

---

## Decisiones de diseño relevantes

- **DTOs de entrada/salida separados de las entidades** para no exponer datos sensibles (ej. `contrasenia`) ni permitir setear campos que no corresponden desde el cliente.
- **Manejo centralizado de errores** mediante `@RestControllerAdvice` (`ManejoGlobalErrores`): duplicados, no encontrado, validación, etc.
- **Baja lógica** para los tipos de producto/servicio (flag `activo`) en lugar de borrado físico, para preservar integridad de registros históricos.
- **Almacenamiento local** de archivos (interfaz `StorageService` con implementación `LocalStorageService`); hay credenciales AWS definidas, reservadas para un futuro almacenamiento en S3.
- **Nombre de usuario como identificador público** en las URLs de perfil (`/perfil/@usuario`).

---

## Trabajo pendiente / próximos pasos

- Gestión de **países** como tabla administrable (con autocompletar en el perfil).
- Módulos de **moderación**, **sanciones**, **notificaciones** y **auditoría** del panel admin.
- Secciones de **privacidad/contenido sensible**, **términos de artista** y **cuenta/seguridad** en configuración.
- Flujo completo de **pedidos**, **compras**, **pagos** y **reseñas**.
- Integración con **Google OAuth** (planificado, aún no implementado).
- Tests unitarios y de integración más allá del test de carga de contexto.