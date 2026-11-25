-- Etapa de modelos preditivos: permissão para treinar/remover modelos de machine learning.
-- A consulta dos modelos (status/projeção) continua coberta por analytics.view; apenas as
-- operações de escrita (treinar, remover) exigem models.manage. Concedida a ADMIN e ANALYST.
-- Não altera a V1 (checksum do Flyway): esta é uma migração incremental.

INSERT INTO app_permission (id, permission_key, description) VALUES
    ('33333333-3333-4333-8333-000000000014', 'models.manage', 'Treinar e remover modelos preditivos');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM app_role r CROSS JOIN app_permission p
WHERE r.name IN ('ADMIN', 'ANALYST') AND p.permission_key = 'models.manage';
