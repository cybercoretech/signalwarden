# 🛰️ SignalWarden

**See what your connection is doing. Keep the diagnostics on your device.**

SignalWarden is a Cybercore Android network diagnostics and privacy project. Its first phase gives you a readable view of the network Android is using and an explicit, on-demand check of DNS and certificate-verified HTTPS reachability. It is being built in small, inspectable steps, with privacy boundaries documented alongside the code.

> 🧭 **Current status:** Phase 1 prototype · Android 11+ · version `0.1.0-dev` · not yet a production VPN or packet analyzer.

🌐 **Project site:** [cybercoretech.github.io/signalwarden](https://cybercoretech.github.io/signalwarden/) · 🧰 **Source and issues:** [github.com/cybercoretech/signalwarden](https://github.com/cybercoretech/signalwarden)

## ⚡ What works today

- 📡 Shows the active transport, Android's validated-internet status, metered status, and whether Android reports an active VPN transport.
- 🧩 Displays the active interface, device addresses, DNS servers, proxy (if present), and a short route summary.
- 🧪 Runs a DNS lookup for `example.com` and then a TLS connection to `example.com:443` only when you tap **Run connection test**. TLS certificate and hostname checks are enabled.
- 🔄 Refreshes the network snapshot when Android reports that the default network or its properties have changed.
- 🛡️ Requests only `INTERNET` and `ACCESS_NETWORK_STATE`; it has no account, analytics, telemetry, advertising SDK, or SignalWarden server.

## 🔐 Privacy boundaries

Network information can be sensitive. SignalWarden reads Android's active network properties so it can display them on your phone. The current prototype does not upload or retain a diagnostic history. Pressing the test button makes a DNS request for `example.com` and attempts a verified HTTPS handshake with that host over the active Android network. Your DNS resolver and `example.com` may therefore observe the ordinary connection metadata associated with those requests.

The app currently has **no `VpnService`, packet capture, traffic inspection, background scanning, kill switch, DNS filtering, or remote diagnostics API**. VPN protocol or packet inspection functionality would require a separately designed and reviewed phase; it is not implied by the current app name or roadmap.

See [Privacy](docs/PRIVACY.md), [Security](SECURITY.md), and [Architecture](docs/ARCHITECTURE.md) for the fuller data flow and threat boundaries.

## 🧰 Build and run

Requirements: Android Studio or JDK 21, Android SDK 36, and an Android 11+ device or emulator.

```bash
git clone https://github.com/cybercoretech/signalwarden.git
cd signalwarden
./gradlew :app:assembleDebug :app:lintDebug
```

Install the development build on a connected device:

```bash
./gradlew :app:installDebug
```

The debug application uses the package ID `dev.cybercore.signalwarden.debug`. The release build is currently a development artifact and is not yet configured as a public signed release. Do not treat a locally built APK as an official production release.

## 🗺️ Direction

The roadmap separates safe diagnostics from higher-risk privacy controls. Next work can improve diagnostic clarity and add opt-in tests; any VPN, packet capture, or filtering capability needs an explicit design for consent, Android platform constraints, data minimization, and failure handling first. See [ROADMAP.md](docs/ROADMAP.md).

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
