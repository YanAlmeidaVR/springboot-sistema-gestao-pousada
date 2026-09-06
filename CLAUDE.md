# Aconchega — contexto do projeto

Este trabalho é dividido em dois repositórios separados:

- **aconchega-api** (aqui) — backend Spring Boot 4, Java 17, PostgreSQL, Flyway.
- **aconchega** (`../aconchega`) — frontend Next.js 16, React 19, shadcn/ui.

## Rotas da API

Todos os controllers expõem rotas sob o prefixo `/aconchega`:

- `/aconchega/hospedes` — `HospedeController`
- `/aconchega/quartos` — `QuartoController`
- `/aconchega/reservas` — `ReservaController`

No frontend, `NEXT_PUBLIC_API_URL` (`../aconchega/.env.local`) já inclui esse prefixo
(`http://localhost:8080/aconchega`). Não repita `/aconchega` ao montar caminhos em
`../aconchega/lib/api.ts` — apenas concatene o recurso, ex.: `` `${API_URL}/quartos` ``.

## Enums — fonte da verdade

Os enums abaixo (pacote `enums.quarto`) são a fonte da verdade para os respectivos
domínios. Qualquer mapeamento no frontend (`lib/api.ts`) deve refletir exatamente estes
valores, sem inventar variantes.

**`TipoQuarto`**
- `SOLTEIRO`
- `CASAL`
- `TRIPLA`

**`QuartoStatus`**
- `DISPONIVEL`
- `OCUPADO`
- `MANUTENÇÃO`

O tipo `Quarto` em `../aconchega/lib/api.ts` usa esses mesmos literais diretamente
(sem camada de tradução) — ao alterar o domínio de quartos, mude o enum Java e depois
o tipo do frontend, nessa ordem.

## Convenções de código

- Sem comentários explicando o que o código faz.
- Chave de abertura sem espaço antes do `(`: `){` (aplica-se a métodos e estruturas de
  controle), ex.:

  ```java
  public ResponseEntity<QuartoResponseDTO> criar(@RequestBody QuartoCreateDTO quarto){
      ...
  }
  ```
