package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones de persistencia JPA.

@Entity // Representa la asignación de un permiso a un rol.
@Table(name = "rolpermiso")
public class RolPermiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_permiso")
    private Integer idRolPermiso; // Identificador de la relación.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol; // Rol al que pertenece el permiso.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_permiso", nullable = false)
    private Permiso permiso; // Permiso asignado.

    public RolPermiso() {} // Constructor requerido por JPA.

    public Integer getIdRolPermiso() { return idRolPermiso; }
    public void setIdRolPermiso(Integer idRolPermiso) { this.idRolPermiso = idRolPermiso; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Permiso getPermiso() { return permiso; }
    public void setPermiso(Permiso permiso) { this.permiso = permiso; }
}