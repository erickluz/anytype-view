---
trigger: always_on
description: 
globs: 
---

# Arquitetura e Estrutura do Projeto

## Princípios de Arquitetura

### Domain-Driven Design (DDD) & Clean Architecture

- Seguir princípios DDD com limites de domínio claros
- Implementar Clean Architecture com inversão de dependência
- O domínio core não deve depender de frameworks externos (exceto anotações de entidade)
- Todas as dependências externas devem ser acessadas através de gateways

### Estrutura de Pacotes

```
com.wp.orchestratorl/
├── core/                           # Camada de domínio core
│   ├── domain/
│   │   ├── entity/                 # Entidades de domínio
│   │   ├── enums/                  # Enums de domínio
│   │   ├── factory/                # Factories de entidades
│   │   ├── service/                # Serviços de domínio
│   │   └── usecase/                # Use cases (camada de aplicação)
│   ├── dto/                        # Data Transfer Objects
│   ├── event/                      # Eventos de domínio
│   ├── exception/                  # Exceções customizadas
│   ├── gateway/                    # Interfaces de gateway
│   ├── mail/                       # Templates de email
│   └── util/                       # Utilitários do core
├── inbound/                        # Adaptadores para requisições de entrada
│   └── http/                       # Controllers HTTP
│       └── interfaces/             # Interfaces dos controllers
├── infra/                          # Camada de infraestrutura
│   ├── config/                     # Classes de configuração
│   ├── constants/                  # Constantes da aplicação
│   ├── dto/                        # DTOs de infraestrutura
│   ├── gateway/                    # Implementações de gateway
│   ├── repository/                 # Repositórios JPA
│   ├── security/                   # Configurações de segurança
│   └── util/                       # Utilitários de infraestrutura
└── PortalApplication.java          # Classe principal da aplicação
```

## Regras de Dependência

### Camada Core

- **NÃO DEVE** depender de frameworks externos (exceto anotações JPA em entidades)
- **NÃO DEVE** depender da camada de infraestrutura
- **PODE** usar anotações do framework apenas em entidades (@Entity, @Table, etc.)
- **DEVE** definir interfaces para todas as dependências externas

### Camada Inbound

- **PODE** depender da camada core
- **NÃO DEVE** depender diretamente da camada de infraestrutura
- **DEVE** usar apenas interfaces de gateway do core

### Camada Infra

- **PODE** depender da camada core (apenas interfaces)
- **DEVE** implementar todas as interfaces de gateway definidas no core
- **DEVE** conter toda a lógica específica de framework

## Comunicação Entre Camadas

### Use Cases

- **NÃO PODEM** chamar outros Use Cases diretamente
- **DEVEM** usar eventos (`ApplicationEventPublisher`) para comunicação
- **DEVEM** depender apenas de interfaces de gateway

### Gateways

- **DEVEM** ter interface no core e implementação na infra
- **DEVEM** abstrair todas as dependências externas
- **DEVEM** converter entre objetos de domínio e externos

### Eventos

- **DEVEM** ser usados para comunicação assíncrona
- **DEVEM** manter baixo acoplamento entre componentes
- **PODEM** ser processados de forma assíncrona

## Validação da Arquitetura

### Testes de Arquitetura

- Verificar que core não depende de infra
- Verificar que Use Cases não se chamam diretamente
- Verificar que gateways são usados corretamente
- Verificar estrutura de pacotes

### Code Review

- Verificar inversão de dependência
- Verificar uso correto de gateways
- Verificar separação de responsabilidades
- Verificar princípios DDD

## Exemplo de Fluxo

```
HTTP Request → Controller → Use Case → Gateway Interface → Gateway Implementation → External Service/Database
                    ↓
                 Event Publisher → Event Handler → Another Use Case
```

## Benefícios da Arquitetura

- **Testabilidade**: Fácil mock de dependências
- **Manutenibilidade**: Código organizado e desacoplado
- **Flexibilidade**: Fácil troca de implementações
- **Escalabilidade**: Arquitetura preparada para crescimento
- **Qualidade**: Princípios SOLID aplicados
