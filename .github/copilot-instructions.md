# Instruções do Projeto

## Requisitos Funcionais e Regras de Negócio

Os requisitos funcionais e regras de negócio são documentados exclusivamente em:

`docs/requisitos.md`

Regras:
- Antes de implementar ou modificar funcionalidades, inspecione os requisitos relevantes neste arquivo.
- Trate o documento como a única fonte de verdade para o comportamento da aplicação.
- Não considere outros arquivos Markdown no repositório como requisitos funcionais.
- Não invente novas regras de negócio, casos de uso, validações ou comportamentos esperados.]
- Se houver requisitos ausentes, ambíguos ou contraditórios, solicite esclarecimentos antes de fazer suposições.
- Ao implementar uma funcionalidade, identifique o requisito ou caso de uso relevante pelo título do cabeçalho.
- Se a implementação conflitar com os requisitos documentados, priorize os requisitos e reporte o conflito.

## Princípios Gerais de Implementação

- Siga os princípios SOLID e dê preferência a soluções simples e fáceis de manter.
- Prefira composição em vez de herança quando apropriado.
- Evite abstrações desnecessárias e otimização precoce.
- Siga a arquitetura e as convenções de nomenclatura existentes no projeto.
- Mantenha a lógica de negócio independente de preocupações de infraestrutura.
- Trate erros de forma explícita; nunca silencie exceções.
- Nunca introduza dependências sem uma justificativa clara.
- Escreva testes para novas funcionalidades e testes de regressão para correção de bugs.
- Nunca altere requisitos funcionais sem autorização explícita.
- Não considere códigos gerados ou o comportamento da implementação existente como prova do comportamento de negócio pretendido.
