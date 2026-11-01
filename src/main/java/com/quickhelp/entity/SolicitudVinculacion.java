
package com.quickhelp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity // Representa la tabla de solicitudes.
@Table(name = "solicitudvinculacion")
public class SolicitudVinculacion {

    @Id // Identificador principal.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY) // Adulto mayor destinatario.
    @JoinColumn(name = "id_adulto_mayor", nullable = false)
    private Usuario adultoMayor;

    @ManyToOne(fetch = FetchType.LAZY) // Asistente solicitante.
    @JoinColumn(name = "id_asistente", nullable = false)
    private Usuario asistente;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "PENDIENTE"; // Estado inicial.

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud; // Fecha de creación.

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta; // Fecha de aprobación o rechazo.

    @PrePersist // Se ejecuta antes de insertar.
    public void antesDeGuardar() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
    }

    public Integer getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Integer idSolicitud) { this.idSolicitud = idSolicitud; }

    public Usuario getAdultoMayor() { return adultoMayor; }
    public void setAdultoMayor(Usuario adultoMayor) { this.adultoMayor = adultoMayor; }

    public Usuario getAsistente() { return asistente; }
    public void setAsistente(Usuario asistente) { this.asistente = asistente; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }
}
