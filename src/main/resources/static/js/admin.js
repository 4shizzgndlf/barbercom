const csrf = () => document.cookie.split("; ").find(value => value.startsWith("XSRF-TOKEN="))?.split("=").slice(1).join("=");
let dashboard;
const dashboardPageSize = 10;
const dashboardPages = { users: 1, businesses: 1 };

const money = value => `${Number(value || 0).toFixed(2).replace(".", ",")} DKK`;
const escapeHtml = value => String(value ?? "").replace(/[&<>'"]/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[character]));

function renderPagedList(container, key, entries, renderEntry, emptyMessage) {
    const pageCount = Math.max(1, Math.ceil(entries.length / dashboardPageSize));
    const page = Math.min(dashboardPages[key], pageCount);
    dashboardPages[key] = page;
    if (!entries.length) {
        container.innerHTML = `<p class="empty-state">${emptyMessage}</p>`;
        return;
    }
    const first = (page - 1) * dashboardPageSize;
    const controls = pageCount > 1 ? `<nav class="list-pagination" aria-label="Sider"><button type="button" data-dashboard-page="${key}" data-page-change="-1" ${page === 1 ? "disabled" : ""}>← Forrige</button><span>Side ${page} af ${pageCount}</span><button type="button" data-dashboard-page="${key}" data-page-change="1" ${page === pageCount ? "disabled" : ""}>Næste →</button></nav>` : "";
    container.innerHTML = entries.slice(first, first + dashboardPageSize).map(renderEntry).join("") + controls;
}

function renderUsers() {
    const query = document.querySelector("#userSearch")?.value.trim().toLowerCase() || "";
    const users = dashboard.users.filter(user => [user.firstName, user.lastName, user.email, user.role]
        .some(value => String(value ?? "").toLowerCase().includes(query)));
    renderPagedList(document.querySelector("#userList"), "users", users, user =>
        `<div class="admin-row"><span><b>${escapeHtml(user.firstName)} ${escapeHtml(user.lastName)}</b><small>${escapeHtml(user.email)} · ${escapeHtml(user.role)}</small></span></div>`,
    "Ingen brugere matcher din søgning.");
}

function renderBusinesses() {
    const query = document.querySelector("#businessSearch")?.value.trim().toLowerCase() || "";
    const businesses = dashboard.businesses.filter(business => [business.businessName, business.firstName, business.lastName, business.email]
        .some(value => String(value ?? "").toLowerCase().includes(query)));
    renderPagedList(document.querySelector("#businessList"), "businesses", businesses, business => {
        const address = [business.address, business.zipCode].filter(Boolean).join(", ");
        const contact = [business.email, business.phoneNumber, address].filter(Boolean).join(" · ");
        return `<div class="admin-row"><span><b>${escapeHtml(business.businessName)}</b><small>${escapeHtml(business.firstName)} ${escapeHtml(business.lastName)}</small><small>${escapeHtml(contact)}</small></span><button data-delete-barber="${business.id}">Fjern</button></div>`;
    }, "Ingen barbershops matcher din søgning.");
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        credentials: "same-origin",
        headers: { ...options.headers, "X-XSRF-TOKEN": decodeURIComponent(csrf() || "") }
    });
    if (!response.ok) throw new Error((await response.text()) || `Handlingen kunne ikke gennemføres (${response.status}).`);
    return response.status === 204 ? null : response.json();
}

function render() {
    document.querySelector("#userCount").textContent = dashboard.userCount;
    document.querySelector("#businessCount").textContent = dashboard.businessCount;
    document.querySelector("#subscriptionCount").textContent = dashboard.subscriptionCount;
    document.querySelector("#cancelledSubscriptionCount").textContent = dashboard.cancelledSubscriptionCount;
    document.querySelector("#orderCount").textContent = dashboard.orderCount;
    document.querySelector("#bookingCount").textContent = dashboard.bookingCount;
    document.querySelector("#cancellationCount").textContent = dashboard.cancellationCount;
    document.querySelector("#revenue").textContent = money(dashboard.totalRevenue);

    const seller = document.querySelector("#productSeller");
    seller.innerHTML = '<option value="">Vælg barbershop</option>' + dashboard.businesses.map(business => `<option value="${business.id}">${business.businessName}</option>`).join("");
    renderUsers();
    renderBusinesses();
    document.querySelector("#productList").innerHTML = dashboard.products.length
        ? dashboard.products.map(product => `<div class="admin-row product-admin-row"><div class="product-admin-row__details"><b>${product.name}</b><small>${product.sellerName} · ${money(product.price)}</small><label class="homepage-placement"><span>Vis på forsiden</span><select data-homepage-product="${product.id}"><option value="0" ${product.homepageSlider === 0 ? "selected" : ""}>Vis ikke</option><option value="1" ${product.homepageSlider === 1 ? "selected" : ""}>Slider 1</option><option value="2" ${product.homepageSlider === 2 ? "selected" : ""}>Slider 2</option></select></label></div><label class="product-image-upload"><span>Skift produktbillede</span><input type="file" accept="image/png,image/jpeg,image/webp,image/gif" data-product-image="${product.id}"></label><button data-delete-product="${product.id}">Fjern produkt</button></div>`).join("")
        : '<p class="empty-state">Ingen produkter er tilføjet fra dashboardet endnu.</p>';
}

async function load() {
    dashboard = await request("/api/admin/dashboard");
    render();
}

document.querySelector("#productForm").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        await request("/api/admin/products", { method: "POST", body: new FormData(event.target) });
        event.target.reset();
        load();
    } catch (error) {
        alert(error.message);
    }
});

document.querySelector("#businessForm").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        const data = Object.fromEntries(new FormData(event.target));
        ["phoneNumber", "zipCode"].forEach(field => {
            data[field] = data[field] ? Number(data[field]) : null;
        });
        await request("/api/admin/barbers", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });
        event.target.reset();
        load();
    } catch (error) {
        alert(error.message);
    }
});

document.querySelector("#resetCommerceData")?.addEventListener("click", async () => {
    if (!confirm("Vil du slette alle ordrer, bookinger og annulleringer?")) return;
    if (!confirm("Dette kan ikke fortrydes. Vil du fortsætte?")) return;
    try {
        await request("/api/admin/reset-commerce-data", { method: "POST" });
        await load();
        alert("Ordrer, bookinger og annulleringer er nulstillet.");
    } catch (error) {
        alert(error.message);
    }
});

document.addEventListener("click", async event => {
    const pageButton = event.target.closest("[data-dashboard-page]");
    if (pageButton && !pageButton.disabled) {
        const key = pageButton.dataset.dashboardPage;
        dashboardPages[key] += Number(pageButton.dataset.pageChange);
        key === "users" ? renderUsers() : renderBusinesses();
        return;
    }
    const product = event.target.dataset.deleteProduct;
    const barber = event.target.dataset.deleteBarber;
    if (!product && !barber) return;
    const confirmation = barber
        ? "Vil du fjerne denne barbershop? Dens produkter og ubrugte stole fjernes også."
        : "Vil du fjerne dette produkt?";
    if (!confirm(confirmation)) return;
    try {
        await request(product ? `/api/admin/products/${product}` : `/api/admin/barbers/${barber}`, { method: "DELETE" });
        load();
    } catch (error) {
        alert(error.message);
    }
});

["userSearch", "businessSearch"].forEach(id => document.querySelector(`#${id}`)?.addEventListener("input", event => {
    const key = id === "userSearch" ? "users" : "businesses";
    dashboardPages[key] = 1;
    key === "users" ? renderUsers() : renderBusinesses();
}));

document.addEventListener("change", async event => {
    const productId = event.target.dataset.homepageProduct;
    const imageProductId = event.target.dataset.productImage;
    if (imageProductId) {
        const image = event.target.files?.[0];
        if (!image) return;
        const data = new FormData();
        data.append("image", image);
        try {
            await request(`/api/admin/products/${imageProductId}/image`, { method: "POST", body: data });
            await load();
        } catch (error) {
            alert(error.message);
        }
        return;
    }
    if (!productId) return;
    try {
        await request(`/api/admin/products/${productId}/homepage?slider=${event.target.value}`, { method: "PUT" });
    } catch (error) {
        alert(error.message);
        load();
    }
});

load().catch(() => location.href = "/");
