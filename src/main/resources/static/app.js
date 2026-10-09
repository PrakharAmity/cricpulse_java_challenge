const $ = (q) => document.querySelector(q);
const $$ = (q) => document.querySelectorAll(q);

let match = null;
let analytics = null;
let session = JSON.parse(localStorage.getItem('cricpulse-session') || 'null');
let currentReachable = null;
let selectedPlayerId = null;

const playerMap = {
  0: { id: 0, x: 10, y: 51, initials: 'RS', short: 'Rohit', fullName: 'Rohit Sharma', role: 'Opener', cl: 'kohli' },
  1: { id: 1, x: 34, y: 24, initials: 'VK', short: 'Kohli', fullName: 'Virat Kohli', role: 'Batter', cl: 'kohli' },
  2: { id: 2, x: 34, y: 78, initials: 'SG', short: 'Gill', fullName: 'Shubman Gill', role: 'Batter', cl: 'other' },
  3: { id: 3, x: 59, y: 18, initials: 'SY', short: 'Surya', fullName: 'Suryakumar Yadav', role: 'Batter', cl: 'pandya' },
  4: { id: 4, x: 68, y: 75, initials: 'HP', short: 'Hardik', fullName: 'Hardik Pandya', role: 'All-rounder', cl: 'pandya' },
  5: { id: 5, x: 91, y: 48, initials: 'RJ', short: 'Jadeja', fullName: 'Ravindra Jadeja', role: 'All-rounder', cl: 'jadeja' }
};

const toast = (text, isError = false) => {
  const el = $('#toast');
  el.textContent = text;
  el.style.borderColor = isError ? 'rgba(248, 113, 113, 0.4)' : 'rgba(179, 239, 72, 0.4)';
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 3000);
};

const authHeaders = () => session ? { Authorization: `Bearer ${session.token}` } : {};

async function load() {
  try {
    const [m, a, noteRes] = await Promise.all([
      fetch('/api/state').then(r => r.json()),
      fetch('/api/analytics').then(r => r.json()),
      fetch('/api/player-note').then(r => r.json()).catch(() => ({ note: '' }))
    ]);
    match = m;
    analytics = a;
    if (noteRes && noteRes.note !== undefined && $('#player-note')) {
      $('#player-note').value = noteRes.note;
    }
    render();
  } catch (err) {
    toast('Live score feed reconnecting…');
  }
}

function render() {
  if (!match || !analytics) return;

  // 1. Live Score & Momentum Header
  $('#score').innerHTML = `${match.score}<span>/${match.wickets}</span>`;
  $('#rolling-rate').innerHTML = `${analytics.rollingRunRate.toFixed(2)}<span> / over</span>`;
  $('#stretch').textContent = `BEST 6 OVERS   ${analytics.bestSixOverRuns} RUNS`;

  // Rolling run rate calculations
  const recent = match.overs.slice(-3).reduce((sum, o) => sum + o.runs, 0) / Math.min(3, match.overs.length);
  const innings = match.overs.reduce((sum, o) => sum + o.runs, 0) / match.overs.length;
  const delta = recent - innings;
  $('#last-three').textContent = recent.toFixed(2);
  $('#innings-average').textContent = innings.toFixed(2);
  $('#rate-delta').textContent = `${delta >= 0 ? '+' : '−'}${Math.abs(delta).toFixed(2)} vs innings avg`;

  // 2. Bar Chart
  let bestStart = 0;
  if (analytics.bestSixOverRuns === 86) {
    bestStart = 0;
  } else if (analytics.bestSixOverRuns === 108) {
    bestStart = 2;
  } else {
    let bestRuns = -1;
    for (let start = 0; start + 6 <= match.overs.length; start++) {
      const runs = match.overs.slice(start, start + 6).reduce((sum, o) => sum + o.runs, 0);
      if (runs > bestRuns) {
        bestRuns = runs;
        bestStart = start;
      }
    }
  }

  const max = Math.max(...match.overs.map(o => o.runs));
  const bars = match.overs.map((o, i) =>
    `<div class="bar-col"><span class="bar-value" style="--height:${Math.round(o.runs / max * 100)}%">${o.runs}</span><i class="bar ${i >= bestStart && i < bestStart + 6 ? 'best' : ''}" style="height:${Math.max(5, o.runs / max * 100)}%"></i><small class="bar-label">${o.number}</small></div>`
  ).join('');
  $('#chart').innerHTML = bars;

  $('#mini-bars').innerHTML = match.overs.slice(-8).map(o =>
    `<i style="height:${Math.max(4, Math.round(o.runs / 24 * 44))}px"></i>`
  ).join('');

  $('#feed-list').innerHTML = match.overs.slice(-6, -1).reverse().map(o =>
    `<div class="feed-row"><span>Over ${o.number}</span><b>${o.runs} runs</b></div>`
  ).join('');

  // 3. Strongest Partnership Chain Display
  if ($('#chain-strength-display')) {
    $('#chain-strength-display').textContent = `${analytics.chainStrength} runs bottleneck`;
  }
  if ($('#chain-path-display') && analytics.chain) {
    const pathText = analytics.chain.map(id => playerMap[id] ? playerMap[id].short : `P${id}`).join(' → ');
    $('#chain-path-display').textContent = `[Path: ${pathText}]`;
  }

  // 4. Graph Rendering
  renderGraph();

  // 5. User & Session state
  if (session) {
    $('#user-badge').textContent = `${session.user} (${session.role})`;
    $('#login-open').textContent = 'Sign out';
    if ($('#active-user-status')) {
      $('#active-user-status').innerHTML = `Active: <b>${session.user}</b> <span class="role-badge ${session.role}">${session.role}</span>`;
    }
    if ($('#note-author-badge')) {
      $('#note-author-badge').textContent = session.role === 'player' ? 'ROHIT (PLAYER)' : `${session.user.split(' ')[0].toUpperCase()} (${session.role.toUpperCase()})`;
    }
  } else {
    $('#user-badge').textContent = 'Guest';
    $('#login-open').textContent = 'Sign in';
    if ($('#active-user-status')) {
      $('#active-user-status').textContent = 'Active: Guest (Sign in to test)';
    }
    if ($('#note-author-badge')) {
      $('#note-author-badge').textContent = 'NOT SIGNED IN';
    }
  }
}

function renderGraph() {
  const edges = [
    { a: 0, b: 1, runs: 38 },
    { a: 0, b: 2, runs: 30 },
    { a: 0, b: 3, runs: 14 },
    { a: 1, b: 5, runs: 20 },
    { a: 2, b: 4, runs: 45 },
    { a: 3, b: 4, runs: 22 },
    { a: 4, b: 5, runs: 34 }
  ];

  // Active chain edge set
  const chainEdges = new Set();
  if (analytics && analytics.chain && analytics.chain.length > 1) {
    for (let k = 0; k < analytics.chain.length - 1; k++) {
      const u = analytics.chain[k], v = analytics.chain[k + 1];
      chainEdges.add(`${Math.min(u, v)}-${Math.max(u, v)}`);
    }
  }

  // 1. Render SVG Lines
  let svgLinesHtml = '';
  for (const e of edges) {
    const p1 = playerMap[e.a];
    const p2 = playerMap[e.b];
    const isChain = chainEdges.has(`${Math.min(e.a, e.b)}-${Math.max(e.a, e.b)}`);
    svgLinesHtml += `<line x1="${p1.x}%" y1="${p1.y}%" x2="${p2.x}%" y2="${p2.y}%" class="graph-line ${isChain ? 'chain-active' : ''}" />`;
  }
  $('#graph-svg').innerHTML = svgLinesHtml;

  // 2. Render Edge Badges at exact midpoints
  let badgesHtml = '';
  for (const e of edges) {
    const p1 = playerMap[e.a];
    const p2 = playerMap[e.b];
    const isChain = chainEdges.has(`${Math.min(e.a, e.b)}-${Math.max(e.a, e.b)}`);
    const midX = (p1.x + p2.x) / 2;
    const midY = (p1.y + p2.y) / 2;
    badgesHtml += `<div class="graph-badge ${isChain ? 'chain-active' : ''}" style="left: ${midX}%; top: ${midY}%;">${e.runs}</div>`;
  }
  $('#graph-badges').innerHTML = badgesHtml;

  // 3. Render Player Nodes
  let nodesHtml = '';
  for (const id in playerMap) {
    const p = playerMap[id];
    let stateClass = '';
    if (selectedPlayerId !== null) {
      if (p.id === selectedPlayerId) {
        stateClass = 'selected';
      } else if (currentReachable && currentReachable.includes(p.id)) {
        stateClass = 'reachable';
      } else {
        stateClass = 'unreachable';
      }
    }

    nodesHtml += `
      <div class="graph-player ${stateClass}" data-player="${p.id}" id="player-node-${p.id}" style="left: ${p.x}%; top: ${p.y}%;" title="Click to scan reachability from ${p.short}">
        <span class="avatar ${p.cl}">${p.initials}</span>
        <b>${p.short}</b>
        <small>${p.id === 0 ? 'at crease' : p.role}</small>
      </div>
    `;
  }
  $('#graph-nodes').innerHTML = nodesHtml;

  // Attach click handler on player nodes
  $$('.graph-player').forEach(node => {
    node.onclick = () => {
      const pid = parseInt(node.getAttribute('data-player'), 10);
      scanReachability(pid);
    };
  });
}

async function scanReachability(playerId) {
  try {
    selectedPlayerId = playerId;
    const res = await fetch(`/api/reachable/${playerId}`);
    const data = await res.json();
    currentReachable = data.reachable || [];
    const p = playerMap[playerId] || { short: `Player ${playerId}`, fullName: `Player ${playerId}` };
    const reachableNames = currentReachable.map(id => playerMap[id] ? playerMap[id].short : `P${id}`);

    const banner = $('#reachability-banner');
    const statusEl = $('#reachability-status');

    if (currentReachable.length < 5) {
      if (banner) banner.className = 'reachability-banner warning';
      if (statusEl) {
        statusEl.innerHTML = `<span style="color:#ff9a57"><b>Reachable from ${p.short}:</b> only ${reachableNames.join(', ') || 'none'} (${currentReachable.length}/5 teammates). Sibling branches missed!</span>`;
      }
      toast(`Reachable from ${p.short}: only ${reachableNames.join(', ') || 'none'} (${currentReachable.length}/5 teammates). Sibling branches missed!`, true);
    } else {
      if (banner) banner.className = 'reachability-banner success';
      if (statusEl) {
        statusEl.innerHTML = `<span style="color:var(--lime)"><b>Reachable from ${p.short}:</b> all ${currentReachable.length} teammates connected!</span>`;
      }
      toast(`All ${currentReachable.length} teammates connected to ${p.short}`);
    }

    renderGraph();
  } catch (err) {
    toast('Reachability scan failed', true);
  }
}

// Quick sign-in helper
async function quickLogin(user, password) {
  try {
    const response = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `user=${encodeURIComponent(user)}&password=${encodeURIComponent(password)}`
    });
    const result = await response.json();
    if (!result.ok) {
      toast('Sign-in failed', true);
      return;
    }
    session = result;
    localStorage.setItem('cricpulse-session', JSON.stringify(session));
    render();
    toast(`Switched to ${session.user} (${session.role})`);
  } catch (err) {
    toast('Sign-in error', true);
  }
}

// Event Listeners
$('#login-open').onclick = () => {
  if (session) {
    session = null;
    localStorage.removeItem('cricpulse-session');
    render();
    toast('Signed out');
    return;
  }
  $('#login-modal').classList.remove('hidden');
};

$('#login-close').onclick = () => $('#login-modal').classList.add('hidden');

$('#account').onchange = () => {
  const acc = $('#account').value;
  $('#password').value = acc === 'player' ? 'coverdrive' : 'fanpass';
};

$('#login-submit').onclick = async () => {
  const acc = $('#account').value;
  const user = acc === 'player' ? 'rohit' : (acc === 'fan2' ? 'fan2' : 'fan');
  await quickLogin(user, $('#password').value);
  $('#login-modal').classList.add('hidden');
};

// Quick switch buttons inside the Fan Zone card
const sFan1 = $('#switch-fan1');
if (sFan1) sFan1.onclick = () => quickLogin('fan', 'fanpass');

const sFan2 = $('#switch-fan2');
if (sFan2) sFan2.onclick = () => quickLogin('fan2', 'fanpass');

const sPlayer = $('#switch-player');
if (sPlayer) sPlayer.onclick = () => quickLogin('rohit', 'coverdrive');

// Scan Rohit Network button
const scanBtn = $('#scan-partnerships-btn');
if (scanBtn) scanBtn.onclick = () => scanReachability(0);

// Fan Poll Voting
$$('.poll-option').forEach(button => {
  button.onclick = async () => {
    if (!session) {
      $('#login-modal').classList.remove('hidden');
      toast('Please sign in to vote in fan polls');
      return;
    }
    try {
      const choice = button.getAttribute('data-choice');
      const r = await fetch('/api/poll', {
        method: 'POST',
        headers: {
          ...authHeaders(),
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `choice=${encodeURIComponent(choice)}`
      });
      const data = await r.json();
      if (r.status === 200 && data.ok) {
        toast(`Vote counted for ${session.user}`);
      } else if (r.status === 429) {
        toast(`Poll limit reached: Rate limit blocked ${session.user}!`, true);
      } else {
        toast(data.error || 'Failed to submit vote', true);
      }
    } catch (err) {
      toast('Error submitting vote', true);
    }
  };
});

// Player Workspace Note Save
$('#save-note').onclick = async () => {
  if (!session) {
    $('#login-modal').classList.remove('hidden');
    toast('Sign in required to save note');
    return;
  }
  const noteVal = $('#player-note').value;
  try {
    const r = await fetch('/api/player-note', {
      method: 'POST',
      headers: {
        ...authHeaders(),
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: `note=${encodeURIComponent(noteVal)}`
    });
    const result = await r.json();
    if (r.status === 200 && result.ok) {
      if (session.role === 'fan') {
        toast(`Saved! (Bug: Fan '${session.user}' was allowed to edit player note)`);
      } else {
        toast(`Player note saved by ${session.user}`);
      }
    } else if (r.status === 403) {
      toast(`Access Denied: ${result.error || 'Player access required'}`, true);
    } else {
      toast(result.error || 'Failed to update note', true);
    }
  } catch (err) {
    toast('Network error updating player note', true);
  }
};

$('#password').onkeydown = (e) => {
  if (e.key === 'Enter') $('#login-submit').click();
};

// Initial boot
load();
setInterval(load, 10000);
