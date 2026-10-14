/**
 * Camada de dados.
 * Hoje: localStorage.
 * Futuro: API REST.
 *
 * Para ativar o backend:
 * 1. altere USE_API para true em config.js;
 * 2. defina API_BASE_URL;
 * 3. implemente os endpoints documentados no README.
 */
const Store = (() => {
  const cfg = window.SGS_CONFIG;

  const defaults = {
    services: [
      { id: 1, name: "Corte Cabelo (Fade/Degradê)", price: 45, duration: 30, active: true },
      { id: 2, name: "Barba Terapia Completa", price: 35, duration: 30, active: true },
      { id: 3, name: "Combo (Corte + Barba)", price: 70, duration: 50, active: true },
      { id: 4, name: "Pezinho / Sobrancelha", price: 15, duration: 15, active: true }
    ],
    products: [
      { id: 101, name: "Pomada Modeladora Matte", price: 35, stock: 10, active: true },
      { id: 102, name: "Refrigerante Lata", price: 6, stock: 30, active: true },
      { id: 103, name: "Cerveja Long Neck", price: 12, stock: 20, active: true }
    ],
    barbers: [
      { id: 1001, name: "Administrador SGS", phone: "(81) 90000-0000", email: "admin@sgsbarber.com", commission: 50, active: true, serviceIds: [1,2,3,4] },
      { id: 1002, name: "Rafael Santos", phone: "(81) 98888-1002", email: "rafael@sgsbarber.com", commission: 50, active: true, serviceIds: [1,2,3] },
      { id: 1003, name: "Lucas Oliveira", phone: "(81) 97777-1003", email: "lucas@sgsbarber.com", commission: 45, active: true, serviceIds: [1,2,4] }
    ],
    clients: [
      { id: 1, name: "Gabriel Ramos", phone: "(81) 90000-0001", cpf: "000.000.000-01", birthDate: "2000-05-14" },
      { id: 2, name: "Lucas Andrade", phone: "(81) 90000-0002", cpf: "000.000.000-02", birthDate: "1999-11-03" }
    ],
    appointments: [
      { id: 1, clientId: 1, client: "Gabriel Ramos", serviceId: 3, service: "Combo (Corte + Barba)", price: 70, extras: 12, date: cfg.DEFAULT_DATE, time: "09:00", status: "Na Cadeira", barberId: 1001, barber: "Administrador SGS", prefs: ["Silêncio", "Aceita Café"] },
      { id: 2, clientId: 2, client: "Lucas Andrade", serviceId: 1, service: "Corte Cabelo (Fade/Degradê)", price: 45, extras: 0, date: cfg.DEFAULT_DATE, time: "10:00", status: "Em Espera", barberId: 1002, barber: "Rafael Santos", prefs: ["Bate-Papo"] }
    ],
    blocks: [],
    ratings: [],
    notifications: [
      { id: 7001, type: "system", title: "Bem-vindo ao SGS Barber", message: "O painel está pronto para receber novos agendamentos.", read: false, createdAt: new Date().toISOString() }
    ],
    barberSchedules: [],
    barberTimeOff: [],
    businessProfile: {
      name: "SGS Barber", phone: "(81) 99999-9999", whatsapp: "5581999999999", instagram: "@sgsbarber",
      address: "Endereço da barbearia será configurado pelo administrador.", city: "Olinda - PE", description: "Seu visual, seu horário."
    },
    users: [
      { id: 9001, role: "barber", name: "Administrador SGS", email: "admin@sgsbarber.com", phone: "", cpf: "00000000000", password: "123456" },
      { id: 9002, role: "client", name: "Cliente Demo", email: "cliente@sgsbarber.com", phone: "(81) 90000-0000", cpf: "00000000000", password: "123456" }
    ]
  };

  function key(name) { return cfg.STORAGE_PREFIX + name; }
  function seed() {
    Object.entries(defaults).forEach(([name, value]) => {
      if (localStorage.getItem(key(name)) === null) {
        localStorage.setItem(key(name), JSON.stringify(value));
      }
    });

    // Migração simples para instalações antigas: garante uma equipe inicial
    // com mais de um barbeiro sem apagar dados já existentes.
    const existingBarbers = JSON.parse(localStorage.getItem(key("barbers")) || "[]");
    const demoBarbers = defaults.barbers.filter(d => !existingBarbers.some(b => String(b.id) === String(d.id)));
    if (demoBarbers.length) {
      localStorage.setItem(key("barbers"), JSON.stringify([...existingBarbers, ...demoBarbers]));
    }
  }
  seed();

  async function request(path, options = {}) {
    const response = await fetch(`${cfg.API_BASE_URL}${path}`, {
      credentials: "include",
      headers: { "Content-Type": "application/json", ...(options.headers || {}) },
      ...options
    });
    if (!response.ok) {
      const message = await response.text();
      throw new Error(message || `Erro HTTP ${response.status}`);
    }
    if (response.status === 204) return null;
    return response.json();
  }

  return {
    async list(resource, params = {}) {
      if (cfg.USE_API) {
        const qs = new URLSearchParams(params).toString();
        return request(`/api/${resource}${qs ? `?${qs}` : ""}`);
      }
      return JSON.parse(localStorage.getItem(key(resource)) || "[]");
    },
    async get(resource, id) {
      if (cfg.USE_API) return request(`/api/${resource}/${id}`);
      return (await this.list(resource)).find(x => String(x.id) === String(id)) || null;
    },
    async create(resource, data) {
      if (cfg.USE_API) return request(`/api/${resource}`, { method: "POST", body: JSON.stringify(data) });
      const rows = await this.list(resource);
      const item = { id: Date.now(), ...data };
      rows.push(item);
      localStorage.setItem(key(resource), JSON.stringify(rows));
      return item;
    },
    async update(resource, id, data) {
      if (cfg.USE_API) return request(`/api/${resource}/${id}`, { method: "PUT", body: JSON.stringify(data) });
      const rows = await this.list(resource);
      const index = rows.findIndex(x => String(x.id) === String(id));
      if (index < 0) throw new Error("Registro não encontrado.");
      rows[index] = { ...rows[index], ...data };
      localStorage.setItem(key(resource), JSON.stringify(rows));
      return rows[index];
    },
    async remove(resource, id) {
      if (cfg.USE_API) return request(`/api/${resource}/${id}`, { method: "DELETE" });
      const rows = await this.list(resource);
      localStorage.setItem(key(resource), JSON.stringify(rows.filter(x => String(x.id) !== String(id))));
    },
    async login(role, credentials) {
      if (cfg.USE_API) return request("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ role, ...credentials })
      });
      const users = await this.list("users");
      const identifier = String(credentials.identifier || credentials.email || credentials.phone || credentials.cpf || "").trim().toLowerCase();
      const password = String(credentials.password || "");
      const user = users.find(u => u.role === role && [u.email, u.phone, u.cpf].filter(Boolean).some(v => String(v).trim().toLowerCase() === identifier) && u.password === password);
      if (!user) return { ok: false, message: "Dados de acesso inválidos." };
      return { ok: true, role, user: { ...user, password: undefined } };
    },
    async register(role, data) {
      if (cfg.USE_API) return request("/api/auth/register", { method: "POST", body: JSON.stringify({ role, ...data }) });
      const users = await this.list("users");
      const email = String(data.email || "").trim().toLowerCase();
      const phone = String(data.phone || "").trim();
      const cpf = String(data.cpf || "").replace(/\D/g, "");
      if (users.some(u => (email && String(u.email || "").toLowerCase() === email) || (phone && String(u.phone || "") === phone) || (cpf && String(u.cpf || "") === cpf))) {
        return { ok: false, message: "Já existe uma conta com esses dados." };
      }
      const user = await this.create("users", { ...data, role, email, phone, cpf });
      if (role === "barber") {
        const barber = await this.create("barbers", { name: data.name, phone: data.phone, email, commission: 50, active: true, serviceIds: [] });
        user.barberId = barber.id;
        await this.update("users", user.id, { barberId: barber.id });
      }
      if (role === "client") {
        const client = await this.create("clients", { name: data.name, phone: data.phone, cpf, email, birthDate: data.birthDate || "" });
        await this.update("users", user.id, { clientId: client.id });
        user.clientId = client.id;
      }
      return { ok: true, user: { ...user, password: undefined } };
    },
    async dashboard() {
      if (cfg.USE_API) return request("/api/dashboard/summary");
      const appointments = await this.list("appointments");
      const completed = appointments.filter(a => a.status === "Concluído");
      const gross = completed.reduce((s, a) => s + Number(a.price || 0) + Number(a.extras || 0), 0);
      const commission = completed.reduce((s, a) => s + Number(a.price || 0) * .5 + Number(a.extras || 0) * .2, 0);
      return { gross, commission, completed: completed.length };
    }
  };
})();
