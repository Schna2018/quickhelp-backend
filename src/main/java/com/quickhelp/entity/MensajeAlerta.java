package com.quickhelp.entity;

import jakarta.persistence.*; // Relaciona la entidad con PostgreSQL.
import java.time.LocalDateTime; // Fecha del mensaje.
import java.util.UUID; // Identificador para evitar envíos duplicados.

@Entity
@Table(name = "mensajealerta")
public class MensajeAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer idMensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_autor", nullable = false)
    private Usuario autor;

    @Column(name = "texto", nullable = false, length = 2000)
    private String texto;

    @Column(name = "clave", nullable = false)
    private UUID clave;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    public MensajeAlerta() {
    } // Constructor requerido por JPA.

    @PrePersist
    public void antesDeGuardar() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    public Integer getIdMensaje() { return idMensaje; }
    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public UUID getClave() { return clave; }
    public void setClave(UUID clave) { this.clave = clave; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}