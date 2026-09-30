# Diagrama Entidade-Relacionamento - SGS-BARBER

```mermaid
erDiagram
    CLIENTE {
        BIGSERIAL id PK
        VARCHAR nome
        VARCHAR email UK
        VARCHAR telefone
    }

    BARBEIRO {
        BIGSERIAL id PK
        VARCHAR nome
        VARCHAR telefone
        VARCHAR especialidades "Separadas por vírgula"
        VARCHAR foto_url
    }

    SERVICO {
        BIGSERIAL id PK
        VARCHAR nome
        VARCHAR descricao
        DECIMAL preco
        INTEGER duracao_minutos
        BOOLEAN combo
    }

    HORARIO_BARBEIRO {
        BIGSERIAL id PK
        BIGINT barbeiro_id FK
        INTEGER dia_semana "1=Seg..7=Dom"
        TIME hora_inicio
        TIME hora_fim
        TIME almoco_inicio "Opcional"
        TIME almoco_fim "Opcional"
    }

    AGENDAMENTO {
        BIGSERIAL id PK
        BIGINT cliente_id FK
        BIGINT barbeiro_id FK
        BIGINT servico_id FK
        DATE data_agendamento
        TIME hora_inicio
        TIME hora_fim
        VARCHAR status "AGENDADO|CANCELADO"
        VARCHAR observacao "Composição de combos"
    }

    CONVERSA {
        BIGSERIAL id PK
        VARCHAR usuario_id
        BIGINT cliente_id FK
        VARCHAR agente_atual "AGENDADOR|RECOMENDADOR"
        TIMESTAMP criado_em
        TIMESTAMP atualizado_em
    }

    MENSAGEM_CONVERSA {
        BIGSERIAL id PK
        BIGINT conversa_id FK
        VARCHAR papel "SYSTEM|USER|ASSISTANT|TOOL"
        VARCHAR agente "AGENDADOR|RECOMENDADOR|null"
        TEXT conteudo
        VARCHAR ferramenta_nome
        TEXT ferramenta_argumentos "JSON"
        TEXT ferramenta_resultado "JSON"
        TIMESTAMP criado_em
    }

    CLIENTE ||--o{ AGENDAMENTO : "realiza"
    BARBEIRO ||--o{ AGENDAMENTO : "atende"
    SERVICO ||--o{ AGENDAMENTO : "refere-se"
    BARBEIRO ||--o{ HORARIO_BARBEIRO : "possui"
    CLIENTE ||--o{ CONVERSA : "participa"
    CONVERSA ||--o{ MENSAGEM_CONVERSA : "contém"
```

## Notas

- **Separação de agentes**: `CONVERSA.agente_atual` rastreia se a conversa está com Agendador ou Recomendador (handoff via `encaminhar_para_agente`).
- **Function Calling**: `MENSAGEM_CONVERSA` armazena `ferramenta_nome`, `ferramenta_argumentos` (JSON) e `ferramenta_resultado` (JSON), permitindo reconstruir o histórico no formato `/chat/completions` (role=tool/tool_call).
- **Combos**: `SERVICO.combo = true` indica combo multi-serviço. Em `AGENDAMENTO`, a duração total fica em `hora_inicio/hora_fim` e a composição é registrada em `observacao`.
- **Almoço**: `HORARIO_BARBEIRO` modela pausa com `almoco_inicio`/`almoco_fim` (opcionais). O serviço impede reserva durante esse intervalo.
- **Concorrência**: criação de agendamento usa lock pessimista (`BarbeiroRepository.findByIdComLock`) para evitar sobreposição entre requisições simultâneas.
- **Índices**: otimizam consulta de disponibilidade (`agendamento.barbeiro_id+data_agendamento`) e histórico de conversa (`mensagem_conversa.conversa_id+criado_em`).
