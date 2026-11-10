
package com.quickhelp.event;

// Evento que indica que se ha registrado una alerta de emergencia.
public record AlertaCreadaEvent(
    Integer idAlerta // Identificador de la alerta creada en PostgreSQL.
) {}
