# Banco de dados

## PostgreSQL local

Copie `.env.example` para `.env`. `POSTGRES_PASSWORD` e `SUPABASE_DB_PASSWORD` devem ser iguais. O prefixo SUPABASE foi mantido para compatibilidade com o código; ele também atende PostgreSQL local.

`docker compose up -d --wait` cria somente o banco. O frontend e o backend são iniciados separadamente. O script PowerShell de inicialização lê `.env`; o Maven não o lê automaticamente.

Para usar Maven diretamente no PowerShell:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://localhost:5432/sgsbarber"
$env:SUPABASE_DB_USERNAME="sgsbarber"
$env:SUPABASE_DB_PASSWORD="SUA_SENHA_LOCAL"
$env:SUPABASE_DB_SSLMODE="disable"
cd backend
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, exporte as mesmas quatro variáveis com `export NOME='valor'`, entre em `backend` e execute `./mvnw spring-boot:run`.

## Supabase

Na tela de conexão do seu próprio projeto, obtenha host, porta, banco e usuário PostgreSQL. Para o Session Pooler, use a porta informada pelo painel. Converta para JDBC e preencha `.env`:

```dotenv
SUPABASE_DB_URL=jdbc:postgresql://SEU_HOST:SUA_PORTA/postgres?sslmode=require
SUPABASE_DB_USERNAME=SEU_USUARIO_POSTGRES
SUPABASE_DB_PASSWORD=SUA_SENHA_DO_BANCO
SUPABASE_DB_SSLMODE=require
```

A senha aqui é a senha do PostgreSQL, não uma chave pública ou service_role da API Supabase. Não coloque credenciais em `frontend/config.js`. Os campos POSTGRES_* só são usados pelo Docker local.

## Migrações

- V1: tabelas e índices iniciais.
- V2: admite atendimento de balcão sem cliente cadastrado.
- V3: preserva o nome do cliente no atendimento.

Mantenha os arquivos existentes intactos ao reaproveitar um banco já migrado; crie V4 e seguintes para novas alterações. Não force baseline ou limpe o histórico para contornar erro de checksum. Faça backup e compare o histórico antes de conectar a um banco existente.

## Conexão pela rede local

Os servidores agora escutam em loopback por padrão. Para testar entre dispositivos, defina `SERVER_ADDRESS=0.0.0.0` no `.env` e `$env:HOST="0.0.0.0"` no terminal do frontend. Use as portas 8080 e 5500 e configure no CORS a origem exata usada. O banco não precisa ser exposto à rede. Prefira localhost nos testes iniciais.
