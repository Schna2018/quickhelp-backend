
package com.quickhelp.dto;

public class AuthResponseDTO { // Respuesta del inicio de sesión.

    private String token; // Token JWT para consumir la API.
    private String tipo = "Bearer"; // Tipo de autenticación.
    private Integer idUsuario; // Identificador del usuario.
    private String nombreApellido; // Nombre del usuario.
    private String rol; // Código del rol asignado.

    public AuthResponseDTO(String token, Integer idUsuario, String nombreApellido, String rol) {
        this.token = token;
        this.idUsuario = idUsuario;
        this.nombreApellido = nombreApellido;
        this.rol = rol;
    }

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public Integer getIdUsuario() { return idUsuario; }
    public String getNombreApellido() { return nombreApellido; }
    public String getRol() { return rol; }
}
