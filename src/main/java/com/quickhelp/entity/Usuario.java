package com.quickhelp.entity;

import jakarta.persistence.*;

@Entity // Indica que esta clase representa una tabla de PostgreSQL.
@Table(name = "usuario") // Relaciona esta entidad con la tabla usuario.
public class Usuario {

    @Id // Define la clave primaria.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL genera el ID automáticamente.
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne // Muchos usuarios pueden pertenecer a un mismo rol.
    @JoinColumn(name = "id_rol", nullable = false) // Usa id_rol como clave foránea hacia rol.
    private Rol rol;

    @Column(name = "nombre_apellido", nullable = false, length = 150)
    private String nombreApellido; // Nombre y apellido del usuario.

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo; // Correo único utilizado por el usuario.

    @Column(name = "telefono", length = 30)
    private String telefono; // Teléfono del usuario; puede ser NULL.

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // Contraseña almacenada de forma cifrada/hash.

    @Column(name = "token_fcm")
    private String tokenFcm; // Token para futuras notificaciones push de la app móvil.

    @Column(name = "frase_activacion", length = 100)
    private String fraseActivacion; // Frase configurada para activar funciones por voz.

    @Column(name = "codigo_vinculacion", length = 20, unique = true)
    private String codigoVinculacion; // Identificador permanente del adulto mayor

    @Column(name = "voz_activa", nullable = false)
    private Boolean vozActiva; // Indica si la activación por voz está habilitada.

    @Column(name = "estado", nullable = false)
    private Boolean estado; // Indica si el usuario está activo en el sistema.

    public Usuario() {
    } // Constructor vacío requerido por JPA.

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getNombreApellido() {
        return nombreApellido;
    }

    public void setNombreApellido(String nombreApellido) {
        this.nombreApellido = nombreApellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getTokenFcm() {
        return tokenFcm;
    }

    public void setTokenFcm(String tokenFcm) {
        this.tokenFcm = tokenFcm;
    }

    public String getFraseActivacion() {
        return fraseActivacion;
    }

    public void setFraseActivacion(String fraseActivacion) {
        this.fraseActivacion = fraseActivacion;
    }

     public String getCodigoVinculacion() {
        return codigoVinculacion;
    }

    public void setCodigoVinculacion(String codigoVinculacion) {
        this.codigoVinculacion = codigoVinculacion;
    }

    public Boolean getVozActiva() {
        return vozActiva;
    }

    public void setVozActiva(Boolean vozActiva) {
        this.vozActiva = vozActiva;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}