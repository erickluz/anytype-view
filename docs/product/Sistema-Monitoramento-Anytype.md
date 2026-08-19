# Sistema Monitoramento Anytype

## Brainstorm de requisitos abstratos

- Visibilidade de:
  
  - progresso
  
  - de conclusao por
    
    - periodo
    
    - tema
    
    - quantidade
    
    - velocidade

- dashboard com graficos

## Problemas que quero resolver

O anytype resolveu varios problemas que eu tinha para estudar na area de ti, mas o principal foi tirar todo o mapa mental do que preciso estudar da minha cabeca, sistematizar de uma forma inteligente e sofisticada, persistindo o estudo no sentido de que quando eu preciso suspender os estudos para lidar com outras questoes da vida, quando eu volto sei da onde parei. Mas ele nao resolve o problema de visibilidade do meu progresso de estudo. Enquanto eu estou em esforco de estudo, eu consigo instanciar na minha mente o progresso que ja dei a longo prazo e a curto prazo, so que quando preciso parar e voltar, preciso refazer esse levantamento mental do meu progresso denovo. O Anytype pode ate ter recurso para isso, mas e muito limitado, ele nao trabalha com scripts para processar dados, cruzar dados, soma, tirar medias, estatisticas.

Outro problema, conforme vou estudando, conforme minha vida profissional anda, progride, avanca, minha conciencia sobre a area que atuo aumenta, meu sistema incha, novos conceitos aumentam, conceitos perdem relevancia, outros ganham. Por conta dessa volatilidade natural do sistema, ao longo do tempo, eu perco a visibilidade de como esta, como de quantidade de conceitos, quantidade de temas, dominio sobre temas, conceitos, nocao de como eu estou em relacao oque preciso fazer. Ou seja, preciso ter visibilidade dessa volatilidade.

## Abordagem inicial.

### Requisitos

Inicialmente estou levantando requisitos, estou tentando dimensionar quais problemas quero resolver e quais fazem sentido nesse momento, para nao inchar o ponta pe inicial. 

Tenho a ideia de fazer um bootstrap inicial com poucos requisitos, com os requisitos mais impactantes, que irao trazer resultados imediatos, que nao irao comprometer o sucesso do projeto consumindo muito da minha energia e tempo.

### Viabilidade

Estou avaliando se realmente o esforco de fazer esse projeto me dara algum retorno. Ja tenho certeza de que o sistema que fiz dentro do anytype vai me trazer resultados concretos porque ja trouxe, ja colhi frutos financeiros por causa dele.

Estou avaliando se realmente ter uma observabilidade do meu desempenho, do meu progresso, do meu ritmo, em relacao ao oque preciso enfrentar, realmente ira me trazer resultados concretos.

Ja acho que sim, porque com essa visibilidade, irei tomar decisoes com informacoes mais concretas, decisoes como mudanca de direcao, mudanca de prioridade em relacao a temas e conceitos, e ate mesmo dos meus estudo em relacao a minha vida pessoal, se preciso acelerar o passo ou diminuir. Ter nocao de quando poderei alcancar certos objetivos, objetivos que acredito que sao desbloquedores na minha carreira, como por exemplo: "Estudei esse conjunto de conceitos, agora estou apto para aplicar para vagas que pedem isso do profissional". Hoje me sinto perdido em relacao a isso.

Preciso avaliar a viabilidade de tecnologias, nao sei se estou levando em conta tudo, mas tenho uma arquitetura em mente e quais teconologias usar.

Hoje nao considero eu mesmo codificar um sistema desse na mao, somente seria viavel com desenvolvimento de IA, da qual ja tenho experiencia e sei que e totalmente plausivel essa execucao.

### Arquitetura e Tecnologias

A Arquitetura que tenho em mente seria, uma aplicacao spring boot monolito, com um frontend embarcado no monolito. Essa aplicacao rodando localhost requisitando informacoes online no anytype que tambem roda localhost, realizando calculos a nivel de memoria, nao persistindo calculos de graficos,e persistindo em um banco de dados talvez embarcado somente informacoes que nao nao faz sentido de serem persistidas no anytype, como progresso, data de conclusao de uma demanda, coisas assim.

Gostaria que tudo tivesse encapsulado em um artefato so, como por exemplo um .EXE, mas nao sei a viabilidade disso inicialmente, talvez valha a pena rodar em navegador e depois realizar um esforco de encapsulamento.

## Objetivo do sistema

## Princípios do MVP

- Anytype como source of truth
- Não alterar a modelagem Evolução Técnica
- Sistema somente leitura em relação ao Anytype
- Baixo esforço de manutenção
- Não registrar tempo dedicado aos estudos

## Perguntas que o sistema deve responder

- Qual meu ritmo de evolução?
- Em quais dias/semanas houve avanço?
- Quantas interrupções ocorreram?
- Quanto duraram?
- Qual meu progresso atual?
- Qual o tamanho atual do sistema?
- Como esse tamanho está evoluindo?

## Definição de atividade

A atividade é inferida a partir das alterações dos objetos existentes
no Anytype, principalmente Conceitos e Checkpoints.

Não existe apontamento manual de horas ou tempo de estudo.

## Escopo funcional do MVP

1. Integração REST com Anytype
2. Persistência histórica
3. Detecção de alterações
4. Métricas
5. Dashboard
6. Sincronização automática/manual

## Métricas iniciais

...

## Fora do escopo do MVP

- IA
- recomendação de estudos
- previsão
- score próprio de conhecimento
- alteração automática do Anytype
- multiusuário
- aplicação desktop
  ...
