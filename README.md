# 💈 SGS-BARBER
### Sistema Inteligente de Gestão e Agendamento para Barbearias

O **SGS-BARBER** é um sistema web desenvolvido para modernizar o gerenciamento de barbearias, permitindo o cadastro de clientes e profissionais, a organização da agenda e o agendamento de serviços.

O projeto integra **Frontend, Backend, Banco de Dados e Inteligência Artificial**, oferecendo uma experiência prática para clientes e barbeiros.

O sistema utiliza **Java com Spring Boot**, **PostgreSQL/Supabase**, **HTML, CSS e JavaScript** e um assistente virtual chamado **SGS AI**, desenvolvido com integração ao **Ollama**.

---

## 🚀 Funcionalidades

- Cadastro e autenticação de clientes e barbeiros.
- Agendamento de serviços com seleção de profissional, data e horário.
- Consulta de disponibilidade e prevenção de conflitos de agenda.
- Dashboard do cliente e do barbeiro.
- Cadastro de serviços, preços e duração.
- Histórico de agendamentos e avaliações.
- Gestão de horários, folgas e bloqueios.
- Atendimento presencial (*walk-in*).
- Módulos de produtos, estoque, CRM e notificações.
- **SGS AI:** assistente conversacional para consultar serviços, profissionais e horários.
- Interface responsiva para computadores e dispositivos móveis.

> Algumas funcionalidades estão implementadas no código, enquanto outras ainda dependem de validação completa dos fluxos e da integração entre as camadas.

---

## 📅 Evolução do projeto — Sprints

### Sprint 1 — Planejamento e estruturação

**Objetivo:** definir o escopo e preparar a arquitetura do sistema.

- Levantamento de requisitos funcionais e não funcionais.
- Elaboração do Documento de Visão e Especificação Funcional.
- Definição das regras de negócio.
- Prototipação das interfaces.
- Planejamento da arquitetura e modelagem inicial do banco de dados.

### Sprint 2 — Desenvolvimento das funcionalidades

**Objetivo:** implementar os principais módulos do SGS-BARBER.

- Desenvolvimento do frontend com HTML, CSS e JavaScript.
- Estruturação do backend com Java e Spring Boot.
- Configuração do PostgreSQL no Supabase.
- Criação das entidades, controllers, services e repositories.
- Implementação das APIs REST.
- Desenvolvimento dos fluxos de cadastro, autenticação e agendamento.
- Implementação inicial da SGS AI.

### Sprint 3 — Integração e MVP funcional

**Objetivo:** integrar os componentes e validar o funcionamento do sistema.

**Requisitos solicitados pelo professor:**
- Conexão end-to-end entre Frontend, Backend e IA.
- Tratamento de erros.
- MVP funcional testado com dados reais.

**Evoluções realizadas:**
- Integração do frontend com endpoints REST do Spring Boot.
- Implementação de serviços de interpretação e memória conversacional.
- Integração da SGS AI com o Ollama.
- Consultas de catálogo e disponibilidade.
- Estrutura de persistência utilizando Supabase e Flyway.
- Preparação de cenários para validação de agendamentos e erros.

**Validações pendentes:** confirmar a execução completa do agendamento via IA, sua persistência no banco e o tratamento de falhas em todos os cenários. A conexão com o Supabase também precisa ser revalidada após o erro de autenticação identificado na inicialização.

---

## 🛠️ Tecnologias utilizadas

| Camada | Tecnologia |
|---|---|
| Frontend | HTML5, CSS3 e JavaScript |
| Interface | Font Awesome e Chart.js |
| Backend | Java 17 e Spring Boot 3 |
| API | REST, Spring Web e Spring Data JPA |
| Banco de dados | PostgreSQL — Supabase |
| Migrações | Flyway |
| Inteligência Artificial | Ollama — Llama 3.2 3B |
| Gerenciamento de dependências | Maven Wrapper |
| Desenvolvimento | VS Code e GitHub |

---

## 🏗️ Arquitetura do sistema

```text
              SGS-BARBER
                  |
          FRONTEND WEB
        HTML / CSS / JS
                  |
               API REST
                  |
          SPRING BOOT
           Backend Java
             /       \
            /         \
       SUPABASE      SGS AI
      PostgreSQL     Ollama
                       |
                  Llama 3.2 3B
```

O frontend realiza requisições ao backend. O Spring Boot aplica as regras de negócio, consulta os dados no Supabase e disponibiliza os serviços utilizados pela SGS AI.

---

## 📂 Estrutura do projeto

```text
SGS-BARBER/
├── frontend/
│   ├── index.html
│   ├── login-cliente.html
│   ├── login-barbeiro.html
│   ├── dashboard-cliente.html
│   ├── dashboard-barbeiro.html
│   ├── app.js
│   ├── api.js
│   └── server.js
│
├── sgsbarber/
│   ├── src/main/java/
│   ├── src/main/resources/
│   │   └── db/migration/
│   ├── pom.xml
│   └── mvnw.cmd
│
└── README.md
```

A localização exata do frontend pode variar conforme a versão do repositório. Utilize a pasta que contém o arquivo `server.js`.

---

# 💻 Como executar no VS Code

## 1. Pré-requisitos

Instale:

- Java JDK 17 ou superior.
- Node.js 18 ou superior.
- Git.
- Visual Studio Code.
- Ollama com o modelo `llama3.2:3b`.
- Conta e projeto configurados no Supabase.

## 2. Clonar o projeto

Abra o terminal do VS Code e execute:

```powershell
git clone https://github.com/erikmenezes07/Sistema-de-agendamento-de-barbearia---SGS-BARBER.git
```

Entre na pasta do repositório:

```powershell
cd Sistema-de-agendamento-de-barbearia---SGS-BARBER
```

## 3. Configurar o banco de dados — Supabase

O Supabase hospeda o PostgreSQL na nuvem. Portanto, não é necessário iniciar um servidor PostgreSQL local.

Acesse o painel do Supabase e obtenha os dados de conexão do projeto.

No terminal PowerShell, configure as variáveis:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://SEU_HOST:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME="SEU_USUARIO"
$env:SUPABASE_DB_PASSWORD=Read-Host "Senha do banco"
```

**Importante:** confirme no arquivo `application.properties` se essas são as variáveis utilizadas pelo projeto. Caso utilize outro nome, como `DB_PASSWORD`, configure o nome correspondente.

As migrações Flyway são executadas automaticamente durante a inicialização do backend.

## 4. Iniciar a Inteligência Artificial — Ollama

Abra um novo terminal:

```powershell
ollama list
```

Se o modelo ainda não estiver instalado:

```powershell
ollama pull llama3.2:3b
```

Inicie o Ollama:

```powershell
ollama serve
```

O serviço normalmente fica disponível em:

```text
http://localhost:11434
```

Se o Ollama já estiver funcionando, não é necessário iniciar outra instância.

## 5. Iniciar o backend — Spring Boot

No terminal em que configurou as variáveis do Supabase, acesse a pasta que contém o `pom.xml`:

```powershell
cd sgsbarber
```

Execute:

```powershell
.\mvnw.cmd spring-boot:run
```

O backend deverá iniciar em:

```text
http://localhost:8080
```

Para verificar a API:

```text
http://localhost:8080/api/ia/catalogo
```

Uma resposta JSON com os serviços cadastrados indica que a consulta está funcionando.

## 6. Iniciar o frontend

Abra outro terminal e acesse a pasta que contém o `server.js` do frontend.

Se estiver dentro da pasta `sgsbarber/frontend`, a partir da raiz do repositório:

```powershell
cd sgsbarber\frontend
node server.js
```

Se estiver na pasta `frontend` da raiz:

```powershell
cd frontend
node server.js
```

Abra no navegador:

```text
http://localhost:5500
```

Confira se a configuração do frontend aponta para o backend na porta `8080`.

## 7. Testar a SGS AI

Com os serviços em execução, abra o chat do sistema e experimente:

```text
Olá!
Quais serviços vocês oferecem?
Quero agendar um corte de cabelo.
Quais horários estão disponíveis amanhã?
```

A SGS AI utiliza o backend para consultar informações do sistema. O fluxo completo de confirmação e gravação da reserva deve ser validado antes de ser considerado concluído.

---

## 🧪 Testes do MVP — Sprint 3

| Teste | Resultado esperado |
|---|---|
| Login | Autenticar o usuário corretamente |
| Catálogo | Exibir serviços cadastrados no Supabase |
| Disponibilidade | Consultar horários livres |
| Agendamento | Registrar reserva no banco |
| Conflito | Impedir horários sobrepostos |
| SGS AI | Interpretar solicitações e consultar dados |
| Erro de conexão | Apresentar falha sem interromper a interface |

### Testes automatizados

Para executar os testes do backend:

```powershell
cd sgsbarber
.\mvnw.cmd test
```

Os testes configurados com H2 em memória não exigem conexão com o Supabase, mas não substituem os testes end-to-end com PostgreSQL real.

---

## 👨‍💻 Equipe do projeto

| Integrante | Responsabilidade |
|---|---|
| Erik Menezes | Gerenciamento do projeto |
| Wesley Gonçalves | Requisitos e backend |
| Lucas | Backend e inteligência artificial |
| Thales Tadeu | Banco de dados |
| Daniel Duarte | Frontend e UX |
| André Cauê | Qualidade e testes |

---

## 📌 Status atual

O SGS-BARBER possui estrutura de frontend, backend REST, persistência PostgreSQL e módulo de inteligência artificial implementados no projeto.

A evolução da Sprint 3 está concentrada na **validação end-to-end**, garantindo que as operações iniciadas na interface sejam processadas corretamente pelo backend, consultem ou persistam dados reais e retornem respostas adequadas ao usuário.

**Projeto acadêmico desenvolvido na disciplina de Fábrica de Software — Análise e Desenvolvimento de Sistemas.**
