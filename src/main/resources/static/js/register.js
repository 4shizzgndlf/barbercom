
const registerForm = document.getElementById("registerForm");
const message = document.getElementById("message");
const businessNameField = document.getElementById("businessNameField");
const businessNameInput = document.getElementById("businessName");
const businessSubscriptionNotice = document.getElementById("businessSubscriptionNotice");
const submitButton = registerForm.querySelector('button[type="submit"]');

function updateAccountType() {
    const isBusiness = registerForm.elements.role.value === "BUSINESS";
    businessNameField.hidden = !isBusiness;
    businessSubscriptionNotice.hidden = !isBusiness;
    submitButton.textContent = isBusiness ? "Fortsæt til betaling · 199 DKK/md." : "Opret konto";
    businessNameInput.required = isBusiness;
    if (!isBusiness) businessNameInput.value = "";
}

registerForm.querySelectorAll('input[name="role"]').forEach(input => {
    input.addEventListener("change", updateAccountType);
});

// Keep the company field hidden for the default private account selection.
updateAccountType();

if (new URLSearchParams(window.location.search).get("businessSubscription") === "cancelled") {
    message.textContent = "Betalingen blev annulleret. Din business-konto blev ikke oprettet.";
}

function getCsrfToken() {
    return document.cookie
        .split("; ")
        .find(cookie => cookie.startsWith("XSRF-TOKEN="))
        ?.split("=")
        .slice(1)
        .join("=");
}

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    message.textContent = "";

    const formData = new FormData(registerForm);

    const data = {
        firstName: formData.get("firstName"),
        lastName: formData.get("lastName"),
        email: formData.get("email"),
        password: formData.get("password"),
        phoneNumber: formData.get("phoneNumber")
            ? Number(formData.get("phoneNumber"))
            : null,
        address: formData.get("address") || null,
        zipCode: formData.get("zipCode")
            ? Number(formData.get("zipCode"))
            : null,
        role: formData.get("role"),
        businessName: formData.get("businessName") || null
    };

    const csrfToken = getCsrfToken();

    if (!csrfToken) {
        message.textContent =
            "Sikkerhedstoken mangler. Opdatér siden, og prøv igen.";
        return;
    }

    try {
        const response = await fetch("/api/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-XSRF-TOKEN": decodeURIComponent(csrfToken)
            },
            credentials: "same-origin",
            body: JSON.stringify(data)
        });

        const result = await response.json().catch(() => ({}));

        if (response.ok && result.checkoutUrl) {
            window.location.assign(result.checkoutUrl);
            return;
        }

        if (response.ok) {
            window.location.href = "/login.html?registered";
            return;
        }

        message.textContent =
            result.message || "Oprettelse mislykkedes. Kontrollér dine oplysninger.";
    } catch (error) {
        message.textContent = "Kunne ikke oprette forbindelse til serveren.";
        console.error(error);
    }
});
