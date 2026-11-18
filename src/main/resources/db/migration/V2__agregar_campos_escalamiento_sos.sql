
-- ============================================================
-- QUICK HELP - MIGRACIÓN V2: ESCALAMIENTO SOS
-- ============================================================

-- Agrega el intervalo propio de cada emergencia.
-- NULL identifica alertas anteriores a esta implementación.
ALTER TABLE alerta
ADD COLUMN IF NOT EXISTS intervalo_escalamiento_minutos INTEGER;

-- Agrega un identificador permanente para el tipo de aviso.
ALTER TABLE notificacion
ADD COLUMN IF NOT EXISTS tipo_aviso VARCHAR(30);

-- Recupera los tipos de avisos antiguos que fueron
-- almacenados temporalmente en la columna resultado.
UPDATE notificacion
SET tipo_aviso = resultado
WHERE tipo_aviso IS NULL
  AND resultado IN ('REAVISO_RED', 'ESCALAMIENTO_ADMIN');

-- Identifica las notificaciones iniciales anteriores.
-- No modifica el resultado del envío registrado.
UPDATE notificacion
SET tipo_aviso = 'INICIAL_SOS'
WHERE tipo_aviso IS NULL;

-- Impide duplicar el mismo tipo de aviso PUSH para
-- una alerta y un destinatario.
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
