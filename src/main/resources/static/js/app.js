(() => {
    // ============ State ============
    const state = {
        user: null,           // { username }
        categories: [],       // [{ name, type, isCustom }]
        transactions: [],     // current view list
        goals: [],
        currentView: "transactions"
    };

    // ============ Helpers ============
    const $ = (sel) => document.querySelector(sel);
    const $$ = (sel) => Array.from(document.querySelectorAll(sel));

    const toastEl = $("#toast");
    let toastTimer = null;
    function toast(msg, kind = "") {
        toastEl.textContent = msg;
        toastEl.className = "toast show " + kind;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => {
            toastEl.className = "toast hidden " + kind;
        }, 2600);
    }

    function formatMoney(n) {
        const num = Number(n);
        if (isNaN(num)) return n;
        return num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function todayIso() {
        const d = new Date();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        return `${d.getFullYear()}-${m}-${day}`;
    }

    function fillSelect(select, options, { placeholder = null, valueKey = "value", labelKey = "label" } = {}) {
        select.innerHTML = "";
        if (placeholder !== null) {
            const opt = document.createElement("option");
            opt.value = "";
            opt.textContent = placeholder;
            select.appendChild(opt);
        }
        options.forEach((o) => {
            const opt = document.createElement("option");
            opt.value = typeof o === "string" ? o : o[valueKey];
            opt.textContent = typeof o === "string" ? o : o[labelKey];
            select.appendChild(opt);
        });
    }

    // ============ Auth ============
    function showAuth() {
        $("#authScreen").classList.remove("hidden");
        $("#dashboard").classList.add("hidden");
        $("#topbar").classList.add("hidden");
    }

    function showDashboard() {
        $("#authScreen").classList.add("hidden");
        $("#dashboard").classList.remove("hidden");
        $("#topbar").classList.remove("hidden");
        $("#currentUserLabel").textContent = state.user.username;
        switchView("transactions");
        refreshCategories().then(loadTransactions).then(loadGoals);
        // Prefill report year inputs
        $$("#monthlyForm input[name=year], #yearlyForm input[name=year]").forEach((el) => {
            el.value = new Date().getFullYear();
        });
        $("#monthlyForm select[name=month]").value = new Date().getMonth() + 1;
        $("#txForm input[name=date]").value = todayIso();
    }

    function bindAuthTabs() {
        $$(".auth-tab").forEach((btn) => {
            btn.addEventListener("click", () => {
                $$(".auth-tab").forEach((b) => b.classList.toggle("active", b === btn));
                const tab = btn.dataset.tab;
                $("#loginForm").classList.toggle("hidden", tab !== "login");
                $("#registerForm").classList.toggle("hidden", tab !== "register");
            });
        });
    }

    function bindAuthForms() {
        $("#loginForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const data = Object.fromEntries(new FormData(e.target).entries());
            $("#loginError").textContent = "";
            try {
                await Api.login(data);
                state.user = { username: data.username };
                toast("Welcome back!", "success");
                showDashboard();
            } catch (err) {
                $("#loginError").textContent = err.message;
            }
        });

        $("#registerForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const data = Object.fromEntries(new FormData(e.target).entries());
            $("#registerError").textContent = "";
            try {
                await Api.register(data);
                // auto-login after register
                await Api.login({ username: data.username, password: data.password });
                state.user = { username: data.username };
                toast("Account created", "success");
                showDashboard();
            } catch (err) {
                $("#registerError").textContent = err.message;
            }
        });

        $("#logoutBtn").addEventListener("click", async () => {
            try { await Api.logout(); } catch (_) { /* ignore */ }
            state.user = null;
            toast("Logged out");
            // reset forms
            $("#loginForm").reset();
            $("#registerForm").reset();
            showAuth();
        });
    }

    // ============ View switching ============
    function bindTabs() {
        $$(".tab").forEach((btn) => {
            btn.addEventListener("click", () => switchView(btn.dataset.view));
        });
    }

    function switchView(name) {
        state.currentView = name;
        $$(".tab").forEach((b) => b.classList.toggle("active", b.dataset.view === name));
        $$(".view").forEach((v) => v.classList.toggle("hidden", v.id !== `view-${name}`));
        if (name === "categories") loadCategories();
        if (name === "goals") loadGoals();
    }

    // ============ Categories ============
    async function refreshCategories() {
        try {
            const res = await Api.listCategories();
            state.categories = res.categories || [];
            populateCategorySelects();
        } catch (err) {
            toast(err.message, "error");
        }
    }

    function populateCategorySelects() {
        const txSelect = $("#txCategorySelect");
        fillSelect(txSelect, state.categories.map((c) => ({
            value: c.name,
            label: `${c.name} (${c.type})`
        })), { placeholder: "Select category" });

        const filterSelect = $("#txFilterCategory");
        fillSelect(filterSelect, state.categories.map((c) => ({
            value: c.name,
            label: `${c.name} (${c.type})`
        })), { placeholder: "All" });
    }

    async function loadCategories() {
        await refreshCategories();
        const tbody = $("#categoryTableBody");
        tbody.innerHTML = "";
        if (state.categories.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="muted">No categories.</td></tr>`;
            return;
        }
        state.categories.forEach((c) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${escapeHtml(c.name)}</td>
                <td><span class="badge ${c.type.toLowerCase()}">${c.type}</span></td>
                <td><span class="badge ${c.isCustom ? "custom" : "default"}">${c.isCustom ? "Custom" : "Default"}</span></td>
                <td>${c.isCustom ? `<button class="btn-danger" data-name="${escapeHtml(c.name)}">Delete</button>` : ""}</td>
            `;
            tbody.appendChild(tr);
        });
        tbody.querySelectorAll(".btn-danger").forEach((btn) => {
            btn.addEventListener("click", () => deleteCategory(btn.dataset.name));
        });
    }

    async function deleteCategory(name) {
        if (!confirm(`Delete custom category "${name}"?`)) return;
        try {
            await Api.deleteCategory(name);
            toast("Category deleted", "success");
            await loadCategories();
        } catch (err) {
            toast(err.message, "error");
        }
    }

    function bindCategoryForm() {
        $("#categoryForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const data = Object.fromEntries(new FormData(e.target).entries());
            $("#categoryError").textContent = "";
            try {
                await Api.createCategory(data);
                e.target.reset();
                toast("Category created", "success");
                await loadCategories();
            } catch (err) {
                $("#categoryError").textContent = err.message;
            }
        });
    }

    // ============ Transactions ============
    function bindTxForms() {
        $("#txForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            const payload = {
                amount: parseFloat(fd.get("amount")),
                date: fd.get("date"),
                category: fd.get("category"),
                description: fd.get("description") || null
            };
            $("#txError").textContent = "";
            try {
                await Api.createTransaction(payload);
                e.target.reset();
                $("#txForm input[name=date]").value = todayIso();
                toast("Transaction added", "success");
                await loadTransactions(currentFilters());
            } catch (err) {
                $("#txError").textContent = err.message;
            }
        });

        $("#txFilterForm").addEventListener("submit", (e) => {
            e.preventDefault();
            loadTransactions(currentFilters());
        });

        $("#txFilterReset").addEventListener("click", () => {
            $("#txFilterForm").reset();
            loadTransactions({});
        });
    }

    function currentFilters() {
        const fd = new FormData($("#txFilterForm"));
        const filters = {};
        if (fd.get("startDate")) filters.startDate = fd.get("startDate");
        if (fd.get("endDate")) filters.endDate = fd.get("endDate");
        if (fd.get("category")) filters.categoryName = fd.get("category");
        if (fd.get("type")) filters.type = fd.get("type");
        return filters;
    }

    async function loadTransactions(filters = {}) {
        try {
            // backend supports startDate, endDate, categoryId. We resolve categoryId locally.
            const params = { startDate: filters.startDate, endDate: filters.endDate };
            const res = await Api.listTransactions(params);
            let transactions = res.transactions || [];
            if (filters.categoryName) {
                transactions = transactions.filter((t) => t.category === filters.categoryName);
            }
            if (filters.type) {
                transactions = transactions.filter((t) => t.type === filters.type);
            }
            state.transactions = transactions;
            renderTransactions();
        } catch (err) {
            toast(err.message, "error");
        }
    }

    function renderTransactions() {
        const tbody = $("#txTableBody");
        tbody.innerHTML = "";
        if (state.transactions.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="muted">No transactions to show.</td></tr>`;
            return;
        }
        state.transactions.forEach((t) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${escapeHtml(t.date)}</td>
                <td>${escapeHtml(t.category)}</td>
                <td><span class="badge ${t.type.toLowerCase()}">${t.type}</span></td>
                <td class="right">${formatMoney(t.amount)}</td>
                <td>${escapeHtml(t.description || "")}</td>
                <td>
                    <button class="btn-link" data-action="edit" data-id="${t.id}">Edit</button>
                    <button class="btn-danger" data-action="delete" data-id="${t.id}">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        tbody.querySelectorAll("button[data-action]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const id = Number(btn.dataset.id);
                const tx = state.transactions.find((x) => x.id === id);
                if (btn.dataset.action === "delete") return deleteTransaction(id);
                if (btn.dataset.action === "edit") return editTransaction(tx);
            });
        });
    }

    async function deleteTransaction(id) {
        if (!confirm("Delete this transaction?")) return;
        try {
            await Api.deleteTransaction(id);
            toast("Deleted", "success");
            await loadTransactions(currentFilters());
        } catch (err) {
            toast(err.message, "error");
        }
    }

    async function editTransaction(tx) {
        const newAmount = prompt("New amount:", tx.amount);
        if (newAmount === null) return;
        const newCategory = prompt("New category (leave blank to keep):", tx.category) || tx.category;
        const newDescription = prompt("New description:", tx.description || "") ?? tx.description;
        const payload = {};
        const parsed = parseFloat(newAmount);
        if (!isNaN(parsed)) payload.amount = parsed;
        if (newCategory && newCategory !== tx.category) payload.category = newCategory;
        if (newDescription !== tx.description) payload.description = newDescription;
        try {
            await Api.updateTransaction(tx.id, payload);
            toast("Updated", "success");
            await loadTransactions(currentFilters());
        } catch (err) {
            toast(err.message, "error");
        }
    }

    // ============ Goals ============
    function bindGoalForm() {
        $("#goalForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            const payload = {
                goalName: fd.get("goalName"),
                targetAmount: parseFloat(fd.get("targetAmount")),
                targetDate: fd.get("targetDate")
            };
            if (fd.get("startDate")) payload.startDate = fd.get("startDate");
            $("#goalError").textContent = "";
            try {
                await Api.createGoal(payload);
                e.target.reset();
                toast("Goal created", "success");
                await loadGoals();
            } catch (err) {
                $("#goalError").textContent = err.message;
            }
        });
    }

    async function loadGoals() {
        try {
            const res = await Api.listGoals();
            state.goals = res.goals || [];
            renderGoals();
        } catch (err) {
            toast(err.message, "error");
        }
    }

    function renderGoals() {
        const container = $("#goalsList");
        container.innerHTML = "";
        if (state.goals.length === 0) {
            container.innerHTML = `<p class="muted">No goals yet.</p>`;
            return;
        }
        state.goals.forEach((g) => {
            const pct = Math.max(0, Math.min(100, Number(g.progressPercentage) || 0));
            const card = document.createElement("div");
            card.className = "goal-card";
            card.innerHTML = `
                <h3>${escapeHtml(g.goalName)}</h3>
                <div class="goal-row"><span>Target</span><strong>${formatMoney(g.targetAmount)}</strong></div>
                <div class="goal-row"><span>Progress</span><strong>${formatMoney(g.currentProgress)} (${pct.toFixed(2)}%)</strong></div>
                <div class="progress-bar"><span style="width:${pct}%"></span></div>
                <div class="goal-row"><span>Remaining</span><strong>${formatMoney(g.remainingAmount)}</strong></div>
                <div class="goal-row"><span>Period</span><span>${escapeHtml(g.startDate)} → ${escapeHtml(g.targetDate)}</span></div>
                <div class="goal-actions">
                    <button class="btn-link" data-action="edit" data-id="${g.id}">Update</button>
                    <button class="btn-danger" data-action="delete" data-id="${g.id}">Delete</button>
                </div>
            `;
            container.appendChild(card);
        });
        container.querySelectorAll("button[data-action]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const id = Number(btn.dataset.id);
                const goal = state.goals.find((x) => x.id === id);
                if (btn.dataset.action === "delete") return deleteGoal(id);
                if (btn.dataset.action === "edit") return editGoal(goal);
            });
        });
    }

    async function deleteGoal(id) {
        if (!confirm("Delete this goal?")) return;
        try {
            await Api.deleteGoal(id);
            toast("Goal deleted", "success");
            await loadGoals();
        } catch (err) {
            toast(err.message, "error");
        }
    }

    async function editGoal(goal) {
        const newAmount = prompt("New target amount:", goal.targetAmount);
        if (newAmount === null) return;
        const newDate = prompt("New target date (YYYY-MM-DD):", goal.targetDate);
        if (newDate === null) return;
        const payload = {};
        const parsed = parseFloat(newAmount);
        if (!isNaN(parsed)) payload.targetAmount = parsed;
        if (newDate) payload.targetDate = newDate;
        try {
            await Api.updateGoal(goal.id, payload);
            toast("Goal updated", "success");
            await loadGoals();
        } catch (err) {
            toast(err.message, "error");
        }
    }

    // ============ Reports ============
    function bindReports() {
        $("#monthlyForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                const data = await Api.monthlyReport(fd.get("year"), fd.get("month"));
                renderReport($("#monthlyResult"), data, true);
            } catch (err) {
                $("#monthlyResult").innerHTML = `<p class="form-error">${escapeHtml(err.message)}</p>`;
            }
        });

        $("#yearlyForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                const data = await Api.yearlyReport(fd.get("year"));
                renderReport($("#yearlyResult"), data, false);
            } catch (err) {
                $("#yearlyResult").innerHTML = `<p class="form-error">${escapeHtml(err.message)}</p>`;
            }
        });
    }

    function renderReport(container, data, isMonthly) {
        const incomeRows = renderMoneyRows(data.totalIncome);
        const expenseRows = renderMoneyRows(data.totalExpenses);
        const net = Number(data.netSavings || 0);
        const netClass = net >= 0 ? "positive" : "negative";
        const periodLabel = isMonthly
            ? `${monthName(data.month)} ${data.year}`
            : `Year ${data.year}`;
        container.innerHTML = `
            <div class="report-block">
                <h3>${periodLabel}</h3>
                <p class="net-savings ${netClass}">Net savings: ${formatMoney(net)}</p>
            </div>
            <div class="report-block">
                <h3>Income</h3>
                ${incomeRows || '<p class="muted">No income recorded.</p>'}
            </div>
            <div class="report-block">
                <h3>Expenses</h3>
                ${expenseRows || '<p class="muted">No expenses recorded.</p>'}
            </div>
        `;
    }

    function renderMoneyRows(map) {
        if (!map || Object.keys(map).length === 0) return "";
        const entries = Object.entries(map);
        let total = 0;
        const rows = entries.map(([name, amount]) => {
            total += Number(amount);
            return `<tr><td>${escapeHtml(name)}</td><td class="right">${formatMoney(amount)}</td></tr>`;
        }).join("");
        return `<table>${rows}<tr><td><strong>Total</strong></td><td class="right"><strong>${formatMoney(total)}</strong></td></tr></table>`;
    }

    function monthName(m) {
        return ["January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"][Number(m) - 1] || "";
    }

    // ============ Util ============
    function escapeHtml(s) {
        if (s === null || s === undefined) return "";
        return String(s)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // ============ Bootstrap ============
    async function init() {
        bindAuthTabs();
        bindAuthForms();
        bindTabs();
        bindCategoryForm();
        bindTxForms();
        bindGoalForm();
        bindReports();

        // Try to detect an existing session by hitting a protected endpoint.
        try {
            const res = await Api.listCategories();
            state.categories = res.categories || [];
            state.user = { username: "Signed in" };
            showDashboard();
        } catch (err) {
            showAuth();
        }
    }

    document.addEventListener("DOMContentLoaded", init);
})();
