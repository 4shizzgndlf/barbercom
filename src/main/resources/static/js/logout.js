
document.addEventListener("DOMContentLoaded", () => {
    const logoutButton = document.querySelector("#logoutButton");

    if (!logoutButton) return;

    logoutButton.addEventListener("click", async () => {
        const token = document.cookie
            .split("; ")
            .find(cookie => cookie.startsWith("XSRF-TOKEN="))
            ?.split("=")
            .slice(1)
            .join("=");

        if (!token) {
            alert("Sikkerhedstoken mangler. Opdatér siden, og prøv igen.");
            return;
        }

        try {
            const response = await fetch("/logout", {
                method: "POST",
                headers: {
                    "X-XSRF-TOKEN": decodeURIComponent(token)
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
