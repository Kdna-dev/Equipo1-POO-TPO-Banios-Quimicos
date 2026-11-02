'use strict';
const ESTADOS = {
  LIBRE: ['check', 'Libre'], OCUPADO: ['user', 'Ocupado'], SATURADO: ['alert', 'Saturado'],
  MANTENIMIENTO: ['wrench', 'En mantenimiento'], FUERA_DE_SERVICIO: ['slash', 'Fuera de servicio']
};
const FUNC = { MOVILIDAD_REDUCIDA: 'Movilidad reducida', LAVAMANOS: 'Lavamanos', DISPENSER_ALCOHOL: 'Alcohol', CAMBIADOR_BEBES: 'Cambiador', ESPEJO: 'Espejo' };
const $ = id => document.getElementById(id);
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const icon = (id, cls = '') => `<svg class="ic ${cls}" aria-hidden="true"><use href="icons.svg#i-${id}"/></svg>`;

async function api(ruta, metodo = 'GET', params = {}) {
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== null && v !== '') qs.set(k, v);
  const r = await fetch('/api/' + ruta + (qs.toString() ? '?' + qs : ''), { method: metodo });
  const data = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(data.error || ('Error ' + r.status));
  return data;
}

function aviso(msg, tipo = '') {
  const t = document.createElement('div');
  t.className = 'aviso-toast ' + tipo;
  if (tipo === 'error') t.setAttribute('role', 'alert');
  t.innerHTML = `${icon(tipo === 'error' ? 'alert' : 'check')}<span>${esc(msg)}</span>`;
  $('avisos').appendChild(t);
  setTimeout(() => t.remove(), 5000);
}

let ultimo = '';
async function cargar() {
  try {
    const [banios, ev] = await Promise.all([api('admin/banios'), api('evento')]);
    const html = banios.map(b => {
      const [ic, texto] = ESTADOS[b.estado];
      const btn = (tipo, label) => `<button class="btn tinted" data-tipo="${tipo}" data-id="${b.id}">${label}</button>`;
      return `<tr>
        <th scope="row"><span class="plata mini st-${b.estado}" aria-label="Baño ${b.id}"><b>${b.id}</b></span></th>
        <td>${esc(b.direccion)}</td>
        <td><span class="estado st-${b.estado}">${icon(ic, 'sm')}${texto}</span></td>
        <td>${b.personasEsperando} / ${b.capacidad}</td>
        <td>${b.funcionalidades.map(f => FUNC[f]).join(', ') || 'Ninguna'}</td>
        <td class="acc">${btn('INICIAR_MANTENIMIENTO', 'Mantenimiento')}${btn('FINALIZAR_MANTENIMIENTO', 'Terminar mant.')}${btn('DESCOMPONER', 'Descomponer')}${btn('REPARAR', 'Reparar')}
          <button class="btn tinted" data-simular="${b.id}">+3 personas</button><button class="btn danger" data-baja="${b.id}">Dar de baja</button></td></tr>`;
    }).join('');
    if (html !== ultimo) { ultimo = html; $('filas').innerHTML = html; }
    $('evento').textContent = `${ev.nombre}. ${ev.resumen.baniosOperativos} baños operativos y ${ev.resumen.personasEsperando} personas esperando.`;
  } catch (e) { $('evento').textContent = 'Sin conexión con el servidor'; }
}

async function ejecutar(fn, ok) {
  try { await fn(); if (ok) aviso(ok); } catch (e) { aviso(e.message, 'error'); }
  cargar();
}

document.addEventListener('click', e => {
  const b = e.target.closest('button'); if (!b) return;
  if (b.dataset.tipo) ejecutar(() => api(`admin/banios/${b.dataset.id}/accion`, 'POST', { tipo: b.dataset.tipo }), 'Listo');
  else if (b.dataset.simular) ejecutar(() => api(`admin/banios/${b.dataset.simular}/simular`, 'POST', { cantidad: 3 }), 'Personas de ejemplo agregadas');
  else if (b.dataset.baja && confirm(`¿Dar de baja el baño #${b.dataset.baja}? No se puede deshacer.`))
    ejecutar(() => api(`admin/banios/${b.dataset.baja}/baja`, 'POST'), 'Baño dado de baja');
});

$('alta').addEventListener('submit', e => {
  e.preventDefault();
  const f = new FormData(e.target);
  ejecutar(() => api('admin/banios', 'POST', {
    direccion: f.get('direccion'), lat: f.get('lat'), lng: f.get('lng'), capacidad: f.get('capacidad'),
    tiempoPromedio: f.get('tiempoPromedio'), funcionalidades: f.getAll('f').join(',')
  }), 'Baño creado');
});

cargar();
setInterval(cargar, 3000);
