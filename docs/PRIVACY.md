# 🕵️ SignalWarden privacy notes

**Last reviewed:** 2026-10-19

**Applies to:** current Phase 1 source in this repository (`0.1.0-dev`).

SignalWarden is designed to keep its current diagnostic display local to the Android device. It has no SignalWarden server, user account, analytics, advertising SDK, or telemetry pipeline.

## What the app reads

The app requests Android's `ACCESS_NETWORK_STATE` permission and reads the active network's transport, internet and validation capabilities, metered state, VPN transport presence, interface name, link addresses, DNS server addresses, routes, and HTTP proxy when present. These values are displayed in the app. Android may expose private or identifying network details; use care when sharing screenshots or logs.

The app requests `INTERNET` to perform the explicit connectivity probe. Only after the user taps the test button, it resolves `example.com` on Android's active network and attempts TLS to `example.com:443`, checking the server certificate and hostname. The DNS resolver and remote host may observe those requests and normal connection metadata such as the source network address and time of the request.

## Storage and transmission

The app does not intentionally send the displayed network snapshot to a Cybercore endpoint, save a history, or collect it in the background. Probe results are held in the current UI process state and are not written to a local database. Android system networking handles DNS and TLS traffic for the probe.

## Current exclusions

This version does not implement `VpnService`, packet capture, traffic interception, DNS filtering, a kill switch, background scanning, or remote diagnostic upload. Future work that changes these boundaries must update this document, permissions, UI disclosure, and security review before release.

## Questions or concerns

Review the [source](https://github.com/cybercoretech/signalwarden) and [security policy](../SECURITY.md). Report vulnerabilities privately using the process in `SECURITY.md`; redact network identifiers from public issues.
