package com.quickhelp.event;

// Identifica el mensaje nuevo sin incluir el texto del chat.
public record MensajeAlertaCreadoEvent(Integer idAlerta, Integer idMensaje) {}
