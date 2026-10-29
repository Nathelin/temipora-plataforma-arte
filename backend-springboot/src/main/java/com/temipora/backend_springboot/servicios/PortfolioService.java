package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.FiltroPortfolioRequest;
import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.ImagenEtiqueta;
import com.temipora.backend_springboot.modelo.ImagenMaterial;
import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.ImagenPortfolioRequest;
import com.temipora.backend_springboot.modelo.ImagenPortfolioVerificadaRequest;
import com.temipora.backend_springboot.modelo.Material;
import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Portfolio;
import com.temipora.backend_springboot.modelo.PortfolioEntregaVerificada;
import com.temipora.backend_springboot.modelo.ReseniaPublicaResumenDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioImagen;
import com.temipora.backend_springboot.modelo.TipoArte;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VerificacionPortfolioDTO;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenMaterialRepository;
import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.MaterialRepository;
import com.temipora.backend_springboot.repositorios.PortfolioEntregaVerificadaRepository;
import com.temipora.backend_springboot.repositorios.PortfolioRepository;
import com.temipora.backend_springboot.repositorios.ServicioImagenRepository;
import com.temipora.backend_springboot.repositorios.TipoArteRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class PortfolioService {

    private static final int MAXIMO_ETIQUETAS = 5;
    private static final int MAXIMO_MATERIALES = 5;

    private final UsuarioRepository usuarioRepository;
    private final PortfolioRepository portfolioRepository;
    private final ImagenRepository imagenRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final ImagenEtiquetaRepository imagenEtiquetaRepository;
    private final TipoArteRepository tipoArteRepository;
    private final MaterialRepository materialRepository;
    private final ImagenMaterialRepository imagenMaterialRepository;
    private final PortfolioEntregaVerificadaRepository verificacionRepository;
    private final EntregaArchivoRepository archivoRepository;
    private final ServicioImagenRepository servicioImagenRepository;
    private final ImagenGaleriaService imagenGaleriaService;
    private final ReseniaPublicaService reseniaPublicaService;

    public PortfolioService(
            UsuarioRepository usuarioRepository,
            PortfolioRepository portfolioRepository,
            ImagenRepository imagenRepository,
            EtiquetaRepository etiquetaRepository,
            ImagenEtiquetaRepository imagenEtiquetaRepository,
            TipoArteRepository tipoArteRepository,
            MaterialRepository materialRepository,
            ImagenMaterialRepository imagenMaterialRepository,
            PortfolioEntregaVerificadaRepository verificacionRepository,
            EntregaArchivoRepository archivoRepository,
            ServicioImagenRepository servicioImagenRepository,
            ImagenGaleriaService imagenGaleriaService,
            ReseniaPublicaService reseniaPublicaService) {
        this.usuarioRepository = usuarioRepository;
        this.portfolioRepository = portfolioRepository;
        this.imagenRepository = imagenRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.imagenEtiquetaRepository = imagenEtiquetaRepository;
        this.tipoArteRepository = tipoArteRepository;
        this.materialRepository = materialRepository;
        this.imagenMaterialRepository = imagenMaterialRepository;
        this.verificacionRepository = verificacionRepository;
        this.archivoRepository = archivoRepository;
        this.servicioImagenRepository = servicioImagenRepository;
        this.imagenGaleriaService = imagenGaleriaService;
        this.reseniaPublicaService = reseniaPublicaService;
    }

    @Transactional(readOnly = true)
    public List<ImagenPortfolioDTO> listar(String nombreUsuario) {
        Usuario artista = obtenerArtista(nombreUsuario);
        return portfolioRepository.findByArtista(artista)
            .map(portfolio -> {
                List<Imagen> imagenes = imagenRepository
                    .findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio);
                Map<Long, List<EtiquetaDTO>> porImagen = etiquetasPorImagen(idsDe(imagenes));
                Map<Long, List<MaterialDTO>> matsPorImagen = materialesPorImagen(idsDe(imagenes));
                List<ImagenPortfolioDTO> dtos = imagenes.stream()
                    .map(img -> {
                        ImagenPortfolioDTO dto = ImagenPortfolioDTO.desdeImagen(img);
                        dto.setEtiquetas(porImagen.getOrDefault(img.getIdImagen(), List.of()));
                        dto.setMateriales(matsPorImagen.getOrDefault(img.getIdImagen(), List.of()));
                        return dto;
                    })
                    .toList();
                aplicarVerificaciones(dtos);
                return dtos;
            })
            .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public Page<ImagenPortfolioDTO> listarParaVisor(
            Long portfolioId, Long viewerId, FiltroPortfolioRequest filtro, Pageable pageable) {
        FiltroPortfolioRequest f = filtro == null ? new FiltroPortfolioRequest() : filtro;

        // 1. Obtener preferencia del visitante (o default ADVERTIR)
        EPreferenciaContenidoSensible pref = (viewerId != null)
            ? usuarioRepository.findByIdUsuario(viewerId)
                .map(Usuario::getPreferenciaSensible)
                .orElse(EPreferenciaContenidoSensible.ADVERTIR)
            : EPreferenciaContenidoSensible.ADVERTIR;

        // 2. Consultar imágenes visibles según preferencia y filtros
        Page<Imagen> page = imagenRepository.findParaVisor(
            portfolioId,
            viewerId,
            normalizarTitulo(f.getTitulo()),
            f.getIdEtiqueta(),
            f.getIdTipoArte(),
            parseSensible(f.getSensible()),
            pref == EPreferenciaContenidoSensible.OCULTAR,
            parseOrdenAsc(f.getOrden()),
            pageable);

        // 3. Mapear a DTO con campo calculado requiereAdvertencia
        Map<Long, List<EtiquetaDTO>> porImagen = etiquetasPorImagen(idsDe(page.getContent()));
        Map<Long, List<MaterialDTO>> matsPorImagen = materialesPorImagen(idsDe(page.getContent()));
        List<ImagenPortfolioDTO> dtos = page.getContent().stream()
            .map(img -> {
                boolean requiereAdvertencia = img.getContenidoSensible() != null
                    && pref == EPreferenciaContenidoSensible.ADVERTIR
                    && (viewerId == null || !img.getUsuario().getIdUsuario().equals(viewerId));
                ImagenPortfolioDTO dto = ImagenPortfolioDTO.desdeImagen(img, requiereAdvertencia);
                dto.setEtiquetas(porImagen.getOrDefault(img.getIdImagen(), List.of()));
                dto.setMateriales(matsPorImagen.getOrDefault(img.getIdImagen(), List.of()));
                return dto;
            })
            .toList();
        aplicarVerificaciones(dtos);
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ImagenPortfolioDTO> listarParaVisorPorNombreUsuario(
            String nombreUsuario, Long viewerId, FiltroPortfolioRequest filtro, Pageable pageable) {
        Usuario artista = obtenerArtista(nombreUsuario);
        Portfolio portfolio = portfolioRepository.findByArtista(artista)
            .orElse(null);
        if (portfolio == null) {
            return Page.empty(pageable);
        }
        return listarParaVisor(portfolio.getIdPortfolio(), viewerId, filtro, pageable);
    }

    @Transactional(readOnly = true)
    public List<EtiquetaDTO> etiquetasDePortfolio(String nombreUsuario) {
        Usuario artista = obtenerArtista(nombreUsuario);
        return portfolioRepository.findByArtista(artista)
            .map(portfolio -> etiquetaRepository
                .findEtiquetasDePortfolio(portfolio.getIdPortfolio()).stream()
                .map(EtiquetaDTO::desdeEtiqueta)
                .toList())
            .orElse(List.of());
    }

    @Transactional
    public ImagenPortfolioDTO crear(Long idUsuario, ImagenPortfolioRequest request) {
        Usuario artista = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Artista no encontrado: " + idUsuario));
        validarArtista(artista);

        List<Long> pedidos = normalizarPedido(request.getIdsEtiqueta(), MAXIMO_ETIQUETAS, "etiquetas");
        List<Etiqueta> etiquetas = cargarEtiquetasAsignables(artista, pedidos);

        TipoArte tipoArte = cargarTipoArteActivo(request.getIdTipoArte());
        List<Long> pedidosMateriales = normalizarPedido(request.getIdsMaterial(), MAXIMO_MATERIALES, "materiales");
        List<Material> materiales = cargarMaterialesAsignables(pedidosMateriales, tipoArte);

        Portfolio portfolio = portfolioRepository.findByArtista(artista)
            .orElseGet(() -> {
                Portfolio nuevo = new Portfolio();
                nuevo.setArtista(artista);
                return portfolioRepository.save(nuevo);
            });

        String titulo = request.getTitulo().trim();
        if (imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                portfolio, titulo, tipoArte)) {
            throw new IllegalArgumentException(
                "Ya existe una imagen con ese título y tipo de arte en el portfolio.");
        }

        Imagen imagen = new Imagen();
        imagen.setUrl(request.getUrl().trim());
        imagen.setTitulo(titulo);
        imagen.setDescripcion(request.getDescripcion().trim());
        imagen.setFechaSubida(LocalDateTime.now());
        imagen.setTipoArte(tipoArte);
        imagen.setContenidoSensible(request.getContenidoSensible());
        imagen.setEstado(true);
        imagen.setPortfolio(portfolio);
        imagen.setUsuario(artista); // asignar el usuario que sube la imagen

        Imagen guardada = imagenRepository.save(imagen);
        for (Etiqueta etiqueta : etiquetas) {
            imagenEtiquetaRepository.save(ImagenEtiqueta.builder()
                .imagen(guardada)
                .etiqueta(etiqueta)
                .build());
        }
        for (Material material : materiales) {
            imagenMaterialRepository.save(ImagenMaterial.builder()
                .imagen(guardada)
                .material(material)
                .build());
        }
        return dtoCompleta(guardada, false);
    }

    @Transactional
    public ImagenPortfolioDTO crearDesdeEntrega(
            Long idUsuario, ImagenPortfolioVerificadaRequest request) {
        Usuario artista = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Artista no encontrado: " + idUsuario));
        validarArtista(artista);

        EntregaArchivo archivo = archivoRepository
            .buscarParaGaleria(request.getIdArchivoEntrega())
            .orElseThrow(() -> new NoSuchElementException(
                "Entrega no encontrada: " + request.getIdArchivoEntrega()));
        Pedido pedido = archivo.getEntrega().getHitoPedido().getPedido();
        validarEntregaVerificable(archivo, pedido, idUsuario);

        List<Long> idsEtiquetas = normalizarPedido(
            request.getIdsEtiqueta(), MAXIMO_ETIQUETAS, "etiquetas");
        List<Etiqueta> etiquetas = cargarEtiquetasAsignables(artista, idsEtiquetas);
        TipoArte tipoArte = cargarTipoArteActivo(request.getIdTipoArte());
        List<Long> idsMateriales = normalizarPedido(
            request.getIdsMaterial(), MAXIMO_MATERIALES, "materiales");
        List<Material> materiales = cargarMaterialesAsignables(
            idsMateriales, tipoArte);

        Portfolio portfolio = portfolioRepository.findByArtista(artista)
            .orElseGet(() -> {
                Portfolio nuevo = new Portfolio();
                nuevo.setArtista(artista);
                return portfolioRepository.save(nuevo);
            });

        String titulo = request.getTitulo().trim();
        if (imagenRepository
                .existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                    portfolio, titulo, tipoArte)) {
            throw new IllegalArgumentException(
                "Ya existe una imagen con ese título y tipo de arte en el portfolio.");
        }

        imagenGaleriaService.asegurarLimpia(archivo);

        Imagen imagen = new Imagen();
        imagen.setUrl(urlPortfolioVerificado(archivo.getIdArchivo()));
        imagen.setTitulo(titulo);
        imagen.setDescripcion(request.getDescripcion().trim());
        imagen.setFechaSubida(LocalDateTime.now());
        imagen.setTipoArte(tipoArte);
        imagen.setContenidoSensible(request.getContenidoSensible());
        imagen.setEstado(true);
        imagen.setPortfolio(portfolio);
        imagen.setUsuario(artista);
        Imagen guardada = imagenRepository.save(imagen);

        PortfolioEntregaVerificada verificacion =
            new PortfolioEntregaVerificada();
        verificacion.setImagen(guardada);
        verificacion.setArchivoEntrega(archivo);
        verificacion.setFechaVinculacion(LocalDateTime.now());
        verificacion.setVigente(true);
        verificacionRepository.save(verificacion);

        if (request.isAgregarComoEjemplo()) {
            agregarEjemploDeServicio(pedido.getServicio(), guardada.getUrl());
        }

        for (Etiqueta etiqueta : etiquetas) {
            imagenEtiquetaRepository.save(ImagenEtiqueta.builder()
                .imagen(guardada)
                .etiqueta(etiqueta)
                .build());
        }
        for (Material material : materiales) {
            imagenMaterialRepository.save(ImagenMaterial.builder()
                .imagen(guardada)
                .material(material)
                .build());
        }
        return dtoCompleta(guardada, false);
    }

    @Transactional(readOnly = true)
    public List<VerificacionPortfolioDTO> listarVerificadas(
            String nombreUsuario) {
        Usuario artista = obtenerArtista(nombreUsuario);
        return verificacionRepository
            .listarActivasPorUsuario(artista.getIdUsuario()).stream()
            .map(verificacion -> VerificacionPortfolioDTO.builder()
                .idImagen(verificacion.getImagen().getIdImagen())
                .idArchivo(verificacion.getArchivoEntrega().getIdArchivo())
                .titulo(verificacion.getImagen().getTitulo())
                .servicioNombre(verificacion.getArchivoEntrega()
                    .getEntrega().getHitoPedido().getPedido()
                    .getServicio().getNombre())
                .build())
            .toList();
    }

    private void validarEntregaVerificable(
            EntregaArchivo archivo, Pedido pedido, Long idArtista) {
        if (!pedido.getArtista().getIdUsuario().equals(idArtista)) {
            throw new AccesoDenegadoException(
                "No podés agregar esta entrega a tu portfolio.");
        }
        if (pedido.getEstado() != EstadoPedido.FINALIZADO) {
            throw new IllegalArgumentException(
                "El pedido todavía no está completo.");
        }
        if (!archivo.getEntrega().isEsFinal()
                || !archivo.getEntrega().isVigente()
                || archivo.getEntrega().isRetirada()
                || archivo.isRetirado()) {
            throw new IllegalArgumentException(
                "Solo podés agregar la entrega final vigente.");
        }
        if (archivo.getMime() == null
                || !archivo.getMime().startsWith("image/")) {
            throw new IllegalArgumentException(
                "Solo podés agregar imágenes al portfolio.");
        }
        if (verificacionRepository
                .existsByArchivoEntregaIdArchivoAndVigenteTrue(
                    archivo.getIdArchivo())) {
            throw new IllegalArgumentException(
                "Esta entrega ya está en tu portfolio.");
        }
    }

    private String urlPortfolioVerificado(Long idArchivo) {
        return "/api/portfolio/verificadas/archivos/"
            + idArchivo + "/preview";
    }

    private void agregarEjemploDeServicio(Servicio servicio, String url) {
        if (servicio == null || url == null
                || servicio.getIdServicio() == null
                || servicioImagenRepository.existsByServicioIdServicioAndUrl(
                    servicio.getIdServicio(), url)) {
            return;
        }
        int orden = servicio.getImagenes().stream()
            .map(ServicioImagen::getOrden)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .map(ultimo -> ultimo + 1)
            .orElse(0);
        ServicioImagen ejemplo = new ServicioImagen();
        ejemplo.setServicio(servicio);
        ejemplo.setUrl(url);
        ejemplo.setOrden(orden);
        servicioImagenRepository.save(ejemplo);
    }

    @Transactional
    public ImagenPortfolioDTO modificar(Long idUsuario, Long idImagen, ImagenPortfolioRequest request) {
        Usuario artista = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Artista no encontrado: " + idUsuario));
        validarArtista(artista);

        List<Long> pedidos = normalizarPedido(request.getIdsEtiqueta(), MAXIMO_ETIQUETAS, "etiquetas");
        List<Long> pedidosMateriales = normalizarPedido(request.getIdsMaterial(), MAXIMO_MATERIALES, "materiales");

        Imagen imagen = imagenRepository.findById(idImagen)
            .orElseThrow(() -> new NoSuchElementException("Imagen no encontrada: " + idImagen));
        if (!artista.getIdUsuario().equals(imagen.getPortfolio().getArtista().getIdUsuario())) {
            throw new NoSuchElementException("Imagen no encontrada: " + idImagen);
        }

        TipoArte tipoArte = cargarTipoArte(request.getIdTipoArte());
        boolean tipoPropio = imagen.getTipoArte() != null
            && tipoArte.getIdTipoArte().equals(imagen.getTipoArte().getIdTipoArte());
        if (!tipoArte.isActivo() && !tipoPropio) {
            throw new IllegalArgumentException(
                "El tipo de arte '" + tipoArte.getNombre() + "' fue dado de baja y no puede asignarse a piezas nuevas.");
        }

        List<ImagenEtiqueta> actuales = imagenEtiquetaRepository.findByImagenIdImagen(imagen.getIdImagen());
        Set<Long> idsActuales = actuales.stream()
            .map(ie -> ie.getEtiqueta().getIdEtiqueta())
            .collect(Collectors.toSet());
        List<Etiqueta> nuevas = cargarEtiquetasAsignables(artista,
            pedidos.stream().filter(id -> !idsActuales.contains(id)).toList());

        List<ImagenMaterial> materialesActuales = imagenMaterialRepository
            .findByImagenIdImagen(imagen.getIdImagen());
        Set<Long> idsMaterialesActuales = materialesActuales.stream()
            .map(im -> im.getMaterial().getIdMaterial())
            .collect(Collectors.toSet());
        List<Material> materialesNuevos = cargarMaterialesAsignables(
            pedidosMateriales.stream().filter(id -> !idsMaterialesActuales.contains(id)).toList(),
            tipoArte);

        String titulo = request.getTitulo().trim();
        boolean tituloYTipoPropios = imagen.getTitulo().equalsIgnoreCase(titulo) && tipoPropio;
        if (!tituloYTipoPropios && imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                imagen.getPortfolio(), titulo, tipoArte)) {
            throw new IllegalArgumentException(
                "Ya existe una imagen con ese título y tipo de arte en el portfolio.");
        }

        String nuevaUrl = request.getUrl().trim();
        boolean esVerificada = verificacionRepository
            .findByImagenIdImagenAndVigenteTrue(idImagen).isPresent();
        if (esVerificada && !nuevaUrl.equals(imagen.getUrl())) {
            throw new IllegalArgumentException(
                "No podés reemplazar la imagen de una pieza verificada. "
                    + "Eliminá la pieza y creá una nueva.");
        }

        imagen.setTitulo(titulo);
        imagen.setDescripcion(request.getDescripcion().trim());
        imagen.setUrl(nuevaUrl);
        imagen.setTipoArte(tipoArte);
        imagen.setContenidoSensible(request.getContenidoSensible());
        Imagen guardada = imagenRepository.save(imagen);

        for (ImagenEtiqueta fila : actuales) {
            if (!pedidos.contains(fila.getEtiqueta().getIdEtiqueta())) {
                imagenEtiquetaRepository.delete(fila);
            }
        }
        for (Etiqueta etiqueta : nuevas) {
            imagenEtiquetaRepository.save(ImagenEtiqueta.builder()
                .imagen(guardada)
                .etiqueta(etiqueta)
                .build());
        }

        for (ImagenMaterial fila : materialesActuales) {
            if (!pedidosMateriales.contains(fila.getMaterial().getIdMaterial())) {
                imagenMaterialRepository.delete(fila);
            }
        }
        for (Material material : materialesNuevos) {
            imagenMaterialRepository.save(ImagenMaterial.builder()
                .imagen(guardada)
                .material(material)
                .build());
        }
        return dtoCompleta(guardada, false);
    }

    @Transactional
    public ImagenPortfolioDTO darDeBaja(Long idUsuario, Long idImagen) {
        Usuario artista = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Artista no encontrado: " + idUsuario));
        validarArtista(artista);

        Imagen imagen = imagenRepository.findById(idImagen)
            .orElseThrow(() -> new NoSuchElementException("Imagen no encontrada: " + idImagen));
        if (!artista.getIdUsuario().equals(imagen.getPortfolio().getArtista().getIdUsuario())) {
            throw new NoSuchElementException("Imagen no encontrada: " + idImagen);
        }
        if (!imagen.isEstado()) {
            throw new IllegalArgumentException("La imagen ya fue dada de baja.");
        }

        imagen.setEstado(false);
        Imagen guardada = imagenRepository.save(imagen);
        verificacionRepository.findByImagenIdImagenAndVigenteTrue(idImagen)
            .ifPresent(verificacion -> {
                verificacion.setVigente(false);
                verificacionRepository.save(verificacion);
                servicioImagenRepository.deleteByUrl(imagen.getUrl());
            });
        imagenEtiquetaRepository.deleteByImagenIdImagen(idImagen);
        imagenMaterialRepository.deleteByImagenIdImagen(idImagen);
        return dtoCompleta(guardada, false);
    }

    public String nombreUsuario(Long idUsuario) {
        return usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario))
            .getNombreUsuario();
    }

    private String normalizarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return null;
        }
        return "%" + titulo.trim().toLowerCase() + "%";
    }

    private Boolean parseSensible(String sensible) {
        if (sensible == null || sensible.isBlank()) {
            return null;
        }
        return switch (sensible.trim().toLowerCase()) {
            case "true", "si" -> Boolean.TRUE;
            case "false", "no" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException(
                "Filtro de contenido sensible no válido: " + sensible);
        };
    }

    private boolean parseOrdenAsc(String orden) {
        if (orden == null || orden.isBlank()) {
            return false;
        }
        return switch (orden.trim().toUpperCase()) {
            case "RECIENTE" -> false;
            case "ANTIGUO" -> true;
            default -> throw new IllegalArgumentException("Orden no válido: " + orden);
        };
    }

    private Usuario obtenerArtista(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + nombreUsuario));
        validarArtista(usuario);
        return usuario;
    }

    private void validarArtista(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().getNombre() != ERol.ARTISTA) {
            throw new IllegalArgumentException("El usuario no es un artista.");
        }
    }

    // ---------------------------------------------------------------
    // Etiquetas de la imagen (Fase B)
    // ---------------------------------------------------------------

    private List<Long> normalizarPedido(List<Long> idsPedidos, int maximo, String queSon) {
        List<Long> pedidos = idsPedidos == null ? List.of()
            : idsPedidos.stream().filter(Objects::nonNull).distinct().toList();
        if (pedidos.size() > maximo) {
            throw new IllegalArgumentException(
                "Una imagen puede tener como máximo " + maximo + " " + queSon + ".");
        }
        return pedidos;
    }

    private List<Etiqueta> cargarEtiquetasAsignables(Usuario artista, List<Long> ids) {
        List<Etiqueta> etiquetas = new ArrayList<>();
        for (Long id : ids) {
            Etiqueta etiqueta = etiquetaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Etiqueta no encontrada: " + id));
            validarAsignable(etiqueta, artista);
            etiquetas.add(etiqueta);
        }
        return etiquetas;
    }

    private void validarAsignable(Etiqueta etiqueta, Usuario artista) {
        boolean propiaPendiente = etiqueta.getEstado() == EtiquetaEstado.PENDIENTE
            && etiqueta.getCreadoPor() != null
            && etiqueta.getCreadoPor().getIdUsuario().equals(artista.getIdUsuario());
        if (etiqueta.getEstado() == EtiquetaEstado.VALIDA || propiaPendiente) {
            return;
        }
        if (etiqueta.getEstado() == EtiquetaEstado.INACTIVA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre() + "' fue dada de baja y no puede asignarse a piezas nuevas.");
        }
        if (etiqueta.getEstado() == EtiquetaEstado.RECHAZADA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre() + "' fue rechazada y no puede asignarse a una imagen.");
        }
        throw new IllegalArgumentException(
            "La etiqueta '" + etiqueta.getNombre() + "' está pendiente de evaluación y solo su creador puede asignarla.");
    }

    // ---------------------------------------------------------------
    // Tipo de arte y materiales de la imagen
    // ---------------------------------------------------------------

    private TipoArte cargarTipoArte(Long idTipoArte) {
        if (idTipoArte == null) {
            throw new IllegalArgumentException("El tipo de arte es obligatorio.");
        }
        return tipoArteRepository.findByIdTipoArte(idTipoArte)
            .orElseThrow(() -> new NoSuchElementException("Tipo de arte no encontrado: " + idTipoArte));
    }

    private TipoArte cargarTipoArteActivo(Long idTipoArte) {
        TipoArte tipoArte = cargarTipoArte(idTipoArte);
        if (!tipoArte.isActivo()) {
            throw new IllegalArgumentException(
                "El tipo de arte '" + tipoArte.getNombre() + "' fue dado de baja y no puede asignarse a piezas nuevas.");
        }
        return tipoArte;
    }

    private List<Material> cargarMaterialesAsignables(List<Long> ids, TipoArte tipoArte) {
        List<Material> materiales = new ArrayList<>();
        for (Long id : ids) {
            Material material = materialRepository.findByIdMaterial(id)
                .orElseThrow(() -> new NoSuchElementException("Material no encontrado: " + id));
            if (!material.isActivo()) {
                throw new IllegalArgumentException(
                    "El material '" + material.getNombre() + "' fue dado de baja y no puede asignarse a piezas nuevas.");
            }
            if (material.getTipoArte() == null || !material.getTipoArte().isActivo()) {
                throw new IllegalArgumentException(
                    "El material '" + material.getNombre() + "' pertenece a un tipo de arte dado de baja.");
            }
            if (!material.getTipoArte().getMedio().getIdMedio()
                    .equals(tipoArte.getMedio().getIdMedio())) {
                throw new IllegalArgumentException(
                    "El material '" + material.getNombre() + "' no corresponde al medio del tipo de arte seleccionado.");
            }
            materiales.add(material);
        }
        return materiales;
    }

    private ImagenPortfolioDTO dtoCompleta(Imagen imagen, boolean requiereAdvertencia) {
        ImagenPortfolioDTO dto = ImagenPortfolioDTO.desdeImagen(imagen, requiereAdvertencia);
        dto.setEtiquetas(imagenEtiquetaRepository.findByImagenIdImagen(imagen.getIdImagen()).stream()
            .map(ie -> EtiquetaDTO.desdeEtiqueta(ie.getEtiqueta()))
            .toList());
        dto.setMateriales(imagenMaterialRepository.findByImagenIdImagen(imagen.getIdImagen()).stream()
            .map(im -> MaterialDTO.desdeMaterial(im.getMaterial()))
            .toList());
        PortfolioEntregaVerificada verificacion = verificacionRepository
            .listarActivasPorImagenes(List.of(imagen.getIdImagen()))
            .stream()
            .findFirst()
            .orElse(null);
        aplicarVerificacion(dto, verificacion);
        return dto;
    }

    private void aplicarVerificaciones(List<ImagenPortfolioDTO> dtos) {
        if (dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream()
            .map(ImagenPortfolioDTO::getIdImagen)
            .toList();
        Map<Long, PortfolioEntregaVerificada> porImagen = verificacionesPorImagen(ids);
        Map<Long, ReseniaPublicaResumenDTO> resenias = reseniasDeVerificaciones(
            porImagen.values());
        dtos.forEach(dto -> aplicarVerificacion(
            dto, porImagen.get(dto.getIdImagen()), resenias));
    }

    private Map<Long, ReseniaPublicaResumenDTO> reseniasDeVerificaciones(
            Collection<PortfolioEntregaVerificada> verificaciones) {
        if (verificaciones.isEmpty()) {
            return Map.of();
        }
        List<Long> idsPedido = verificaciones.stream()
            .map(verificacion -> verificacion.getArchivoEntrega()
                .getEntrega().getHitoPedido().getPedido().getIdPedido())
            .distinct()
            .toList();
        Map<Long, ReseniaPublicaResumenDTO> resenias =
            reseniaPublicaService.resumenPorPedidos(idsPedido);
        return resenias != null ? resenias : Map.of();
    }

    private Map<Long, PortfolioEntregaVerificada> verificacionesPorImagen(
            Collection<Long> idsImagen) {
        if (idsImagen.isEmpty()) {
            return Map.of();
        }
        return verificacionRepository.listarActivasPorImagenes(idsImagen).stream()
            .collect(Collectors.toMap(
                verificacion -> verificacion.getImagen().getIdImagen(),
                verificacion -> verificacion));
    }

    private void aplicarVerificacion(
            ImagenPortfolioDTO dto, PortfolioEntregaVerificada verificacion) {
        if (dto == null || verificacion == null) {
            return;
        }
        Pedido pedido = verificacion.getArchivoEntrega()
            .getEntrega().getHitoPedido().getPedido();
        ReseniaPublicaResumenDTO resenia = null;
        if (pedido.isPublicacionAutorizada()
                && !pedido.isPublicacionOcultadaCliente()) {
            resenia = reseniaPublicaService.resumenPorPedido(pedido.getIdPedido());
            if (resenia != null && resenia.isAnonima()) {
                resenia = null;
            }
        }
        aplicarDatosVerificacion(dto, verificacion, pedido, resenia);
    }

    private void aplicarVerificacion(
            ImagenPortfolioDTO dto,
            PortfolioEntregaVerificada verificacion,
            Map<Long, ReseniaPublicaResumenDTO> resenias) {
        if (dto == null || verificacion == null) {
            return;
        }
        Pedido pedido = verificacion.getArchivoEntrega()
            .getEntrega().getHitoPedido().getPedido();
        ReseniaPublicaResumenDTO resenia = null;
        if (pedido.isPublicacionAutorizada()
                && !pedido.isPublicacionOcultadaCliente()) {
            resenia = resenias.get(pedido.getIdPedido());
            if (resenia != null && resenia.isAnonima()) {
                resenia = null;
            }
        }
        aplicarDatosVerificacion(dto, verificacion, pedido, resenia);
    }

    private void aplicarDatosVerificacion(
            ImagenPortfolioDTO dto,
            PortfolioEntregaVerificada verificacion,
            Pedido pedido,
            ReseniaPublicaResumenDTO resenia) {
        Servicio servicio = pedido.getServicio();
        dto.setVerificada(true);
        dto.setServicioNombre(servicio.getNombre());
        dto.setIdServicio(servicio.getIdServicio());
        dto.setServicioDisponible(servicio.getEstado() == EstadoServicio.PUBLICADO
            && servicio.getModoRecepcion() != ModoRecepcionSolicitud.CERRADA);
        dto.setResenia(resenia);
    }

    private Map<Long, List<EtiquetaDTO>> etiquetasPorImagen(Collection<Long> idsImagen) {
        if (idsImagen.isEmpty()) {
            return Map.of();
        }
        return imagenEtiquetaRepository.findByImagenIdImagenIn(idsImagen).stream()
            .collect(Collectors.groupingBy(
                ie -> ie.getImagen().getIdImagen(),
                Collectors.mapping(ie -> EtiquetaDTO.desdeEtiqueta(ie.getEtiqueta()), Collectors.toList())));
    }

    private Map<Long, List<MaterialDTO>> materialesPorImagen(Collection<Long> idsImagen) {
        if (idsImagen.isEmpty()) {
            return Map.of();
        }
        return imagenMaterialRepository.findByImagenIdImagenIn(idsImagen).stream()
            .collect(Collectors.groupingBy(
                im -> im.getImagen().getIdImagen(),
                Collectors.mapping(im -> MaterialDTO.desdeMaterial(im.getMaterial()), Collectors.toList())));
    }

    private List<Long> idsDe(List<Imagen> imagenes) {
        return imagenes.stream().map(Imagen::getIdImagen).filter(Objects::nonNull).toList();
    }
}
