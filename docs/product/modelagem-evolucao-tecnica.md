#Modelagem Evolucao Tecnica

### Os 4 tipos centrais

**1. Tema** (59 objetos) — a espinha dorsal organizacional.

- Propriedade `Tipo` (select: `tema` / `subtema`) permite uma **hierarquia auto-referenciada**: um Tema "pai" (ex.: "IA", "Cloud Native", "Delivery & Release") se conecta a seus "filhos" via a propriedade `Links` (relação objects). Ex.: "Cloud Native" → linka "Containers" e "Orquestração Containers" (ambos tipo=subtema).
- Também tem `Prioridade` (número) e `Tag`.
- Cobre praticamente toda a trilha de estudo: JAVA (por versão), Spring (com vários subtemas: Security, Data JPA, Async, Testes...), Arquitetura Reativa, Microsserviços, System Design, DDD, Observabilidade, Mensageria/Kafka, IA (Vibe Coding, MCP, RAG, Agentic AI, SDD), Cloud/Containers, Delivery & Release etc.

**2. Conceito** (414 objetos) — a unidade atômica de conhecimento, e de longe a mais numerosa.

- Segue um template fixo no corpo: *O que é / Problema que resolve / Tradeoff principal / Quando usar*.
- Propriedades: `Categoria` (objects → aponta para um ou mais Temas), `Veredito` (select: ex. "Fraco"/"Forte" — um julgamento de qualidade/relevância do conceito), `Entendimento` (select: Desconhecido/Básico/Intermediário...), `Tradeoff` (text), `Checkpoint` (objects → liga o conceito ao(s) Checkpoint(s) em que foi revisado), `Prioridade`.
- Ou seja, cada Conceito pertence a um Tema (via Categoria) e pode ser rastreado ao longo de sessões de revisão (via Checkpoint).

**3. Checkpoint de Conhecimento** (10 objetos) — sessões periódicas de consolidação/avaliação.

- Template fixo: *O que aprendi? / O que realmente sei fazer? / Como os conceitos se conectam? / O que disso é vendável? / Como eu explicaria isso em entrevista? / Lacunas? / Próximos passos?*
- Propriedades: `Tema` (objects → o tema avaliado), `Conecta com` (objects → provavelmente os Conceitos cobertos), `Última Revisão` (date), `Stack relacionada` (multi_select, ex. JAVA), `Lacunas` (text), `Aplicação Prática` (text), `Vendabilidade` (number, 0–10 — o quanto isso "vende" em entrevista), `Nível Percebido` (select: Básico/Intermediário/Avançado) e `Status` (select: Estudando/...).
- É basicamente o mecanismo de **auto-avaliação e "employability check"** por tema — um checkpoint puxa e resume vários Conceitos ligados a ele.

**4. Aplicação** (2 objetos) — projetos práticos que materializam o aprendizado.

- Template: *Objetivo / Conceitos relacionados / Contexto / Decisões técnicas / Trade-offs observados / Como isso é vendável*.
- Propriedades: `Tema` (objects), `Nível Implementação` (select: ex. "Protótipo"), `Status` (select: "Em andamento"), `GitHub` (url).
- Exemplos atuais: "Lab Microsserviços" e "Telemetria" (o projeto pessoal de plataforma de telemetria mencionado antes).

### Como as peças se conectam

```
Tema (pai) ──links──> Tema (subtema)
   ▲                        ▲
   │ categoria              │ tema
   │                        │
Conceito ──checkpoint──> Checkpoint de Conhecimento
   ▲                        │
   └── conecta_com ─────────┘

Aplicação ──tema──> Tema
```

Em resumo: **Tema** é a árvore de domínios de estudo; **Conceito** é o "flashcard estruturado" de cada assunto dentro de um Tema; **Checkpoint** é a auditoria periódica que agrupa Conceitos de um Tema e mede maturidade e "vendabilidade" (o quanto dá pra defender aquilo numa entrevista); e **Aplicação** ancora tudo isso em projetos reais de código.

É um sistema deliberadamente desenhado para apoiar sua meta de consolidação técnica rumo à arquitetura — combina profundidade conceitual (414 conceitos) com mecanismos explícitos de auto-avaliação de empregabilidade (vendabilidade, nível percebido, lacunas).
