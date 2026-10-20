# 🧪 Build and test

## Requirements

- JDK 21
- Android SDK 36 and matching build tools
- Android 11 (API 30) or newer for device testing

## Checks

Run the debug build, Android lint, and local unit tests:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Install on a connected device:

```bash
./gradlew :app:installDebug
```

## Manual smoke test

1. Launch SignalWarden and confirm that transport, status, interface, and DNS details match Android's current connection as far as the OS exposes them.
2. Switch Wi-Fi or mobile data, or enable/disable an existing VPN. Confirm the displayed snapshot updates.
3. Tap **Run latency + connection test** and confirm separate Android resolver-call, TCP, and verified TLS timings appear. This resolves `example.com` and connects to port 443 on at most two DNS answers. The DNS duration may include a cached answer; it is not necessarily wire DNS latency.
4. Repeat while offline or behind a restrictive network and confirm failures are described without crashing the app.
5. Review the permission declarations and ensure no private address, resolver, or route detail appears in screenshots or logs shared publicly.

The connection probe's unit tests use fakes and do not make network requests. The live website is a separate static site under `site/`; its theme chooser loads the public Cybercore theme catalog. Website theme selection should still display a readable fallback if that catalog cannot load.
