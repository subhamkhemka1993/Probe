(() => {
  const TOKEN_KEY = "probe-browser-token";
  const POLL_INTERVAL_MS = 2000;
  const SEARCH_DEBOUNCE_MS = 250;

  const app = document.getElementById("app");
  const searchInput = document.getElementById("search");
  const state = {
    calls: [],
    selectedId: null,
    selectedTab: "overview",
    search: "",
    token: readToken(),
    socket: null,
    pollTimer: null,
    isExpired: false,
    status: "connecting",
  };

  function readToken() {
    const params = new URLSearchParams(window.location.search);
    const tokenFromUrl = params.get("token");
    if (tokenFromUrl) {
      sessionStorage.setItem(TOKEN_KEY, tokenFromUrl);
      return tokenFromUrl;
    }
    return sessionStorage.getItem(TOKEN_KEY) || "";
  }

  function authHeaders() {
    return state.token ? { Authorization: `Bearer ${state.token}` } : {};
  }

  async function api(path, options = {}) {
    const response = await fetch(path, {
      ...options,
      headers: {
        ...authHeaders(),
        ...(options.headers || {}),
      },
    });
    if (response.status === 401) {
      expireSession();
      throw new Error("unauthorized");
    }
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    return response.json();
  }

  function expireSession() {
    state.isExpired = true;
    state.status = "expired";
    stopPolling();
    if (state.socket) {
      state.socket.close();
      state.socket = null;
    }
    render();
  }

  async function loadCalls() {
    if (!state.token || state.isExpired) {
      render();
      return;
    }
    const search = encodeURIComponent(state.search);
    const data = await api(`/api/v1/calls?search=${search}`);
    state.calls = sortCalls(data.calls || []);
    if (!state.selectedId && state.calls.length > 0) {
      state.selectedId = state.calls[0].id;
    }
    if (state.selectedId && !state.calls.some((call) => call.id === state.selectedId)) {
      state.selectedId = state.calls[0]?.id || null;
    }
    render();
  }

  function mergeCall(updatedCall) {
    const existingIndex = state.calls.findIndex((call) => call.id === updatedCall.id);
    if (existingIndex >= 0) {
      state.calls[existingIndex] = updatedCall;
    } else {
      state.calls.unshift(updatedCall);
    }
    state.calls = sortCalls(state.calls);
    if (!state.selectedId) {
      state.selectedId = updatedCall.id;
    }
    render();
  }

  function sortCalls(calls) {
    return [...calls].sort((left, right) => {
      return Date.parse(right.timestampIso || 0) - Date.parse(left.timestampIso || 0);
    });
  }

  function connectWebSocket() {
    if (!state.token || state.isExpired) {
      return;
    }
    stopPolling();
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const socketUrl = `${protocol}//${window.location.host}/ws/v1/calls?token=${encodeURIComponent(state.token)}`;
    const socket = new WebSocket(socketUrl);
    state.socket = socket;
    state.status = "connecting";
    render();

    socket.onopen = () => {
      state.status = "live";
      render();
    };
    socket.onmessage = (event) => {
      const payload = safeJson(event.data);
      if (payload?.call && (!payload.type || payload.type === "call_updated")) {
        mergeCall(payload.call);
      }
    };
    socket.onclose = () => {
      if (state.socket === socket) {
        state.socket = null;
        state.status = "polling";
        startPolling();
        render();
      }
    };
    socket.onerror = () => {
      state.status = "polling";
      socket.close();
    };
  }

  function startPolling() {
    if (state.pollTimer || state.isExpired) {
      return;
    }
    state.pollTimer = window.setInterval(() => {
      loadCalls().catch(() => {});
    }, POLL_INTERVAL_MS);
  }

  function stopPolling() {
    if (state.pollTimer) {
      window.clearInterval(state.pollTimer);
      state.pollTimer = null;
    }
  }

  async function clearCalls() {
    await api("/api/v1/calls", { method: "DELETE" });
    state.calls = [];
    state.selectedId = null;
    render();
  }

  async function copyCurl(id) {
    const data = await api(`/api/v1/calls/${encodeURIComponent(id)}/curl`);
    await navigator.clipboard.writeText(data.curl || "");
  }

  function render() {
    if (!state.token) {
      app.innerHTML = `<div class="expired">Missing token. Re-open Probe and copy a fresh browser link.</div>`;
      return;
    }
    const selectedCall = state.calls.find((call) => call.id === state.selectedId) || null;
    app.innerHTML = `
      ${state.isExpired ? `<div class="expired">Session expired - re-open Probe and copy new link.</div>` : ""}
      <section class="toolbar">
        <div class="connection">status: ${escapeHtml(state.status)} | calls: ${state.calls.length}</div>
        <button id="clearCalls" type="button">Clear calls</button>
      </section>
      <section class="layout">
        <div class="panel">
          ${renderTable()}
        </div>
        <aside class="panel">
          ${selectedCall ? renderDetail(selectedCall) : `<div class="empty">Select a network call</div>`}
        </aside>
      </section>
    `;
    app.querySelector("#clearCalls")?.addEventListener("click", () => {
      clearCalls().catch((error) => showError(error.message));
    });
    app.querySelectorAll("[data-call-id]").forEach((row) => {
      row.addEventListener("click", () => {
        state.selectedId = row.getAttribute("data-call-id");
        render();
      });
    });
    app.querySelectorAll("[data-tab]").forEach((tab) => {
      tab.addEventListener("click", () => {
        state.selectedTab = tab.getAttribute("data-tab") || "overview";
        render();
      });
    });
    app.querySelector("#copyCurl")?.addEventListener("click", () => {
      if (!selectedCall) return;
      copyCurl(selectedCall.id)
        .then(() => {
          const button = app.querySelector("#copyCurl");
          if (button) button.textContent = "Copied";
        })
        .catch((error) => showError(error.message));
    });
  }

  function renderTable() {
    if (state.calls.length === 0) {
      return `<div class="empty">No calls captured</div>`;
    }
    const rows = state.calls.map((call) => {
      const selected = call.id === state.selectedId ? "selected" : "";
      return `
        <tr class="${selected}" data-call-id="${escapeAttr(call.id)}">
          <td>${methodBadge(call.method)}</td>
          <td><div class="path">${escapeHtml(call.path || call.url || "")}</div><div class="muted">${escapeHtml(call.host || "")}</div></td>
          <td>${statusBadge(call.responseStatus, call.error, call.isComplete)}</td>
          <td>${formatDuration(call.durationMs)}</td>
          <td>${formatTime(call.timestampIso)}</td>
        </tr>
      `;
    }).join("");
    return `
      <table>
        <thead>
          <tr>
            <th>Method</th>
            <th>Path</th>
            <th>Status</th>
            <th>Duration</th>
            <th>Time</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    `;
  }

  function renderDetail(call) {
    const tabs = ["overview", "request", "response"].map((tab) => {
      const active = tab === state.selectedTab ? "active" : "";
      return `<button class="tab ${active}" type="button" data-tab="${tab}">${titleCase(tab)}</button>`;
    }).join("");
    return `
      <div class="detail-header">
        <div>
          ${methodBadge(call.method)}
          ${statusBadge(call.responseStatus, call.error, call.isComplete)}
          <h2 class="detail-title">${escapeHtml(call.url || call.path || "")}</h2>
        </div>
        <button id="copyCurl" type="button">Copy cURL</button>
      </div>
      <div class="tabs">${tabs}</div>
      <div class="detail-body">${renderTab(call)}</div>
    `;
  }

  function renderTab(call) {
    if (state.selectedTab === "request") {
      return renderPayload(call.requestHeaders, call.requestBody);
    }
    if (state.selectedTab === "response") {
      return renderPayload(call.responseHeaders, call.responseBody || call.error);
    }
    return `
      <dl class="kv">
        <dt>ID</dt><dd>${escapeHtml(call.id)}</dd>
        <dt>Host</dt><dd>${escapeHtml(call.host || "")}</dd>
        <dt>Path</dt><dd>${escapeHtml(call.path || "")}</dd>
        <dt>Query</dt><dd>${escapeHtml(call.query || "-")}</dd>
        <dt>Started</dt><dd>${escapeHtml(call.timestampIso || "")}</dd>
        <dt>Duration</dt><dd>${formatDuration(call.durationMs)}</dd>
        <dt>Complete</dt><dd>${call.isComplete ? "yes" : "no"}</dd>
        <dt>Error</dt><dd>${escapeHtml(call.error || "-")}</dd>
      </dl>
    `;
  }

  function renderPayload(headers, body) {
    return `
      <pre>${escapeHtml(formatHeaders(headers))}</pre>
      <pre>${escapeHtml(formatBody(body))}</pre>
    `;
  }

  function formatHeaders(headers) {
    const entries = Object.entries(headers || {});
    if (entries.length === 0) {
      return "No headers";
    }
    return entries.map(([key, value]) => `${key}: ${value}`).join("\n");
  }

  function formatBody(body) {
    if (!body) {
      return "No body";
    }
    const json = safeJson(body);
    return json ? JSON.stringify(json, null, 2) : body;
  }

  function methodBadge(method) {
    const value = (method || "-").toUpperCase();
    return `<span class="method-badge ${escapeAttr(value.toLowerCase())}">${escapeHtml(value)}</span>`;
  }

  function statusBadge(status, error, isComplete) {
    if (error) {
      return `<span class="status-badge error">ERR</span>`;
    }
    if (!isComplete) {
      return `<span class="status-badge warn">...</span>`;
    }
    if (!status) {
      return `<span class="status-badge warn">-</span>`;
    }
    const className = status >= 400 ? "error" : status >= 300 ? "warn" : "";
    return `<span class="status-badge ${className}">${status}</span>`;
  }

  function formatDuration(durationMs) {
    return durationMs == null ? "-" : `${durationMs}ms`;
  }

  function formatTime(timestampIso) {
    if (!timestampIso) {
      return "-";
    }
    const date = new Date(timestampIso);
    if (Number.isNaN(date.getTime())) {
      return timestampIso;
    }
    return date.toLocaleTimeString();
  }

  function showError(message) {
    state.status = message || "error";
    render();
  }

  function safeJson(value) {
    try {
      return JSON.parse(value);
    } catch (_error) {
      return null;
    }
  }

  function titleCase(value) {
    return value.charAt(0).toUpperCase() + value.slice(1);
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function escapeAttr(value) {
    return escapeHtml(value);
  }

  function debounce(callback, delayMs) {
    let timer = null;
    return (...args) => {
      window.clearTimeout(timer);
      timer = window.setTimeout(() => callback(...args), delayMs);
    };
  }

  searchInput.addEventListener("input", debounce((event) => {
    state.search = event.target.value;
    loadCalls().catch((error) => showError(error.message));
  }, SEARCH_DEBOUNCE_MS));

  render();
  loadCalls()
    .then(connectWebSocket)
    .catch((error) => showError(error.message));
})();
