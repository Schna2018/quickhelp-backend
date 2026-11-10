
package com.quickhelp.entity;

import jakarta.persistence.Column; // Configura las columnas.
import jakarta.persistence.Entity; // Define la entidad JPA.
import jakarta.persistence.FetchType; // Configura la carga de relaciones.
import jakarta.persistence.GeneratedValue; // Genera el identificador.
import jakarta.persistence.GenerationType; // Estrategia de generación.
import jakarta.persistence.Id; // Identifica la clave primaria.
import jakarta.persistence.JoinColumn; // Define las claves foráneas.
import jakarta.persistence.ManyToOne; // Relaciona entidades.
import jakarta.persistence.Table; // Identifica la tabla PostgreSQL.
import java.time.LocalDateTime; // Representa fechas y horas.

@Entity // Representa una emergencia registrada.
@Table(name = "alerta") // Tabla existente en PostgreSQL.
public class Alerta {

    @Id // Clave primaria de la alerta.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL genera el ID.
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @ManyToOne(fetch = FetchType.LAZY) // Adulto mayor que genera la alerta.
    @JoinColumn(name = "id_adulto_mayor", nullable = false)
    private Usuario adultoMayor;

    @ManyToOne(fetch = FetchType.LAZY) // Estado actual de la alerta.
    @JoinColumn(name = "id_estado_actual", nullable = false)
    private EstadoAlerta estadoActual;

    @ManyToOne(fetch = FetchType.LAZY) // Asistente responsable de la atención.
    @JoinColumn(name = "id_responsable_atencion")
    private Usuario responsableAtencion;

    @ManyToOne(fetch = FetchType.LAZY) // Operador asignado a la emergencia.
    @JoinColumn(name = "id_operador_asignado")
    private Usuario operadorAsignado;

    @Column(name = "tipo_activacion", nullable = false)
    private String tipoActivacion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "observacion_cierre", columnDefinition = "text")
    private String observacionCierre;

    // Constructor requerido por JPA.
    public Alerta() {
    }

    public Integer getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(Integer idAlerta) {
        this.idAlerta = idAlerta;
    }

    public Usuario getAdultoMayor() {
        return adultoMayor;
    }

    public void setAdultoMayor(Usuario adultoMayor) {
        this.adultoMayor = adultoMayor;
    }

    public EstadoAlerta getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(EstadoAlerta estadoActual) {
        this.estadoActual = estadoActual;
    }

    public Usuario getResponsableAtencion() {
        return responsableAtencion;
    }

    public void setResponsableAtencion(Usuario responsableAtencion) {
        this.responsableAtencion = responsableAtencion;
    }

    public Usuario getOperadorAsignado() {
        return operadorAsignado;
    }

    public void setOperadorAsignado(Usuario operadorAsignado) {
        this.operadorAsignado = operadorAsignado;
    }

    public String getTipoActivacion() {
        return tipoActivacion;
    }

    public void setTipoActivacion(String tipoActivacion) {
        this.tipoActivacion = tipoActivacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaAtencion() {
        return fechaAtencion;
    }

    public void setFechaAtencion(LocalDateTime fechaAtencion) {
        this.fechaAtencion = fechaAtencion;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public void setObservacionCierre(String observacionCierre) {
        this.observacionCierre = observacionCierre;
    }
}
