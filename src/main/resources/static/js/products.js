function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[character]));
}

function createSlider(products, gridId, pageId, previousId, nextId) {
    const grid = document.querySelector(gridId);
    const label = document.querySelector(pageId);
    const previous = document.querySelector(previousId);
    const next = document.querySelector(nextId);
    let page = 0;

    function render() {
        const pageCount = Math.ceil(products.length / 5);
        if (!pageCount) {
            grid.innerHTML = '<p class="empty-state">Ingen produkter er tilføjet endnu.</p>';
            label.textContent = "0 / 0";
            previous.disabled = next.disabled = true;
            return;
        }
        page = Math.min(page, pageCount - 1);
        const pageProducts = products.slice(page * 5, page * 5 + 5);
        grid.innerHTML = pageProducts.map(product =>
            `<article class="product-card" data-product-link="/product.html?id=${product.id}&seller=${product.barberId}" tabindex="0" role="link">
                <div class="product-art ${escapeHtml(product.type)}"><img src="${escapeHtml(product.imageUrl || window.barberProductImages[product.type])}" alt="${escapeHtml(product.name)}"></div>
                <div class="shop-product-card__meta"><span>${escapeHtml(product.barber)}</span><span>${escapeHtml(product.category)}</span></div>
                <h3>${escapeHtml(product.name)}</h3><b>${Number(product.price).toFixed(2).replace(".", ",")} DKK</b>
                <p>${escapeHtml(product.description)}</p>
                <button data-product="${product.id}">Læg i kurv</button>
            </article>`
        ).join("");
        label.textContent = `${page + 1} / ${pageCount}`;
        previous.disabled = next.disabled = false;
    }

    previous.onclick = next.onclick = () => {
        if (!products.length) return;
        page = page ? 0 : Math.ceil(products.length / 5) - 1;
        render();
    };
    grid.onclick = event => {
        const button = event.target.closest("[data-product]");
        if (button) {
            const product = products.find(item => item.id === button.dataset.product);
            const cart = window.BarberCart.get();
            const existing = cart.find(item => item.name === product.name && item.sellerId === product.barberId);
            existing ? existing.quantity++ : cart.push({ name: product.name, price: product.price, quantity: 1, sellerId: product.barberId });
            if (!window.BarberCart.save(cart)) return;
            button.textContent = "Tilføjet";
            return;
        }
        const card = event.target.closest("[data-product-link]");
        if (card) window.location.href = card.dataset.productLink;
    };
    grid.addEventListener("keydown", event => {
        if (event.key === "Enter" && event.target.matches("[data-product-link]")) window.location.href = event.target.dataset.productLink;
    });
    render();
}

async function loadHomeProducts() {
    try {
        const response = await fetch("/api/catalog/products");
        if (!response.ok) throw new Error();
        const products = (await response.json()).map(product => ({
            id: `custom-${product.id}`,
            name: product.name,
            description: product.description,
            price: Number(product.price),
            type: product.type,
            imageUrl: product.imageUrl,
            category: product.category,
            barber: product.sellerName || "Din lokale barber",
            barberId: product.sellerId,
            homepageSlider: product.homepageSlider
        }));
        const hasHomepageSelection = products.some(product => product.homepageSlider === 1 || product.homepageSlider === 2);
        const firstSlider = hasHomepageSelection ? products.filter(product => product.homepageSlider === 1) : products.slice(0, 10);
        const secondSlider = hasHomepageSelection ? products.filter(product => product.homepageSlider === 2) : products.slice(10);
        createSlider(firstSlider, "#productGrid", "#productPage", "#productsPrevious", "#productsNext");
        createSlider(secondSlider, "#secondaryProductGrid", "#secondaryProductPage", "#secondaryProductsPrevious", "#secondaryProductsNext");
    } catch {
        ["#productGrid", "#secondaryProductGrid"].forEach(id => {
            document.querySelector(id).innerHTML = '<p class="empty-state">Produkterne kunne ikke indlæses.</p>';
        });
    }
}

loadHomeProducts();
