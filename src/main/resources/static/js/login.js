
const form = document.querySelector("#loginForm");
const message = document.querySelector("#message");

if (new URLSearchParams(window.location.search).get("businessSubscription") === "success") {
    message.textContent = "Tak for din betaling. Din business-konto er nu aktiveret — du kan logge ind.";
}

function csrfToken() {
    return document.cookie
        .split("; ")
        .find(cookie => cookie.startsWith("XSRF-TOKEN="))
        ?.split("=").slice(1).join("=");
}

async function getCsrfToken() {
    const cookieToken = csrfToken();
    if (cookieToken) {
        return decodeURIComponent(cookieToken);
    }

    // A first visit to a new domain can occur before the CSRF cookie has been
    // written. Ask the server for a token instead of making the user refresh.
    const response = await fetch("/api/csrf", {
        credentials: "same-origin",
        cache: "no-store"
    });
    if (!response.ok) {
        return null;
    }

    const data = await response.json();
    return data.token || null;
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const email = document.querySelector("#email").value.trim();
    const password = document.querySelector("#password").value;

    try {
        const token = await getCsrfToken();
        if (!token) {
            message.textContent = "Sikkerhedstoken mangler. Prøv igen om et øjeblik.";
            return;
        }

        const response = await fetch("/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-XSRF-TOKEN": token
            },
            body: new URLSearchParams({ email, password }),
            credentials: "same-origin"
        });

        if (response.ok && !response.url.includes("/login.html")) {
            window.location.assign(response.url);
            return;
        }

        message.textContent = "Login mislykkedes. Kontrollér din e-mail og adgangskode.";
    } catch {
        message.textContent = "Kunne ikke oprette forbindelse til serveren.";
    }
});
