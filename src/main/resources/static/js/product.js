const productId = new URLSearchParams(window.location.search).get("id");
const sellerId = Number(new URLSearchParams(window.location.search).get("seller")) || null;
const detail = document.querySelector("#productDetail");

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[character]));
}

async function loadProduct() {
    if (!productId?.startsWith("custom-")) return null;
    try {
        const response = await fetch("/api/catalog/products");
        if (!response.ok) return null;
        const products = await response.json();
        const product = products.find(item => `custom-${item.id}` === productId);
        if (!product) return null;
        return {
            name: product.name,
            description: product.description,
            price: Number(product.price),
            type: product.type,
            imageUrl: product.imageUrl || window.barberProductImages[product.type],
            sellerName: product.sellerName || "Din lokale barber",
            sellerId: product.sellerId
        };
    } catch {
        return null;
    }
}

function renderProduct(product) {
    if (!product) {
        detail.innerHTML = '<div class="product-not-found"><p class="eyebrow">Produktet findes ikke</p><h1>Vi kunne ikke finde produktet.</h1><a class="primary-button" href="/shop.html">Se shoppen</a></div>';
        return;
    }

    const name = escapeHtml(product.name);
    const shortDescription = escapeHtml(product.description);
    const description = `${shortDescription} Produktet er udvalgt af ${escapeHtml(product.sellerName)} og er lavet til en enkel, skarp rutine — uanset om du styler håret hjemme eller får råd hos din barber.`;
    const price = Number(product.price);
    const imageUrl = escapeHtml(product.imageUrl);
    const effectiveSellerId = product.sellerId || sellerId;
    document.title = `${product.name} | BookaBarber`;
    detail.innerHTML = `<div class="product-detail__art product-art ${escapeHtml(product.type)}"><img src="${imageUrl}" alt="${name}"></div><div class="product-detail__content"><p class="eyebrow">Køb hos ${escapeHtml(product.sellerName)}</p><h1>${name}</h1><p class="product-detail__description">${description}</p><p class="product-detail__price">${price.toFixed(2).replace(".", ",")} DKK</p><div class="quantity-picker" aria-label="Vælg antal"><button type="button" id="decreaseQuantity" aria-label="Fjern én">−</button><output id="quantity">1</output><button type="button" id="increaseQuantity" aria-label="Tilføj én">+</button></div><button class="primary-button product-add" id="addToCart" type="button">Læg i kurv</button><p class="product-detail__note">Fri fragt ved køb over 499 DKK · Levering 1–3 hverdage</p></div>`;

    let quantity = 1;
    const quantityOutput = document.querySelector("#quantity");
    document.querySelector("#decreaseQuantity").onclick = () => {
        quantity = Math.max(1, quantity - 1);
        quantityOutput.textContent = quantity;
    };
    document.querySelector("#increaseQuantity").onclick = () => {
        quantity++;
        quantityOutput.textContent = quantity;
    };
    document.querySelector("#addToCart").onclick = event => {
        const cart = window.BarberCart.get();
        const existing = cart.find(item => item.name === product.name && item.sellerId === effectiveSellerId);
        existing ? existing.quantity += quantity : cart.push({ name: product.name, price, quantity, sellerId: effectiveSellerId });
        if (!window.BarberCart.save(cart)) return;
        event.currentTarget.textContent = "Tilføjet til kurven";
    };
}

async function initializeProductPage() {
    renderProduct(await loadProduct());
}

initializeProductPage();
