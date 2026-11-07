(() => {
    const root = document.documentElement;
    root.classList.add("page-is-loading");

    const addLoader = () => {
        if (document.querySelector(".site-loader")) return;

        document.body.insertAdjacentHTML("afterbegin", `
            <div class="site-loader" role="status" aria-live="polite" aria-label="BookaBarber indlæser siden">
                <div class="site-loader__ring" aria-hidden="true"></div>
                <img class="site-loader__logo" src="/images/bookabarber-logo.png" alt="BookaBarber">
                <span class="site-loader__text">Indlæser din næste fade</span>
            </div>`);

        const loader = document.querySelector(".site-loader");
        let dismissed = false;
        const dismiss = () => {
            if (dismissed) return;
            dismissed = true;
            root.classList.remove("page-is-loading");
            loader.classList.add("site-loader--leaving");
            window.setTimeout(() => loader.remove(), 320);
        };

        // Never keep a visitor waiting more than five seconds for a slow image.
        if (root.dataset.waitForDynamicContent === "true") {
            // Some pages create their images after fetching data from the API.
            // They tell the loader when those images have settled.
            window.addEventListener("bookabarber:content-ready", dismiss, { once: true });
        } else if (document.readyState === "complete") {
            dismiss();
        } else {
            window.addEventListener("load", dismiss, { once: true });
        }
        window.setTimeout(dismiss, 5000);
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", addLoader, { once: true });
    } else {
        addLoader();
    }
})();
