# 🗺️ Roadmap

SignalWarden is being developed in phases. Items below are direction, not claims that those features are already available.

## Phase 1 · Local diagnostics

- ✅ Active network summary and selected link properties.
- ✅ User-triggered DNS and verified HTTPS reachability check.
- 🔄 Improve error explanations, accessibility, and automated coverage.

## Phase 2 · Useful diagnostics

- Consider opt-in latency, DNS comparison, and route diagnostics with clear destinations, timeouts, and on-device results.
- Add export only with a preview and redaction controls for addresses and network identifiers.
- Document test traffic before each test so users can tell exactly what leaves the device.

## Phase 3 · Privacy controls research

- Evaluate Android `VpnService` requirements and user consent before deciding whether a local VPN mode belongs in the app.
- Assess established FOSS libraries and protocol implementations before implementing any VPN or packet-level feature.
- Define abuse prevention, secure defaults, performance and battery impact, failure behavior, and independent security review.

No VPN, packet capture, filtering, or remote service is currently implemented. The project will not present a roadmap item as an available feature.
