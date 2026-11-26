package yukashi.blast.data

data class DnsProfile(val id: String, val title: String, val hostname: String)

object DnsProfiles {
    // Hostnames are deliberately not rendered in the app UI.
    // Profile 1 hostname corrected after user confirmed a typo.
    val all = listOf(
        DnsProfile("profile_1", "DNS Aman 1", "dns.adguard.com"),
        DnsProfile("profile_2", "DNS Aman 2", "dns.adguard-dns.com"),
        DnsProfile("profile_3", "DNS Aman 3", "one.one.one.one")
    )
}
