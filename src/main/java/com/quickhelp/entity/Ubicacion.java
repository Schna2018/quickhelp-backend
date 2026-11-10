
package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones de persistencia JPA.
import java.math.BigDecimal; // Valores decimales precisos.
import java.time.LocalDateTime; // Fecha de registro.

@Entity // Representa la tabla ubicacion.
@Table(name = "ubicacion")
public class Ubicacion {

    @Id // Clave primaria.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    private Integer idUbicacion;

    @ManyToOne(fetch = FetchType.LAZY) // Alerta asociada a la ubicación.
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @Column(name = "latitud", nullable = false)
    private BigDecimal latitud; // Coordenada de latitud.

    @Column(name = "longitud", nullable = false)
    private BigDecimal longitud; // Coordenada de longitud.

    @Column(name = "precision_metros")
    private BigDecimal precisionMetros; // Precisión del GPS en metros.

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora; // Momento del registro.

    public Ubicacion() {
    }

    @PrePersist // Registra la fecha si no fue indicada.
    public void antesDeGuardar() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }

    public Integer getIdUbicacion() { return idUbicacion; }
    public void setIdUbicacion(Integer idUbicacion) { this.idUbicacion = idUbicacion; }

    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }

    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }

    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }

    public BigDecimal getPrecisionMetros() { return precisionMetros; }
    public void setPrecisionMetros(BigDecimal precisionMetros) {
        this.precisionMetros = precisionMetros;
    }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}
