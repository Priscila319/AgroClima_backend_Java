CREATE TABLE app_role (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(240) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT uq_app_role_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE app_permission (
    id CHAR(36) NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    description VARCHAR(240) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT uq_app_permission_key UNIQUE (permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE app_user (
    id CHAR(36) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL DEFAULT '',
    organization VARCHAR(160) NOT NULL DEFAULT '',
    job_title VARCHAR(120) NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    last_login_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_failed_login_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_app_user_status ON app_user(status);

CREATE TABLE user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES app_role(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE role_permissions (
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES app_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES app_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE grape_variety (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT '',
    active BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uq_grape_variety_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO grape_variety (id, name, category) VALUES
    ('11111111-1111-4111-8111-000000000001', 'Uva Itália', 'Uva de mesa'),
    ('11111111-1111-4111-8111-000000000002', 'Crimson Seedless', 'Sem sementes'),
    ('11111111-1111-4111-8111-000000000003', 'Thompson Seedless', 'Sem sementes'),
    ('11111111-1111-4111-8111-000000000004', 'Sweet Globe', 'Sem sementes'),
    ('11111111-1111-4111-8111-000000000005', 'Uva Vitória', 'Sem sementes');

CREATE TABLE plantation (
    id CHAR(36) NOT NULL,
    owner_id CHAR(36) NOT NULL,
    variety VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    field_name VARCHAR(80) NOT NULL DEFAULT '',
    notes VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL,
    planted_at DATETIME(6) NOT NULL,
    harvested_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_plantation_owner FOREIGN KEY (owner_id) REFERENCES app_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_plantation_variety FOREIGN KEY (variety) REFERENCES grape_variety(name) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_plantation_quantity CHECK (quantity > 0),
    CONSTRAINT ck_plantation_status CHECK (status IN ('EM_CULTIVO', 'COLHIDA', 'ARQUIVADA')),
    CONSTRAINT ck_plantation_harvest_state CHECK (
      (status = 'EM_CULTIVO' AND harvested_at IS NULL AND archived_at IS NULL) OR
      (status = 'COLHIDA' AND harvested_at IS NOT NULL AND archived_at IS NULL) OR
      (status = 'ARQUIVADA' AND archived_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_plantation_owner_planted ON plantation(owner_id, planted_at);
CREATE INDEX idx_plantation_variety ON plantation(variety);
CREATE INDEX idx_plantation_status ON plantation(status);

CREATE TABLE climate_reading (
    id CHAR(36) NOT NULL,
    channel_id VARCHAR(100) NOT NULL,
    entry_id BIGINT NOT NULL,
    sensor_code VARCHAR(100) NOT NULL,
    captured_at DATETIME(6) NOT NULL,
    measurements JSON NOT NULL,
    quality_status VARCHAR(24) NOT NULL DEFAULT 'VALID',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_climate_channel_entry UNIQUE (channel_id, entry_id),
    CONSTRAINT ck_climate_quality CHECK (quality_status IN ('VALID', 'REVIEW', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_climate_reading_captured ON climate_reading(captured_at);
CREATE INDEX idx_climate_reading_channel_captured ON climate_reading(channel_id, captured_at);
CREATE INDEX idx_climate_reading_quality_captured ON climate_reading(quality_status, captured_at);

CREATE TABLE system_setting (
    id CHAR(36) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    json_value JSON NOT NULL,
    updated_by CHAR(36) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_system_setting_key UNIQUE (setting_key),
    CONSTRAINT fk_system_setting_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_event (
    id CHAR(36) NOT NULL,
    actor_id CHAR(36) NULL,
    actor_name VARCHAR(160) NOT NULL,
    actor_email VARCHAR(254) NOT NULL DEFAULT '',
    action VARCHAR(160) NOT NULL,
    resource VARCHAR(160) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    source VARCHAR(200) NOT NULL DEFAULT 'API',
    status VARCHAR(24) NOT NULL DEFAULT 'CONCLUIDO',
    details TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_event_actor FOREIGN KEY (actor_id) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_audit_event_time ON audit_event(occurred_at);
CREATE INDEX idx_audit_event_resource ON audit_event(resource, occurred_at);
CREATE INDEX idx_audit_event_actor ON audit_event(actor_id, occurred_at);

INSERT INTO app_role (id, name, description) VALUES
    ('22222222-2222-4222-8222-000000000001', 'PRODUCER', 'Produtor/Exportador'),
    ('22222222-2222-4222-8222-000000000002', 'ANALYST', 'Analista de Dados'),
    ('22222222-2222-4222-8222-000000000003', 'ADMIN', 'Administrador');

INSERT INTO app_permission (id, permission_key, description) VALUES
    ('33333333-3333-4333-8333-000000000001', 'overview.view', 'Ver painéis e visão geral'),
    ('33333333-3333-4333-8333-000000000002', 'plantations.manage', 'Cadastrar e acompanhar as próprias plantações'),
    ('33333333-3333-4333-8333-000000000003', 'plantations.read.all', 'Consultar plantações de todos os produtores'),
    ('33333333-3333-4333-8333-000000000004', 'monitoring.view', 'Consultar monitoramento climático'),
    ('33333333-3333-4333-8333-000000000005', 'analytics.view', 'Consultar análises, comparações e modelos'),
    ('33333333-3333-4333-8333-000000000006', 'reports.view', 'Consultar e gerar relatórios'),
    ('33333333-3333-4333-8333-000000000007', 'users.manage', 'Consultar e gerenciar usuários'),
    ('33333333-3333-4333-8333-000000000008', 'rbac.manage', 'Gerenciar papéis e permissões'),
    ('33333333-3333-4333-8333-000000000009', 'security.manage', 'Consultar políticas de segurança'),
    ('33333333-3333-4333-8333-000000000010', 'logs.read', 'Consultar registros de auditoria'),
    ('33333333-3333-4333-8333-000000000011', 'settings.manage', 'Alterar configurações do sistema'),
    ('33333333-3333-4333-8333-000000000012', 'design.manage', 'Visualizar e testar o design system'),
    ('33333333-3333-4333-8333-000000000013', 'integrations.manage', 'Consultar integrações');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM app_role r CROSS JOIN app_permission p
WHERE r.name = 'PRODUCER' AND p.permission_key IN (
    'overview.view', 'plantations.manage', 'monitoring.view', 'analytics.view', 'reports.view'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM app_role r CROSS JOIN app_permission p
WHERE r.name = 'ANALYST' AND p.permission_key IN (
    'overview.view', 'plantations.read.all', 'monitoring.view', 'analytics.view', 'reports.view', 'integrations.manage'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM app_role r CROSS JOIN app_permission p
WHERE r.name = 'ADMIN';
