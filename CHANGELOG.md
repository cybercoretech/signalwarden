# 📜 Changelog

User-visible changes are recorded here. This project has not published a production release yet.

## Unreleased · `0.1.0-dev`

- 🛰️ Show active Android network transport, validation and metering state, VPN presence, interface, addresses, DNS servers, proxy, and a short route list.
- 🧪 Add an explicit DNS lookup and certificate-verified TLS reachability check for `example.com`.
- 📶 Measure DNS, TCP connect, and TLS handshake durations separately after the user starts a test; cap TCP attempts to two DNS answers.
- 🧫 Add deterministic tests for successful probes, stage failures, offline state, measured durations, and address fallback.
- 🔄 Refresh the snapshot when Android reports default-network changes.
- 🛡️ Document current privacy boundaries and Phase 1.1 limitations.
