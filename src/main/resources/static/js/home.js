const slides = [...document.querySelectorAll(".hero-slide")];
const dots = [...document.querySelectorAll("[data-slide]")];
let activeSlide = 0;
function showSlide(index) { activeSlide = (index + slides.length) % slides.length; slides.forEach((slide, i) => slide.classList.toggle("is-active", i === activeSlide)); dots.forEach((dot, i) => dot.classList.toggle("is-active", i === activeSlide)); }
dots.forEach(dot => dot.addEventListener("click", () => showSlide(Number(dot.dataset.slide))));
setInterval(() => showSlide(activeSlide + 1), 6500);
