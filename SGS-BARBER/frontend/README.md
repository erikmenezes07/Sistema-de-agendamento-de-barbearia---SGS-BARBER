# SGS Barber — Front-end

Sistema de gestão e agendamento para barbearia. O front-end usa a API Spring
Boot por padrão (`USE_API: true` em `config.js`).

## Incluído

- Página pública da barbearia (`index.html`)
- Login/cadastro de cliente e barbeiro
- Agendamento: barbeiro → perfil → serviço → data → horário
- Perfil individual do barbeiro
- Favorito do cliente
- Avaliação após atendimento concluído
- Histórico e agendar novamente
- Dashboard operacional de hoje
- Agenda visual por barbeiro
- Lista de espera
- Comanda e fechamento de atendimento
- Produtos e estoque
- Clientes e CRM
- Clientes para reativação
- Aniversários
- Escalas, folgas, férias/ausências, intervalos e buffers
- Central de notificações
- Relatórios
- Configurações
- Placeholder de IA
- Preparação para WhatsApp e calendário
- Layout responsivo para celular, tablet e PC

## Importante

O backend e o banco são necessários para autenticação e dados persistentes. O
modo de demonstração local usa `localStorage`: altere `USE_API` para `false` em
`config.js`. Esse modo não usa o Supabase nem compartilha dados entre
navegadores.

IA, pagamentos e envio automático por WhatsApp não estão implementados.
Consulte `TUTORIAL-INTEGRACAO-BACKEND-E-IA.txt` para os contratos e limites da
integração.

## Arquitetura

`HTML/CSS → app.js → Store (api.js) → Backend REST → Banco`

A IA deve ser acessada pelo backend. Chaves secretas nunca devem aparecer no JavaScript do navegador.
