const articles = {
    wax: {
        eyebrow: "Produktguide", title: "Sådan finder du den rigtige voks", image: "/images/article-wax.png", alt: "Hårvoks og barberredskaber på en barberdisk",
        intro: "Den rigtige voks skal arbejde med dit hår — ikke imod det. Her er den korte guide til at vælge finish og hold med ro i maven.",
        sections: [["Start med den finish, du vil have", "En mat voks giver et naturligt, tørt udtryk og passer godt til korte fades eller tekstureret hår. En pomade giver derimod mere glans og et skarpere, klassisk resultat. Vælg looket før du vælger produktet."], ["Hold skal passe til din hverdag", "Let hold er godt til hår, der skal kunne bevæge sig. Medium hold er den sikre allrounder, mens stærkt hold holder formen gennem en lang dag. Brug altid mindre end du tror — du kan nemt bygge mere på."], ["Sådan påfører du voks", "Varm produktet mellem hænderne, arbejd fra baghovedet mod fronten og form til sidst detaljerne med fingerspidserne. Din barber kan altid hjælpe dig med at finde den type, der passer til din hårtype."]]
    },
    fade: {
        eyebrow: "Barberens råd", title: "Hvad gør en fade virkelig skarp?", image: "/images/article-fade.png", alt: "Barber der laver en skarp fade",
        intro: "En god fade ser enkel ud, men den bygger på præcision, balance og en overgang, der er lavet til netop din hovedform.",
        sections: [["Overgangen er hele pointen", "En fade skal glide fra kort til længere hår uden hårde streger. Barberen arbejder med små justeringer og forskellige afstandskamme, så overgangen ser naturlig ud fra alle vinkler."], ["Din hovedform bestemmer placeringen", "En low fade kan give et roligt og klassisk look, mens en mid eller high fade skaber mere kontrast. Den bedste placering handler ikke om trends alene, men om at fremhæve dine proportioner."], ["Hold din fade frisk", "Planlæg en hurtig oprydning efter to til tre uger, hvis du vil holde linjerne knivskarpe. Vask håret, brug et let stylingprodukt og lad din barber vise dig, hvordan du nemt styler toppen derhjemme."]]
    },
    copenhagen: {
        eyebrow: "København", title: "5 barbershops, du bør kende", image: "/images/article-copenhagen-barbers.png", alt: "Barbershops i København",
        intro: "København er fyldt med gode stole, skarpe fades og barbere med deres helt egen stil. Her er fem ting, der er værd at kigge efter, når du vælger din næste shop.",
        sections: [["Find en stil, der passer til dig", "Nogle shops er stærke i klassiske herreklip, andre i moderne fades, skægtrim eller længere hår. Se deres arbejde, spørg ind til specialer og vælg et sted, hvor du kan mærke, at din stil bliver forstået."], ["God konsultation før saksen", "De bedste besøg starter med et par minutter i stolen: Hvordan styler du håret? Hvor ofte vil du klippes? Hvad fungerede sidst? Det gør det lettere at lande præcis det resultat, du håber på."], ["Gør det til en fast rutine", "Når du finder en barber, der kender dit hår, bliver hvert besøg bedre. Gem dine præferencer, book i god tid og brug samme produkter hjemme, som barberen anbefaler til dit look."]]
    }
};

const article = articles[new URLSearchParams(window.location.search).get("id")] || articles.wax;
document.title = `${article.title} | BookaBarber`;
document.querySelector("#articlePage").innerHTML = `<a class="back-link article-back" href="/#articlesTitle">← Tilbage til journalen</a><article class="article-content"><header><p class="eyebrow">${article.eyebrow}</p><h1>${article.title}</h1><p class="article-intro">${article.intro}</p></header><img class="article-hero" src="${article.image}" alt="${article.alt}">${article.sections.map(([heading, text]) => `<section><h2>${heading}</h2><p>${text}</p></section>`).join("")}<a class="primary-button article-shop-link" href="/shop.html">Se produkter i shoppen</a></article>`;
