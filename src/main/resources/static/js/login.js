
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

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const token = csrfToken();
    if (!token) {
        message.textContent = "Sikkerhedstoken mangler. Opdatér siden, og prøv igen.";
        return;
    }

    const email = document.querySelector("#email").value.trim();
    const password = document.querySelector("#password").value;

    try {
        const response = await fetch("/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-XSRF-TOKEN": decodeURIComponent(token)
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
