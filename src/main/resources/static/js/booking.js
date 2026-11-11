const form = document.querySelector("#bookingForm");
const barberSelect = document.querySelector("#bookingBarber");
const dateInput = document.querySelector("#bookingDate");
const timeSlots = document.querySelector("#timeSlots");
const bookButton = document.querySelector("#bookButton");
const message = document.querySelector("#bookingMessage");
const csrf = () => document.cookie.split("; ").find(value => value.startsWith("XSRF-TOKEN="))?.split("=").slice(1).join("=");
const bookingDurationMinutes = 20;
const slots = ["09:00", "09:20", "09:40", "10:00", "10:20", "10:40", "11:00", "11:20", "11:40", "12:00", "12:20", "12:40", "13:00", "13:20", "13:40", "14:00", "14:20", "14:40", "15:00", "15:20", "15:40", "16:00", "16:20", "16:40", "17:00", "17:20", "17:40"];
let selectedTime = null;
let selectedChairId = null;
let availability = [];

function time(value) { return String(value).slice(0, 5); }
function minutes(value) { const [hours, minutes] = time(value).split(":").map(Number); return hours * 60 + minutes; }
function currentDuration() { return bookingDurationMinutes; }
function isPast(timeValue) {
    const now = new Date();
    return dateInput.value === now.toISOString().slice(0, 10) && minutes(timeValue) <= now.getHours() * 60 + now.getMinutes();
}

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

function slotIsUnavailable(chair, startTime) {
    const duration = currentDuration();
    if (!Number.isFinite(duration) || duration < 5 || duration > 480) return true;
    const start = minutes(startTime);
    const end = start + duration;
    if (end > 18 * 60 || isPast(startTime)) return true;
    return chair.bookings.some(booking => start < minutes(booking.endTime) && minutes(booking.startTime) < end);
}

function renderAvailability() {
    if (selectedChairId && selectedTime) {
        const selectedChair = availability.find(chair => chair.chairId === selectedChairId);
        if (!selectedChair || slotIsUnavailable(selectedChair, selectedTime)) {
            selectedChairId = null;
            selectedTime = null;
        }
    }
    timeSlots.innerHTML = availability.map(chair => `<section class="chair-slots"><h3>${chair.chairName}</h3><div>${slots.map(startTime => {
        const unavailable = slotIsUnavailable(chair, startTime);
        const selected = selectedChairId === chair.chairId && selectedTime === startTime;
        return `<button type="button" class="time-slot${selected ? " is-selected" : ""}${unavailable ? " is-booked" : ""}" ${unavailable ? "disabled" : ""} data-chair="${chair.chairId}" data-time="${startTime}">${startTime}</button>`;
    }).join("")}</div></section>`).join("");
    updateSubmitState();
}

async function loadAvailability() {
    selectedTime = null;
    selectedChairId = null;
    bookButton.disabled = true;
    if (!barberSelect.value || !dateInput.value) {
        timeSlots.innerHTML = "<p>Vælg først barbershop og dato.</p>";
        return;
    }
    timeSlots.innerHTML = "<p>Indlæser stole og reserverede tider …</p>";
    try {
        const response = await fetch(`/api/bookings/availability?barberId=${barberSelect.value}&date=${dateInput.value}`);
        if (!response.ok) throw new Error();
        availability = await response.json();
        renderAvailability();
    } catch {
        timeSlots.innerHTML = "<p>Tider kunne ikke indlæses.</p>";
    }
}

function updateSubmitState() {
    bookButton.disabled = !(barberSelect.value && dateInput.value && selectedChairId && selectedTime && Number.isFinite(currentDuration()));
}

barberSelect.addEventListener("change", loadAvailability);
dateInput.addEventListener("change", loadAvailability);
timeSlots.addEventListener("click", event => {
    const button = event.target.closest("[data-time]");
    if (!button || button.disabled) return;
    selectedChairId = Number(button.dataset.chair);
    selectedTime = button.dataset.time;
    renderAvailability();
});

form.addEventListener("submit", async event => {
    event.preventDefault();
    const token = csrf();
    const durationMinutes = bookingDurationMinutes;
    if (!token || !selectedChairId || !selectedTime) {
        message.textContent = "Vælg en ledig stol og et tidspunkt.";
        return;
    }
    bookButton.disabled = true;
    const response = await fetch("/api/bookings", {
        method: "POST",
        headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": decodeURIComponent(token)},
        credentials: "same-origin",
        body: JSON.stringify({
            barberId: Number(barberSelect.value), chairId: selectedChairId, date: dateInput.value,
            startTime: selectedTime, durationMinutes
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
    selectedTime = null;
    selectedChairId = null;
    availability = [];
    timeSlots.innerHTML = "<p>Tak for din booking.</p>";
    bookButton.disabled = true;
});

dateInput.min = new Date().toISOString().slice(0, 10);
loadCustomer();
loadBarbers();
