
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

@Entity // Representa una notificación de emergencia.
@Table(name = "notificacion") // Tabla existente en PostgreSQL.
public class Notificacion {

    @Id // Clave primaria de la notificación.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY) // Emergencia relacionada.
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY) // Usuario destinatario.
    @JoinColumn(name = "id_destinatario", nullable = false)
    private Usuario destinatario;

    @Column(name = "numero_intento")
    private Integer numeroIntento;

    @Column(name = "canal")
    private String canal;

    @Column(name = "estado_envio")
    private String estadoEnvio;

    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_inicio_proceso")
    private LocalDateTime fechaInicioProceso;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "resultado", columnDefinition = "text")
    private String resultado;

    // Identifica el tipo de aviso sin depender del resultado del envío.
    // Permite NULL para conservar las notificaciones históricas.
    @Column(name = "tipo_aviso", length = 30)
    private String tipoAviso;

    // Constructor requerido por JPA.
    public Notificacion() {
    }

    public Integer getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Integer idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public Alerta getAlerta() {
        return alerta;
    }

    public void setAlerta(Alerta alerta) {
        this.alerta = alerta;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    public Integer getNumeroIntento() {
        return numeroIntento;
    }

    public void setNumeroIntento(Integer numeroIntento) {
        this.numeroIntento = numeroIntento;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(String estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public LocalDateTime getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(LocalDateTime fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public LocalDateTime getFechaInicioProceso() {
        return fechaInicioProceso;
    }

    public void setFechaInicioProceso(LocalDateTime fechaInicioProceso) {
        this.fechaInicioProceso = fechaInicioProceso;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getTipoAviso() {
        return tipoAviso;
    }

    public void setTipoAviso(String tipoAviso) {
        this.tipoAviso = tipoAviso;
    }
}
