---
trigger: always_on
description: 
globs: 
---

# Regras Windsurf - PEL Portal API

Este documento contém as regras principais do projeto. Para regras específicas de cada contexto, consulte os arquivos individuais:

- [Arquitetura e Estrutura](arquitetura.md)
- [Entidades](entidades.md)
- [Enums](enums.md)
- [DTOs](dtos.md)
- [Use Cases](usecases.md)
- [Controllers](controllers.md)
- [Gateways](gateways.md)
- [Eventos](eventos.md)
- [Factories](./factories.md)
- [Serviços de Domínio](./servicos-dominio.md)
- [Exceções](./excecoes.md)
- [Validações](./validacoes.md)
- [Testes](testes.md)
- [Migrations](migrations.md)
- [Qualidade de Código](./qualidade-codigo.md)
- [Segurança](./seguranca.md)
- [Performance](./performance.md)
- [Documentação](./documentacao.md)
- [Deploy](./deploy.md)

## Princípios Fundamentais

### Domain-Driven Design (DDD) & Clean Architecture
- Seguir princípios DDD com limites de domínio claros
- Implementar Clean Architecture com inversão de dependência
- O domínio core não deve depender de frameworks externos (exceto anotações de entidade)
- Todas as dependências externas devem ser acessadas através de gateways

### Regras Gerais
- **Use Cases não podem ser chamados entre si** - deve ser disparado e criado evento
- **Entidades sempre estender de BaseEntity**
- **Enums sempre implementar IEnum e criar classe estática para Converter**
- **DTOs sempre estender de BaseDTO**
- **Use cases ter somente um método público chamado execute**
- **Tudo que for externo, utilize biblioteca ou framework deve ter um gateway para utilizar no core**
- **Pode ser criado validators em /domain/service se necessário para utilizar dentro de entidades, factories ou useCases**

## Checklist Rápido

### Criando uma Nova Entidade
- [ ] Estende `BaseEntity`
- [ ] Possui anotações obrigatórias
- [ ] Possui construtor com validação
- [ ] Usa UUID como chave primária
- [ ] Possui `@EqualsAndHashCode` e `@ToString` corretos

### Criando um Novo Use Case
- [ ] Método único `execute`
- [ ] `@Service` e `@RequiredArgsConstructor`
- [ ] Usa `@Transactional` se necessário
- [ ] Depende apenas de interfaces de gateway
- [ ] Publica eventos ao invés de chamar outros Use Cases

### Criando um Novo Controller
- [ ] Possui interface com anotações
- [ ] Implementação delega para Use Cases
- [ ] Usa códigos de status HTTP apropriados
- [ ] Valida entrada com `@Valid`

### Criando um Novo Gateway
- [ ] Interface em `/core/gateway/`
- [ ] Implementação em `/infra/gateway/`
- [ ] Trata dependências externas
- [ ] Tratamento de erro adequado

Lembre-se: **O domínio core é o coração da aplicação. Mantenha-o limpo, focado e independente de preocupações externas.**
