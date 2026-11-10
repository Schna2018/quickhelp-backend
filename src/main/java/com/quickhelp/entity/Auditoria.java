
package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones de persistencia JPA.
import java.time.LocalDateTime; // Fecha y hora de auditoría.

@Entity // Representa la tabla auditoria.
@Table(name = "auditoria")
public class Auditoria {

    @Id // Clave primaria.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID generado por PostgreSQL.
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY) // Usuario responsable de la acción.
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "entidad", nullable = false)
    private String entidad; // Tabla o entidad afectada.

    @Column(name = "id_registro")
    private Integer idRegistro; // Identificador del registro afectado.

    @Column(name = "accion", nullable = false)
    private String accion; // Acción realizada.

    @Column(name = "detalle", columnDefinition = "text")
    private String detalle; // Información adicional.

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora; // Fecha de la acción.

    public Auditoria() {
    }

    @PrePersist // Asigna la fecha al crear el registro.
    public void antesDeGuardar() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }

    public Integer getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(Integer idAuditoria) { this.idAuditoria = idAuditoria; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public Integer getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Integer idRegistro) { this.idRegistro = idRegistro; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}
