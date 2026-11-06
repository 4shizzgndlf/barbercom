
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

async function isLoggedIn() {
    const response = await fetch("/api/auth/status", {
        credentials: "same-origin",
        cache: "no-store"
    });
    return response.ok && (await response.json()).authenticated === true;
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

        // Spring redirects a successful login to the front page and a failed
        // login back to login.html?error. The redirect itself is definitive;
        // do not let a second session request turn a successful login into a
        // false error message.
        if (response.ok && !new URL(response.url).pathname.endsWith("/login.html")) {
            window.location.assign("/");
            return;
        }

        message.textContent = "Login mislykkedes. Kontrollér din e-mail og adgangskode.";
    } catch {
        // The server can finish the login even if a proxy interrupts the
        // fetch response while it follows the redirect. Check the session
        // once before showing a connection error.
        try {
            if (await isLoggedIn()) {
                window.location.assign("/");
                return;
            }
        } catch {
            // Show the normal message below if the status check also fails.
        }
        message.textContent = "Kunne ikke oprette forbindelse til serveren.";
    }
});
