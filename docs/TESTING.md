# 🧪 Build and test

## Requirements

- JDK 21
- Android SDK 36 and matching build tools
- Android 11 (API 30) or newer for device testing

## Checks

Run the debug build and Android lint:

```bash
./gradlew :app:assembleDebug :app:lintDebug
```

Run unit tests when present:

```bash
./gradlew :app:testDebugUnitTest
```

Install on a connected device:

```bash
./gradlew :app:installDebug
```

## Manual smoke test

1. Launch SignalWarden and confirm that transport, status, interface, and DNS details match Android's current connection as far as the OS exposes them.
2. Switch Wi-Fi or mobile data, or enable/disable an existing VPN. Confirm the displayed snapshot updates.
3. Tap **Run connection test** and confirm DNS and verified HTTPS results appear. This makes a DNS request and TLS connection to `example.com`.
4. Repeat while offline or behind a restrictive network and confirm failures are described without crashing the app.
5. Review the permission declarations and ensure no private address, resolver, or route detail appears in screenshots or logs shared publicly.

The live website is a separate static site under `site/`; its theme chooser loads the public Cybercore theme catalog. Website theme selection should still display a readable fallback if that catalog cannot load.
