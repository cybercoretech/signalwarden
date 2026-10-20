# 🧩 Contributing

Thanks for helping make SignalWarden useful and understandable. This is an early Android project; please keep changes small and describe user-visible behavior and privacy implications clearly.

## Before you start

- Check existing [issues](https://github.com/cybercoretech/signalwarden/issues) and open a discussion or issue for larger changes.
- Avoid adding analytics, network endpoints, permissions, background work, or collection of diagnostic data without documenting why and how users control it.
- Never include real network identifiers, device logs, credentials, or private screenshots in a commit.

## Build and validate

Use JDK 21 and Android SDK 36, then run:

```bash
./gradlew :app:assembleDebug :app:lintDebug
```

For changes to the project website, open `site/index.html` locally and run `node --check site/app.js`. Keep theme selection usable with keyboard navigation and with the Cybercore theme catalog unavailable.

## Pull requests

Describe the problem, the resulting behavior, relevant Android versions, and checks run. Include screenshots only when they do not expose personal network details. Update `CHANGELOG.md` when a user-visible behavior changes, and update the privacy and security documentation if data flows or permissions change.
