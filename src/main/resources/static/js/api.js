/**
 * Thin wrapper around fetch that always sends the session cookie
 * (same-origin so credentials are sent by default, but include is explicit)
 * and normalises errors into { status, message } thrown objects.
 */
const Api = (() => {
    const BASE = "/api";

    async function request(method, path, body) {
        const opts = {
            method,
            credentials: "include",
            headers: {}
        };
        if (body !== undefined) {
            opts.headers["Content-Type"] = "application/json";
            opts.body = JSON.stringify(body);
        }
        const res = await fetch(BASE + path, opts);
        const text = await res.text();
        const data = text ? safeParse(text) : null;
        if (!res.ok) {
            const msg = (data && data.message) || res.statusText || "Request failed";
            const err = new Error(msg);
            err.status = res.status;
            err.data = data;
            throw err;
        }
        return data;
    }

    function safeParse(text) {
        try { return JSON.parse(text); } catch { return text; }
    }

    return {
        // Auth
        register: (payload) => request("POST", "/auth/register", payload),
        login: (payload) => request("POST", "/auth/login", payload),
        logout: () => request("POST", "/auth/logout"),

        // Transactions
        listTransactions: (params = {}) => {
            const q = new URLSearchParams();
            if (params.startDate) q.set("startDate", params.startDate);
            if (params.endDate) q.set("endDate", params.endDate);
            if (params.categoryId) q.set("categoryId", params.categoryId);
            const qs = q.toString();
            return request("GET", "/transactions" + (qs ? "?" + qs : ""));
        },
        createTransaction: (payload) => request("POST", "/transactions", payload),
        updateTransaction: (id, payload) => request("PUT", `/transactions/${id}`, payload),
        deleteTransaction: (id) => request("DELETE", `/transactions/${id}`),

        // Categories
        listCategories: () => request("GET", "/categories"),
        createCategory: (payload) => request("POST", "/categories", payload),
        deleteCategory: (name) => request("DELETE", `/categories/${encodeURIComponent(name)}`),

        // Goals
        listGoals: () => request("GET", "/goals"),
        getGoal: (id) => request("GET", `/goals/${id}`),
        createGoal: (payload) => request("POST", "/goals", payload),
        updateGoal: (id, payload) => request("PUT", `/goals/${id}`, payload),
        deleteGoal: (id) => request("DELETE", `/goals/${id}`),

        // Reports
        monthlyReport: (year, month) => request("GET", `/reports/monthly/${year}/${month}`),
        yearlyReport: (year) => request("GET", `/reports/yearly/${year}`)
    };
})();
