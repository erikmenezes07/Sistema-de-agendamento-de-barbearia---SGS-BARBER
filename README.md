# DER - SGS-BARBER

```mermaid
erDiagram

    USUARIOS {
        bigint id_usuario PK
        varchar nome
        varchar email UK
        varchar senha
        varchar perfil
        boolean ativo
    }

    CLIENTES {
        bigint id_cliente PK
        bigint id_usuario FK
        varchar telefone
        timestamp data_cadastro
    }

    BARBEIROS {
        bigint id_barbeiro PK
        bigint id_usuario FK
        varchar telefone
        boolean ativo
    }

    SERVICOS {
        bigint id_servico PK
        varchar nome
        varchar descricao
        decimal preco
        integer duracao_minutos
        boolean ativo
    }

    HORARIOS_DISPONIVEIS {
        bigint id_horario PK
        bigint id_barbeiro FK
        integer dia_semana
        time hora_inicio
        time hora_fim
    }

    AGENDAMENTOS {
        bigint id_agendamento PK
        bigint id_cliente FK
        bigint id_barbeiro FK
        bigint id_servico FK
        date data_agendamento
        time hora_inicio
        time hora_fim
        varchar status
        varchar observacao
        timestamp data_criacao
    }

    USUARIOS ||--o| CLIENTES : possui
    USUARIOS ||--o| BARBEIROS : possui

    BARBEIROS ||--o{ HORARIOS_DISPONIVEIS : define

    CLIENTES ||--o{ AGENDAMENTOS : realiza
    BARBEIROS ||--o{ AGENDAMENTOS : atende
    SERVICOS ||--o{ AGENDAMENTOS : possui
