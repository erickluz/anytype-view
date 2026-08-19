# Instruções do projeto

Antes de implementar qualquer funcionalidade, consulte a documentação relevante em `/docs`.

## Fontes de verdade

Use as fontes nesta ordem:

1. `/docs/product/mvp.md` para requisitos e escopo do MVP.

2. `/docs/product/anytype-model.md` para a modelagem existente no espaço Evolução Técnica.

3. `/docs/architecture/architecture.md` para decisões arquiteturais.

4. Os documentos específicos em `/docs/architecture` para detalhes de cada componente.

5. `/docs/integration/anytype-openapi.json` como fonte de verdade do contrato REST do Anytype.

6. A documentação oficial do Anytype como complemento explicativo.

## Integração Anytype

Não invente endpoints, parâmetros, propriedades ou comportamento da API.

Use a especificação OpenAPI fornecida no projeto para contratos HTTP.

Quando a especificação não for suficiente, consulte a documentação oficial:

- [https://developers.anytype.io/](https://developers.anytype.io/)

- https://developers.anytype.io/docs/reference/

- https://developers.anytype.io/docs/examples/

## Restrições

- O Anytype é a fonte de verdade dos dados de conhecimento.

- Não alterar nem exigir alterações na modelagem do espaço Evolução Técnica.

- Não criar funcionalidades fora do MVP sem solicitação explícita.

- Priorizar soluções simples.

- Evitar abstrações e infraestrutura sem necessidade concreta.

- O projeto é local e single-user no MVP.
