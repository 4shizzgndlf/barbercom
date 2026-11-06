
document.addEventListener("DOMContentLoaded", () => {
    const logoutButton = document.querySelector("#logoutButton");

    if (!logoutButton) return;

    async function getCsrfToken() {
        const cookieToken = document.cookie
            .split("; ")
            .find(cookie => cookie.startsWith("XSRF-TOKEN="))
            ?.split("=")
            .slice(1)
            .join("=");
        if (cookieToken) return decodeURIComponent(cookieToken);

        const response = await fetch("/api/csrf", {
            credentials: "same-origin",
            cache: "no-store"
        });
        if (!response.ok) return null;
        return (await response.json()).token || null;
    }

    logoutButton.addEventListener("click", async () => {
        try {
            const token = await getCsrfToken();
            if (!token) throw new Error("missing-token");
            const response = await fetch("/logout", {
                method: "POST",
                headers: {
                    "X-XSRF-TOKEN": token
                },
                credentials: "same-origin",
                redirect: "follow"
            });

            if (response.ok) {
                sessionStorage.removeItem("barbercom-cart-owner");
                sessionStorage.removeItem("barbercom-guest-cart-id");
                window.location.href = "/login.html?logout";
            } else {
                alert("Kunne ikke logge ud. Prøv igen.");
            }
        } catch {
            alert("Kunne ikke oprette forbindelse til serveren.");
        }
    });
});
