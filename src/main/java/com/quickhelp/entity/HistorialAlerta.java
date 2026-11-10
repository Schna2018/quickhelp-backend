
package com.quickhelp.entity;

import jakarta.persistence.Column; // Configura las columnas.
import jakarta.persistence.Entity; // Define la entidad JPA.
import jakarta.persistence.FetchType; // Configura la carga de relaciones.
import jakarta.persistence.GeneratedValue; // Genera identificadores.
import jakarta.persistence.GenerationType; // Estrategia de generación.
import jakarta.persistence.Id; // Define la clave primaria.
import jakarta.persistence.JoinColumn; // Configura claves foráneas.
import jakarta.persistence.ManyToOne; // Relaciona entidades.
import jakarta.persistence.Table; // Identifica la tabla PostgreSQL.
import java.time.LocalDateTime; // Maneja fechas y horas.

@Entity // Representa un evento en el historial de una alerta.
@Table(name = "historialalerta")
public class HistorialAlerta {

    @Id // Identificador del registro histórico.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL genera el ID.
    @Column(name = "id_historial")
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.LAZY) // Alerta a la que pertenece el evento.
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY) // Usuario que realizó la acción.
    @JoinColumn(name = "id_usuario_actor")
    private Usuario usuarioActor;

    @ManyToOne(fetch = FetchType.LAZY) // Estado previo, si corresponde.
    @JoinColumn(name = "id_estado_anterior")
    private EstadoAlerta estadoAnterior;

    @ManyToOne(fetch = FetchType.LAZY) // Estado resultante, si corresponde.
    @JoinColumn(name = "id_estado_nuevo")
    private EstadoAlerta estadoNuevo;

    @Column(name = "accion", nullable = false)
    private String accion;

    @Column(name = "observacion", columnDefinition = "text")
    private String observacion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Constructor requerido por JPA.
    public HistorialAlerta() {
    }

    public Integer getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(Integer idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Alerta getAlerta() {
        return alerta;
    }

    public void setAlerta(Alerta alerta) {
        this.alerta = alerta;
    }

    public Usuario getUsuarioActor() {
        return usuarioActor;
    }

    public void setUsuarioActor(Usuario usuarioActor) {
        this.usuarioActor = usuarioActor;
    }

    public EstadoAlerta getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoAlerta estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoAlerta getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoAlerta estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
