const productId = new URLSearchParams(window.location.search).get("id");
const sellerId = Number(new URLSearchParams(window.location.search).get("seller")) || null;
const allProducts = Object.values(window.barberProducts).flat();
const product = allProducts.find(([id]) => id === productId);
const detail = document.querySelector("#productDetail");

if (!product) {
    detail.innerHTML = '<div class="product-not-found"><p class="eyebrow">Produktet findes ikke</p><h1>Vi kunne ikke finde produktet.</h1><a class="primary-button" href="/#shop">Se shoppen</a></div>';
} else {
    const [, name, description, price, type] = product;
    document.title = `${name} | Barber.com`;
    detail.innerHTML = `<div class="product-detail__art product-art ${type}"><img src="${window.barberProductImages[type]}" alt="${name}"></div><div class="product-detail__content"><p class="eyebrow">Barber.com selection</p><h1>${name}</h1><p class="product-detail__description">${description}. Et kvalitetsprodukt udvalgt til en enkel og skarp rutine hos barberen eller derhjemme.</p><p class="product-detail__price">${price} DKK</p><div class="quantity-picker" aria-label="Vælg antal"><button type="button" id="decreaseQuantity" aria-label="Fjern én">−</button><output id="quantity">1</output><button type="button" id="increaseQuantity" aria-label="Tilføj én">+</button></div><button class="primary-button product-add" id="addToCart" type="button">Læg i kurv</button><p class="product-detail__note">Fri fragt ved køb over 499 DKK · Levering 1–3 hverdage</p></div>`;

    let quantity = 1;
    const quantityOutput = document.querySelector("#quantity");
    document.querySelector("#decreaseQuantity").onclick = () => { quantity = Math.max(1, quantity - 1); quantityOutput.textContent = quantity; };
    document.querySelector("#increaseQuantity").onclick = () => { quantity++; quantityOutput.textContent = quantity; };
    document.querySelector("#addToCart").onclick = event => {
        const cart = window.BarberCart.get();
        const existing = cart.find(item => item.name === name && item.sellerId === sellerId);
        existing ? existing.quantity += quantity : cart.push({name, price, quantity, sellerId});
        if (!window.BarberCart.save(cart)) return;
        event.currentTarget.textContent = "Tilføjet til kurven";
    };
}
