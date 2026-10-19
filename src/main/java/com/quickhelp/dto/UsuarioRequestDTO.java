package com.quickhelp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UsuarioRequestDTO { // DTO que define los datos permitidos al crear o recibir un usuario desde la app o panel web.

    @NotNull(message = "El rol es obligatorio") // Evita crear un usuario sin rol.
    private Integer idRol;

    @NotBlank(message = "El nombre y apellido son obligatorios") // No permite texto vacío.
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres") // Respeta el tamaño definido en PostgreSQL.
    private String nombreApellido;

    @NotBlank(message = "El correo es obligatorio") // El correo debe venir informado.
    @Email(message = "El correo no tiene un formato válido") // Verifica formato tipo usuario@correo.com.
    private String correo;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefono; // Teléfono opcional del usuario.

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password; // Contraseña recibida; después se convertirá en hash antes de guardarla.

    private String tokenFcm; // Token de Firebase para notificaciones push de la aplicación móvil.

    @Size(max = 100, message = "La frase de activación no puede superar 100 caracteres")
    private String fraseActivacion; // Frase utilizada para la futura activación por voz.

    private Boolean vozActiva = false; // Por defecto la activación por voz queda deshabilitada.
    private Boolean estado = true; // Por defecto el usuario se crea activo.

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public String getNombreApellido() { return nombreApellido; }
    public void setNombreApellido(String nombreApellido) { this.nombreApellido = nombreApellido; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTokenFcm() { return tokenFcm; }
    public void setTokenFcm(String tokenFcm) { this.tokenFcm = tokenFcm; }

    public String getFraseActivacion() { return fraseActivacion; }
    public void setFraseActivacion(String fraseActivacion) { this.fraseActivacion = fraseActivacion; }

    public Boolean getVozActiva() { return vozActiva; }
    public void setVozActiva(Boolean vozActiva) { this.vozActiva = vozActiva; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
}