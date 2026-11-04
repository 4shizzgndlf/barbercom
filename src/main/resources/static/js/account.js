const profileForm = document.querySelector("#profileForm");
const profileMessage = document.querySelector("#profileMessage");
const appointmentsList = document.querySelector("#appointmentsList");
const businessDayNav = document.querySelector("#businessDayNav");
const businessDayLabel = document.querySelector("#businessDayLabel");
let currentUser;
let selectedBusinessDate = new Date();
const paymentSuccess = new URLSearchParams(window.location.search).get("payment") === "success";

function csrfToken() {
    return document.cookie.split("; ").find(value => value.startsWith("XSRF-TOKEN="))?.split("=").slice(1).join("=");
}

function isoDate(date) {
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

function formatDate(date) {
    return date.toLocaleDateString("da-DK", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[character]));
}

async function loadProfile() {
    const response = await fetch("/api/account", { credentials: "same-origin" });
    if (!response.ok) return null;
    const user = await response.json();
    ["firstName", "lastName", "email", "phoneNumber", "address", "zipCode", "businessName"].forEach(field => {
        profileForm.elements[field].value = user[field] ?? "";
    });
    document.querySelector("#businessNameProfileField").hidden = user.role !== "BUSINESS";
    document.querySelector("#accountName").textContent = `${user.firstName} ${user.lastName}`.trim() || "Min profil";
    return user;
}

async function loadCustomerAppointments() {
    try {
        const response = await fetch("/api/bookings/mine", { credentials: "same-origin" });
        if (!response.ok) throw new Error();
        const bookings = await response.json();
        if (!bookings.length) {
            appointmentsList.innerHTML = '<p class="empty-state">Du har ingen bookinger endnu.</p>';
            return;
        }
        appointmentsList.innerHTML = bookings.map(booking => {
            const date = formatDate(new Date(`${booking.date}T12:00:00`));
            return `<article class="appointment-item"><div><p class="eyebrow">${date}</p><h4>${escapeHtml(booking.barberName)}</h4></div><div class="appointment-item__actions"><time>${booking.startTime.slice(0, 5)}</time><button type="button" data-cancel-booking="${booking.id}">Annullér</button></div></article>`;
        }).join("");
    } catch {
        appointmentsList.innerHTML = '<p class="empty-state">Kunne ikke hente dine bookinger.</p>';
    }
}

async function loadBusinessAppointments() {
    businessDayLabel.textContent = formatDate(selectedBusinessDate);
    try {
        const response = await fetch(`/api/bookings/business?date=${isoDate(selectedBusinessDate)}`, { credentials: "same-origin" });
        if (!response.ok) throw new Error();
        const bookings = await response.json();
        if (!bookings.length) {
            appointmentsList.innerHTML = '<p class="empty-state">Der er ingen bookinger denne dag.</p>';
            return;
        }
        appointmentsList.innerHTML = bookings.map(booking => `
            <article class="appointment-item appointment-item--business">
                <time>${booking.startTime.slice(0, 5)}</time>
                <div><h4>${escapeHtml(booking.customerName)}</h4><p>${escapeHtml(booking.customerEmail)}</p></div>
            </article>`).join("");
    } catch {
        appointmentsList.innerHTML = '<p class="empty-state">Kunne ikke hente dagens bookinger.</p>';
    }
}

async function loadOrders() {
    const list = document.querySelector("#ordersList");
    if (!list) return;
    try {
        const response = await fetch("/api/orders/mine", { credentials: "same-origin" });
        if (!response.ok) throw new Error();
        const orders = await response.json();
        if (!orders.length) {
            list.innerHTML = '<p class="empty-state">Du har ikke gennemført et køb endnu.</p>';
            return;
        }
        list.innerHTML = orders.map(order => {
            const date = new Date(order.purchasedAt).toLocaleDateString("da-DK", { day: "numeric", month: "long", year: "numeric" });
            const products = order.items.map(item => `${item.quantity} × ${escapeHtml(item.name)}<span class="order-seller">Købt hos ${escapeHtml(item.sellerName || "Barber.com")}</span>`).join("");
            return `<article class="order-item"><div><h4>Ordre #${order.id}</h4><p>${date}</p><p>${products}</p></div><strong>${Number(order.totalAmount).toFixed(2).replace(".", ",")} DKK</strong></article>`;
        }).join("");
    } catch {
        list.innerHTML = '<p class="empty-state">Kunne ikke hente dine ordrer.</p>';
    }
}

async function loadBusinessSales() {
    const card = document.querySelector("#businessSalesCard");
    const list = document.querySelector("#businessSalesList");
    if (!card || !list) return;
    card.hidden = false;
    try {
        const response = await fetch("/api/orders/sales", { credentials: "same-origin" });
        if (!response.ok) throw new Error();
        const sales = await response.json();
        if (!sales.length) {
            list.innerHTML = '<p class="empty-state">Du har ikke solgt produkter endnu.</p>';
            return;
        }
        list.innerHTML = sales.map(sale => {
            const date = new Date(sale.purchasedAt).toLocaleDateString("da-DK", { day: "numeric", month: "long", year: "numeric" });
            const address = sale.customerAddress ? escapeHtml(sale.customerAddress) : "Adresse ikke angivet";
            const zipCode = sale.customerZipCode ? `, ${escapeHtml(String(sale.customerZipCode))}` : "";
            const email = sale.customerEmail ? escapeHtml(sale.customerEmail) : "E-mail ikke angivet";
            const phone = sale.customerPhoneNumber ? `+45 ${escapeHtml(String(sale.customerPhoneNumber))}` : "Telefon ikke angivet";
            const total = Number(sale.unitPrice) * Number(sale.quantity);
            return `<article class="sale-item"><div><h4>${escapeHtml(sale.productName)} · ${sale.quantity} stk.</h4><p>${date}</p><p><strong>Køber:</strong> ${escapeHtml(sale.customerName)}</p><p><strong>E-mail:</strong> ${email}</p><p><strong>Telefon:</strong> ${phone}</p><p><strong>Adresse:</strong> ${address}${zipCode}</p></div><strong>${total.toFixed(2).replace(".", ",")} DKK</strong></article>`;
        }).join("");
    } catch {
        list.innerHTML = '<p class="empty-state">Kunne ikke hente dit salg.</p>';
    }
}

appointmentsList?.addEventListener("click", async event => {
    const button = event.target.closest("[data-cancel-booking]");
    if (!button || !confirm("Vil du annullere denne booking?")) return;
    const token = csrfToken();
    if (!token) return;
    const response = await fetch(`/api/bookings/${button.dataset.cancelBooking}`, {
        method: "DELETE",
        headers: { "X-XSRF-TOKEN": decodeURIComponent(token) },
        credentials: "same-origin"
    });
    if (response.ok) loadCustomerAppointments();
    else alert("Kunne ikke annullere bookingen. Prøv igen.");
});

businessDayNav?.addEventListener("click", event => {
    const button = event.target.closest("[data-day-change]");
    if (!button) return;
    selectedBusinessDate.setDate(selectedBusinessDate.getDate() + Number(button.dataset.dayChange));
    loadBusinessAppointments();
});

profileForm.addEventListener("submit", async event => {
    event.preventDefault();
    const token = csrfToken();
    if (!token) { profileMessage.textContent = "Opdatér siden, og prøv igen."; return; }
    const data = Object.fromEntries(new FormData(profileForm));
    ["phoneNumber", "zipCode"].forEach(field => data[field] = data[field] ? Number(data[field]) : null);
    try {
        const response = await fetch("/api/account", { method: "PUT", headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": decodeURIComponent(token) }, credentials: "same-origin", body: JSON.stringify(data) });
        if (response.ok) { window.location.reload(); return; }
        profileMessage.textContent = "Kunne ikke gemme dine oplysninger.";
    } catch { profileMessage.textContent = "Kunne ikke oprette forbindelse til serveren."; }
});

async function initializeAccount() {
    currentUser = await loadProfile();
    if (!currentUser) return;
    if (paymentSuccess) {
        await window.BarberCart?.ready;
        window.BarberCart?.clear();
        const message = document.querySelector("#paymentSuccessMessage");
        message.hidden = false;
        message.textContent = "Tak for din handel! Din betaling er gennemført, og ordren vises nedenfor.";
        window.history.replaceState({}, "", "/account.html");
    }
    if (currentUser.role === "BUSINESS") {
        document.querySelector("#appointmentsTitle").textContent = "Virksomhedens bookinger";
        document.querySelector("#bookAppointmentLink").hidden = true;
        document.querySelector("#orders").hidden = true;
        businessDayNav.hidden = false;
        await loadBusinessAppointments();
        await loadBusinessSales();
    } else {
        await loadCustomerAppointments();
    }
    await loadOrders();
    if (paymentSuccess) window.setTimeout(loadOrders, 1500);
}

initializeAccount();
