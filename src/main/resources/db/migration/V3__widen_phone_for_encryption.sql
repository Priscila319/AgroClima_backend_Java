-- O telefone passa a ser guardado cifrado (AES-256-GCM, prefixo "enc:v1:" + Base64).
-- O texto cifrado de um telefone de até 30 caracteres cabe em ~100 caracteres; 255 dá folga.
-- Linhas antigas continuam em claro até o usuário ser salvo de novo (o conversor aceita os dois formatos).
ALTER TABLE app_user MODIFY phone VARCHAR(255) NOT NULL DEFAULT '';
