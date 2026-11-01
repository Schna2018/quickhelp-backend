
package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones para mapear la entidad con PostgreSQL.
import java.time.LocalDateTime; // Maneja la fecha y hora de asociación.

@Entity // Indica que esta clase representa una tabla de la base de datos.
@Table(name = "redapoyo") // Nombre exacto de la tabla en PostgreSQL.
public class RedApoyo {

    @Id // Clave primaria.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID generado por PostgreSQL.
    @Column(name = "id_red_apoyo")
    private Integer idRedApoyo;

    @ManyToOne(fetch = FetchType.LAZY) // Relación con el adulto mayor.
    @JoinColumn(name = "id_adulto_mayor", nullable = false)
    private Usuario adultoMayor;

    @ManyToOne(fetch = FetchType.LAZY) // Relación con la persona de apoyo.
    @JoinColumn(name = "id_integrante", nullable = false)
    private Usuario integrante;

    @Column(name = "prioridad", nullable = false)
    private Integer prioridad; // Orden de prioridad de la persona de apoyo.

    @Column(name = "estado", nullable = false)
    private Boolean estado; // Indica si la vinculación está activa.

    @Column(name = "fecha_asociacion", nullable = false)
    private LocalDateTime fechaAsociacion; // Fecha de creación de la relación.

    public Integer getIdRedApoyo() { return idRedApoyo; }
    public void setIdRedApoyo(Integer idRedApoyo) { this.idRedApoyo = idRedApoyo; }

    public Usuario getAdultoMayor() { return adultoMayor; }
    public void setAdultoMayor(Usuario adultoMayor) { this.adultoMayor = adultoMayor; }

    public Usuario getIntegrante() { return integrante; }
    public void setIntegrante(Usuario integrante) { this.integrante = integrante; }

    public Integer getPrioridad() { return prioridad; }
    public void setPrioridad(Integer prioridad) { this.prioridad = prioridad; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

    public LocalDateTime getFechaAsociacion() { return fechaAsociacion; }
    public void setFechaAsociacion(LocalDateTime fechaAsociacion) { this.fechaAsociacion = fechaAsociacion; }
}
