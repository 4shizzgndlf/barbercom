const csrf = () => document.cookie.split("; ").find(value => value.startsWith("XSRF-TOKEN="))?.split("=").slice(1).join("=");
let dashboard;

const money = value => `${Number(value || 0).toFixed(2).replace(".", ",")} DKK`;

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
    document.querySelector("#orderCount").textContent = dashboard.orderCount;
    document.querySelector("#bookingCount").textContent = dashboard.bookingCount;
    document.querySelector("#cancellationCount").textContent = dashboard.cancellationCount;
    document.querySelector("#revenue").textContent = money(dashboard.totalRevenue);

    const seller = document.querySelector("#productSeller");
    seller.innerHTML = '<option value="">Vælg barbershop</option>' + dashboard.businesses.map(business => `<option value="${business.id}">${business.businessName}</option>`).join("");
    document.querySelector("#userList").innerHTML = dashboard.users.map(user => `<div class="admin-row"><span><b>${user.firstName} ${user.lastName}</b><small>${user.email} · ${user.role}</small></span></div>`).join("");
    document.querySelector("#businessList").innerHTML = dashboard.businesses.map(business => {
        const address = [business.address, business.zipCode].filter(Boolean).join(", ");
        const contact = [business.email, business.phoneNumber, address].filter(Boolean).join(" · ");
        return `<div class="admin-row"><span><b>${business.businessName}</b><small>${business.firstName} ${business.lastName}</small><small>${contact}</small></span><button data-delete-barber="${business.id}">Fjern</button></div>`;
    }).join("");
    document.querySelector("#productList").innerHTML = dashboard.products.length
        ? dashboard.products.map(product => `<div class="admin-row"><span><b>${product.name}</b><small>${product.sellerName} · ${money(product.price)}</small></span><label class="homepage-placement">Forside<select data-homepage-product="${product.id}"><option value="0" ${product.homepageSlider === 0 ? "selected" : ""}>Vis ikke</option><option value="1" ${product.homepageSlider === 1 ? "selected" : ""}>Slider 1</option><option value="2" ${product.homepageSlider === 2 ? "selected" : ""}>Slider 2</option></select></label><button data-delete-product="${product.id}">Fjern</button></div>`).join("")
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

document.addEventListener("click", async event => {
    const product = event.target.dataset.deleteProduct;
    const barber = event.target.dataset.deleteBarber;
    if (!product && !barber) return;
    if (!confirm("Vil du fjerne denne post?")) return;
    try {
        await request(product ? `/api/admin/products/${product}` : `/api/admin/barbers/${barber}`, { method: "DELETE" });
        load();
    } catch (error) {
        alert(error.message);
    }
});

document.addEventListener("change", async event => {
    const productId = event.target.dataset.homepageProduct;
    if (!productId) return;
    try {
        await request(`/api/admin/products/${productId}/homepage?slider=${event.target.value}`, { method: "PUT" });
    } catch (error) {
        alert(error.message);
        load();
    }
});

load().catch(() => location.href = "/");
