# 🛡️ Security policy

SignalWarden is an early-stage Android project. Do not rely on it as a VPN, firewall, intrusion detector, or security monitor. The current version reports selected Android network properties and performs a user-triggered DNS/TLS reachability test.

## Reporting a vulnerability

Please do not publish exploit details in a public issue. Use GitHub's private vulnerability reporting for [cybercoretech/signalwarden](https://github.com/cybercoretech/signalwarden/security/advisories/new), if available, or contact the maintainers through the [Cybercore Tech organization](https://github.com/cybercoretech). Include the affected version or commit, Android version, impact, and a minimal reproduction. Do not include real network captures, credentials, or unrelated personal data.

There is no guaranteed response time or current bug-bounty program. We will acknowledge reports as soon as practical and coordinate disclosure with the reporter.

## Scope and data handling

The current app has no backend, account, analytics, telemetry, VPN service, or packet capture. A report should focus on this repository or the Android application. Details shown by the app can include private addresses, DNS resolvers, routes, interface names, and proxy configuration; redact them before sharing logs or screenshots.
