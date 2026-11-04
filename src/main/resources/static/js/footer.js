if (!document.querySelector(".site-footer")) {
    document.body.insertAdjacentHTML("beforeend", `
        <footer class="site-footer">
            <div class="footer-top">
                <div><img class="footer-logo" src="/images/barbercom-logo.png" alt="Barber.com"><p class="footer-intro">Din genvej til en fresh fade, gode barbers og produkter, der holder din stil på plads.</p></div>
                <div class="footer-column"><h3>Barber.com</h3><a href="/">Forside</a><a href="/booking.html">Book tid</a><a href="/shop.html">Produkter</a></div>
                <div class="footer-column"><h3>Kontakt</h3><p>Lucas Hoffmann Christensen</p><a href="mailto:loegcas@gmail.com">loegcas@gmail.com</a><a href="tel:+4561128112">+45 61128112</a><p>Vesterbrogade 181<br>1800 Frederiksberg</p></div>
                <div class="footer-column"><h3>Følg os</h3><div class="social-links"><a href="https://instagram.com" target="_blank" rel="noreferrer">Instagram</a><a href="https://tiktok.com" target="_blank" rel="noreferrer">TikTok</a><a href="https://facebook.com" target="_blank" rel="noreferrer">Facebook</a><a href="https://youtube.com" target="_blank" rel="noreferrer">YouTube</a></div></div>
            </div>
            <div class="footer-bottom"><span>© 2026 Barber.com. Alle rettigheder forbeholdes.</span><span>Privatliv · Vilkår · Cookies</span></div>
        </footer>`);
}
