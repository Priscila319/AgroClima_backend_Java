# Backend Java — AgroClima API

API Spring Boot responsável por autenticação JWT, RBAC, usuários, variedades, plantações, configurações, auditoria e persistência em **MySQL Server**. O ORM continua sendo JPA/Hibernate; MySQL Workbench é opcional e serve como cliente gráfico para administrar o servidor, não como banco ou ORM. O serviço Python consome esta API e não acessa o banco diretamente.

## Requisitos locais

- JDK 21;
- Maven 3.9+;
- MySQL Server 8.4+ em `localhost:3306`;
- MySQL Workbench opcional.

Docker não é necessário para iniciar ou desenvolver a API.

## Criar o banco e configurar localmente

No MySQL Workbench, conecte-se ao servidor local e execute no SQL Editor (como usuário administrativo):

```sql
CREATE DATABASE agroclima
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'agroclima'@'localhost' IDENTIFIED BY 'DEFINA_UMA_SENHA_LOCAL';
GRANT ALL PRIVILEGES ON agroclima.* TO 'agroclima'@'localhost';
```

Depois de copiar `backend/java/.env.example` para `backend/java/.env`, configure:

- `DATABASE_URL=jdbc:mysql://localhost:3306/agroclima`, `DATABASE_USER` e `DATABASE_PASSWORD` para o servidor local;
- `JWT_SECRET` e `INTERNAL_API_KEY` como segredos aleatórios independentes com pelo menos 32 bytes;
- `FIELD_ENCRYPTION_KEY`: chave AES-256 (Base64 de 32 bytes, `openssl rand -base64 32`) que cifra o telefone dos usuários no banco. Guarde-a fora do Git; sem ela a API não sobe e, se for perdida, os telefones cifrados não podem ser recuperados;
- `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD` para criar o primeiro Administrador;
- `DATABASE_SSL_MODE` (`PREFERRED` por padrão; use `REQUIRED` ou `VERIFY_CA` em produção) e `SWAGGER_ENABLED` (`false` por padrão);
- `CORS_ALLOWED_ORIGINS` com as origens locais permitidas, sem wildcard;
- `SERVER_ADDRESS=127.0.0.1` para o processo local e `PORT=8080`.

O Spring Boot não lê `.env` automaticamente. Use `bash backend/java/run-local.sh` em Linux/macOS ou `backend/java/run-local.ps1` no PowerShell; os scripts carregam as variáveis e iniciam `mvn spring-boot:run`. Também é possível exportar as variáveis manualmente.

A senha administrativa deve ter pelo menos 8 caracteres, conter letra e número e obedecer ao limite de tamanho. Não versione o `.env` real. O Workbench sozinho não substitui o MySQL Server: ambos precisam estar instalados/iniciados para abrir uma conexão.

A migration `src/main/resources/db/migration/V1__create_agroclima_schema.sql` cria as tabelas da aplicação, os papéis/permissões e as cinco variedades existentes no frontend. Flyway aplica a migration no startup e Hibernate valida o schema (`ddl-auto=validate`). Após iniciar a API, atualize os schemas no Workbench para conferir as tabelas. O projeto não migra dados já existentes em PostgreSQL; use um schema MySQL limpo e trate uma eventual transferência de dados separadamente.

## Executar e verificar

```bash
bash backend/java/run-local.sh
```

A API local fica em `http://localhost:8080`:

- Swagger UI: `/swagger-ui.html` (somente com `SWAGGER_ENABLED=true`);
- OpenAPI JSON: `/v3/api-docs` (somente com `SWAGGER_ENABLED=true`);
- Health: `/actuator/health`.

## Prefixos e proteção

- `/api/auth/login`: público (emite JWT). `/api/auth/me`: autenticado; devolve o perfil e as permissões efetivas (`permissionKeys`). **Cadastro público removido** — não existe `/api/auth/register`;
- `/api/users/**`: exige `users.manage` (criação de contas é exclusiva do administrador). Telefones são validados e normalizados para dígitos com DDI 55;
- `/api/roles/**`, `/api/permissions`: exigem `rbac.manage`;
- `/api/settings`: leitura/escrita exigem `settings.manage`. `/api/settings/public`: qualquer usuário autenticado (identidade visual, tema e tokens de design);
- `/api/audit-events`: leitura exige `logs.read`; registro de evento próprio exige autenticação;
- `/api/plantations/**`: escopo por proprietário, papel e permissões aplicados no servidor; datas controladas pela API;
- `/api/varieties/**`: consulta autenticada; inclusão e desativação somente pelo Administrador;
- `/api/climate/readings/**`: consulta autenticada;
- `/api/internal/climate-readings/**`: integração Python → Java autenticada por `X-Internal-Api-Key`.

A sessão usa JWT stateless; senhas são armazenadas com BCrypt. O logout remove o token do cliente; a validade é controlada por `JWT_TTL_MINUTES`. Falhas consecutivas podem bloquear a conta temporariamente.

## Testes

```bash
cd backend/java
mvn test
```

Testes unitários não precisam de Docker. `MySqlPersistenceTest` usa Testcontainers com MySQL e é configurado para ser ignorado se Docker não estiver disponível. Sem Docker, valide o schema e a persistência executando a API contra o MySQL Server local configurado acima.
