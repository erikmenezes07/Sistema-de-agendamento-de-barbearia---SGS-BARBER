# Documentação das Tabelas - SGS-BARBER

## Visão Geral

Modelo de dados relacional (PostgreSQL) que suporta:
- Cadastro de clientes, barbeiros, serviços e horários de expediente
- Agendamentos com validação de disponibilidade, pausa de almoço e combos
- Arquitetura de Agentes de IA (Agendador + Recomendador) com histórico completo de conversas e tool calls

## 1. cliente

| Coluna | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| id | BIGSERIAL (PK) | Sim | Identificador único |
| nome | VARCHAR(120) | Sim | Nome completo do cliente |
| email | VARCHAR(120) | Sim | E-mail (único) |
| telefone | VARCHAR(20) | Sim | Telefone/WhatsApp |

**Constraints**: `UNIQUE(email)`, validações de não-vazio.

## 2. barbeiro

| Coluna | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| id | BIGSERIAL (PK) | Sim | Identificador único |
| nome | VARCHAR(120) | Sim | Nome do barbeiro/profissional |
| telefone | VARCHAR(20) | Sim | Contato |
| especialidades | VARCHAR(500) | Não | Especialidades separadas por vírgula (ex.: `Degrade,Barba,Tesoura`) |
| foto_url | VARCHAR(500) | Não | URL da foto (avatar) |

**Notas**: A camada JPA mapeia `especialidades` (string separada por vírgula) para `List<String>` via `@Convert`/getter/setter customizado.

## 3. servico

| Coluna | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| id | BIGSERIAL (PK) | Sim | Identificador único |
| nome | VARCHAR(120) | Sim | Nome do serviço |
| descricao | VARCHAR(500) | Não | Descrição detalhada |
| preco | DECIMAL(10,2) | Sim | Valor (>= 0) |
| duracao_minutos | INTEGER | Sim | Duração total em minutos (> 0) |
| combo | BOOLEAN | Sim | `true` se for combo multi-serviço (ex.: Corte + Barba). Padrão: `FALSE` |

**Notas**: Para combos, o `Agendamento` reserva o tempo total (`duracao_minutos`) e registra a composição na `observacao`.

## 4. horario_barbeiro

Define jornada e pausa de almoço por dia da semana.

| Coluna | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| id | BIGSERIAL (PK) | Sim | Identificador único |
| barbeiro_id | BIGINT (FK) | Sim | Referência a `barbeiro.id` (ON DELETE CASCADE) |
| dia_semana | INTEGER | Sim | 1=Segunda-feira, 2=Terça-feira, 3=Quarta-feira, 4=Quinta-feira, 5=Sexta-feira, 6=Sábado, 7=Domingo |
| hora_inicio | TIME | Sim | Início do expediente |
| hora_fim | TIME | Sim | Fim do expediente |
| almoco_inicio | TIME | Não | Início da pausa de almoço/descanso |
| almoco_fim | TIME | Não | Fim da pausa de almoço/descanso |

**Constraints**: `hora_fim > hora_inicio`. Se ambos `almoco_*` forem informados, deve haver `almoco_fim > almoco_inicio` e o intervalo deve estar dentro do expediente.

**Índice**: `(barbeiro_id, dia_semana)` para otimizar busca de turnos na geração de slots.

## 5. agendamento

| Coluna | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| id | BIGSERIAL (PK) | Sim | Identificador único |
| cliente_id | BIGINT (FK) | Sim | `cliente.id` (ON DELETE RESTRICT) |
| barbeiro_id | BIGINT (FK) | Sim | `barbeiro.id` (ON DELETE RESTRICT) |
| servico_id | BIGINT (FK) | Sim | `servico.id` (ON DELETE RESTRICT) |
| data_agendamento | DATE | Sim | Data da reserva |
| hora_inicio | TIME | Sim | Horário de início |
| hora_fim | TIME | Sim | Horário de término (calculado = início + duração total) |
| status | VARCHAR(20) | Sim | `AGENDADO` ou `CANCELADO` (padrão: `AGENDADO`) |
| observacao | VARCHAR(500) | Não | Observações. Para **combos**, contém a lista de serviços (`Serviços: Corte Degrade (45min) + Barba Completa (30min) | obs...`) |

**Regras de negócio**:
- Não permite sobreposição de agendamentos para o mesmo barbeiro/data/horário (validação + lock pessimista).
- Respeita expediente e pausa de almoço (`almoco_inicio`–`almoco_fim`).
- Combos: reserva única faixa (`hora_inicio` → `hora_fim` = total), serviços adicionais registrados em `observacao`.

**Índices**: `(barbeiro_id, data_agendamento)`, `(cliente_id)`, `(status)`.

## 6. conversa

Representa uma sessão de conversa entre o usuário/cliente e o orquestrador de IA.

| Coluna | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| id | BIGSERIAL (PK) | Sim | Identificador único da sessão |
| usuario_id | VARCHAR(100) | Sim | Identificador anônimo/único do usuário no front-end (ex.: UUID ou string) |
| cliente_id | BIGINT (FK) | Não | Vincula conversa a um `cliente.id` (ON DELETE SET NULL) |
| agente_atual | VARCHAR(30) | Sim | Agente ativo no turno atual: `AGENDADOR` ou `RECOMENDADOR` (padrão: `AGENDADOR`) |
| criado_em | TIMESTAMP WITH TIME ZONE | Sim | Data/hora de criação (NOW()) |
| atualizado_em | TIMESTAMP WITH TIME ZONE | Sim | Data/hora da última atualização (NOW()) |

**Uso**: Permite reiniciar histórico (`/api/chat/reiniciar/{usuarioId}`), recuperar histórico (`/api/chat/historico/{usuarioId}`) e manter coerência de contexto entre mensagens (handoff preservado).

**Índice**: `(usuario_id)`.

## 7. mensagem_conversa

Armazena histórico completo da conversa no formato compatível com OpenAI Function Calling (`system/user/assistant/tool` + tool calls/results).

| Coluna | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| id | BIGSERIAL (PK) | Sim | Identificador único |
| conversa_id | BIGINT (FK) | Sim | Referência a `conversa.id` (ON DELETE CASCADE) |
| papel | VARCHAR(20) | Sim | Papel da mensagem: `SYSTEM`, `USER`, `ASSISTANT` ou `TOOL` |
| agente | VARCHAR(30) | Não | Agente responsável naquele turno: `AGENDADOR`, `RECOMENDADOR` ou `NULL` (para mensagens do sistema/usuário) |
| conteudo | TEXT | Não | Conteúdo da mensagem (texto da resposta, ou `NULL` quando for tool call/tool result puro) |
| ferramenta_nome | VARCHAR(80) | Não | Nome da tool chamada (`listar_servicos`, `verificar_disponibilidade`, `criar_agendamento`, `encaminhar_para_agente`, etc.) |
| ferramenta_argumentos | TEXT | Não | Argumentos da chamada em JSON (serializados) |
| ferramenta_resultado | TEXT | Não | Resultado da execução da tool em JSON (serializados) |
| criado_em | TIMESTAMP WITH TIME ZONE | Sim | Ordem cronológica (NOW()) |

**Mapeamento p/ Function Calling**:
- `papel='ASSISTANT'` + `ferramenta_nome` preenchido (e sem `conteudo` completo) representa `tool_calls[]`
- `papel='TOOL'` representa `tool` result (com `ferramenta_nome` + `ferramenta_resultado`)
- `papel='ASSISTANT'` com `conteudo` representa resposta final em texto
- `papel='USER'` mensagem do cliente; `papel='SYSTEM'` prompt do agente

**Índice**: `(conversa_id, criado_em)` para reconstrução eficiente do histórico por ordem temporal.

## Relacionamentos

```text
cliente (1) ───< agendamento (N)
barbeiro (1) ──< agendamento (N)
servico (1) ───< agendamento (N)
barbeiro (1) ──< horario_barbeiro (N)
cliente (1) ───< conversa (N)  [0..1 opcional]
conversa (1) ──< mensagem_conversa (N)
```

## Decisões de Modelagem

1. **`especialidades` como VARCHAR separada por vírgula** (não tabela N:N). Decisão pragmática para simplicidade, mapeada em Java como `List<String>`. Facilita busca por `LIKE`/contains e evita joins extras no fluxo de IA.
2. **`combo` em `servico`**: identifica natureza do serviço. Agendamentos de combo usam 1 linha, com duração total em `hora_fim` e composição em `observacao`.
3. **Almoço dividido (`almoco_inicio`/`almoco_fim`)**: permite modelar pausa com precisão (ex.: 12:00–13:00). `semAlmoco()` é derivado (ambos nulos).
4. **`mensagem_conversa` unificado**: armazena tanto histórico conversacional quanto tool calls/tool results, viabilizando `ConversaService.historicoParaModelo()` reconstruir exatamente o formato exigido pelo LLM (`/chat/completions`).
5. **Soft-delete não aplicado**: `status` em `agendamento` cobre cancelamento. Demais entidades usam exclusão física (com FKs apropriadas).
6. **TZ em timestamps**: `TIMESTAMP WITH TIME ZONE` em `conversa`/`mensagem_conversa` para rastreabilidade correta em diferentes ambientes.

## Scripts

- [`Database/Scripts/DDL.sql`](../Scripts/DDL.sql): criação completa do schema (com comentários, índices, constraints e dados de exemplo comentados).
- [`Database/Diagrama/mermaid.md`](../Diagrama/mermaid.md): diagrama ER em Mermaid.
