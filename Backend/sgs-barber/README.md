# SGS - BARBER — Sistema de Gestão de Agendamento para Barbearia

O **SGS - BARBER** é um sistema web desenvolvido para centralizar o cadastro de clientes, barbeiros (profissionais) e serviços, além de gerenciar a consulta de horários e a realização de agendamentos.

Este projeto faz parte da disciplina de **Fábrica de Software**.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 17
* **Framework:** Spring Boot 3
* **Banco de Dados:** PostgreSQL
* **Persistência de Dados:** Spring Data JPA / Hibernate
* **Gerenciador de Dependências:** Maven

---

## 🚀 Como Executar o Projeto Localmente

### Pré-requisitos
* Java JDK 17
* PostgreSQL 16+
* Git

### Conexão com o Supabase

O backend usa o PostgreSQL do Supabase por meio da variável de ambiente
`SUPABASE_DB_PASSWORD`. Antes de iniciar a API, defina essa variável no
terminal do backend:

```powershell
$env:SUPABASE_DB_PASSWORD = "sua-senha-do-banco"
.\mvnw.cmd spring-boot:run
```

Com a API em `http://localhost:8080`, abra `FrontEnd/login.html` pelo Live
Server. Os cadastros de clientes e barbeiros são enviados imediatamente para
`/api/clientes` e `/api/barbeiros`, e os dados do Supabase são carregados ao
abrir o dashboard.

### Passo a Passo

1. **Clonar o repositório:**
   ```bash
   git clone [https://github.com/SEU_GRUPO/sgs-barber-backend.git](https://github.com/SEU_GRUPO/sgs-barber-backend.git)
   cd sgs-barber-backend