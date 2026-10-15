# Aplicar no seu GitHub

Destino: https://github.com/erikmenezes07/Sistema-de-agendamento-de-barbearia---SGS-BARBER

O repositório remoto não foi acessível durante esta preparação. Este pacote contém o código do RAR reorganizado; não é um diff validado contra a versão atual do GitHub. Os comandos abaixo são para PowerShell no seu computador e preservam o histórico remoto.

## 1. Clonar e criar a branch

```powershell
git clone https://github.com/erikmenezes07/Sistema-de-agendamento-de-barbearia---SGS-BARBER.git
cd Sistema-de-agendamento-de-barbearia---SGS-BARBER
git switch -c reorganizacao/estrutura-sgs
```

Se já tem uma cópia local, salve seu trabalho pendente e crie a branch nela. Não use `git init` sobre o clone e não copie os históricos `.git` do RAR.

## 2. Extrair o pacote

Extraia `SGS-BARBER-organizado.zip` em uma pasta temporária. Copie **o conteúdo de SGS-BARBER** para a raiz do clone, incluindo `.github`, `.vscode`, `.gitignore`, `.gitattributes` e `.env.example`. A pasta `.git` criada pelo clone deve permanecer.

Ao abrir a raiz do clone, você deverá ver `frontend`, `backend`, `docs`, `scripts`, `tests`, `README.md` e `compose.yaml`. Evite criar uma pasta SGS-BARBER extra dentro do repositório.

Compare arquivos existentes antes de sobrescrever: o remoto pode conter mudanças que não estavam no RAR. Incorpore essas mudanças nos arquivos correspondentes de `frontend/` ou `backend/`.

Depois dessa comparação, remova pelo VS Code ou Explorador **apenas** as pastas antigas substituídas: `sgsbarber/` (agora `backend/`) e `sgsbarbearia/` se forem as cópias antigas. Se o repositório contém outros arquivos próprios, mantenha-os e ajuste os caminhos. Não apague a pasta `.git` do clone.

Arquivos de `target/` e `.env` já rastreados pelo Git precisam também sair do índice; `.gitignore` só impede novos arquivos. Localize-os com `git ls-files` e, se presentes, use `git rm --cached -- CAMINHO_DO_ARQUIVO` (ou `git rm -r --cached -- CAMINHO_DA_PASTA`) para deixar de versioná-los sem apagar sua cópia local.

## 3. Revisar e enviar

```powershell
npm --prefix frontend run check
npm --prefix frontend test
git status --short
git add .
git diff --cached --stat
git diff --cached
```

Confira se as pastas antigas aparecem removidas e os códigos aparecem nos novos caminhos. Não inclua senhas, `.env`, arquivos RAR ou `target/`.

```powershell
git commit -m "Reorganiza SGS Barber em frontend e backend"
git push -u origin reorganizacao/estrutura-sgs
```

Abra o repositório no navegador, use **Compare & pull request**, escolha a branch principal como destino e revise o resultado do GitHub Actions. Nenhum comando de envio foi executado durante a preparação do pacote.

## Inicialização

Siga o README da raiz para configurar o banco e iniciar os dois servidores. Os caminhos antigos `sgsbarber/` dos seus próprios scripts devem passar a `backend/`.
