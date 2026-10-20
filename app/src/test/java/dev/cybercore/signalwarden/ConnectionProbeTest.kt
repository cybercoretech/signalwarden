package dev.cybercore.signalwarden

import java.io.IOException
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionProbeTest {
    @Test
    fun successfulProbeReportsIndependentStageDurationsAndFixedDestination() {
        val clock = FakeClock()
        val transport = FakeTransport(clock)
            .withResolution(address("192.0.2.10"), address("192.0.2.11"))
            .withTimings(dns = 12, tcp = 24, tls = 35)

        val result = ConnectionProbe.run(transport, clock)

        assertTrue(result.successful)
        assertEquals(12L, result.dnsMillis)
        assertEquals(2, result.resolvedAddressCount)
        assertEquals(24L, result.tcpMillis)
        assertEquals(35L, result.tlsMillis)
        assertEquals(ConnectionProbe.HOST, transport.requestedHost)
        assertEquals(address("192.0.2.10"), transport.connectedAddress)
        assertEquals(ConnectionProbe.PORT, transport.connectedPort)
        assertEquals(ConnectionProbe.HOST, transport.tlsHost)
    }

    @Test
    fun dnsFailureStopsBeforeOpeningConnection() {
        val clock = FakeClock()
        val transport = FakeTransport(clock).failResolution()

        val result = ConnectionProbe.run(transport, clock)

        assertFalse(result.dnsOk)
        assertNull(result.tcpMillis)
        assertNull(result.tlsMillis)
        assertEquals(0, transport.openCount)
        assertTrue(result.detail.contains("DNS lookup"))
    }

    @Test
    fun emptyDnsAnswerIsReportedAndDoesNotAttemptTcp() {
        val clock = FakeClock()
        val transport = FakeTransport(clock).withResolution()

        val result = ConnectionProbe.run(transport, clock)

        assertFalse(result.dnsOk)
        assertEquals(0, result.resolvedAddressCount)
        assertNull(result.tcpMillis)
        assertEquals(0, transport.openCount)
        assertTrue(result.detail.contains("no addresses"))
    }

    @Test
    fun tcpFailureStopsBeforeTlsAndKeepsMeasuredLatency() {
        val clock = FakeClock()
        val transport = FakeTransport(clock)
            .withResolution(address("192.0.2.10"))
            .withTimings(dns = 5, tcp = 47)
            .failTcp()

        val result = ConnectionProbe.run(transport, clock)

        assertTrue(result.dnsOk)
        assertFalse(result.tcpOk)
        assertEquals(47L, result.tcpMillis)
        assertNull(result.tlsMillis)
        assertFalse(transport.tlsAttempted)
    }

    @Test
    fun tcpProbeFallsBackToSecondDnsAnswerWhenFirstAddressFails() {
        val first = address("192.0.2.10")
        val second = address("192.0.2.11")
        val clock = FakeClock()
        val transport = FakeTransport(clock)
            .withResolution(first, second)
            .withTimings(dns = 5, tcp = 14, tls = 30)
            .failFirstTcp()

        val result = ConnectionProbe.run(transport, clock)

        assertTrue(result.successful)
        assertEquals(2, transport.openCount)
        assertEquals(second, transport.connectedAddress)
        assertEquals(28L, result.tcpMillis)
    }

    @Test
    fun tlsFailurePreservesSuccessfulDnsAndTcpMeasurements() {
        val clock = FakeClock()
        val transport = FakeTransport(clock)
            .withResolution(address("192.0.2.10"))
            .withTimings(dns = 7, tcp = 18, tls = 91)
            .failTls()

        val result = ConnectionProbe.run(transport, clock)

        assertTrue(result.dnsOk)
        assertTrue(result.tcpOk)
        assertFalse(result.tlsOk)
        assertEquals(7L, result.dnsMillis)
        assertEquals(18L, result.tcpMillis)
        assertEquals(91L, result.tlsMillis)
        assertFalse(result.successful)
    }

    @Test
    fun missingNetworkReturnsFailureWithoutMeasurements() {
        val result = ConnectionProbe.run(null)

        assertFalse(result.successful)
        assertNull(result.dnsMillis)
        assertNull(result.tcpMillis)
        assertNull(result.tlsMillis)
        assertEquals("No active network", result.detail)
    }

    private class FakeClock : MonotonicClock {
        var now = 0L
        override fun nowMillis() = now
        fun advance(duration: Long) { now += duration }
    }

    private class FakeTransport(private val clock: FakeClock) : ProbeTransport {
        private var addresses = emptyList<InetAddress>()
        private var dnsDuration = 0L
        private var tcpDuration = 0L
        private var tlsDuration = 0L
        private var resolutionFails = false
        private var tcpFails = false
        private var firstTcpFails = false
        private var tlsFails = false

        var requestedHost: String? = null
            private set
        var connectedAddress: InetAddress? = null
            private set
        var connectedPort: Int? = null
            private set
        var tlsHost: String? = null
            private set
        var tlsAttempted = false
            private set
        var openCount = 0
            private set

        fun withResolution(vararg result: InetAddress) = apply { addresses = result.toList() }
        fun withTimings(dns: Long, tcp: Long = 0, tls: Long = 0) = apply {
            dnsDuration = dns
            tcpDuration = tcp
            tlsDuration = tls
        }
        fun failResolution() = apply { resolutionFails = true }
        fun failTcp() = apply { tcpFails = true }
        fun failFirstTcp() = apply { firstTcpFails = true }
        fun failTls() = apply { tlsFails = true }

        override fun resolve(host: String): List<InetAddress> {
            requestedHost = host
            clock.advance(dnsDuration)
            if (resolutionFails) throw IOException("simulated DNS failure")
            return addresses
        }

        override fun open(address: InetAddress, port: Int): ProbeConnection {
            openCount++
            connectedAddress = address
            connectedPort = port
            return object : ProbeConnection {
                override fun connect(timeoutMillis: Int) {
                    clock.advance(tcpDuration)
                    if (tcpFails || (firstTcpFails && address == addresses.firstOrNull())) {
                        throw IOException("simulated TCP failure")
                    }
                }

                override fun verifyTls(host: String, timeoutMillis: Int) {
                    tlsAttempted = true
                    tlsHost = host
                    clock.advance(tlsDuration)
                    if (tlsFails) throw IOException("simulated TLS failure")
                }

                override fun close() = Unit
            }
        }
    }

    private fun address(ip: String): InetAddress = InetAddress.getByName(ip)
}
