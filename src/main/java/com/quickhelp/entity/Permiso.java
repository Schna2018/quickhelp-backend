package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones para mapear la entidad en PostgreSQL.

@Entity // Representa un registro de permisos.
@Table(name = "permiso")
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso; // Identificador del permiso.

    @Column(name = "codigo", nullable = false)
    private String codigo; // Código único de la operación.

    @Column(name = "nombre", nullable = false)
    private String nombre; // Nombre descriptivo.

    @Column(name = "descripcion")
    private String descripcion; // Explicación del permiso.

    public Permiso() {} // Constructor requerido por JPA.

    public Integer getIdPermiso() { return idPermiso; }
    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}