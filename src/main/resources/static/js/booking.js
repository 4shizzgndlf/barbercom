const form = document.querySelector("#bookingForm");
const barberSelect = document.querySelector("#bookingBarber");
const dateInput = document.querySelector("#bookingDate");
const chairSelect = document.querySelector("#bookingChair");
const timeInput = document.querySelector("#bookingTime");
const durationInput = document.querySelector("#bookingDuration");
const timeSlots = document.querySelector("#timeSlots");
const bookButton = document.querySelector("#bookButton");
const message = document.querySelector("#bookingMessage");
const csrf = () => document.cookie.split("; ").find(value => value.startsWith("XSRF-TOKEN="))?.split("=").slice(1).join("=");

function time(value) { return String(value).slice(0, 5); }

async function loadCustomer() {
    const response = await fetch("/api/account");
    if (!response.ok) return;
    const user = await response.json();
    document.querySelector("#customerName").textContent = `${user.firstName || ""} ${user.lastName || ""}`.trim();
    document.querySelector("#customerEmail").textContent = user.email || "";
    document.querySelector("#customerPhone").textContent = user.phoneNumber ? `+45 ${user.phoneNumber}` : "Tilføj telefonnummer på din konto";
}

async function loadBarbers() {
    try {
        const response = await fetch("/api/barbers");
        if (!response.ok) throw new Error();
        const barbers = await response.json();
        barbers.forEach(barber => barberSelect.insertAdjacentHTML("beforeend", `<option value="${barber.id}">${barber.displayName}</option>`));
    } catch {
        message.textContent = "Kunne ikke hente barbershops.";
    }
}

function renderAvailability(chairs) {
    chairSelect.disabled = false;
    chairSelect.innerHTML = '<option value="">Vælg stol</option>' + chairs
        .map(chair => `<option value="${chair.chairId}">${chair.chairName}</option>`).join("");
    timeSlots.innerHTML = chairs.map(chair => {
        const intervals = chair.bookings.length
            ? chair.bookings.map(booking => `<li>${time(booking.startTime)}–${time(booking.endTime)}</li>`).join("")
            : "<li>Ingen reserverede tider</li>";
        return `<section class="chair-slots"><h3>${chair.chairName}</h3><ul class="booked-intervals">${intervals}</ul></section>`;
    }).join("");
}

async function loadAvailability() {
    chairSelect.value = "";
    chairSelect.disabled = true;
    bookButton.disabled = true;
    if (!barberSelect.value || !dateInput.value) {
        chairSelect.innerHTML = '<option value="">Vælg først barbershop og dato</option>';
        timeSlots.innerHTML = "<p>Vælg først barbershop og dato.</p>";
        return;
    }
    timeSlots.innerHTML = "<p>Indlæser stole og reserverede tider …</p>";
    try {
        const response = await fetch(`/api/bookings/availability?barberId=${barberSelect.value}&date=${dateInput.value}`);
        if (!response.ok) throw new Error();
        renderAvailability(await response.json());
    } catch {
        timeSlots.innerHTML = "<p>Tider kunne ikke indlæses.</p>";
    }
}

function updateSubmitState() {
    bookButton.disabled = !(barberSelect.value && dateInput.value && chairSelect.value && timeInput.value && durationInput.value);
}

barberSelect.addEventListener("change", loadAvailability);
dateInput.addEventListener("change", loadAvailability);
[chairSelect, timeInput, durationInput].forEach(input => input.addEventListener("input", updateSubmitState));

form.addEventListener("submit", async event => {
    event.preventDefault();
    const token = csrf();
    const durationMinutes = Number(durationInput.value);
    if (!token || !chairSelect.value || !timeInput.value || !Number.isFinite(durationMinutes)) {
        message.textContent = "Vælg stol, tidspunkt og varighed.";
        return;
    }
    bookButton.disabled = true;
    const response = await fetch("/api/bookings", {
        method: "POST",
        headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": decodeURIComponent(token)},
        credentials: "same-origin",
        body: JSON.stringify({
            barberId: Number(barberSelect.value), chairId: Number(chairSelect.value), date: dateInput.value,
            startTime: timeInput.value, durationMinutes
        })
    });
    if (!response.ok) {
        message.textContent = (await response.text()) || "Kunne ikke gennemføre bookingen.";
        await loadAvailability();
        return;
    }
    const result = await response.json();
    message.textContent = `Din tid hos ${result.barberName} ved ${result.chairName} den ${result.date} kl. ${time(result.startTime)} i ${result.durationMinutes} minutter er bekræftet.`;
    form.reset();
    chairSelect.innerHTML = '<option value="">Vælg først barbershop og dato</option>';
    chairSelect.disabled = true;
    timeSlots.innerHTML = "<p>Tak for din booking.</p>";
    bookButton.disabled = true;
});

dateInput.min = new Date().toISOString().slice(0, 10);
loadCustomer();
loadBarbers();
