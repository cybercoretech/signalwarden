# 🧱 Architecture and data flow

SignalWarden Phase 1 is a native Kotlin Android application using Jetpack Compose. The app is intentionally a client-only diagnostic screen.

## Runtime flow

1. `MainActivity` reads a snapshot from Android's `ConnectivityManager` and displays it locally.
2. A default network callback refreshes the snapshot when network availability, capabilities, or link properties change.
3. The user may tap **Run connection test**. `ConnectionProbe` resolves `example.com` on the active Android `Network`, then opens a TLS socket to `example.com:443` and enables HTTPS endpoint identification.
4. The results are rendered in memory. There is no database, analytics pipeline, or application server.

## Trust boundaries

- Android OS APIs provide network state and perform the network operations.
- The configured DNS resolver handles the hostname lookup.
- `example.com` receives the HTTPS connection attempt and its ordinary network metadata.
- SignalWarden itself does not transmit the displayed snapshot to Cybercore infrastructure.

The app manifest currently declares only `INTERNET` and `ACCESS_NETWORK_STATE`. It does not declare `VpnService` or request location, contacts, storage, or nearby-device access. `INTERNET` is a normal permission; `ACCESS_NETWORK_STATE` allows inspection of connectivity state.

## Planned capability boundary

VPN-based diagnostics, packet inspection, filtering, or privacy controls are not part of Phase 1. Each would need its own architecture and consent model, platform and policy review, abuse analysis, and documentation before implementation. See [Roadmap](ROADMAP.md).
