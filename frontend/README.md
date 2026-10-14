# SGS Barber — Front-end

Sistema de gestão e agendamento para barbearia, desenvolvido somente no front-end.

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

O projeto não implementa banco, backend, IA real, pagamento real ou WhatsApp automático.

O modo atual usa `localStorage` por meio de `api.js`. Para conectar o backend, altere `config.js` para `USE_API: true` e implemente os contratos documentados em `TUTORIAL-INTEGRACAO-BACKEND-E-IA.txt`.

## Arquitetura

`HTML/CSS → app.js → Store (api.js) → Backend REST → Banco`

A IA deve ser acessada pelo backend. Chaves secretas nunca devem aparecer no JavaScript do navegador.
