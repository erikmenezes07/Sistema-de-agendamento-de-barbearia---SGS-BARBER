# SGS BARBER

Sistema de agendamento e gestão de barbearia com frontend HTML/CSS/JavaScript, API Java 17 / Spring Boot 3.3.4 e PostgreSQL. Código reorganizado a partir do arquivo `sgsbarbearia.rar`.

## Organização

| Pasta/arquivo | Conteúdo |
| --- | --- |
| `frontend/` | Páginas, estilos, JavaScript e servidor estático Node |
| `backend/` | Projeto Maven / Spring Boot |
| `backend/src/main/java/com/sgsbarber/controller/` | Endpoints REST |
| `backend/src/main/java/com/sgsbarber/service/` | Regras de negócio |
| `backend/src/main/java/com/sgsbarber/repository/` | Persistência JPA |
| `backend/src/main/java/com/sgsbarber/model/` | Entidades |
| `backend/src/main/java/com/sgsbarber/dto/` | Objetos de transferência |
| `backend/src/main/resources/db/migration/` | Migrações Flyway V1, V2 e V3 |
| `scripts/` | Inicialização no Windows |
| `tests/` | Testes do servidor frontend |
| `docs/` | GitHub, banco, análise e validação |
| `.github/workflows/ci.yml` | Verificações no GitHub Actions |
| `.env.example` | Modelo de configuração local |
| `compose.yaml` | PostgreSQL local opcional |

## Executar no Windows (PowerShell)

Pré-requisitos: Git, JDK 17, Node.js 22 ou superior. Para o banco local, Docker Desktop com Docker Compose. O Maven Wrapper baixa o Maven e as dependências na primeira execução; é necessária conexão com a internet.

Na raiz do projeto:

```powershell
Copy-Item .env.example .env
notepad .env
```

Troque as duas senhas de exemplo por uma mesma senha local. Depois:

```powershell
docker compose up -d --wait
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

A opção `ExecutionPolicy Bypass` vale apenas para esse processo. O script carrega `.env` sem executar seu conteúdo e inicia o backend em `http://localhost:8080`.

Em outro terminal, na raiz:

```powershell
npm --prefix frontend start
```

Acesse **http://localhost:5500**. O servidor frontend escuta somente no computador local por padrão.

Para encerrar, use Ctrl+C nos dois terminais e `docker compose down`. O banco permanece no volume Docker. Alterar a senha no `.env` depois de criar o volume não altera a senha do usuário existente no PostgreSQL.

## Usar Supabase

Não é necessário executar o Docker. Configure `.env` conforme [docs/BANCO.md](docs/BANCO.md) e use o mesmo script de inicialização. Nenhum identificador de projeto Supabase original foi mantido na configuração padrão.

## Demonstração sem banco

Em `frontend/config.js`, mude `USE_API` para `false` e inicie apenas o frontend. A demonstração usa dados fictícios no localStorage, separados por navegador. Acessos de demonstração: `admin@sgsbarber.com` (área profissional) e `cliente@sgsbarber.com` (cliente), senha `123456`. Essas contas **não são criadas no PostgreSQL**.

No modo API, um banco novo começa vazio. As migrações criam as tabelas, sem contas ou catálogo. O código recebido oferece cadastro de cliente e barbeiro; crie uma conta profissional para cadastrar serviços e configurar a barbearia, depois crie o cliente para testar. O registro público de barbeiro precisa ser restringido antes de operação pública; veja a análise.

## Verificar

```powershell
npm --prefix frontend run check
npm --prefix frontend test
cd backend
.\mvnw.cmd verify
```

O último comando requer as variáveis do banco no terminal e PostgreSQL disponível. O Maven sozinho não lê `.env`; carregue as variáveis manualmente como em [docs/BANCO.md](docs/BANCO.md). No Linux/macOS, use `chmod +x mvnw` e `./mvnw verify`.

O GitHub Actions foi configurado para executar verificações JavaScript e `mvn verify` com PostgreSQL temporário. O resultado real aparece na aba Actions após o envio.

## Enviar ao repositório

Siga [docs/GITHUB.md](docs/GITHUB.md) para aplicar em uma branch e revisar as alterações.

## Limites conhecidos

O pacote preserva as regras de negócio recebidas. IA, pagamentos e envio automático por WhatsApp permanecem como recursos não implementados. A revisão técnica e os resultados dos testes estão em [docs/ANALISE.md](docs/ANALISE.md).

GitHub Pages hospeda o frontend estático; a API Java e o PostgreSQL precisam de hospedagem própria. Publicar o frontend não coloca o sistema completo em funcionamento. Configure a URL HTTPS da API em `frontend/config.js`, CORS e a política de cookies/sessão conforme o domínio escolhido. A execução local documentada usa `localhost` para ambos os servidores.
