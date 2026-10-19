package com.quickhelp.dto;

public class UsuarioResponseDTO { // DTO que define qué datos del usuario se pueden enviar al frontend.

    private Integer idUsuario; // Identificador del usuario.
    private Integer idRol; // Identificador del rol asociado.
    private String nombreApellido; // Nombre completo del usuario.
    private String correo; // Correo del usuario.
    private String telefono; // Teléfono del usuario.
    private String tokenFcm; // Token usado para notificaciones push.
    private String fraseActivacion; // Frase configurada para activación por voz.
    private Boolean vozActiva; // Indica si la activación por voz está habilitada.
    private Boolean estado; // Indica si el usuario está activo.

    public UsuarioResponseDTO() {} // Constructor vacío.

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public String getNombreApellido() { return nombreApellido; }
    public void setNombreApellido(String nombreApellido) { this.nombreApellido = nombreApellido; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTokenFcm() { return tokenFcm; }
    public void setTokenFcm(String tokenFcm) { this.tokenFcm = tokenFcm; }

    public String getFraseActivacion() { return fraseActivacion; }
    public void setFraseActivacion(String fraseActivacion) { this.fraseActivacion = fraseActivacion; }

    public Boolean getVozActiva() { return vozActiva; }
    public void setVozActiva(Boolean vozActiva) { this.vozActiva = vozActiva; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
}