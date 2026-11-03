const shopGrid = document.querySelector("#shopProductGrid");
const shopCount = document.querySelector("#shopCount");
const shopSearch = document.querySelector("#shopSearch");
const shopCategory = document.querySelector("#shopCategory");
const shopBarber = document.querySelector("#shopBarber");
const shopSort = document.querySelector("#shopSort");
const clearShopFilters = document.querySelector("#clearShopFilters");
const shopProducts = Object.values(window.barberProducts).flat().map(([id, name, description, price, type]) => ({
    id, name, description, price, type,
    category: window.barberProductMeta[id][0],
    barber: "",
    barberId: null
}));

shopSearch.value = new URLSearchParams(window.location.search).get("search") || "";

function populateSelect(select, values) {
    values.forEach(value => select.insertAdjacentHTML("beforeend", `<option value="${value}">${value}</option>`));
}

populateSelect(shopCategory, [...new Set(shopProducts.map(product => product.category))]);

async function loadShopBarbers() {
    const response = await fetch("/api/barbers");
    if (!response.ok) return;
    const barbers = await response.json();
    barbers.forEach((barber, index) => {
        shopProducts.forEach((product, productIndex) => {
            if (!product.barberId && productIndex % barbers.length === index) {
                product.barber = barber.displayName;
                product.barberId = barber.id;
            }
        });
        shopBarber.insertAdjacentHTML("beforeend", `<option value="${barber.id}">${barber.displayName}</option>`);
    });
    renderShop();
}

async function loadAdminProducts() {
    try {
        const response = await fetch("/api/catalog/products");
        if (!response.ok) return;
        const products = await response.json();
        products.forEach(product => shopProducts.push({
            id: `custom-${product.id}`, name: product.name, description: product.description,
            price: Number(product.price), type: product.type, imageUrl: product.imageUrl, category: product.category,
            barber: product.sellerName, barberId: product.sellerId
        }));
        renderShop();
    } catch { /* Static catalogue remains available. */ }
}

function renderShop() {
    const term = shopSearch.value.trim().toLowerCase();
    const filtered = shopProducts.filter(product =>
        (!term || `${product.name} ${product.description}`.toLowerCase().includes(term)) &&
        (!shopCategory.value || product.category === shopCategory.value) &&
        (!shopBarber.value || product.barberId === Number(shopBarber.value))
    );
    if (shopSort.value === "price-low") filtered.sort((a, b) => a.price - b.price);
    if (shopSort.value === "price-high") filtered.sort((a, b) => b.price - a.price);
    if (shopSort.value === "name") filtered.sort((a, b) => a.name.localeCompare(b.name, "da"));

    shopCount.textContent = `${filtered.length} ${filtered.length === 1 ? "produkt" : "produkter"}`;
    shopGrid.innerHTML = filtered.length ? filtered.map(product => `<article class="shop-product-card" data-product-link="/product.html?id=${product.id}&seller=${product.barberId || ""}" tabindex="0" role="link"><div class="product-art ${product.type}"><img src="${product.imageUrl || window.barberProductImages[product.type]}" alt="${product.name}"></div><div class="shop-product-card__meta"><span>${product.barber || "Din lokale barber"}</span><span>${product.category}</span></div><h2>${product.name}</h2><p>${product.description}</p><div class="shop-product-card__bottom"><b>${product.price} DKK</b><button type="button" data-add-product="${product.id}">Læg i kurv</button></div></article>`).join("") : '<p class="shop-empty">Ingen produkter matcher dine filtre. Prøv at rydde dem.</p>';
}

shopSearch.addEventListener("input", renderShop);
[shopCategory, shopBarber, shopSort].forEach(control => control.addEventListener("change", renderShop));
clearShopFilters.addEventListener("click", () => { shopSearch.value = ""; shopCategory.value = ""; shopBarber.value = ""; shopSort.value = "featured"; renderShop(); });
shopGrid.addEventListener("click", event => {
    const button = event.target.closest("[data-add-product]");
    if (button) {
        const product = shopProducts.find(item => item.id === button.dataset.addProduct);
        const cart = window.BarberCart.get();
        const existing = cart.find(item => item.name === product.name && item.sellerId === product.barberId);
        existing ? existing.quantity++ : cart.push({name: product.name, price: product.price, quantity: 1, sellerId: product.barberId});
        if (!window.BarberCart.save(cart)) return;
        button.textContent = "Tilføjet";
        return;
    }
    const card = event.target.closest("[data-product-link]");
    if (card) window.location.href = card.dataset.productLink;
});
shopGrid.addEventListener("keydown", event => {
    if (event.key === "Enter" && event.target.matches("[data-product-link]")) window.location.href = event.target.dataset.productLink;
});
renderShop();
loadShopBarbers();
loadAdminProducts();
