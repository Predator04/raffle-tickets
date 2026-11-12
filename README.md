# Raffle Tickets

A simple Android app to keep track of raffle tickets and check winning numbers.

## Features
- **Add tickets** — a single number, or a whole strip at once as a range like `374129-374199`
- **Check a winning number** — type the full number *or* just the last 4 digits; it matches every ticket ending in what you type and shows **WINNER!** or **NO MATCH**
- **Share / Copy / Import** — send your ticket list to someone else, and they paste it back in, so two people keep identical copies
- **Auto-update** — checks GitHub Releases for a new version (12-hour cooldown) and installs in place
- **Delete protection** — long-press to delete one, and "Clear all" requires typing `DELETE` to confirm
- Dark glass UI, tickets stored on-device in SQLite

## Download
Get the latest APK from the [Releases](../../releases/latest) page.

## Build
Requires JDK 17 and the Android SDK.

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleRelease
```

Release APKs are signed with a private keystore (not in this repo) and attached to each GitHub Release.
