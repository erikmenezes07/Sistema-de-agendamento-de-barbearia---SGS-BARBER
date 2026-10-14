# Configuração do Supabase

O backend usa o PostgreSQL do Supabase via JDBC. A migração
`src/main/resources/db/migration/V1__create_sgs_barber_schema.sql` cria as tabelas
recomendadas no tutorial e é executada automaticamente pelo Flyway na primeira
subida da aplicação.

No painel do Supabase, abra **Connect** e copie a connection string do
**Session pooler**. Ela é a opção correta quando o painel não oferece uma
conexão direta IPv6 para o ambiente onde o backend será executado.

Se o Supabase mostrar uma URI como:

```text
postgresql://postgres.<project-ref>:<senha>@<host-do-pooler>:5432/postgres
```

adicione `jdbc:` no início para formar a URL JDBC e configure as variáveis:

```text
SUPABASE_DB_URL=jdbc:postgresql://<host-do-pooler>:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=postgres.<project-ref>
SUPABASE_DB_PASSWORD=<senha-do-banco>
SUPABASE_DB_SSLMODE=require
```

Não cole a URI completa em `SUPABASE_DB_USERNAME`. Separe o usuário, a senha e
a URL conforme o exemplo acima. A connection string **Direct** só deve ser usada
se o ambiente do backend tiver conectividade IPv6 e você escolher explicitamente
essa opção no painel.

Para desenvolvimento local, as propriedades têm como padrão um PostgreSQL em
`localhost:5432/sgs_barber`, usuário `postgres`. A senha não fica armazenada no
repositório; informe `SUPABASE_DB_PASSWORD` no ambiente.

Após configurar as variáveis, execute:

```text
mvn spring-boot:run
```

O `ddl-auto=validate` impede que o Hibernate altere o banco sem controle. Novas
alterações de estrutura devem ser adicionadas como novas migrações Flyway em
`src/main/resources/db/migration`, por exemplo `V2__add_column.sql`.
