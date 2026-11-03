const checkoutItems = document.querySelector("#checkoutItems");
const checkoutTotal = document.querySelector("#checkoutTotal");
const purchaseButton = document.querySelector("#purchaseButton");
const checkoutMessage = document.querySelector("#checkoutMessage");
function getCart() {
    return window.BarberCart.get();
}

function csrfToken() {
    return document.cookie.split("; ").find(value => value.startsWith("XSRF-TOKEN="))?.split("=").slice(1).join("=");
}

function renderCheckout() {
    const cart = getCart();
    const total = cart.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 1), 0);
    checkoutItems.innerHTML = cart.length ? cart.map(item => `<div><span>${item.quantity} × ${item.name}</span><strong>${(item.price * item.quantity).toFixed(2)} DKK</strong></div>`).join("") : '<p class="empty-state">Din kurv er tom. <a href="/shop.html">Gå til shoppen</a></p>';
    checkoutTotal.textContent = `${total.toFixed(2).replace(".", ",")} DKK`;
    purchaseButton.disabled = !cart.length;
}

purchaseButton.addEventListener("click", async () => {
    const items = getCart();
    const token = csrfToken();
    if (!items.length || !token) return;
    purchaseButton.disabled = true;
    checkoutMessage.textContent = "Gennemfører dit køb…";
    try {
        const response = await fetch("/api/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": decodeURIComponent(token) },
            credentials: "same-origin",
            body: JSON.stringify({ items })
        });
        if (!response.ok) throw new Error(await response.text());
        window.BarberCart.clear();
        checkoutMessage.textContent = "Tak for dit køb. Din ordre ligger nu under Ordrer på din konto.";
        purchaseButton.hidden = true;
    } catch (error) {
        checkoutMessage.textContent = error.message || "Kunne ikke gennemføre købet. Prøv igen.";
        purchaseButton.disabled = false;
    }
});

renderCheckout();
window.addEventListener("cart-ready", renderCheckout);
window.addEventListener("cart-updated", renderCheckout);
