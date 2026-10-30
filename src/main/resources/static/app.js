/**
 * CAR-ZTNA — Frontend Application Logic
 */

const API = {
  login: '/api/auth/login',
  register: '/api/auth/register',
  logout: '/api/auth/logout',
  contextReport: '/api/audit/context-report',
  protectedBase: '/api/protected',
  admin: {
    stats: '/api/admin/stats',
    users: '/api/admin/users',
    superKeyGen: '/api/admin/super-key/generate',
    superKeyUnlock: '/api/admin/super-key/unlock',
    networks: '/api/admin/networks',
    devices: '/api/admin/devices',
    policies: '/api/admin/policies',
    sessions: '/api/admin/sessions',
    logs: '/api/admin/logs'
  }
};

// ── State ─────────────────────────────────────────────────────────────────────
let currentUser = null;
let currentSessionId = localStorage.getItem('ztna_session_id');
let detectedDevice = '';   // auto-detected once on load; reused for every request
let detectedLocalIp = null; // real LAN/WiFi IP detected via WebRTC

// ── Init ──────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  detectedDevice = detectDevice();
  detectedLocalIp = await detectLocalIp();

  const badge = document.getElementById('login-ambient-info');
  if (badge) {
    const ipLabel = detectedLocalIp ? ` · IP: ${detectedLocalIp}` : '';
    badge.innerText = `Device: ${detectedDevice}${ipLabel}`;
  }

  if (currentSessionId) {
    const saved = localStorage.getItem('ztna_user');
    if (saved) {
      try { currentUser = JSON.parse(saved); navigateUserView(); }
      catch { handleLogout(); }
    }
  }
});

// ── Device auto-detection ─────────────────────────────────────────────────────
/**
 * Builds a clean device label from navigator.userAgent.
 * Used as the X-Device-Name request header — no user input needed.
 */
function detectDevice() {
  const ua = navigator.userAgent;
  let os = 'Unknown', browser = 'Unknown', type = 'Workstation';

  if (/Windows NT 1[01]/i.test(ua)) os = 'Windows 11/10';
  else if (/Windows/i.test(ua)) os = 'Windows';
  else if (/Mac OS X|macOS/i.test(ua)) os = 'macOS';
  else if (/Android/i.test(ua)) os = 'Android';
  else if (/iPhone|iPad/i.test(ua)) os = 'iOS';
  else if (/Linux/i.test(ua)) os = 'Linux';

  if (/Edg\//i.test(ua)) browser = 'Edge';
  else if (/Chrome\//i.test(ua)) browser = 'Chrome';
  else if (/Firefox\//i.test(ua)) browser = 'Firefox';
  else if (/Safari\//i.test(ua) && !/Chrome/i.test(ua)) browser = 'Safari';

  if (/Mobile|Android|iPhone/i.test(ua)) type = 'Mobile';
  else if (/iPad|Tablet/i.test(ua)) type = 'Tablet';

  return `${os} (${browser} ${type})`;
}

/**
 * Detects the real local network IP (WiFi/LAN) using a WebRTC peer connection.
 * Returns a promise that resolves with the IP string, or null if unavailable.
 *
 * This is the only reliable way to get the actual network interface IP
 * when the browser and server are on the same machine (no proxy/forwarding).
 */
function detectLocalIp() {
  return new Promise(resolve => {
    // Abort after 1 second if WebRTC takes too long
    const timeout = setTimeout(() => resolve(null), 1000);

    try {
      const pc = new RTCPeerConnection({ iceServers: [] });
      pc.createDataChannel('');
      pc.createOffer()
        .then(offer => pc.setLocalDescription(offer))
        .catch(() => { clearTimeout(timeout); resolve(null); });

      pc.onicecandidate = e => {
        if (!e || !e.candidate) return;
        // ICE candidate line looks like: "a=candidate:... 192.168.x.x ..."
        const match = /([0-9]{1,3}(\.[0-9]{1,3}){3})/.exec(e.candidate.candidate);
        if (match) {
          const ip = match[1];
          // Only accept real routable LAN addresses — skip loopback and link-local
          if (!ip.startsWith('127.')
            && !ip.startsWith('169.254.')
            && !ip.startsWith('0.')) {
            clearTimeout(timeout);
            pc.onicecandidate = null;
            pc.close();
            resolve(ip);
          }
        }
      };
    } catch {
      clearTimeout(timeout);
      resolve(null);
    }
  });
}

// ── Auth tabs ─────────────────────────────────────────────────────────────────
function switchAuthTab(tab) {
  document.getElementById('btn-tab-login').classList.toggle('active', tab === 'login');
  document.getElementById('btn-tab-register').classList.toggle('active', tab === 'register');
  document.getElementById('tab-login').classList.toggle('hidden', tab !== 'login');
  document.getElementById('tab-register').classList.toggle('hidden', tab !== 'register');
}

// ── Register ──────────────────────────────────────────────────────────────────
async function handleRegister() {
  const username = document.getElementById('reg-username').value.trim();
  const password = document.getElementById('reg-password').value.trim();
  const roleEl = document.getElementById('reg-role');
  const role = roleEl ? roleEl.value : 'USER';   // form has no role field → always USER
  const msgBox = document.getElementById('reg-message');

  msgBox.className = 'alert hidden';
  if (!username || !password) { showMsg(msgBox, 'Username and password required.', 'alert-error'); return; }

  try {
    const res = await fetch(API.register, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, role })
    });
    const data = await res.json();
    if (res.ok) {
      showMsg(msgBox, `Account '${data.username}' created. You can now sign in.`, 'alert-success');
      setTimeout(() => switchAuthTab('login'), 1500);
    } else {
      showMsg(msgBox, data.error || 'Registration failed.', 'alert-error');
    }
  } catch {
    showMsg(msgBox, 'Network error.', 'alert-error');
  }
}

// ── Login ─────────────────────────────────────────────────────────────────────
async function handleLogin() {
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value.trim();
  const errBox = document.getElementById('login-error');

  errBox.className = 'alert hidden';
  if (!username || !password) { showMsg(errBox, 'Username and password required.', 'alert-error'); return; }

  try {
    const res = await fetch(API.login, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Device-Name': navigator.userAgent,   // raw UA — backend parses os/browser from this
        ...(detectedLocalIp ? { 'X-Client-Real-IP': detectedLocalIp } : {})
      },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json();

    if (res.ok && data.decision === 'ALLOW') {
      currentUser = data.userData;
      currentSessionId = data.sessionId;
      localStorage.setItem('ztna_session_id', currentSessionId);
      localStorage.setItem('ztna_user', JSON.stringify(currentUser));
      localStorage.setItem('ztna_net', data.networkClassification || 'UNKNOWN');
      localStorage.setItem('ztna_dev', data.deviceStatus || 'UNREGISTERED');
      navigateUserView();
    } else {
      showMsg(errBox, data.message || data.error || 'Authentication denied.', 'alert-error');
    }
  } catch {
    showMsg(errBox, 'Server unreachable.', 'alert-error');
  }
}

// ── Logout ────────────────────────────────────────────────────────────────────
async function handleLogout() {
  if (currentSessionId) {
    try {
      await fetch(API.logout, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${currentSessionId}` }
      });
    } catch { /* ignore */ }
  }
  currentUser = null; currentSessionId = null;
  localStorage.removeItem('ztna_session_id');
  localStorage.removeItem('ztna_user');
  localStorage.removeItem('ztna_net');
  localStorage.removeItem('ztna_dev');
  showView('view-login');
}

// ── View routing ──────────────────────────────────────────────────────────────
function showView(id) {
  document.querySelectorAll('.view').forEach(v => {
    v.classList.remove('active');
    v.classList.add('hidden');
    v.style.display = 'none';
  });
  const el = document.getElementById(id);
  if (el) {
    el.classList.remove('hidden');
    el.classList.add('active');
    // Restore the display value the CSS expects for each view.
    // #view-login uses flex (for centring); all others use block.
    el.style.display = el.id === 'view-login' ? 'flex' : 'block';
  }
}

function navigateUserView() {
  if (!currentUser) { showView('view-login'); return; }

  if (currentUser.role === 'ADMIN') {
    showView('view-admin');
    document.getElementById('admin-username').innerText = currentUser.username;
    const adminNet = localStorage.getItem('ztna_net') || 'UNKNOWN';
    const adminDev = localStorage.getItem('ztna_dev') || 'UNREGISTERED';
    document.getElementById('admin-network-badge').innerText = `Network: ${adminNet}`;
    document.getElementById('admin-device-badge').innerText = `Device: ${adminDev}`;
    loadAdminStats();
    loadAdminUsers();
  } else {
    showView('view-portal');
    document.getElementById('portal-username').innerText = currentUser.username;
    document.getElementById('portal-role-badge').innerText = currentUser.role;
    document.getElementById('portal-network-badge').innerText = `Network: ${localStorage.getItem('ztna_net') || 'UNKNOWN'}`;
    document.getElementById('portal-device-badge').innerText = `Device: ${localStorage.getItem('ztna_dev') || 'UNREGISTERED'}`;
  }
}

// ── Protected resources ───────────────────────────────────────────────────────
async function requestProtectedResource(endpointPath, resourceName) {
  const statusEl = document.getElementById(`status-${resourceName}`);
  const respBox = document.getElementById('resource-response-box');
  const respTitle = document.getElementById('response-title');
  const respBody = document.getElementById('response-body');

  statusEl.innerText = 'Evaluating…';
  statusEl.className = 'resource-status text-muted';
  respBox.classList.remove('hidden');

  try {
    const res = await fetch(`${API.protectedBase}/${endpointPath}`, {
      headers: {
        'Authorization': `Bearer ${currentSessionId}`,
        'X-Device-Name': navigator.userAgent,
        ...(detectedLocalIp ? { 'X-Client-Real-IP': detectedLocalIp } : {})
      }
    });
    const data = await res.json();

    if (res.ok) {
      statusEl.innerHTML = `<span class="text-green">Access granted — ${data.policyMatched || 'policy matched'}</span>`;
      respTitle.innerHTML = `<span class="text-green">ALLOW</span> &nbsp;${resourceName}`;
    } else {
      statusEl.innerHTML = `<span class="text-red">Access denied — ${data.reason || 'policy denied'}</span>`;
      respTitle.innerHTML = `<span class="text-red">DENY</span> &nbsp;${resourceName}`;
    }
    respBody.innerText = JSON.stringify(data, null, 2);
  } catch (err) {
    statusEl.innerHTML = `<span class="text-red">Request error</span>`;
    respTitle.innerText = 'Request Error';
    respBody.innerText = String(err);
  }
}

function closeResponseBox() {
  document.getElementById('resource-response-box').classList.add('hidden');
}

// ── Context posture modal ─────────────────────────────────────────────────────
async function openPostureReport() {
  const modal = document.getElementById('modal-posture-report');
  const body = document.getElementById('posture-report-body');
  modal.classList.remove('hidden');
  body.innerHTML = '<p class="text-muted" style="font-size:.8125rem">Loading…</p>';

  try {
    const res = await fetch(`${API.contextReport}/${currentUser.username}`, {
      headers: { 'Authorization': `Bearer ${currentSessionId}` }
    });
    if (!res.ok) { body.innerHTML = '<p class="text-muted">Unable to load report.</p>'; return; }
    const d = await res.json();

    const accountColour = d.accountStatus === 'ACTIVE' ? 'text-green' : 'text-red';
    const decisionColour = d.lastDecision === 'ALLOW' ? 'text-green' : 'text-red';

    body.innerHTML = `
      <ul class="report-list">
        <li><span class="lbl">Username</span>        <span class="val">${d.username}</span></li>
        <li><span class="lbl">Role</span>             <span class="val">${d.role}</span></li>
        <li><span class="lbl">Account Status</span>  <span class="val ${accountColour}">${d.accountStatus}</span></li>
        <li><span class="lbl">Network</span>          <span class="val">${d.networkClassification} &nbsp;<span class="text-muted">${d.ipAddress}</span></span></li>
        <li><span class="lbl">Device</span>           <span class="val">${d.deviceStatus}</span></li>
        <li><span class="lbl">Failed Attempts</span> <span class="val">${d.failedLoginAttempts} / 5</span></li>
        <li><span class="lbl">Last Decision</span>   <span class="val ${decisionColour}">${d.lastDecision}</span></li>
      </ul>
      <p style="font-size:.6875rem;font-weight:600;text-transform:uppercase;letter-spacing:.05em;color:var(--text-2);margin-bottom:.6rem">Recent Audit Events</p>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Event</th><th>Severity</th><th>Description</th></tr></thead>
          <tbody>
            ${(d.recentAuditEvents || []).map(e => `
              <tr>
                <td style="font-family:monospace;font-size:.75rem">${e.eventType}</td>
                <td><span class="badge-sensitivity ${(e.severity || 'info').toLowerCase()}">${e.severity}</span></td>
                <td style="color:var(--text-2)">${e.description || ''}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  } catch {
    body.innerHTML = '<p class="text-muted">Failed to load report.</p>';
  }
}

// ── Admin tab switching ───────────────────────────────────────────────────────
function switchAdminTab(tabName) {
  document.querySelectorAll('.dash-tab').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.dash-panel').forEach(p => p.classList.add('hidden'));
  const btn = document.getElementById(`tabbtn-${tabName}`);
  const panel = document.getElementById(`admin-tab-${tabName}`);
  if (btn) btn.classList.add('active');
  if (panel) panel.classList.remove('hidden');

  const loaders = {
    users: loadAdminUsers, networks: loadAdminNetworks, devices: loadAdminDevices,
    policies: loadAdminPolicies, sessions: loadAdminSessions, logs: loadAdminLogs
  };
  if (loaders[tabName]) loaders[tabName]();
}

// ── Admin Stats ───────────────────────────────────────────────────────────────
async function loadAdminStats() {
  try {
    const res = await fetch(API.admin.stats, { headers: authHeader() });
    if (!res.ok) return;
    const s = await res.json();
    document.getElementById('admin-stats-container').innerHTML = `
      <div class="stat-card"><div class="stat-val">${s.totalUsers}</div><div class="stat-lbl">Users</div></div>
      <div class="stat-card"><div class="stat-val text-red">${s.lockedUsers}</div><div class="stat-lbl">Locked</div></div>
      <div class="stat-card"><div class="stat-val">${s.activeSessions}</div><div class="stat-lbl">Sessions</div></div>
      <div class="stat-card"><div class="stat-val">${s.totalNetworkRules}</div><div class="stat-lbl">Network Rules</div></div>
      <div class="stat-card"><div class="stat-val">${s.totalDevices}</div><div class="stat-lbl">Devices</div></div>
      <div class="stat-card"><div class="stat-val">${s.totalPolicies}</div><div class="stat-lbl">Policies</div></div>
      <div class="stat-card"><div class="stat-val text-green">${s.allowDecisions}</div><div class="stat-lbl">Allows</div></div>
      <div class="stat-card"><div class="stat-val text-red">${s.denyDecisions}</div><div class="stat-lbl">Denials</div></div>`;
  } catch { /* ignore */ }
}

// ── Admin Users ───────────────────────────────────────────────────────────────
async function loadAdminUsers() {
  const tbody = document.querySelector('#table-users tbody');
  tbody.innerHTML = '<tr><td colspan="7" class="text-muted">Loading…</td></tr>';
  try {
    const res = await fetch(API.admin.users, { headers: authHeader() });
    const users = await res.json();
    tbody.innerHTML = users.map(u => `
      <tr>
        <td><strong>${u.username}</strong></td>
        <td><code style="font-size:.75rem">${u.role}</code></td>
        <td class="${u.status === 'ACTIVE' ? 'text-green' : 'text-red'}">${u.status}</td>
        <td>${u.locked ? '<span class="text-red">Yes</span>' : '<span class="text-muted">No</span>'}</td>
        <td class="text-muted">${u.lockReason || '—'}</td>
        <td class="text-muted" style="font-size:.75rem">${u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : 'Never'}</td>
        <td class="action-cell">
          <button class="btn-sm" onclick="toggleRole('${u.username}','${u.role}')">Toggle role</button>
          <button class="btn-sm danger" onclick="deleteUser('${u.username}')">Delete</button>
        </td>
      </tr>`).join('');
  } catch { tbody.innerHTML = '<tr><td colspan="7" class="text-muted">Failed to load.</td></tr>'; }
}

async function toggleRole(username, currentRole) {
  const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
  if (newRole === 'USER') {
    // Demoting to USER needs no dual-auth
    if (!confirm(`Demote '${username}' to USER?`)) return;
    const res = await fetch(`${API.admin.users}/${username}/role?role=USER`, {
      method: 'PUT', headers: authHeader()
    });
    const data = await res.json();
    if (!res.ok) alert(data.error || 'Failed to change role.');
    loadAdminUsers(); loadAdminStats();
  } else {
    // Promoting to ADMIN requires dual-admin verification
    openDualAdminDirectModal(username);
  }
}

// Direct ADMIN promotion modal (not via promotion request)
let _directPromoteTarget = null;
function openDualAdminDirectModal(username) {
  _directPromoteTarget = username;
  document.getElementById('dual-auth-target-display').value = username + ' → ADMIN';
  document.getElementById('dual-auth-second-admin-user').value = '';
  document.getElementById('dual-auth-second-admin-pass').value = '';
  document.getElementById('dual-auth-request-id').value = '';
  document.getElementById('dual-auth-target-role').value = 'ADMIN_DIRECT';
  document.getElementById('dual-auth-msg').className = 'alert hidden';
  document.getElementById('modal-dual-admin-approval').classList.remove('hidden');
}

async function confirmDualAdminPromotion() {
  const secondUser = document.getElementById('dual-auth-second-admin-user').value.trim();
  const secondPass = document.getElementById('dual-auth-second-admin-pass').value.trim();
  const requestId = document.getElementById('dual-auth-request-id').value.trim();
  const mode = document.getElementById('dual-auth-target-role').value;
  const msgEl = document.getElementById('dual-auth-msg');
  msgEl.className = 'alert hidden';

  if (!secondUser || !secondPass) {
    showMsg(msgEl, 'Provide second administrator username and password.', 'alert-error'); return;
  }

  try {
    let res, data;
    if (mode === 'ADMIN_DIRECT') {
      // Direct role promotion
      res = await fetch(`${API.admin.users}/${_directPromoteTarget}/role?role=ADMIN`, {
        method: 'PUT',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ secondAdminUsername: secondUser, secondAdminPassword: secondPass })
      });
      data = await res.json();
    } else {
      // Promotion request approval
      res = await fetch(`/api/admin/promotion-requests/${requestId}/approve`, {
        method: 'POST',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ secondAdminUsername: secondUser, secondAdminPassword: secondPass })
      });
      data = await res.json();
    }

    if (res.ok && data.status === 'success') {
      showMsg(msgEl, '✓ ' + data.message, 'alert-success');
      setTimeout(() => {
        closeModal('modal-dual-admin-approval');
        loadAdminUsers(); loadAdminStats();
        if (mode !== 'ADMIN_DIRECT') loadAdminPromotionRequests();
      }, 1200);
    } else {
      showMsg(msgEl, data.error || data.message || 'Verification failed.', 'alert-error');
    }
  } catch {
    showMsg(msgEl, 'Network error.', 'alert-error');
  }
}

async function deleteUser(username) {
  if (!confirm(`Permanently delete '${username}'?`)) return;
  await fetch(`${API.admin.users}/${username}`, { method: 'DELETE', headers: authHeader() });
  loadAdminUsers(); loadAdminStats();
}

// ── Super Key ─────────────────────────────────────────────────────────────────
async function generateSuperKey() {
  const resBox = document.getElementById('sk-generate-result');
  resBox.className = 'alert hidden';

  try {
    const res = await fetch(API.admin.superKeyGen, { method: 'POST', headers: authHeader() });
    const data = await res.json();

    if (res.ok) {
      resBox.className = 'alert alert-warn';
      resBox.innerHTML = `
        <strong>Super Key Generated</strong><br>
        <div style="font-size:1.75rem;font-family:monospace;letter-spacing:0.5rem;margin:.5rem 0;color:var(--text-1)">${data.superKey}</div>
        <small class="text-muted">Valid 10 min · Single-use · Only SHA-256 hash stored.</small>`;
      document.getElementById('sk-redeem-key').value = data.superKey;
    } else {
      showMsg(resBox, data.error || 'Failed to generate.', 'alert-error');
    }
  } catch {
    showMsg(resBox, 'Network error.', 'alert-error');
  }
}

async function redeemSuperKey() {
  const targetUsername = document.getElementById('sk-redeem-username').value.trim();
  const superKey = document.getElementById('sk-redeem-key').value.trim();
  const resBox = document.getElementById('sk-redeem-result');
  resBox.className = 'alert hidden';

  if (!targetUsername || !superKey) {
    showMsg(resBox, 'Enter the target username and 4-digit key.', 'alert-error'); return;
  }

  try {
    const res = await fetch(API.admin.superKeyUnlock, {
      method: 'POST',
      headers: { ...authHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ targetUsername, superKey })
    });
    const data = await res.json();

    if (res.ok && data.status === 'success') {
      showMsg(resBox, `✓ ${data.message}`, 'alert-success');
      loadAdminUsers(); loadAdminStats();
    } else {
      showMsg(resBox, data.message || 'Redemption failed.', 'alert-error');
    }
  } catch {
    showMsg(resBox, 'Network error.', 'alert-error');
  }
}

// ── Network Rules ─────────────────────────────────────────────────────────────
async function loadAdminNetworks() {
  const tbody = document.querySelector('#table-networks tbody');
  tbody.innerHTML = '<tr><td colspan="5">Loading…</td></tr>';
  try {
    const res = await fetch(API.admin.networks, { headers: authHeader() });
    const rules = await res.json();
    tbody.innerHTML = rules.map(r => `
      <tr>
        <td style="font-family:monospace;font-size:.75rem">${r.ipOrCidr}</td>
        <td>${r.classification}</td>
        <td class="text-muted">${r.description || '—'}</td>
        <td class="${r.active ? 'text-green' : 'text-muted'}">${r.active ? 'Active' : 'Inactive'}</td>
        <td><button class="btn-sm danger" onclick="deleteNetworkRule(${r.id})">Delete</button></td>
      </tr>`).join('');
  } catch { tbody.innerHTML = '<tr><td colspan="5">Failed to load.</td></tr>'; }
}

function openAddNetworkModal() { document.getElementById('modal-network').classList.remove('hidden'); }

async function submitNetworkRule() {
  const ipOrCidr = document.getElementById('net-ip').value.trim();
  const classification = document.getElementById('net-classification').value;
  const description = document.getElementById('net-desc').value.trim();
  if (!ipOrCidr) return;

  await fetch(API.admin.networks, {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({ ipOrCidr, classification, description })
  });
  closeModal('modal-network'); loadAdminNetworks(); loadAdminStats();
}

async function deleteNetworkRule(id) {
  if (!confirm('Delete this rule?')) return;
  await fetch(`${API.admin.networks}/${id}`, { method: 'DELETE', headers: authHeader() });
  loadAdminNetworks(); loadAdminStats();
}

// ── Devices ───────────────────────────────────────────────────────────────────
async function loadAdminDevices() {
  const tbody = document.querySelector('#table-devices tbody');
  tbody.innerHTML = '<tr><td colspan="7">Loading…</td></tr>';
  try {
    const res = await fetch(API.admin.devices, { headers: authHeader() });
    const devices = await res.json();
    tbody.innerHTML = devices.map(d => `
      <tr>
        <td>${d.user ? d.user.username : '—'}</td>
        <td>${d.deviceName || '—'}</td>
        <td class="text-muted">${d.operatingSystem || '—'}</td>
        <td class="text-muted">${d.browser || '—'}</td>
        <td style="font-size:.75rem;font-family:monospace">${d.status}</td>
        <td class="text-muted" style="font-size:.75rem">${d.lastSeen ? new Date(d.lastSeen).toLocaleDateString() : '—'}</td>
        <td class="action-cell">
          <button class="btn-sm" onclick="setDeviceStatus(${d.id},'TRUSTED')">Trust</button>
          <button class="btn-sm danger" onclick="setDeviceStatus(${d.id},'BLOCKED')">Block</button>
        </td>
      </tr>`).join('');
  } catch { tbody.innerHTML = '<tr><td colspan="7">Failed to load.</td></tr>'; }
}

async function setDeviceStatus(id, status) {
  await fetch(`${API.admin.devices}/${id}/status?status=${status}`, {
    method: 'POST', headers: authHeader()
  });
  loadAdminDevices(); loadAdminStats();
}

// ── Policies ──────────────────────────────────────────────────────────────────
async function loadAdminPolicies() {
  const tbody = document.querySelector('#table-policies tbody');
  tbody.innerHTML = '<tr><td colspan="8">Loading…</td></tr>';
  try {
    const res = await fetch(API.admin.policies, { headers: authHeader() });
    const policies = await res.json();
    tbody.innerHTML = policies.map(p => `
      <tr>
        <td>${p.policyName}</td>
        <td style="font-family:monospace;font-size:.75rem">${p.resourceName}</td>
        <td>${p.requiredRole}</td>
        <td>${p.allowedNetwork}</td>
        <td>${p.requiredDeviceStatus}</td>
        <td class="${p.effect === 'ALLOW' ? 'text-green' : 'text-red'}">${p.effect}</td>
        <td class="text-muted">${p.priority}</td>
        <td><button class="btn-sm danger" onclick="deletePolicy(${p.id})">Delete</button></td>
      </tr>`).join('');
  } catch { tbody.innerHTML = '<tr><td colspan="8">Failed to load.</td></tr>'; }
}

function openAddPolicyModal() { document.getElementById('modal-policy').classList.remove('hidden'); }

async function submitPolicy() {
  const policyName = document.getElementById('pol-name').value.trim();
  const resourceName = document.getElementById('pol-resource').value;
  const requiredRole = document.getElementById('pol-role').value;
  const allowedNetwork = document.getElementById('pol-network').value;
  const requiredDeviceStatus = document.getElementById('pol-device').value;
  const effect = document.getElementById('pol-effect').value;
  const priority = parseInt(document.getElementById('pol-priority').value, 10) || 10;
  const description = document.getElementById('pol-desc').value.trim();
  if (!policyName) return;

  await fetch(API.admin.policies, {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      policyName, resourceName, requiredRole, allowedNetwork,
      requiredDeviceStatus, effect, priority, description
    })
  });
  closeModal('modal-policy'); loadAdminPolicies(); loadAdminStats();
}

async function deletePolicy(id) {
  if (!confirm('Delete this policy?')) return;
  await fetch(`${API.admin.policies}/${id}`, { method: 'DELETE', headers: authHeader() });
  loadAdminPolicies(); loadAdminStats();
}

// ── Sessions ──────────────────────────────────────────────────────────────────
async function loadAdminSessions() {
  const tbody = document.querySelector('#table-sessions tbody');
  tbody.innerHTML = '<tr><td colspan="7">Loading…</td></tr>';
  try {
    const res = await fetch(API.admin.sessions, { headers: authHeader() });
    const sessions = await res.json();
    tbody.innerHTML = sessions.map(s => `
      <tr>
        <td style="font-family:monospace;font-size:.75rem">${(s.sessionId || '').substring(0, 8)}…</td>
        <td><strong>${s.user ? s.user.username : '—'}</strong></td>
        <td class="text-muted">${s.user ? s.user.role : '—'}</td>
        <td class="text-muted">${s.ipAddress || '—'}</td>
        <td class="text-muted" style="max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${s.deviceInfo || '—'}</td>
        <td class="text-muted" style="font-size:.75rem">${s.lastActivity ? new Date(s.lastActivity).toLocaleTimeString() : '—'}</td>
        <td><button class="btn-sm danger" onclick="terminateSession('${s.sessionId}')">Terminate</button></td>
      </tr>`).join('');
  } catch { tbody.innerHTML = '<tr><td colspan="7">Failed to load.</td></tr>'; }
}

async function terminateSession(sessionId) {
  await fetch(`${API.admin.sessions}/terminate/${sessionId}`, {
    method: 'POST', headers: authHeader()
  });
  loadAdminSessions(); loadAdminStats();
}

// ── Admin Promotion Requests ─────────────────────────────────────────────────
async function loadAdminPromotionRequests() {
  const tbody = document.querySelector('#table-promotions tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="7">Loading…</td></tr>';
  try {
    const res = await fetch('/api/admin/promotion-requests', { headers: authHeader() });
    const list = await res.json();
    if (!list.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-muted">No pending promotion requests.</td></tr>'; return;
    }
    tbody.innerHTML = list.map(r => `
      <tr>
        <td>${r.id}</td>
        <td><strong>${r.requesterUsername}</strong></td>
        <td class="text-muted">${r.reason || '—'}</td>
        <td class="text-muted" style="font-size:.75rem">${r.createdAt ? new Date(r.createdAt).toLocaleString() : '—'}</td>
        <td class="${r.status === 'PENDING' ? 'text-muted' : r.status === 'APPROVED' ? 'text-green' : 'text-red'}">${r.status}</td>
        <td class="text-muted" style="font-size:.75rem">${[r.approvedByAdmin, r.secondAdminUsername].filter(Boolean).join(' + ') || '—'}</td>
        <td class="action-cell">
          ${r.status === 'PENDING' ? `
            <button class="btn-sm" onclick="openDualAdminApprovalModal(${r.id}, '${r.requesterUsername}')">Approve</button>
            <button class="btn-sm danger" onclick="rejectPromotionRequest(${r.id})">Reject</button>
          ` : '—'}
        </td>
      </tr>`).join('');
  } catch { tbody.innerHTML = '<tr><td colspan="7" class="text-muted">Failed to load.</td></tr>'; }
}

function openRequestAdminModal() {
  document.getElementById('req-admin-reason').value = '';
  document.getElementById('req-admin-msg').className = 'alert hidden';
  document.getElementById('modal-request-admin').classList.remove('hidden');
}

async function submitAdminPromotionRequest() {
  const reason = document.getElementById('req-admin-reason').value.trim();
  const msgEl = document.getElementById('req-admin-msg');
  msgEl.className = 'alert hidden';
  if (!reason) { showMsg(msgEl, 'Please provide a reason.', 'alert-error'); return; }
  try {
    const res = await fetch('/api/auth/promotion-request', {
      method: 'POST',
      headers: { ...authHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason })
    });
    const data = await res.json();
    if (res.ok) {
      showMsg(msgEl, '✓ Request submitted. An administrator will review it.', 'alert-success');
      setTimeout(() => closeModal('modal-request-admin'), 1500);
    } else {
      showMsg(msgEl, data.error || data.message || 'Failed to submit.', 'alert-error');
    }
  } catch { showMsg(msgEl, 'Network error.', 'alert-error'); }
}

function openDualAdminApprovalModal(requestId, targetUsername) {
  _directPromoteTarget = null;
  document.getElementById('dual-auth-target-display').value = targetUsername + ' → ADMIN';
  document.getElementById('dual-auth-second-admin-user').value = '';
  document.getElementById('dual-auth-second-admin-pass').value = '';
  document.getElementById('dual-auth-request-id').value = requestId;
  document.getElementById('dual-auth-target-role').value = 'PROMOTION_REQUEST';
  document.getElementById('dual-auth-msg').className = 'alert hidden';
  document.getElementById('modal-dual-admin-approval').classList.remove('hidden');
}

async function rejectPromotionRequest(id) {
  if (!confirm('Reject this promotion request?')) return;
  const res = await fetch(`/api/admin/promotion-requests/${id}/reject`, {
    method: 'POST', headers: authHeader()
  });
  const data = await res.json();
  if (!res.ok) alert(data.error || 'Failed to reject.');
  loadAdminPromotionRequests();
}

// Stubs for Security Reports tab (section removed from HTML, kept to avoid JS errors)
function loadAdminSelectedUserReport() { }
function loadAdminReportsTab() { }

// ── Audit Logs ────────────────────────────────────────────────────────────────
async function loadAdminLogs() {
  const tbody = document.querySelector('#table-logs tbody');
  const severity = document.getElementById('log-severity-filter').value;
  tbody.innerHTML = '<tr><td colspan="6">Loading…</td></tr>';
  try {
    const url = severity ? `${API.admin.logs}?severity=${severity}` : API.admin.logs;
    const res = await fetch(url, { headers: authHeader() });
    const logs = await res.json();
    tbody.innerHTML = logs.map(l => `
      <tr>
        <td class="text-muted" style="font-size:.75rem;white-space:nowrap">${new Date(l.eventTime).toLocaleString()}</td>
        <td><strong>${l.username}</strong></td>
        <td style="font-family:monospace;font-size:.75rem">${l.eventType}</td>
        <td><span class="badge-sensitivity ${(l.severity || 'info').toLowerCase()}">${l.severity}</span></td>
        <td class="text-muted">${l.ipAddress || '—'}</td>
        <td class="text-muted">${l.description || ''}</td>
      </tr>`).join('');
  } catch { tbody.innerHTML = '<tr><td colspan="6">Failed to load.</td></tr>'; }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function authHeader() {
  return { 'Authorization': `Bearer ${currentSessionId}` };
}

function filterTable(tableId, query) {
  const q = query.toLowerCase();
  document.querySelectorAll(`#${tableId} tbody tr`).forEach(row => {
    row.style.display = row.innerText.toLowerCase().includes(q) ? '' : 'none';
  });
}

function closeModal(id) { document.getElementById(id).classList.add('hidden'); }
function closeResponseBox() { document.getElementById('resource-response-box').classList.add('hidden'); }

function showMsg(el, text, cls) {
  el.className = `alert ${cls}`;
  el.innerText = text;
}
