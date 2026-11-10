
package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones para mapear la entidad en PostgreSQL.
import java.time.LocalDateTime; // Maneja la fecha y hora de la respuesta.

@Entity // Identifica la clase como una entidad JPA.
@Table(name = "respuestaalerta") // Tabla existente en PostgreSQL.
public class RespuestaAlerta {

    @Id // Identificador principal.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID generado por PostgreSQL.
    @Column(name = "id_respuesta")
    private Integer idRespuesta;

    @ManyToOne(fetch = FetchType.LAZY) // Relación con la emergencia.
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY) // Usuario que envía la respuesta.
    @JoinColumn(name = "id_integrante", nullable = false)
    private Usuario integrante;

    @Column(name = "tipo_respuesta", nullable = false)
    private String tipoRespuesta;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "observacion", columnDefinition = "text")
    private String observacion;

    // Identificador de la respuesta.
    public Integer getIdRespuesta() {
        return idRespuesta;
    }

    public void setIdRespuesta(Integer idRespuesta) {
        this.idRespuesta = idRespuesta;
    }

    // Emergencia asociada.
    public Alerta getAlerta() {
        return alerta;
    }

    public void setAlerta(Alerta alerta) {
        this.alerta = alerta;
    }

    // Integrante que responde.
    public Usuario getIntegrante() {
        return integrante;
    }

    public void setIntegrante(Usuario integrante) {
        this.integrante = integrante;
    }

    // Tipo de respuesta seleccionada.
    public String getTipoRespuesta() {
        return tipoRespuesta;
    }

    public void setTipoRespuesta(String tipoRespuesta) {
        this.tipoRespuesta = tipoRespuesta;
    }

    // Fecha y hora de la respuesta.
    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    // Mensaje opcional del asistente.
    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
