-- SG B-BARBER: SCRIPT DE CRIAÇÃO DE BANCO DE DADOS (POSTGRESQL)
-- Arquitetura: Spring Boot 4.0.8 + JPA/Hibernate + Agente IA com Function Calling
-- Compatível com as entidades implementadas no projeto.

SET search_path TO public;

-- Extensões úteis
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- CLIENTE
CREATE TABLE IF NOT EXISTS cliente (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    CONSTRAINT ck_cliente_nome CHECK (TRIM(nome) <> ''),
    CONSTRAINT ck_cliente_email CHECK (email LIKE '%@%.%'),
    CONSTRAINT ck_cliente_telefone CHECK (TRIM(telefone) <> '')
);

-- BARBEIRO
CREATE TABLE IF NOT EXISTS barbeiro (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    especialidades VARCHAR(500),
    foto_url VARCHAR(500),
    CONSTRAINT ck_barbeiro_nome CHECK (TRIM(nome) <> ''),
    CONSTRAINT ck_barbeiro_telefone CHECK (TRIM(telefone) <> '')
);

-- SERVICO
CREATE TABLE IF NOT EXISTS servico (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    preco DECIMAL(10,2) NOT NULL,
    duracao_minutos INTEGER NOT NULL,
    combo BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_servico_nome CHECK (TRIM(nome) <> ''),
    CONSTRAINT ck_servico_preco CHECK (preco >= 0),
    CONSTRAINT ck_servico_duracao CHECK (duracao_minutos > 0)
);

-- HORARIO DE TRABALHO DO BARBEIRO (por dia da semana)
-- dia_semana: 1=Segunda-feira, 2=Terça-feira, 3=Quarta-feira, 4=Quinta-feira, 5=Sexta-feira, 6=Sábado, 7=Domingo
CREATE TABLE IF NOT EXISTS horario_barbeiro (
    id BIGSERIAL PRIMARY KEY,
    barbeiro_id BIGINT NOT NULL,
    dia_semana INTEGER NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    almoco_inicio TIME,
    almoco_fim TIME,
    CONSTRAINT fk_horario_barbeiro_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro(id) ON DELETE CASCADE,
    CONSTRAINT ck_horario_barbeiro_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_barbeiro_intervalo CHECK (hora_fim > hora_inicio),
    CONSTRAINT ck_horario_barbeiro_almoco CHECK (
        (almoco_inicio IS NULL AND almoco_fim IS NULL) OR
        (almoco_inicio IS NOT NULL AND almoco_fim IS NOT NULL AND almoco_fim > almoco_inicio AND
         almoco_inicio >= hora_inicio AND almoco_fim <= hora_fim)
    )
);
CREATE INDEX IF NOT EXISTS idx_horario_barbeiro_barbeiro_dia ON horario_barbeiro (barbeiro_id, dia_semana);

-- AGENDAMENTO
CREATE TABLE IF NOT EXISTS agendamento (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    barbeiro_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    data_agendamento DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',
    observacao VARCHAR(500),
    CONSTRAINT fk_agendamento_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE RESTRICT,
    CONSTRAINT fk_agendamento_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro(id) ON DELETE RESTRICT,
    CONSTRAINT fk_agendamento_servico FOREIGN KEY (servico_id) REFERENCES servico(id) ON DELETE RESTRICT,
    CONSTRAINT ck_agendamento_status CHECK (status IN ('AGENDADO','CANCELADO')),
    CONSTRAINT ck_agendamento_intervalo CHECK (hora_fim > hora_inicio),
    CONSTRAINT ck_agendamento_data CHECK (data_agendamento >= CURRENT_DATE - INTERVAL '1 DAY') -- flexibilidade para testes locais
);
CREATE INDEX IF NOT EXISTS idx_agendamento_barbeiro_data ON agendamento (barbeiro_id, data_agendamento);
CREATE INDEX IF NOT EXISTS idx_agendamento_cliente ON agendamento (cliente_id);
CREATE INDEX IF NOT EXISTS idx_agendamento_status ON agendamento (status);

-- CONVERSA (Sessão do agente de IA por usuário)
CREATE TABLE IF NOT EXISTS conversa (
    id BIGSERIAL PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL,
    cliente_id BIGINT,
    agente_atual VARCHAR(30) NOT NULL DEFAULT 'AGENDADOR',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conversa_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE SET NULL,
    CONSTRAINT ck_conversa_agente CHECK (agente_atual IN ('AGENDADOR','RECOMENDADOR')),
    CONSTRAINT ck_conversa_usuario CHECK (TRIM(usuario_id) <> '')
);
CREATE INDEX IF NOT EXISTS idx_conversa_usuario ON conversa (usuario_id);

-- MENSAGEM DE CONVERSA (Histórico completo + tool calls)
CREATE TABLE IF NOT EXISTS mensagem_conversa (
    id BIGSERIAL PRIMARY KEY,
    conversa_id BIGINT NOT NULL,
    papel VARCHAR(20) NOT NULL,
    agente VARCHAR(30),
    conteudo TEXT,
    ferramenta_nome VARCHAR(80),
    ferramenta_argumentos TEXT,
    ferramenta_resultado TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mensagem_conversa_conversa FOREIGN KEY (conversa_id) REFERENCES conversa(id) ON DELETE CASCADE,
    CONSTRAINT ck_mensagem_conversa_papel CHECK (papel IN ('SYSTEM','USER','ASSISTANT','TOOL')),
    CONSTRAINT ck_mensagem_conversa_agente CHECK (agente IS NULL OR agente IN ('AGENDADOR','RECOMENDADOR'))
);
CREATE INDEX IF NOT EXISTS idx_mensagem_conversa_conversa ON mensagem_conversa (conversa_id, criado_em);

-- DADOS INICIAIS (opcionais, para demonstração)
-- Descomente se quiser popular automaticamente
-- INSERT INTO servico (nome, descricao, preco, duracao_minutos, combo) VALUES
--   ('Corte Degrade', 'Corte masculino com degrade nas laterais', 40.00, 45, FALSE),
--   ('Barba Completa', 'Barba com acabamento e navalha', 30.00, 30, FALSE),
--   ('Corte + Barba', 'Combo dos dois, sai mais barato', 60.00, 75, TRUE)
-- ON CONFLICT DO NOTHING;
-- INSERT INTO barbeiro (nome, telefone, especialidades, foto_url) VALUES
--   ('Daniel', '(81) 99999-1111', 'Degrade,Barba', 'https://ui-avatars.com/api/?name=Daniel'),
--   ('Marcos', '(81) 97777-2222', 'Barba,Tesoura', 'https://ui-avatars.com/api/?name=Marcos')
-- ON CONFLICT DO NOTHING;
-- INSERT INTO cliente (nome, email, telefone) VALUES
--   ('Wesley Goncalves', 'wesley@teste.com', '(81) 98888-7777')
-- ON CONFLICT DO NOTHING;
-- INSERT INTO horario_barbeiro (barbeiro_id, dia_semana, hora_inicio, hora_fim, almoco_inicio, almoco_fim)
-- SELECT 1, g.dia, '09:00', '18:00', '12:00', '13:00' FROM generate_series(1,5) g(dia)
-- UNION ALL
-- SELECT 2, g.dia, '10:00', '19:00', '13:00', '14:00' FROM generate_series(1,5) g(dia)
-- ON CONFLICT DO NOTHING;

COMMENT ON TABLE conversa IS 'Sessões de conversa entre cliente/usuário e o orquestrador de IA (Agentes Agendador/Recomendador)';
COMMENT ON TABLE mensagem_conversa IS 'Armazena histórico, tool calls, argumentos e resultados para reconstrução do contexto no formato OpenAI Function Calling';
COMMENT ON COLUMN horario_barbeiro.dia_semana IS '1=Segunda, 2=Terça, 3=Quarta, 4=Quinta, 5=Sexta, 6=Sábado, 7=Domingo';
COMMENT ON COLUMN barbeiro.especialidades IS 'Separadas por vírgula (ex.: Degrade,Barba)';
COMMENT ON COLUMN servico.combo IS 'Indica se é um combo multi-serviço';
COMMENT ON COLUMN agendamento.observacao IS 'Para combos armazena a composição dos serviços';
