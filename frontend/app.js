const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];
const money = n => Number(n || 0).toLocaleString("pt-BR", {style:"currency",currency:"BRL"});
const fmtDate = d => d ? new Date(`${d}T00:00:00`).toLocaleDateString("pt-BR") : "-";

function toast(message) {
  let el = $("#toast");
  if (!el) { el = document.createElement("div"); el.id = "toast"; document.body.appendChild(el); }
  el.className = "toast"; el.textContent = message;
  clearTimeout(window.__toastTimer); window.__toastTimer = setTimeout(() => el.remove(), 3200);
}
function toggleTheme() {
  document.body.classList.toggle("light-theme");
  localStorage.setItem("sgs_theme", document.body.classList.contains("light-theme") ? "light" : "dark");
}
function initTheme() {
  if (localStorage.getItem("sgs_theme") === "light") document.body.classList.add("light-theme");
}
function statusClass(s) {
  return s === "Concluído" ? "success" : s === "No-Show" ? "danger" : s === "Na Cadeira" ? "info" : "warning";
}

async function initLogin() {
  const clientForm = $("#clientLoginForm");
  const barberForm = $("#barberLoginForm");
  const clientRegister = $("#clientRegisterForm");
  const barberRegister = $("#barberRegisterForm");
  $$(".auth-tab").forEach(tab => tab.addEventListener("click", () => {
    const target = tab.dataset.authTab;
    $$(".auth-tab").forEach(t => t.classList.toggle("active", t === tab));
    $$(".auth-form").forEach(form => form.hidden = form.dataset.authPanel !== target);
  }));
  if (clientForm) clientForm.addEventListener("submit", async e => {
    e.preventDefault();
    const result = await Store.login("client", { identifier: $("#phone").value.trim(), password: $("#password").value });
    if (!result.ok) return toast(result.message || "Não foi possível entrar.");
    sessionStorage.setItem("sgs_role", "client"); sessionStorage.setItem("sgs_user", JSON.stringify(result.user));
    location.href = "dashboard-cliente.html";
  });
  if (barberForm) barberForm.addEventListener("submit", async e => {
    e.preventDefault();
    const result = await Store.login("barber", { identifier: $("#cpf").value.trim(), password: $("#password").value });
    if (!result.ok) return toast(result.message || "Não foi possível entrar.");
    sessionStorage.setItem("sgs_role", "barber"); sessionStorage.setItem("sgs_user", JSON.stringify(result.user));
    location.href = "dashboard-barbeiro.html";
  });
  if (clientRegister) clientRegister.addEventListener("submit", async e => {
    e.preventDefault();
    const data = { name: $("#registerClientName").value.trim(), phone: $("#registerClientPhone").value.trim(), email: $("#registerClientEmail").value.trim(), cpf: $("#registerClientCpf").value.replace(/\D/g,""), birthDate: $("#registerClientBirthDate").value, password: $("#registerClientPassword").value };
    const result = await Store.register("client", data);
    if (!result.ok) return toast(result.message || "Não foi possível criar a conta.");
    sessionStorage.setItem("sgs_role", "client"); sessionStorage.setItem("sgs_user", JSON.stringify(result.user));
    location.href = "dashboard-cliente.html";
  });
  if (barberRegister) barberRegister.addEventListener("submit", async e => {
    e.preventDefault();
    const data = { name: $("#registerBarberName").value.trim(), email: $("#registerBarberEmail").value.trim(), phone: $("#registerBarberPhone").value.trim(), cpf: $("#registerBarberCpf").value.replace(/\D/g,""), password: $("#registerBarberPassword").value };
    const result = await Store.register("barber", data);
    if (!result.ok) return toast(result.message || "Não foi possível criar a conta.");
    sessionStorage.setItem("sgs_role", "barber"); sessionStorage.setItem("sgs_user", JSON.stringify(result.user));
    location.href = "dashboard-barbeiro.html";
  });
}

let clientState = { barber:null, service:null, slot:null, prefs:new Set() };
let reportPeriod = "week";
const reportCharts = {};

async function initClient() {
  const [barbers, services, appointments] = await Promise.all([Store.list("barbers"), Store.list("services"), Store.list("appointments")]);
  const user = JSON.parse(sessionStorage.getItem("sgs_user") || "null");
  const date = new Date(); date.setDate(date.getDate()+1);
  $("#bookingDate").value = SGS_CONFIG.DEFAULT_DATE || date.toISOString().slice(0,10);
  if (user) { $("#clientName").value = user.name || ""; $("#clientPhone").value = user.phone || ""; }
  renderClientBarbers(barbers);
  renderClientServices(services);
  renderSlots([]);
  renderClientHistory(appointments);
  $("#bookingDate").addEventListener("change", async () => { clientState.slot = null; $("#selectedTime").textContent="—"; updateClientSummary(); renderSlots(await Store.list("appointments")); });
  $$("#preferences .pref").forEach(b => b.addEventListener("click", () => { b.classList.toggle("selected"); clientState.prefs.has(b.dataset.pref) ? clientState.prefs.delete(b.dataset.pref) : clientState.prefs.add(b.dataset.pref); }));
  $("#bookingForm")?.addEventListener("submit", e => { e.preventDefault(); confirmBooking(); });
  $("#addCalendarBtn")?.addEventListener("click", addBookingToCalendar);
  $("#whatsappBtn")?.addEventListener("click", sendBookingWhatsApp);
  $("#clientName")?.addEventListener("input", async () => renderClientHistory(await Store.list("appointments")));
}
function renderClientBarbers(barbers) {
  const active = barbers.filter(b => b.active !== false);
  $("#barberCount").textContent = `${active.length} profissional(is) disponível(is)`;
  $("#clientBarbers").innerHTML = active.length ? active.map((b,i) => `<button type="button" class="barber-option ${i===0?'selected':''}" data-id="${b.id}"><span class="barber-avatar">${(b.name||"B").split(" ").map(x=>x[0]).slice(0,2).join("").toUpperCase()}</span><span class="barber-copy"><strong>${b.name}</strong><small>${b.specialty || "Profissional da equipe"}</small></span><i class="fa-solid fa-chevron-right"></i></button>`).join("") : `<div class="empty-state"><strong>Nenhum barbeiro disponível</strong><span>A equipe ainda não foi cadastrada.</span></div>`;
  clientState.barber = null;
  clientState.service = null;
  clientState.slot = null;
  updateClientSelection();
  $$("#clientBarbers .barber-option").forEach(el => el.onclick = async () => {
    $$("#clientBarbers .barber-option").forEach(x => x.classList.remove("selected")); el.classList.add("selected");
    clientState.barber = active.find(b => String(b.id) === el.dataset.id) || null;
    clientState.service = null; clientState.slot = null;
    renderClientServices(await Store.list("services"));
    renderSlots([]);
    updateClientSelection();
    document.querySelector("#services")?.scrollIntoView({behavior:"smooth",block:"start"});
  });
}
function renderClientServices(services) {
  const barber = clientState.barber;
  const active = services.filter(s => s.active !== false && (!barber || !Array.isArray(barber.serviceIds) || !barber.serviceIds.length || barber.serviceIds.map(String).includes(String(s.id))));
  if (!barber) { $("#clientServices").innerHTML = `<div class="empty-state"><strong>Escolha um barbeiro primeiro</strong><span>Os serviços disponíveis aparecerão aqui.</span></div>`; clientState.service=null; updateClientSummary(); return; }
  $("#clientServices").innerHTML = active.length ? active.map(s => `<button type="button" class="service-option" data-id="${s.id}"><span class="service-icon"><i class="fa-solid fa-scissors"></i></span><span class="service-copy"><strong>${s.name}</strong><small>${s.duration} min</small></span><b>${money(s.price)}</b></button>`).join("") : `<div class="empty-state"><strong>Nenhum serviço disponível</strong><span>Esse profissional ainda não possui serviços vinculados.</span></div>`;
  clientState.service = null;
  updateClientSummary();
  $$("#clientServices .service-option").forEach(el => el.onclick = () => { $$("#clientServices .service-option").forEach(x => x.classList.remove("selected")); el.classList.add("selected"); clientState.service = active.find(s => String(s.id) === el.dataset.id) || null; clientState.slot=null; renderSlots(window.__clientAppointments || []); updateClientSummary(); document.querySelector("#booking")?.scrollIntoView({behavior:"smooth",block:"start"}); });
}
function updateClientSelection() {
  const b=clientState.barber, s=clientState.service;
  if ($("#selectedBarberName")) $("#selectedBarberName").textContent=b?.name||"Selecione um barbeiro";
  if ($("#selectedServiceName")) $("#selectedServiceName").textContent=s?.name||"Selecione um serviço";
  if ($("#summaryBarber")) $("#summaryBarber").textContent=b?.name||"Selecione";
  updateClientSummary();
}
function updateClientSummary() {
  const s=clientState.service; if ($("#summaryService")) $("#summaryService").textContent=s?.name||"Escolha um serviço";
  if ($("#summaryDuration")) $("#summaryDuration").textContent=s?`${s.duration} minutos`:"—";
  if ($("#summaryPrice")) $("#summaryPrice").textContent=money(s?.price||0);
  if ($("#summaryDate")) $("#summaryDate").textContent=$("#bookingDate")?.value ? fmtDate($("#bookingDate").value) : "Selecione";
  if ($("#summarySlot")) $("#summarySlot").textContent=clientState.slot||"—";
  updateClientSelectionText();
}
function updateClientSelectionText(){ const b=clientState.barber,s=clientState.service; if($("#selectedBarberName")) $("#selectedBarberName").textContent=b?.name||"Selecione um barbeiro"; if($("#selectedServiceName")) $("#selectedServiceName").textContent=s?.name||"Selecione um serviço"; if($("#summaryBarber")) $("#summaryBarber").textContent=b?.name||"Selecione"; }
function renderSlots(appointments) {
  window.__clientAppointments = appointments || [];
  const date = $("#bookingDate")?.value; const slots=[]; const barberId=clientState.barber?.id;
  if (!barberId || !clientState.service) { $("#slots").innerHTML=`<div class="empty-state"><strong>Selecione barbeiro e serviço</strong><span>Os horários disponíveis serão mostrados aqui.</span></div>`; return; }
  for(let h=8;h<=18;h++) for(const m of [0,30]) { if(h===18&&m>0) continue; const time=`${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`; const busy=appointments.some(a=>a.date===date&&String(a.barberId)===String(barberId)&&a.time===time&&!['Cancelado','No-Show'].includes(a.status)); slots.push(`<button type="button" class="slot ${busy?'busy':''}" ${busy?'disabled':''} data-time="${time}"><span>${time}</span><small>${busy?'Ocupado':'Livre'}</small></button>`); }
  $("#slots").innerHTML=slots.join("");
  $$("#slots .slot:not(.busy)").forEach(b=>b.onclick=()=>{ $$("#slots .slot").forEach(x=>x.classList.remove("selected")); b.classList.add("selected"); clientState.slot=b.dataset.time; $("#selectedTime").textContent=b.dataset.time; $("#summarySlot").textContent=b.dataset.time; });
}
async function confirmBooking() {
  if(!clientState.barber) return toast("Escolha um barbeiro.");
  if(!clientState.service) return toast("Escolha um serviço.");
  if(!clientState.slot) return toast("Escolha um horário.");
  const user=JSON.parse(sessionStorage.getItem("sgs_user")||"null");
  const name=$("#clientName").value.trim();
  const phone=$("#clientPhone").value.trim();
  if(!name||!phone) return toast("Informe nome e telefone.");
  const date=$("#bookingDate").value;
  const time=clientState.slot;
  const clients=await Store.list("clients");
  let client=clients.find(c=>String(c.id)===String(user?.clientId)||c.name.toLowerCase()===name.toLowerCase()||c.phone===phone);
  if(!client) client=await Store.create("clients",{name,phone,cpf:user?.cpf||"",email:user?.email||"",birthDate:user?.birthDate||""});
  const appointment=await Store.create("appointments",{clientId:client.id,client:name,clientPhone:phone,barberId:clientState.barber.id,barber:clientState.barber.name,serviceId:clientState.service.id,service:clientState.service.name,price:clientState.service.price,extras:0,duration:clientState.service.duration,date,time,status:"Em Espera",prefs:[...clientState.prefs]});
  window.__lastBooking={appointment,client,barber:clientState.barber,service:clientState.service};
  $("#bookingActions")?.removeAttribute("hidden");
  toast("Agendamento confirmado.");
  const apps=await Store.list("appointments");
  renderSlots(apps);
  renderClientHistory(apps);
  updateClientSummary();
}

function calendarDateTime(date,time){
  const [y,m,d]=date.split("-").map(Number);
  const [hh,mm]=time.split(":").map(Number);
  return new Date(y,m-1,d,hh,mm,0);
}
function icsEscape(value){ return String(value||"").replace(/\\/g,"\\\\").replace(/;/g,"\\;").replace(/,/g,"\\,").replace(/\r?\n/g,"\\n"); }
function icsStamp(date){
  const pad=n=>String(n).padStart(2,"0");
  return `${date.getFullYear()}${pad(date.getMonth()+1)}${pad(date.getDate())}T${pad(date.getHours())}${pad(date.getMinutes())}00`;
}
function addBookingToCalendar(){
  const b=window.__lastBooking;
  if(!b?.appointment) return toast("Confirme um agendamento primeiro.");
  const a=b.appointment, start=calendarDateTime(a.date,a.time);
  const end=new Date(start.getTime()+Number(a.duration||b.service?.duration||30)*60000);
  const title=`${a.service} - SGS Barber`;
  const description=`Barbeiro: ${a.barber}\nServiço: ${a.service}\nCliente: ${a.client}`;
  const ics=["BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//SGS Barber//Agendamento//PT-BR","BEGIN:VEVENT",`UID:sgs-${a.id}@sgsbarber`,`DTSTART:${icsStamp(start)}`,`DTEND:${icsStamp(end)}`,`SUMMARY:${icsEscape(title)}`,`DESCRIPTION:${icsEscape(description)}`,"END:VEVENT","END:VCALENDAR"].join("\r\n");
  const blob=new Blob([ics],{type:"text/calendar;charset=utf-8"});
  const url=URL.createObjectURL(blob); const link=document.createElement("a"); link.href=url; link.download=`sgs-barber-${a.date}-${a.time.replace(":","h")}.ics`; document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url);
  toast("Arquivo de calendário criado.");
}
function sendBookingWhatsApp(){
  const b=window.__lastBooking;
  if(!b?.appointment) return toast("Confirme um agendamento primeiro.");
  const a=b.appointment;
  let phone=String(a.clientPhone||b.client?.phone||"").replace(/\D/g,"");
  if(phone.length===10||phone.length===11) phone="55"+phone;
  const text=`Lembrete do meu agendamento na SGS Barber\n\nBarbeiro: ${a.barber}\nServiço: ${a.service}\nData: ${fmtDate(a.date)}\nHorário: ${a.time}\n\nNão esqueça do meu horário.`;
  const url=phone?`https://wa.me/${phone}?text=${encodeURIComponent(text)}`:`https://wa.me/?text=${encodeURIComponent(text)}`;
  window.open(url,"_blank","noopener,noreferrer");
}
function renderClientHistory(apps) {
  const user=JSON.parse(sessionStorage.getItem("sgs_user")||"null"); const name=$("#clientName")?.value.trim(); const rows=apps.filter(a=>(user?.clientId&&String(a.clientId)===String(user.clientId))||(name&&a.client.toLowerCase()===name.toLowerCase()));
  $("#clientHistory").innerHTML=rows.length?rows.sort((a,b)=>b.date.localeCompare(a.date)||b.time.localeCompare(a.time)).map(a=>`<article class="history-item"><div class="history-date"><strong>${a.time}</strong><small>${fmtDate(a.date)}</small></div><div><strong>${a.service}</strong><small>${a.barber||"Barbeiro não informado"}</small></div><div class="history-total"><strong>${money(Number(a.price)+Number(a.extras||0))}</strong><span class="badge ${statusClass(a.status)}">${a.status}</span></div></article>`).join(""):`<div class="empty-state"><i class="fa-regular fa-calendar"></i><strong>Seu histórico aparecerá aqui</strong><span>Agende seu primeiro atendimento.</span></div>`;
}

async function initBarber() {
  const currentUser = JSON.parse(sessionStorage.getItem("sgs_user") || "null");
  if ($("#barberGreeting")) $("#barberGreeting").textContent = currentUser?.name?.split(" ")[0] || "barbeiro";
  const date = SGS_CONFIG.DEFAULT_DATE;
  $("#agendaDate").value = date; $("#fullAgendaDate").value = date; $("#blockDate").value = date;
  $$(".nav button[data-section], .mobile-nav button[data-section]").forEach(btn => btn.addEventListener("click", () => switchSection(btn.dataset.section)));
  $("#agendaDate").addEventListener("change", renderBarber);
  $("#fullAgendaDate").addEventListener("change", renderFullAgenda);
  $("#quickForm").addEventListener("submit", createQuickAppointment);
  $("#blockForm").addEventListener("submit", createBlock);
  $("#comandaForm").addEventListener("submit", addComanda);
  $("#serviceForm").addEventListener("submit", createService);
  $$(".period-btn").forEach(btn => btn.addEventListener("click", () => { reportPeriod = btn.dataset.period; $$(".period-btn").forEach(x => x.classList.toggle("active", x === btn)); renderReports(); }));
  $("#productForm").addEventListener("submit", createProduct);
  $("#barberForm")?.addEventListener("submit", createBarber);
  await renderBarber();
  await renderServices();
  await renderProducts();
  await renderClients();
  await renderBarbers();
  await renderFullAgenda();
}
function switchSection(section) {
  $$(".section").forEach(s => s.classList.toggle("active", s.id === `section-${section}`));
  $$(".nav button, .mobile-nav button").forEach(b => b.classList.toggle("active", b.dataset.section === section));
  const names={overview:"Dashboard",agenda:"Agenda",services:"Serviços",products:"Produtos",clients:"Clientes",barbers:"Barbeiros",reports:"Relatórios",ai:"Assistente IA"};
  $("#pageTitle").textContent=names[section] || "Dashboard";
  if(section==="reports") renderReports();
}
async function renderBarber() {
  const [summary, apps, blocks, services, products] = await Promise.all([
    Store.dashboard(), Store.list("appointments"), Store.list("blocks"), Store.list("services"), Store.list("products")
  ]);
  $("#gross").textContent=money(summary.gross); $("#commission").textContent=money(summary.commission); $("#completed").textContent=summary.completed;
  $("#todayAgenda").innerHTML = buildAgendaRows(apps.filter(a=>a.date===$("#agendaDate").value), blocks.filter(b=>b.date===$("#agendaDate").value));
  $("#quickService").innerHTML=services.filter(s=>s.active!==false).map(s=>`<option value="${s.id}">${s.name} — ${money(s.price)}</option>`).join("");
  $("#comandaAppointment").innerHTML=apps.filter(a=>!["Concluído","Cancelado"].includes(a.status)).map(a=>`<option value="${a.id}">${a.client} (${a.time})</option>`).join("");
  $("#comandaProduct").innerHTML=products.filter(p=>p.active!==false&&p.stock>0).map(p=>`<option value="${p.id}">${p.name} — ${money(p.price)}</option>`).join("");
}
function buildAgendaRows(apps, blocks=[]) {
  const rows=apps.map(a=>`<tr><td><strong>${a.time}</strong></td><td>${a.client}<br><small>${(a.prefs||[]).join(" · ")}</small></td><td>${a.service}</td><td>${money(Number(a.price)+Number(a.extras||0))}</td><td><span class="badge ${statusClass(a.status)}">${a.status}</span></td><td><button class="btn btn-info" onclick="setStatus(${a.id},'Na Cadeira')">Cadeira</button> <button class="btn btn-primary" onclick="setStatus(${a.id},'Concluído')">Concluir</button> <button class="btn btn-danger" onclick="setStatus(${a.id},'No-Show')">Faltou</button></td></tr>`);
  const blocked=blocks.map(b=>`<tr><td>${b.time}</td><td colspan="4" style="color:var(--danger)">Bloqueado — ${b.reason||"Indisponível"}</td><td><button class="btn btn-danger" onclick="removeBlock(${b.id})">Remover</button></td></tr>`);
  return [...rows,...blocked].join("") || `<tr><td colspan="6" class="empty">Nenhum registro para esta data.</td></tr>`;
}
async function setStatus(id,status){await Store.update("appointments",id,{status});toast("Status atualizado.");await renderBarber();await renderFullAgenda();await renderClients();}
async function createQuickAppointment(e){e.preventDefault();const s=await Store.get("services",$("#quickService").value);const current=JSON.parse(sessionStorage.getItem("sgs_user")||"null"); const profile=(await Store.list("barbers")).find(b=>String(b.id)===String(current?.barberId)); await Store.create("appointments",{clientId:null,client:$("#quickName").value.trim(),barberId:profile?.id||null,barber:profile?.name||current?.name||"",serviceId:s.id,service:s.name,price:s.price,extras:0,date:$("#agendaDate").value,time:$("#quickTime").value,status:"Na Cadeira",prefs:["Cliente de Balcão"]});toast("Encaixe adicionado.");e.target.reset();hidePanels();await renderBarber();await renderFullAgenda();}
async function createBlock(e){e.preventDefault();await Store.create("blocks",{date:$("#blockDate").value,time:$("#blockTime").value,reason:$("#blockReason").value.trim()||"Pausa / Intervalo"});toast("Horário bloqueado.");e.target.reset();hidePanels();await renderBarber();}
async function removeBlock(id){await Store.remove("blocks",id);toast("Bloqueio removido.");await renderBarber();}
async function addComanda(e){e.preventDefault();const id=$("#comandaAppointment").value, p=await Store.get("products",$("#comandaProduct").value), a=await Store.get("appointments",id);if(!p||!a)return;await Store.update("appointments",id,{extras:Number(a.extras||0)+Number(p.price)});if(p.stock>0)await Store.update("products",p.id,{stock:p.stock-1});toast("Produto lançado na comanda.");await renderBarber();await renderProducts();}
async function createService(e){e.preventDefault();await Store.create("services",{name:$("#serviceName").value.trim(),price:Number($("#servicePrice").value),duration:Number($("#serviceDuration").value),active:true});toast("Serviço cadastrado.");e.target.reset();await renderServices();await renderBarber();}
async function removeService(id){await Store.update("services",id,{active:false});toast("Serviço desativado.");await renderServices();await renderBarber();}
async function renderServices(){const rows=await Store.list("services");$("#servicesTable").innerHTML=rows.map(s=>`<tr><td>${s.name}</td><td>${money(s.price)}</td><td>${s.duration} min</td><td>${s.active!==false?`<button class="btn btn-danger" onclick="removeService(${s.id})">Desativar</button>`:`<span class="badge danger">Inativo</span>`}</td></tr>`).join("")}
async function createProduct(e){e.preventDefault();await Store.create("products",{name:$("#productName").value.trim(),price:Number($("#productPrice").value),stock:Number($("#productStock").value||0),active:true});toast("Produto cadastrado.");e.target.reset();await renderProducts();await renderBarber();}
async function removeProduct(id){await Store.update("products",id,{active:false});toast("Produto desativado.");await renderProducts();await renderBarber();}
async function renderProducts(){const rows=await Store.list("products");$("#productsTable").innerHTML=rows.map(p=>`<tr><td>${p.name}</td><td>${money(p.price)}</td><td>${p.stock}</td><td>${p.active!==false?`<button class="btn btn-danger" onclick="removeProduct(${p.id})">Desativar</button>`:`<span class="badge danger">Inativo</span>`}</td></tr>`).join("")}
async function createBarber(e){e.preventDefault();const data={name:$("#barberName").value.trim(),phone:$("#barberPhone").value.trim(),email:$("#barberEmail").value.trim().toLowerCase(),commission:Number($("#barberCommission").value||50),active:true,serviceIds:[]};if(!data.name)return;await Store.create("barbers",data);toast("Barbeiro cadastrado.");e.target.reset();$("#barberCommission").value=50;await renderBarbers();}
async function toggleBarber(id,active){await Store.update("barbers",id,{active});toast(active?"Barbeiro ativado.":"Barbeiro desativado.");await renderBarbers();}
async function renderBarbers(){const rows=await Store.list("barbers");$("#barbersTable").innerHTML=rows.map(b=>`<tr><td><strong>${b.name}</strong></td><td>${b.phone||"-"}<br><small>${b.email||"-"}</small></td><td>${Number(b.commission||0)}%</td><td><span class="badge ${b.active!==false?"success":"danger"}">${b.active!==false?"Ativo":"Inativo"}</span></td><td><button class="btn ${b.active!==false?"btn-danger":"btn-primary"}" onclick="toggleBarber(${b.id},${b.active===false})">${b.active!==false?"Desativar":"Ativar"}</button></td></tr>`).join("")||`<tr><td colspan="5" class="empty">Nenhum barbeiro cadastrado.</td></tr>`;}
async function renderClients(){const [clients,apps]=await Promise.all([Store.list("clients"),Store.list("appointments")]);$("#clientsTable").innerHTML=clients.map(c=>{const mine=apps.filter(a=>a.clientId===c.id||a.client===c.name);const completed=mine.filter(a=>a.status==="Concluído");const last=completed.sort((a,b)=>b.date.localeCompare(a.date)||b.time.localeCompare(a.time))[0];return `<tr><td><strong>${c.name}</strong></td><td>${c.phone||"-"}</td><td>${c.birthDate?fmtDate(c.birthDate):"-"}</td><td>${completed.length}</td><td>${last?fmtDate(last.date):"-"}</td></tr>`}).join("")||`<tr><td colspan="5" class="empty">Nenhum cliente.</td></tr>`}
async function renderFullAgenda(){const apps=await Store.list("appointments"),date=$("#fullAgendaDate")?.value;const rows=apps.filter(a=>!date||a.date===date).sort((a,b)=>a.time.localeCompare(b.time));$("#fullAgenda").innerHTML=rows.map(a=>`<tr><td>${fmtDate(a.date)}</td><td>${a.time}</td><td>${a.client}</td><td>${a.service}</td><td><span class="badge ${statusClass(a.status)}">${a.status}</span></td><td>${money(Number(a.price)+Number(a.extras||0))}</td></tr>`).join("")||`<tr><td colspan="6" class="empty">Nenhum agendamento.</td></tr>`}
function startOfWeek(date) {
  const d = new Date(date);
  const day = d.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  d.setHours(0,0,0,0);
  return d;
}
function isoDate(date) { return date.toISOString().slice(0,10); }
function addDays(date, days) { const d = new Date(date); d.setDate(d.getDate()+days); return d; }
function fmtShortDate(date) { return date.toLocaleDateString("pt-BR", {day:"2-digit", month:"2-digit"}); }
function fmtHours(value) { const n = Number(value || 0); const h = Math.floor(n); const m = Math.round((n-h)*60); return `${h}h${m ? ` ${m}min` : ""}`; }
function reportReferenceDate() {
  const value = $("#agendaDate")?.value || SGS_CONFIG.DEFAULT_DATE;
  return new Date(`${value}T12:00:00`);
}
function reportDataForPeriod(appointments, services) {
  const reference = reportReferenceDate();
  const completed = appointments.filter(a => a.status === "Concluído");
  const serviceMap = new Map(services.map(s => [String(s.id), s]));
  const getMetrics = rows => rows.reduce((acc, a) => {
    const service = serviceMap.get(String(a.serviceId));
    acc.hours += Number(service?.duration || a.duration || 0) / 60;
    acc.clients += 1;
    acc.money += Number(a.price || 0) + Number(a.extras || 0);
    return acc;
  }, {hours:0, clients:0, money:0});

  if (reportPeriod === "week") {
    const start = startOfWeek(reference);
    const buckets = Array.from({length:7}, (_, i) => {
      const date = addDays(start, i);
      const key = isoDate(date);
      return {key, label: date.toLocaleDateString("pt-BR", {weekday:"short"}).replace(".", ""), dateLabel:fmtShortDate(date), rows: completed.filter(a => a.date === key)};
    });
    return {buckets, metrics:getMetrics(completed.filter(a => a.date >= isoDate(start) && a.date <= isoDate(addDays(start,6))))};
  }

  const year = reference.getFullYear(), month = reference.getMonth();
  const first = new Date(year, month, 1, 12), last = new Date(year, month+1, 0, 12);
  const buckets = [];
  let cursor = startOfWeek(first);
  let index = 1;
  while (cursor <= last) {
    const weekStart = new Date(cursor), weekEnd = addDays(weekStart,6);
    const from = weekStart < first ? first : weekStart;
    const to = weekEnd > last ? last : weekEnd;
    const rows = completed.filter(a => a.date >= isoDate(from) && a.date <= isoDate(to));
    buckets.push({key:`w${index}`, label:`Sem ${index}`, dateLabel:`${fmtShortDate(from)}–${fmtShortDate(to)}`, rows});
    cursor = addDays(cursor,7); index++;
  }
  const monthRows = completed.filter(a => a.date >= isoDate(first) && a.date <= isoDate(last));
  return {buckets, metrics:getMetrics(monthRows)};
}
function renderChart(id, type, labels, data, label, prefix="", integer=false) {
  if (typeof Chart === "undefined") return;
  if (reportCharts[id]) reportCharts[id].destroy();
  const ctx = document.getElementById(id); if (!ctx) return;
  const styles = getComputedStyle(document.body);
  const primary = styles.getPropertyValue("--primary").trim() || "#f59e0b";
  const muted = styles.getPropertyValue("--muted").trim() || "#94a3b8";
  reportCharts[id] = new Chart(ctx, {
    type,
    data:{labels,datasets:[{label,data,fill:type==="line",borderWidth:2,borderRadius:type==="bar"?8:0,tension:.35,backgroundColor:"rgba(245,158,11,.72)",borderColor:primary,pointBackgroundColor:primary,pointRadius:3}]},
    options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:false},tooltip:{callbacks:{label:function(c){const value=integer?Number(c.raw||0).toLocaleString("pt-BR"):prefix+Number(c.raw||0).toLocaleString("pt-BR",{minimumFractionDigits:2});return label+": "+value;}}}},scales:{x:{grid:{display:false},ticks:{color:muted}},y:{beginAtZero:true,grid:{color:"rgba(148,163,184,.12)"},ticks:{color:muted,callback:function(v){return integer?Number(v).toLocaleString("pt-BR"):prefix+Number(v).toLocaleString("pt-BR");}}}}}
  });
}
async function renderReports(){
  const [appointments, services] = await Promise.all([Store.list("appointments"), Store.list("services")]);
  const {buckets, metrics} = reportDataForPeriod(appointments, services);
  $("#reportHours").textContent = fmtHours(metrics.hours);
  $("#reportClients").textContent = metrics.clients.toLocaleString("pt-BR");
  $("#reportMoney").textContent = money(metrics.money);
  $("#reportTicket").textContent = money(metrics.clients ? metrics.money/metrics.clients : 0);
  $("#reportAvgHours").textContent = fmtHours(metrics.clients ? metrics.hours/metrics.clients : 0);
  $("#reportCommission").textContent = money(appointments.filter(a=>a.status==="Concluído").filter(a=>{
    const r=reportReferenceDate(), d=new Date(`${a.date}T12:00:00`); return reportPeriod==="week" ? d>=startOfWeek(r)&&d<=addDays(startOfWeek(r),6) : d.getFullYear()===r.getFullYear()&&d.getMonth()===r.getMonth();
  }).reduce((sum,a)=>sum+Number(a.price||0)*.5+Number(a.extras||0)*.2,0));
  const labels=buckets.map(b=>b.label), hourData=buckets.map(b=>reportDataForRows(b.rows,services).hours), clientData=buckets.map(b=>b.rows.length), moneyData=buckets.map(b=>reportDataForRows(b.rows,services).money);
  renderChart("hoursChart","bar",labels,hourData,"Horas","",false);
  renderChart("clientsChart","bar",labels,clientData,"Clientes","",true);
  renderChart("moneyChart","line",labels,moneyData,"Receita","R$ ",false);
  $("#reportHoursNote").textContent = reportPeriod === "week" ? "segunda a domingo" : "mês selecionado";
  $("#reportClientsNote").textContent = reportPeriod === "week" ? "concluídos na semana" : "concluídos no mês";
  $("#reportMoneyNote").textContent = reportPeriod === "week" ? "recebidos na semana" : "recebidos no mês";
}
function reportDataForRows(rows, services) {
  const map=new Map(services.map(s=>[String(s.id),s]));
  return rows.reduce((a,x)=>{a.hours+=Number(map.get(String(x.serviceId))?.duration||x.duration||0)/60;a.money+=Number(x.price||0)+Number(x.extras||0);return a},{hours:0,money:0});
}

/* =========================================================
   SGS BARBER — CAMADA FRONT-END APRIMORADA
   Tudo abaixo continua usando Store. Nenhuma regra depende de
   banco ou servidor: quando USE_API=true, a mesma interface
   passa a consumir os endpoints do backend.
========================================================= */
function initials(name){ return String(name||"SB").split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join("").toUpperCase(); }
function esc(value){ return String(value??"").replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#039;"}[m])); }
function sameDate(a,b){ return String(a||"")===String(b||""); }
function daysUntilBirthday(dateValue){
  if(!dateValue) return 9999;
  const birth=new Date(`${dateValue}T12:00:00`), now=new Date();
  const target=new Date(now.getFullYear(),birth.getMonth(),birth.getDate(),12);
  if(target < new Date(now.getFullYear(),now.getMonth(),now.getDate(),12)) target.setFullYear(now.getFullYear()+1);
  return Math.round((target-new Date(now.getFullYear(),now.getMonth(),now.getDate(),12))/86400000);
}
function savedSettings(){
  try{return JSON.parse(localStorage.getItem(`${SGS_CONFIG.STORAGE_PREFIX}settings`)||"null")||{open:"08:00",close:"20:00",interval:30,days:[1,2,3,4,5,6]};}
  catch{return {open:"08:00",close:"20:00",interval:30,days:[1,2,3,4,5,6]};}
}
function timeToMinutes(v){const [h,m]=String(v||"00:00").split(":").map(Number);return h*60+m;}
function minutesToTime(v){return `${String(Math.floor(v/60)).padStart(2,"0")}:${String(v%60).padStart(2,"0")}`;}

function renderBarberServiceChecks(selected=[]){
  const el=$("#barberServiceChecks"); if(!el) return;
  Store.list("services").then(rows=>{
    el.innerHTML=rows.filter(s=>s.active!==false).map(s=>`<label class="check-option"><input type="checkbox" name="barberService" value="${s.id}" ${selected.map(String).includes(String(s.id))?"checked":""}> ${esc(s.name)}</label>`).join("")||'<span class="muted">Cadastre serviços primeiro.</span>';
  });
}

async function createBarber(e){
  e.preventDefault();
  const serviceIds=$$("input[name='barberService']:checked").map(x=>Number(x.value));
  const data={name:$("#barberName").value.trim(),phone:$("#barberPhone").value.trim(),email:$("#barberEmail").value.trim().toLowerCase(),commission:Number($("#barberCommission").value||50),active:true,serviceIds};
  if(!data.name)return toast("Informe o nome do profissional.");
  await Store.create("barbers",data); toast("Barbeiro cadastrado."); e.target.reset(); $("#barberCommission").value=50; renderBarberServiceChecks(); await renderBarbers();
}
async function renderBarbers(){
  const [rows,services]=await Promise.all([Store.list("barbers"),Store.list("services")]);
  const map=new Map(services.map(s=>[String(s.id),s.name]));
  $("#barbersTable").innerHTML=rows.map(b=>{
    const names=(b.serviceIds||[]).map(id=>map.get(String(id))).filter(Boolean);
    return `<tr><td><div class="person-cell"><span class="person-avatar">${initials(b.name)}</span><strong>${esc(b.name)}</strong></div></td><td>${names.length?esc(names.join(", ")):"Todos / não definidos"}</td><td>${esc(b.phone||"-")}<br><small>${esc(b.email||"-")}</small></td><td>${Number(b.commission||0)}%</td><td><span class="badge ${b.active!==false?"success":"danger"}">${b.active!==false?"Ativo":"Inativo"}</span></td><td><button class="btn ${b.active!==false?"btn-danger":"btn-primary"}" onclick="toggleBarber(${b.id},${b.active===false})">${b.active!==false?"Desativar":"Ativar"}</button></td></tr>`;
  }).join("")||`<tr><td colspan="6" class="empty">Nenhum barbeiro cadastrado.</td></tr>`;
  renderBarberServiceChecks();
}

async function renderAgendaFilters(){
  const barbers=await Store.list("barbers");
  const select=$("#agendaBarberFilter");
  if(select) select.innerHTML='<option value="all">Todos os barbeiros</option>'+barbers.map(b=>`<option value="${b.id}">${esc(b.name)}</option>`).join("");
  const wait=$("#waitBarber");
  if(wait) wait.innerHTML='<option value="">Qualquer barbeiro</option>'+barbers.filter(b=>b.active!==false).map(b=>`<option value="${b.id}">${esc(b.name)}</option>`).join("");
}
function appointmentStatusOptions(a){
  const statuses=["Agendado","Confirmado","Na Cadeira","Concluído","Cancelado","No-Show"];
  return statuses.map(s=>`<button type="button" class="status-menu-item ${a.status===s?"active":""}" onclick="setStatus(${a.id},'${s}')">${s}</button>`).join("");
}
function appointmentCard(a){
  const total=Number(a.price||0)+Number(a.extras||0);
  return `<article class="agenda-appointment"><div class="agenda-time">${esc(a.time)}<small>${Number(a.duration||30)} min</small></div><div class="agenda-client"><div class="person-avatar">${initials(a.client)}</div><div><strong>${esc(a.client)}</strong><small>${esc(a.service)}</small></div></div><div class="agenda-professional"><span>Barbeiro</span><strong>${esc(a.barber||"-")}</strong></div><div class="agenda-price">${money(total)}</div><div class="agenda-status"><span class="badge ${statusClass(a.status)}">${esc(a.status)}</span><div class="status-menu">${appointmentStatusOptions(a)}</div></div></article>`;
}
async function renderVisualAgenda(){
  const date=$("#fullAgendaDate")?.value || $("#agendaDate")?.value;
  const filter=$("#agendaBarberFilter")?.value || "all";
  const [apps,blocks]=await Promise.all([Store.list("appointments"),Store.list("blocks")]);
  const rows=apps.filter(a=>a.date===date&&(filter==="all"||String(a.barberId)===String(filter))).sort((a,b)=>a.time.localeCompare(b.time));
  const board=$("#visualAgenda"); if(!board)return;
  const byBarber={}; rows.forEach(a=>{const key=a.barber||"Sem barbeiro";(byBarber[key]??=[]).push(a);});
  const blockRows=blocks.filter(b=>b.date===date);
  board.innerHTML=Object.keys(byBarber).length?Object.entries(byBarber).map(([barber,list])=>`<section class="agenda-column"><header><div class="person-avatar">${initials(barber)}</div><div><strong>${esc(barber)}</strong><small>${list.length} atendimento(s)</small></div></header>${list.map(appointmentCard).join("")}</section>`).join(""):`<div class="empty-state"><i class="fa-regular fa-calendar-xmark"></i><strong>Nenhum atendimento nesta data</strong><span>Quando houver reservas, elas aparecerão separadas por profissional.</span></div>`;
  if(blockRows.length) board.insertAdjacentHTML("beforeend",`<section class="agenda-column blocked-column"><header><div class="person-avatar"><i class="fa-solid fa-ban"></i></div><div><strong>Horários bloqueados</strong><small>${blockRows.length} bloqueio(s)</small></div></header>${blockRows.map(b=>`<article class="agenda-appointment blocked"><div class="agenda-time">${esc(b.time)}</div><div class="agenda-client"><div><strong>Indisponível</strong><small>${esc(b.reason||"Bloqueio")}</small></div></div></article>`).join("")}</section>`);
}
async function renderFullAgenda(){
  const apps=await Store.list("appointments"),date=$("#fullAgendaDate")?.value,filter=$("#agendaBarberFilter")?.value||"all";
  const rows=apps.filter(a=>(!date||a.date===date)&&(filter==="all"||String(a.barberId)===String(filter))).sort((a,b)=>a.time.localeCompare(b.time));
  const el=$("#fullAgenda"); if(el)el.innerHTML=rows.map(a=>`<tr><td>${fmtDate(a.date)}</td><td><strong>${esc(a.time)}</strong></td><td>${esc(a.client)}</td><td>${esc(a.service)}</td><td>${esc(a.barber||"-")}</td><td><span class="badge ${statusClass(a.status)}">${esc(a.status)}</span></td><td>${money(Number(a.price||0)+Number(a.extras||0))}</td></tr>`).join("")||`<tr><td colspan="7" class="empty">Nenhum agendamento.</td></tr>`;
  await renderVisualAgenda();
}
async function renderOccupancy(){
  const date=$("#agendaDate")?.value;if(!date||!$("#occupancy"))return;
  const settings=savedSettings(),apps=await Store.list("appointments"),barbers=(await Store.list("barbers")).filter(b=>b.active!==false);
  const totalSlots=Math.max(1,Math.floor((timeToMinutes(settings.close)-timeToMinutes(settings.open))/Number(settings.interval||30))*Math.max(1,barbers.length));
  const busy=apps.filter(a=>a.date===date&&!['Cancelado','No-Show'].includes(a.status)).length;
  $("#occupancy").textContent=Math.min(100,Math.round(busy/totalSlots*100))+"%";
}
async function renderBarber(){
  const [summary,apps,blocks,services,products]=await Promise.all([Store.dashboard(),Store.list("appointments"),Store.list("blocks"),Store.list("services"),Store.list("products")]);
  $("#gross").textContent=money(summary.gross);$("#commission").textContent=money(summary.commission);$("#completed").textContent=summary.completed;
  const date=$("#agendaDate")?.value;
  $("#todayAgenda").innerHTML=buildAgendaRows(apps.filter(a=>a.date===date),blocks.filter(b=>b.date===date));
  $("#quickService").innerHTML=services.filter(s=>s.active!==false).map(s=>`<option value="${s.id}">${esc(s.name)} — ${money(s.price)}</option>`).join("");
  $("#comandaAppointment").innerHTML=apps.filter(a=>!['Concluído','Cancelado','No-Show'].includes(a.status)).map(a=>`<option value="${a.id}">${esc(a.client)} (${a.time})</option>`).join("");
  $("#comandaProduct").innerHTML=products.filter(p=>p.active!==false&&Number(p.stock)>0).map(p=>`<option value="${p.id}">${esc(p.name)} — ${money(p.price)}</option>`).join("");
  await renderOccupancy(); await renderVisualAgenda();
}

async function createWaitlist(e){
  e.preventDefault();
  await Store.create("waitlist",{client:$("#waitClient").value.trim(),phone:$("#waitPhone").value.trim(),date:$("#waitDate").value,barberId:$("#waitBarber").value||null,status:"Aguardando",createdAt:new Date().toISOString()});
  toast("Cliente adicionado à lista de espera.");e.target.reset();await renderWaitlist();
}
async function renderWaitlist(){
  const [rows,barbers]=await Promise.all([Store.list("waitlist"),Store.list("barbers")]);const map=new Map(barbers.map(b=>[String(b.id),b.name]));const el=$("#waitlistTable");if(!el)return;
  el.innerHTML=rows.filter(x=>x.status!=="Atendido").sort((a,b)=>String(a.date).localeCompare(String(b.date))).map(x=>`<tr><td><strong>${esc(x.client)}</strong><br><small>${esc(x.phone||"-")}</small></td><td>${fmtDate(x.date)}</td><td>${esc(map.get(String(x.barberId))||"Qualquer")}</td><td><span class="badge warning">${esc(x.status)}</span></td><td><button class="btn btn-primary" onclick="promoteWaitlist(${x.id})">Atender</button></td></tr>`).join("")||`<tr><td colspan="5" class="empty">Nenhum cliente aguardando.</td></tr>`;
}
async function promoteWaitlist(id){await Store.update("waitlist",id,{status:"Atendido"});toast("Cliente retirado da lista de espera.");await renderWaitlist();}

async function renderClients(){
  const [clients,apps]=await Promise.all([Store.list("clients"),Store.list("appointments")]);const search=($("#clientSearch")?.value||"").trim().toLowerCase();
  const filtered=clients.filter(c=>!search||`${c.name} ${c.phone} ${c.email||""}`.toLowerCase().includes(search));
  $("#clientsTable").innerHTML=filtered.map(c=>{const mine=apps.filter(a=>String(a.clientId)===String(c.id)||a.client===c.name);const completed=mine.filter(a=>a.status==="Concluído");const last=[...completed].sort((a,b)=>`${b.date} ${b.time}`.localeCompare(`${a.date} ${a.time}`))[0];return `<tr><td><div class="person-cell"><span class="person-avatar">${initials(c.name)}</span><div><strong>${esc(c.name)}</strong><small>${esc(c.email||"")}</small></div></div></td><td>${esc(c.phone||"-")}</td><td>${c.birthDate?fmtDate(c.birthDate):"-"}</td><td>${completed.length}</td><td>${last?fmtDate(last.date):"-"}</td><td>${c.phone?`<button class="btn btn-secondary" onclick="openClientWhatsApp('${String(c.phone).replace(/\D/g,"")}','${esc(c.name)}')"><i class="fa-brands fa-whatsapp"></i></button>`:""}</td></tr>`}).join("")||`<tr><td colspan="6" class="empty">Nenhum cliente encontrado.</td></tr>`;
  renderBirthdayBanner(clients);
}
function openClientWhatsApp(phone,name){let p=String(phone||"").replace(/\D/g,"");if(p.length===10||p.length===11)p="55"+p;window.open(`https://wa.me/${p}?text=${encodeURIComponent(`Olá, ${name}. Aqui é da SGS Barber. Podemos ajudar com seu próximo horário?`)}`,"_blank","noopener,noreferrer");}
function renderBirthdayBanner(clients){
  const el=$("#birthdayBanner");if(!el)return;const upcoming=clients.map(c=>({...c,days:daysUntilBirthday(c.birthDate)})).filter(c=>c.days<=30).sort((a,b)=>a.days-b.days);
  el.innerHTML=upcoming.length?`<div class="birthday-inner"><div><i class="fa-solid fa-cake-candles"></i><strong>Próximos aniversários</strong><small>${upcoming.slice(0,4).map(c=>`${esc(c.name)} — ${c.days===0?"hoje":`em ${c.days} dia(s)`}`).join(" · ")}</small></div><span class="badge warning">${upcoming.length} próximo(s)</span></div>`:`<div class="birthday-inner muted"><div><i class="fa-regular fa-calendar"></i><strong>Nenhum aniversário nos próximos 30 dias</strong></div></div>`;
}

async function saveSettings(e){
  e.preventDefault();const days=$$("input[name='day']:checked").map(x=>Number(x.value));const value={open:$("#settingsOpen").value,close:$("#settingsClose").value,interval:Number($("#settingsInterval").value),days};localStorage.setItem(`${SGS_CONFIG.STORAGE_PREFIX}settings`,JSON.stringify(value));toast("Configurações salvas no frontend.");await renderOccupancy();}
function loadSettings(){const s=savedSettings();if($("#settingsOpen"))$("#settingsOpen").value=s.open;if($("#settingsClose"))$("#settingsClose").value=s.close;if($("#settingsInterval"))$("#settingsInterval").value=s.interval;$$("input[name='day']").forEach(x=>x.checked=(s.days||[]).includes(Number(x.value)));}

async function renderPublicHome(){
  const [services,barbers]=await Promise.all([Store.list("services"),Store.list("barbers")]);
  const se=$("#publicServices"),te=$("#publicTeam");if(!se||!te)return;
  se.innerHTML=services.filter(s=>s.active!==false).map(s=>`<article class="public-service"><div class="public-service-icon"><i class="fa-solid fa-scissors"></i></div><div><strong>${esc(s.name)}</strong><small>${Number(s.duration||0)} minutos</small></div><b>${money(s.price)}</b></article>`).join("");
  te.innerHTML=barbers.filter(b=>b.active!==false).map(b=>`<article class="public-person"><div class="public-person-avatar">${initials(b.name)}</div><div><strong>${esc(b.name)}</strong><small>${esc(b.specialty||"Profissional da equipe")}</small></div><a href="dashboard-cliente.html" class="icon-btn" title="Agendar"><i class="fa-solid fa-arrow-right"></i></a></article>`).join("");
}

function updateClientProgress(){
  const steps=[!!clientState.barber,!!clientState.service,!!clientState.slot];const nums=$$(".booking-progress span");const next=steps.findIndex(x=>!x);const activeUntil=next===-1?3:next;nums.forEach((n,i)=>n.classList.toggle("active",i<=activeUntil));}

function renderClientBarbers(barbers){
  const active=barbers.filter(b=>b.active!==false);$("#barberCount").textContent=`${active.length} profissional(is) disponível(is)`;
  $("#clientBarbers").innerHTML=active.length?active.map(b=>`<button type="button" class="barber-option" data-id="${b.id}"><span class="barber-avatar">${b.photoUrl?`<img src="${esc(b.photoUrl)}" alt="Foto de ${esc(b.name)}">`:initials(b.name)}</span><span class="barber-copy"><strong>${esc(b.name)}</strong><small>${esc(b.specialty||"Profissional da equipe")}</small></span><i class="fa-solid fa-chevron-right"></i></button>`).join(""):`<div class="empty-state"><strong>Nenhum barbeiro disponível</strong><span>A equipe ainda não foi cadastrada.</span></div>`;
  clientState.barber=null;clientState.service=null;clientState.slot=null;updateClientSelection();renderSlots([]);updateClientProgress();
  $$("#clientBarbers .barber-option").forEach(el=>el.onclick=async()=>{ $$("#clientBarbers .barber-option").forEach(x=>x.classList.remove("selected"));el.classList.add("selected");clientState.barber=active.find(b=>String(b.id)===el.dataset.id)||null;clientState.service=null;clientState.slot=null;renderClientServices(await Store.list("services"));renderSlots([]);updateClientSelection();updateClientProgress();$("#services")?.scrollIntoView({behavior:"smooth",block:"start"}); });
}
function renderClientServices(services){
  const barber=clientState.barber;if(!barber){$("#clientServices").innerHTML='<div class="empty-state"><strong>Escolha um barbeiro primeiro</strong><span>Os serviços aparecerão depois da escolha do profissional.</span></div>';clientState.service=null;updateClientSummary();updateClientProgress();return;}
  const active=services.filter(s=>s.active!==false&&(!Array.isArray(barber.serviceIds)||!barber.serviceIds.length||barber.serviceIds.map(String).includes(String(s.id))));
  $("#clientServices").innerHTML=active.length?active.map(s=>`<button type="button" class="service-option" data-id="${s.id}"><span class="service-icon"><i class="fa-solid fa-scissors"></i></span><span class="service-copy"><strong>${esc(s.name)}</strong><small>${Number(s.duration||0)} min</small></span><b>${money(s.price)}</b></button>`).join(""):'<div class="empty-state"><strong>Nenhum serviço disponível</strong><span>Esse profissional ainda não possui serviços vinculados.</span></div>';
  $$("#clientServices .service-option").forEach(el=>el.onclick=()=>{$$("#clientServices .service-option").forEach(x=>x.classList.remove("selected"));el.classList.add("selected");clientState.service=active.find(s=>String(s.id)===el.dataset.id)||null;clientState.slot=null;renderSlots(window.__clientAppointments||[]);updateClientSummary();updateClientProgress();$("#booking")?.scrollIntoView({behavior:"smooth",block:"start"});});
  updateClientProgress();
}
async function renderSlots(appointments){
  window.__clientAppointments=appointments||[];const date=$("#bookingDate")?.value,barberId=clientState.barber?.id,service=clientState.service,el=$("#slots");if(!el)return;
  if(!barberId||!service){el.innerHTML='<div class="empty-state"><strong>Selecione barbeiro e serviço</strong><span>Os horários disponíveis aparecerão aqui.</span></div>';return;}
  const settings=savedSettings();const day=new Date(`${date}T12:00:00`).getDay();if(!(settings.days||[1,2,3,4,5,6]).includes(day)){el.innerHTML='<div class="empty-state"><strong>A barbearia não atende nesta data</strong><span>Escolha outro dia para consultar horários.</span></div>';return;}
  const blocks=await Store.list("blocks");const step=Number(settings.interval||30),start=timeToMinutes(settings.open),close=timeToMinutes(settings.close),duration=Number(service.duration||30),slots=[];
  for(let t=start;t+duration<=close;t+=step){const time=minutesToTime(t);const end=t+duration;const busy=appointments.some(a=>a.date===date&&String(a.barberId)===String(barberId)&&!['Cancelado','No-Show'].includes(a.status)&&(()=>{const at=timeToMinutes(a.time),ae=at+Number(a.duration||30);return t<ae&&end>at;})());const blocked=blocks.some(b=>b.date===date&&(()=>{const bt=timeToMinutes(b.time);return t<bt+step&&end>bt;})());const unavailable=busy||blocked;slots.push(`<button type="button" class="slot ${unavailable?'busy':''}" ${unavailable?'disabled':''} data-time="${time}"><span>${time}</span><small>${blocked?'Indisponível':busy?'Ocupado':'Livre'}</small></button>`);}
  el.innerHTML=slots.join("")||'<div class="empty-state"><strong>Nenhum horário disponível</strong><span>Não há espaço para a duração desse serviço nesta data.</span></div>';
  $$("#slots .slot:not(.busy)").forEach(b=>b.onclick=()=>{$$("#slots .slot").forEach(x=>x.classList.remove("selected"));b.classList.add("selected");clientState.slot=b.dataset.time;$("#selectedTime").textContent=b.dataset.time;$("#summarySlot").textContent=b.dataset.time;updateClientProgress();});
}
function renderClientHistory(apps){
  const user=JSON.parse(sessionStorage.getItem("sgs_user")||"null"),name=$("#clientName")?.value.trim();const rows=apps.filter(a=>(user?.clientId&&String(a.clientId)===String(user.clientId))||(name&&String(a.client).toLowerCase()===name.toLowerCase()));
  $("#clientHistory").innerHTML=rows.length?[...rows].sort((a,b)=>`${b.date} ${b.time}`.localeCompare(`${a.date} ${a.time}`)).map(a=>`<article class="history-item"><div class="history-date"><strong>${esc(a.time)}</strong><small>${fmtDate(a.date)}</small></div><div><strong>${esc(a.service)}</strong><small>${esc(a.barber||"Barbeiro não informado")}</small></div><div class="history-total"><strong>${money(Number(a.price||0)+Number(a.extras||0))}</strong><span class="badge ${statusClass(a.status)}">${esc(a.status)}</span><button type="button" class="history-book-again" onclick="bookAgain(${a.serviceId},${a.barberId})">Agendar novamente</button></div></article>`).join(""):'<div class="empty-state"><i class="fa-regular fa-calendar"></i><strong>Seu histórico aparecerá aqui</strong><span>Depois do primeiro agendamento, você poderá repetir um serviço.</span></div>';
}
async function bookAgain(serviceId,barberId){const [barbers,services]=await Promise.all([Store.list("barbers"),Store.list("services")]);const b=barbers.find(x=>String(x.id)===String(barberId)&&x.active!==false)||barbers.find(x=>x.active!==false);const s=services.find(x=>String(x.id)===String(serviceId)&&x.active!==false);if(!b||!s)return toast("O barbeiro ou serviço não está disponível.");clientState.barber=b;clientState.service=s;clientState.slot=null;$("#clientBarbers .barber-option")?.forEach?.(()=>{});renderClientServices(services);setTimeout(()=>{$$("#clientBarbers .barber-option").forEach(x=>x.classList.toggle("selected",String(x.dataset.id)===String(b.id)));$$("#clientServices .service-option").forEach(x=>x.classList.toggle("selected",String(x.dataset.id)===String(s.id)));},0);updateClientSummary();updateClientProgress();$("#booking")?.scrollIntoView({behavior:"smooth",block:"start"});await renderSlots(await Store.list("appointments"));}


async function renderPublicHome(){
  const [services,barbers,ratings]=await Promise.all([Store.list("services"),Store.list("barbers"),Store.list("ratings")]);
  const se=$("#publicServices"),te=$("#publicTeam");if(!se||!te)return;
  se.innerHTML=services.filter(s=>s.active!==false).map(s=>`<article class="public-service"><div class="public-service-icon"><i class="fa-solid fa-scissors"></i></div><div><strong>${esc(s.name)}</strong><small>${Number(s.duration||0)} minutos</small></div><b>${money(s.price)}</b></article>`).join("");
  te.innerHTML=barbers.filter(b=>b.active!==false).map(b=>{const rr=ratings.filter(r=>String(r.barberId)===String(b.id));const avg=rr.length?rr.reduce((x,r)=>x+Number(r.barberRating||0),0)/rr.length:0;return `<article class="public-person public-person-rich"><div class="public-person-avatar">${initials(b.name)}</div><div><strong>${esc(b.name)}</strong><small>${esc(b.specialty||"Profissional da equipe")}</small><span class="profile-mini-rating">★★★★★ <em>${avg?avg.toFixed(1):"Novo"}</em></span></div><a href="dashboard-cliente.html#barbers" class="icon-btn" title="Agendar"><i class="fa-solid fa-arrow-right"></i></a></article>`}).join("");
}

function initPage(){
  initTheme();
  if($("#clientLoginForm")||$("#barberLoginForm")){initLogin();return;}
  if($("#barberDashboard")){initBarber().then(initEnhancedBarber).catch(err=>{console.error(err);toast("Não foi possível carregar o painel.");});return;}
  if($("#clientPortal")){initClient().then(initEnhancedClient).catch(err=>{console.error(err);toast("Não foi possível carregar o agendamento.");});return;}
  if($("#publicServices")){renderPublicHome().catch(err=>console.error(err));}
}
window.addEventListener("DOMContentLoaded",initPage);

/* =========================================================
   SGS BARBER — NOVOS FLUXOS FRONT-END
   Perfil, avaliações, favorito, fechamento, escalas, reativação
   e notificações. Tudo permanece atrás da camada Store.
========================================================= */

const DAY_NAMES_PT = {0:"Domingo",1:"Segunda",2:"Terça",3:"Quarta",4:"Quinta",5:"Sexta",6:"Sábado"};

function currentClient(){ return JSON.parse(sessionStorage.getItem("sgs_user") || "null"); }
function localKey(name){ return `${SGS_CONFIG.STORAGE_PREFIX}${name}`; }
function readLocalArray(name){ try{return JSON.parse(localStorage.getItem(localKey(name))||"[]");}catch{return [];} }
function writeLocalArray(name,value){ localStorage.setItem(localKey(name),JSON.stringify(value)); }
function clientIdFromSession(){ return currentClient()?.clientId || null; }

async function notify(type,title,message,meta={}){
  const rows=await Store.list("notifications");
  await Store.create("notifications",{type,title,message,read:false,createdAt:new Date().toISOString(),meta});
  return rows;
}

async function renderTodayOperational(){
  const date=$("#agendaDate")?.value || SGS_CONFIG.DEFAULT_DATE;
  const apps=await Store.list("appointments");
  const rows=apps.filter(a=>a.date===date);
  const active=rows.filter(a=>!['Cancelado','No-Show'].includes(a.status));
  const completed=rows.filter(a=>a.status==="Concluído");
  const cancelled=rows.filter(a=>['Cancelado','No-Show'].includes(a.status));
  const revenue=completed.reduce((sum,a)=>sum+Number(a.price||0)+Number(a.extras||0),0);
  const waiting=rows.filter(a=>['Em Espera','Agendado','Confirmado'].includes(a.status)).length;
  $("#todayTotal") && ($("#todayTotal").textContent=rows.length);
  $("#todayRevenue") && ($("#todayRevenue").textContent=money(revenue));
  $("#todayCompleted") && ($("#todayCompleted").textContent=completed.length);
  $("#todayWaiting") && ($("#todayWaiting").textContent=waiting);
  $("#todayCancelled") && ($("#todayCancelled").textContent=cancelled.length);
  $("#todayDateLabel") && ($("#todayDateLabel").textContent=fmtDate(date));
  const nowMinutes=new Date().getHours()*60+new Date().getMinutes();
  const upcoming=active.filter(a=>timeToMinutes(a.time)>=nowMinutes).sort((a,b)=>a.time.localeCompare(b.time))[0] || active.sort((a,b)=>a.time.localeCompare(b.time))[0];
  const box=$("#nextAppointmentBox");
  if(box) box.innerHTML=upcoming?`<div class="next-appointment-main"><div class="next-time">${esc(upcoming.time)}</div><div><strong>${esc(upcoming.client)}</strong><span>${esc(upcoming.service)}</span><small>${esc(upcoming.barber||"Barbeiro não informado")} · ${Number(upcoming.duration||30)} min</small></div></div><button class="btn btn-primary btn-small" onclick="switchSection('agenda')">Ver agenda</button>`:`<div class="next-appointment-empty"><i class="fa-regular fa-calendar"></i><span>Nenhum atendimento próximo.</span></div>`;
}

async function renderNotifications(){
  const rows=(await Store.list("notifications")).sort((a,b)=>String(b.createdAt).localeCompare(String(a.createdAt)));
  const unread=rows.filter(n=>!n.read).length;
  $("#notificationCount") && ($("#notificationCount").textContent=unread);
  const list=$("#notificationsList");
  if(!list)return;
  list.innerHTML=rows.length?rows.map(n=>`<article class="notification-item ${n.read?'read':''}"><div class="notification-icon"><i class="fa-regular fa-bell"></i></div><div><strong>${esc(n.title)}</strong><p>${esc(n.message)}</p><small>${n.createdAt?new Date(n.createdAt).toLocaleString("pt-BR"):""}</small></div>${n.read?'<span class="badge badge-info">Lida</span>':`<button class="btn btn-secondary btn-small" onclick="markNotificationRead(${n.id})">Marcar lida</button>`}</article>`).join(""):`<div class="empty-state"><i class="fa-regular fa-bell-slash"></i><strong>Nenhuma notificação</strong><span>Novos eventos aparecerão aqui.</span></div>`;
}
async function markNotificationRead(id){ await Store.update("notifications",id,{read:true}); await renderNotifications(); }
async function markAllNotificationsRead(){ const rows=await Store.list("notifications"); await Promise.all(rows.filter(n=>!n.read).map(n=>Store.update("notifications",n.id,{read:true}))); await renderNotifications(); toast("Notificações marcadas como lidas."); }

async function renderReactivation(){
  const [clients,apps]=await Promise.all([Store.list("clients"),Store.list("appointments")]);
  const now=new Date();
  const candidates=clients.map(c=>{
    const completed=apps.filter(a=>(a.clientId===c.id||a.client===c.name)&&a.status==="Concluído").sort((a,b)=>`${b.date} ${b.time}`.localeCompare(`${a.date} ${a.time}`));
    if(!completed.length)return null;
    const last=new Date(`${completed[0].date}T12:00:00`);
    const days=Math.max(0,Math.floor((now-last)/86400000));
    return {...c,lastVisit:completed[0].date,days};
  }).filter(Boolean).filter(c=>c.days>=30).sort((a,b)=>b.days-a.days);
  const el=$("#reactivationList"); if(!el)return;
  el.innerHTML=candidates.length?candidates.map(c=>`<article class="reactivation-card"><div class="person-cell"><span class="person-avatar">${initials(c.name)}</span><div><strong>${esc(c.name)}</strong><small>Última visita: ${fmtDate(c.lastVisit)}</small></div></div><strong>${c.days} dias</strong><button class="btn btn-primary btn-small" onclick="reactivateClient(${c.id})"><i class="fa-brands fa-whatsapp"></i> Preparar mensagem</button></article>`).join(""):`<div class="empty-state"><i class="fa-solid fa-user-check"></i><strong>Nenhum cliente para reativar</strong><span>Clientes com 30 dias ou mais sem atendimento aparecerão aqui.</span></div>`;
}
async function reactivateClient(id){
  const c=await Store.get("clients",id); if(!c)return;
  const phone=String(c.phone||"").replace(/\D/g,"");
  const msg=`Olá, ${c.name}! Sentimos sua falta na SGS Barber. Que tal marcar seu próximo horário?`;
  const url=phone?`https://wa.me/${phone.length<=11?'55'+phone:phone}?text=${encodeURIComponent(msg)}`:`https://wa.me/?text=${encodeURIComponent(msg)}`;
  window.open(url,"_blank","noopener,noreferrer");
}

async function renderCheckout(){
  const apps=await Store.list("appointments");
  const selectable=apps.filter(a=>!['Cancelado','No-Show','Concluído'].includes(a.status));
  const select=$("#checkoutAppointment");
  if(select) select.innerHTML=selectable.map(a=>`<option value="${a.id}">${esc(a.client)} — ${esc(a.service)} — ${fmtDate(a.date)} ${esc(a.time)}</option>`).join("")||'<option value="">Nenhum atendimento aberto</option>';
  await updateCheckoutPreview();
}
async function updateCheckoutPreview(){
  const id=$("#checkoutAppointment")?.value; const box=$("#checkoutPreview"); if(!box)return;
  const a=id?await Store.get("appointments",id):null;
  if(!a){box.innerHTML='<div class="empty-state"><i class="fa-solid fa-cash-register"></i><strong>Nenhum atendimento selecionado</strong><span>Conclua um atendimento para gerar o fechamento.</span></div>';return;}
  const discount=Number($("#checkoutDiscount")?.value||0); const service=Number(a.price||0); const extras=Number(a.extras||0); const total=Math.max(0,service+extras-discount);
  box.innerHTML=`<span class="eyebrow">COMANDA</span><h3>${esc(a.client)}</h3><div class="checkout-line"><span>${esc(a.service)}</span><strong>${money(service)}</strong></div><div class="checkout-line"><span>Extras</span><strong>${money(extras)}</strong></div><div class="checkout-line"><span>Desconto</span><strong>- ${money(discount)}</strong></div><div class="checkout-total"><span>TOTAL</span><strong>${money(total)}</strong></div><small>${esc(a.barber||"Barbeiro não informado")} · ${fmtDate(a.date)} às ${esc(a.time)}</small>`;
}
async function closeAppointment(e){
  e.preventDefault();
  const id=$("#checkoutAppointment")?.value; if(!id)return toast("Selecione um atendimento.");
  const a=await Store.get("appointments",id); if(!a)return toast("Atendimento não encontrado.");
  const discount=Math.max(0,Number($("#checkoutDiscount").value||0));
  const payment=$("#checkoutPayment").value; const note=$("#checkoutNote").value.trim();
  const total=Math.max(0,Number(a.price||0)+Number(a.extras||0)-discount);
  await Store.update("appointments",id,{status:"Concluído",discount,paymentMethod:payment,finalTotal:total,checkoutNote:note,completedAt:new Date().toISOString()});
  await notify("completed","Atendimento concluído",`${a.client} teve o atendimento finalizado por ${money(total)}.`);
  toast("Atendimento fechado."); e.target.reset(); $("#checkoutDiscount").value=0; await renderCheckout(); await renderBarber(); await renderClients(); await renderReactivation(); await renderNotifications();
}

async function renderSchedules(){
  const [barbers,rows]=await Promise.all([Store.list("barbers"),Store.list("barberSchedules")]);
  const select=$("#scheduleBarber"); if(select)select.innerHTML=barbers.filter(b=>b.active!==false).map(b=>`<option value="${b.id}">${esc(b.name)}</option>`).join("");
  const el=$("#scheduleTable"); if(!el)return;
  el.innerHTML=rows.map(r=>{const b=barbers.find(x=>String(x.id)===String(r.barberId));return `<tr><td>${esc(b?.name||r.barber||"-")}</td><td>${DAY_NAMES_PT[r.day]||r.day}</td><td>${r.type==="working"?`${r.open||"-"} — ${r.close||"-"}`:"-"}</td><td>${r.breakStart&&r.breakEnd?`${r.breakStart} — ${r.breakEnd}`:"-"}</td><td>${Number(r.buffer||0)} min</td><td><span class="badge ${r.type==="working"?'success':'warning'}">${r.type==="working"?'Expediente':r.type==="day_off"?'Folga':'Férias'}</span></td><td><button class="btn btn-danger btn-small" onclick="removeSchedule(${r.id})">Remover</button></td></tr>`}).join("")||'<tr><td colspan="7" class="empty">Nenhuma escala específica. O sistema usará as configurações gerais.</td></tr>';
}
async function saveSchedule(e){
  e.preventDefault();
  const data={barberId:Number($("#scheduleBarber").value),day:Number($("#scheduleDay").value),open:$("#scheduleOpen").value,close:$("#scheduleClose").value,breakStart:$("#scheduleBreakStart").value,breakEnd:$("#scheduleBreakEnd").value,buffer:Number($("#scheduleBuffer").value||0),type:$("#scheduleType").value};
  const rows=await Store.list("barberSchedules"); const old=rows.find(r=>String(r.barberId)===String(data.barberId)&&Number(r.day)===data.day);
  if(old)await Store.update("barberSchedules",old.id,data);else await Store.create("barberSchedules",data);
  toast("Escala salva."); await renderSchedules();
}
async function removeSchedule(id){await Store.remove("barberSchedules",id);toast("Escala removida.");await renderSchedules();}

async function scheduleForBarber(barberId,date){
  const day=new Date(`${date}T12:00:00`).getDay();
  const rows=await Store.list("barberSchedules");
  const specific=rows.find(r=>String(r.barberId)===String(barberId)&&Number(r.day)===day);
  if(specific)return specific;
  const settings=savedSettings();
  if(!(settings.days||[1,2,3,4,5,6]).includes(day))return {type:"day_off"};
  return {type:"working",open:settings.open,close:settings.close,breakStart:"",breakEnd:"",buffer:0};
}

async function renderBarberProfileModal(barberId){
  const [barbers,services,ratings]=await Promise.all([Store.list("barbers"),Store.list("services"),Store.list("ratings")]);
  const b=barbers.find(x=>String(x.id)===String(barberId)); if(!b)return;
  const ids=(b.serviceIds||[]).map(String); const list=services.filter(s=>s.active!==false&&(!ids.length||ids.includes(String(s.id))));
  const reviews=ratings.filter(r=>String(r.barberId)===String(barberId)); const avg=reviews.length?reviews.reduce((s,r)=>s+Number(r.barberRating||0),0)/reviews.length:0;
  let modal=$("#barberProfileModal"); if(!modal){modal=document.createElement("div");modal.id="barberProfileModal";modal.className="modal-backdrop";document.body.appendChild(modal);}
  const favorite=String(getFavoriteBarberId()||"")===String(b.id);
  modal.innerHTML=`<div class="modal-card barber-profile-modal"><button class="icon-btn modal-close" onclick="closeBarberProfile()"><i class="fa-solid fa-xmark"></i></button><div class="profile-cover"><div class="profile-avatar-large">${b.photoUrl?`<img src="${esc(b.photoUrl)}" alt="Foto de ${esc(b.name)}">`:initials(b.name)}</div></div><div class="profile-body"><span class="eyebrow">PERFIL DO PROFISSIONAL</span><h2>${esc(b.name)}</h2><p>${esc(b.specialty||"Profissional da equipe")}</p><div class="profile-rating"><span class="stars">${"★".repeat(Math.round(avg)||0)}${"☆".repeat(5-(Math.round(avg)||0))}</span><strong>${avg?avg.toFixed(1):"Novo"}</strong><small>${reviews.length} avaliação(ões)</small></div><div class="profile-block"><h3>Especialidades</h3><div class="tag-list">${(b.specialties||[b.specialty||"Fade","Degradê","Barba"]).map(x=>`<span>${esc(x)}</span>`).join("")}</div></div><div class="profile-block"><h3>Serviços</h3><div class="profile-services">${list.map(s=>`<div><span>${esc(s.name)}</span><strong>${money(s.price)}</strong></div>`).join("")}</div></div><div class="profile-block"><h3>Horários</h3><div class="profile-hours">${[1,2,3,4,5,6,0].map(day=>`<span><b>${DAY_NAMES_PT[day]}</b><small data-profile-day="${day}">Consultar</small></span>`).join("")}</div></div><div class="profile-actions"><button class="btn ${favorite?'btn-primary':'btn-secondary'}" onclick="toggleFavoriteBarber(${b.id})"><i class="fa-${favorite?'solid':'regular'} fa-heart"></i> ${favorite?'Barbeiro favorito':'Favoritar barbeiro'}</button><a class="btn btn-primary" href="dashboard-cliente.html#booking" onclick="sessionStorage.setItem('sgs_preferred_barber','${b.id}')">Agendar com ${esc(b.name)}</a></div></div></div>`;
  modal.hidden=false;
  const schedules=await Promise.all([1,2,3,4,5,6,0].map(day=>Store.list("barberSchedules").then(rows=>rows.find(r=>String(r.barberId)===String(b.id)&&Number(r.day)===day))));
  schedules.forEach((r,i)=>{const el=modal.querySelector(`[data-profile-day="${[1,2,3,4,5,6,0][i]}"]`);if(el)el.textContent=r?.type==="day_off"?"Folga":r?.type==="vacation"?"Férias":r?`${r.open} — ${r.close}`:"Horário geral";});
}
function closeBarberProfile(){const m=$("#barberProfileModal");if(m)m.hidden=true;}
function getFavoriteBarberId(){return localStorage.getItem(localKey("favoriteBarberId"));}
async function toggleFavoriteBarber(id){localStorage.setItem(localKey("favoriteBarberId"),String(id));toast("Barbeiro favorito atualizado.");if($("#barberProfileModal"))await renderBarberProfileModal(id);await renderClientFavorite();}
async function renderClientFavorite(){
  const box=$("#favoriteBarberBox");if(!box)return;const id=getFavoriteBarberId();const barbers=await Store.list("barbers");const b=barbers.find(x=>String(x.id)===String(id)&&x.active!==false);
  box.innerHTML=b?`<div class="favorite-card"><div class="person-cell"><span class="person-avatar">${initials(b.name)}</span><div><span class="eyebrow">MEU BARBEIRO PREFERIDO</span><strong>${esc(b.name)}</strong><small>${esc(b.specialty||"Profissional da equipe")}</small></div></div><a class="btn btn-primary btn-small" href="#booking" onclick="bookWithFavorite(${b.id})">Agendar novamente</a></div>`:`<div class="favorite-empty"><i class="fa-regular fa-heart"></i><div><strong>Escolha seu barbeiro preferido</strong><small>Isso facilita seus próximos agendamentos.</small></div></div>`;
}
async function bookWithFavorite(id){const [barbers,services]=await Promise.all([Store.list("barbers"),Store.list("services")]);const b=barbers.find(x=>String(x.id)===String(id));if(!b)return;clientState.barber=b;clientState.service=null;clientState.slot=null;renderClientBarbers(barbers);renderClientServices(services);const item=$(`#clientBarbers .barber-option[data-id="${id}"]`);item?.click();}

async function submitRating(appointmentId){
  const a=await Store.get("appointments",appointmentId);if(!a)return;
  const existing=(await Store.list("ratings")).find(r=>String(r.appointmentId)===String(appointmentId));if(existing)return toast("Este atendimento já foi avaliado.");
  const modal=document.createElement("div");modal.className="modal-backdrop";modal.id="ratingModal";modal.innerHTML=`<div class="modal-card rating-modal"><button class="icon-btn modal-close" onclick="this.closest('.modal-backdrop').remove()"><i class="fa-solid fa-xmark"></i></button><span class="eyebrow">AVALIAÇÃO</span><h2>Como foi seu atendimento?</h2><p>${esc(a.service)} com ${esc(a.barber||"seu barbeiro")}</p><div class="rating-stars" id="ratingStars">${[1,2,3,4,5].map(n=>`<button type="button" data-rating="${n}">★</button>`).join("")}</div><div class="form-grid"><div class="field"><label>Barbearia</label><select id="shopRating"><option value="5">5 — Excelente</option><option value="4">4 — Muito bom</option><option value="3">3 — Bom</option><option value="2">2 — Regular</option><option value="1">1 — Ruim</option></select></div><div class="field"><label>Barbeiro</label><select id="barberRating"><option value="5">5 — Excelente</option><option value="4">4 — Muito bom</option><option value="3">3 — Bom</option><option value="2">2 — Regular</option><option value="1">1 — Ruim</option></select></div><div class="field field-wide"><label>Comentário</label><textarea id="ratingComment" rows="4" placeholder="Conte como foi sua experiência."></textarea></div></div><button class="btn btn-primary" onclick="saveRating(${a.id})">Enviar avaliação</button></div>`;document.body.appendChild(modal);
  let selected=5; $$("#ratingStars button").forEach(btn=>btn.onclick=()=>{selected=Number(btn.dataset.rating);$$("#ratingStars button").forEach(x=>x.classList.toggle("selected",Number(x.dataset.rating)<=selected));}); $$("#ratingStars button").forEach(x=>x.classList.add("selected"));
  modal.dataset.serviceRating=selected;
  modal.querySelectorAll("#ratingStars button").forEach(btn=>btn.addEventListener("click",()=>modal.dataset.serviceRating=btn.dataset.rating));
}
async function saveRating(appointmentId){const a=await Store.get("appointments",appointmentId);const modal=$("#ratingModal");if(!a||!modal)return;const rating={appointmentId:a.id,clientId:a.clientId,barberId:a.barberId,shopRating:Number($("#shopRating").value),barberRating:Number($("#barberRating").value),comment:$("#ratingComment").value.trim(),createdAt:new Date().toISOString()};await Store.create("ratings",rating);modal.remove();toast("Obrigado pela avaliação.");await renderClientHistory(await Store.list("appointments"));}

async function enhanceClientHistoryRatings(rows){
  const user=currentClient(),name=$("#clientName")?.value.trim();const mine=rows.filter(a=>(user?.clientId&&String(a.clientId)===String(user.clientId))||(name&&String(a.client).toLowerCase()===name.toLowerCase()));const ratings=await Store.list("ratings");
  return [...mine].sort((a,b)=>`${b.date} ${b.time}`.localeCompare(`${a.date} ${a.time}`)).map(a=>`<article class="history-item"><div class="history-date"><strong>${esc(a.time)}</strong><small>${fmtDate(a.date)}</small></div><div><strong>${esc(a.service)}</strong><small>${esc(a.barber||"Barbeiro não informado")}</small></div><div class="history-total"><strong>${money(Number(a.finalTotal??Number(a.price||0)+Number(a.extras||0)))}</strong><span class="badge ${statusClass(a.status)}">${esc(a.status)}</span>${a.status==="Concluído"&&!ratings.some(r=>String(r.appointmentId)===String(a.id))?`<button type="button" class="history-book-again" onclick="submitRating(${a.id})">Avaliar atendimento</button>`:''}<button type="button" class="history-book-again" onclick="bookAgain(${a.serviceId},${a.barberId})">Agendar novamente</button></div></article>`).join("");
}

// Reforça o fluxo de disponibilidade: expediente individual + intervalo + buffer + duração.
async function renderSlots(appointments){
  window.__clientAppointments=appointments||[];const date=$("#bookingDate")?.value,barberId=clientState.barber?.id,service=clientState.service,el=$("#slots");if(!el)return;
  if(!barberId||!service){el.innerHTML='<div class="empty-state"><strong>Selecione barbeiro e serviço</strong><span>Os horários disponíveis aparecerão aqui.</span></div>';return;}
  const schedule=await scheduleForBarber(barberId,date);if(schedule.type!=="working"){el.innerHTML='<div class="empty-state"><strong>O profissional não atende nesta data</strong><span>Escolha outro dia para consultar horários.</span></div>';return;}
  const blocks=await Store.list("blocks"),step=Number(savedSettings().interval||30),start=timeToMinutes(schedule.open),close=timeToMinutes(schedule.close),duration=Number(service.duration||30),buffer=Number(schedule.buffer||0),slots=[];
  for(let t=start;t+duration+buffer<=close;t+=step){const time=minutesToTime(t),end=t+duration+buffer;const busy=appointments.some(a=>a.date===date&&String(a.barberId)===String(barberId)&&!['Cancelado','No-Show'].includes(a.status)&&(()=>{const at=timeToMinutes(a.time),ae=at+Number(a.duration||30)+Number(a.buffer||0);return t<ae&&end>at;})());const blocked=blocks.some(b=>b.date===date&&(()=>{const bt=timeToMinutes(b.time);return t<bt+step&&end>bt;})());const brk=schedule.breakStart&&schedule.breakEnd&&t<timeToMinutes(schedule.breakEnd)&&end>timeToMinutes(schedule.breakStart);const unavailable=busy||blocked||brk;slots.push(`<button type="button" class="slot ${unavailable?'busy':''}" ${unavailable?'disabled':''} data-time="${time}"><span>${time}</span><small>${brk?'Intervalo':blocked?'Indisponível':busy?'Ocupado':'Livre'}</small></button>`);}
  el.innerHTML=slots.join("")||'<div class="empty-state"><strong>Nenhum horário disponível</strong><span>Não há espaço para a duração desse serviço nesta data.</span></div>';
  $$("#slots .slot:not(.busy)").forEach(b=>b.onclick=()=>{$$("#slots .slot").forEach(x=>x.classList.remove("selected"));b.classList.add("selected");clientState.slot=b.dataset.time;$("#selectedTime").textContent=b.dataset.time;$("#summarySlot").textContent=b.dataset.time;updateClientProgress();});
}

// Cards de barbeiro mais completos no fluxo público/cliente.
function renderClientBarbers(barbers){
  const active=barbers.filter(b=>b.active!==false);$("#barberCount") && ($("#barberCount").textContent=`${active.length} profissional(is) disponível(is)`);
  const fav=getFavoriteBarberId();
  $("#clientBarbers").innerHTML=active.length?active.map(b=>`<article class="barber-profile-card ${String(fav)===String(b.id)?'is-favorite':''}"><button type="button" class="barber-option" data-id="${b.id}"><span class="barber-avatar">${b.photoUrl?`<img src="${esc(b.photoUrl)}" alt="Foto de ${esc(b.name)}">`:initials(b.name)}</span><span class="barber-copy"><strong>${esc(b.name)}</strong><small>${esc(b.specialty||"Profissional da equipe")}</small><span class="profile-mini-rating">★★★★★ <em>${Number(b.rating||0)?Number(b.rating).toFixed(1):"Novo"}</em></span></span><i class="fa-solid fa-chevron-right"></i></button><div class="barber-card-actions"><button type="button" class="btn btn-secondary btn-small" onclick="renderBarberProfileModal(${b.id})">Ver perfil</button><button type="button" class="icon-btn ${String(fav)===String(b.id)?'favorite-active':''}" onclick="toggleFavoriteBarber(${b.id})" title="Favoritar"><i class="fa-${String(fav)===String(b.id)?'solid':'regular'} fa-heart"></i></button></div></article>`).join(""):`<div class="empty-state"><strong>Nenhum barbeiro disponível</strong><span>A equipe ainda não foi cadastrada.</span></div>`;
  clientState.barber=null;clientState.service=null;clientState.slot=null;updateClientSelection();renderSlots([]);updateClientProgress();
  $$("#clientBarbers .barber-option").forEach(el=>el.onclick=async()=>{$$("#clientBarbers .barber-option").forEach(x=>x.classList.remove("selected"));el.classList.add("selected");clientState.barber=active.find(b=>String(b.id)===el.dataset.id)||null;clientState.service=null;clientState.slot=null;renderClientServices(await Store.list("services"));renderSlots([]);updateClientSelection();updateClientProgress();$("#services")?.scrollIntoView({behavior:"smooth",block:"start"});});
}

async function renderClientHistory(apps){
  const html=await enhanceClientHistoryRatings(apps);$("#clientHistory").innerHTML=html||'<div class="empty-state"><i class="fa-regular fa-calendar"></i><strong>Seu histórico aparecerá aqui</strong><span>Depois do primeiro agendamento, você poderá avaliar e repetir serviços.</span></div>';
}

async function initEnhancedBarber(){
  await renderAgendaFilters();renderBarberServiceChecks();loadSettings();await renderWaitlist();await renderTodayOperational();await renderNotifications();await renderReactivation();await renderCheckout();await renderSchedules();
  $("#agendaBarberFilter")?.addEventListener("change",renderFullAgenda);$("#waitlistForm")?.addEventListener("submit",createWaitlist);$("#clientSearch")?.addEventListener("input",renderClients);$("#settingsForm")?.addEventListener("submit",saveSettings);$("#checkoutForm")?.addEventListener("submit",closeAppointment);$("#checkoutAppointment")?.addEventListener("change",updateCheckoutPreview);$("#checkoutDiscount")?.addEventListener("input",updateCheckoutPreview);$("#scheduleForm")?.addEventListener("submit",saveSchedule);$("#notificationBtn")?.addEventListener("click",()=>switchSection("notifications"));$("#agendaDate")?.addEventListener("change",async()=>{await renderBarber();await renderTodayOperational();});$("#fullAgendaDate")?.addEventListener("change",renderFullAgenda);
}

async function initEnhancedClient(){
  $("#bookingDate")?.setAttribute("min",new Date().toISOString().slice(0,10));updateClientProgress();await renderClientFavorite();
  $("#favoriteBarberBox")?.addEventListener("click",()=>{});
}

// Extende os nomes de navegação sem alterar a arquitetura existente.
const _switchSectionOriginal = switchSection;
switchSection = function(section){
  _switchSectionOriginal(section);
  const names={checkout:"Caixa e comanda",schedules:"Escalas dos barbeiros",reactivation:"Clientes para reativar",notifications:"Notificações"};
  if(names[section]&&$("#pageTitle"))$("#pageTitle").textContent=names[section];
  if(section==="checkout")renderCheckout();
  if(section==="schedules")renderSchedules();
  if(section==="reactivation")renderReactivation();
  if(section==="notifications")renderNotifications();
};

// Ajusta o perfil favorito para clientes já logados.

async function createBarber(e){
  e.preventDefault();
  const serviceIds=$$("input[name='barberService']:checked").map(x=>Number(x.value));
  const data={name:$("#barberName").value.trim(),specialty:$("#barberSpecialty")?.value.trim()||"Profissional da equipe",photoUrl:$("#barberPhoto")?.value.trim()||"",phone:$("#barberPhone").value.trim(),email:$("#barberEmail").value.trim().toLowerCase(),commission:Number($("#barberCommission").value||50),active:true,serviceIds};
  if(!data.name)return toast("Informe o nome do profissional.");
  await Store.create("barbers",data);toast("Barbeiro cadastrado.");e.target.reset();$("#barberCommission").value=50;renderBarberServiceChecks();await renderBarbers();await renderAgendaFilters();
}

async function saveSchedule(e){
  e.preventDefault();
  const type=$("#scheduleType").value;const barberId=Number($("#scheduleBarber").value);const offStart=$("#scheduleOffStart")?.value||"";const offEnd=$("#scheduleOffEnd")?.value||"";
  if(type==="vacation" && (!offStart||!offEnd))return toast("Informe início e fim da ausência.");
  if(type==="vacation"){await Store.create("barberTimeOff",{barberId,startDate:offStart,endDate:offEnd,reason:"Férias / ausência"});toast("Período de ausência salvo.");e.target.reset();await renderSchedules();return;}
  const data={barberId,day:Number($("#scheduleDay").value),open:$("#scheduleOpen").value,close:$("#scheduleClose").value,breakStart:$("#scheduleBreakStart").value,breakEnd:$("#scheduleBreakEnd").value,buffer:Number($("#scheduleBuffer").value||0),type};
  const rows=await Store.list("barberSchedules");const old=rows.find(r=>String(r.barberId)===String(data.barberId)&&Number(r.day)===data.day);if(old)await Store.update("barberSchedules",old.id,data);else await Store.create("barberSchedules",data);toast("Escala salva.");e.target.reset();$("#scheduleOpen").value="08:00";$("#scheduleClose").value="18:00";$("#scheduleBuffer").value="10";await renderSchedules();
}
async function renderSchedules(){
  const [barbers,rows,offs]=await Promise.all([Store.list("barbers"),Store.list("barberSchedules"),Store.list("barberTimeOff")]);const select=$("#scheduleBarber");if(select)select.innerHTML=barbers.filter(b=>b.active!==false).map(b=>`<option value="${b.id}">${esc(b.name)}</option>`).join("");const el=$("#scheduleTable");if(!el)return;
  const scheduleRows=rows.map(r=>{const b=barbers.find(x=>String(x.id)===String(r.barberId));return `<tr><td>${esc(b?.name||"-")}</td><td>${DAY_NAMES_PT[r.day]}</td><td>${r.type==="working"?`${r.open} — ${r.close}`:"-"}</td><td>${r.breakStart&&r.breakEnd?`${r.breakStart} — ${r.breakEnd}`:"-"}</td><td>${Number(r.buffer||0)} min</td><td><span class="badge ${r.type==="working"?'success':'warning'}">${r.type==="working"?'Expediente':'Folga semanal'}</span></td><td><button class="btn btn-danger btn-small" onclick="removeSchedule(${r.id})">Remover</button></td></tr>`});
  const offRows=offs.map(r=>{const b=barbers.find(x=>String(x.id)===String(r.barberId));return `<tr><td>${esc(b?.name||"-")}</td><td>${fmtDate(r.startDate)} → ${fmtDate(r.endDate)}</td><td>-</td><td>-</td><td>-</td><td><span class="badge warning">Férias / ausência</span></td><td><button class="btn btn-danger btn-small" onclick="removeTimeOff(${r.id})">Remover</button></td></tr>`});
  el.innerHTML=[...scheduleRows,...offRows].join("")||'<tr><td colspan="7" class="empty">Nenhuma escala específica. O sistema usará as configurações gerais.</td></tr>';
}
async function removeTimeOff(id){await Store.remove("barberTimeOff",id);toast("Ausência removida.");await renderSchedules();}
async function scheduleForBarber(barberId,date){
  const offs=await Store.list("barberTimeOff");if(offs.some(x=>String(x.barberId)===String(barberId)&&date>=x.startDate&&date<=x.endDate))return {type:"day_off"};
  const day=new Date(`${date}T12:00:00`).getDay();const rows=await Store.list("barberSchedules");const specific=rows.find(r=>String(r.barberId)===String(barberId)&&Number(r.day)===day);if(specific)return specific;const settings=savedSettings();if(!(settings.days||[1,2,3,4,5,6]).includes(day))return {type:"day_off"};return {type:"working",open:settings.open,close:settings.close,breakStart:"",breakEnd:"",buffer:0};
}
