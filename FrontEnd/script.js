let currentProfile = localStorage.getItem('sgs_user_profile') || 'admin';
let authMode = 'login';
let currentStep = 1;
const API_BASE_URL = window.SGS_API_BASE_URL || 'http://localhost:8080/api';

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

async function apiRequest(endpoint, options = {}) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        ...options
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `Erro HTTP ${response.status}`);
    }

    return response.status === 204 ? null : response.json();
}

async function loadRemoteData() {
    if (!document.getElementById('tableBarbeirosBody') && !document.getElementById('clientBookingContainer')) return;

    try {
        const [barbers, clients] = await Promise.all([
            apiRequest('/barbeiros'),
            apiRequest('/clientes')
        ]);

        setStorageData('barbers', barbers.map(barber => ({
            id: barber.id,
            name: barber.nome,
            specialty: 'Não informada',
            phone: barber.telefone,
            scale: barber.ativo ? 'Ativo' : 'Inativo'
        })));
        setStorageData('clients', clients.map(client => ({
            id: client.id,
            name: client.nome,
            phone: client.telefone,
            email: client.email
        })));

        if (document.getElementById('tableBarbeirosBody')) {
            renderAdminTables();
            renderKanban();
        }
    } catch (error) {
        console.error('Não foi possível carregar os dados da API:', error);
        showToast('API indisponível. Exibindo dados locais.', 'info');
    }
}

document.addEventListener('DOMContentLoaded', () => {
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
        authForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            if (authMode === 'register') {
                const name = document.getElementById('reg-name').value;
                const identifier = document.getElementById('user-identifier').value.trim();

                if (!identifier.includes('@')) {
                    showToast('Informe um e-mail válido para criar a conta.', 'error');
                    return;
                }

                try {
                    const client = await apiRequest('/clientes', {
                        method: 'POST',
                        body: JSON.stringify({
                            nome: name,
                            email: identifier,
                            telefone: '(81) 90000-0000'
                        })
                    });
                    const clients = getStorageData('clients', defaultClients);
                    clients.push({ id: client.id, name: client.nome, phone: client.telefone, email: client.email });
                    setStorageData('clients', clients);
                } catch (error) {
                    console.error('Erro ao cadastrar cliente:', error);
                    showToast('Não foi possível cadastrar o cliente.', 'error');
                    return;
                }
                
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

    loadRemoteData();

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

async function saveBarbeiro(e) {
    e.preventDefault();
    const name = document.getElementById('barName').value;
    const phone = document.getElementById('barPhone').value;

    try {
        await apiRequest('/barbeiros', {
            method: 'POST',
            body: JSON.stringify({ nome: name, telefone: phone, ativo: true })
        });
        await loadRemoteData();
    } catch (error) {
        console.error('Erro ao cadastrar barbeiro:', error);
        showToast('Não foi possível cadastrar o barbeiro.', 'error');
        return;
    }

    closeModal('modalBarbeiro');
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

// Simulação simples do Chat da IA
function handleAiSend() {
    const input = document.getElementById('aiUserInput');
    const box = document.getElementById('aiChatBox');
    if(!input || !box || !input.value.trim()) return;

    box.innerHTML += `<div class="ai-message user">${input.value}</div>`;
    const text = input.value;
    input.value = '';

    setTimeout(() => {
        let reply = "Entendido. Analisei a agenda e todos os horários estão sincronizados perfeitamente.";
        if(text.toLowerCase().includes('horário') || text.toLowerCase().includes('livre')) {
            reply = "Os barbeiros possuem horários livres nos períodos das 08:30 e 10:00 hoje.";
        }
        box.innerHTML += `<div class="ai-message bot">${reply}</div>`;
        box.scrollTop = box.scrollHeight;
    }, 800);
}