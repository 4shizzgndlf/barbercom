const homeProducts = Object.values(window.barberProducts).flat().map(([id, name, description, price, type]) => ({
    id, name, description, price, type,
    category: window.barberProductMeta[id]?.[0] || "Produkt",
    barber: "Din lokale barber", barberId: null
}));

function createSlider(products, gridId, pageId, previousId, nextId) {
    const grid = document.querySelector(gridId);
    const label = document.querySelector(pageId);
    let page = 0;

    function render() {
        const pageProducts = products.slice(page * 5, page * 5 + 5);
        grid.innerHTML = pageProducts.map(product =>
            `<article class="product-card" data-product-link="/product.html?id=${product.id}&seller=${product.barberId || ""}" tabindex="0" role="link">
                <div class="product-art ${product.type}"><img src="${window.barberProductImages[product.type]}" alt="${product.name}"></div>
                <div class="shop-product-card__meta"><span>${product.barber}</span><span>${product.category}</span></div>
                <h3>${product.name}</h3><b>${product.price} DKK</b>
                <p>${product.description}</p>
                <button data-product="${product.id}">Læg i kurv</button>
            </article>`
        ).join("");
        label.textContent = `${page + 1} / ${Math.ceil(products.length / 5)}`;
    }

    document.querySelector(previousId).onclick = document.querySelector(nextId).onclick = () => {
        page = page ? 0 : Math.ceil(products.length / 5) - 1;
        render();
    };
    grid.onclick = event => {
        const button = event.target.closest("[data-product]");
        if (!button) return;
        const product = products.find(item => item.id === button.dataset.product);
        const cart = window.BarberCart.get();
        const existing = cart.find(item => item.name === product.name && item.sellerId === product.barberId);
        existing ? existing.quantity++ : cart.push({ name: product.name, price: product.price, quantity: 1, sellerId: product.barberId });
        if (!window.BarberCart.save(cart)) return;
        button.textContent = "Tilføjet";
    };
    grid.addEventListener("click", event => {
        if (event.target.closest("button")) return;
        const card = event.target.closest("[data-product-link]");
        if (card) window.location.href = card.dataset.productLink;
    });
    grid.addEventListener("keydown", event => {
        if (event.key === "Enter" && event.target.matches("[data-product-link]")) window.location.href = event.target.dataset.productLink;
    });
    render();
    return render;
}

const renderEssentials = createSlider(homeProducts.slice(0, 10), "#productGrid", "#productPage", "#productsPrevious", "#productsNext");
const renderSelection = createSlider(homeProducts.slice(10), "#secondaryProductGrid", "#secondaryProductPage", "#secondaryProductsPrevious", "#secondaryProductsNext");

async function loadHomeBarbers() {
    try {
        const response = await fetch("/api/barbers");
        if (!response.ok) return;
        const barbers = await response.json();
        if (!barbers.length) return;
        homeProducts.forEach((product, index) => {
            const barber = barbers[index % barbers.length];
            product.barber = barber.displayName;
            product.barberId = barber.id;
        });
        renderEssentials();
        renderSelection();
    } catch {
        // The product sliders still work with the neutral fallback label.
    }
}

loadHomeBarbers();
