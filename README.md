# 🛰️ SignalWarden

**See what your connection is doing. Keep the diagnostics on your device.**

SignalWarden is a Cybercore Android network diagnostics and privacy project. Phase 1.1 gives you a readable view of the network Android is using and an explicit, on-demand latency breakdown for DNS, TCP, and certificate-verified HTTPS. It is being built in small, inspectable steps, with privacy boundaries documented alongside the code.

> 🧭 **Current status:** Phase 1.1 · Android 11+ · version `0.1.0-dev` · not a VPN or packet analyzer.

🌐 **Project site:** [cybercoretech.github.io/signalwarden](https://cybercoretech.github.io/signalwarden/) · 🧰 **Source and issues:** [github.com/cybercoretech/signalwarden](https://github.com/cybercoretech/signalwarden)

## ⚡ What works today

- 📡 Shows the active transport, Android's validated-internet status, metered status, and whether Android reports an active VPN transport.
- 🧩 Displays the active interface, device addresses, DNS servers, proxy (if present), and a short route summary.
- 🧪 On tap, measures Android's DNS resolver call, TCP connect, and certificate-verified TLS handshake time to `example.com:443`; resolver timing may include cached results, and TCP tries at most the first two DNS answers with bounded timeouts.
- 🧫 Tests the probe's success, failure, timing, offline, and address-fallback behavior using deterministic local fakes.
- 🔄 Refreshes the network snapshot when Android reports that the default network or its properties have changed.
- 🛡️ Requests only `INTERNET` and `ACCESS_NETWORK_STATE`; it has no account, analytics, telemetry, advertising SDK, or SignalWarden server.

## 🔐 Privacy boundaries

Network information can be sensitive. SignalWarden reads Android's active network properties so it can display them on your phone. It does not upload or retain a diagnostic history. Pressing the test button asks Android to resolve `example.com`, then attempts a TCP connection and verified TLS handshake to port 443 using up to two returned addresses. Resolver duration may include an OS-cached result; it is not necessarily wire DNS latency. Your DNS resolver and `example.com` may observe ordinary connection metadata associated with requests.

The app currently has **no `VpnService`, packet capture, traffic inspection, background scanning, kill switch, DNS filtering, or remote diagnostics API**. VPN protocol or packet inspection functionality would require a separately designed and reviewed phase; it is not implied by the current app name or roadmap.

See [Privacy](docs/PRIVACY.md), [Security](SECURITY.md), and [Architecture](docs/ARCHITECTURE.md) for the fuller data flow and threat boundaries.

## 🧰 Build and run

Requirements: Android Studio or JDK 21, Android SDK 36, and an Android 11+ device or emulator.

```bash
git clone https://github.com/cybercoretech/signalwarden.git
cd signalwarden
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Install the development build on a connected device:

```bash
./gradlew :app:installDebug
```

The debug application uses the package ID `dev.cybercore.signalwarden.debug`. The release build is currently a development artifact and is not yet configured as a public signed release. Do not treat a locally built APK as an official production release.

## 🗺️ Direction

Phase 1.1 adds stage-by-stage latency results and deterministic tests. Any VPN, packet capture, or filtering capability needs an explicit design for consent, Android platform constraints, data minimization, and failure handling first. See [ROADMAP.md](docs/ROADMAP.md).

## 📚 Project docs

- 🧱 [Architecture and data flow](docs/ARCHITECTURE.md)
- 🕵️ [Privacy policy](docs/PRIVACY.md)
- 🛡️ [Security policy and reporting](SECURITY.md)
- 🧪 [Build and test guide](docs/TESTING.md)
- 🧩 [Contributing](CONTRIBUTING.md)
- 🗺️ [Roadmap](docs/ROADMAP.md)
- 📜 [Changelog](CHANGELOG.md)
- ⚖️ [Apache-2.0 license](LICENSE)

Made with ⚡ by [Cybercore Tech](https://github.com/cybercoretech).
