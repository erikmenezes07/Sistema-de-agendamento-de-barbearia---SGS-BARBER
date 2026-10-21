# SGS Barber — Sistema de Agendamento de Barbearia

Sistema web para gestão de barbearia e agendamento de serviços. O projeto é dividido em um frontend estático, desenvolvido com HTML, CSS e JavaScript, e um backend REST em Spring Boot integrado ao PostgreSQL do Supabase.

## Funcionalidades

- Página pública da barbearia.
- Login para clientes e profissionais.
- Agendamento de serviços com escolha de barbeiro, data e horário.
- Perfil, favoritos, histórico e avaliações de barbeiros.
- Dashboard operacional para o barbeiro.
- Agenda, lista de espera, clientes e CRM.
- Produtos, estoque e fechamento de atendimento.
- Notificações, relatórios e configurações.
- Layout responsivo para computador, tablet e celular.
- Persistência em PostgreSQL com migrations gerenciadas pelo Flyway.

## Tecnologias

### Frontend

- HTML5, CSS3 e JavaScript puro.
- Font Awesome para ícones.
- Chart.js para gráficos.
- Servidor estático simples em Node.js.

### Backend

- Java 17 ou superior.
- Spring Boot.
- Spring Web, Spring Data JPA e Spring Validation.
- PostgreSQL via Supabase.
- Flyway para controle de migrations.
- Maven Wrapper para execução sem instalação global do Maven.

## Estrutura do projeto

```text
sgsbarbearia/
├── frontend/
│   ├── index.html
│   ├── login-cliente.html
│   ├── login-barbeiro.html
│   ├── dashboard-cliente.html
│   ├── dashboard-barbeiro.html
│   ├── style.css
│   ├── app.js
│   ├── api.js
│   └── server.js
├── sgsbarber/
│   ├── src/main/java/       # Controllers, services, repositories e models
│   ├── src/main/resources/  # Configurações e migrations Flyway
│   ├── pom.xml
│   └── mvnw.cmd
└── .vscode/                 # Configuração opcional do Run and Debug
```

## Requisitos

- Git.
- JDK 17 ou superior.
- Node.js 18 ou superior.
- Uma conta/projeto Supabase com banco PostgreSQL.
- Visual Studio Code é opcional.

## Clonar o repositório

```powershell
git clone https://github.com/erikmenezes07/Sistema-de-agendamento-de-barbearia---SGS-BARBER.git
cd Sistema-de-agendamento-de-barbearia---SGS-BARBER
```

## Configurar o banco de dados

As credenciais do Supabase não devem ser publicadas no GitHub. O projeto lê a senha pela variável `SUPABASE_DB_PASSWORD`.

No PowerShell:

```powershell
$env:SUPABASE_DB_PASSWORD="SUA_SENHA_DO_SUPABASE"
```

Também é possível configurar estas variáveis quando necessário:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://SEU_HOST:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME="SEU_USUARIO"
$env:SUPABASE_DB_PASSWORD="SUA_SENHA"
```

As migrations ficam em `sgsbarber/src/main/resources/db/migration` e são aplicadas automaticamente pelo Flyway quando o backend é iniciado.

Para informações específicas sobre o Supabase, consulte [sgsbarber/SUPABASE.md](sgsbarber/SUPABASE.md).

## Executar o backend

Em um terminal:

```powershell
cd sgsbarber
.\mvnw.cmd spring-boot:run
```

O backend ficará disponível em:

```text
http://localhost:8080
```

## Executar o frontend

Em outro terminal:

```powershell
cd sgsbarber\frontend
node server.js
```

Abra no navegador:

```text
http://localhost:5500
```

O frontend usa a API do backend na porta `8080`. O servidor frontend escuta em `0.0.0.0`, permitindo acesso por outros dispositivos da mesma rede.

## Executar os testes sem Supabase

Os testes automatizados usam H2 em memória e não precisam de conta, senha ou conexão com o Supabase. A configuração fica em `sgsbarber/src/test/resources/application.properties` e é aplicada automaticamente pelo Maven durante os testes.

```powershell
cd sgsbarber
.\mvnw.cmd test
```

Esse modo valida o carregamento do contexto Spring e os testes do backend, mas não inicia o frontend e não substitui o banco PostgreSQL usado na execução normal da aplicação.

## Acesso em celular ou outro computador

1. Execute o frontend e o backend no computador principal.
2. Descubra o endereço IPv4 com `ipconfig`.
3. No outro dispositivo, abra `http://IP-DO-COMPUTADOR:5500`.
4. Se necessário, libere as portas `5500` e `8080` no Firewall do Windows.

## Executar pelo VS Code

1. Abra a raiz do projeto no VS Code.
2. Acesse **Run and Debug** com `Ctrl+Shift+D`.
3. Selecione a configuração **SGS Barber - Supabase**.
4. Pressione `F5` e informe a senha do Supabase quando solicitado.

## Segurança e contribuição

- Nunca faça commit de senhas, tokens ou arquivos `.env`.
- Use novas migrations Flyway para alterações no banco; não edite migrations já aplicadas.
- Mantenha o frontend e o backend em execução para testar o fluxo completo.
- Antes de enviar alterações, valide o backend com o Maven Wrapper e confira se o frontend carrega corretamente.

## Status do projeto

O frontend possui integração preparada com a API REST. Algumas integrações externas, como IA, pagamento, WhatsApp e calendário, dependem de implementação adicional conforme os contratos documentados em [sgsbarber/frontend/TUTORIAL-INTEGRACAO-BACKEND-E-IA.txt](sgsbarber/frontend/TUTORIAL-INTEGRACAO-BACKEND-E-IA.txt).
