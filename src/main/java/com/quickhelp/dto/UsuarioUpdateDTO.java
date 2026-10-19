package com.quickhelp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UsuarioUpdateDTO { // DTO con los datos permitidos para actualizar un usuario.

    @NotNull(message = "El rol es obligatorio")
    private Integer idRol; // ID del rol que tendrá el usuario.

    @NotBlank(message = "El nombre y apellido son obligatorios")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombreApellido; // Nombre completo del usuario.

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo; // Correo del usuario.

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefono; // Teléfono opcional.

    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password; // Opcional: solo se cambia si viene informado.

    private String tokenFcm; // Token utilizado para notificaciones móviles.

    @Size(max = 100, message = "La frase de activación no puede superar 100 caracteres")
    private String fraseActivacion; // Frase utilizada para activación por voz.

    private Boolean vozActiva; // Estado de la función de voz.
    private Boolean estado; // Estado activo/inactivo del usuario.

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