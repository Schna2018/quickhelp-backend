package com.quickhelp.entity;

import jakarta.persistence.*;

/**
 * QUICKHELP - ENTIDAD ROL
 *
 * Representa la tabla "rol" de PostgreSQL.
 * Cada registro define un tipo de usuario dentro del sistema.
 */
@Entity
@Table(name = "rol")
public class Rol {

    // Clave primaria de la tabla rol.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    // Código interno del rol.
    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    // Nombre descriptivo del rol.
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Descripción opcional del rol.
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    // Indica si el rol se encuentra activo.
    @Column(name = "estado", nullable = false)
    private Boolean estado;

    /**
     * Constructor vacío requerido por JPA.
     */
    public Rol() {
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}
