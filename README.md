# SGS Barber

Sistema de gestão e agendamento para barbearia, com frontend web, backend
Spring Boot e banco PostgreSQL hospedado no Supabase.

## Requisitos

- Git
- JDK 17 ou superior
- Node.js
- Visual Studio Code (opcional, mas recomendado)
- Acesso à internet para baixar as dependências Maven

## Clonar o projeto

```powershell
git clone https://github.com/thalestadeu18/sgsbarbearia.git
cd sgsbarbearia
```

## Configurar o banco Supabase

O projeto usa o PostgreSQL do Supabase. A senha não é armazenada no GitHub.

No PowerShell, informe a senha antes de iniciar o backend:

```powershell
$env:SUPABASE_DB_PASSWORD="SUA_SENHA_DO_SUPABASE"
```

As demais configurações estão em
`sgsbarber/src/main/resources/application.properties` e usam o Session Pooler
do Supabase. As tabelas são criadas automaticamente pelo Flyway na primeira
execução.

Para mais detalhes, consulte [`sgsbarber/SUPABASE.md`](sgsbarber/SUPABASE.md).

## Iniciar pelo Run and Debug

1. Abra a pasta raiz `sgsbarbearia` no VS Code.
2. Abra **Run and Debug** (`Ctrl+Shift+D`).
3. Selecione **SGS Barber - Supabase**.
4. Clique em **Start Debugging** ou pressione `F5`.
5. Quando solicitado, informe a senha do banco Supabase.

O backend será iniciado na porta `8080`.

## Iniciar manualmente o backend

Caso não use o VS Code:

```powershell
cd sgsbarber
.\mvnw.cmd spring-boot:run
```

## Iniciar o frontend

Com o backend em execução, abra outro terminal:

```powershell
cd frontend
node server.js
```

Abra no navegador:

```text
http://localhost:5500
```

O frontend está configurado para usar a API na porta `8080`.

## Acessar pelo celular ou outro computador

O celular e o computador que executa o projeto precisam estar na mesma rede.

Descubra o IPv4 do computador com:

```powershell
ipconfig
```

Depois, no celular ou outro computador, acesse:

```text
http://IP-DO-COMPUTADOR:5500
```

Exemplo:

```text
http://10.0.0.174:5500
```

Se não abrir, permita as portas `5500` e `8080` no Firewall do Windows e
confirme que os dois dispositivos estão na mesma rede Wi-Fi.

## Estrutura principal

- `frontend/`: páginas, estilos e servidor web estático.
- `sgsbarber/`: backend Spring Boot, entidades, regras, API REST e migrations.
- `.vscode/launch.json`: configuração para iniciar o backend pelo Run and Debug.

## Observações

- Não commite a senha do Supabase.
- O banco usado é o projeto Supabase configurado nas variáveis de ambiente.
- Alterações de estrutura do banco devem ser feitas com novas migrations Flyway
  em `sgsbarber/src/main/resources/db/migration`.
