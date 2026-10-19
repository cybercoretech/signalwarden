package dev.cybercore.signalwarden

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.InetSocketAddress
import java.security.cert.CertificateException
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.system.measureTimeMillis

data class NetworkSnapshot(
    val connected: Boolean,
    val transport: String,
    val validated: Boolean,
    val metered: Boolean,
    val vpnActive: Boolean,
    val interfaceName: String?,
    val addresses: List<String>,
    val dnsServers: List<String>,
    val routes: List<String>,
    val proxy: String?,
) {
    companion object {
        fun read(context: Context): NetworkSnapshot {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val network = manager.activeNetwork ?: return disconnected()
            val caps = manager.getNetworkCapabilities(network) ?: return disconnected()
            val properties = manager.getLinkProperties(network)
            val transports = buildList {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Mobile")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("Bluetooth")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
            }
            return NetworkSnapshot(
                connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                transport = transports.ifEmpty { listOf("Other") }.joinToString(" + "),
                validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                vpnActive = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
                interfaceName = properties?.interfaceName,
                addresses = properties?.linkAddresses.orEmpty().map { it.address.hostAddress.orEmpty() },
                dnsServers = properties?.dnsServers.orEmpty().map { it.hostAddress.orEmpty() },
                routes = properties?.routes.orEmpty().take(4).map { route ->
                    val gateway = route.gateway?.hostAddress ?: "on-link"
                    "${route.destination}  →  $gateway"
                },
                proxy = properties?.httpProxy?.let { proxy ->
                    if (proxy.host.isNullOrBlank()) null else "${proxy.host}:${proxy.port}"
                },
            )
        }

        private fun disconnected() = NetworkSnapshot(
            connected = false,
            transport = "Offline",
            validated = false,
            metered = false,
            vpnActive = false,
            interfaceName = null,
            addresses = emptyList(),
            dnsServers = emptyList(),
            routes = emptyList(),
            proxy = null,
        )
    }
}

data class ProbeResult(
    val dnsOk: Boolean,
    val dnsMillis: Long?,
    val tlsOk: Boolean,
    val tlsMillis: Long?,
    val detail: String,
) {
    val successful: Boolean get() = dnsOk && tlsOk
}

object ConnectionProbe {
    private const val HOST = "example.com"
    private const val TIMEOUT_MS = 4_000

    fun run(network: Network?): ProbeResult {
        if (network == null) return ProbeResult(false, null, false, null, "No active network")

        var addresses: Array<java.net.InetAddress>? = null
        val dnsTime = runCatching {
            measureTimeMillis { addresses = network.getAllByName(HOST) }
        }.getOrNull()
        if (dnsTime == null || addresses.isNullOrEmpty()) {
            return ProbeResult(false, null, false, null, "DNS lookup for $HOST failed")
        }

        val tlsTime = runCatching {
            measureTimeMillis {
                network.socketFactory.createSocket().use { raw ->
                    raw.connect(InetSocketAddress(HOST, 443), TIMEOUT_MS)
                    ((SSLSocketFactory.getDefault() as SSLSocketFactory)
                        .createSocket(raw, HOST, 443, true) as SSLSocket)
                        .use { secure ->
                            secure.soTimeout = TIMEOUT_MS
                            secure.sslParameters = secure.sslParameters.apply {
                                endpointIdentificationAlgorithm = "HTTPS"
                            }
                            secure.startHandshake()
                            if (secure.session.peerCertificates.isEmpty()) {
                                throw CertificateException("Server did not present a certificate")
                            }
                        }
                }
            }
        }.getOrNull()

        return if (tlsTime == null) {
            ProbeResult(true, dnsTime, false, null, "DNS works; verified HTTPS connection failed")
        } else {
            ProbeResult(true, dnsTime, true, tlsTime, "DNS and verified HTTPS are reachable")
        }
    }
}

fun activeNetwork(context: Context): Network? =
    context.getSystemService(ConnectivityManager::class.java).activeNetwork
