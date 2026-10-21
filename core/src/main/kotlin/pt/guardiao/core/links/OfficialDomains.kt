package pt.guardiao.core.links

/**
 * Brands commonly impersonated in Portugal and the registrable domains they
 * actually use. A link that mentions a brand but is not on one of its domains
 * is treated as a lookalike. Kept deliberately small and reviewable: it must be
 * maintained, and an omission produces a false positive rather than trust.
 */
object OfficialDomains {

    /**
     * @param exactOnly when true, a token must be a whole part of the host name
     *   ("apple-id.com"), not a substring ("pineapple.com").
     */
    data class Brand(val tokens: List<String>, val domains: Set<String>, val exactOnly: Boolean = false)

    val BRANDS: List<Brand> = listOf(
        Brand(listOf("cgd", "caixadirecta", "caixageral"), setOf("cgd.pt", "caixadirecta.cgd.pt")),
        Brand(listOf("millennium", "millenniumbcp"), setOf("millenniumbcp.pt", "millennium.pt")),
        Brand(listOf("novobanco"), setOf("novobanco.pt")),
        Brand(listOf("santander"), setOf("santander.pt", "santander.com")),
        Brand(listOf("bancobpi", "bpinet"), setOf("bancobpi.pt", "bpi.pt", "bpinet.pt")),
        Brand(listOf("montepio"), setOf("montepio.pt", "montepio.org")),
        Brand(listOf("creditoagricola"), setOf("creditoagricola.pt")),
        Brand(listOf("activobank"), setOf("activobank.pt")),
        Brand(listOf("bancoctt"), setOf("bancoctt.pt")),
        Brand(listOf("mbway"), setOf("mbway.pt")),
        Brand(listOf("sibs", "multibanco"), setOf("sibs.com", "sibs.pt", "multibanco.pt")),
        Brand(listOf("ctt", "cttexpresso"), setOf("ctt.pt")),
        Brand(listOf("dhl"), setOf("dhl.com", "dhl.pt")),
        Brand(listOf("fedex"), setOf("fedex.com")),
        Brand(listOf("portaldasfinancas", "financas", "autoridadetributaria"), setOf("portaldasfinancas.gov.pt", "gov.pt")),
        Brand(listOf("segsocial", "segurancasocial"), setOf("seg-social.pt", "gov.pt")),
        Brand(listOf("viaverde"), setOf("viaverde.pt")),
        Brand(listOf("policiajudiciaria"), setOf("policiajudiciaria.pt")),
        Brand(listOf("bportugal", "bancodeportugal"), setOf("bportugal.pt")),
        Brand(listOf("whatsapp"), setOf("whatsapp.com", "whatsapp.net", "wa.me")),
        Brand(listOf("paypal"), setOf("paypal.com", "paypal.me")),
        Brand(listOf("microsoft", "office365"), setOf("microsoft.com", "outlook.com", "live.com", "office.com", "office365.com")),
        Brand(listOf("apple", "icloud"), setOf("apple.com", "icloud.com"), exactOnly = true),
        Brand(listOf("netflix"), setOf("netflix.com")),
        Brand(listOf("amazon"), setOf("amazon.com", "amazon.es", "amazon.de", "amazon.fr"), exactOnly = true),
    )

    /** Public suffixes with two labels that we need to recognise. Not a full public suffix list. */
    val MULTI_LABEL_SUFFIXES = setOf(
        "gov.pt", "com.pt", "org.pt", "edu.pt", "net.pt", "co.uk", "org.uk", "com.br", "gov.br", "com.es",
    )

    val SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly", "cutt.ly", "rb.gy",
        "shorturl.at", "tiny.cc", "s.id", "t.ly", "rebrand.ly", "bl.ink", "short.io", "lnkd.in",
    )

    /** Endings frequently seen in phishing campaigns. Legitimate sites use them too. */
    val UNUSUAL_TLDS = setOf(
        "top", "xyz", "icu", "buzz", "cfd", "sbs", "click", "link", "live", "shop", "online", "site",
        "rest", "cyou", "monster", "quest", "tk", "ml", "ga", "cf", "gq", "zip", "mov", "lat", "vip", "bond",
    )

    /** Free email providers. A bank or public authority does not write from these. */
    val FREE_MAIL = setOf(
        "gmail.com", "hotmail.com", "outlook.com", "outlook.pt", "live.com", "yahoo.com", "sapo.pt",
        "icloud.com", "aol.com", "gmx.com", "proton.me", "protonmail.com", "mail.com",
    )

    /** Organisations people expect to write from their own domain, by display-name token. */
    val ORGANISATION_SENDERS: List<Brand> = BRANDS + listOf(
        Brand(listOf("banco", "bank", "seguranca social", "financas", "policia", "tribunal", "governo"), emptySet()),
    )
}
