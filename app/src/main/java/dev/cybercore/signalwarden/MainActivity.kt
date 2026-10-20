package dev.cybercore.signalwarden

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF080D14)
private val Panel = Color(0xFF111A24)
private val Line = Color(0xFF253443)
private val Cyan = Color(0xFF32E6ED)
private val Lime = Color(0xFFB8F36B)
private val Muted = Color(0xFF9AAAB7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SignalWardenScreen() }
    }
}

@androidx.compose.runtime.Composable
private fun SignalWardenScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(NetworkSnapshot.read(context)) }
    var probe by remember { mutableStateOf<ProbeResult?>(null) }
    var testing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { snapshot = NetworkSnapshot.read(context) }
    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun refresh() { handler.post { snapshot = NetworkSnapshot.read(context) } }
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: android.net.NetworkCapabilities) = refresh()
            override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) = refresh()
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan,
        secondary = Lime,
        background = Ink,
        surface = Panel,
        onPrimary = Ink,
        onBackground = Color(0xFFE8F0F4),
        onSurface = Color(0xFFE8F0F4),
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = Color(0xFF10242B), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text("⌁", color = Cyan, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SIGNALWARDEN", color = Cyan, fontSize = 12.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold)
                        Text("Network diagnostics", color = Muted, fontSize = 13.sp)
                    }
                    androidx.compose.material3.IconButton(onClick = { snapshot = NetworkSnapshot.read(context) }) {
                        androidx.compose.material3.Icon(Icons.Default.Refresh, "Refresh network status", tint = Muted)
                    }
                }

                StatusCard(snapshot)
                DiagnosticsCard(
                    result = probe,
                    testing = testing,
                    onRun = {
                        if (!testing) {
                            testing = true
                            scope.launch {
                                val network = activeNetwork(context)
                                probe = withContext(Dispatchers.IO) { ConnectionProbe.run(network) }
                                testing = false
                            }
                        }
                    },
                )
                NetworkDetails(snapshot)
                PrivacyNote()
                Text("PHASE 1.1  /  LOCAL DIAGNOSTICS  /  NO CAPTURE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp, bottom = 8.dp))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StatusCard(snapshot: NetworkSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.Icon(
                    if (snapshot.validated) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (snapshot.validated) Lime else Color(0xFFFFB45E),
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(if (snapshot.validated) "Internet reachable" else if (snapshot.connected) "Network connected · unverified" else "No active connection", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("${snapshot.transport}  ·  ${if (snapshot.metered) "Metered" else "Not metered / unknown"}", color = Muted, fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = Line)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("SYSTEM CHECK", if (snapshot.validated) "PASS" else "CHECK", if (snapshot.validated) Lime else Color(0xFFFFB45E))
                if (snapshot.vpnActive) InfoPill("VPN", "ACTIVE", Cyan)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DiagnosticsCard(result: ProbeResult?, testing: Boolean, onRun: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10222A)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ON-DEMAND LATENCY + CONNECTION TEST", color = Cyan, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Text("Measure the path to example.com:443", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text("When you tap, SignalWarden measures the Android DNS resolver call (which may use cache), TCP connect, and verified TLS handshake. It uses the active Android network; results stay on this screen and are not saved.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            if (result != null) {
                HorizontalDivider(color = Line)
                CheckLine("DNS · ${result.resolvedAddressCount} answers", result.dnsOk, result.dnsMillis?.let { "${it} ms" } ?: "Failed")
                CheckLine("TCP connect", result.tcpOk, result.tcpMillis?.let { "${it} ms" } ?: if (result.dnsOk) "Failed" else "Not run")
                CheckLine("Verified TLS", result.tlsOk, result.tlsMillis?.let { "${it} ms" } ?: if (result.tcpOk) "Failed" else "Not run")
                Text(result.detail, color = if (result.successful) Lime else Color(0xFFFFB45E), fontSize = 12.sp)
            }
            Button(
                onClick = onRun,
                enabled = !testing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (testing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                else Text("Run latency + connection test", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NetworkDetails(snapshot: NetworkSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("NETWORK DETAILS", color = Cyan, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            DetailRow("Interface", snapshot.interfaceName ?: "—")
            DetailRow("DNS servers", snapshot.dnsServers.ifEmpty { listOf("—") }.joinToString(" · "))
            DetailRow("Device addresses", snapshot.addresses.ifEmpty { listOf("—") }.joinToString(" · "))
            if (snapshot.proxy != null) DetailRow("Proxy", snapshot.proxy)
            if (snapshot.routes.isNotEmpty()) {
                HorizontalDivider(color = Line)
                Text("ROUTES", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                snapshot.routes.forEach { Text(it, color = Color(0xFFD2DDE4), fontSize = 12.sp) }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PrivacyNote() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10151C)), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            androidx.compose.material3.Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Lime, modifier = Modifier.size(21.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Private by design", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Phase 1.1 reads Android network state and runs a test only when you tap. It has no VPN service, account, analytics, or background capture.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun InfoPill(label: String, value: String, tint: Color) {
    Surface(color = Color(0xFF0A1219), shape = RoundedCornerShape(9.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            Text(value, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@androidx.compose.runtime.Composable
private fun CheckLine(label: String, passed: Boolean, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(if (passed) Icons.Default.CheckCircle else Icons.Default.Info, contentDescription = null, tint = if (passed) Lime else Color(0xFFFFB45E), modifier = Modifier.size(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(value, color = Muted, fontSize = 12.sp)
    }
}

@androidx.compose.runtime.Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, color = Color(0xFFE1EAF0), fontSize = 13.sp)
    }
}
