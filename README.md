# SignalWarden

**Network diagnostics, with the signal kept local.**

SignalWarden is an early Android network diagnostics app. Phase 1 shows Android's
current network state and lets the user run an explicit DNS and verified HTTPS
connectivity check. It does not capture traffic, create a VPN, run in the
background, or send diagnostics to a SignalWarden server.

## Phase 1 scope

- Current transport, Android's validated-internet status, metered status, and VPN presence.
- Interface, device addresses, DNS servers, proxy, and a short route summary.
- An on-demand DNS lookup and certificate-verified TLS handshake to `example.com`.
- No account, analytics, telemetry, or persistent diagnostic history.

The connection test sends a DNS query for `example.com` through the active network
and opens a TLS connection to `example.com:443`. Results are shown on-device and
discarded when the app process ends. Network details are visible only on the
device. Phase 1 requests only Android's `INTERNET` and `ACCESS_NETWORK_STATE`
permissions.

## Build

```bash
./gradlew :app:assembleDebug :app:lintDebug
```

The project uses the Android SDK in `~/android` and JDK 21. No signing material
or release publishing configuration is included yet.
