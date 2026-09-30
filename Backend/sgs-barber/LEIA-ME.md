# Como integrar

## 1. Copiar os arquivos Java
Copie o conteúdo de `src/main/java/sgs_barber/` deste pacote direto para dentro
de `sgs-barber/src/main/java/sgs_barber/` do projeto (as pastas já batem:
`model`, `dto`, `repository`, `service`, `controller`).

Nenhum arquivo existente é sobrescrito — são todos arquivos novos.

## 2. Banco de dados
Com `spring.jpa.hibernate.ddl-auto=update` (como já está configurado), o
Hibernate cria as tabelas `horarios_barbeiro` e `agendamentos` sozinho na
próxima vez que rodar a aplicação. O arquivo
`Database-Scripts/criar_tabelas_agendamento.sql` é a versão "documentada"
desse schema — cole ele dentro da pasta `Database/Scripts` do repositório.

## 3. Cadastrar a jornada de trabalho do barbeiro
Antes de qualquer verificação de disponibilidade funcionar, é preciso cadastrar
os horários de expediente de cada barbeiro:

```
POST /api/horarios-barbeiro
{
  "barbeiroId": 1,
  "diaSemana": 1,        // 1 = segunda ... 7 = domingo
  "horaInicio": "09:00",
  "horaFim": "18:00"
}
```

## 4. O endpoint que a IA vai chamar
```
GET /api/agendamentos/disponibilidade?barbeiroId=1&data=2026-10-05&horaInicio=14:00&duracaoMinutos=30
```
Resposta quando está livre:
```json
{ "disponivel": true, "motivo": null, "horariosAlternativos": [] }
```
Resposta quando não está livre:
```json
{
  "disponivel": false,
  "motivo": "Já existe um agendamento nesse horário",
  "horariosAlternativos": ["14:30", "14:45", "16:00"]
}
```

Isso é o que o agente de IA usa como "function/tool" — ele recebe a intenção
do cliente ("quero cortar cabelo sexta às 14h"), extrai barbeiro/data/hora/
serviço, chama esse endpoint, e responde ao cliente com base no JSON.

## Pendências que eu identifiquei e ainda não resolvi aqui
- Não há `@ControllerAdvice` / tratamento de erro global no projeto (os
  `RuntimeException` viram HTTP 500 em vez de 404/400). Recomendo resolver
  isso antes de ligar a IA, porque um agente automatizado precisa de
  respostas de erro previsíveis para lidar bem com falhas.
- A senha do banco está em texto puro no `application.properties`.
