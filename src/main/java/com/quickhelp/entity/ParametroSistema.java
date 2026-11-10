
package com.quickhelp.entity;

import jakarta.persistence.*; // Anotaciones de persistencia JPA.
import java.time.LocalDateTime; // Fecha de actualización.

@Entity // Representa la tabla parametrosistema.
@Table(name = "parametrosistema")
public class ParametroSistema {

    @Id // Clave primaria.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Integer idParametro;

    @Column(name = "clave", nullable = false)
    private String clave; // Identificador de la configuración.

    @Column(name = "valor", nullable = false, columnDefinition = "text")
    private String valor; // Valor almacenado.

    @Column(name = "tipo_dato", nullable = false)
    private String tipoDato; // Tipo de dato de la configuración.

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion; // Explicación del parámetro.

    @ManyToOne(fetch = FetchType.LAZY) // Usuario que actualizó la configuración.
    @JoinColumn(name = "id_usuario_actualiza")
    private Usuario usuarioActualiza;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion; // Última actualización.

    public ParametroSistema() {
    }

    @PrePersist // Asigna la fecha inicial.
    public void antesDeGuardar() {
        if (fechaActualizacion == null) fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdParametro() { return idParametro; }
    public void setIdParametro(Integer idParametro) { this.idParametro = idParametro; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public String getTipoDato() { return tipoDato; }
    public void setTipoDato(String tipoDato) { this.tipoDato = tipoDato; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Usuario getUsuarioActualiza() { return usuarioActualiza; }
    public void setUsuarioActualiza(Usuario usuarioActualiza) { this.usuarioActualiza = usuarioActualiza; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
