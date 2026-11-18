
-- Quick Help: migración para el escalamiento SOS.
-- Agrega columnas sin eliminar los registros existentes.

-- Conserva el intervalo asignado a cada emergencia.
ALTER TABLE alerta
ADD COLUMN IF NOT EXISTS intervalo_escalamiento_minutos INTEGER;

-- Identifica el tipo de notificación.
ALTER TABLE notificacion
ADD COLUMN IF NOT EXISTS tipo_aviso VARCHAR(30);
