---
trigger: always_on
description: 
globs: 
---

# Regras para Migrations

## Regra Principal - NÃO Criar Novas Migrations

### Regra Fundamental
**SEMPRE** alterar a migration existente que cria a tabela ao invés de criar uma nova migration quando:
- Alterar um campo existente
- Adicionar um novo campo
- Remover um campo
- Alterar índices ou constraints

### ❌ ERRADO - Criar Nova Migration
```sql
-- V002__add_phone_to_user.sql
ALTER TABLE portal_user ADD COLUMN phone VARCHAR(15);
```

### ✅ CORRETO - Alterar Migration Existente
```sql
-- V001__create_user_table.sql (MODIFICAR ESTA)
CREATE TABLE portal_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255),
    cpf VARCHAR(11) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15), -- CAMPO ADICIONADO AQUI
    active BOOLEAN DEFAULT true,
    authorized BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

## Quando Criar Nova Migration

### Casos Permitidos
Criar nova migration APENAS quando:
- **Nova tabela** sendo criada
- **Dados de produção** já existem (não pode alterar migration executada)
- **Refatoração major** que requer múltiplas etapas
- **Correção de dados** específica

### Regras
- Criar sempre em lowercase 
- Não colocar comentários
- Primary keys, foreign key sempre colocar no final do create table exemplo: 
```sql
create table if not exists portal_user (
    id uuid not null,
    user_details_id uuid not null,
    address_id uuid,
    name varchar(150) not null,
    email varchar(255),
    cpf varchar(11) not null,
    password varchar(255) not null,
    active boolean default true,
    authorized boolean not null default false,
    responsible_token text,
    responsible_token_generated_at timestamp,
    responsible_token_expires_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp,
    constraint portal_user_pk primary key (id),
    constraint portal_user_user_details_fk foreign key (user_details_id) 	references portal_user_details(id) on delete cascade,
    constraint portal_user_address_fk foreign key (ad
```

## Estrutura de Migrations

### Nomenclatura
- **Padrão**: `V[número]__[descrição].sql`
- **Número**: Sequencial, sem lacunas
- **Descrição**: Em inglês, snake_case
- **Exemplo**: `V001__create_user_table.sql`

### Estrutura de Criação de Tabela
```sql
-- V001__create_user_table.sql
CREATE TABLE portal_user (
    -- Chave primária (sempre UUID)
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Campos obrigatórios
    name VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    
    -- Campos opcionais
    email VARCHAR(255),
    phone VARCHAR(15),
    
    -- Campos de controle
    active BOOLEAN DEFAULT true,
    authorized BOOLEAN DEFAULT false,
    
    -- Campos de auditoria (sempre incluir)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Índices
CREATE INDEX idx_portal_user_cpf ON portal_user(cpf);
CREATE INDEX idx_portal_user_email ON portal_user(email);
CREATE INDEX idx_portal_user_active ON portal_user(active);

-- Comentários
COMMENT ON TABLE portal_user IS 'Tabela de usuários do portal';
COMMENT ON COLUMN portal_user.cpf IS 'CPF único do usuário';
COMMENT ON COLUMN portal_user.authorized IS 'Indica se usuário foi autorizado pelo responsável';
```

## Padrões de Campos

### Campos Obrigatórios em Todas as Tabelas
```sql
-- Chave primária
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

-- Auditoria (sempre incluir)
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP
```

### Tipos de Dados Padrão
```sql
-- IDs
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL,

-- Textos
name VARCHAR(150) NOT NULL,
email VARCHAR(255),
description TEXT,

-- Números
age INTEGER,
amount DECIMAL(10,2),

-- Booleanos
active BOOLEAN DEFAULT true,
enabled BOOLEAN DEFAULT false,

-- Datas
birth_date DATE,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
expires_at TIMESTAMP,

-- Enums (sempre VARCHAR)
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
type VARCHAR(50) NOT NULL
```

### Foreign Keys
```sql
-- Sempre com constraint nomeada
CONSTRAINT fk_user_details FOREIGN KEY (user_details_id) REFERENCES user_details(id),
CONSTRAINT fk_guardian FOREIGN KEY (guardian_id) REFERENCES portal_user(id)
```

### Índices
```sql
-- Campos únicos
CREATE UNIQUE INDEX uk_portal_user_cpf ON portal_user(cpf);

-- Campos de busca frequente
CREATE INDEX idx_portal_user_email ON portal_user(email);
CREATE INDEX idx_portal_user_active ON portal_user(active);

-- Foreign keys (sempre indexar)
CREATE INDEX idx_user_dependent_guardian ON user_dependent(guardian_id);
CREATE INDEX idx_user_dependent_dependent ON user_dependent(dependent_id);

-- Índices compostos
CREATE INDEX idx_user_dependent_status_created ON user_dependent(status, created_at);
```

## Modificações em Migrations Existentes

### Adicionando Campo
```sql
-- V001__create_user_table.sql (MODIFICAR)
CREATE TABLE portal_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255),
    cpf VARCHAR(11) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15), -- ✅ NOVO CAMPO ADICIONADO AQUI
    birth_date DATE,   -- ✅ OUTRO CAMPO ADICIONADO
    active BOOLEAN DEFAULT true,
    authorized BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- ✅ ADICIONAR ÍNDICE SE NECESSÁRIO
CREATE INDEX idx_portal_user_phone ON portal_user(phone);
```

### Alterando Campo Existente
```sql
-- V001__create_user_table.sql (MODIFICAR)
CREATE TABLE portal_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL, -- ✅ ALTERADO DE 150 PARA 200
    email VARCHAR(255),
    cpf VARCHAR(11) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true,
    authorized BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Removendo Campo
```sql
-- V001__create_user_table.sql (MODIFICAR)
CREATE TABLE portal_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    -- email VARCHAR(255), -- ✅ CAMPO REMOVIDO
    cpf VARCHAR(11) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true,
    authorized BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

## Migrations para Relacionamentos

### OneToOne
```sql
-- Tabela principal
CREATE TABLE portal_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    user_details_id UUID NOT NULL UNIQUE, -- ✅ FK para OneToOne
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_user_details FOREIGN KEY (user_details_id) REFERENCES user_details(id)
);

-- Tabela relacionada
CREATE TABLE user_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    birth_date DATE NOT NULL,
    phone VARCHAR(15),
    special_needs BOOLEAN DEFAULT false,
    program_knowledge_source VARCHAR(50) NOT NULL,
    program_knowledge_source_other VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

### OneToMany / ManyToOne
```sql
-- Tabela "Many" (que possui a FK)
CREATE TABLE user_dependent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guardian_id UUID NOT NULL, -- ✅ FK para o "One"
    dependent_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_guardian FOREIGN KEY (guardian_id) REFERENCES portal_user(id),
    CONSTRAINT fk_dependent FOREIGN KEY (dependent_id) REFERENCES portal_user(id)
);

-- Índices para performance
CREATE INDEX idx_user_dependent_guardian ON user_dependent(guardian_id);
CREATE INDEX idx_user_dependent_dependent ON user_dependent(dependent_id);
```

## Comentários e Documentação

### Comentários Obrigatórios
```sql
-- Comentário da tabela
COMMENT ON TABLE portal_user IS 'Usuários do sistema portal PEL';

-- Comentários de campos importantes
COMMENT ON COLUMN portal_user.cpf IS 'CPF único do usuário (sem formatação)';
COMMENT ON COLUMN portal_user.authorized IS 'Indica se menor foi autorizado pelo responsável';
COMMENT ON COLUMN portal_user.responsible_token IS 'Token para autorização de menor de idade';

-- Comentários de constraints
COMMENT ON CONSTRAINT uk_portal_user_cpf IS 'CPF deve ser único no sistema';
```

### Documentação no Cabeçalho
```sql
-- V001__create_user_table.sql
-- 
-- Descrição: Criação da tabela principal de usuários
-- Autor: [Nome do desenvolvedor]
-- Data: 2024-08-26
-- 
-- Campos principais:
-- - id: Identificador único (UUID)
-- - cpf: Identificador único do usuário (11 dígitos)
-- - authorized: Controla se menor foi autorizado pelo responsável
-- 
-- Relacionamentos:
-- - OneToOne com user_details
-- - OneToMany com user_dependent (como guardian)
-- - ManyToOne com user_dependent (como dependent)

CREATE TABLE portal_user (
    -- ... definição da tabela
);
```

## Validação de Migrations

### Checklist Antes de Modificar Migration
- [ ] Verificar se migration já foi executada em produção
- [ ] Verificar se alteração não quebra dados existentes
- [ ] Verificar se novos campos têm valores padrão apropriados
- [ ] Verificar se índices necessários foram adicionados
- [ ] Verificar se comentários foram atualizados
- [ ] Testar migration em ambiente local
- [ ] Verificar se entidade JPA está sincronizada

### Sincronização com Entidades JPA
Após modificar migration, **SEMPRE** verificar se a entidade JPA está sincronizada:

```java
// ✅ Migration e entidade devem estar alinhadas
@Entity
@Table(name = "portal_user")
public class UserEntity extends BaseEntity {
    
    @Column(name = "phone", length = 15) // ✅ Mesmo tipo da migration
    private String phone;
    
    @Column(name = "birth_date") // ✅ Mesmo nome da migration
    private Date birthDate;
}
```

## Rollback e Versionamento

### Estratégia de Rollback
- **Desenvolvimento**: Pode alterar migration livremente
- **Staging**: Cuidado ao alterar migrations executadas
- **Produção**: NUNCA alterar migration executada

### Controle de Versão
```bash
# ✅ Commit migration modificada
git add src/main/resources/db/migration/V001__create_user_table.sql
git commit -m "feat: adicionar campo phone na tabela portal_user"

# ✅ Commit entidade sincronizada
git add src/main/java/.../UserEntity.java
git commit -m "feat: adicionar campo phone na entidade UserEntity"
```

## Checklist para Migrations

### Modificando Migration Existente
- [ ] Migration não foi executada em produção
- [ ] Novos campos têm valores padrão apropriados
- [ ] Campos obrigatórios têm NOT NULL
- [ ] Índices necessários foram adicionados
- [ ] Comentários foram atualizados
- [ ] Entidade JPA foi sincronizada
- [ ] Testado em ambiente local

### Criando Nova Migration
- [ ] Realmente necessário criar nova migration
- [ ] Nomenclatura seguindo padrão V[número]__[descrição].sql
- [ ] Inclui campos de auditoria (created_at, updated_at)
- [ ] Inclui comentários na tabela e campos importantes
- [ ] Inclui índices necessários
- [ ] Foreign keys com nomes de constraint
- [ ] Testado em ambiente local