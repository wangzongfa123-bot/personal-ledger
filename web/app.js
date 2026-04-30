const API = "/api";

const state = {
  categories: [],
  filterKind: "",
  filterCategory: "",
};

const $ = (sel) => document.querySelector(sel);

const fmt = {
  money(n) {
    return "¥" + Number(n || 0).toFixed(2);
  },
  time(iso) {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (x) => String(x).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
      d.getHours()
    )}:${pad(d.getMinutes())}`;
  },
};

async function api(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    let msg = res.statusText;
    try {
      const data = await res.json();
      msg = data.detail || msg;
    } catch (_) {}
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

// 设置默认时间为当前
function setDefaultDateTime() {
  const now = new Date();
  const tz = now.getTimezoneOffset() * 60_000;
  const local = new Date(now - tz).toISOString().slice(0, 16);
  $("#occurred-at").value = local;
}

async function loadCategories() {
  state.categories = await api("/categories");
  renderCategorySelects();
}

function renderCategorySelects() {
  const kindEl = document.querySelector('input[name="kind"]:checked');
  const kind = kindEl ? kindEl.value : "expense";

  const catSel = $("#category-select");
  catSel.innerHTML = "";
  state.categories
    .filter((c) => c.kind === kind)
    .forEach((c) => {
      const opt = document.createElement("option");
      opt.value = c.id;
      opt.textContent = `${c.icon || ""} ${c.name}`.trim();
      catSel.appendChild(opt);
    });

  const filterSel = $("#filter-category");
  const cur = filterSel.value;
  filterSel.innerHTML = '<option value="">全部分类</option>';
  state.categories.forEach((c) => {
    const opt = document.createElement("option");
    opt.value = c.id;
    opt.textContent = `${c.icon || ""} ${c.name} (${c.kind === "income" ? "收" : "支"})`;
    filterSel.appendChild(opt);
  });
  filterSel.value = cur;
}

function renderTransactions(items) {
  const list = $("#tx-list");
  if (!items.length) {
    list.innerHTML = '<div class="empty">暂无账单，去记一笔吧～</div>';
    return;
  }
  list.innerHTML = items
    .map(
      (t) => `
      <div class="tx-item" data-id="${t.id}">
        <div class="tx-icon">${t.category.icon || (t.kind === "income" ? "＋" : "－")}</div>
        <div class="tx-meta">
          <div class="tx-cat">${t.category.name}</div>
          <div class="tx-note">${t.note ? escapeHtml(t.note) : "—"}</div>
        </div>
        <div class="tx-time">${fmt.time(t.occurred_at)}</div>
        <div class="tx-amount ${t.kind}">${t.kind === "income" ? "+" : "-"}${fmt.money(t.amount)}</div>
        <button class="tx-actions" data-action="delete" data-id="${t.id}" title="删除">删除</button>
      </div>`
    )
    .join("");
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])
  );
}

async function loadTransactions() {
  const params = new URLSearchParams();
  if (state.filterKind) params.set("kind", state.filterKind);
  if (state.filterCategory) params.set("category_id", state.filterCategory);
  params.set("limit", "200");
  const items = await api("/transactions?" + params.toString());
  renderTransactions(items);
}

async function loadStats() {
  const s = await api("/stats/summary");
  $("#stat-income").textContent = fmt.money(s.income);
  $("#stat-expense").textContent = fmt.money(s.expense);
  $("#stat-balance").textContent = fmt.money(s.balance);
  $("#stat-count").textContent = s.count;

  const grid = $("#by-category");
  if (!s.by_category.length) {
    grid.innerHTML = '<div class="empty">还没有数据</div>';
    return;
  }
  grid.innerHTML = s.by_category
    .map(
      (c) => `
      <div class="cat-item">
        <div>
          <div class="name">${escapeHtml(c.category_name)}</div>
          <div class="meta">${c.kind === "income" ? "收入" : "支出"} · ${c.count} 笔</div>
        </div>
        <div class="total ${c.kind}">${c.kind === "income" ? "+" : "-"}${fmt.money(c.total)}</div>
      </div>`
    )
    .join("");
}

async function refreshAll() {
  await Promise.all([loadTransactions(), loadStats()]);
}

function showTip(msg, type = "") {
  const tip = $("#form-tip");
  tip.textContent = msg;
  tip.className = "form-tip " + type;
  if (msg) {
    setTimeout(() => {
      tip.textContent = "";
      tip.className = "form-tip";
    }, 2500);
  }
}

async function handleSubmit(e) {
  e.preventDefault();
  const form = e.target;
  const fd = new FormData(form);

  const payload = {
    amount: Number(fd.get("amount")),
    kind: fd.get("kind"),
    category_id: Number(fd.get("category_id")),
    note: (fd.get("note") || "").toString().trim() || null,
  };
  const occurred = fd.get("occurred_at");
  if (occurred) {
    payload.occurred_at = new Date(occurred).toISOString();
  }

  try {
    await api("/transactions", { method: "POST", body: JSON.stringify(payload) });
    form.reset();
    setDefaultDateTime();
    document.querySelector('input[name="kind"][value="expense"]').checked = true;
    renderCategorySelects();
    showTip("已保存", "success");
    await refreshAll();
  } catch (err) {
    showTip("保存失败：" + err.message, "error");
  }
}

async function handleListClick(e) {
  const btn = e.target.closest('[data-action="delete"]');
  if (!btn) return;
  const id = btn.dataset.id;
  if (!confirm("确定删除这笔账单？")) return;
  try {
    await api(`/transactions/${id}`, { method: "DELETE" });
    await refreshAll();
  } catch (err) {
    alert("删除失败：" + err.message);
  }
}

function bindEvents() {
  $("#tx-form").addEventListener("submit", handleSubmit);
  $("#tx-list").addEventListener("click", handleListClick);
  $("#btn-refresh").addEventListener("click", refreshAll);
  document.querySelectorAll('input[name="kind"]').forEach((el) => {
    el.addEventListener("change", renderCategorySelects);
  });
  $("#filter-kind").addEventListener("change", (e) => {
    state.filterKind = e.target.value;
    loadTransactions();
  });
  $("#filter-category").addEventListener("change", (e) => {
    state.filterCategory = e.target.value;
    loadTransactions();
  });
}

(async function init() {
  setDefaultDateTime();
  bindEvents();
  try {
    await loadCategories();
    await refreshAll();
  } catch (err) {
    showTip("加载失败：" + err.message, "error");
  }
})();
