/* ============================================================
   LumiSound Admin Panel — app.js
   ============================================================ */

// ── db init ────────────────────────────────────────────
const SUPABASE_URL = 'https://dmwnegtvotnrajzpyfad.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRtd25lZ3R2b3RucmFqenB5ZmFkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI1MDc5MzMsImV4cCI6MjA3ODA4MzkzM30.urUPVr7kS1JPeFU7IXHrSciwE_7vUm4B-2EiSKcu3P8';
const db = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ── In-memory data cache ─────────────────────────────────────
const cache = {
  users: [],
  artists: [],
  tracks: [],
  ratings: [],
  comments: [],
  playlists: [],
  reports: [],
  bans: [],
  appeals: [],
};

// ── Utility: format date ─────────────────────────────────────
function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

// ── Utility: format duration ─────────────────────────────────
function formatDuration(seconds) {
  if (!seconds && seconds !== 0) return '—';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

// ── Utility: escape HTML ─────────────────────────────────────
function esc(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ── Toast notifications ──────────────────────────────────────
let toastTimer = null;
function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = `toast ${type}`;
  toast.classList.remove('hidden');
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.add('hidden'), 3000);
}

// ── Modal ────────────────────────────────────────────────────
let modalSaveCallback = null;

function openModal(title, bodyHTML, onSave) {
  document.getElementById('modal-title').textContent = title;
  document.getElementById('modal-body').innerHTML = bodyHTML;
  modalSaveCallback = onSave;
  document.getElementById('modal-overlay').classList.remove('hidden');
}

function closeModal() {
  document.getElementById('modal-overlay').classList.add('hidden');
  modalSaveCallback = null;
}

// ── Confirm delete ───────────────────────────────────────────
let confirmCallback = null;

function confirmDelete(text, onConfirm) {
  document.getElementById('confirm-text').textContent = text;
  confirmCallback = onConfirm;
  document.getElementById('confirm-overlay').classList.remove('hidden');
}

function closeConfirm() {
  document.getElementById('confirm-overlay').classList.add('hidden');
  confirmCallback = null;
}

// ── Sanitize filename — keep only extension, use timestamp as name ────────
function sanitizeFileName(originalName) {
  const parts = originalName.split('.');
  const ext = parts.length > 1 ? '.' + parts.pop().toLowerCase() : '';
  return Date.now() + ext;
}

// ── Upload file to db Storage ─────────────────────────
async function uploadFile(file, bucket, pathPrefix, progressBarEl) {
  if (!file) return null;

  // Use only timestamp + extension — avoids "Invalid key" errors with Cyrillic/special chars
  const ext = file.name.includes('.') ? '.' + file.name.split('.').pop().toLowerCase() : '';
  const safePath = pathPrefix + Date.now() + ext;

  if (progressBarEl) {
    progressBarEl.style.width = '30%';
  }

  const { data, error } = await db.storage
    .from(bucket)
    .upload(safePath, file, { upsert: true });

  if (error) {
    showToast('Ошибка загрузки файла: ' + error.message, 'error');
    return null;
  }

  if (progressBarEl) {
    progressBarEl.style.width = '100%';
  }

  const { data: urlData } = db.storage.from(bucket).getPublicUrl(data.path);
  return urlData.publicUrl;
}

// ── Auth ─────────────────────────────────────────────────────
const ADMIN_PASSWORD = '3952mir!';

function login() {
  const passwordEl = document.getElementById('login-password');
  const errorEl = document.getElementById('login-error');
  const debugEl = document.getElementById('login-debug');
  const password = passwordEl ? passwordEl.value : '';

  errorEl.textContent = '';

  const entered = JSON.stringify(password);
  const expected = JSON.stringify(ADMIN_PASSWORD);
  const match = password === ADMIN_PASSWORD;

  console.log('login() called');
  console.log('entered:', entered, 'length:', password.length);
  console.log('expected:', expected, 'length:', ADMIN_PASSWORD.length);
  console.log('match:', match);

  if (debugEl) {
    debugEl.textContent = `Введено: ${entered} (${password.length} симв.) | Ожидается: ${expected} | Совпадение: ${match}`;
  }

  if (match) {
    sessionStorage.setItem('admin_auth', '1');
    showApp();
  } else {
    errorEl.textContent = 'Неверный пароль';
  }
}

function logout() {
  sessionStorage.removeItem('admin_auth');
  document.getElementById('app-screen').classList.add('hidden');
  document.getElementById('login-screen').classList.remove('hidden');
  const pw = document.getElementById('login-password');
  if (pw) pw.value = '';
}

function showApp() {
  document.getElementById('login-screen').classList.add('hidden');
  document.getElementById('app-screen').classList.remove('hidden');
  navigateTo('dashboard');
}

// ── Navigation ───────────────────────────────────────────────
function navigateTo(page) {
  // Update nav items
  document.querySelectorAll('.nav-item').forEach(el => {
    el.classList.toggle('active', el.dataset.page === page);
  });

  // Show/hide pages
  document.querySelectorAll('.page').forEach(el => {
    el.classList.add('hidden');
  });
  const pageEl = document.getElementById(`page-${page}`);
  if (pageEl) pageEl.classList.remove('hidden');

  // Load page data
  switch (page) {
    case 'dashboard': loadDashboard(); break;
    case 'users':     loadUsers();     break;
    case 'artists':   loadArtists();   break;
    case 'tracks':    loadTracks();    break;
    case 'ratings':   loadRatings();   break;
    case 'comments':  loadComments();  break;
    case 'playlists': loadPlaylists(); break;
    case 'reports':   loadReports();   break;
    case 'bans':      loadBans();      break;
    case 'appeals':   loadAppeals();   break;
  }
}

// ── Dashboard ────────────────────────────────────────────────
async function loadDashboard() {
  const tables = [
    { id: 'stat-users',    table: 'profiles' },
    { id: 'stat-artists',  table: 'custom_artists' },
    { id: 'stat-tracks',   table: 'custom_tracks' },
    { id: 'stat-ratings',  table: 'track_ratings' },
    { id: 'stat-comments', table: 'track_comments' },
    { id: 'stat-playlists',table: 'playlists' },
  ];

  await Promise.all(tables.map(async ({ id, table }) => {
    const { count, error } = await db
      .from(table)
      .select('*', { count: 'exact', head: true });
    document.getElementById(id).textContent = error ? '?' : count;
  }));
}

// ── Users ────────────────────────────────────────────────────
async function loadUsers() {
  const tbody = document.querySelector('#table-users tbody');
  tbody.innerHTML = '<tr><td colspan="7" class="loading">Загрузка...</td></tr>';

  const { data, error } = await db
    .from('profiles')
    .select('*')
    .order('created_at', { ascending: false });

  if (error) {
    tbody.innerHTML = `<tr><td colspan="7" class="loading">Ошибка: ${esc(error.message)}</td></tr>`;
    return;
  }

  cache.users = data || [];
  renderUsers(cache.users);
}

function renderUsers(users) {
  const tbody = document.querySelector('#table-users tbody');
  if (!users.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="loading">Нет данных</td></tr>';
    return;
  }

  tbody.innerHTML = users.map(u => `
    <tr>
      <td class="cell-avatar">
        ${u.avatar_url
          ? `<img src="${esc(u.avatar_url)}" alt="" />`
          : `<div class="avatar-placeholder">👤</div>`}
      </td>
      <td>${esc(u.username) || '<span class="text-muted">—</span>'}</td>
      <td class="text-muted">${esc(u.email) || '—'}</td>
      <td class="text-truncate text-muted">${esc(u.bio) || '—'}</td>
      <td>${u.is_public
        ? '<span class="badge badge-green">Да</span>'
        : '<span class="badge badge-gray">Нет</span>'}</td>
      <td class="text-muted">${formatDate(u.created_at)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" title="Редактировать" onclick="editUser('${esc(u.id)}')">✏️</button>
          <button class="btn-icon danger" title="Удалить" onclick="deleteUser('${esc(u.id)}', '${esc(u.username || u.email)}')">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function editUser(id) {
  const u = cache.users.find(x => x.id === id);
  if (!u) return;

  const body = `
    <div class="form-group">
      <label>Username</label>
      <input id="edit-username" type="text" value="${esc(u.username || '')}" />
    </div>
    <div class="form-group">
      <label>Bio</label>
      <textarea id="edit-bio" rows="3">${esc(u.bio || '')}</textarea>
    </div>
    <div class="checkbox-row">
      <input id="edit-is-public" type="checkbox" ${u.is_public ? 'checked' : ''} />
      <label for="edit-is-public">Публичный профиль</label>
    </div>
  `;

  openModal('Редактировать пользователя', body, async () => {
    const updates = {
      username:  document.getElementById('edit-username').value.trim(),
      bio:       document.getElementById('edit-bio').value.trim(),
      is_public: document.getElementById('edit-is-public').checked,
    };

    const { error } = await db.from('profiles').update(updates).eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }

    showToast('Пользователь обновлён');
    closeModal();
    loadUsers();
  });
}

function deleteUser(id, name) {
  confirmDelete(`Удалить пользователя «${name}»? Это действие необратимо.`, async () => {
    const { error } = await db.from('profiles').delete().eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    showToast('Пользователь удалён');
    closeConfirm();
    loadUsers();
  });
}

// ── Artists ──────────────────────────────────────────────────
async function loadArtists() {
  const tbody = document.querySelector('#table-artists tbody');
  tbody.innerHTML = '<tr><td colspan="8" class="loading">Загрузка...</td></tr>';

  const [artistsRes, tracksRes] = await Promise.all([
    db.from('custom_artists').select('*').order('created_at', { ascending: false }),
    db.from('custom_tracks').select('artist_id'),
  ]);

  if (artistsRes.error) {
    tbody.innerHTML = `<tr><td colspan="8" class="loading">Ошибка: ${esc(artistsRes.error.message)}</td></tr>`;
    return;
  }

  // Build track count map
  const trackCountMap = {};
  (tracksRes.data || []).forEach(t => {
    trackCountMap[t.artist_id] = (trackCountMap[t.artist_id] || 0) + 1;
  });

  cache.artists = (artistsRes.data || []).map(a => ({
    ...a,
    track_count: trackCountMap[a.id] || 0,
  }));

  renderArtists(cache.artists);
}

function renderArtists(artists) {
  const tbody = document.querySelector('#table-artists tbody');
  if (!artists.length) {
    tbody.innerHTML = '<tr><td colspan="8" class="loading">Нет данных</td></tr>';
    return;
  }

  tbody.innerHTML = artists.map(a => `
    <tr>
      <td class="cell-cover">
        ${a.avatar_url
          ? `<img src="${esc(a.avatar_url)}" alt="" />`
          : `<div class="cover-placeholder">🎤</div>`}
      </td>
      <td>${esc(a.name)}</td>
      <td class="text-muted">${esc(a.genre) || '—'}</td>
      <td class="text-muted">${esc(a.location) || '—'}</td>
      <td>${a.is_verified
        ? '<span class="badge badge-blue">✓ Верифицирован</span>'
        : '<span class="badge badge-gray">Нет</span>'}</td>
      <td>${a.track_count}</td>
      <td class="text-muted">${formatDate(a.created_at)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" title="Редактировать" onclick="editArtist('${esc(a.id)}')">✏️</button>
          <button class="btn-icon danger" title="Удалить" onclick="deleteArtist('${esc(a.id)}', '${esc(a.name)}')">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function artistFormHTML(a = {}) {
  return `
    <div class="form-row">
      <div class="form-group">
        <label>Имя *</label>
        <input id="af-name" type="text" value="${esc(a.name || '')}" required />
      </div>
      <div class="form-group">
        <label>Жанр</label>
        <input id="af-genre" type="text" value="${esc(a.genre || '')}" />
      </div>
    </div>
    <div class="form-group">
      <label>Bio</label>
      <textarea id="af-bio" rows="3">${esc(a.bio || '')}</textarea>
    </div>
    <div class="form-group">
      <label>Локация</label>
      <input id="af-location" type="text" value="${esc(a.location || '')}" />
    </div>
    <div class="checkbox-row">
      <input id="af-verified" type="checkbox" ${a.is_verified ? 'checked' : ''} />
      <label for="af-verified">Верифицирован</label>
    </div>
    <div class="form-group">
      <label>Аватар</label>
      <div class="upload-area" id="af-avatar-area" onclick="document.getElementById('af-avatar-input').click()">
        📷 Нажмите для загрузки аватара
        ${a.avatar_url ? `<img class="upload-preview" src="${esc(a.avatar_url)}" />` : ''}
      </div>
      <input id="af-avatar-input" type="file" accept="image/*" class="hidden" />
      <div class="progress-bar" id="af-avatar-progress" style="display:none"><div class="progress-fill" id="af-avatar-fill" style="width:0%"></div></div>
    </div>
    <div class="form-group">
      <label>Обложка (cover)</label>
      <div class="upload-area" id="af-cover-area" onclick="document.getElementById('af-cover-input').click()">
        🖼️ Нажмите для загрузки обложки
        ${a.cover_url ? `<img class="upload-preview" src="${esc(a.cover_url)}" />` : ''}
      </div>
      <input id="af-cover-input" type="file" accept="image/*" class="hidden" />
      <div class="progress-bar" id="af-cover-progress" style="display:none"><div class="progress-fill" id="af-cover-fill" style="width:0%"></div></div>
    </div>
  `;
}

function bindArtistFormUploads() {
  const avatarInput = document.getElementById('af-avatar-input');
  const coverInput  = document.getElementById('af-cover-input');

  avatarInput?.addEventListener('change', () => {
    const file = avatarInput.files[0];
    if (!file) return;
    const area = document.getElementById('af-avatar-area');
    area.classList.add('has-file');
    area.innerHTML = `📷 ${esc(file.name)}<img class="upload-preview" src="${URL.createObjectURL(file)}" />`;
  });

  coverInput?.addEventListener('change', () => {
    const file = coverInput.files[0];
    if (!file) return;
    const area = document.getElementById('af-cover-area');
    area.classList.add('has-file');
    area.innerHTML = `🖼️ ${esc(file.name)}<img class="upload-preview" src="${URL.createObjectURL(file)}" />`;
  });
}

function openNewArtist() {
  openModal('Новый артист', artistFormHTML(), async () => {
    const name = document.getElementById('af-name').value.trim();
    if (!name) { showToast('Имя обязательно', 'error'); return; }

    const avatarFile = document.getElementById('af-avatar-input').files[0];
    const coverFile  = document.getElementById('af-cover-input').files[0];

    let avatar_url = null;
    let cover_url  = null;

    if (avatarFile) {
      document.getElementById('af-avatar-progress').style.display = 'block';
      avatar_url = await uploadFile(avatarFile, 'admin-uploads', 'artists/avatar_', document.getElementById('af-avatar-fill'));
    }
    if (coverFile) {
      document.getElementById('af-cover-progress').style.display = 'block';
      cover_url = await uploadFile(coverFile, 'admin-uploads', 'artists/cover_', document.getElementById('af-cover-fill'));
    }

    const payload = {
      name,
      bio:         document.getElementById('af-bio').value.trim() || null,
      genre:       document.getElementById('af-genre').value.trim() || null,
      location:    document.getElementById('af-location').value.trim() || null,
      is_verified: document.getElementById('af-verified').checked,
      avatar_url,
      cover_url,
    };

    const { error } = await db.from('custom_artists').insert(payload);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }

    showToast('Артист создан');
    closeModal();
    loadArtists();
  });

  setTimeout(bindArtistFormUploads, 50);
}

function editArtist(id) {
  const a = cache.artists.find(x => x.id === id);
  if (!a) return;

  openModal('Редактировать артиста', artistFormHTML(a), async () => {
    const name = document.getElementById('af-name').value.trim();
    if (!name) { showToast('Имя обязательно', 'error'); return; }

    const avatarFile = document.getElementById('af-avatar-input').files[0];
    const coverFile  = document.getElementById('af-cover-input').files[0];

    let avatar_url = a.avatar_url;
    let cover_url  = a.cover_url;

    if (avatarFile) {
      document.getElementById('af-avatar-progress').style.display = 'block';
      avatar_url = await uploadFile(avatarFile, 'admin-uploads', 'artists/avatar_', document.getElementById('af-avatar-fill'));
    }
    if (coverFile) {
      document.getElementById('af-cover-progress').style.display = 'block';
      cover_url = await uploadFile(coverFile, 'admin-uploads', 'artists/cover_', document.getElementById('af-cover-fill'));
    }

    const updates = {
      name,
      bio:         document.getElementById('af-bio').value.trim() || null,
      genre:       document.getElementById('af-genre').value.trim() || null,
      location:    document.getElementById('af-location').value.trim() || null,
      is_verified: document.getElementById('af-verified').checked,
      avatar_url,
      cover_url,
    };

    const { error } = await db.from('custom_artists').update(updates).eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }

    showToast('Артист обновлён');
    closeModal();
    loadArtists();
  });

  setTimeout(bindArtistFormUploads, 50);
}

function deleteArtist(id, name) {
  confirmDelete(`Удалить артиста «${name}»?`, async () => {
    const { error } = await db.from('custom_artists').delete().eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    showToast('Артист удалён');
    closeConfirm();
    loadArtists();
  });
}

// ── Tracks ───────────────────────────────────────────────────
async function loadTracks() {
  const tbody = document.querySelector('#table-tracks tbody');
  tbody.innerHTML = '<tr><td colspan="9" class="loading">Загрузка...</td></tr>';

  const [tracksRes, artistsRes] = await Promise.all([
    db.from('custom_tracks').select('*').order('created_at', { ascending: false }),
    db.from('custom_artists').select('id, name'),
  ]);

  if (tracksRes.error) {
    tbody.innerHTML = `<tr><td colspan="9" class="loading">Ошибка: ${esc(tracksRes.error.message)}</td></tr>`;
    return;
  }

  // Build artist name map
  const artistMap = {};
  (artistsRes.data || []).forEach(a => { artistMap[a.id] = a.name; });

  cache.tracks = (tracksRes.data || []).map(t => ({
    ...t,
    artist_name: artistMap[t.artist_id] || '—',
  }));

  // Also cache artists for the form select
  if (artistsRes.data) cache.artists = artistsRes.data;

  renderTracks(cache.tracks);
}

function renderTracks(tracks) {
  const tbody = document.querySelector('#table-tracks tbody');
  if (!tracks.length) {
    tbody.innerHTML = '<tr><td colspan="9" class="loading">Нет данных</td></tr>';
    return;
  }

  tbody.innerHTML = tracks.map(t => `
    <tr>
      <td class="cell-cover">
        ${t.cover_url
          ? `<img src="${esc(t.cover_url)}" alt="" />`
          : `<div class="cover-placeholder">🎵</div>`}
      </td>
      <td>${esc(t.title)}</td>
      <td class="text-muted">${esc(t.artist_name)}</td>
      <td class="text-muted">${esc(t.genre) || '—'}</td>
      <td class="text-muted">${formatDuration(t.duration)}</td>
      <td class="text-muted">${t.play_count || 0}</td>
      <td>${t.is_published
        ? '<span class="badge badge-green">Да</span>'
        : '<span class="badge badge-gray">Нет</span>'}</td>
      <td class="text-muted">${formatDate(t.created_at)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" title="Редактировать" onclick="editTrack('${esc(t.id)}')">✏️</button>
          <button class="btn-icon danger" title="Удалить" onclick="deleteTrack('${esc(t.id)}', '${esc(t.title)}')">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function artistSelectOptions(selectedId) {
  return cache.artists.map(a =>
    `<option value="${esc(a.id)}" ${a.id === selectedId ? 'selected' : ''}>${esc(a.name)}</option>`
  ).join('');
}

function trackFormHTML(t = {}) {
  return `
    <div class="form-row">
      <div class="form-group">
        <label>Название *</label>
        <input id="tf-title" type="text" value="${esc(t.title || '')}" required />
      </div>
      <div class="form-group">
        <label>Жанр</label>
        <input id="tf-genre" type="text" value="${esc(t.genre || '')}" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>Артист</label>
        <select id="tf-artist">
          <option value="">— Выберите артиста —</option>
          ${artistSelectOptions(t.artist_id)}
        </select>
      </div>
      <div class="form-group">
        <label>Длительность (сек)</label>
        <input id="tf-duration" type="number" min="0" value="${t.duration || ''}" />
      </div>
    </div>
    <div class="checkbox-row">
      <input id="tf-published" type="checkbox" ${t.is_published ? 'checked' : ''} />
      <label for="tf-published">Опубликован</label>
    </div>
    <div class="form-group">
      <label>Аудио файл ${t.audio_url ? '<span style="color:#4ade80;font-size:12px">✓ уже загружено</span>' : '<span style="color:#f87171;font-size:12px">⚠ не загружено</span>'}</label>
      ${t.audio_url ? `<div style="font-size:11px;color:#888;margin-bottom:6px;word-break:break-all">${esc(t.audio_url)}</div>` : ''}
      <div class="upload-area" id="tf-audio-area" onclick="document.getElementById('tf-audio-input').click()">
        🎵 ${t.audio_url ? 'Заменить аудио' : 'Загрузить аудио (обязательно)'}
      </div>
      <input id="tf-audio-input" type="file" accept="audio/*" class="hidden" />
      <div class="progress-bar" id="tf-audio-progress" style="display:none"><div class="progress-fill" id="tf-audio-fill" style="width:0%"></div></div>
    </div>
    <div class="form-group">
      <label>Обложка ${t.cover_url ? '<span style="color:#4ade80;font-size:12px">✓ уже загружена</span>' : '<span style="color:#f87171;font-size:12px">⚠ не загружена</span>'}</label>
      <div class="upload-area" id="tf-cover-area" onclick="document.getElementById('tf-cover-input').click()">
        🖼️ ${t.cover_url ? 'Заменить обложку' : 'Загрузить обложку'}
        ${t.cover_url ? `<img class="upload-preview" src="${esc(t.cover_url)}" />` : ''}
      </div>
      <input id="tf-cover-input" type="file" accept="image/*" class="hidden" />
      <div class="progress-bar" id="tf-cover-progress" style="display:none"><div class="progress-fill" id="tf-cover-fill" style="width:0%"></div></div>
    </div>
  `;
}

function bindTrackFormUploads() {
  document.getElementById('tf-audio-input')?.addEventListener('change', () => {
    const file = document.getElementById('tf-audio-input').files[0];
    if (!file) return;
    const area = document.getElementById('tf-audio-area');
    area.classList.add('has-file');
    area.textContent = `🎵 ${file.name}`;
  });

  document.getElementById('tf-cover-input')?.addEventListener('change', () => {
    const file = document.getElementById('tf-cover-input').files[0];
    if (!file) return;
    const area = document.getElementById('tf-cover-area');
    area.classList.add('has-file');
    area.innerHTML = `🖼️ ${esc(file.name)}<img class="upload-preview" src="${URL.createObjectURL(file)}" />`;
  });
}

function openNewTrack() {
  // Ensure artists are loaded
  if (!cache.artists.length) {
    db.from('custom_artists').select('id, name').then(({ data }) => {
      if (data) cache.artists = data;
      openNewTrack();
    });
    return;
  }

  openModal('Новый трек', trackFormHTML(), async () => {
    const title = document.getElementById('tf-title').value.trim();
    if (!title) { showToast('Название обязательно', 'error'); return; }

    const audioFile = document.getElementById('tf-audio-input').files[0];
    const coverFile = document.getElementById('tf-cover-input').files[0];

    if (!audioFile) {
      showToast('Аудио файл обязателен для нового трека', 'error');
      return;
    }

    let audio_url = null;
    let cover_url = null;

    document.getElementById('tf-audio-progress').style.display = 'block';
      audio_url = await uploadFile(audioFile, 'admin-uploads', 'tracks/audio_', document.getElementById('tf-audio-fill'));
    if (!audio_url) return; // uploadFile already showed error toast

    if (coverFile) {
      document.getElementById('tf-cover-progress').style.display = 'block';
      cover_url = await uploadFile(coverFile, 'admin-uploads', 'tracks/cover_', document.getElementById('tf-cover-fill'));
    }

    const artistId = document.getElementById('tf-artist').value || null;
    const duration = parseInt(document.getElementById('tf-duration').value) || null;

    const payload = {
      title,
      genre:        document.getElementById('tf-genre').value.trim() || null,
      artist_id:    artistId,
      duration,
      is_published: document.getElementById('tf-published').checked,
      audio_url,
      cover_url,
    };

    console.log('Creating track with payload:', payload);
    const { data, error } = await db.from('custom_tracks').insert(payload).select();
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    console.log('Track created:', data);

    showToast('Трек создан');
    closeModal();
    loadTracks();
  });

  setTimeout(bindTrackFormUploads, 50);
}

function editTrack(id) {
  const t = cache.tracks.find(x => x.id === id);
  if (!t) return;

  openModal('Редактировать трек', trackFormHTML(t), async () => {
    const title = document.getElementById('tf-title').value.trim();
    if (!title) { showToast('Название обязательно', 'error'); return; }

    const audioFile = document.getElementById('tf-audio-input').files[0];
    const coverFile = document.getElementById('tf-cover-input').files[0];

    let audio_url = t.audio_url;
    let cover_url = t.cover_url;

    if (audioFile) {
      document.getElementById('tf-audio-progress').style.display = 'block';
      audio_url = await uploadFile(audioFile, 'admin-uploads', 'tracks/audio_', document.getElementById('tf-audio-fill'));
    }
    if (coverFile) {
      document.getElementById('tf-cover-progress').style.display = 'block';
      cover_url = await uploadFile(coverFile, 'admin-uploads', 'tracks/cover_', document.getElementById('tf-cover-fill'));
    }

    const artistId = document.getElementById('tf-artist').value || null;
    const duration = parseInt(document.getElementById('tf-duration').value) || null;

    const updates = {
      title,
      genre:        document.getElementById('tf-genre').value.trim() || null,
      artist_id:    artistId,
      duration,
      is_published: document.getElementById('tf-published').checked,
      audio_url,
      cover_url,
    };

    const { error } = await db.from('custom_tracks').update(updates).eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }

    showToast('Трек обновлён');
    closeModal();
    loadTracks();
  });

  setTimeout(bindTrackFormUploads, 50);
}

function deleteTrack(id, title) {
  confirmDelete(`Удалить трек «${title}»?`, async () => {
    const { error } = await db.from('custom_tracks').delete().eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    showToast('Трек удалён');
    closeConfirm();
    loadTracks();
  });
}

// ── Ratings ──────────────────────────────────────────────────
async function loadRatings() {
  const tbody = document.querySelector('#table-ratings tbody');
  tbody.innerHTML = '<tr><td colspan="8" class="loading">Загрузка...</td></tr>';

  const { data, error } = await db
    .from('track_ratings')
    .select('id, username, track_title, track_artist, overall_score, review, reputation, created_at')
    .order('created_at', { ascending: false });

  if (error) {
    tbody.innerHTML = `<tr><td colspan="8" class="loading">Ошибка: ${esc(error.message)}</td></tr>`;
    return;
  }

  cache.ratings = data || [];
  renderRatings(cache.ratings);
}

function renderRatings(ratings) {
  const tbody = document.querySelector('#table-ratings tbody');
  if (!ratings.length) {
    tbody.innerHTML = '<tr><td colspan="8" class="loading">Нет данных</td></tr>';
    return;
  }

  tbody.innerHTML = ratings.map(r => `
    <tr>
      <td>${esc(r.username)}</td>
      <td>${esc(r.track_title)}</td>
      <td class="text-muted">${esc(r.track_artist)}</td>
      <td><span class="score-badge">${r.overall_score ?? '—'}</span></td>
      <td class="text-truncate text-muted">${esc(r.review) || '—'}</td>
      <td class="text-muted">${r.reputation ?? '—'}</td>
      <td class="text-muted">${formatDate(r.created_at)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon danger" title="Удалить" onclick="deleteRating('${esc(r.id)}')">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function deleteRating(id) {
  confirmDelete('Удалить эту оценку?', async () => {
    const { error } = await db.from('track_ratings').delete().eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    showToast('Оценка удалена');
    closeConfirm();
    loadRatings();
  });
}

// ── Comments ─────────────────────────────────────────────────
async function loadComments() {
  const tbody = document.querySelector('#table-comments tbody');
  tbody.innerHTML = '<tr><td colspan="5" class="loading">Загрузка...</td></tr>';

  const { data, error } = await db
    .from('track_comments')
    .select('id, username, track_title, track_artist, comment, created_at')
    .order('created_at', { ascending: false });

  if (error) {
    tbody.innerHTML = `<tr><td colspan="5" class="loading">Ошибка: ${esc(error.message)}</td></tr>`;
    return;
  }

  cache.comments = data || [];
  renderComments(cache.comments);
}

function renderComments(comments) {
  const tbody = document.querySelector('#table-comments tbody');
  if (!comments.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="loading">Нет данных</td></tr>';
    return;
  }

  tbody.innerHTML = comments.map(c => `
    <tr>
      <td>${esc(c.username)}</td>
      <td class="text-muted">${esc(c.track_title) || esc(c.track_artist) || '—'}</td>
      <td class="text-truncate text-muted">${esc(c.comment) || '—'}</td>
      <td class="text-muted">${formatDate(c.created_at)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon danger" title="Удалить" onclick="deleteComment('${esc(c.id)}')">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function deleteComment(id) {
  confirmDelete('Удалить этот комментарий?', async () => {
    const { error } = await db.from('track_comments').delete().eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    showToast('Комментарий удалён');
    closeConfirm();
    loadComments();
  });
}

// ── Playlists ────────────────────────────────────────────────
async function loadPlaylists() {
  const tbody = document.querySelector('#table-playlists tbody');
  tbody.innerHTML = '<tr><td colspan="9" class="loading">Загрузка...</td></tr>';

  const { data, error } = await db
    .from('playlists')
    .select('id, name, cover_url, track_count, likes_count, is_public, is_synthesis, created_at, username')
    .order('created_at', { ascending: false });

  if (error) {
    tbody.innerHTML = `<tr><td colspan="9" class="loading">Ошибка: ${esc(error.message)}</td></tr>`;
    return;
  }

  cache.playlists = data || [];
  renderPlaylists(cache.playlists);
}

function renderPlaylists(playlists) {
  const tbody = document.querySelector('#table-playlists tbody');
  if (!playlists.length) {
    tbody.innerHTML = '<tr><td colspan="9" class="loading">Нет данных</td></tr>';
    return;
  }

  tbody.innerHTML = playlists.map(p => `
    <tr>
      <td class="cell-cover">
        ${p.cover_url
          ? `<img src="${esc(p.cover_url)}" alt="" />`
          : `<div class="cover-placeholder">📋</div>`}
      </td>
      <td>${esc(p.name)}</td>
      <td class="text-muted">${esc(p.username)}</td>
      <td class="text-muted">${p.track_count || 0}</td>
      <td class="text-muted">${p.likes_count || 0}</td>
      <td>${p.is_public
        ? '<span class="badge badge-green">Да</span>'
        : '<span class="badge badge-gray">Нет</span>'}</td>
      <td>${p.is_synthesis
        ? '<span class="badge badge-blue">Синтез</span>'
        : '<span class="badge badge-gray">Нет</span>'}</td>
      <td class="text-muted">${formatDate(p.created_at)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon danger" title="Удалить" onclick="deletePlaylist('${esc(p.id)}', '${esc(p.name)}')">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function deletePlaylist(id, name) {
  confirmDelete(`Удалить плейлист «${name}»?`, async () => {
    const { error } = await db.from('playlists').delete().eq('id', id);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    showToast('Плейлист удалён');
    closeConfirm();
    loadPlaylists();
  });
}

// ── Search (client-side filtering) ──────────────────────────
function setupSearch(inputId, renderFn, cacheKey, filterFn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  input.addEventListener('input', () => {
    const q = input.value.trim().toLowerCase();
    if (!q) {
      renderFn(cache[cacheKey]);
      return;
    }
    renderFn(cache[cacheKey].filter(item => filterFn(item, q)));
  });
}

// ── Event listeners ──────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  // Check existing session
  if (sessionStorage.getItem('admin_auth') === '1') {
    showApp();
  }

  // Login
  document.getElementById('login-btn').addEventListener('click', login);
  document.getElementById('login-password').addEventListener('keydown', e => {
    if (e.key === 'Enter') login();
  });

  // Logout
  document.getElementById('logout-btn').addEventListener('click', logout);

  // Navigation
  document.querySelectorAll('.nav-item[data-page]').forEach(el => {
    el.addEventListener('click', () => navigateTo(el.dataset.page));
  });

  // Modal controls
  document.getElementById('modal-close').addEventListener('click', closeModal);
  document.getElementById('modal-cancel').addEventListener('click', closeModal);
  document.getElementById('modal-save').addEventListener('click', () => {
    if (typeof modalSaveCallback === 'function') modalSaveCallback();
  });

  // Close modal on overlay click
  document.getElementById('modal-overlay').addEventListener('click', e => {
    if (e.target === e.currentTarget) closeModal();
  });

  // Confirm dialog
  document.getElementById('confirm-cancel').addEventListener('click', closeConfirm);
  document.getElementById('confirm-ok').addEventListener('click', () => {
    if (typeof confirmCallback === 'function') confirmCallback();
  });

  // New artist / new track buttons
  document.getElementById('btn-new-artist').addEventListener('click', openNewArtist);
  document.getElementById('btn-new-track').addEventListener('click', openNewTrack);

  // Search bindings
  setupSearch('search-users', renderUsers, 'users', (u, q) =>
    (u.username || '').toLowerCase().includes(q) ||
    (u.email || '').toLowerCase().includes(q)
  );

  setupSearch('search-artists', renderArtists, 'artists', (a, q) =>
    (a.name || '').toLowerCase().includes(q) ||
    (a.genre || '').toLowerCase().includes(q) ||
    (a.location || '').toLowerCase().includes(q)
  );

  setupSearch('search-tracks', renderTracks, 'tracks', (t, q) =>
    (t.title || '').toLowerCase().includes(q) ||
    (t.artist_name || '').toLowerCase().includes(q) ||
    (t.genre || '').toLowerCase().includes(q)
  );

  setupSearch('search-ratings', renderRatings, 'ratings', (r, q) =>
    (r.username || '').toLowerCase().includes(q) ||
    (r.track_title || '').toLowerCase().includes(q) ||
    (r.track_artist || '').toLowerCase().includes(q)
  );

  setupSearch('search-comments', renderComments, 'comments', (c, q) =>
    (c.username || '').toLowerCase().includes(q) ||
    (c.track_title || '').toLowerCase().includes(q) ||
    (c.comment || '').toLowerCase().includes(q)
  );

  setupSearch('search-playlists', renderPlaylists, 'playlists', (p, q) =>
    (p.name || '').toLowerCase().includes(q) ||
    (p.username || '').toLowerCase().includes(q)
  );

  setupSearch('search-reports', renderReports, 'reports', (r, q) =>
    (r.reason || '').toLowerCase().includes(q) ||
    (r.target_username || '').toLowerCase().includes(q) ||
    (r.target_content || '').toLowerCase().includes(q)
  );

  setupSearch('search-bans', renderBans, 'bans', (b, q) =>
    (b.username || '').toLowerCase().includes(q) ||
    (b.reason || '').toLowerCase().includes(q)
  );

  setupSearch('search-appeals', renderAppeals, 'appeals', (a, q) =>
    (a.username || '').toLowerCase().includes(q) ||
    (a.message || '').toLowerCase().includes(q)
  );

  // Фильтр статуса жалоб
  document.getElementById('filter-reports')?.addEventListener('change', e => {
    const val = e.target.value;
    renderReports(val ? cache.reports.filter(r => r.status === val) : cache.reports);
  });
});

// ── Reports ──────────────────────────────────────────────────
async function loadReports() {
  const tbody = document.querySelector('#table-reports tbody');
  tbody.innerHTML = '<tr><td colspan="8" class="loading">Загрузка...</td></tr>';

  const { data, error } = await db
    .from('reports')
    .select('*')
    .order('created_at', { ascending: false });

  if (error) {
    tbody.innerHTML = `<tr><td colspan="8" class="loading">Ошибка: ${esc(error.message)}</td></tr>`;
    return;
  }

  cache.reports = data || [];
  renderReports(cache.reports);
}

function renderReports(reports) {
  const tbody = document.querySelector('#table-reports tbody');
  if (!reports.length) {
    tbody.innerHTML = '<tr><td colspan="8" class="loading">Нет жалоб</td></tr>';
    return;
  }

  tbody.innerHTML = reports.map(r => `
    <tr>
      <td><span class="badge ${r.target_type === 'comment' ? 'badge-blue' : 'badge-gray'}">${r.target_type === 'comment' ? '💬 Комментарий' : '⭐ Рецензия'}</span></td>
      <td>${esc(r.reason)}</td>
      <td class="text-muted">${esc(r.target_username) || '—'}</td>
      <td class="text-truncate text-muted" style="max-width:180px">${esc(r.target_content) || '—'}</td>
      <td class="text-muted">${esc(r.track_title) || '—'}</td>
      <td>${statusBadge(r.status)}</td>
      <td class="text-muted">${formatDate(r.created_at)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" title="Удалить контент" onclick="resolveReport('${esc(r.id)}', '${esc(r.target_type)}', '${esc(r.target_id)}', '${esc(r.target_user_id || '')}')">🗑️</button>
          <button class="btn-icon" title="Отклонить жалобу" onclick="dismissReport('${esc(r.id)}')">✕</button>
          ${r.target_user_id ? `<button class="btn-icon danger" title="Забанить пользователя" onclick="banUserFromReport('${esc(r.target_user_id)}', '${esc(r.target_username || '')}')">🔨</button>` : ''}
        </div>
      </td>
    </tr>
  `).join('');
}

function statusBadge(status) {
  switch (status) {
    case 'pending':   return '<span class="badge badge-gray">Ожидает</span>';
    case 'resolved':  return '<span class="badge badge-green">Решена</span>';
    case 'dismissed': return '<span class="badge badge-red">Отклонена</span>';
    default: return `<span class="badge badge-gray">${esc(status)}</span>`;
  }
}

async function resolveReport(reportId, targetType, targetId, targetUserId) {
  // Удаляем контент
  const table = targetType === 'comment' ? 'track_comments' : 'track_ratings';
  const { error } = await db.from(table).delete().eq('id', targetId);
  if (error) { showToast('Ошибка удаления: ' + error.message, 'error'); return; }

  // Помечаем жалобу как решённую
  await db.from('reports').update({ status: 'resolved' }).eq('id', reportId);

  // Добавляем страйк пользователю
  if (targetUserId) {
    await db.from('content_strikes').insert({
      user_id: targetUserId,
      content_type: targetType,
      content_id: targetId,
      reason: 'Удалено по жалобе'
    });

    // Проверяем количество страйков — если 5+, баним
    const { count } = await db
      .from('content_strikes')
      .select('*', { count: 'exact', head: true })
      .eq('user_id', targetUserId);

    if (count >= 5) {
      // Автобан
      await db.from('user_bans').upsert({
        user_id: targetUserId,
        reason: `Автоматический бан: ${count} удалённых материалов`,
        is_active: true
      }, { onConflict: 'user_id' });

      await db.from('profiles').update({ is_banned: true, ban_reason: 'Автоматический бан за нарушения' }).eq('id', targetUserId);
      showToast(`Пользователь автоматически забанен (${count} страйков)`, 'error');
    }
  }

  showToast('Контент удалён, жалоба решена');
  loadReports();
}

async function dismissReport(reportId) {
  const { error } = await db.from('reports').update({ status: 'dismissed' }).eq('id', reportId);
  if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
  showToast('Жалоба отклонена');
  loadReports();
}

function banUserFromReport(userId, username) {
  const body = `
    <div class="form-group">
      <label>Причина бана</label>
      <textarea id="ban-reason" rows="3" placeholder="Укажите причину..."></textarea>
    </div>
    <p class="text-muted" style="font-size:12px">Пользователь: <strong>${esc(username)}</strong></p>
  `;
  openModal('Забанить пользователя', body, async () => {
    const reason = document.getElementById('ban-reason').value.trim();
    if (!reason) { showToast('Укажите причину', 'error'); return; }

    const { error: banError } = await db.from('user_bans').upsert({
      user_id: userId,
      reason,
      is_active: true
    }, { onConflict: 'user_id' });

    if (banError) { showToast('Ошибка: ' + banError.message, 'error'); return; }

    await db.from('profiles').update({ is_banned: true, ban_reason: reason }).eq('id', userId);

    showToast('Пользователь забанен');
    closeModal();
    loadReports();
  });
}

// ── Bans ─────────────────────────────────────────────────────
async function loadBans() {
  const tbody = document.querySelector('#table-bans tbody');
  tbody.innerHTML = '<tr><td colspan="6" class="loading">Загрузка...</td></tr>';

  const { data, error } = await db
    .from('user_bans')
    .select('*')
    .order('created_at', { ascending: false });

  if (error) {
    tbody.innerHTML = `<tr><td colspan="6" class="loading">Ошибка: ${esc(error.message)}</td></tr>`;
    return;
  }

  // Получаем username из profiles отдельным запросом
  const userIds = (data || []).map(b => b.user_id).filter(Boolean);
  let profilesMap = {};
  if (userIds.length) {
    const { data: profiles } = await db.from('profiles').select('id, username, email').in('id', userIds);
    (profiles || []).forEach(p => { profilesMap[p.id] = p; });
  }

  // Получаем количество страйков
  let strikesMap = {};
  if (userIds.length) {
    const { data: strikes } = await db.from('content_strikes').select('user_id').in('user_id', userIds);
    (strikes || []).forEach(s => { strikesMap[s.user_id] = (strikesMap[s.user_id] || 0) + 1; });
  }

  cache.bans = (data || []).map(b => ({
    ...b,
    username: profilesMap[b.user_id]?.username || '—',
    email: profilesMap[b.user_id]?.email || '—',
    strikes: strikesMap[b.user_id] || 0
  }));

  renderBans(cache.bans);
}

function renderBans(bans) {
  const tbody = document.querySelector('#table-bans tbody');
  if (!bans.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="loading">Нет банов</td></tr>';
    return;
  }

  tbody.innerHTML = bans.map(b => `
    <tr>
      <td>
        <div>${esc(b.username)}</div>
        <div class="text-muted" style="font-size:11px">${esc(b.email)}</div>
      </td>
      <td class="text-muted text-truncate">${esc(b.reason)}</td>
      <td><span class="badge ${b.strikes >= 5 ? 'badge-red' : 'badge-gray'}">${b.strikes} страйков</span></td>
      <td>${b.is_active
        ? '<span class="badge badge-red">Активен</span>'
        : '<span class="badge badge-green">Снят</span>'}</td>
      <td class="text-muted">${formatDate(b.created_at)}</td>
      <td>
        <div class="actions-cell">
          ${b.is_active
            ? `<button class="btn-icon" title="Снять бан" onclick="unbanUser('${esc(b.id)}', '${esc(b.user_id)}')">✅</button>`
            : ''}
          <button class="btn-icon danger" title="Удалить запись" onclick="deleteBan('${esc(b.id)}')">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

async function unbanUser(banId, userId) {
  const { error } = await db.from('user_bans').update({ is_active: false }).eq('id', banId);
  if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
  await db.from('profiles').update({ is_banned: false, ban_reason: null }).eq('id', userId);
  showToast('Бан снят');
  loadBans();
}

async function deleteBan(banId) {
  confirmDelete('Удалить запись о бане?', async () => {
    const { error } = await db.from('user_bans').delete().eq('id', banId);
    if (error) { showToast('Ошибка: ' + error.message, 'error'); return; }
    showToast('Запись удалена');
    closeConfirm();
    loadBans();
  });
}

// ── Appeals ───────────────────────────────────────────────────
async function loadAppeals() {
  const tbody = document.querySelector('#table-appeals tbody');
  tbody.innerHTML = '<tr><td colspan="5" class="loading">Загрузка...</td></tr>';

  const { data, error } = await db
    .from('ban_appeals')
    .select('*')
    .order('created_at', { ascending: false });

  if (error) {
    tbody.innerHTML = `<tr><td colspan="5" class="loading">Ошибка: ${esc(error.message)}</td></tr>`;
    return;
  }

  cache.appeals = data || [];
  renderAppeals(cache.appeals);
}

function renderAppeals(appeals) {
  const tbody = document.querySelector('#table-appeals tbody');
  if (!appeals.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="loading">Нет апелляций</td></tr>';
    return;
  }

  tbody.innerHTML = appeals.map(a => `
    <tr>
      <td>
        <div>${esc(a.username)}</div>
        <div class="text-muted" style="font-size:11px">${esc(a.email)}</div>
      </td>
      <td class="text-truncate text-muted">${esc(a.message)}</td>
      <td>${statusBadge(a.status)}</td>
      <td class="text-muted">${formatDate(a.created_at)}</td>
      <td>
        <div class="actions-cell">
          ${a.status === 'pending' ? `
            <button class="btn-icon" title="Одобрить (снять бан)" onclick="approveAppeal('${esc(a.id)}', '${esc(a.ban_id)}', '${esc(a.user_id)}')">✅</button>
            <button class="btn-icon danger" title="Отклонить" onclick="rejectAppeal('${esc(a.id)}')">✕</button>
          ` : ''}
        </div>
      </td>
    </tr>
  `).join('');
}

async function approveAppeal(appealId, banId, userId) {
  await db.from('ban_appeals').update({ status: 'approved', reviewed_at: new Date().toISOString() }).eq('id', appealId);
  await db.from('user_bans').update({ is_active: false }).eq('id', banId);
  await db.from('profiles').update({ is_banned: false, ban_reason: null }).eq('id', userId);
  showToast('Апелляция одобрена, бан снят');
  loadAppeals();
}

async function rejectAppeal(appealId) {
  await db.from('ban_appeals').update({ status: 'rejected', reviewed_at: new Date().toISOString() }).eq('id', appealId);
  showToast('Апелляция отклонена');
  loadAppeals();
}
