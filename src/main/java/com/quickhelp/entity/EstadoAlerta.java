
package com.quickhelp.entity;

import jakarta.persistence.Column; // Configura las columnas.
import jakarta.persistence.Entity; // Define una entidad JPA.
import jakarta.persistence.Id; // Identifica la clave primaria.
import jakarta.persistence.Table; // Relaciona la entidad con PostgreSQL.

@Entity // Representa un estado de alerta.
@Table(name = "estadoalerta") // Tabla existente en PostgreSQL.
public class EstadoAlerta {

    @Id // Identificador del estado.
    @Column(name = "id_estado")
    private Integer idEstado;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "es_final", nullable = false)
    private Boolean esFinal;

    // Constructor requerido por JPA.
    public EstadoAlerta() {
    }

    // Permite consultar el identificador.
    public Integer getIdEstado() {
        return idEstado;
    }

    // Permite establecer el identificador.
    public void setIdEstado(Integer idEstado) {
        this.idEstado = idEstado;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getEsFinal() {
        return esFinal;
    }

    public void setEsFinal(Boolean esFinal) {
        this.esFinal = esFinal;
    }
}
