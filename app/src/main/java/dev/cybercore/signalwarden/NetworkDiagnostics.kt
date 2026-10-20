package dev.cybercore.signalwarden

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.io.Closeable
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.CertificateException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

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
    val resolvedAddressCount: Int,
    val tcpOk: Boolean,
    val tcpMillis: Long?,
    val tlsOk: Boolean,
    val tlsMillis: Long?,
    val detail: String,
) {
    val successful: Boolean get() = dnsOk && tcpOk && tlsOk
}

object ConnectionProbe {
    const val HOST = "example.com"
    const val PORT = 443
    private const val TIMEOUT_MS = 4_000
    private const val TCP_ADDRESS_TIMEOUT_MS = 2_000

    fun run(network: Network?): ProbeResult {
        if (network == null) return failed("No active network")
        return run(AndroidProbeTransport(network), MonotonicClock.SYSTEM)
    }

    internal fun run(transport: ProbeTransport, clock: MonotonicClock): ProbeResult {
        val dnsStart = clock.nowMillis()
        val addresses = runCatching { transport.resolve(HOST) }.getOrElse {
            return ProbeResult(
                dnsOk = false,
                dnsMillis = elapsed(dnsStart, clock),
                resolvedAddressCount = 0,
                tcpOk = false,
                tcpMillis = null,
                tlsOk = false,
                tlsMillis = null,
                detail = "DNS lookup for $HOST failed",
            )
        }
        val dnsMillis = elapsed(dnsStart, clock)
        if (addresses.isEmpty()) {
            return ProbeResult(false, dnsMillis, 0, false, null, false, null, "DNS returned no addresses for $HOST")
        }

        val tcpStart = clock.nowMillis()
        var connection: ProbeConnection? = null
        for (address in addresses.take(2)) {
            val candidate = runCatching { transport.open(address, PORT) }.getOrNull() ?: continue
            if (runCatching { candidate.connect(TCP_ADDRESS_TIMEOUT_MS) }.isSuccess) {
                connection = candidate
                break
            }
            runCatching { candidate.close() }
        }
        val tcpMillis = elapsed(tcpStart, clock)
        val connected = connection ?: return ProbeResult(
            true, dnsMillis, addresses.size, false, tcpMillis, false, null,
            "DNS works; TCP connection to $HOST:$PORT failed for all attempted DNS answers",
        )
        connected.use {
            val tlsStart = clock.nowMillis()
            val tlsOk = runCatching { it.verifyTls(HOST, TIMEOUT_MS) }.isSuccess
            val tlsMillis = elapsed(tlsStart, clock)
            return if (tlsOk) {
                ProbeResult(true, dnsMillis, addresses.size, true, tcpMillis, true, tlsMillis, "DNS, TCP, and verified HTTPS are reachable")
            } else {
                ProbeResult(true, dnsMillis, addresses.size, true, tcpMillis, false, tlsMillis, "DNS and TCP work; verified HTTPS handshake failed")
            }
        }
    }

    private fun elapsed(start: Long, clock: MonotonicClock) = (clock.nowMillis() - start).coerceAtLeast(0)

    private fun failed(detail: String) = ProbeResult(
        dnsOk = false,
        dnsMillis = null,
        resolvedAddressCount = 0,
        tcpOk = false,
        tcpMillis = null,
        tlsOk = false,
        tlsMillis = null,
        detail = detail,
    )
}

internal fun interface MonotonicClock {
    fun nowMillis(): Long

    companion object {
        val SYSTEM = MonotonicClock { android.os.SystemClock.elapsedRealtime() }
    }
}

internal interface ProbeTransport {
    fun resolve(host: String): List<InetAddress>
    fun open(address: InetAddress, port: Int): ProbeConnection
}

internal interface ProbeConnection : Closeable {
    fun connect(timeoutMillis: Int)
    fun verifyTls(host: String, timeoutMillis: Int)
}

private class AndroidProbeTransport(private val network: Network) : ProbeTransport {
    override fun resolve(host: String): List<InetAddress> = network.getAllByName(host).toList()

    override fun open(address: InetAddress, port: Int): ProbeConnection = AndroidProbeConnection(network, address, port)
}

private class AndroidProbeConnection(
    private val network: Network,
    private val address: InetAddress,
    private val port: Int,
) : ProbeConnection {
    private var raw: Socket? = null
    private var secure: SSLSocket? = null

    override fun connect(timeoutMillis: Int) {
        raw = network.socketFactory.createSocket().apply {
            connect(InetSocketAddress(address, port), timeoutMillis)
        }
    }

    override fun verifyTls(host: String, timeoutMillis: Int) {
        val socket = requireNotNull(raw) { "TCP must connect before TLS verification" }
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val tls = factory.createSocket(socket, host, port, true) as SSLSocket
        secure = tls
        tls.soTimeout = timeoutMillis
        tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
        tls.startHandshake()
        if (tls.session.peerCertificates.isEmpty()) throw CertificateException("Server did not present a certificate")
    }

    override fun close() {
        runCatching { secure?.close() }
        runCatching { raw?.close() }
    }
}

fun activeNetwork(context: Context): Network? =
    context.getSystemService(ConnectivityManager::class.java).activeNetwork
