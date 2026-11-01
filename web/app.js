'use strict';
/*
 * Frontend del MVP: solo presenta lo que decide el backend.
 *  - estado (incluido SATURADO), puedeIngresar, tiempos, posición y turno llegan resueltos;
 *  - el orden lo da la Strategy elegida (acá solo se manda su código);
 *  - las funcionalidades llegan como nombres, nunca como bitmask.
 * Diseño: ver styles.css (color = estado, vidrio solo en la capa funcional, objetivos >= 44 px).
 */

const ESTADOS = {
  LIBRE:             { texto: 'Libre',             icono: 'check' },
  OCUPADO:           { texto: 'Ocupado',           icono: 'user' },
  SATURADO:          { texto: 'Saturado',          icono: 'alert' },
  MANTENIMIENTO:     { texto: 'En mantenimiento',  icono: 'wrench' },
  FUERA_DE_SERVICIO: { texto: 'Fuera de servicio', icono: 'slash' }
};
const FUNC = {
  MOVILIDAD_REDUCIDA: { icono: 'access',    texto: 'Movilidad reducida' },
  LAVAMANOS:          { icono: 'drop',      texto: 'Lavamanos' },
  DISPENSER_ALCOHOL:  { icono: 'sanitizer', texto: 'Dispenser de alcohol' },
  CAMBIADOR_BEBES:    { icono: 'baby',      texto: 'Cambiador de bebés' },
  ESPEJO:             { icono: 'mirror',    texto: 'Espejo' }
};
const CRITERIOS = { CERCANIA: 'Cercanía', CANTIDAD_ESPERA: 'Personas esperando', TIEMPO_ESPERA: 'Tiempo de espera', MEJOR_OPCION: 'Mejor opción' };
const MOTIVOS = {
  RUTINA: 'limpieza de rutina', SOLICITADO_ANTES_DE_USO: 'reportado antes de usarlo',
  SOLICITADO_DESPUES_DE_USO: 'reportado después de usarlo', POST_REPARACION: 'limpieza posterior a una reparación'
};
const TITULOS = { lista: 'Baños', mapa: 'Mapa', cola: 'Mi cola' };
const ORIGEN_EJEMPLO = { lat: -34.6081, lng: -58.3731 };
const POLL_MS = 3000;
// Recorrido del evento: la tira de ruta ubica cada baño proyectándolo sobre este tramo.
const RUTA = { ini: { nombre: 'Catedral', lat: -34.6083, lng: -58.3731 }, fin: { nombre: 'Obelisco', lat: -34.6037, lng: -58.3816 } };

const $ = id => document.getElementById(id);
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const icon = (id, cls = '') => `<svg class="ic ${cls}" aria-hidden="true" focusable="false"><use href="icons.svg#i-${id}"/></svg>`;
const LS = {
  get(k) { try { return localStorage.getItem(k); } catch (e) { return null; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch (e) { /* sin storage: seguimos igual */ } }
};
/** Reemplaza el HTML solo si cambió: evita parpadeo y pérdida de foco en cada refresco. */
function setHtml(el, html) { if (el._h !== html) { el._h = html; el.innerHTML = html; } }

const state = {
  personaId: null, origen: { ...ORIGEN_EJEMPLO, real: false }, vista: 'lista', cargado: false,
  banios: [], miCola: null, ultimaNotif: 0, notifInit: false, seleccion: null, detalleId: null, sucio: false,
  filtros: { criterio: 'CERCANIA', distanciaMax: '', funcionalidades: new Set(), estados: new Set() },
  map: null, markers: {}, yoMarker: null
};

// ------------------------------------------------------------------ API
async function api(ruta, metodo = 'GET', params = {}) {
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== null && v !== '') qs.set(k, v);
  const r = await fetch('/api/' + ruta + (qs.toString() ? '?' + qs : ''), { method: metodo });
  const data = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(data.error || ('Error ' + r.status));
  return data;
}
const paramsBanios = () => ({
  lat: state.origen.lat, lng: state.origen.lng, criterio: state.filtros.criterio, distanciaMax: state.filtros.distanciaMax,
  funcionalidades: [...state.filtros.funcionalidades].join(','), estados: [...state.filtros.estados].join(','), personaId: state.personaId
});

async function asegurarPersona() {
  const guardada = LS.get('personaId');
  if (guardada) { try { await api(`personas/${guardada}/cola`); return guardada; } catch (e) { /* servidor reiniciado: se crea otra */ } }
  const p = await api('personas', 'POST');
  LS.set('personaId', p.id);
  return p.id;
}

// ------------------------------------------------------------- refresco
let refrescando = false;
async function refrescar() {
  if (refrescando || !state.personaId) return;
  refrescando = true;
  try {
    const [banios, cola, notifs] = await Promise.all([
      api('banios', 'GET', paramsBanios()),
      api(`personas/${state.personaId}/cola`),
      api(`personas/${state.personaId}/notificaciones`, 'GET', { desde: state.ultimaNotif })
    ]);
    state.banios = banios;
    state.miCola = cola.enCola ? cola : null;
    state.cargado = true;
    procesarNotificaciones(notifs);
    render();
    if (state.detalleId != null && $('sheet-detalle').open) await pintarDetalle(state.detalleId);
  } catch (e) {
    if (!state.cargado) { state.cargado = true; render(); }
  } finally { refrescando = false; }
}

function procesarNotificaciones(lista) {
  if (!state.notifInit) {                          // primera carga: no repetimos avisos viejos
    state.notifInit = true;
    if (lista.length) state.ultimaNotif = lista[lista.length - 1].id;
    return;
  }
  for (const n of lista) {
    state.ultimaNotif = Math.max(state.ultimaNotif, n.id);
    if (/^(Entraste a|Saliste de la cola|Gracias por usar)/.test(n.mensaje)) continue;   // acciones propias: ya se ven en pantalla
    const turno = n.mensaje.includes('tu turno');
    aviso(n.mensaje, turno ? 'turno' : '', turno ? 'bell' : 'people');
    if (turno && navigator.vibrate) navigator.vibrate([200, 100, 200]);   // doble señal: texto + vibración + badge
    state.ultimaNotif = Math.max(state.ultimaNotif, n.id);
  }
}

// --------------------------------------------------------------- piezas
const plata = (b, cls = '') => `<span class="plata ${cls} st-${b.estado}" aria-hidden="true"><b>${b.id}</b></span>`;
const chipEstado = e => `<span class="estado st-${e}">${icon(ESTADOS[e].icono, 'sm')}${ESTADOS[e].texto}</span>`;
const fmtDist = m => m < 1000 ? `${m} m` : `${(m / 1000).toFixed(1).replace('.', ',')} km`;
const fmtSeg = s => s >= 60 ? `${Math.floor(s / 60)} min ${s % 60} s` : `${s} s`;

function espera(min) {
  return min === 0
    ? '<span class="num sin">Sin espera</span>'
    : `<span class="num">${min}</span><span class="unidad">min</span><span class="etq">de espera</span>`;
}
function gauge(llenos, total, estado) {
  const n = Math.max(1, total);
  let celdas = '';
  for (let i = 0; i < n; i++) celdas += `<i class="${i < llenos ? 'on' : ''}"></i>`;
  return `<div class="gauge st-${estado}" role="img" aria-label="${llenos} de ${n} lugares de la cola ocupados">${celdas}</div>`;
}
function feats(fs) {
  if (!fs.length) return '<li class="feat">Sin funcionalidades extra</li>';
  return fs.map(f => `<li class="feat">${icon(FUNC[f].icono, 'sm')}${FUNC[f].texto}</li>`).join('');
}
function avisoMantenimiento(b) {
  if (b.mantenimientoRestanteSeg == null) return '';
  return `<div class="aviso st-MANTENIMIENTO">${icon('wrench')}<span>En mantenimiento (${MOTIVOS[b.motivoMantenimiento] || 'limpieza'}). Vuelve en ~${fmtSeg(b.mantenimientoRestanteSeg)}. Podés seguir esperando.</span></div>`;
}
function botonIngresar(b, extra = 'tinted') {
  if (state.miCola && state.miCola.banioId === b.id) return `<button class="btn ${extra}" data-act="tab" data-vista="cola">Ver mi cola</button>`;
  const etiqueta = b.puedeIngresar ? 'Ingresar a la cola' : (b.estado === 'SATURADO' ? 'Cola llena' : 'No disponible');
  return `<button class="btn ${extra}" data-act="ingresar" data-id="${b.id}" ${b.puedeIngresar ? '' : 'disabled'}>${etiqueta}</button>`;
}

function cardHtml(b) {
  return `
  <article class="card st-${b.estado}" aria-label="${esc(b.nombre)}">
    <div class="card-top">
      ${plata(b)}<h3>${esc(b.direccion || b.nombre)}</h3>${chipEstado(b.estado)}
    </div>
    <div class="hero">
      <div class="espera">${espera(b.tiempoEstimadoMin)}</div>
      ${b.distanciaMetros != null ? `<div class="dist">${icon('pin', 'sm')}a ${fmtDist(b.distanciaMetros)}</div>` : ''}
    </div>
    <div class="gauge-wrap">${gauge(b.personasEsperando, b.capacidad, b.estado)}<span class="gauge-txt">${b.personasEsperando} en cola</span></div>
    <ul class="feats" aria-label="Funcionalidades">${feats(b.funcionalidades)}</ul>
    ${avisoMantenimiento(b)}
    <div class="acciones">${botonIngresar(b)}<button class="btn plain" data-act="detalle" data-id="${b.id}">Detalles</button></div>
  </article>`;
}

// --------------------------------------------------------------- render
function filtrosActivos() {
  const f = state.filtros, r = [];
  if (f.distanciaMax) r.push({ k: 'dist', v: '', t: `Hasta ${fmtDist(Number(f.distanciaMax))}` });
  for (const x of f.funcionalidades) r.push({ k: 'func', v: x, t: FUNC[x].texto });
  for (const x of f.estados) r.push({ k: 'estado', v: x, t: ESTADOS[x].texto });
  return r;
}

function render() {
  const activos = filtrosActivos();
  $('cnt-filtros').hidden = activos.length === 0;
  $('cnt-filtros').textContent = activos.length;
  $('orden-txt').textContent = CRITERIOS[state.filtros.criterio];
  setHtml($('activos'), activos.map(a =>
    `<button class="chip-activo" data-act="quitar" data-k="${a.k}" data-v="${a.v}" aria-label="Quitar filtro: ${esc(a.t)}">${esc(a.t)}${icon('close', 'sm')}</button>`).join(''));

  // lista
  let html;
  if (!state.cargado) html = '<div class="skel"></div><div class="skel"></div><div class="skel"></div>';
  else if (!state.banios.length) {
    html = activos.length
      ? `<div class="vacio">${icon('sliders')}<h2>Ningún baño cumple los filtros</h2><p>Probá con una distancia mayor o menos funcionalidades.</p><button class="btn tinted" data-act="reset-filtros">Quitar filtros</button></div>`
      : `<div class="vacio">${icon('pin')}<h2>Todavía no hay baños cerca</h2><p>Cuando el evento los habilite, aparecen acá.</p></div>`;
  } else html = state.banios.map(cardHtml).join('');
  setHtml($('lista'), html);

  const operativos = state.banios.filter(b => ['LIBRE', 'OCUPADO', 'SATURADO'].includes(b.estado)).length;
  $('resumen').textContent = state.cargado ? (activos.length ? `${state.banios.length} baños con tus filtros` : `${operativos} baños operativos`) : '';
  renderRuta(); renderMapa(); renderPeek(); renderCola(); renderTabs();
}

/** Tira de ruta: cada baño se proyecta sobre el tramo RUTA (corrigiendo la longitud por la latitud) y se dibuja como chapa. */
function renderRuta() {
  const el = $('ruta'), bs = state.banios.filter(b => b.latitud != null && b.longitud != null);
  if (!state.cargado || !bs.length) { el.hidden = true; return; }
  const k = Math.cos(RUTA.ini.lat * Math.PI / 180);
  const vx = (RUTA.fin.lat - RUTA.ini.lat), vy = (RUTA.fin.lng - RUTA.ini.lng) * k, vv = vx * vx + vy * vy;
  const t = (lat, lng) => Math.max(0, Math.min(1, ((lat - RUTA.ini.lat) * vx + (lng - RUTA.ini.lng) * k * vy) / vv));
  const pct = x => (x * 100).toFixed(2) + '%';
  const paradas = bs.map(b => ({ b, t: t(b.latitud, b.longitud) })).sort((p, q) => p.t - q.t).map((p, i) =>
    `<button class="ruta-parada" style="left:${pct(p.t)};--i:${i}" data-act="detalle" data-id="${p.b.id}" aria-label="Baño ${p.b.id}, ${ESTADOS[p.b.estado].texto}${p.b.distanciaMetros != null ? ', a ' + fmtDist(p.b.distanciaMetros) : ''}">${plata(p.b, 'mini')}</button>`).join('');
  const yo = `<div class="ruta-yo" style="left:${pct(t(state.origen.lat, state.origen.lng))}">Vos</div>`;
  el.hidden = false;
  const primera = !el.dataset.listo;
  setHtml(el, `<div class="ruta-linea"></div><span class="ruta-ext ini">${RUTA.ini.nombre}</span><span class="ruta-ext fin">${RUTA.fin.nombre}</span>${paradas}${yo}`);
  if (primera) { el.dataset.listo = '1'; el.classList.add('entra'); setTimeout(() => el.classList.remove('entra'), 1600); }
}

function renderTabs() {
  document.querySelectorAll('.tab').forEach(t => {
    if (t.dataset.vista === state.vista) t.setAttribute('aria-current', 'page'); else t.removeAttribute('aria-current');
  });
  $('badge-cola').hidden = !(state.miCola && state.miCola.usandoBanio);   // el badge se reserva para lo crítico: ya es tu turno
}

function renderCola() {
  const c = state.miCola, el = $('cola');
  if (!c) {
    setHtml(el, `<div class="vacio" style="max-width:520px;margin:0 auto">${icon('people')}<h2>No estás en ninguna cola</h2>
      <p>Elegí un baño y sumate. Te avisamos cuando sea tu turno.</p><button class="btn prominent" data-act="tab" data-vista="lista">Ver baños</button></div>`);
    return;
  }
  const cab = `<div class="card-top st-${c.estado}">${plata({ id: c.banioId, estado: c.estado })}<h2>${esc(c.direccion || c.nombreBanio)}</h2>${chipEstado(c.estado)}</div>`;
  let cuerpo;
  if (c.usandoBanio) {
    cuerpo = `<div class="dentro st-LIBRE">${icon('door', 'lg')}Es tu turno. Pasá.</div>
      <p class="sub">Estás usando el baño. Cuando salgas, el próximo de la cola entra.</p>
      <label class="fila-sw"><span>Quedó sucio: avisar a mantenimiento</span><input class="sw" type="checkbox" role="switch" id="chk-sucio" ${state.sucio ? 'checked' : ''}></label>
      <button class="btn prominent" data-act="salir-banio">Salir del baño</button>`;
  } else {
    const turno = `<div><div class="etq">Tu posición</div><div class="pos"><small>#</small>${c.posicion}</div></div>
         <div class="datos"><div class="dato"><div class="etq">Personas delante</div><b>${c.personasDelante}</b></div>
         <div class="dato"><div class="etq">Espera estimada</div><b>${c.tiempoEstimadoMin} min</b></div></div>`;
    cuerpo = `${turno}${avisoMantenimiento(c)}<button class="btn plain" data-act="salir-cola">Salir de la cola</button>`;
  }
  setHtml(el, `<div class="cola-card st-${c.estado}">${cab}${cuerpo}</div>`);
}

// ----------------------------------------------------------------- mapa
function iniciarMapa() {
  if (state.map) return;
  if (typeof L === 'undefined') { $('mapa').innerHTML = '<div class="vacio" style="margin:96px 16px">' + icon('map') + '<h2>No se pudo cargar el mapa</h2><p>Revisá tu conexión. La lista funciona igual.</p></div>'; return; }
  state.map = L.map('mapa', { zoomControl: false }).setView([state.origen.lat, state.origen.lng], 16);
  L.control.zoom({ position: 'topright', zoomInTitle: 'Acercar', zoomOutTitle: 'Alejar' }).addTo(state.map);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; colaboradores de OpenStreetMap' }).addTo(state.map);
  state.map.on('click', e => { if (state.seleccion != null) { state.seleccion = null; renderMapa(); renderPeek(); } else setOrigen(e.latlng.lat, e.latlng.lng, false); });
}
const pinIcon = (e, sel, id) => L.divIcon({ className: 'pin-wrap', iconSize: [44, 44], iconAnchor: [22, 22],
  html: `<div class="pin st-${e} ${sel ? 'sel' : ''}"><b>${id}</b></div>` });

function renderMapa() {
  if (!state.map) return;
  const vistos = new Set();
  for (const b of state.banios) {
    vistos.add(b.id);
    const ic = pinIcon(b.estado, state.seleccion === b.id, b.id);
    let m = state.markers[b.id];
    if (!m) {
      m = L.marker([b.latitud, b.longitud], { icon: ic, title: `${b.nombre}, ${ESTADOS[b.estado].texto}`, alt: b.nombre }).addTo(state.map);
      m.on('click', () => { state.seleccion = b.id; renderMapa(); renderPeek(); });
      state.markers[b.id] = m;
    } else { m.setIcon(ic); m.options.title = `${b.nombre}, ${ESTADOS[b.estado].texto}`; }
  }
  for (const id of Object.keys(state.markers)) if (!vistos.has(Number(id))) { state.map.removeLayer(state.markers[id]); delete state.markers[id]; }
  const pos = [state.origen.lat, state.origen.lng];
  const yo = L.divIcon({ className: 'pin-wrap', iconSize: [44, 44], iconAnchor: [22, 22], html: `<div class="pin yo">${icon('locate')}</div>` });
  if (!state.yoMarker) state.yoMarker = L.marker(pos, { icon: yo, zIndexOffset: 1000, interactive: false, keyboard: false }).addTo(state.map);
  else state.yoMarker.setLatLng(pos);
}

function renderPeek() {
  const el = $('peek'), b = state.banios.find(x => x.id === state.seleccion);
  if (!b || state.vista !== 'mapa') { el.hidden = true; return; }
  el.hidden = false;
  setHtml(el, `
    <button class="icon-btn peek-x" data-act="peek-cerrar" aria-label="Cerrar resumen">${icon('close')}</button>
    <div class="card-top st-${b.estado}">${plata(b)}<h3>${esc(b.direccion || b.nombre)}</h3>${chipEstado(b.estado)}<p class="sub">${b.distanciaMetros != null ? 'a ' + fmtDist(b.distanciaMetros) + ', ' : ''}${b.personasEsperando} en cola</p></div>
    <div class="hero"><div class="espera">${espera(b.tiempoEstimadoMin)}</div></div>
    <div class="acciones"><button class="btn tinted" data-act="detalle" data-id="${b.id}">Detalles</button>${botonIngresar(b, 'prominent')}</div>`);
}

// ------------------------------------------------------------ ubicación
function setOrigen(lat, lng, real, tipo = real ? 'real' : 'mapa') {
  state.origen = { lat, lng, real };
  $('ubic-txt').textContent = { real: 'Usando tu ubicación actual', mapa: 'Punto elegido en el mapa', ejemplo: 'Ubicación de ejemplo (Plaza de Mayo)' }[tipo];
  if (state.map && real) state.map.setView([lat, lng]);
  refrescar();
}
function usarMiUbicacion() {
  if (!navigator.geolocation) return aviso('Tu navegador no ofrece geolocalización', 'error', 'alert');
  navigator.geolocation.getCurrentPosition(p => setOrigen(p.coords.latitude, p.coords.longitude, true),
    () => aviso('No pudimos obtener tu ubicación. Revisá el permiso del navegador.', 'error', 'alert'), { timeout: 8000 });
}

// --------------------------------------------------------------- hojas
function sheetHtml({ titulo, izq = '', der = '', cuerpo, clase = '' }) {
  return `<div class="sheet-in ${clase}"><div class="grabber-zone"><div class="grabber"></div></div>
    <div class="sheet-bar"><div class="izq">${izq}</div>${titulo ? `<h2>${titulo}</h2>` : "<span></span>"}<div class="der">${der}</div></div>
    <div class="sheet-body">${cuerpo}</div></div>`;
}
const cerrarBtn = `<button class="icon-btn" data-act="cerrar-sheet" aria-label="Cerrar">${icon('close')}</button>`;

async function pintarDetalle(id) {
  const b = await api(`banios/${id}`, 'GET', { lat: state.origen.lat, lng: state.origen.lng, personaId: state.personaId });
  setHtml($('sheet-detalle'), sheetHtml({
    titulo: esc(b.nombre), izq: cerrarBtn,
    cuerpo: `<div class="det-hero st-${b.estado}">${plata(b)}<div class="espera">${espera(b.tiempoEstimadoMin)}</div>${chipEstado(b.estado)}</div>
      <div class="gauge-wrap">${gauge(b.personasEsperando, b.capacidad, b.estado)}<span class="gauge-txt">${b.personasEsperando} de ${b.capacidad}</span></div>
      ${avisoMantenimiento(b)}
      <div class="grupo"><dl class="det-lista" style="margin:0">
        <div><dt>Dirección</dt><dd>${esc(b.direccion || 'Sin dato')}</dd></div>
        <div><dt>Distancia</dt><dd>${b.distanciaMetros != null ? fmtDist(b.distanciaMetros) : 'Sin dato'}</dd></div>
        <div><dt>Capacidad de la cola</dt><dd>${b.capacidad} personas</dd></div>
        <div><dt>Funcionalidades</dt><dd><ul class="feats" style="justify-content:flex-end">${feats(b.funcionalidades)}</ul></dd></div>
      </dl></div>
      ${botonIngresar(b, 'prominent block')}`
  }));
}
async function verDetalle(id) {
  try { state.detalleId = id; await pintarDetalle(id); abrir($('sheet-detalle')); }
  catch (e) { aviso(e.message, 'error', 'alert'); }
}
function abrir(d) { if (!d.open) d.showModal(); }
function cerrarTodas() { document.querySelectorAll('dialog[open]').forEach(d => d.close()); }

function confirmarSalirDeCola() {
  const c = state.miCola; if (!c) return;
  setHtml($('sheet-confirmar'), sheetHtml({ titulo: '', clase: 'confirmar', cuerpo: `
    <h2>¿Salir de la cola?</h2>
    <p>Vas a perder tu lugar (#${c.posicion}) en el ${esc(c.nombreBanio)}. Para volver a entrar tendrías que empezar desde el final.</p>
    <button class="btn prominent block" data-act="cerrar-sheet">Seguir esperando</button>
    <button class="btn danger block" data-act="confirmar-salir">Salir de la cola</button>` }));
  abrir($('sheet-confirmar'));
}

/** Gesto de cerrar una hoja arrastrándola hacia abajo, desde el agarrador. */
function habilitarArrastre(d) {
  let y0 = null, dy = 0;
  d.addEventListener('pointerdown', e => { if (!e.target.closest('.grabber-zone')) return; y0 = e.clientY; dy = 0; d.setPointerCapture(e.pointerId); d.style.animation = 'none'; });
  d.addEventListener('pointermove', e => { if (y0 == null) return; dy = Math.max(0, e.clientY - y0); d.style.transform = `translateY(${dy}px)`; });
  const fin = () => { if (y0 == null) return; const cerrar = dy > 100; y0 = null; d.style.transform = ''; d.style.animation = ''; if (cerrar) d.close(); };
  d.addEventListener('pointerup', fin); d.addEventListener('pointercancel', fin);
  d.addEventListener('click', e => { if (e.target === d) d.close(); });          // tocar el fondo también cierra
  d.addEventListener('close', () => { if (d.id === 'sheet-detalle') state.detalleId = null; });
}

// -------------------------------------------------------------- acciones
async function accion(fn, okMsg, btn) {
  if (btn) { btn.disabled = true; btn.setAttribute('aria-busy', 'true'); }
  try { await fn(); if (okMsg) aviso(okMsg, '', 'check'); }
  catch (e) { aviso(e.message, 'error', 'alert'); }
  await refrescar();
}
const post = (ruta, params) => api(ruta, 'POST', { personaId: state.personaId, ...params });

const ACCIONES = {
  tab: d => mostrarVista(d.vista),
  filtros: () => { sincronizarFiltros(); abrir($('sheet-filtros')); },
  'cerrar-sheet': () => cerrarTodas(),
  'reset-filtros': () => { state.filtros.distanciaMax = ''; state.filtros.funcionalidades.clear(); state.filtros.estados.clear(); sincronizarFiltros(); refrescar(); },
  quitar: d => {
    if (d.k === 'dist') state.filtros.distanciaMax = ''; else if (d.k === 'func') state.filtros.funcionalidades.delete(d.v); else state.filtros.estados.delete(d.v);
    sincronizarFiltros(); refrescar();
  },
  geo: () => usarMiUbicacion(),
  ejemplo: () => setOrigen(ORIGEN_EJEMPLO.lat, ORIGEN_EJEMPLO.lng, false, 'ejemplo'),
  detalle: d => verDetalle(Number(d.id)),
  'peek-cerrar': () => { state.seleccion = null; renderMapa(); renderPeek(); },
  ingresar: (d, btn) => accion(async () => { await post(`banios/${d.id}/cola`); cerrarTodas(); state.seleccion = null; mostrarVista('cola'); }, null, btn),
  'salir-cola': () => confirmarSalirDeCola(),
  'confirmar-salir': (d, btn) => { cerrarTodas(); return accion(() => post('cola/salir'), 'Saliste de la cola', btn); },
  'salir-banio': (d, btn) => {
    const sucio = state.sucio; state.sucio = false;
    return accion(async () => { await post('uso/salir', { higiene: sucio ? 'mala' : 'ok' }); mostrarVista('lista'); },
      sucio ? 'Gracias por avisar: mantenimiento va en camino' : 'Gracias por usar el sistema', btn);
  }
};

// --------------------------------------------------------------- vistas
const obs = new IntersectionObserver(([en]) => { if (state.vista !== 'mapa') $('bar').classList.toggle('compact', !en.isIntersecting); },
  { rootMargin: `-${56 + 8}px 0px 0px 0px` });

function mostrarVista(v) {
  state.vista = v;
  for (const k of Object.keys(TITULOS)) $('vista-' + k).hidden = k !== v;
  $('bar-title').textContent = TITULOS[v];
  $('btn-filtros').hidden = v === 'cola';
  obs.disconnect();
  if (v === 'mapa') { $('bar').classList.add('compact'); iniciarMapa(); if (state.map) setTimeout(() => { state.map.invalidateSize(); renderMapa(); }, 50); }
  else { $('bar').classList.remove('compact'); obs.observe($(v === 'lista' ? 'titulo-lista' : 'titulo-cola')); }
  window.scrollTo(0, 0);
  renderTabs(); renderPeek();
}

// ------------------------------------------------------- filtros (hoja)
function sincronizarFiltros() {
  document.querySelectorAll('input[name=dist]').forEach(r => { r.checked = r.value === state.filtros.distanciaMax; });
  document.querySelectorAll('[data-func]').forEach(c => { c.checked = state.filtros.funcionalidades.has(c.dataset.func); });
  document.querySelectorAll('[data-estado]').forEach(c => { c.checked = state.filtros.estados.has(c.dataset.estado); });
  $('criterio').value = state.filtros.criterio;
}

// ---------------------------------------------------------------- avisos
function aviso(msg, tipo = '', ic = 'check') {
  const t = document.createElement('div');
  t.className = 'aviso-toast ' + tipo;
  if (tipo === 'error') t.setAttribute('role', 'alert');
  t.innerHTML = `${icon(ic)}<span>${esc(msg)}</span>`;
  $('avisos').appendChild(t);
  setTimeout(() => t.remove(), tipo === 'turno' ? 9000 : 5000);
}

// ------------------------------------------------------------------ init
async function init() {
  // delegación de eventos: un solo listener para todos los botones con data-act
  document.addEventListener('click', e => {
    const el = e.target.closest('[data-act]');
    if (!el || el.disabled) return;
    const fn = ACCIONES[el.dataset.act];
    if (fn) fn(el.dataset, el);
  });
  document.addEventListener('change', e => {
    const t = e.target;
    if (t.id === 'chk-sucio') { state.sucio = t.checked; return; }
    if (t.id === 'criterio') state.filtros.criterio = t.value;
    else if (t.name === 'dist') state.filtros.distanciaMax = t.value;
    else if (t.dataset.func) { t.checked ? state.filtros.funcionalidades.add(t.dataset.func) : state.filtros.funcionalidades.delete(t.dataset.func); }
    else if (t.dataset.estado) { t.checked ? state.filtros.estados.add(t.dataset.estado) : state.filtros.estados.delete(t.dataset.estado); }
    else return;
    refrescar();
  });
  document.querySelectorAll('dialog.sheet').forEach(habilitarArrastre);
  $('ubic-txt').textContent = 'Ubicación de ejemplo (Plaza de Mayo)';
  mostrarVista('lista');
  render();

  try {
    const ev = await api('evento');
    $('evento').textContent = ev.nombre;
    state.personaId = await asegurarPersona();
  } catch (e) {
    $('evento').textContent = 'Sin conexión con el servidor';
    state.cargado = true; render();
  }
  await refrescar();
  setInterval(refrescar, POLL_MS);
}
window.addEventListener('DOMContentLoaded', init);
