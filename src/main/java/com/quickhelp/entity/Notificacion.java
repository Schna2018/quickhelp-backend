
package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones para mapear la tabla en PostgreSQL.
import java.time.LocalDateTime; // Manejo de fechas y horas.

@Entity // Identifica la clase como una entidad JPA.
@Table(name = "notificacion") // Tabla existente en PostgreSQL.
public class Notificacion {

    @Id // Clave primaria de la notificación.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID generado por PostgreSQL.
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY) // Emergencia asociada.
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY) // Usuario que recibe la notificación.
    @JoinColumn(name = "id_destinatario", nullable = false)
    private Usuario destinatario;

    @Column(name = "numero_intento", nullable = false)
    private Integer numeroIntento;

    @Column(name = "canal", nullable = false)
    private String canal;

    @Column(name = "estado_envio", nullable = false)
    private String estadoEnvio;

    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_inicio_proceso")
    private LocalDateTime fechaInicioProceso; // Momento en que se reserva el envío.

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "resultado", columnDefinition = "text")
    private String resultado;

    // Identificador de la notificación.
    public Integer getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Integer idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    // Emergencia relacionada.
    public Alerta getAlerta() {
        return alerta;
    }

    public void setAlerta(Alerta alerta) {
        this.alerta = alerta;
    }

    // Usuario destinatario.
    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    // Número de intentos de envío.
    public Integer getNumeroIntento() {
        return numeroIntento;
    }

    public void setNumeroIntento(Integer numeroIntento) {
        this.numeroIntento = numeroIntento;
    }

    // Canal utilizado para la notificación.
    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    // Estado del envío.
    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(String estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    // Fecha en que se programó el envío.
    public LocalDateTime getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(LocalDateTime fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    // Fecha de inicio del procesamiento.
    public LocalDateTime getFechaInicioProceso() {
        return fechaInicioProceso;
    }

    public void setFechaInicioProceso(LocalDateTime fechaInicioProceso) {
        this.fechaInicioProceso = fechaInicioProceso;
    }

    // Fecha efectiva de envío.
    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    // Resultado o detalle del intento.
    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }
}
