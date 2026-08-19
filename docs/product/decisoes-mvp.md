# Decisões do MVP

Este documento registra as decisões consolidadas para orientar a implementação inicial do Sistema Monitoramento Anytype.

## Objetivo do MVP

Construir um sistema local, single-user e somente leitura em relação ao Anytype para dar visibilidade ao progresso de estudos registrado no espaço Evolução Técnica.

O Anytype permanece como fonte de verdade dos dados de conhecimento. O sistema auxiliar calcula métricas, mantém histórico mínimo local e apresenta um dashboard.

## Escopo Confirmado

- Integração REST com a API local do Anytype.
- Leitura do espaço Evolução Técnica.
- Leitura dos tipos centrais: Tema, Conceito, Checkpoint de Conhecimento e Aplicação.
- Leitura das propriedades necessárias para reconstruir a modelagem existente.
- Cálculo do estado atual em memória a partir dos dados consultados no Anytype.
- Persistência local apenas de histórico mínimo e snapshots diários.
- Dashboard local simples.
- Sincronização manual e automática.

## Fora do Escopo do MVP

- Escrita ou alteração automática no Anytype.
- Alteração da modelagem do espaço Evolução Técnica.
- IA.
- Recomendações de estudo.
- Previsões.
- Score próprio de conhecimento.
- Multiusuário.
- Login/autenticação própria.
- Aplicação desktop empacotada.
- Registro manual de tempo dedicado aos estudos.

## Fonte de Verdade

O Anytype é a fonte de verdade para:

- objetos de conhecimento;
- tipos;
- propriedades;
- relações;
- conteúdo dos objetos;
- estado atual da modelagem.

O banco local não deve ser tratado como réplica do Anytype. Ele existe apenas para armazenar histórico de observação e dados mínimos necessários para métricas temporais.

## Persistência Local

Banco recomendado para o MVP: SQLite.

Motivos:

- adequado para aplicação local e single-user;
- não exige servidor;
- baixo esforço operacional;
- simples de empacotar futuramente;
- suficiente para snapshots, alterações detectadas e métricas históricas.

O sistema deve evitar duplicar dados completos do Anytype. Não devem ser persistidos no banco local:

- corpo completo dos conceitos;
- blocos completos dos objetos;
- anexos;
- textos longos sem uso direto nas métricas;
- propriedades irrelevantes para o dashboard.

O histórico mínimo pode conter:

- data do snapshot;
- identificador do objeto no Anytype;
- tipo do objeto;
- nome do objeto;
- status de arquivamento;
- `last_modified_date`;
- hash das propriedades relevantes;
- propriedades relevantes para métricas, em formato normalizado ou JSON reduzido.

## Estratégia de Cálculo

O estado atual deve ser calculado preferencialmente em memória:

1. Consultar o Anytype.
2. Montar o grafo atual de temas, conceitos, checkpoints e aplicações.
3. Calcular métricas atuais em memória.
4. Persistir somente o snapshot diário mínimo.
5. Usar snapshots locais para métricas históricas.

Essa abordagem evita transformar o banco local em uma segunda base de conhecimento.

## Snapshots Diários

O sistema deve manter um snapshot consolidado por dia civil no timezone `America/Sao_Paulo`.

Regras:

- quando o sistema rodar mais de uma vez no mesmo dia, o snapshot do dia pode ser substituído pela versão mais recente;
- snapshots criados por observação real da API devem ser marcados como reais;
- dias sem execução devem ser preenchidos retroativamente com snapshots sintéticos do tipo `carry-forward`;
- snapshots sintéticos carregam o último estado conhecido e devem ser marcados explicitamente como sintéticos.

O sistema não deve tentar reconstruir exatamente o estado histórico de dias em que não executou, pois a API do Anytype fornece o estado atual observado, não uma linha do tempo completa.

## Atividade e Alterações

Critério inicial de atividade:

```text
atividade = objeto relevante teve last_modified_date alterado
```

Critério inicial de alteração:

```text
alterado = last_modified_date mudou ou hash das propriedades relevantes mudou
```

A atividade retroativa pode ser inferida por `last_modified_date`. Exemplo: se o sistema rodar hoje e encontrar um objeto com `last_modified_date` de três dias atrás, pode registrar atividade inferida naquela data.

Essa inferência não equivale a um snapshot real daquele dia. Ela indica apenas que houve alteração em um objeto naquela data.

## Identificação de Tipos e Propriedades

Para o MVP, tipos e propriedades devem ser identificados por nome configurável e validados durante a sincronização.

Depois de localizados, os `id` e `key` retornados pela API podem ser usados internamente e persistidos localmente como cache/configuração observada.

Tipos esperados:

- `Tema`
- `Conceito`
- `Checkpoint de Conhecimento`
- `Aplicação`

Propriedades relevantes esperadas:

- `Tipo`
- `Links`
- `Prioridade`
- `Tag`
- `Categoria`
- `Veredito`
- `Entendimento`
- `Tradeoff`
- `Checkpoint`
- `Tema`
- `Conecta com`
- `Última Revisão`
- `Stack relacionada`
- `Lacunas`
- `Aplicação Prática`
- `Vendabilidade`
- `Nível Percebido`
- `Status`
- `Nível Implementação`
- `GitHub`

## Configuração Local

A API key será gerada manualmente pelo usuário na interface do Anytype e cadastrada no sistema.

Configurações iniciais:

- `ANYTYPE_BASE_URL`, padrão `http://127.0.0.1:31009`;
- `ANYTYPE_VERSION`, padrão `2025-11-08`;
- `ANYTYPE_API_KEY`, informada manualmente;
- `ANYTYPE_SPACE_NAME`, padrão `Evolução Técnica`.

Para o início do MVP, a configuração pode ser feita por variável de ambiente ou arquivo local ignorado pelo Git. A API key não deve ser versionada nem registrada em logs.

Uma evolução posterior aceitável é armazenar a API key no Windows Credential Manager.

## Métricas Iniciais

As métricas iniciais devem responder aos problemas de progresso, ritmo, interrupção e volatilidade.

### Tamanho Atual

- total de temas;
- total de conceitos;
- total de checkpoints;
- total de aplicações;
- conceitos por tema;
- subtemas por tema.

### Evolução do Tamanho

- novos conceitos por dia, semana e mês;
- novos temas ou subtemas por período;
- novos checkpoints por período;
- variação do total de objetos observados.

### Atividade Inferida

- dias com alterações em conceitos ou checkpoints;
- semanas com pelo menos uma alteração;
- quantidade de objetos alterados por período;
- última data de atividade detectada;
- sequência atual de dias ou semanas sem atividade.

### Interrupções

- períodos sem alterações acima de um limite configurável;
- duração média das interrupções;
- maior interrupção;
- data da última retomada após interrupção.

### Progresso por Entendimento

- quantidade de conceitos por `Entendimento`;
- percentual por nível de entendimento;
- evolução desses percentuais ao longo do tempo.

### Progresso por Tema

- conceitos por tema;
- distribuição de `Entendimento` por tema;
- temas sem checkpoint recente;
- temas com maior quantidade de conceitos em níveis baixos de entendimento.

### Checkpoints

- checkpoints criados ou alterados por período;
- dias desde a `Última Revisão`;
- temas com checkpoint mais antigo;
- média de `Vendabilidade`;
- distribuição de `Nível Percebido`;
- checkpoints com `Lacunas` preenchidas.

### Volatilidade

- objetos alterados por semana e mês;
- temas que mais recebem conceitos novos;
- conceitos com alteração recente;
- crescimento de conceitos por tema.

## Operações Anytype Necessárias

Todas as chamadas devem usar o header `Anytype-Version: 2025-11-08`.

Após autenticação, chamadas protegidas devem usar `Authorization: Bearer <api_key>`.

Operações principais:

- `GET /v1/spaces`
- `GET /v1/spaces/{space_id}`
- `GET /v1/spaces/{space_id}/types`
- `GET /v1/spaces/{space_id}/types/{type_id}`
- `GET /v1/spaces/{space_id}/properties`
- `GET /v1/spaces/{space_id}/properties/{property_id}`
- `GET /v1/spaces/{space_id}/properties/{property_id}/tags`
- `GET /v1/spaces/{space_id}/objects`
- `GET /v1/spaces/{space_id}/objects/{object_id}`
- `POST /v1/spaces/{space_id}/search`

Como a API key será cadastrada manualmente, o fluxo `POST /v1/auth/challenges` e `POST /v1/auth/api_keys` não precisa ser implementado no início do MVP.

## Sequência Incremental de Implementação

1. Criar bootstrap Spring Boot local.
2. Configurar SQLite.
3. Implementar configuração local do Anytype.
4. Implementar client REST somente leitura.
5. Validar conexão com Anytype.
6. Localizar o espaço Evolução Técnica.
7. Listar e validar tipos e propriedades esperadas.
8. Consultar objetos dos tipos centrais.
9. Montar o modelo atual em memória.
10. Criar snapshot diário mínimo.
11. Implementar backfill sintético `carry-forward`.
12. Detectar atividade por `last_modified_date`.
13. Calcular métricas iniciais.
14. Expor endpoints internos para o dashboard.
15. Criar dashboard simples.
16. Adicionar sincronização manual.
17. Adicionar sincronização automática.

