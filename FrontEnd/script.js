let currentProfile = localStorage.getItem('sgs_user_profile') || 'admin';
let authMode = 'login';
let currentStep = 1;

// ============================ Agente de IA ============================
// O navegador NUNCA fala com o provedor de modelo: envia a mensagem para
// POST /api/chat e o back-end decide qual agente responde e quais tools
// consultar no banco. A chave da IA fica somente no servidor.

// A porta do back-end nao pode ser fixada aqui. O Spring Boot sobe em 8080 por
// padrao, mas em desenvolvimento e comum rodar em 8081 (ou 5173/3000 com
// proxy), e um valor fixo no front vira "servidor nao encontrado" sem aviso
// util. Em vez disso, o front pergunta /api/chat/config em uma lista de
// enderecos plausiveis e usa o primeiro que responder. A ordem importa: mesma
// origem primeiro (caso o front e a API sejam servidos juntos), depois as
// portas de desenvolvimento mais comuns.
const CANDIDATOS_API = [
    'http://localhost:8080',
    'http://localhost:8081',
    'http://127.0.0.1:8080',
    'http://127.0.0.1:8081',
    'http://localhost:5173',
    'http://localhost:3000'
];

let API_BASE = (window.SGS_API_BASE || '').replace(/\/$/, '');
let apiResolvida = API_BASE !== '';

function candidatosDaApi() {
    if (API_BASE) {
        return [API_BASE];
    }
    // Servido por http(s), a propria origem e a aposta mais provavel.
    if (location.protocol === 'http:' || location.protocol === 'https:') {
        return [location.origin].concat(CANDIDATOS_API);
    }
    return CANDIDATOS_API;
}

/**
 * Descobre em qual endereco a API responde. Faz so uma vez e memoriza, senao
 * toda mensagem repetiria a varredura de portas.
 *
 * Repete a varredura algumas vezes antes de desistir, porque o caso comum nao
 * e "backend nunca existiu": e a pagina aberta enquanto o Spring Boot ainda
 * esta subindo. Sem essa espera, abrir o front antes do backend dava erro
 * imediato e obrigava a recarregar a pagina na mao.
 */
const TENTATIVAS_DE_BUSCA = 4;
const ESPERA_ENTRE_BUSCAS_MS = 1500;

async function resolverApiBase() {
    if (apiResolvida) {
        return API_BASE;
    }
    const tentou = [];
    for (let rodada = 1; rodada <= TENTATIVAS_DE_BUSCA; rodada++) {
        for (const candidato of candidatosDaApi()) {
            tentou.push(candidato);
            try {
                const resposta = await fetch(candidato + '/api/chat/config', { cache: 'no-store' });
                if (resposta.ok) {
                    API_BASE = candidato;
                    apiResolvida = true;
                    return API_BASE;
                }
            } catch (e) { /* porta fechada: tenta a proxima */ }
        }
        if (rodada < TENTATIVAS_DE_BUSCA) {
            definirStatus('aguardando o servidor...', true);
            await esperar(ESPERA_ENTRE_BUSCAS_MS);
        }
    }
    definirStatus('servidor offline', true);
    throw new Error('Nao achei o backend do SGS-BARBER. Tentei:\n- ' + tentou.join('\n- ')
        + '\n\nSuba o Spring Boot (cd Backend/sgs-barber e mvnw.cmd spring-boot:run) '
        + 'ou defina window.SGS_API_BASE no HTML antes de carregar o script.js.');
}

function esperar(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

const CHAT_IA = {
    // Identifica a sessao no servidor. Persistido para o historico sobreviver
    // a F5; o back-end agrupa as mensagens por este id.
    usuarioId: localStorage.getItem('sgs_chat_usuario') || gerarUsuarioId(),
    clienteId: null,
    enviando: false,
    config: null
};

function gerarUsuarioId() {
    const id = 'web-' + (crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2) + Date.now().toString(36));
    localStorage.setItem('sgs_chat_usuario', id);
    return id;
}

// Pequeno wrapper de fetch: o front inteiro e localStorage, entao o chat e o
// unico lugar que fala HTTP. Centralizar aqui evita repetir tratamento de erro.
async function apiPost(caminho, corpo) {
    const base = await resolverApiBase();
    // Se o backend só subiu depois da pagina carregar, o badge ainda está
    // "servidor offline". A checagem acontece na primeira mensagem que dá
    // certo, então o estado se corrige sozinho sem o usuario recarregar a
    // pagina - que era o que acontecia antes.
    if (!CHAT_IA.config) {
        carregarConfigIA();
    }
    let resposta;
    try {
        resposta = await fetch(base + caminho, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(corpo)
        });
    } catch (e) {
        throw new Error('Perdi a conexao com o servidor em ' + base + '. Ele ainda esta rodando?');
    }

    if (!resposta.ok) {
        let detalhe = '';
        try {
            const erro = await resposta.json();
            detalhe = erro.message || erro.error || '';
        } catch (e) { /* resposta sem JSON */ }
        throw new Error(detalhe || ('Erro ' + resposta.status + ' do servidor.'));
    }
    return resposta.json();
}

let bookingData = { service: '', price: '', barber: '', date: '24/09/2026', time: '' };

// Estado inicial carregado no LocalStorage se estiver vazio
const defaultServices = [
    { id: 1, name: 'Corte Degradê', price: '40.00', duration: '30 min' },
    { id: 2, name: 'Barba Completa', price: '30.00', duration: '30 min' }
];
const defaultBarbers = [
    { id: 1, name: 'Carlos', specialty: 'Degradê / Social', phone: '(81) 99888-1111', scale: '08:00 - 17:00' },
    { id: 2, name: 'Marcos', specialty: 'Barba & Tesoura', phone: '(81) 99777-2222', scale: '08:00 - 17:00' }
];
const defaultClients = [
    { id: 1, name: 'Lucas Silva', phone: '(81) 97777-2222', email: 'lucas@email.com' },
    { id: 2, name: 'Bruno Souza', phone: '(81) 96666-3333', email: 'bruno@email.com' }
];
const defaultAppointments = [
    { id: 1, barber: 'Carlos', time: '08:00', client: 'Lucas Silva', service: 'Corte Degradê (30min)' },
    { id: 2, barber: 'Marcos', time: '08:30', client: 'Bruno Souza', service: 'Corte + Barba (60min)' }
];

function getStorageData(key, def) {
    const data = localStorage.getItem('sgs_' + key);
    return data ? JSON.parse(data) : def;
}

function setStorageData(key, data) {
    localStorage.setItem('sgs_' + key, JSON.stringify(data));
}

document.addEventListener('DOMContentLoaded', () => {
    // Consulta /api/chat/config logo no carregamento, e nao so ao abrir a aba:
    // se o servidor desligou o assistente (ia.agendador-ativo=false), o menu
    // precisa sumir antes de o usuario clicar nele.
    carregarConfigIA();

    // Inicializa localStorage se vazio
    if (!localStorage.getItem('sgs_services')) setStorageData('services', defaultServices);
    if (!localStorage.getItem('sgs_barbers')) setStorageData('barbers', defaultBarbers);
    if (!localStorage.getItem('sgs_clients')) setStorageData('clients', defaultClients);
    if (!localStorage.getItem('sgs_appointments')) setStorageData('appointments', defaultAppointments);

    // Gestão de Abas de Login
    const profileTabs = document.querySelectorAll('#profileTypeTabs .tab-btn');
    profileTabs.forEach(btn => {
        btn.addEventListener('click', (e) => {
            profileTabs.forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            currentProfile = e.target.getAttribute('data-profile');
        });
    });

    const authForm = document.getElementById('authForm');
    if (authForm) {
        authForm.addEventListener('submit', (e) => {
            e.preventDefault();
            
            if (authMode === 'register') {
                const name = document.getElementById('reg-name').value;
                const clients = getStorageData('clients', defaultClients);
                clients.push({ id: Date.now(), name: name, phone: '(81) 90000-0000', email: 'novo@email.com' });
                setStorageData('clients', clients);
                
                showToast('Conta criada com sucesso! Faça login para continuar.', 'success');
                
                // Retorna para a tela de login após o cadastro sem entrar direto
                setTimeout(() => {
                    switchAuthMode('login');
                    authForm.reset();
                }, 1500);
                
                return;
            }

            // Modo Login normal
            localStorage.setItem('sgs_user_profile', currentProfile);
            window.location.href = 'dashboard.html';
        });
    }

    // Configuração do Dashboard por Perfil
    const savedProfile = localStorage.getItem('sgs_user_profile') || 'admin';
    if (savedProfile === 'cliente') {
        const adminSidebar = document.getElementById('adminSidebar');
        const adminViewsContainer = document.getElementById('adminViewsContainer');
        const clientBookingContainer = document.getElementById('clientBookingContainer');
        const pageTitle = document.getElementById('pageTitle');
        const userNameDisplay = document.getElementById('userNameDisplay');
        const userAvatar = document.getElementById('userAvatar');

        if(adminSidebar) adminSidebar.style.display = 'none';
        if(adminViewsContainer) adminViewsContainer.style.display = 'none';
        if(clientBookingContainer) clientBookingContainer.style.display = 'block';
        if(pageTitle) pageTitle.textContent = 'Painel do Cliente - Novo Agendamento';
        if(userNameDisplay) userNameDisplay.textContent = 'Cliente (Você)';
        if(userAvatar) userAvatar.src = 'https://ui-avatars.com/api/?name=Cliente+SGS&background=0284c7&color=fff';
        loadClientWizardData();
    } else {
        const adminSidebar = document.getElementById('adminSidebar');
        const adminViewsContainer = document.getElementById('adminViewsContainer');
        const clientBookingContainer = document.getElementById('clientBookingContainer');

        if(adminSidebar) adminSidebar.style.display = 'flex';
        if(adminViewsContainer) adminViewsContainer.style.display = 'block';
        if(clientBookingContainer) clientBookingContainer.style.display = 'none';
        renderAdminTables();
        renderKanban();
    }

    // Menu lateral Admin
    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            menuItems.forEach(m => m.classList.remove('active'));
            item.classList.add('active');

            const targetId = item.getAttribute('data-target');
            document.querySelectorAll('.admin-view').forEach(view => view.classList.remove('active'));
            const targetView = document.getElementById(targetId);
            if(targetView) targetView.classList.add('active');
            
            const pageTitle = document.getElementById('pageTitle');
            if(pageTitle) pageTitle.textContent = item.textContent.trim();

            // primeira vez que a aba do assistente abre, monta a saudacao e
            // pergunta ao servidor qual modelo esta de fato ativo
    if(targetId === 'view-ai') {
        // A view pode ter sido removida por aplicarAgendadorDesativado().
        const chatBox = document.getElementById('aiChatBox');
        if(chatBox && !chatBox.hasChildNodes()) {
            saudacaoIA();
        }
    }
        });
    });
});

// Autenticação modos
function switchAuthMode(mode) {
    authMode = mode;
    const modeLogin = document.getElementById('modeLogin');
    const modeRegister = document.getElementById('modeRegister');
    const groupName = document.getElementById('groupName');
    const btnSubmitAction = document.getElementById('btnSubmitAction');
    const forgotPassLink = document.getElementById('forgotPassLink');

    if(modeLogin) modeLogin.classList.toggle('active-mode', mode === 'login');
    if(modeRegister) modeRegister.classList.toggle('active-mode', mode === 'register');
    if(groupName) groupName.style.display = mode === 'register' ? 'block' : 'none';
    if(btnSubmitAction) btnSubmitAction.textContent = mode === 'register' ? 'Cadastrar Conta' : 'Entrar';
    if(forgotPassLink) forgotPassLink.style.display = mode === 'register' ? 'none' : 'block';
}

// Renderização das Tabelas CRUD Dinâmicas
function renderAdminTables() {
    // Serviços
    const services = getStorageData('services', defaultServices);
    const srvBody = document.getElementById('tableServicosBody');
    if(srvBody) {
        srvBody.innerHTML = services.map(s => `
            <tr><td>${s.name}</td><td>R$ ${s.price}</td><td>${s.duration}</td>
            <td><button class="btn-action text-danger" onclick="deleteItem('services', ${s.id})"><i class="fa-solid fa-trash"></i></button></td></tr>
        `).join('');
    }

    // Barbeiros
    const barbers = getStorageData('barbers', defaultBarbers);
    const barBody = document.getElementById('tableBarbeirosBody');
    if(barBody) {
        barBody.innerHTML = barbers.map(b => `
            <tr><td>${b.name}</td><td>${b.specialty}</td><td>${b.phone}</td><td>${b.scale}</td>
            <td><button class="btn-action text-danger" onclick="deleteItem('barbers', ${b.id})"><i class="fa-solid fa-trash"></i></button></td></tr>
        `).join('');
    }

    // Clientes
    const clients = getStorageData('clients', defaultClients);
    renderClientsTable(clients);
}

function renderClientsTable(clients) {
    const cliBody = document.getElementById('tableClientesBody');
    if(cliBody) {
        cliBody.innerHTML = clients.map(c => `
            <tr><td>${c.name}</td><td>${c.phone}</td><td>${c.email}</td>
            <td><button class="btn-action text-danger" onclick="deleteItem('clients', ${c.id})"><i class="fa-solid fa-trash"></i></button></td></tr>
        `).join('');
    }
}

// Filtro de Busca em Tempo Real para Clientes
function filterClients() {
    const searchInput = document.getElementById('searchClientInput');
    if(!searchInput) return;
    const query = searchInput.value.toLowerCase();
    const clients = getStorageData('clients', defaultClients);
    const filtered = clients.filter(c => c.name.toLowerCase().includes(query) || c.email.toLowerCase().includes(query));
    renderClientsTable(filtered);
}

// Renderizar Agenda Kanban Dinâmica
function renderKanban() {
    const barbers = getStorageData('barbers', defaultBarbers);
    const appointments = getStorageData('appointments', defaultAppointments);
    const grid = document.getElementById('kanbanGridContainer');
    if(!grid) return;

    const slots = ['08:00', '08:30', '09:00', '09:30', '10:00'];

    grid.innerHTML = barbers.map(b => {
        const colAppointments = appointments.filter(a => a.barber === b.name);
        return `
            <div class="kanban-column">
                <div class="column-header"><h4>${b.name}</h4><span class="badge-status">Ativo</span></div>
                <div class="time-slots">
                    ${slots.map(time => {
                        const app = colAppointments.find(a => a.time === time);
                        if (app) {
                            return `
                                <div class="time-slot-row"><span class="time-label">${time}</span>
                                    <div class="appointment-card occupied">
                                        <div class="card-info"><strong>${app.client}</strong><small>${app.service}</small></div>
                                        <div class="card-actions">
                                            <button onclick="cancelAppointment(${app.id})" title="Cancelar"><i class="fa-solid fa-trash"></i></button>
                                        </div>
                                    </div>
                                </div>`;
                        } else {
                            return `
                                <div class="time-slot-row"><span class="time-label">${time}</span>
                                    <div class="appointment-card free"><span class="free-slot">Horário Disponível</span></div>
                                </div>`;
                        }
                    }).join('')}
                </div>
            </div>`;
    }).join('');
}

// Modais Genéricos
function openModal(id) { 
    const modal = document.getElementById(id);
    if(modal) modal.style.display = 'flex'; 
}
function closeModal(id) { 
    const modal = document.getElementById(id);
    if(modal) modal.style.display = 'none'; 
}

// Salvar novos registros via Modal com Atualização de DOM instantânea
function saveServico(e) {
    e.preventDefault();
    const name = document.getElementById('srvName').value;
    const price = document.getElementById('srvPrice').value;
    const duration = document.getElementById('srvDuration').value;

    const services = getStorageData('services', defaultServices);
    services.push({ id: Date.now(), name, price, duration });
    setStorageData('services', services);

    closeModal('modalServico');
    renderAdminTables();
    showToast('Serviço cadastrado com sucesso!', 'success');
}

function saveBarbeiro(e) {
    e.preventDefault();
    const name = document.getElementById('barName').value;
    const specialty = document.getElementById('barSpec').value;
    const phone = document.getElementById('barPhone').value;
    const scale = document.getElementById('barScale').value;

    const barbers = getStorageData('barbers', defaultBarbers);
    barbers.push({ id: Date.now(), name, specialty, phone, scale });
    setStorageData('barbers', barbers);

    closeModal('modalBarbeiro');
    renderAdminTables();
    showToast('Barbeiro cadastrado com sucesso!', 'success');
}

function deleteItem(type, id) {
    let items = getStorageData(type, []);
    items = items.filter(i => i.id !== id);
    setStorageData(type, items);
    if(type === 'services' || type === 'barbers' || type === 'clients') renderAdminTables();
    showToast('Registro removido com sucesso!', 'info');
}

function cancelAppointment(id) {
    let apps = getStorageData('appointments', defaultAppointments);
    apps = apps.filter(a => a.id !== id);
    setStorageData('appointments', apps);
    renderKanban();
    showToast('Agendamento cancelado.', 'info');
}

// Notificações Toast Modernas
function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    if(!container) return;
    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;
    toast.innerHTML = `<i class="fa-solid ${type === 'success' ? 'fa-circle-check' : 'fa-circle-info'}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Wizard do Cliente
function loadClientWizardData() {
    const services = getStorageData('services', defaultServices);
    const serviceGrid = document.getElementById('clientServiceGrid');
    if(serviceGrid) {
        serviceGrid.innerHTML = services.map(s => `
            <div class="selectable-card" onclick="selectService('${s.name}', '${s.price}', '${s.duration}')">
                <h4>${s.name}</h4><p>Atendimento profissional e personalizado.</p>
                <div class="card-meta"><span>${s.duration}</span><strong>R$ ${s.price}</strong></div>
            </div>
        `).join('');
    }

    const barbers = getStorageData('barbers', defaultBarbers);
    const barberGrid = document.getElementById('clientBarberGrid');
    if(barberGrid) {
        barberGrid.innerHTML = barbers.map(b => `
            <div class="selectable-card avatar-card" onclick="selectBarber('${b.name}')">
                <img src="https://ui-avatars.com/api/?name=${b.name}+Barber&background=d97706&color=fff" alt="${b.name}">
                <div><h4>${b.name}</h4><small>${b.specialty}</small></div>
            </div>
        `).join('');
    }
}

function selectService(name, price, duration) {
    bookingData.service = name + ' (' + duration + ')';
    bookingData.price = 'R$ ' + price;
    changeStep(1);
}

function selectBarber(name) {
    bookingData.barber = name;
    changeStep(1);
}

function updateBookingDate(date) { 
    bookingData.date = date; 
}

function selectTime(time) {
    bookingData.time = time;
    document.querySelectorAll('.time-chip').forEach(c => c.classList.remove('selected'));
    if(event && event.target) event.target.classList.add('selected');

    const summaryService = document.getElementById('summaryService');
    const summaryBarber = document.getElementById('summaryBarber');
    const summaryDateTime = document.getElementById('summaryDateTime');
    const summaryPrice = document.getElementById('summaryPrice');

    if(summaryService) summaryService.textContent = bookingData.service;
    if(summaryBarber) summaryBarber.textContent = bookingData.barber;
    if(summaryDateTime) summaryDateTime.textContent = bookingData.date + ' às ' + bookingData.time;
    if(summaryPrice) summaryPrice.textContent = bookingData.price;
    changeStep(1);
}

function confirmClientBooking() {
    const apps = getStorageData('appointments', defaultAppointments);
    apps.push({ id: Date.now(), barber: bookingData.barber, time: bookingData.time, client: 'Cliente Logado', service: bookingData.service });
    setStorageData('appointments', apps);
    showToast('Agendamento confirmado com sucesso!', 'success');
    setTimeout(() => location.reload(), 1500);
}

function changeStep(direction) {
    const steps = document.querySelectorAll('.wizard-step-content');
    const badges = document.querySelectorAll('.step-badge');
    currentStep += direction;
    if (currentStep < 1) currentStep = 1;
    if (currentStep > 4) currentStep = 4;

    steps.forEach((step, idx) => step.classList.toggle('active', idx === currentStep - 1));
    badges.forEach((badge, idx) => badge.classList.toggle('active', idx === currentStep - 1));

    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    if(prevBtn) prevBtn.style.display = currentStep > 1 ? 'block' : 'none';
    if(nextBtn) nextBtn.style.display = currentStep < 4 ? 'block' : 'none';
}

// Chat do Agente IA: envia para POST /api/chat e desenha a resposta real,
// incluindo de qual agente veio e quais tools ele consultou no banco.
async function handleAiSend() {
    const input = document.getElementById('aiUserInput');
    const box = document.getElementById('aiChatBox');
    if (!input || !box || !input.value.trim() || CHAT_IA.enviando) return;

    const texto = input.value.trim();
    box.innerHTML += `<div class="ai-message user">${escaparHtml(texto)}</div>`;
    input.value = '';
    box.scrollTop = box.scrollHeight;

    bloquearChat(true);
    mostrarDigitando(box);
    definirStatus('consultando a agenda...', true);

    try {
        const resposta = await apiPost('/api/chat', {
            // O nome do campo precisa bater com ChatRequestDTO: 'usuarioId',
            // e nao 'usuario_id' — o campo errado devolve 400.
            usuarioId: CHAT_IA.usuarioId,
            mensagem: texto,
            clienteId: CHAT_IA.clienteId
        });

        removerDigitando(box);
        desenharRespostaIA(box, resposta);
    } catch (e) {
        removerDigitando(box);
        box.innerHTML += `<div class="ai-message bot">Nao consegui falar com o assistente: ${escaparHtml(e.message)}</div>`;
        definirStatus('erro na chamada', true);
        box.scrollTop = box.scrollHeight;
    } finally {
        bloquearChat(false);
    }
}

// O trace usa "nome"; "ferramenta" e aceito por compatibilidade com respostas
// antigas, para o rotulo nunca sair vazio.
function nomeDaTool(t) {
    return (t && (t.nome || t.ferramenta)) || 'ferramenta';
}

function toolFalhou(t) {
    if (!t) return false;
    if (t.erro) return true;
    return typeof t.resultado === 'string' && t.resultado.indexOf('erro') !== -1;
}

function resumoTool(t) {
    if (!t) return '';
    const base = nomeDaTool(t);
    if (t.argumentos && Object.keys(t.argumentos).length) {
        return base + ' ' + JSON.stringify(t.argumentos);
    }
    return t.resultado ? base + ' - ' + String(t.resultado).slice(0, 120) : base;
}
function desenharRespostaIA(box, r) {
    if (r.encaminhou && r.agente) marcarAgente(r.agente);

    box.innerHTML += `<div class="ai-message bot">${formatarTexto(r.mensagem)}</div>`;

    if (r.ferramentasUsadas && r.ferramentasUsadas.length) {
        box.innerHTML += `<div class="ai-trace"><span class="ai-trace-label">consultou no banco</span>${
            r.ferramentasUsadas.map(t =>
                `<span class="ai-trace-tool${toolFalhou(t) ? ' erro' : ''}" title="${escaparHtml(resumoTool(t))}">${escaparHtml(nomeDaTool(t))}</span>`
            ).join('')}</div>`;
    }

    if (r.aviso) {
        box.innerHTML += `<div class="ai-message aviso">${escaparHtml(r.aviso)}</div>`;
    }

    marcarAgente(r.agente);
    definirStatus('pronto', false);
    box.scrollTop = box.scrollHeight;
}

// Badge do cabecalho: mostra com quem a conversa esta.
function marcarAgente(tipo) {
    const badge = document.getElementById('aiAgentBadge');
    const header = document.getElementById('aiChatHeader');
    if (!badge || !header) return;
    header.hidden = false;
    const recomendador = tipo === 'RECOMENDADOR';
    badge.textContent = recomendador ? 'Recomendador' : 'Agendador';
    badge.classList.toggle('recomendador', recomendador);
}

function definirStatus(texto, ocupado) {
    const status = document.getElementById('aiStatus');
    if (!status) return;
    status.textContent = texto;
    status.classList.toggle('ocupado', !!ocupado);
}

function bloquearChat(bloqueado) {
    CHAT_IA.enviando = bloqueado;
    const input = document.getElementById('aiUserInput');
    const btn = document.getElementById('aiSendBtn');
    if (input) input.disabled = bloqueado;
    if (btn) btn.disabled = bloqueado;
}

function mostrarDigitando(box) {
    box.innerHTML += `<div class="ai-message bot" id="aiTyping"><span class="ai-typing"><span></span><span></span><span></span></span></div>`;
    box.scrollTop = box.scrollHeight;
}

function removerDigitando(box) {
    const el = document.getElementById('aiTyping');
    if (el) el.remove();
}

// A IA devolve markdown simples (**negrito**) e listas com "- ".
function formatarTexto(texto) {
    if (!texto) return '';
    const linhas = String(texto).split('\n');
    const html = [];
    let emLista = false;

    linhas.forEach(linha => {
        const item = linha.match(/^\s*[-*]\s+(.*)$/);
        if (item) {
            if (!emLista) { html.push('<ul>'); emLista = true; }
            html.push(`<li>${formatarInline(item[1])}</li>`);
            return;
        }
        if (emLista) { html.push('</ul>'); emLista = false; }
        if (linha.trim()) html.push(`<p>${formatarInline(linha)}</p>`);
    });

    if (emLista) html.push('</ul>');
    return html.join('');
}

function formatarInline(texto) {
    return escaparHtml(texto).replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
}

function escaparHtml(texto) {
    return String(texto == null ? '' : texto)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

// "Nova conversa": limpa o historico no servidor, nao so na tela.
async function reiniciarConversaIA() {
    const box = document.getElementById('aiChatBox');
    if (!box || CHAT_IA.enviando) return;

    bloquearChat(true);
    try {
        await apiPost('/api/chat/reiniciar/' + encodeURIComponent(CHAT_IA.usuarioId), {});
        box.innerHTML = '';
        saudacaoIA();
        definirStatus('pronto', false);
    } catch (e) {
        definirStatus('erro: ' + e.message, true);
    } finally {
        bloquearChat(false);
    }
}

function saudacaoIA() {
    const box = document.getElementById('aiChatBox');
    if (!box) return;
    box.innerHTML = `<div class="ai-message bot">Ola! Sou o assistente do SGS Barber. Posso mostrar os horarios livres, agendar, cancelar, e tambem sugerir corte, servico e quem faz melhor cada estilo.</div>`;
    box.innerHTML += `<div class="ai-trace"><span class="ai-trace-label">modelo</span><span class="ai-trace-tool" id="aiModeloInfo">verificando...</span></div>`;
    carregarConfigIA();
}

// Le /api/chat/config para mostrar o estado real da integracao (chave, modelo,
// fallback) em vez de prometer um modelo que talvez nao esteja ativo.
async function carregarConfigIA() {
    const info = document.getElementById('aiModeloInfo');
    const hint = document.getElementById('aiHint');
    try {
        const base = await resolverApiBase();
        const resposta = await fetch(base + '/api/chat/config', { cache: 'no-store' });
        if (!resposta.ok) throw new Error('HTTP ' + resposta.status);
        const cfg = await resposta.json();
        CHAT_IA.config = cfg;

        if (info) {
            info.textContent = cfg.modelo + (cfg.chave_configurada ? '' : ' (sem chave)');
        }
        if (hint) {
            hint.textContent = cfg.chave_configurada
                ? 'Respostas pelo modelo ' + cfg.modelo + '.'
                : 'Sem IA_API_KEY: as respostas sao montadas por regras e o assistente nao confirma reservas.';
        }
        if (cfg.agendador_ativo === false) {
            // O servidor desligou o assistente: esconder o menu e a view evita
            // o usuario digitar e receber 503 sem entender o motivo.
            aplicarAgendadorDesativado();
        }
    } catch (e) {
        if (info) info.textContent = 'servidor offline';
        if (hint) {
            // Botao de reconectar: subir o Spring Boot leva ~50s com o Maven,
            // e obrigar o usuario a dar F5 para atualizar o estado e chato.
            // Aqui ele so clica e o front procura o backend de novo.
            hint.innerHTML = 'Nao consegui falar com o Spring Boot. Suba o backend '
                + '(cd Backend/sgs-barber e mvnw.cmd spring-boot:run) e depois '
                + '<button type="button" class="ai-link-btn" onclick="reconectarBackend()">reconectar</button>.';
        }
    }
}

/** Tenta achar o backend de novo, sem recarregar a pagina. */
async function reconectarBackend() {
    apiResolvida = false;
    CHAT_IA.config = null;
    definirStatus('procurando o servidor...', true);
    await carregarConfigIA();
    if (CHAT_IA.config) {
        definirStatus('pronto');
    }
}

/** Remove o assistente da navegacao quando o servidor o desligou. */
function aplicarAgendadorDesativado() {
    const menu = document.querySelector('[data-target="view-ai"]');
    if (menu) menu.parentElement ? menu.parentElement.remove() : menu.remove();
    const view = document.getElementById('view-ai');
    if (view) view.remove();
}