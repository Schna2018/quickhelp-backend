
package com.quickhelp.dto;

import jakarta.validation.constraints.Email; // Valida el formato del correo.
import jakarta.validation.constraints.NotBlank; // Impide campos vacíos.
import jakarta.validation.constraints.Size; // Controla la longitud de los datos.

public class RegistroUsuarioDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombreApellido; // Nombre completo del usuario.

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String correo; // Correo utilizado para iniciar sesión.

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefono; // Número de contacto opcional.

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password; // Contraseña que será cifrada en el servicio.

    @Size(max = 20, message = "El código de vinculación no puede superar 20 caracteres")
    private String codigoVinculacion; // Código del adulto mayor ingresado por el asistente.

    public String getNombreApellido() {
        return nombreApellido;
    }

    public void setNombreApellido(String nombreApellido) {
        this.nombreApellido = nombreApellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCodigoVinculacion() {
        return codigoVinculacion; // Obtiene el código recibido durante el registro.
    }

    public void setCodigoVinculacion(String codigoVinculacion) {
        this.codigoVinculacion = codigoVinculacion; // Asigna el código enviado por la aplicación.
    }

}
