
-- ============================================================
-- QUICK HELP - MIGRACIÓN V2: ESCALAMIENTO SOS
-- ============================================================

-- Agrega el intervalo propio de cada emergencia.
-- NULL identifica alertas anteriores a esta implementación.
ALTER TABLE alerta
ADD COLUMN IF NOT EXISTS intervalo_escalamiento_minutos INTEGER;

-- Agrega un identificador permanente para el tipo de aviso.
-- NULL conserva las notificaciones históricas sin clasificar.
ALTER TABLE notificacion
ADD COLUMN IF NOT EXISTS tipo_aviso VARCHAR(30);

-- Recupera únicamente los tipos antiguos identificables
-- de forma explícita en la columna resultado.
UPDATE notificacion
SET tipo_aviso = resultado
WHERE tipo_aviso IS NULL
  AND resultado IN ('REAVISO_RED', 'ESCALAMIENTO_ADMIN');

-- No clasifica automáticamente otros registros históricos.
-- Los avisos nuevos tendrán su tipo asignado desde Java.

-- Evita duplicados futuros para los avisos identificados.
-- Antes de activar este índice, verificar que no existan
-- duplicados históricos de estos tipos.
CREATE UNIQUE INDEX IF NOT EXISTS
ux_notificacion_alerta_destinatario_tipo_push
ON notificacion (
    id_alerta,
    id_destinatario,
    tipo_aviso
)
WHERE canal = 'PUSH'
  AND tipo_aviso IN (
      'INICIAL_SOS',
      'REAVISO_RED',
      'ESCALAMIENTO_ADMIN'
  );
