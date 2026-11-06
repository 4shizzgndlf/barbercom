const menuButton = document.querySelector("[data-menu-toggle]");
const searchButton = document.querySelector("[data-search-toggle]");
const closeButton = document.querySelector("[data-menu-close]");
const menu = document.querySelector("#sideMenu");
const backdrop = document.querySelector("#menuBackdrop");
const searchInput = document.querySelector("#menuSearch");
const brandName = "BookaBarber";
const cartOwnerKey = "barbercom-cart-owner";
const guestCartIdKey = "barbercom-guest-cart-id";
let activeCartKey = null;
let cartReady = false;
let resolveCartReady;

function applyBranding() {
    document.title = document.title.replaceAll("Barber.com", brandName);
    document.querySelectorAll("[aria-label], [alt], [placeholder]").forEach(element => {
        ["aria-label", "alt", "placeholder"].forEach(attribute => {
            const value = element.getAttribute(attribute);
            if (value) element.setAttribute(attribute, value.replaceAll("Barber.com", brandName));
        });
    });
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    let node;
    while ((node = walker.nextNode())) {
        const tagName = node.parentElement?.tagName;
        if (tagName !== "SCRIPT" && tagName !== "STYLE") {
            node.nodeValue = node.nodeValue.replaceAll("Barber.com", brandName);
        }
    }
    document.querySelectorAll(".brand .logo-image").forEach(logo => {
        logo.src = "/images/bookabarber-logo.png";
        logo.alt = brandName;
    });
}

applyBranding();

// A cart is scoped to the signed-in e-mail address, never shared through one
// browser-wide localStorage key. Guests receive a temporary session cart.
const cartReadyPromise = new Promise(resolve => { resolveCartReady = resolve; });
window.BarberCart = {
    get() {
        if (!cartReady || !activeCartKey) return [];
        try { return JSON.parse(localStorage.getItem(activeCartKey)) || []; }
        catch { return []; }
    },
    save(items) {
        if (!cartReady || !activeCartKey) return false;
        localStorage.setItem(activeCartKey, JSON.stringify(items));
        window.dispatchEvent(new Event("cart-updated"));
        return true;
    },
    clear() {
        if (activeCartKey) localStorage.removeItem(activeCartKey);
        window.dispatchEvent(new Event("cart-updated"));
    },
    ready: cartReadyPromise
};

function activateCart(user) {
    const email = user?.email?.trim().toLowerCase();
    if (email) {
        sessionStorage.setItem(cartOwnerKey, email);
        activeCartKey = `barbercom-cart:${email}`;
    } else {
        sessionStorage.removeItem(cartOwnerKey);
        let guestId = sessionStorage.getItem(guestCartIdKey);
        if (!guestId) {
            guestId = crypto.randomUUID();
            sessionStorage.setItem(guestCartIdKey, guestId);
        }
        activeCartKey = `barbercom-cart:guest:${guestId}`;
    }
    cartReady = true;
    resolveCartReady();
    window.dispatchEvent(new Event("cart-ready"));
    renderCart();
}

function renderAccountNotifications(count) {
    document.querySelectorAll("[data-account-notification]").forEach(badge => {
        badge.hidden = !count;
        badge.textContent = count > 99 ? "99+" : String(count);
    });
}

async function getCsrfToken() {
    const cookieToken = document.cookie.split("; ")
        .find(cookie => cookie.startsWith("XSRF-TOKEN="))
        ?.split("=").slice(1).join("=");
    if (cookieToken) return decodeURIComponent(cookieToken);

    const response = await fetch("/api/csrf", {
        credentials: "same-origin",
        cache: "no-store"
    });
    if (!response.ok) return null;
    return (await response.json()).token || null;
}

function loadAccountNotifications(user) {
    if (user?.role !== "BUSINESS") {
        document.querySelectorAll("[data-account-notification]").forEach(badge => badge.remove());
        return;
    }
    document.querySelectorAll('a[href="/account.html"]').forEach(link => {
        if (!link.querySelector("[data-account-notification]")) {
            link.insertAdjacentHTML("beforeend", '<span class="account-notification" data-account-notification hidden aria-live="polite"></span>');
        }
    });
    fetch("/api/account/notifications", { credentials: "same-origin" })
        .then(response => response.ok ? response.json() : { count: 0 })
        .then(data => renderAccountNotifications(Number(data.count) || 0))
        .catch(() => renderAccountNotifications(0));
}

// Remove the previous shared cart once. It could expose one account's items
// to the next account using this browser.
localStorage.removeItem("barbercom-cart");

const menuLinks = menu.querySelector(".menu-links");
if (menuLinks) {
    menuLinks.innerHTML = `
        <a href="/">Forside</a>
        <a href="/booking.html">Book tid</a>
        <a href="/shop.html">Shop produkter</a>
        <a href="/login.html">Log ind</a>
        <a href="/register.html">Opret konto</a>`;

    fetch("/api/account", { credentials: "same-origin" })
        .then(async response => {
            const contentType = response.headers.get("content-type") || "";
            return response.ok && contentType.includes("application/json") ? response.json() : null;
        })
        .then(user => {
            activateCart(user);
            loadAccountNotifications(user);
            if (user?.role === "ADMIN") {
                menuLinks.querySelector('a[href="/shop.html"]')?.insertAdjacentHTML("afterend", '<a href="/admin.html">Dashboard</a>');
            }
            if (user && !menuLinks.querySelector("[data-logout]")) {
                menuLinks.insertAdjacentHTML(
                    "beforeend",
                    '<button class="menu-logout" type="button" data-logout>Log ud</button>'
                );
            }
        })
        .catch(() => activateCart(null));
}

document.querySelectorAll(".side-menu__footer").forEach(footer => {
    footer.textContent = "Gå på opdagelse frit. Log ind, når du vil booke eller købe.";
});
document.querySelectorAll(".search-label").forEach(label => label.textContent = "Søg");
document.querySelectorAll(".contact-label").forEach(label => label.textContent = "Kontakt os");
document.querySelectorAll("#menuSearch").forEach(input => input.placeholder = "Søg på BookaBarber");
document.querySelectorAll("#contactPanel .panel-top h2").forEach(title => title.textContent = "Kontakt os");
document.querySelectorAll("#cartPanel .panel-top h2").forEach(title => title.textContent = "Din kurv");
document.querySelectorAll("#contactPanel .contact-details").forEach(details => {
    details.innerHTML = `<div><dt>Ejer</dt><dd>Lucas Hoffmann Christensen</dd></div>
        <div><dt>E-mail</dt><dd><a href="mailto:loegcas@gmail.com">loegcas@gmail.com</a></dd></div>
        <div><dt>Telefon</dt><dd><a href="tel:+4561128112">+45 61128112</a></dd></div>
        <div><dt>Besøg os</dt><dd>Vesterbrogade 181<br>1800 Frederiksberg</dd></div>`;
});

document.querySelectorAll(".side-menu__title").forEach(logo => {
    logo.innerHTML = '<a class="menu-logo-link" href="/" aria-label="BookaBarber forside"><img src="/images/bookabarber-logo.png" alt="BookaBarber"></a>';
});
const contactButton = document.querySelector("[data-contact-toggle]");
const cartButton = document.querySelector("[data-cart-toggle]");
const rightBackdrop = document.querySelector("#rightBackdrop");
const rightPanels = document.querySelectorAll(".right-panel");

function closeRightPanels() {
    rightPanels.forEach(panel => panel.classList.remove("is-open"));
    rightBackdrop.classList.remove("is-open");
}

function openRightPanel(id) {
    closeMenu();
    closeRightPanels();
    document.querySelector(id).classList.add("is-open");
    rightBackdrop.classList.add("is-open");
}

function closeMenu() {
    menu.classList.remove("is-open");
    backdrop.classList.remove("is-open");
    document.body.classList.remove("menu-open");
    menuButton.setAttribute("aria-expanded", "false");
    menu.setAttribute("aria-hidden", "true");
}

function openMenu(focusSearch = false) {
    menu.classList.add("is-open");
    backdrop.classList.add("is-open");
    document.body.classList.add("menu-open");
    menuButton.setAttribute("aria-expanded", "true");
    menu.setAttribute("aria-hidden", "false");

    if (focusSearch) searchInput.focus();
}

menuButton.addEventListener("click", () => {
    menu.classList.contains("is-open") ? closeMenu() : openMenu();
});

searchButton.addEventListener("click", () => openMenu(true));
searchInput.addEventListener("keydown", event => {
    if (event.key !== "Enter") return;
    const query = searchInput.value.trim();
    window.location.href = `/shop.html${query ? `?search=${encodeURIComponent(query)}` : ""}`;
});
closeButton.addEventListener("click", closeMenu);
backdrop.addEventListener("click", closeMenu);

document.addEventListener("keydown", event => {
    if (event.key === "Escape" && menu.classList.contains("is-open")) {
        closeMenu();
    }
    if (event.key === "Escape") closeRightPanels();
});

document.querySelectorAll("[data-right-close]").forEach(button =>
    button.addEventListener("click", closeRightPanels));
rightBackdrop.addEventListener("click", closeRightPanels);
contactButton.addEventListener("click", () => openRightPanel("#contactPanel"));
cartButton.addEventListener("click", () => {
    renderCart();
    openRightPanel("#cartPanel");
});

const cartItems = document.querySelector("#cartItems");
const cartCount = document.querySelector("#cartCount");
const cartTotal = document.querySelector("#cartTotal");

if (cartTotal && !document.querySelector("#cartCheckout")) {
    cartTotal.parentElement.insertAdjacentHTML(
        "afterend",
        '<a class="cart-checkout" id="cartCheckout" href="/checkout.html">Gå til betaling</a>'
    );
}
const cartCheckout = document.querySelector("#cartCheckout");

function getCart() {
    return window.BarberCart.get();
}

function renderCart() {
    const items = getCart();
    const count = items.reduce((sum, item) => sum + (item.quantity || 1), 0);
    const total = items.reduce((sum, item) => sum + Number(item.price || 0) * (item.quantity || 1), 0);
    cartCount.textContent = count;
    cartTotal.textContent = `${total.toFixed(2)} DKK`;
    cartCheckout.hidden = !items.length;
    cartItems.innerHTML = items.length ? items.map((item, index) =>
        `<li><span>${item.name}</span><button type="button" data-remove-item="${index}">Fjern</button></li>`).join("") :
        "<li class=\"cart-empty\">Din kurv er tom.</li>";
}

cartItems.addEventListener("click", event => {
    const button = event.target.closest("[data-remove-item]");
    if (!button) return;
    const items = getCart();
    items.splice(Number(button.dataset.removeItem), 1);
    window.BarberCart.save(items);
});

renderCart();
window.addEventListener("cart-updated", renderCart);
window.addEventListener("cart-ready", renderCart);

document.addEventListener("click", async event => {
    const button = event.target.closest("[data-logout]");
    if (!button) return;
    try {
        const token = await getCsrfToken();
        if (!token) throw new Error("missing-token");
        const response = await fetch("/logout", {
            method: "POST",
            headers: { "X-XSRF-TOKEN": token },
            credentials: "same-origin"
        });
        if (!response.ok) throw new Error("logout-failed");

        sessionStorage.removeItem(cartOwnerKey);
        sessionStorage.removeItem(guestCartIdKey);
        activeCartKey = null;
        cartReady = false;
        window.location.href = "/login.html?logout";
    } catch {
        alert("Kunne ikke logge ud. Prøv igen.");
    }
});
